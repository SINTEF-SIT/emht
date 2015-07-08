@import play.i18n._

DEBUG = true;

var Alarms = (function ($) {

	/* Private fields here */
	var ACTIVE_ALARM = null;
	var ME = null;

	// We keep track of overall state of all alarms in a single array
	var alarms = [];

	/* Internal classes */

	// The Alarm object is the main component used. It uses different state fields as well as prototype methods
	// for selecting, deselecting, moving the alarm between different states etc.
	var Alarm = function (data) {
		if (DEBUG) console.log("Alarm constructor called with data: " + JSON.stringify(data, null, 4));
		this.id = data.id;
		this.selected = false;
		this.data = data;
		this.state = 'open';
		this.protected = false;
		this.DOM = null;
		if (data.attendant !== null && data.attendant !== undefined) {
			this.state = 'assigned';
			if (data.dispatchingTime !== null && data.dispatchingTime !== undefined) {
				this.state = 'followup';
			}
		}
		if (data.finished !== null && data.finished === true) this.state = 'finished';
		if (data.closingTime !== null && data.closingTime !== undefined) {
			this.state = 'closed';
		}
	};
	Alarm.prototype = {
		constructor: Alarm,

		isOpen: function () { return this.state === 'open'; },

		isAssigned: function () { return this.state === 'assigned'; },

		isFollowup: function () { return this.state === 'followup'; },

		isFinished: function () { return this.state === 'finished'; },

		isClosed: function () { return this.state === 'closed'; },

		// We treat open alarms as basically belonging to 'everyone' until assigned to an operator
		isMine: function () {
			return this.isOpen() || (this.data.attendant !== null && this.data.attendant.id === ME.id);
		},

		isLocationVerified: function () { return this.data.latitude !== null && this.data.longitude !== null; },

		moveToAssigned: function () {
			if (this.protected) return alert('Alarm is protected, cannot modify.');

			// If we already are in assigned, just ignore the call
			if (this.state === 'assigned') return;
			this.DOM.removeAttr('onclick');
			// Select this Alarm, so previously selected are deselected
			this.select();
			var self = this;
			// Make a new DOM item, unbind click listeners, update DOM ref, re-select and bind new click listeners
			var newAlarmItem = this.DOM.clone();
			this.DOM.remove();
			this.DOM = newAlarmItem;
			this.DOM.off('click');
			this.DOM.unbind('click');

			if (this.state === 'finished') {
				this.DOM.children('.clock-icon').remove();
				this.DOM.children(':first')
					.after('<img src="/assets/images/finished.png" class="img-thumbnail pull-left finished-icon">');
			}
			else this.state = 'assigned';

			// Now that we have updated all references and state, we select 'this' again to update
			// the DOM with .active class etc.
			this.select();

			// Add a new click-listener, since we are now in an assigned state and require the selectMyAlarm
			// functionality
			this.DOM.on('click', function (e) {
				e.preventDefault();
				Alarms.gui.selectMyAlarm(self.id);
			});

			// Remove the clock icon if it was set
			this.DOM.children('.clock-icon').remove();

			// Prepend the new DOM object to the assigned alarm list.
			$('#assignedAlarmList').prepend(newAlarmItem);

			Alarms.gui.populateAlarmDetails(this.id);
			Alarms.gui.resetAlarmCount();
		},

		moveToFollowup: function () {
			if (this.protected) return alert('Alarm is protected, cannot modify.');

			// Check if we have an assignment to care taker, if so we must set the value in the list item
			if (this.data.mobileCareTaker !== null) {
				this.DOM.children('.assignedTo')
					.html('Assigned to: <strong>' + this.data.mobileCareTaker.username + '</strong>');
			}

			// If we already are in followup, just ignore the rest of the call
			if (this.state === 'followup') return;
			this.DOM.removeAttr('onclick');
			// We select ourselves to invalidate other potentially selected/active Alarm objects
			this.select();
			// Keep a reference of self, since callback functions override 'this'
			var self = this;

			// Now it's time to clone the DOM object, and remove it from current list
			var newAlarmItem = this.DOM.clone();
			this.DOM.remove();
			this.DOM = newAlarmItem;
			this.DOM.off('click');
			this.DOM.unbind('click');
			this.state = 'followup';

			// Add a click listener pointing to the followUp click handler
			this.DOM.on('click', function (e) {
				e.preventDefault();
				Alarms.gui.selectFollowUpAlarm(self.id);
			});
			$('#followupAlarmList').append(this.DOM);
			Alarms.gui.resetAlarmCount();
		},

		toString: function () { return "Alarm " + this.id + " (Status: " + this.state + ") [Selected: " + this.selected + "]"; },

		select: function () {
			if (ACTIVE_ALARM !== null) {
				ACTIVE_ALARM.DOM.removeClass('active');
				ACTIVE_ALARM.selected = false;
			}
			ACTIVE_ALARM = this;
			this.selected = true;
			this.DOM.addClass('active');
			return this;
		},

		selectProtected: function () {
			this.select();
			this.protected = true;
			Alarms.gui.selectFollowUpAlarm(this.id);
			return this;
		},

		deselect: function () {
			if (this.selected && ACTIVE_ALARM === this) {
				this.DOM.removeClass('active');
				this.selected = false;
				ACTIVE_ALARM = null;
				Alarms.gui.clearUpData();
			}
			return this;
		},

		// Updates the state of this alarm and performs a callback
		update: function (callback) {
			var self = this;
			$.getJSON('/alarm/' + self.id, function (data) {
				// Update backend data field and client-side state
				self.data = data;
				if (data.attendant !== null) {
					self.state = 'assigned';
					if (data.dispatchingTime !== null) {
						self.state = 'followup';
					}
				}
				if (data.closingTime !== null && data.closingTime !== undefined) {
					self.state = 'closed';
				}

				// If it has been closed, remove it.
				if (self.isClosed()) Alarms.removeAlarm(self);

				// Do the callback and provide the updated Alarm object in return
				callback(self);
			});
		}
	};

	/* Private helper methods here */

	// Premade sort function that sorts alarms with longest time since arrival first
	var sortByTime = function (a, b) {
		if (a.data.openingTime < b.data.openingTime) return -1;
		else if (a.data.openingTime > b.data.openingTime) return 1;
		return 0;
	};

	var handleModalCancel = function () {
		$("#callee_info_modal").on('hide.bs.modal', function () {
			// unhighlight any highlighted alarm
			var currentSelected = $('.list-group-item.active.alarmItem');
			currentSelected.toggleClass("active");
		});
	};

	var handleResetAlarmCount = function () {
		var unassignedAlarmCount = Alarms.getNumberOfOpenAlarms();
		var assignedAlarmCount = Alarms.getNumberOfAssignedAlarms();
		var followUpAlarmCount = Alarms.getNumberOfFollowupAlarms();
		$("#nbOfUnassignedAlarm").text(unassignedAlarmCount);
		$("#nbOfAssignedAlarm").text(assignedAlarmCount);
		$("#nbOfFollowUpAlarm").text(followUpAlarmCount);
	};

	// Mode switching is when an operator switches between Mine/All alarms in the left panel
	var handleModeSwitch = function () {
		var myAlarmsOnly = $('.showMyAlarmsOnly');
		var allAlarms = $('.showAllAlarms');
		myAlarmsOnly.on('click', function (e) {

			if (DEBUG) console.log('Switching to "My Alarms Only" mode.');
			e.preventDefault();

			// Switch the btn class to change state
			allAlarms.removeClass('btn-primary').addClass('btn-default');
			myAlarmsOnly.removeClass('btn-default').addClass('btn-primary');

			Alarms.gui.setMyAlarmsOnly(true);
		});
		allAlarms.on('click', function (e) {

			if (DEBUG) console.log('Switching to "All Alarms" mode.');
			e.preventDefault();

			// Switch the btn class to change state
			myAlarmsOnly.removeClass('btn-primary').addClass('btn-default');
			allAlarms.removeClass('btn-default').addClass('btn-primary');

			Alarms.gui.setMyAlarmsOnly(false);
		});
	};

	// Fetches all non-closed alarms from the database and populates the client-side alarm cache
	var fetchAllOpenAlarms = function (callback) {
		$.getJSON('/alarm/allOpen', function (data) {
			alarms = [];
			for (var i in data.alarms) {
				if (data.alarms.hasOwnProperty(i)) {
					var alarm = new Alarm(data.alarms[i]);
					alarm.DOM = $('#Alarm' + alarm.id);
					alarms.push(alarm);
				}
			}
			if (DEBUG) console.log(alarms);
			if (callback !== null && callback !== undefined) {
				callback();
			}
		});
	};

	// Fetch alarm by ID
	var getAlarm = function (id) {
		for (var i in alarms) {
			if (alarms.hasOwnProperty(i) && alarms[i].id === id) return alarms[i];
		}
		return null;
	};

	// Takes in an Alarm object and an optional updateFunction. Creates the DOM representation of the Alarm
	// object, and if provided, invokes the update function. The update function must accept the DOM alarm representation
	// as an argument. Common uses of the updateFunction would be calling .html(), .append() etc and adding
	// click event handlers.
	var buildDOMAlarm = function (a, updateFunction) {
		var alarm = a.data;
		// Build DOM representation of alarm
		var time = new Date(alarm.openingTime);
		var formattedTime = $.format.date(time, "dd/MM HH:mm");
		var listItem =
			'<a href="#" idnum="' + alarm.id + '" id="Alarm' + alarm.id +
			'" class="list-group-item alarmItem"><img src="/assets/images/' +
			alarm.type + '.png" class="img-thumbnail pull-left type-icon" data-type="'+
			alarm.type + '" width="48" height="48"/>' +
			'<h4 class="list-group-item-heading"> @Messages.get("listitem.arrived") ' +
			formattedTime  +' </h4><p class="list-group-item-text">@Messages.get("listitem.callee") ' +
			alarm.callee.name + ' ' + alarm.callee.phoneNumber + '<div class="assignedTo">';

		// Pre-fill the Assigned to field if we have a mobile caretaker registered
		if (alarm.mobileCareTaker !== null) {
			listItem += 'Assigned to: <strong>' + alarm.mobileCareTaker.username + '</strong>';
		}

		listItem += '</div></p>';

		if (updateFunction === null || updateFunction === undefined) {
			return listItem;
		} else {
			updateFunction(listItem);
		}
	};

	// Retrieves a JSON array of all past alarms from a specific callee (represented by Callee ID)
	var getPastAlarmsFromCallee = function (calleeIndex) {
		$.getJSON("/pastAlarmsFromCalle/" + calleeIndex,
			function(data) {
				// TODO: check if the json is full before creating the table
				$("#calleeLogTableDiv").empty();
				var htmlTable = '<table class="table" id="pastCalleeAlarmsTable">' +
					'<thead><tr><td>@Messages.get("handling.popup.date")</td>' +
					'<td>@Messages.get("handling.popup.hour")</td>' +
					'<td>@Messages.get("handling.popup.type")</td></tr></thead><tbody>';
				// TODO: validate the json
				// data is a JSON list, so we can iterate over it
				var array = data.alarmArray;
				for(var i in array){
					var day = array[i].day;
					var hour = array[i].hour;
					var type = array[i].type;
					htmlTable+= '<tr><td> ' + day + ' </td><td> ' + hour + ' </td><td> ' + type + ' </td></tr>';
				}
				htmlTable+= "</tbody></table>";
				$("#calleeLogTableDiv").html(htmlTable);
			}
		);
	};

	/* Public methods inside return object */
	return {
		init: function () {

			// Display all fragments
			$("#assesment").show();
			$("#patientBox").show();
			$("#calleeBox").show();
			$("#extraActionButtonsDiv").show();
			$("#closingNotesAndButtons").show();

			// Start by fetching the AlarmAttendant object representing ourselves
			$.getJSON('/me', function (data) {

				if (DEBUG) console.log("Fetched active user object: " + data.username + " (" + data.id + ")");
				ME = data;

				// When we know who we are, fetch all alarms and trigger other module initializations
				fetchAllOpenAlarms(function () {
					WebSocketManager.init();
					Patient.init();
					Assessment.init();
					Actions.init();

					Alarms.gui.resetAlarmCount();
				});
			});

			// if the modal is canceled, we clear the active item
			handleModalCancel();

			// Bind button listener to mode switching between only my alarms or other alarms as well
			handleModeSwitch();
		},

		gui: {
			resetAlarmCount: function () {
				handleResetAlarmCount();
			},

			resetNotes: function () {
				$("#globalNotesBox").val("");
				$('#notesLog').text('');
			},

			populateAlarmDetails: function (alarmIndex) {
				var a = getAlarm(alarmIndex);
				Patient.generatePatientContainer();
				Patient.populateCalleeFromAlarm(alarmIndex);
				Patient.retrievePatientsByAddress(alarmIndex);
				if (a.data.patient !== null && a.data.patient !== undefined) {
					console.log("populateAlarmDetails: " + JSON.stringify(a.data, null, 4));
					Patient.populatePatient(a.data.patient);
				}
				Assessment.pupulateDOMfromAssessment(a.data.assessment);
			},

			moveAlarmToFollowUpList: function () {
				Alarms.getActiveAlarm().moveToFollowup();
				Alarms.getActiveAlarm().deselect();
			},

			selectFollowUpAlarm: function (alarmIndex) {
				var a = getAlarm(alarmIndex);
				a.select();

				if (DEBUG) {
					console.log("selectFollowupAlarm called on index: " + alarmIndex);
					console.log("state of alarm cache is: " + alarms);
					if (DEBUG) console.log("Selected MyAlarm: ", JSON.stringify(a.data, null, 4));
				}

				// Update the alarm with new data from the backend
				a.update(function (alarm) {
					// Remove recurring icon
					var recurring = alarm.DOM.children('.recurring-icon');
					if (recurring != null) recurring.remove();

					Alarms.gui.populateAlarmDetails(alarm.id);
				});
			},

			selectOpenAlarm: function (alarmIndex, calleeIndex) {
				// Select the alarm
				getAlarm(alarmIndex).select();

				// Update past alarms field
				getPastAlarmsFromCallee(calleeIndex);

				$('#callee_info_modal').modal("show");
				$('#confirmCalleeModalButton').unbind("click").click(function (e) {
					Alarms.assign(alarmIndex);
				});
			},

			selectMyAlarm: function (alarmIndex) {
				getAlarm(alarmIndex).select();
				if (DEBUG) console.log("Selected MyAlarm: ", JSON.stringify(getAlarm(alarmIndex).data, null, 4));
				Alarms.gui.populateAlarmDetails(alarmIndex);
			},

			clearUpData: function () {
				if (ACTIVE_ALARM !== null) {
					ACTIVE_ALARM.deselect();
				}
				Patient.clearUpCalleeData();
				Patient.clearUpPatientData();
				Assessment.reset();
				Actions.reset();
				Alarms.gui.resetAlarmCount();
				Alarms.gui.resetNotes();
			},

			getCurrentSelectedAlarmIndex: function () {
				if (ACTIVE_ALARM === null) return null;
				return ACTIVE_ALARM.id;
			},

			setMyAlarmsOnly: function (yes) {
				var a = Alarms.getAlarmsAssignedToOthers();
				for (var i in a) {
					// Wrap inner logic in a closure to prevent the mutable iterator variable from being accessible
					// inside the inner callbacks.
					(function () {
						if (a.hasOwnProperty(i)) {
							var otherAlarm = a[i];
							// If the DOM field is an empty array (does not exist)
							// We need to build the DOM object and bind the regular click listener.
							if (otherAlarm.DOM.length === 0) {
								buildDOMAlarm(otherAlarm, function (domAlarm) {
									// Build the DOM object using jQuery wrapper
									domAlarm = $(domAlarm);

									// Since the DOM ref was void, we need to update it on the Alarm object
									otherAlarm.DOM = domAlarm;

									// Check where to put the alarm and append it
									if (otherAlarm.state === 'assigned') {
										$('#assignedAlarmList').append(domAlarm);

									} else if (otherAlarm.state === 'followup') {
										$('#followupAlarmList').append(domAlarm);
									}

									// Distinguish this item from our own assigned items
									domAlarm.css({'background': '#eee', 'color': '#000'});

									// Add click listener
									domAlarm.on('click', function (e) {
										e.preventDefault();
										otherAlarm.selectProtected();
									});
								});
							}
							// Check whether we are revealing or hiding the DOM object of the other alarms
							if (!yes) {
								a[i].DOM.show();
							} else {
								a[i].DOM.hide();
							}
						}
					})()
				}

				// Finally we clear the view if the selected alarm was not one assigned to us
				if (yes && ACTIVE_ALARM !== null && !ACTIVE_ALARM.isMine()) {
					Alarms.gui.clearUpData();
				}
			}
		},

		assign: function (alarmIndex) {
			if (DEBUG) console.log("assign called on index: " + alarmIndex);

			if (this.protected) return alert('Alarm is protected, cannot modify.');

			var assignAlarmReq = {
				'alarmId' : alarmIndex
			};

			myJsRoutes.controllers.Application.assignAlarmFromJson().ajax({
				data : JSON.stringify(assignAlarmReq),
				contentType : 'application/json',
				success : function (data) {
					getAlarm(alarmIndex).data = data;
					getAlarm(alarmIndex).moveToAssigned();
				}
			});
		},

		addAlarm: function (alarm) {
			var a = new Alarm(alarm);
			buildDOMAlarm(a, function (alarmDOM) {
				// Update DOM, set the Alarm object DOM reference and add click listener
				$("#unassignedAlarmList").append(alarmDOM);
				a.DOM = $('#Alarm' + a.id);
				a.DOM.on('click', function (e) {
					e.preventDefault();
					Alarms.gui.selectOpenAlarm(a.data.id, a.data.callee.id);
				});
				alarms.push(a);
				alarms.sort(sortByTime);
				Alarms.gui.resetAlarmCount();
			});
		},

		getAlarm: function (alarmId) {
			return getAlarm(alarmId);
		},

		getActiveAlarm: function () {
			return ACTIVE_ALARM;
		},

		getMyAlarmsIncludingOpen: function () {
			var collector = [];
			for (var i in alarms) {
				if (alarms.hasOwnProperty(i) &&
				   (alarms[i].data.attendant.id === ME.id || alarms[i].state === 'open')) {
					collector.push(alarms[i]);
				}
			}
			return collector;
		},

		getAllAlarms: function () {
			return alarms.slice(); // Shallow copy since we don't want accidental tampering with the orig. array
		},

		getAlarmsAssignedToOthers: function () {
			var collector = [];
			for (var i in alarms) {
				if (alarms.hasOwnProperty(i) &&
				   (alarms[i].data.attendant !== null && alarms[i].data.attendant.id !== ME.id)) {
					collector.push(alarms[i]);
				}
			}
			return collector;
		},

		getNumberOfOpenAlarms: function () {
			var tot = 0;
			for (var i in alarms) {
				if (alarms.hasOwnProperty(i) &&
					alarms[i].state === 'open') tot++;
			}
			return tot;
		},

		getNumberOfAssignedAlarms: function () {
			var tot = 0;
			for (var i in alarms) {
				if (alarms.hasOwnProperty(i) &&
					alarms[i].state === 'assigned' &&
					alarms[i].data.attendant.id === ME.id) tot++;
			}
			return tot;
		},

		getNumberOfFollowupAlarms: function () {
			var tot = 0;
			for (var i in alarms) {
				if (alarms.hasOwnProperty(i) &&
					alarms[i].state === 'followup' &&
					alarms[i].data.attendant.id === ME.id) tot++;
			}
			return tot;
		},

		removeAlarm: function (alarm) {
			if (alarm !== null && alarm !== undefined) {
				alarm.deselect();
				alarm.DOM.remove();
				alarms.splice(alarms.indexOf(alarm), 1);
				alarms.sort(sortByTime);
				Alarms.gui.clearUpData();
				Alarms.gui.resetAlarmCount();
			}
		},

		me: function () {
			return ME;
		}
	}
})(jQuery);

$(document).ready(function () {
	// Fire up the module on DOM ready
	Alarms.init();
})
