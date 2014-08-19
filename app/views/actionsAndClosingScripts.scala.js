@import play.i18n._

function organizationTableClick(){
	$(this).closest('table').find('tr.active').removeClass('active');
	$(this).addClass('active');
}


function setupActionsAndClosingPage(){
	

	$(".dispatch-ring-btn").click(function() {
		$('#calling_modal').modal("show");
	});
	$(".dispatch-send-btn").click(function() {
		
		//clear modal
		$("#dispatch_data_modal").find(':checkbox').each(
			function() { 
				$(this).prop("checked", "checked");
			}
		);
		
		
		// show modal
		$('#dispatch_data_modal').modal("show");
	});

	// setting up schedule time modal
	
	$(".schedule-btn").click(function() {
		
		//clear modal
		$("#schedule_time_modal").find(':checkbox').each(
			function() { 
				$(this).prop("checked", "checked");
			}
		);
		
		
		// show modal
		$('#schedule_time_modal').modal("show");
	});
	
	$("#schedule-time-picker").datetimepicker();
	$("#schedule_time_modal_btn").click(closeCaseAtClosing);
	// end of setting up schedule time modal
	
	
    $("#closeCaseActionsButton").click(closeCaseAtClosing);
    $("#saveAndFollowUpButton").click(saveAndFollowupAtClosing);
	
    // setting up organization tables so that the rows can be selected
    
    $("#kontaktpersonerTable").on("click","tr",organizationTableClick);
    $("#ambulerendeTable").on("click","tr",organizationTableClick);
    $("#legevaktTable").on("click","tr",organizationTableClick);
    $("#tpTable").on("click","tr",organizationTableClick);
    $("#hjemmesykepleienTable").on("click","tr",organizationTableClick);
    
    
    
    
    
    
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

// called when one of the SEND buttons of the modals
// is called
function actionsDataSent(){
    $('#send_confirmation_modal').modal();
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
