package core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import models.Alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.mvc.WebSocket;

public class MyWebSocketManager {
	
    // collect all websockets here
    private static List<WebSocket.Out<JsonNode>> connections = new ArrayList<WebSocket.Out<JsonNode>>();

    public static void start(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
        
        connections.add(out);
        
        in.onMessage(new Callback<JsonNode>(){
            public void invoke(JsonNode event){
            	//MyWebSocketManager.notifyAll(event);
            }
        });
        
        in.onClose(new Callback0(){ // TODO: possibly remove from the connection list
            public void invoke(){
            	//MyWebSocketManager.notifyAll("A connection closed");
            }
        });
    }
    
    // Iterate connection list and write incoming message
    public static void notifyAll(ObjectNode message){
        for (WebSocket.Out<JsonNode> out : connections) {
            out.write(message);
        }
    }
    
    public static void notifyNewAlarm(Alarm al){
    	// TODO: look into automatic jsonfication
    	ObjectNode jsonNotification = Json.newObject();
    	ObjectNode  action = Json.newObject();
    	jsonNotification.put("action", action);
    	action.put("action", "addAlarm");
    	ObjectNode  alarm = Json.newObject();
    	jsonNotification.put("alarm", alarm);
		alarm.put("id", al.id);
		alarm.put("type", al.type);
		alarm.put("openingDate", al.openingTime.getTime());
		ObjectNode  callee = Json.newObject();
		alarm.put("callee", callee);
		callee.put("id", al.callee.id);
		callee.put("phoneNumber", al.callee.phoneNumber);

		
		MyWebSocketManager.notifyAll(jsonNotification);
    	
    }
    
    public static void addTimeIconToAlarm(long id){
    	ObjectNode jsonNotification = Json.newObject();
    	ObjectNode  action = Json.newObject();
    	jsonNotification.put("action", action);
    	action.put("action", "addTimeNotification");
    	jsonNotification.put("alarmId", id);
    	
    	MyWebSocketManager.notifyAll(jsonNotification);
    }
    
    public static void notifyCloseAlarm(Alarm al){
    	ObjectNode jsonNotification = Json.newObject();
    	ObjectNode  action = Json.newObject();
    	jsonNotification.put("action", action);
    	action.put("action", "removeAlarm");
    	jsonNotification.put("alarmId", al.id);
    	
    	MyWebSocketManager.notifyAll(jsonNotification);
    }
    
    public static void notifyFollowUpAlarm(long alarmId){
    	ObjectNode jsonNotification = Json.newObject();
    	ObjectNode  action = Json.newObject();
    	jsonNotification.put("action", action);
    	action.put("action", "notifyFollowup");
    	jsonNotification.put("alarmId", alarmId);
    	
    	MyWebSocketManager.notifyAll(jsonNotification);
    }
    
}
