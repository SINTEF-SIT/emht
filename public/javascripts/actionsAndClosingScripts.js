function setupActionsAndClosingPage(){
	

	$(".dispatch-ring-btn").click(function() {
		  BootstrapDialog.show({
	            title: '',
	            message: 'calling'
	        });
	});
	$(".dispatch-send-btn").click(function() {
		  BootstrapDialog.show({
	            title: '',
	            message: 'sending incident data'
	        });
	});
	
	
    $("#closeCaseActionsButton").click(closeCaseAtClosing);
    $("#saveAndFollowUpButton").click(closeCaseAtClosing);// TODO: updat so it really follows up
	
	// make sure that no phone is expanded
    $("#dynamicDispatchButtons").find(".in").removeClass(".in");
}



function closeCaseAtClosing(){
	
	// update to get meaningfull data

    var alarmId = $('#assignedAlarmList').find('.list-group-item.active').attr("idNum");
   
    var updatedAlarm = {
            'alarmId' : alarmId,
	    };

	myJsRoutes.controllers.Application.closeCase().ajax({
            data : JSON.stringify(updatedAlarm),
            contentType : 'application/json',
            success : function (data) {
            	highlightBackListTab ();
            }// end of success
    });// end of ajax call

}

