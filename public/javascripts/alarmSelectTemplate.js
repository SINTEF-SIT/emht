function initApplication(){
	   $("#assesment").hide();
	   $("#patientBox").hide();
	   $("#calleeBox").hide();
	    $("#extraActionButtonsDiv").hide();
	    $("#closingNotesAndButtons").hide();

       $("#closeCaseFromPatientRegButton").click(closeCaseAtRegistration);
       $("#goToAssesmentButton").click(fromRegistrationToAssesment);

       
       // those setup functions should be called just once as they may be binding buttons
       // and one does not want to rebind
       setupPatientPage();
       setupAssesmentPage();
       setupActionsAndClosingPage();

       // if the modal is canceled, we clear the active item
       $("#callee_info_modal").on('hide.bs.modal', function () {
           // unhighlight any highlighted alarm
           var currentSelected = $('.list-group-item.active.alarmItem');
           currentSelected.toggleClass("active");	
    	});


	 }



function assignAlarm(alarmIndex){
	   
	   // start by clearing the view
	   highlightBackListTab ();
       
	   var attendant = "Karin";
	   var assignAlarmReq = {
	            'attendant' : attendant,
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
	              openAlarmListItem.attr("onclick","selectMyAlarm(" + alarmIndex + ");return false;");
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

	currentSelected.switchClass("selected_arrow_box", "nonselected_arrow_box" );
	$('#'+ headerId).switchClass("nonselected_arrow_box", "selected_arrow_box" );
}


function removeHighlightedAlarmFromList(){
	var currentSelected = $('.list-group-item.active.alarmItem');
	currentSelected.remove();
}

//this function will move back the incident from the My Incidents list to the follow-up lists
// or just clear it back in case the incident is already on the follow-up list
function moveAlarmToFollowUpList(){

	var currentSelected = $('.list-group-item.active.alarmItem');	
	var alarmIndex = currentSelected.attr("idnum");
	currentSelected.toggleClass("active");	
	
	if( currentSelected.parent().attr('id') == "assignedAlarmList" ){
		
	    // remove from assgined list
	    var openFollowUpListItem =  $('#Alarm' + alarmIndex).clone();
	    $('#Alarm' + alarmIndex).remove();
	    
	    // customize and move into followup list
	    openFollowUpListItem.removeAttr('onclick');
	    openFollowUpListItem.attr("onclick","selectFollowUpAlarm(" + alarmIndex + ");return false;");
	
	    $('#followupAlarmList').prepend(openFollowUpListItem);
	}
	// else means it is on the followup list. In that case I do nothing, because I have already removed the focus of
	// the element and the function calling this one is already showing back the incident list

	
	
}


function highlightBackListTab (){
	highlightArrowHeader("receptionArrowHeader");

   $("#patientBox").hide();
   $("#calleeBox").hide();
    $('#notesDiv').hide();
    $('#assesment').hide();
    $('#extraActionButtonsDiv').hide();
    $('#patientRegistrationNotesBox').val('');
    $('#patientRegistrationNotesBox').val('');
    $('#assesmentNotesBox').val('');
    $("#closingNotesAndButtons").hide();

    resetAssesmentPage();
    resetActionsAndClosingPage();
    
}

function selectFollowUpAlarm(alarmIndex) {
	
	   // start by clearing the view
	   highlightBackListTab ();
	   
       // unhighlight any highlighted alarm
       var currentSelected = $('.list-group-item.active.alarmItem');
       currentSelected.toggleClass("active");	
	
    var currentSelected = $('#Alarm' + alarmIndex);
    currentSelected.toggleClass("active");

	
	populateCalleFromAlarm(alarmIndex);
   $.getJSON("/prospectPatient/" + alarmIndex,
	   function (data){
	   createPatientDiv(data);
		var patientListItem = $("#patientDropDownList li:first a");
		patientListItem.click();
		loadPatientSensor();
	   });

	// populating notebox (TODO: in the future the log an others) from alarm data
				       
   $.getJSON("/alarm/" + alarmIndex,
    	   function (data){
		       // TODO: check if the json is full before populating the DOM		    	 
		       var notes = data.notes;

		       $("#closingNotesBox").text(notes);

    	   });


	   $("#assesment").show();
	   $("#assesmentNotesDiv").hide();
	   $("#extraActionButtonsDiv").show();
	   $("#closingNotesAndButtons").show();
	   highlightArrowHeader("closingArrowHeader");
 };

