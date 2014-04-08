$(function(){

    // get websocket class, firefox has a different way to get it
    var WS = window['MozWebSocket'] ? window['MozWebSocket'] : WebSocket;
    
    // open pewpew with websocket
    var socket = new WS('@routes.Application.wsInterface().webSocketURL(request)');
    
    var writeMessages = function(event){
        $('#socket-messages').prepend('<p>'+event.data+'</p>');
    }
    
    socket.onmessage = writeMessages;
    
 
});