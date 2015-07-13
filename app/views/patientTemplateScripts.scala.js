@import play.i18n._

var DEBUG = true;

var Patient = (function ($) {
	/* Private fields */
	var SEARCH_AUTOCOMPLETE_TIMER = null;
	var SEARCH_AUTOCOMPLETE_DELAY = 500;

	/* Private methods */

	// Generate the Patient information div and bind address checkbox
	var generatePatientContainer = function () {
		if (DEBUG) console.log("Generating patient container");
		// Start by clearing the wrapper
		var wrapper = $('#dynamicPatientInfo');
		wrapper.empty();

		var activeAlarm = Alarms.getActiveAlarm();

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
				activeAlarm.data.latitude = activeAlarm.data.patient.latitude;
				activeAlarm.data.longitude = activeAlarm.data.patient.longitude;
			} else {
				$('#incidentAddress').val('');
			}
			// If patient is set, persist occurrenceAddress on patient object
			if (activeAlarm.isClientSideCacheable()) {
				activeAlarm.data.occuranceAddress = $('#incidentAddress').val()
			}
		});

		// Update client side alarm data object on every keystroke
		$('#incidentAddress').on('keyup', function (e) {
			if (activeAlarm.isClientSideCacheable()) {
				activeAlarm.data.occuranceAddress = $(this).val();
			}
		})

		// Bind the search button
		$('#patientSearch').on('click', function (e) {
			e.preventDefault();
			Patient.openPatientSearchModal();
		})

		// Bind the verify button that checks the location and updates with Lat / Long coordinates if successful
		$('#verifyPatientLocation').on('click', function (e) {
			e.preventDefault();

			if (activeAlarm.protected) return alert('@Messages.get("actions.alerts.alarm_protected")');
			if (activeAlarm.isFollowup()) return alert('@Messages.get("actions.alerts.alarm_followup")');

			var address = $('#incidentAddress').val();
			MapView.convertAddressToLatLng(address, function (locationData) {
				if (DEBUG) console.log(locationData);
				if (locationData === null) {
					alert('@Messages.get("actions.alerts.location_failed"): ' + address);
				} else {
					locationData.location = address;
					myJsRoutes.controllers.Application.setLocationOfAlarm(Alarms.gui.getCurrentSelectedAlarmIndex()).ajax({
						data: JSON.stringify(locationData),
						contentType: 'application/json',
						success: function (data) {
							alert('@Messages.get("actions.alerts.location_verified")');
                            activeAlarm.data.occuranceAddress = data.occuranceAddress;
							activeAlarm.data.latitude = data.latitude;
							activeAlarm.data.longitude = data.longitude;
						},
						error: function (xhr, statusText, thrownError) {
							alert('@Messages.get("actions.alerts.location_failed_server_error")');
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
		var activeAlarm = Alarms.getActiveAlarm();
		var dropDown = $('#patientDropDownList');
		var listItem;
		for (var i in patientList) {
			// Wrap actions in closure to lock variable refs to inner scope
			(function () {
				var pat = patientList[i];
				listItem = $('<li></li>').html('<a href="#">' + patientList[i].name + '</a>');
				listItem.on('click', function (e) {
					e.preventDefault();
					if (activeAlarm.protected) return alert('@Messages.get("actions.alerts.alarm_protected")');
					if (activeAlarm.isFollowup()) return alert('@Messages.get("actions.alerts.alarm_followup")');

					populatePatientInformation(pat);
				});
				listItem.attr('id', 'Patient' + patientList[i].id);

				// If we have an active patient on the alarm that is in the prospect list, prepend it
				if (activeAlarm.data.patient !== null && activeAlarm.data.patient.id === patientList[i].id) {
					dropDown.prepend(listItem);
				} else {
					dropDown.append(listItem);
				}
			})()
		}

		if ($.isArray(patientList) && patientList.length != 0) {
			listItem.append('<li class="divider"></li>');
		}
		var otherPatient = $('<li></li>').html('<a href="#">@Messages.get("patientpane.pill.other.patient")</a>');
		otherPatient.on('click', function (e) {
			e.preventDefault();
			if (activeAlarm.protected) return alert('@Messages.get("actions.alerts.alarm_protected")');
			if (activeAlarm.isFollowup()) return alert('@Messages.get("actions.alerts.alarm_followup")');
			Patient.openPatientSearchModal();
		});
		var unknownPatient = $('<li></li>').html('<a href="#">@Messages.get("patientpane.pill.unknown")</a>');
		unknownPatient.on('click', function (e) {
			e.preventDefault();
			if (activeAlarm.protected) return alert('@Messages.get("actions.alerts.alarm_protected")');
			if (activeAlarm.isFollowup()) return alert('@Messages.get("actions.alerts.alarm_followup")');
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
		if (DEBUG) console.log(JSON.stringify(pat, null, 4));
		// Cache the active alarm DOM object
		var currentSelected = Alarms.getActiveAlarm();

        // Update the active alarm patient field
        currentSelected.data.patient = pat;
        if (DEBUG) console.log("Set patient on active alarm to: " + pat.name);

		// for the personalNumber, if it is more than 6 digits, we add a space after the first 6 digits
		var formattedPersonalNumber;
		if (pat.personalNumber !== null && pat.personalNumber !== undefined && pat.personalNumber.length > 6) {
			formattedPersonalNumber = pat.personalNumber.substring(0, 6) + " " + pat.personalNumber.substring(6);
		} else {
			formattedPersonalNumber = pat.personalNumber;
		}

		// Update fields
		$('#patientAddress').text(pat.address === null ? "" : pat.address);
		$('#patientPersonalNumber').text(formattedPersonalNumber === null ? "" : pat.personalNumber);;
		$('#patientPhoneNumber').text(pat.phoneNumber === null ? "" : pat.phoneNumber);
		$('#patientAge').text(pat.age === null ? "" : pat.age);
		$('#patientId').val(pat.id);
		// Set the obs field in the Assessment page
		if (pat.obs != null) $('#obsBody').text(pat.obs);

		Assessment.loadPatientSensor(pat.id);

		// Update notes and set occurrance address if set
		var notes = currentSelected.data.notes;
		var occurrenceAddress = currentSelected.data.occuranceAddress;
		$("#notesLog").text(notes === null ? "" : notes);
		$("#globalNotesBox").val('')

        // Check existance and potential equality between occurrence address and patient address
        if (occurrenceAddress !== null) {
			$("#incidentAddress").val(occurrenceAddress);
            // Just click the checkbox if it is the same
            if ($('#patientAddress').val().toLowerCase() === occurrenceAddress.toLowerCase()) $('#sameAddressCheckbox').click();
            else ($('#sameAddressCheckbox').removeAttr('checked'));
		} else {
            $('#sameAddressCheckbox').click();
			currentSelected.data.latitude = pat.latitude;
			currentSelected.data.longitude = pat.longitude;
        }

        // Update the dropdown selector
        $('#patientDropDown').find('.selection').html(pat.name);

		populatePastAlarmsFromPatient(pat);
		// Persist the selection in the database.
		setPatientOnAlarm(currentSelected.id, pat);
	};

	// Fetch previous alarms asynchronously and update the alarm history in the patient panel
	var populatePastAlarmsFromPatient = function (pat) {
		// Empty the tbody of the log table
		var tbody = $("#patientLogTable > tbody");
		tbody.html("");;

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

	// Persists patient selection in the back end database
	var setPatientOnAlarm = function (alarmId, patient) {
		myJsRoutes.controllers.Application.setPatientOfAlarm(alarmId).ajax({
			data : JSON.stringify(patient),
			contentType : 'application/json',
			success : function (outputPatient) {
				if (DEBUG) console.log('Patient ' + outputPatient.name + ' updated on active alarm serverside.');
			},
			error: function (xhr, thrownError, statusMsg) {
				alert('@Messages.get("actions.alerts.patient_store_error")');
			}
		})
	};

	// Helper that generates a result table of patients in the patient search modal
	var drawPatientSearchResults = function (results) {
		var activeAlarm = Alarms.getActiveAlarm();
		var resultContainer = $('#patient-search-modal-results');
		var tbl = '<table class="table table-striped table-condensed"><thead><tr>' +
			'<th>@Messages.get("patientpane.name")</th>' +
			'<th>@Messages.get("patientpane.address")</th>' +
			'<th>@Messages.get("patientpane.phonenumber")</th>' +
			'<th>@Messages.get("patientsearchmodal.select")</th>' +
			'</tr></thead><tbody>';

		for (var i in results) {
			if (results.hasOwnProperty(i)) {
				tbl += 	'<tr>' +
						'<td>' + results[i].name + '</td>' +
						'<td>' + results[i].address + '</td>' +
						'<td>' + results[i].phoneNumber + '</td>' +
						'<td><button class="btn btn-primary patientSelection" id="PatientSearch' + i + '">' +
						'@Messages.get("patientsearchmodal.select")</button></td>' +
						'<tr>';
			}
		}

		tbl += '</tbody></table>';

		resultContainer.html(tbl);

		$('.patientSelection').on('click', function (e) {
			e.preventDefault();
			if (activeAlarm.protected) return alert('@Messages.get("actions.alerts.alarm_protected")');
			if (activeAlarm.isFollowup()) return alert('@Messages.get("actions.alerts.alarm_followup")');
			var patient = results[Number($(this).attr('id').replace('PatientSearch', ''))];
			populatePatientInformation(patient);
			$('#patient-search-modal').modal('hide');
		})
	}

	/* Public methods inside return object */
	return {
		init: function () {
			$('#addPatientModalButton').click(function() {
				var activeAlarm = Alarms.getActiveAlarm();
				if (activeAlarm.protected) return alert('@Messages.get("actions.alerts.alarm_protected")');
				if (activeAlarm.isFollowup()) return alert('@Messages.get("actions.alerts.alarm_followup")');
				Patient.addNewPatientFromModal();
			});
			$("#closeCaseFromPatientRegButton").click(Patient.closeCaseAtRegistration);
			$("#goToAssesmentButton").click(Patient.fromRegistrationToAssesment);

			$('#patient-search-modal-search-button').on('click', function (e) {
				e.preventDefault();
				var searchString = $('#patient-search-modal-query').val();
				myJsRoutes.controllers.Application.patientSearch().ajax({
					data : JSON.stringify({"query": searchString}),
					contentType : 'application/json',
					success: function (data) {
						drawPatientSearchResults(data.results);
					},
					error: function (xhr, err, statusTxt) {
						alert('@Messages.get("actions.alerts.patient_search_error")');
					}
				});
			});

			$('#patient-search-modal-query').on('keyup', function (e) {
				e.preventDefault();
				clearTimeout(SEARCH_AUTOCOMPLETE_TIMER);
				SEARCH_AUTOCOMPLETE_TIMER = setTimeout(function () {
					$('#patient-search-modal-search-button').click();
				}, SEARCH_AUTOCOMPLETE_DELAY);
			})

			$('#add_patient_modal_button').on('click', function (e) {
				e.preventDefault();
				Patient.openAddPatientModal();
			});
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

		openPatientSearchModal: function () {
			// Clear modal
			$('#patient-search-modal-query').val('');
			$('#patient-search-modal-results').html('');
			$('#patient-search-modal').modal('show');
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
					function (data) { generateProspectPatients(data.patients) }
				);
			}
		},

		populatePatient: function (pat) {
			populatePatientInformation(pat);
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

			// Define the ajax call to store patient as a function we can trigger on geolocation success
			var savePatient = function () {
				if (DEBUG) console.log("Saving patient to database: " + JSON.stringify(inputPatient, null, 4));
				myJsRoutes.controllers.Application.insertPatientFromJson().ajax({
					data : JSON.stringify(inputPatient),
					contentType : 'application/json',
					success : function (outputPatient) {
						var activeAlarm = Alarms.getActiveAlarm();
						// add it to list
						var patientListItem = '<li id="Patient' + outputPatient.id + '"><a href="#">' + outputPatient.name +'</a></li>';
						$('#patientDropDownList').prepend(patientListItem);
						Patient.populatePatient(outputPatient);

						$('#Patient'+outputPatient.id).on('click', function (e) {
							e.preventDefault();
							if (activeAlarm.protected) return alert('@Messages.get("actions.alerts.alarm_protected")');
							if (activeAlarm.isFollowup()) return alert('@Messages.get("actions.alerts.alarm_followup")');
							Patient.populatePatient(outputPatient);
						});

						// At this point everything was successful, so we can close the modal
						$('#add_patient_modal').modal('hide');
					}
				});
			}

			// Geolocate the address, and save on success
			MapView.convertAddressToLatLng(address, function (locationData) {
				if (DEBUG) console.log("Received GeoLoc data: " + JSON.stringify(locationData, null, 4));
				if (locationData === null) {
					alert('@Messages.get("actions.alerts.location_failed")');
					$('#modalInputPatientAddress').parent().addClass('has-error');
				} else {
					inputPatient.latitude = locationData.latitude;
					inputPatient.longitude = locationData.longitude;
					$('#modalInputPatientAddress').parent().removeClass('has-error');
					savePatient()
				}
			});

		},

		fillUnknownPatient: function () {
			var pat = {
				id: 1,
				name: '@Messages.get("patientpane.pill.unknown")',
				address: '',
				latitude: 0.0,
				longitude: 0.0,
				personalNumber: '',
				phoneNumber: '',
				age: '',
				obs: null
			}
			populatePatientInformation(pat);
		}
	}
})(jQuery)
