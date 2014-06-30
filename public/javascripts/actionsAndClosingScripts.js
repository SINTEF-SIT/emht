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
	
}

