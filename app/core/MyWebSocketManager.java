package core;

import java.util.*;

import static core.event.EventType.*;

import core.event.Event;
import core.event.EventHandler;
import core.event.EventType;
import core.event.EventListener;
import models.Alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.mvc.WebSocket;

public class MyWebSocketManager implements EventListener {

	private static boolean _invoked;
	private static MyWebSocketManager _singleton;

	// Container for all connected users and their socket objects
	private HashMap<String, ConnectionTuple> connections;
	// Event to WS/JSON action map
	private HashMap<EventType, String> actionMap;

	/**
	 * Private constructor supporting Singleton pattern
	 */
	private MyWebSocketManager() {
		connections = new HashMap<>();
		actionMap = new HashMap<>();

		// Create mapping between EventType and json 'action' field
		actionMap.put(ALARM_ASSESSMENT_SET, "alarmAssessmentSet");
		actionMap.put(ALARM_ASSIGNED, "alarmAssigned");
		actionMap.put(ALARM_NEW, "alarmNew");
		actionMap.put(ALARM_LOCATION_VERIFIED, "alarmLocationVerified");
		actionMap.put(ALARM_PATIENT_SET, "alarmPatientSet");
		actionMap.put(ALARM_FIELD_ASSESSMENT_SET, "alarmFieldAssessmentSet");
		actionMap.put(ALARM_OPEN_EXPIRED, "alarmOpenExpired");
		actionMap.put(ALARM_RESOLUTION_EXPIRED, "alarmResolutionExpired");
		actionMap.put(ALARM_CLOSED, "alarmClosed");
		actionMap.put(ALARM_DISPATCHED, "alarmDispatched");
		actionMap.put(ALARM_FINISHED, "alarmFinished");
		actionMap.put(PATIENT_NEW, "patientNew");

		// Register ourselves as interested in events from the central EventHandler
		EventHandler.getInstance().addEventListener(this);

		_invoked = true;
	}

	/**
	 * Instance factory method
	 */
	public static MyWebSocketManager getInstance() {
		if (!_invoked) _singleton = new MyWebSocketManager();
		return _singleton;
	}

	/**
	 * Initialization method invoked after WS handshaking is complete
	 * @param username Username of the user who initialized the connection
	 * @param in Inbound socket object
	 * @param out Outbound socket object
	 */
	public void start(String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {

		connections.put(username, new ConnectionTuple(in, out));
		Logger.debug("Opened WebSocket connection from: " + username);

		in.onMessage(new Callback<JsonNode>() {
			public void invoke(JsonNode event) {
				Logger.debug("Received event from " + getUsernameFromWebSocket(in) + ": " + event.asText());
			}
		});

		in.onClose(new Callback0() {
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
	public String getUsernameFromWebSocket(WebSocket.In<JsonNode> in) {
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
	public String getUsernameFromWebSocket(WebSocket.Out<JsonNode> out) {
		for (Map.Entry<String, ConnectionTuple> entry : connections.entrySet()) {
			if (entry.getValue().out == out) return entry.getKey();
		}
		return null;
	}

	/**
	 * Distribute a message to all registered websocket connections
	 * @param message A JsonNode containing the message information
	 */
	public void notifyAll(ObjectNode message) {
		for (ConnectionTuple connection : connections.values()) {
			connection.out.write(message);
		}
	}

	/* Event listener support */

	@Override
	public void newEvent(Event e) {

	}

	@Override
	public Set<EventType> listenFor() {
		// Just tell the event handler that we are interested in the entire event set
		return actionMap.keySet();
	}

	/* Support classes */

	/**
	 * Helper class for creating a Tuple containing both in and out sockets of a connected user
	 */
	public class ConnectionTuple {
		public WebSocket.In<JsonNode> in;
		public WebSocket.Out<JsonNode> out;

		public ConnectionTuple(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
			this.in = in;
			this.out = out;
		}
	}

	/* Deprecated methods */

	@Deprecated
	public void notifyNewAlarm(Alarm al) {
		ObjectNode wrapper = Json.newObject();
		ObjectNode action = Json.newObject();
		ObjectNode alarm = Alarm.toJson(al);
		action.put("action", "addAlarm");
		wrapper.put("action", action);
		wrapper.put("alarm", alarm);

		notifyAll(wrapper);
	}

	@Deprecated
	public void addTimeIconToAlarm(long id){
		ObjectNode jsonNotification = Json.newObject();
		ObjectNode  action = Json.newObject();
		jsonNotification.put("action", action);
		action.put("action", "addTimeNotification");
		jsonNotification.put("alarmId", id);

		notifyAll(jsonNotification);
	}

	@Deprecated
	public void notifyCloseAlarm(Alarm al){
		ObjectNode jsonNotification = Json.newObject();
		ObjectNode  action = Json.newObject();
		jsonNotification.put("action", action);
		action.put("action", "removeAlarm");
		jsonNotification.put("alarmId", al.id);

		notifyAll(jsonNotification);
	}

	@Deprecated
	public void notifyFollowUpAlarm(long alarmId){
		ObjectNode jsonNotification = Json.newObject();
		ObjectNode  action = Json.newObject();
		jsonNotification.put("action", action);
		action.put("action", "notifyFollowup");
		jsonNotification.put("alarmId", alarmId);

		notifyAll(jsonNotification);
	}

	@Deprecated
	public void notifyFinishedAlarm(Alarm a) {
		ObjectNode wrapper = Json.newObject();
		ObjectNode alarmJson = Alarm.toJson(a);
		ObjectNode action = Json.newObject();
		wrapper.put("action", action);
		action.put("action", "finishedAlarm");
		wrapper.put("alarm", alarmJson);

		notifyAll(wrapper);
	}
}
