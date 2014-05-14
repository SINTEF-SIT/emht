
function openAddPatientModal() {
    $('#add_patient_modal').modal("show");
    $('#addPatientModalButton').click(function(e) {
    	addNewPatientFromModal();
     });
            
 };


  /* retrieve possible patients based on the address */
 function retrivePatientsByAddress(adress) {

	 if( $('#dynamicPatientInfo').length == 1){ //check if we have the dynamic data
		  
			       $.getJSON("/patientsByAddress/" + adress,
			                  function(data) {
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
                                  patientDropDownBox += '<li><a onclick="openAddPatientModal();" href="#">Add Patient</a></li>'
                                  patientDropDownBox += '<li><a onclick="fillUnknownPatient();" href="#">Unknown Patient</a></li>'
                                  patientDropDownBox += '</ul></span>';

                                  // building Patient Details
                                  var patientDetails = '<u>Adress:</u>  <span id="patientAddress"/><br><u>Personal Number:</u>  <span id="patientPersoNum"/><br>' +
                                  '<u>Age:</u>  <span id="patientAge"/><br><p><p><u>Patient Location:</u>';
                                  patientDetails+= '<span class="checkbox inline"><label><input id="sameAddressCheckbox" type="checkbox"> Same address as residence</label>' +
                                  '</span>'; // adds checkbox
                                  patientDetails+= '<input type="text" class="form-control" id="incidentAddress" placeholder="Other Address">';

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

			               });

			    }
	 	return;
	 }
 
	function populatePatient(patientId,patientName,personNumber,address,age){
		$('#patientAddress').text(address);
		$('#patientPersoNum').text(personNumber);
		$('#patientAddress').text(address);
		$('#patientAge').text(age);

	    $('#patientDropDown').find('.selection').text(patientName);
	    //$('#patientDropDown').find('.selection').value(patientName);
	    return;
	}
 
	function addNewPatientFromModal(){
		var name = $('#modalInputPatientName').val();
		var address = $('#modalInputPatientAddress').val();
		var number = $('#modalInputPatientNumber').val();
		var age = $('#modalInputPatientAge').val();
		
		var patient = {
	            'name' : name,
	            'address' : address,
	            'persoNumber' : number,
	            'age' : age
	          };
		myJsRoutes.controllers.Application.insertPatientFromJson().ajax({
	            data : JSON.stringify(patient),
	            contentType : 'application/json',
	            success : function (patient) {
	              // add it to list
	              var patientListItem =  '<li><a onclick="populatePatient(\'' + patient.id + '\',\'' + patient.name + '\',\'' + patient.persoNumber + '\',\'' + patient.address +
                  '\',\'' + patient.age + '\');" href="#">' + patient.name +'</a></li>'
	              $('#patientDropDownList').prepend(patientListItem);
	              populatePatient(patient.id,patient.name,patient.persoNumber,patient.address,patient.age);
	              
	            }// end of success
	    });// end of ajax call
		
	      return;
	}
	
	function fillUnknownPatient(){
		populatePatient("","Unknown Patient","","","");
	}
	

