@import play.i18n._

var Assessment = (function ($) {
	/* Private fields */

	// We keep an internal state of the active assessment
	var currentAssessment = null;

	/* Private methods here */

	// nmi constructor
	var nmi = function (id, conscious, breathing, movement, standing, talking) {
		this.id = id;
		this.conscious = conscious;
		this.breathing = breathing;
		this.movement = movement;
		this.standing = standing;
		this.talking = talking;
	};

	// assessmentInfo constructor
	var assessmentInfo = function (id, nmiAssessment, sensors, patientInfo) {
		this.id = id;
		this.nmi = nmiAssessment;
		this.sensorsChecked = sensors;
		this.patientInformationChecked = patientInfo;
	};

	// Helper method that returns the assessment object as a newline separated string.
	var serializeAssessment = function (assessmentInfo) {
		var assessmentString = '';
		if (assessmentInfo.nmi.conscious !== null) {
			if (assessmentInfo.nmi.conscious) {
				assessmentString += '@Messages.get("assessment.log.affirmation.conscious").\n';
			} else {
				assessmentString += '@Messages.get("assessment.log.negation.conscious").\n';
			}
		}
		if (assessmentInfo.nmi.breathing !== null) {
			if (assessmentInfo.nmi.breathing) {
				assessmentString += '@Messages.get("assessment.log.affirmation.breathing").\n';
			} else {
				assessmentString += '@Messages.get("assessment.log.negation.breathing").\n';
			}
		}
		if (assessmentInfo.nmi.movement !== null) {
			if (assessmentInfo.nmi.movement) {
				assessmentString += '@Messages.get("assessment.log.affirmation.move").\n';
			} else {
				assessmentString += '@Messages.get("assessment.log.negation.move").\n';
			}
		}
		if (assessmentInfo.nmi.standing !== null) {
			if (assessmentInfo.nmi.standing) {
				assessmentString += '@Messages.get("assessment.log.affirmation.standup").\n';
			} else {
				assessmentString += '@Messages.get("assessment.log.negation.standup").\n';
			}
		}
		if (assessmentInfo.nmi.talking !== null) {
			if (assessmentInfo.nmi.talking) {
				assessmentString += '@Messages.get("assessment.log.affirmation.talk").\n';
			} else {
				assessmentString += '@Messages.get("assessment.log.negation.talk").\n';
			}
		}
		if (assessmentInfo.sensorsChecked) assessmentString += '@Messages.get("assessment.log.sensor").\n';
		if (assessmentInfo.patientInformationChecked) assessmentString += '@Messages.get("assessment.log.obs").\n';

		return assessmentString;
	};

	var updateAssessmentInfo = function (radio) {
		var radioId = $(radio).attr('id');
		var field = radioId.substring(0, radioId.length - 1);
		var state = radioId.charAt(radioId.length - 1) === 'Y';
		switch (field) {
			case "NMIcheckBox1":
				currentAssessment.nmi.conscious = state;
				break;
			case "NMIcheckBox2":
				currentAssessment.nmi.breathing = state;
				break;
			case "NMIcheckBox3":
				currentAssessment.nmi.movement = state;
				break;
			case "NMIcheckBox4":
				currentAssessment.nmi.standing = state;
				break;
			case "NMIcheckBox5":
				currentAssessment.nmi.talking = state;
				break;
		}
	};

	// Helper method that sets the state of radio buttons to match that of the currentAssessment
	var updateDOM = function () {
		if (currentAssessment.nmi.conscious !== null) {
			if (currentAssessment.nmi.conscious) {
				$('#NMIcheckBox1Y').attr('checked', true);
				$('#NMIcheckBox1Ylabel').show();
			} else {
				$('#NMIcheckBox1N').attr('checked', true);
				$('#NMIcheckBox1Nlabel').show();
			}
		}
		if (currentAssessment.nmi.breathing !== null) {
			if (currentAssessment.nmi.breathing) {
				$('#NMIcheckBox2Y').attr('checked', true);
				$('#NMIcheckBox2Ylabel').show();
			} else {
				$('#NMIcheckBox2N').attr('checked', true);
				$('#NMIcheckBox2Nlabel').show();
			}
		}
		if (currentAssessment.nmi.movement !== null) {
			if (currentAssessment.nmi.movement) {
				$('#NMIcheckBox3Y').attr('checked', true);
				$('#NMIcheckBox3Ylabel').show();
			} else {
				$('#NMIcheckBox3N').attr('checked', true);
				$('#NMIcheckBox3Nlabel').show();
			}
		}
		if (currentAssessment.nmi.standing !== null) {
			if (currentAssessment.nmi.standing) {
				$('#NMIcheckBox4Y').attr('checked', true);
				$('#NMIcheckBox4Ylabel').show();
			} else {
				$('#NMIcheckBox4N').attr('checked', true);
				$('#NMIcheckBox4Nlabel').show();
			}
		}
		if (currentAssessment.nmi.talking !== null) {
			if (currentAssessment.nmi.talking) {
				$('#NMIcheckBox5Y').attr('checked', true);
				$('#NMIcheckBox5Ylabel').show();
			} else {
				$('#NMIcheckBox5N').attr('checked', true);
				$('#NMIcheckBox5Nlabel').show();
			}
		}
		if (currentAssessment.sensorsChecked) {
			$("#informationSensorlabel").show();
		}
		if (currentAssessment.patientInformationChecked) {
			$("#assedmentSensorlabel").show();
		}
	};

	var removeImageFromSensorTab = function () {
		$(".assesment-graph").remove();
		$("#ampliphied-graph").attr("src", "");
	};

	/* Public methods inside the return object */
	return {
		init: function () {
			Assessment.reset();

			// add actions on check buttons
			$("#nmiTab").find(':radio').each(function (i) {
				// set the action to show the label
				$(this).change(function () {
					var radioId = $(this).attr('id');
					var opositeRadioId = radioId.substring(0, radioId.length -1);

					if (radioId.charAt(radioId.length -1) == "Y") {
						opositeRadioId += "N";
					} else {
						opositeRadioId += "Y";
					}

					var selectedLabel = $("#" + radioId + "label");
					var unSelectedLabel = $("#" + opositeRadioId + "label");

					if (this.checked) {
						selectedLabel.show();
						unSelectedLabel.hide();
					} else {
						selectedLabel.hide();
						unSelectedLabel.show();
					}
					// Pass the clicked radio button to the state update parser
					updateAssessmentInfo(this);
				});
			});

			// Add log actions to tabs
			$("#infoTablink").click(function () {
				currentAssessment.patientInformationChecked = true;
				$("#informationSensorlabel").show();
			});

			$("#sensorTablink").click(function () {
				currentAssessment.sensorsChecked = true;
				$("#assedmentSensorlabel").show();
			});

			// $("#closeCaseFromAssessButton").click(closeCaseAtAssesment);
			// $("#goToClosingButton").click(fromAssementToClosing);
		},

		reset: function () {
			$("#nmiTab").find(':radio').each(
				function (i) {
					// clear all checkbockes
					$(this).removeAttr('checked');
				}
			);

			// hide all log labels
			$("#assesmentLogPanel").children().each(
				function(i) {
					$(this).hide();
				}
			);

			//set active tab to NMI
			$("#nmiTab").addClass("active");
			$("#infoTab").removeClass("active");
			$("#sensorTab").removeClass("active");

			$("#nmiNav").addClass("active");
			$("#infoNav").removeClass("active");
			$("#sensorNav").removeClass("active");

			// Reset the current assessment object
			currentAssessment = new assessmentInfo(null, new nmi(null, null, null, null, null, null), false, false);
		},

		showGraphModal: function () {
			$('#graph_modal').modal("show");
		},

		loadPatientSensor: function (patientId) {
			// TODO retrieve NMI (but in another function)
			removeImageFromSensorTab();
			if(0 != patientId){
				var image = new Image();
				image.src = "/assets/images/patient/" + patientId + ".png" ;
				image.className = "img-responsive assesment-graph";
				image.onerror = removeImageFromSensorTab;
				image.onclick = Assessment.showGraphModal;
				//if (image.width != 0) {
				$("#sensorTab").append(image);
				$("#ampliphied-graph").attr("src", image.src);
				//}
			}
		},

		getAssessmentLog: function () {
			return currentAssessment;
		},

		pupulateDOMfromAssessment: function (assessmentInfo) {
			Assessment.reset();
			currentAssessment = assessmentInfo;
			updateDOM();
			console.log(currentAssessment);
		}
	}
})(jQuery);



