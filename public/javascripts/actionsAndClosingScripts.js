function setupActionsAndClosingPage(){
	

	$(".dispatch-ring-btn").click(function() {
		$('#calling_modal').modal("show");
	});
	$(".dispatch-send-btn").click(function() {
		
		//clear modal
		$("#dispatch_data_modal").find(':checkbox').each(
			function() { 
				$(this).removeAttr('checked');
			}
		);
		
		
		// show modal
		$('#dispatch_data_modal').modal("show");
	});
	
	
    $("#closeCaseActionsButton").click(closeCaseAtClosing);
    $("#saveAndFollowUpButton").click(saveAndFollowupAtClosing);
	
    
    resetActionsAndClosingPage();
	// make sure that no phone is expanded

}

function resetActionsAndClosingPage(){
	
    $("#dynamicDispatchButtons").find(".in").removeClass("in");
}


function closeCaseAtClosing(){
	
	// update to get meaningfull data

    var alarmId = $('#allListsSection').find('.list-group-item.active').attr("idnum");
	var notes = $('#closingNotesBox').val();
   
    var updatedAlarm = {
            'alarmId' : alarmId,
            'notes' : notes
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

function saveAndFollowupAtClosing(){
	
	// update to get meaningfull data

    var alarmId = $('#allListsSection').find('.list-group-item.active').attr("idnum");
	var notes = $('#closingNotesBox').val();
   
    var updatedAlarm = {
            'alarmId' : alarmId,
            'notes' : notes
	    };

	myJsRoutes.controllers.Application.saveAndFollowupCase().ajax({
            data : JSON.stringify(updatedAlarm),
            contentType : 'application/json',
            success : function (data) {
            	moveAlarmToFollowUpList();
            	highlightBackListTab ();
            }// end of success
    });// end of ajax call

}
