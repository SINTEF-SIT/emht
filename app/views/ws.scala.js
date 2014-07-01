$(function(){

    // get websocket class, firefox has a different way to get it
    var WS = window['MozWebSocket'] ? window['MozWebSocket'] : WebSocket;
    
    // open pewpew with websocket
    var socket = new WS('@routes.Application.wsInterface().webSocketURL(request)');
    
    var writeMessages = function(event){
    
        var data = JSON.parse(event.data);
        var action = data.action.action;

        if(null != action){
        	switch (action) {
            case "addAlarm":
                var alarm = data.alarm;
                var time = new Date(alarm.openingDate);
                var formatedTime = $.format.date(time, "dd/MM HH:mm")
                var listItem = '<a href="#" idnum="'+ alarm.id +'" id="Alarm' + alarm.id  + '"  onclick="selectOpenAlarm(' + alarm.id + ',' + alarm.callee.id + ');return false;" class="list-group-item alarmItem">' +
                      '<img src="/assets/images/' + alarm.type + '.png" class="img-thumbnail pull-left" width="48" height="48"/>' +
                      '<h4 class="list-group-item-heading">' + alarm.id + ', arrived at '+ formatedTime  +' </h4>' + 
                      '<p class="list-group-item-text">Callee '+ alarm.callee.phoneNumber + '</p>';
                  $("#unassignedAlarmList").append(listItem);
                break;
            case "addTimeNotification":
                
            	var clockImage = '<img src="/assets/images/clock.png" class="img-thumbnail pull-left clock-icon" width="48" height="48"/>';
            	var alarm = $("#Alarm" + data.alarmId).children(":first").after(clockImage); // find list item and add the timer after its type symbol
            	
                break;
        	}
        }
        
        
    
    }
    
    socket.onmessage = writeMessages;
    
 
});