/*
 function closeCaseAtAssesment(){

 // TODO: retrieve and save logs
 var notes = $('#assesmentNotesBox').val();
 var alarmId = $('#assignedAlarmList').find('.list-group-item.active').attr("idnum");

 var updatedAlarm = {
 'alarmId' : alarmId,
 'notes' : notes,
 };

 myJsRoutes.controllers.Application.closeCase().ajax({
 data : JSON.stringify(updatedAlarm),
 contentType : 'application/json',
 success : function (data) {
 removeHighlightedAlarmFromList();
 highlightBackListTab ();
 }// end of success
 });// end of ajax call

 }

 function fromAssementToClosing(){

 // TODO: retrieve and save logs
 var notes = $('#assesmentNotesBox').val();
 var alarmId = $('#assignedAlarmList').find('.list-group-item.active').attr("idnum");

 var updatedAlarm = {
 'alarmId' : alarmId,
 'notes' : notes,
 };

 myJsRoutes.controllers.Application.saveCase().ajax({
 data : JSON.stringify(updatedAlarm),
 contentType : 'application/json',
 success : function (data) {
 // TODO: possibly move some of this to a function
 highlightArrowHeader("closingArrowHeader");
 $("#assesment").show();
 $('#assementNotesDiv').hide();
 $("#extraActionButtonsDiv").show();
 $("#closingNotesAndButtons").show();
 $('#closingNotesBox').val($('#assesmentNotesBox').val());
 }// end of success
 });// end of ajax call
 }
 */
