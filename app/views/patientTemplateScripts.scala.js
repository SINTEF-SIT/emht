@import play.i18n._

var DEBUG = true;

var Patient = (function ($) {
	/* Private fields */

	/* Private methods */

	// Generate the Patient information div and bind address checkbox
	var generatePatientContainer = function () {
		if (DEBUG) console.log("Generating patient container");
		// Start by clearing the wrapper
		var wrapper = $('#dynamicPatientInfo');
		wrapper.empty();

		// building Patient Drop Down Block
		var patientDropDownBox =
			'<u>@Messages.get("patientpane.name"):</u> ' +
			'<span class="btn-group" id="patientDropDown">' +
			'<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">' +
			'<span class="selection">@Messages.get("patientpane.pill.patient")</span>' +
			'<span class="caret"></span></button>' +
			'<ul id="patientDropDownList" class="dropdown-menu"></ul></span><br>';

		var patientDetails =
			'<u>@Messages.get("patientpane.address"):</u> <span id="patientAddress"></span><br>' +
			'<u>@Messages.get("patientpane.personumber"):</u> <span id="patientPersonalNumber"></span><br>' +
			'<u>@Messages.get("patientpane.phonenumber"):</u> <span id="patientPhoneNumber"></span><br>' +
			'<u>@Messages.get("patientpane.age"):</u> <span id="patientAge"></span><br>' +
			'<input type="hidden" id="patientId" />' +
			'<p/><p/>' +
			'<u>@Messages.get("patientpane.incident.location"):</u>' +
			'<span class="checkbox inline"><label><input id="sameAddressCheckbox" type="checkbox">' +
			'@Messages.get("patientpane.incident.same"):</label></span>' +
			'<div class="input-group">' +
			'<input type="text" class="form-control" id="incidentAddress" placeholder="@Messages.get("patientpane.incident.other")">' +
			'<span class="input-group-btn"><button class="btn btn-default" id="verifyPatientLocation">' +
			'@Messages.get("patientpane.incident.checkaddress")</button>' +
			'</span></div>' +
			'<h5>@Messages.get("patientpane.log.title")</h5>' +
			'<table class="table table-bordered" id="patientLogTable">' +
			'<thead><tr><th>' +
			'@Messages.get("patientpane.log.date")</th><th>@Messages.get("patientpane.log.hour")</th>' +
			'<th>@Messages.get("patientpane.log.type")</th></tr></thead><tbody></tbody></table>';

		$('#dynamicPatientInfo').html(patientDropDownBox + patientDetails);

		// Bind the checkbox to copy patient address to incident location
		$("#sameAddressCheckbox").click(function () {
			var addr = $("#patientAddress").text();
			if (this.checked) {
				$("#incidentAddress").val(addr);
			} else {
				$('#incidentAddress').val('@Messages.get("patientpane.incident.other")');
			}
		});

		// Bind the verify button that checks the location and updates with Lat / Long coordinates if successful
		$('#verifyPatientLocation').on('click', function (e) {
			e.preventDefault();
			var address = $('#incidentAddress').val();
			MapView.convertAddressToLatLng(address, function (locationData) {
				if (DEBUG) console.log(locationData);
				if (locationData === null) {
					alert('@Messages.get("patientpane.incident.checkaddress.fail"): ' + address);
				} else {
					locationData.location = address;
					myJsRoutes.controllers.Application.setLocationOfAlarm(Alarms.gui.getCurrentSelectedAlarmIndex()).ajax({
						data: JSON.stringify(locationData),
						contentType: 'application/json',
						success: function (data) {
							alert('@Messages.get("patientpane.incident.checkaddress.success")');
						},
						error: function (xhr, statusText, thrownError) {
							alert('Failed to save resolved address coordinates to alarm!!!');
						}
					});
				}
			});
		});

		if (DEBUG) console.log("Patient container generation complete");
	};

	// Generate the dropdown list of possible patients based on a patient list
	var generateProspectPatients = function (patientList) {
		if (DEBUG) console.log("generateProspectPatients called: " + JSON.stringify(patientList, null, 4));
		var activePatient = Alarms.getActiveAlarm().data.patient;
		var patInProspects = false;
		var dropDown = $('#patientDropDownList');
		for (var i in patientList) {
			var listItem = $('<li></li>').html('<a href="#">' + patientList[i].name + '</a>');
			listItem.on('click', function (e) {
				e.preventDefault();
				populatePatientInformation(patientList[i]);
			});
			listItem.attr('id', 'Patient' + patientList[i].id);

			// If we have an active patient on the alarm that is in the prospect list, prepend it
			if (activePatient !== null && activePatient.id === patientList[i].id) {
				dropDown.prepend(listItem);
				patInProspects = true;
			} else {
				dropDown.append(listItem);
			}
		}
		// If the registered patient was not returned from prospect patients, prepend it into dropdown
		if (!patInProspects && activePatient !== null) {
			if (DEBUG) console.log("Registered patient was not in prospect patient list: " + activePatient.name);
			var listItem = $('<li></li>').html('<a href="#">' + activePatient.name + '</a>');
			listItem.attr('id', 'Patient' + activePatient.id);
			listItem.on('click', function (e) {
				e.preventDefault();
				populatePatientInformation(activePatient);
			});
			dropDown.prepend(listItem);
		}


		if ($.isArray(patientList) && patientList.length != 0) {
			listItem.append('<li class="divider"></li>');
		}
		var otherPatient = $('<li></li>').html('<a href="#">@Messages.get("patientpane.pill.other.patient")</a>');
		otherPatient.on('click', function (e) {
			e.preventDefault();
			Patient.openAddPatientModal();
		});
		var unknownPatient = $('<li></li>').html('<a href="#">@Messages.get("patientpane.pill.unknown")</a>');
		unknownPatient.on('click', function (e) {
			e.preventDefault();
			Patient.fillUnknownPatient();
		});
		dropDown.append(otherPatient);
		dropDown.append(unknownPatient);

		if (DEBUG) console.log("generateProspectPatients complete");

		/** LEGACY - KEEPING FOR REFERENCE
		 //if it is an alarm of type: fire, safety_alarm or fall, I've set the patient as the callee
		 var typeImage = currentSelected.find('.type-icon');
		 var currentAlarm_type = typeImage.attr('data-type');
		 if (currentAlarm_type == "fall" || currentAlarm_type == "fire" || currentAlarm_type == "safety_alarm") {
			for (var i in array) {
				var patName = array[i].name;
				var patAddress = array[i].address;
				if (patName == $("#calleeName").text() && patAddress == $("#calleeAddress").text()) {
					var patPersoNum = array[i].persoNumber;
					var patId = array[i].id;
					var patAge = array[i].age;
					var patPhoneNum = array[i].phoneNumber;
					var patObs = array[i].obs;
					Patient.populatePatient(patId, patName, patPersoNum, patAddress, patPhoneNum, patAge, patObs);

					break;
				}
			}
		}*/
	};

	// Updates the dynamic patient information with the provided patient object
	var populatePatientInformation = function (pat) {
		if (DEBUG) console.log("populatePatientInformation called: ");
		if (DEBUG) console.log("id:" + pat.id, "name:" + pat.name, "personalNumber:" + pat.personalNumber, "address:"+pat.address);
		// Cache the active alarm DOM object
		var currentSelected = Alarms.getActiveAlarm();

		// for the personalNumber, if it is more than 6 digits, we add a space after the first 6 digits
		var formattedPersonalNumber;
		if (pat.personalNumber.length > 6) {
			formattedPersonalNumber = pat.personalNumber.substring(0, 6) + " " + pat.personalNumber.substring(6);
		} else {
			formattedPersonalNumber = pat.personalNumber;
		}

		// Update fields
		$('#patientAddress').text(pat.address);
		$('#patientPersonalNumber').text(formattedPersonalNumber);
		$('#patientPhoneNumber').text(pat.phoneNumber);
		$('#patientAge').text(pat.age);
		$('#patientId').val(pat.id);
		// Set the obs field in the Assessment page
		if (pat.obs != null) $('#obsBody').text(pat.obs);

		Assessment.loadPatientSensor(pat.id);

		// Update notes and set occurrance address if set
		var notes = currentSelected.data.notes;
		var occurranceAddress = currentSelected.data.occuranceAddress;
		$("#globalNotesBox").val(notes);
		if (occurranceAddress !== null) {
			$("#incidentAddress").val(occurranceAddress);
			if ($('#patientAddress').val() == occurranceAddress) {
				$('#sameAddressCheckbox').attr('checked', true);
			}
		}

		populatePastAlarmsFromPatient(pat);
	};

	// Fetch previous alarms asynchronously and update the alarm history in the patient panel
	var populatePastAlarmsFromPatient = function (pat) {
		// Empty the tbody of the log table
		var tbody = $("#patientLogTable > tbody");
		tbody.html = "";

		if (0 != pat.id) {
			$.getJSON("/pastAlarmsFromPatient/" + pat.id, function(data) {
				if (null!= data && null != data.alarmArray) {
					var list = data.alarmArray;
					if (DEBUG) console.log("Fetched past alarms for " + pat.name + ":", list);
					for(var i in list) {
						var day = list[i].day;
						var hour = list[i].hour;
						var type = list[i].type;
						var notes = list[i].notes;
						var htmlRow = '<tr data-toggle="tooltip" data-placement="right" ' +
							'title="@Messages.get("actions.popup.send.notes"): ' + notes+'">' +
							'<td>' + day + '</td><td>' + hour + '</td><td>' + type + '</td></tr>';
						tbody.prepend(htmlRow);
					}
					$('[data-toggle="tooltip"]').tooltip({'placement': 'right'});
				}

				if (DEBUG) console.log("populatePatientInformation complete");
			});
		}
	}

	/* Public methods inside return object */
	return {
		init: function () {
			$('#addPatientModalButton').click(function() {
				Patient.addNewPatientFromModal();
			});
			$("#closeCaseFromPatientRegButton").click(Patient.closeCaseAtRegistration);
			$("#goToAssesmentButton").click(Patient.fromRegistrationToAssesment);
		},

		generatePatientContainer: function () {
			generatePatientContainer();
		},

		getCalleeAddress: function () {
			return $('#calleeAddress').text();
		},

		clearUpCalleeData: function () {
			$("#calleeName").text("");
			$("#calleeAddress").text("");
			$("#calleePhone").text("");
		},

		clearUpPatientData: function () {
			$("#dynamicPatientInfo").empty();
			// Stop autoUpdate of Sensor data
			Sensor.stopAutoUpdate();
		},

		openAddPatientModal: function () {
			//clear modal
			$("#add_patient_modal").find(':text').each(
				function() {
					$(this).val('');
				}
			);

			$('#add_patient_modal').modal("show");
		},

		populateCalleeFromAlarm: function (alarmId) {
			$.getJSON("/callee/" + alarmId,
				function(data) {
					// TODO: check if the json is full before creating the table
					var calleeId = data.id;
					var calleeName = data.name;
					var calleePhone = data.phoneNumber;
					var calleeAddress = data.address;

					$("#calleeName").text(calleeName);
					$("#calleeAddress").text(calleeAddress);
					$("#calleePhone").text(calleePhone);
				}
			);
		},

		retrievePatientsByAddress: function (alarmIndex) {
			if ($('#dynamicPatientInfo').length === 1) {
				$.getJSON("/prospectPatient/" + alarmIndex,
					function (data) { generateProspectPatients(data.patientArray) }
				);
			}
		},

		populatePatient: function (pat) {
			Assessment.loadPatientSensor(pat.id);
			populatePatientInformation(pat);
			$('#patientDropDown').find('.selection').text(pat.name);
		},

		addNewPatientFromModal: function () {
			var name = $('#modalInputPatientName').val();
			var address = $('#modalInputPatientAddress').val();
			var number = $('#modalInputPatientNumber').val();
			var phoneNumber = $('#modalInputPatientPhoneNumber').val();
			var age = $('#modalInputPatientAge').val();

			var inputPatient = {
				'name' : name,
				'address' : address,
				'personalNumber' : number,
				'phoneNumber' : phoneNumber,
				'age' : age
			};
			myJsRoutes.controllers.Application.insertPatientFromJson().ajax({
				data : JSON.stringify(inputPatient),
				contentType : 'application/json',
				success : function (outputPatient) {
					// add it to list
					var patientListItem = '<li id="Patient' + outputPatient.id + '"><a href="#">' + outputPatient.name +'</a></li>';
					$('#patientDropDownList').prepend(patientListItem);
					Patient.populatePatient(outputPatient);
					$('#Patient'+outputPatient.id).on('click', function (e) {
						e.preventDefault();
						Patient.populatePatient(outputPatient);
					});

				}// end of success
			});// end of ajax call
		},

		fillUnknownPatient: function () {
			var pat = {
				'id': '',
				'name': '@Messages.get("patientpane.pill.unknown")',
				'address': '',
				'personalNumber': '',
				'phoneNumber': '',
				'age': '',
				'obs': null
			}
			populatePatientInformation(pat);
		}
	}
})(jQuery)




/*	function closeCaseAtRegistration(){

 var updatedAlarm = getUpdatedAlarmFromAssesmentPage();

 myJsRoutes.controllers.Application.closeCase().ajax({
 data : JSON.stringify(updatedAlarm),
 contentType : 'application/json',
 success : function (data) {
 removeHighlightedAlarmFromList();
 highlightBackListTab ();
 }// end of success
 });// end of ajax call

 }

 function fromRegistrationToAssesment(){
 var updatedAlarm = getUpdatedAlarmFromAssesmentPage();

 myJsRoutes.controllers.Application.saveCase().ajax({
 data : JSON.stringify(updatedAlarm),
 contentType : 'application/json',
 success : function (data) {
 // TODO: possibly move some of this to a function
 highlightArrowHeader("assesmentArrowHeader");
 loadPatientSensor();
 $("#assesment").show();
 $("#assementNotesDiv").show();
 $('#notesDiv').hide();
 $('#assesmentNotesBox').val($('#patientRegistrationNotesBox').val());
 }// end of success
 });// end of ajax call
 }
 */
