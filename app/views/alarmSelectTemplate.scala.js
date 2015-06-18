@import play.i18n._

var Alarms = (function ($) {
	/* Private fields here */
	var SELECTED_ALARM = null;

	/* Private helper methods here */

	var handleModalCancel = function () {
		$("#callee_info_modal").on('hide.bs.modal', function () {
			// unhighlight any highlighted alarm
			var currentSelected = $('.list-group-item.active.alarmItem');
			currentSelected.toggleClass("active");
		});
	}

	var handleResetAlarmCount = function () {
		var unassignedAlarmCount = $("#unassignedAlarmList .list-group-item").length;
		var assignedAlarmCount = $("#assignedAlarmList .list-group-item").length;
		var followUpAlarmCount = $("#followupAlarmList .list-group-item").length;
		$("#nbOfUnassignedAlarm").text(unassignedAlarmCount);
		$("#nbOfAssignedAlarm").text(assignedAlarmCount);
		$("#nbOfFollowUpAlarm").text(followUpAlarmCount);
	}

	/* Public methods inside return object */
	return {
		init: function () {
			$("#assesment").show();
			$("#patientBox").show();
			$("#calleeBox").show();
			$("#extraActionButtonsDiv").show();
			$("#closingNotesAndButtons").show();

			// those setup functions should be called just once as they may be binding buttons
			// and one does not want to rebind
			WebSocketManager.init();
			Patient.init();
			Assessment.init();
			Actions.init();

			// if the modal is canceled, we clear the active item
			handleModalCancel();

			Alarms.gui.resetAlarmCount();
		},

		gui: {
			resetAlarmCount: function () {
				handleResetAlarmCount();
			},

			populateAlarmDetails: function (alarmIndex) {
				Patient.populateCalleeFromAlarm(alarmIndex);
				Patient.retrievePatientsByAddress(alarmIndex);
			},

			removeHighlightedAlarmFromList: function () {
				var currentSelected = $('.list-group-item.active.alarmItem');
				currentSelected.remove();
				Alarms.gui.resetAlarmCount();
			},

			//this function will move back the incident from the My Incidents list to the follow-up lists
			// or just clear it back in case the incident is already on the follow-up list
			moveAlarmToFollowUpList: function () {
				var currentSelected = $('.list-group-item.active.alarmItem');
				var alarmIndex = currentSelected.attr("idnum");
				currentSelected.toggleClass("active");

				if (currentSelected.parent().attr('id') == "assignedAlarmList") {

					// remove from assgined list
					var openFollowUpListItem =  $('#Alarm' + alarmIndex).clone();
					$('#Alarm' + alarmIndex).remove();

					// customize and move into followup list
					openFollowUpListItem.removeAttr('onclick');
					openFollowUpListItem.attr("onclick","Alarms.gui.selectFollowUpAlarm(" + alarmIndex + ");return false;");

					$('#followupAlarmList').append(openFollowUpListItem);
					Alarms.gui.resetAlarmCount();
				}
				// else means it is on the followup list. In that case I do nothing, because I have already removed the focus of
				// the element and the function calling this one is already showing back the incident list
			},

			selectFollowUpAlarm: function (alarmIndex) {
				// start by clearing the view
				//highlightBackListTab ();

				// unhighlight any highlighted alarm
				var currentSelected = $('.list-group-item.active.alarmItem');
				currentSelected.toggleClass("active");

				var currentSelected = $('#Alarm' + alarmIndex);
				currentSelected.toggleClass("active");
				SELECTED_ALARM = alarmIndex;

				// Remove recurring icon
				var recurring = currentSelected.children('.recurring-icon');
				if (recurring != null) recurring.remove();

				Patient.populateCalleeFromAlarm(alarmIndex);
				// TODO: there is currently a bug in the sense that in case an alarm was set to followup with an
				// unknown patient, it will be loaded here with a person as a patient

				// TODO: and in case there was no patient assigned, it will select the "Add patient" option that will ask for adding a patient
				$.getJSON("/prospectPatient/" + alarmIndex,
					function (data) {
						Patient.createPatientDiv(data);
						var patientListItem = $("#patientDropDownList li:first a");
						patientListItem.click();
						Assessment.loadPatientSensor();

						// populate notebox
						$.getJSON("/alarm/" + alarmIndex,
							function (data) {
								// TODO: check if the json is full before populating the DOM
								var notes = data.notes;
								var occuranceAddress = data.occuranceAddress;
								$("#globalNotesBox").val(notes);
								$("#incidentAddress").val(occuranceAddress);
							}
						);
						// end of populate notebox
					}
				);

				//$("#assesment").show();
				//$("#assesmentNotesDiv").hide();
				//$("#extraActionButtonsDiv").show();
				//$("#closingNotesAndButtons").show();
				//highlightArrowHeader("closingArrowHeader");
			},

			selectOpenAlarm: function (alarmIndex, calleeIndex) {
				// highlight the alarm
				var currentSelected = $('#Alarm' + alarmIndex);
				currentSelected.toggleClass("active");

				$.getJSON("/pastAlarmsFromCalle/" + calleeIndex,
					function(data) {
						// TODO: check if the json is full before creating the table
						$("#calleeLogTableDiv").empty();
						var htmlTable = '<table class="table" id="pastCalleeAlarmsTable"><thead><tr><td>@Messages.get("handling.popup.date")</td><td>@Messages.get("handling.popup.hour")</td><td>@Messages.get("handling.popup.type")</td></tr></thead><tbody>';
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
						// make it a datatable with pagination
						/*$('#pastCalleeAlarmsTable').DataTable( {
						 "paging": true,
						 "searching": false,
						 "ordering":  false,
						 "pageLength": 5,
						 "destroy": true,
						 "lengthChange": false
						 } );*/
					});

				//populateAlarmInfo(alarmIndex);
				$('#callee_info_modal').modal("show");
				$('#confirmCalleeModalButton').unbind("click").click(function (e) {
					Alarms.assign(alarmIndex);
				});
			},

			selectMyAlarm: function (alarmIndex) {
				// TEMPORARY CODE
				Alarms.assign(alarmIndex); // TODO: change this to a simple select of alarm instead of assign
				SELECTED_ALARM = alarmIndex;
			},

			clearUpData: function () {
				SELECTED_ALARM = null;
				Patient.clearUpCalleeData();
				Patient.clearUpPatientData();
				Assessment.reset();
				Actions.reset();
				$("#globalNotesBox").val("");
			},

			getCurrentSelectedAlarmIndex: function () {
				return SELECTED_ALARM;
			}
		},

		assign: function (alarmIndex) {
			// Start by clearing the view
			Alarms.gui.clearUpData();

			var assignAlarmReq = {
				'alarmId' : alarmIndex
			};

			myJsRoutes.controllers.Application.assignAlarmFromJson().ajax({
				data : JSON.stringify(assignAlarmReq),
				contentType : 'application/json',
				success : function (data) {
					// unhighlight any highlighted alarm
					var currentSelected = $('.list-group-item.active.alarmItem');
					currentSelected.toggleClass("active");

					// remove from unassgined list
					var openAlarmListItem =  $('#Alarm' + alarmIndex).clone();
					$('#Alarm' + alarmIndex).remove();

					// customize and move into assigned list
					openAlarmListItem.removeAttr('onclick');
					openAlarmListItem.attr("onclick", "Alarms.gui.selectMyAlarm(" + alarmIndex + ");return false;");
					openAlarmListItem.addClass("active" );
					// remove clock icon
					var clock = openAlarmListItem.children('.clock-icon');
					if (clock != null) clock.remove();
					$('#assignedAlarmList').prepend(openAlarmListItem);
					SELECTED_ALARM = alarmIndex;

					Alarms.gui.resetAlarmCount();
				}// end of success
			});// end of ajax call

			Alarms.gui.populateAlarmDetails(alarmIndex);
		},


	}
})(jQuery);

// Fire up the module on DOM ready
$(document).ready(function () {
	Alarms.init();
});






// takes as input the id of the header to be highlighted and both highlight it and un-highlight the others
/*function highlightArrowHeader(headerId){
 var currentSelected = $('.selected_arrow_box');

 currentSelected.switchClass("selected_arrow_box", "nonselected_arrow_box" );
 $('#'+ headerId).switchClass("nonselected_arrow_box", "selected_arrow_box" );
 }*/


/* function highlightBackListTab (){
 //highlightArrowHeader("receptionArrowHeader");

 $("#patientBox").hide();
 $("#calleeBox").hide();
 $('#notesDiv').hide();
 $('#assesment').hide();
 $('#extraActionButtonsDiv').hide();
 $("#closingNotesAndButtons").hide();

 resetAssesmentPage();
 resetActionsAndClosingPage();

 }*/