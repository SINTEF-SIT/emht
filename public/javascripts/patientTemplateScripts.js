
function openAddPatientModal() {
	
	//clear modal
	$("#add_patient_modal").find(':text').each(
		function() { 
			$(this).val('');
		}
	);
	
    $('#add_patient_modal').modal("show");

            
 }

 
// just to be called on the startup in order not to rebind the button
function setupPatientPage() {
    $('#addPatientModalButton').click(function() {
    	addNewPatientFromModal();
     });

}
 
 /* retrieve the callee and populate it */
 function  populateCalleFromAlarm(alarmId){
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
                 $("#calleeBox").show();
                 

          });

     return;
 }
 

  /* retrieve possible patients based on the address */
 function retrivePatientsByAddress(alarmIndex) {

	 if( $('#dynamicPatientInfo').length == 1){ //check if we have the dynamic data
		  
			       $.getJSON("/prospectPatient/" + alarmIndex,
			    		   function (data){createPatientDiv(data)});

			    }
	 	return;
	 }
 
 
 	// function to be called by the json function retrieving the prospect patients
 	function createPatientDiv(data){
        // TODO: check if the json is full before creating the table
        $("#dynamicPatientInfo").empty();
        var dynamicPatientBlock = '<u>Name:</u> ';

        // building Patient Drop Down Block
        var patientDropDownBox = '<span class="btn-group"  id="patientDropDown" ><button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">' +
        '<span class="selection">Patient</span><span class="caret"></span></button><ul id="patientDropDownList" class="dropdown-menu">';
        var array = data.patientArray;
        for(var i in array){
          var patientId = array[i].id;
          var patientName = array[i].name;
          var patientPersoNum = array[i].persoNumber;
          var patientAddress = array[i].address;
          var patientAge = array[i].age;
          patientDropDownBox += '<li><a onclick="populatePatient(\'' + patientId + '\',\'' + patientName + '\',\'' + patientPersoNum + '\',\'' + patientAddress +
          '\',\'' + patientAge + '\');" href="#">' + patientName +'</a></li>'; 
        }
        if( $.isArray(array) && array.length != 0){
      	  patientDropDownBox += '<li class="divider"></li>';
        }
        patientDropDownBox += '<li><a onclick="openAddPatientModal();" href="#">Other Patient...</a></li>'
        patientDropDownBox += '<li><a onclick="fillUnknownPatient();" href="#">Unknown Patient</a></li>'
        patientDropDownBox += '</ul></span>';

        // building Patient Details
        var patientDetails = '<u>Adress:</u>  <span id="patientAddress"/><br><u>Personal Number:</u>  <span id="patientPersoNum"/><br>' +
        '<u>Age:</u>  <span id="patientAge"/><br><input id="patientId" type="hidden"><p><p><u>Patient Location:</u>';
        patientDetails+= '<span class="checkbox inline"><label><input id="sameAddressCheckbox" type="checkbox"> Same address as residence</label>' +
        '</span>'; // adds checkbox
        patientDetails+= '<input type="text" class="form-control" id="incidentAddress" placeholder="Other Address">';

        patientDetails+= '<h5>Patient Logs</h5><table class="table table-bordered" id="patientLogTable"><thead><tr><td>date</td><td>hour</td><td>type</td></tr></thead><tbody></tbody></table>';
        
        var identity = '<div class="checkbox"><label><input id="unknownIdentityCheckbox" type="checkbox"> Unknown Identity</label></div><br>';
        
        dynamicPatientBlock += patientDropDownBox + '<br>' + patientDetails + '<br>';// + identity;
        

        $("#dynamicPatientInfo").html(dynamicPatientBlock);
        
        $("#sameAddressCheckbox").click(function() {
      	    var addr = $("#patientAddress").text();
      	    if(this.checked){
      	    	$("#incidentAddress").val(addr);
      	    }else{
      	    	$("#incidentAddress").val("Other Address");
      	    }
      	});
        
        $("#patientBox").show();


 		
 	}
 
	function populatePatient(patientId,patientName,personNumber,address,age){
		$('#patientAddress').text(address);
		$('#patientPersoNum').text(personNumber);
		$('#patientAddress').text(address);
		$('#patientAge').text(age);
		$('#patientId').val(patientId);

		// empty existing table
		$("#patientLogTable > tbody").html("");
		
		if(0 != patientId){
	       $.getJSON("/pastAlarmsFromPatient/" + patientId,
	    		   function(data) {
	    	   		  if(null!= data && null != data.alarmArray){
		    	   		  // data is a JSON list, so we can iterate over it
			              var array = data.alarmArray;
			              for(var i in array){
							var day = array[i].day;
							var hour = array[i].hour;
							var type = array[i].type;
							htmlRow= '<tr><td> ' + day + ' </td><td> ' + hour + ' </td><td> ' + type + ' </td></tr>';
							$("#patientLogTable > tbody").prepend(htmlRow);
						  }
	       			}
	       });
		}
		

	    $('#patientDropDown').find('.selection').text(patientName);
	    //$('#patientDropDown').find('.selection').value(patientName);
	    return;
	}
 
	function addNewPatientFromModal(){
		var name = $('#modalInputPatientName').val();
		var address = $('#modalInputPatientAddress').val();
		var number = $('#modalInputPatientNumber').val();
		var age = $('#modalInputPatientAge').val();
		
		var inputPatient = {
	            'name' : name,
	            'address' : address,
	            'persoNumber' : number,
	            'age' : age
	          };
		myJsRoutes.controllers.Application.insertPatientFromJson().ajax({
	            data : JSON.stringify(inputPatient),
	            contentType : 'application/json',
	            success : function (outpuPatient) {
	              // add it to list
	              var patientListItem =  '<li><a onclick="populatePatient(\'' + outpuPatient.id + '\',\'' + outpuPatient.name + '\',\'' + outpuPatient.persoNumber + '\',\'' + outpuPatient.address +
                  '\',\'' + outpuPatient.age + '\');" href="#">' + outpuPatient.name +'</a></li>'
	              $('#patientDropDownList').prepend(patientListItem);
	              populatePatient(outpuPatient.id,outpuPatient.name,outpuPatient.persoNumber,outpuPatient.address,outpuPatient.age);
	              
	            }// end of success
	    });// end of ajax call
		
	      return;
	}
	
	function fillUnknownPatient(){
		populatePatient("","Unknown Patient","","","");
	}
	
	
	// simple function that just gets the data from the assessment page and package it
	// into a json object
	function getUpdatedAlarmFromAssesmentPage(){
	    var patientId = $('#dynamicPatientInfo').find('#patientId').val();
	    var notes = $('#patientRegistrationNotesBox').val();
	    var alarmId = $('#assignedAlarmList').find('.list-group-item.active').attr("idnum");

	    //TODO: add the incident address as well
	    
	    var updatedAlarm = {
            'alarmId' : alarmId,
            'notes' : notes,
             'patient' : {
            	 'patientId' : patientId
             }
	    };
		return updatedAlarm;
	}
	
	
	function closeCaseAtRegistration(){

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


	

