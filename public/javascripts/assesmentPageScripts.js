

function setupAssesmentPage(){
	
	
	// add actions on check buttons
	$("#nmiTab").find(':checkbox').each(
		function(i) { 
			// set the action to show the label
			$(this).change(function(){
				var selectedLabel = $("#" + $(this).attr('id') + "label");
			    if(this.checked) {
			    	selectedLabel.show();
			    }else{
			    	selectedLabel.hide();
			    }
				
			});
		}
	);
	
	
	// add log actions to tabs
	$("#infoTablink").click(function () {
		$("#informationSensorlabel").show();
	});
	
	$("#sensorTablink").click(function () {
		$("#assedmentSensorlabel").show();
	});
	
	
    $("#closeCaseFromAssessButton").click(closeCaseAtAssesment);
    $("#goToClosingButton").click(fromAssementToClosing);

    
    resetAssesmentPage();
}

function resetAssesmentPage(){
	

	$("#nmiTab").find(':checkbox').each(
		function(i) { 
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
}


function closeCaseAtAssesment(){

	// TODO: retrieve and save logs
    var notes = $('#assesmentNotesBox').val();
    var alarmId = $('#assignedAlarmList').find('.list-group-item.active').attr("idNum");

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
    var alarmId = $('#assignedAlarmList').find('.list-group-item.active').attr("idNum");

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
        	    // TODO: implement notes part
            }// end of success
    });// end of ajax call
}
