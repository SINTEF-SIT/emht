$(function(){

    // get websocket class, firefox has a different way to get it
    var WS = window['MozWebSocket'] ? window['MozWebSocket'] : WebSocket;
    
    // open pewpew with websocket
    var socket = new WS('@routes.Application.wsInterface().webSocketURL(request)');
    
    var writeMessages = function(event){
    
        var data = JSON.parse(event.data);
        var action = data.action.action;
        var alarm = data.alarm;
        if(null != action && action === "addAlarm"){
          var listItem = '<a href="#" onclick="selectAlarm(' + alarm.id + ',' + alarm.callee.id + ');return false;" class="list-group-item">' +
                '<img src="/assets/images/' + alarm.type + '.png" class="img-thumbnail pull-left" width="48" height="48"/>' +
                '<h4 class="list-group-item-heading">' + alarm.id + ' of type '+ alarm.type  +' </h4>' + 
                '<p class="list-group-item-text">Callee '+ alarm.callee.phoneNumber + ' ;Patient ' + alarm.patient.personalNumber +'</p>';
            $("#unassignedAlarmList").append(listItem);
        }
        
    
    }
    
    socket.onmessage = writeMessages;
    
 
});