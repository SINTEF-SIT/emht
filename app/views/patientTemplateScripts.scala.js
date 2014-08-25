@import play.i18n._

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
                 //$("#calleeBox").show();
                 

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
        var dynamicPatientBlock = '<u>@Messages.get("patientpane.name"):</u> ';

        // building Patient Drop Down Block
        var patientDropDownBox = '<span class="btn-group"  id="patientDropDown" ><button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">' +
        '<span class="selection">@Messages.get("patientpane.pill.patient")</span><span class="caret"></span></button><ul id="patientDropDownList" class="dropdown-menu">';
        var array = data.patientArray;
        for(var i in array){
          var patientId = array[i].id;
          var patientName = array[i].name;
          var patientPersoNum = array[i].persoNumber;
          var patientAddress = array[i].address;
          var patientAge = array[i].age;
          var patientPhoneNum = array[i].phoneNumber;
          patientDropDownBox += '<li><a onclick="populatePatient(\'' + patientId + '\',\'' + patientName + '\',\'' + patientPersoNum + '\',\'' + patientAddress +
          '\',\'' + patientPhoneNum + '\',\'' + patientAge + '\');" href="#">' + patientName +'</a></li>'; 
        }
        if( $.isArray(array) && array.length != 0){
      	  patientDropDownBox += '<li class="divider"></li>';
        }
        patientDropDownBox += '<li><a onclick="openAddPatientModal();" href="#">@Messages.get("patientpane.pill.other.patient")</a></li>'
        patientDropDownBox += '<li><a onclick="fillUnknownPatient();" href="#">@Messages.get("patientpane.pill.unknown")</a></li>'
        patientDropDownBox += '</ul></span>';

        // building Patient Details
        var patientDetails = '<u>@Messages.get("patientpane.address"):</u>  <span id="patientAddress"/><br><u>@Messages.get("patientpane.personumber"):</u>  <span id="patientPersoNum"/><br>' +
        '<u>@Messages.get("patientpane.phonenumber"):</u>  <span id="patientPhoneNum"/><br>' +
        '<u>@Messages.get("patientpane.age"):</u>  <span id="patientAge"/><br><input id="patientId" type="hidden"><p><p><u>@Messages.get("patientpane.incident.location"):</u>';
        patientDetails+= '<span class="checkbox inline"><label><input id="sameAddressCheckbox" type="checkbox"> @Messages.get("patientpane.incident.same")</label>' +
        '</span>'; // adds checkbox
        patientDetails+= '<input type="text" class="form-control" id="incidentAddress" placeholder="@Messages.get("patientpane.incident.other")">';
        
        patientDetails+= '<h5>@Messages.get("patientpane.log.title")</h5><table class="table table-bordered" id="patientLogTable"><thead><tr><td>@Messages.get("patientpane.log.date")</td><td>@Messages.get("patientpane.log.hour")</td><td>@Messages.get("patientpane.log.type")</td></tr></thead><tbody></tbody></table>';
        
        dynamicPatientBlock += patientDropDownBox + '<br>' + patientDetails + '<br>';// + identity;
        

        $("#dynamicPatientInfo").html(dynamicPatientBlock);
        
        $("#sameAddressCheckbox").click(function() {
      	    var addr = $("#patientAddress").text();
      	    if(this.checked){
      	    	$("#incidentAddress").val(addr);
      	    }else{
      	    	$('#incidentAddress').val('@Messages.get("patientpane.incident.other")');
      	    }
      	});
        
        //$("#patientBox").show();


 		
 	}
 
	function populatePatient(patientId,patientName,personNumber,address,phoneNumber,age){
		$('#patientAddress').text(address);
		
		// for the perso Number, if it is more than 6 digits, we add a space after the first 6 digits
		var formattedPersoNumber;
		if(personNumber.length > 6){
			formattedPersoNumber = personNumber.substring(0, 6) + " " + personNumber.substring(6);
		}else{
			formattedPersoNumber = personNumber;
		}
		
		$('#patientPersoNum').text(formattedPersoNumber);
		$('#patientPhoneNum').text(phoneNumber);
		$('#patientAddress').text(address);
		$('#patientAge').text(age);
		$('#patientId').val(patientId);

		// empty existing table
		$("#patientLogTable > tbody").html("");
		// destroying the datatable
		//var table = $('#patientLogTable').DataTable();
		//table.destroy();
		
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
	    	   		  
	    	  		// make it a datatable with pagination, TODO: investigate further if we want to commit the
	    	   		  // datatable code
	    	          /*$('#patientLogTable').DataTable( {
	    	          	    "paging": true,
	    	          	    "searching": false,
	    	          	    "ordering":  false,
	    	          	    "pageLength": 5,
	    	          	    "destroy": true,
	    	          	    "lengthChange": false
	    	      	} );*/
	    	   		  
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
		var phoneNumber = $('#modalInputPatientPhoneNumber').val();
		var age = $('#modalInputPatientAge').val();
		
		var inputPatient = {
	            'name' : name,
	            'address' : address,
	            'persoNumber' : number,
	            'phoneNumber' : phoneNumber,
	            'age' : age
	          };
		myJsRoutes.controllers.Application.insertPatientFromJson().ajax({
	            data : JSON.stringify(inputPatient),
	            contentType : 'application/json',
	            success : function (outpuPatient) {
	              // add it to list
	              var patientListItem =  '<li><a onclick="populatePatient(\'' + outpuPatient.id + '\',\'' + outpuPatient.name + '\',\'' + outpuPatient.persoNumber + '\',\'' + outpuPatient.address +
	              '\',\'' + outpuPatient.phoneNumber +'\',\'' + outpuPatient.age + '\');" href="#">' + outpuPatient.name +'</a></li>'
	              $('#patientDropDownList').prepend(patientListItem);
	              populatePatient(outpuPatient.id,outpuPatient.name,outpuPatient.persoNumber,outpuPatient.address,outpuPatient.phoneNumber,outpuPatient.age);
	              
	            }// end of success
	    });// end of ajax call
		
	      return;
	}
	
	function fillUnknownPatient(){
		populatePatient('','@Messages.get("patientpane.pill.unknown")','','','','');
	}
	
	

	
	
/*	function closeCaseAtRegistration(){

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
*/

	function   clearUpCaleeData(){
        $("#calleeName").text("");
        $("#calleeAddress").text("");
        $("#calleePhone").text("");
	}
	function   clearUpPatientData(){
		$("#dynamicPatientInfo").empty();
	}
	

