package core;

import java.util.*;

import javax.persistence.CascadeType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import models.Alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
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

	/**
	 *
	 * @param username
	 * @param in
	 * @param out
	 */
    public static void start(String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        
        connections.put(username, new ConnectionTuple(in, out));
		Logger.debug("Opened WebSocket connection from: " + username);
        
        in.onMessage(new Callback<JsonNode>() {
			public void invoke(JsonNode event) {
				Logger.debug("Received event from " + getUsernameFromWebSocket(in) + ": " + event.asText());
			}
		});
        
        in.onClose(new Callback0(){ // TODO: possibly remove from the connection list
            public void invoke() {
				String username = getUsernameFromWebSocket(in);
            	Logger.debug("Closed WebSocket connection from: " + getUsernameFromWebSocket(in));
				connections.remove(username);
            }
        });
    }

	/**
	 * Helper method that retrieves a related username from a WebSocket object.
	 * @param in The socket object to retrieve the related username from.
	 * @return Related username of the socket if found, null otherwise.
	 */
	public static String getUsernameFromWebSocket(WebSocket.In<JsonNode> in) {
		for (Map.Entry<String, ConnectionTuple> entry : connections.entrySet()) {
			if (entry.getValue().in == in) return entry.getKey();
		}
		return null;
	}

	/**
	 * Helper method that retrieves a related username from a WebSocket object.
	 * @param out The socket object to retrieve the related username from.
	 * @return Related username of the socket if found, null otherwise.
	 */
	public static String getUsernameFromWebSocket(WebSocket.Out<JsonNode> out) {
		for (Map.Entry<String, ConnectionTuple> entry : connections.entrySet()) {
			if (entry.getValue().out == out) return entry.getKey();
		}
		return null;
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
    
    public static void notifyNewAlarm(Alarm al) {
		ObjectNode wrapper = Json.newObject();
		ObjectNode action = Json.newObject();
		ObjectNode alarm = Alarm.toJson(al);
		action.put("action", "addAlarm");
		wrapper.put("action", action);
		wrapper.put("alarm", alarm);
		MyWebSocketManager.notifyAll(wrapper);
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

	public static void notifyFinishedAlarm(Alarm a) {
		ObjectNode wrapper = Json.newObject();
		ObjectNode alarmJson = Alarm.toJson(a);
		ObjectNode action = Json.newObject();
		wrapper.put("action", action);
		action.put("action", "finishedAlarm");
		wrapper.put("alarm", alarmJson);

		MyWebSocketManager.notifyAll(wrapper);
	}
    
}
