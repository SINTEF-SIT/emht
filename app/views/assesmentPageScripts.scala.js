@import play.i18n._

function setupAssesmentPage(){
	
	
	// add actions on check buttons
	$("#nmiTab").find(':radio').each(
		function(i) { 
			// set the action to show the label
			$(this).change(function(){
				var radioId = $(this).attr('id');
				var opositeRadioId = radioId.substring(0, radioId.length -1);
				if(radioId.charAt(radioId.length -1) == "Y"){
					opositeRadioId += "N";
				}else {
					opositeRadioId += "Y";
				}
				var selectedLabel = $("#" + radioId + "label");
				var unSelectedLabel = $("#" + opositeRadioId + "label");
			    if(this.checked) {
			    	selectedLabel.show();
			    	unSelectedLabel.hide();
			    }else{
			    	selectedLabel.hide();
			    	unSelectedLabel.show();
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



function loadPatientSensor(){
	
	// TODO retrieve NMI (but in another funtion)
	removeImageFromSensorTab()
	var patId = $('#patientId').val();
	if(0 != patId){
		var image = new Image(); 
		image.src = "/assets/images/patient/" + patId + ".png" ;
		image.className = "img-responsive assesment-graph";
		image.onerror = removeImageFromSensorTab;
		//if (image.width != 0) {
			$("#sensorTab").append(image);
		//}
	}
	
	// TODO: remove picture when IM resetting the page
}

function removeImageFromSensorTab(){
	$(".assesment-graph").remove();
}


function resetAssesmentPage(){
	

	$("#nmiTab").find(':radio').each(
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
