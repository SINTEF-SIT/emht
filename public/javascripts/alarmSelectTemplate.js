
function assignAlarm(alarmIndex){
       var attendant = "Karin";
	   var assignAlarmReq = {
	            'attendant' : attendant,
	            'alarmId' : alarmIndex
	          };
		myJsRoutes.controllers.Application.assignAlarmFromJson().ajax({
	            data : JSON.stringify(assignAlarmReq),
	            contentType : 'application/json',
	            success : function (patient) {
	            	
	              // unhighlight any highlighted alarm
	              var currentSelected = $('.list-group-item.active.alarmItem');
	              currentSelected.toggleClass("active");	
	            	
	              // remove from unassgined list
	              var openAlarmListItem =  $('#Alarm' + alarmIndex).clone();
	              $('#Alarm' + alarmIndex).remove();
	              
	              // customize and move into assigned list
	              openAlarmListItem.onclick = "selectMyAlarm(" + alarmIndex + ");return false;";
	              openAlarmListItem.addClass("active" );
	              // TODO: check if there is a clock element before removing
	              var clock = openAlarmListItem.children('.clock-icon');
	              if(clock != null)
	            	  clock.remove();
	              $('#assignedAlarmList').prepend(openAlarmListItem);
	            }// end of success
	    });// end of ajax call
		

		highlightArrowHeader("registrationArrowHeader");
		populateAlarmDetails(alarmIndex);
    return;
}

function populateAlarmDetails(alarmIndex){
    
	populateCalleFromAlarm(alarmIndex);
	retrivePatientsByAddress(alarmIndex);
	$('#notesDiv').show();
	
 return;
}

// takes as input the id of the header to be highlighted and both highlight it and un-highlight the others
function highlightArrowHeader(headerId){
	var currentSelected = $('.selected_arrow_box');
	//var len = currentSelected.length;
	//for(var i = 0; i < len; i++) {
	currentSelected.switchClass("selected_arrow_box", "nonselected_arrow_box" );
	//}
	$('#'+ headerId).switchClass("nonselected_arrow_box", "selected_arrow_box" );
}

function highlightBackListTab (){
	highlightArrowHeader("receptionArrowHeader");
	var currentSelected = $('.list-group-item.active.alarmItem');
	currentSelected.remove();
   $("#patientBox").hide();
   $("#calleeBox").hide();
    $('#notesDiv').hide();
    $('#assesment').hide();
    $('#extraActionButtonsDiv').hide();
    $('#patientRegistrationNotesBox').val('');
    // TODO: perhaps replace the below for just what is really needed (as done for the actions and closing)
    setupAssesmentPage();
    resetActionsAndClosingPage();
    
}

