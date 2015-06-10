package core;

import java.util.*;

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

	/**
	 * Helper class for creating a Tuple containing both in and out sockets of a connected user
	 */
	public static class ConnectionTuple {
		public WebSocket.In<JsonNode> in;
		public WebSocket.Out<JsonNode> out;

		public ConnectionTuple(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
			this.in = in;
			this.out = out;
		}
	}

	// Container for all connected users and their socket objects
	private static HashMap<String, ConnectionTuple> connections = new HashMap<>();


    public static void start(String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        
        connections.put(username, new ConnectionTuple(in, out));
        
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

	/**
	 * Distribute a message to all registered websocket connections
	 * @param message A JsonNode containing the message information
	 */
    public static void notifyAll(ObjectNode message){
        for (ConnectionTuple connection : connections.values()) {
			connection.out.write(message);
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
		callee.put("name", al.callee.name);

		
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
