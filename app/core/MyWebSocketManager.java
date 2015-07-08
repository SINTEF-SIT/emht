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

import models.Patient;
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
		actionMap.put(ALARM_EXTERNAL_FOLLOWUP_NOTIFY, "alarmExternalFollowupNotify");
		actionMap.put(ALARM_OPEN_EXPIRED, "alarmOpenExpired");
		actionMap.put(ALARM_RESOLUTION_EXPIRED, "alarmResolutionExpired");
		actionMap.put(ALARM_CLOSED, "alarmClosed");
		actionMap.put(ALARM_DISPATCHED, "alarmDispatched");
		actionMap.put(ALARM_FINISHED, "alarmFinished");
		actionMap.put(PATIENT_NEW, "patientNew");
		actionMap.put(MONITOR_STATISTICS, "monitorStatistics");

		// Register ourselves as interested in events from the central EventHandler
		EventHandler.getInstance().addEventListener(this);

		_invoked = true;
	}

	/**
	 * Instance factory method
	 */
	public synchronized static MyWebSocketManager getInstance() {
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

	/**
	 * Interface method triggered when a new event occurs
	 * @param e The Event object containing the relevant data
	 */
	@Override
	public void newEvent(Event e) {
		switch (e.getType()) {
			case ALARM_NEW:
				handleAlarmEvent(e);
				break;
			case ALARM_OPEN_EXPIRED:
				handleAlarmEvent(e);
				break;
			case ALARM_RESOLUTION_EXPIRED:
				handleAlarmEvent(e);
				break;
			case ALARM_CLOSED:
				handleAlarmEvent(e);
				break;
			case ALARM_EXTERNAL_FOLLOWUP_NOTIFY:
				handleAlarmEvent(e);
				break;
			case ALARM_FINISHED:
				handleAlarmEvent(e);
				break;
			case PATIENT_NEW:
				handlePatientEvent(e);
				break;
			case MONITOR_STATISTICS:
				handleStatsEvent(e);
				break;
		}
	}

	/**
	 * Interface method called by the EventHandler to check what events is in the interest set at runtime
	 * @return A Set of EventTypes
	 */
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

	/* Event handler methods */

	/**
	 * Handler method that accepts all events that have an associated Alarm and notifies all WebSocket clients
	 * @param e An Event object
	 */
	public void handleAlarmEvent(Event e) {
		if (e.getAlarm() == null) throw new IllegalArgumentException("Event object did not contain an Alarm");
		ObjectNode wrapper = Json.newObject();
		wrapper.put("action", actionMap.get(e.getType()));
		wrapper.put("alarm", Alarm.toJson(e.getAlarm()));

		notifyAll(wrapper);
	}

	public void handleStatsEvent(Event e) {
		ObjectNode wrapper = Json.newObject();
		wrapper.put("action", actionMap.get(e.getType()));
		wrapper.put("stats", Global.localMonitor.getStats().toJson());

		notifyAll(wrapper);
	}

	public void handlePatientEvent(Event e) {
		if (e.getPatient() == null) throw new IllegalArgumentException("Event object did not contain a Patient");
		ObjectNode wrapper = Json.newObject();
		wrapper.put("action", actionMap.get(e.getType()));
		wrapper.put("patient", Patient.toJson(e.getPatient()));

		notifyAll(wrapper);
	}
}
