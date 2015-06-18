@import play.i18n._

var Patient = (function ($) {
	/* Private fields */


	/* Private methods */


	/* Public methods inside return object */
	return {
		init: function () {
			$('#addPatientModalButton').click(function() {
				Patient.addNewPatientFromModal();
			});
			$("#closeCaseFromPatientRegButton").click(Patient.closeCaseAtRegistration);
			$("#goToAssesmentButton").click(Patient.fromRegistrationToAssesment);
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
					//$("#calleeBox").show();
				}
			);
		},

		retrievePatientsByAddress: function (alarmIndex) {
			if ( $('#dynamicPatientInfo').length == 1){ //check if we have the dynamic data
				$.getJSON("/prospectPatient/" + alarmIndex,
					function (data) { Patient.createPatientDiv(data) }
				);
			}
		},

		createPatientDiv: function (data) {
			// function to be called by the json function retrieving the prospect patients
			// TODO: check if the json is full before creating the table
			$("#dynamicPatientInfo").empty();
			var dynamicPatientBlock = '<u>@Messages.get("patientpane.name"):</u> ';

			// building Patient Drop Down Block
			var patientDropDownBox = '<span class="btn-group"  id="patientDropDown" ><button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">' +
				'<span class="selection">@Messages.get("patientpane.pill.patient")</span><span class="caret"></span></button><ul id="patientDropDownList" class="dropdown-menu">';
			var array = data.patientArray;
			for (var i in array) {
				var patientId = array[i].id;
				var patientName = array[i].name;
				var patientPersoNum = array[i].persoNumber;
				var patientAddress = array[i].address;
				var patientAge = array[i].age;
				var patientPhoneNum = array[i].phoneNumber;
				var patientObs = array[i].obs;
				patientDropDownBox += '<li><a onclick="Patient.populatePatient(\'' + patientId + '\',\'' + patientName + '\',\'' + patientPersoNum + '\',\'' + patientAddress +
					'\',\'' + patientPhoneNum + '\',\'' + patientAge + '\',\'' + patientObs + '\');" href="#">' + patientName + '</a></li>';
			}
			if ($.isArray(array) && array.length != 0) {
				patientDropDownBox += '<li class="divider"></li>';
			}
			patientDropDownBox += '<li><a onclick="Patient.openAddPatientModal();" href="#">@Messages.get("patientpane.pill.other.patient")</a></li>'
			patientDropDownBox += '<li><a onclick="Patient.fillUnknownPatient();" href="#">@Messages.get("patientpane.pill.unknown")</a></li>'
			patientDropDownBox += '</ul></span>';

			// building Patient Details
			var patientDetails = '<u>@Messages.get("patientpane.address"):</u>  <span id="patientAddress"/><br><u>@Messages.get("patientpane.personumber"):</u>  <span id="patientPersoNum"/><br>' +
				'<u>@Messages.get("patientpane.phonenumber"):</u>  <span id="patientPhoneNum"/><br>' +
				'<u>@Messages.get("patientpane.age"):</u>  <span id="patientAge"/><br><input id="patientId" type="hidden"><p><p><u>@Messages.get("patientpane.incident.location"):</u>';
			patientDetails += '<span class="checkbox inline"><label><input id="sameAddressCheckbox" type="checkbox"> @Messages.get("patientpane.incident.same")</label>' +
				'</span>'; // adds checkbox
			patientDetails += '<div class="input-group">';
			patientDetails += '<input type="text" class="form-control" id="incidentAddress" placeholder="@Messages.get("patientpane.incident.other")">';
			patientDetails += '<span class="input-group-btn"><button class="btn btn-default" id="verifyPatientLocation">@Messages.get("patientpane.incident.checkaddress")</button></span></div>';

			patientDetails += '<h5>@Messages.get("patientpane.log.title")</h5><table class="table table-bordered" id="patientLogTable"><thead><tr><td>@Messages.get("patientpane.log.date")</td><td>@Messages.get("patientpane.log.hour")</td><td>@Messages.get("patientpane.log.type")</td></tr></thead><tbody></tbody></table>';

			dynamicPatientBlock += patientDropDownBox + '<br>' + patientDetails + '<br>';// + identity;


			$("#dynamicPatientInfo").html(dynamicPatientBlock);

			$("#sameAddressCheckbox").click(function () {
				var addr = $("#patientAddress").text();
				if (this.checked) {
					$("#incidentAddress").val(addr);
				} else {
					$('#incidentAddress').val('@Messages.get("patientpane.incident.other")');
				}
			});

			var currentSelected = $('.list-group-item.active.alarmItem');

			$('#verifyPatientLocation').on('click', function (e) {
				e.preventDefault();
				var address = $('#incidentAddress').val();
				MapView.convertAddressToLatLng(address, function (locationData) {
					console.log(locationData);
					if (locationData === null) {
						alert('@Messages.get("patientpane.incident.checkaddress.fail"): ' + address);
					} else {
						myJsRoutes.controllers.Application.setLocationOfAlarm(currentSelected.attr('idnum')).ajax({
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

			//$("#patientBox").show();

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
			}
		},

		populatePatient: function (patientId, patientName, personNumber, address, phoneNumber, age, obs) {
			$('#patientAddress').text(address);

			// for the personNumber, if it is more than 6 digits, we add a space after the first 6 digits
			var formattedPersoNumber;
			if (personNumber.length > 6) {
				formattedPersoNumber = personNumber.substring(0, 6) + " " + personNumber.substring(6);
			} else {
				formattedPersoNumber = personNumber;
			}

			$('#patientPersoNum').text(formattedPersoNumber);
			$('#patientPhoneNum').text(phoneNumber);
			$('#patientAddress').text(address);
			$('#patientAge').text(age);
			$('#patientId').val(patientId);
			Assessment.loadPatientSensor(patientId);

			// set the obs field in the assesment page
			if (obs != null) $('#obsBody').text(obs);

			//if it is an alarm of type: fire, safety_alarm or fall, I've set obs to the front
			var currentAlarm_type = $('.list-group-item.active.alarmItem').find('.type-icon').attr('data-type');
			if (currentAlarm_type == "fall" || currentAlarm_type == "fire" || currentAlarm_type == "safety_alarm"){
				$('#assesmentTabHeader a[href="#infoTab"]').tab('show');
				$("#informationSensorlabel").show();
			}


			// empty existing table
			$("#patientLogTable > tbody").html("");
			// destroying the datatable
			//var table = $('#patientLogTable').DataTable();
			//table.destroy();

			if (0 != patientId) {
				$.getJSON("/pastAlarmsFromPatient/" + patientId,
					function(data) {
						if (null!= data && null != data.alarmArray) {
							// data is a JSON list, so we can iterate over it
							var array = data.alarmArray;
							for(var i in array) {
								var day = array[i].day;
								var hour = array[i].hour;
								var type = array[i].type;
								var notes = array[i].notes;
								htmlRow= '<tr data-toggle="tooltip" data-placement="right" title="@Messages.get("actions.popup.send.notes"): ' +notes+'"><td> ' + day + ' </td><td> ' + hour + ' </td><td> ' + type + ' </td></tr>';
								$("#patientLogTable > tbody").prepend(htmlRow);
							}
							$('[data-toggle="tooltip"]').tooltip({'placement': 'right'});
						}

						// make it a datatable with pagination, TODO: investigate further if we want to commit the
						// datatable code
						/*$('#patientLogTable').DataTable( {
						 "paging": true,
						 "searching": false,
						 "ordering":  false,
						 "pageLength": 5,
						 "destroy": true,
						 "lengthChange": false
						 } );*/
					}
				);
			}

			$('#patientDropDown').find('.selection').text(patientName);
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
				'persoNumber' : number,
				'phoneNumber' : phoneNumber,
				'age' : age
			};
			myJsRoutes.controllers.Application.insertPatientFromJson().ajax({
				data : JSON.stringify(inputPatient),
				contentType : 'application/json',
				success : function (outpuPatient) {
					// add it to list
					var patientListItem =  '<li><a onclick="Patient.populatePatient(\'' + outpuPatient.id + '\',\'' + outpuPatient.name + '\',\'' + outpuPatient.persoNumber + '\',\'' + outpuPatient.address +
						'\',\'' + outpuPatient.phoneNumber +'\',\'' + outpuPatient.age + '\'\,\'\');" href="#">' + outpuPatient.name +'</a></li>'
					$('#patientDropDownList').prepend(patientListItem);
					Patient.populatePatient(outpuPatient.id,outpuPatient.name,outpuPatient.persoNumber,outpuPatient.address,outpuPatient.phoneNumber,outpuPatient.age,'');

				}// end of success
			});// end of ajax call
		},

		fillUnknownPatient: function () {
			Patient.populatePatient('','@Messages.get("patientpane.pill.unknown")','','','','','');
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
