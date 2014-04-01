    <script id="showInfoScript" language="javascript" type="text/javascript">
   function showInfoFromAlarm(index) {
	   myJsRoutes.controllers.Application.getAlarm(index).ajax({
         success : function(data) { $("#details").html(data) }
       });
    };

    </script>
    