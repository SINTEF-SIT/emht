package controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import controllers.auth.Authorization;
import core.GoogleCloudMessaging;
import core.event.Event;
import core.event.EventHandler;
import core.event.EventType;
import core.event.MonitorEvent;
import models.*;
import models.sensors.ComponentReading;
import models.sensors.Sensor;
import play.Logger;
import play.Routes;
import play.api.libs.json.JsPath;
import play.cache.Cache;
import play.data.*;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import play.mvc.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.Global;
import core.MyWebSocketManager;
import controllers.auth.Authentication;

public class Application extends Controller {

	// We keep the Web Socket Manager singleton as a field for easier reference
	private static MyWebSocketManager WS = MyWebSocketManager.getInstance();

	static Form<Alarm> alarmForm = Form.form(Alarm.class);

	public static Result index() {
		return redirect(controllers.routes.Application.openAlarms());
	}

	/**
	 * Default login controller
	 * @return A Result object containing the response body
	 */
	public static Result login() {
		// If user is already logged in, redirect to dashboard
		if (session().get("id") != null) {
			return redirect(controllers.routes.Application.openAlarms());
		}
		return ok(views.html.login.render(Authentication.loginForm));
	}

	/**
	 * Default logout controller
	 * @return
	 */
	public static Result logout() {
		session().clear();
		return redirect(controllers.routes.Application.login());
	}

	/**
	 * Default authentication controller. This controller passes the provided credentials
	 * from the login form onto the Authentication class for validation.
	 * @return A Result object containing the response body
	 */
	public static Result authenticate() {
		Form<Authentication.Login> filledForm = Authentication.loginForm.bindFromRequest();

		// Return a bad request if validation failed
		if (filledForm.hasErrors()) {
			Logger.debug("Failed authentication attempt for: " + filledForm.data().get("username"));
			return badRequest(views.html.login.render(filledForm));
		}

		else {
			// Fetch the user object, as all invalid login attempts will cause the form to contain errors
			AlarmAttendant user = AlarmAttendant.getAttendantFromUsername(filledForm.get().username);
			Logger.debug("Successfull authentication from: " + user.username);
			// Set the user ID in session and cache the user object
			session("id", Long.toString(user.id));
			session("username", user.username);
			session("role", Integer.toString(user.role));
			Cache.set(Long.toString(user.id), user);

			return redirect(controllers.routes.Application.openAlarms());
		}
	}

	/* BEGIN ENDPOINTS */

	/**
	 * Returns a JSON object of the currently logged in user
	 * @return
	 */
	@Security.Authenticated(Authorization.Authorized.class)
	public static Result me() {
		AlarmAttendant aa = AlarmAttendant.find.byId(Long.parseLong(session().get("id")));
		if (aa == null || aa.id == 0) return notFound(); // Should never happen
		return ok(AlarmAttendant.toJson(aa));
	}

	/**
	 * Retrieve the main dashboard view
	 * @return HTTP response with the main Dashboard
	 */
	@Security.Authenticated(Authorization.Authorized.class)
	public static Result openAlarms() {
		List<Alarm> alarms = Alarm.allOpenAlarms();
		Content html = views.html.index.render(alarms, alarmForm, session().get("username"));

		return ok(html);
	}

	/**
	 * Retrieves a list of alarms currently assigned to the logged in user
	 * @return A JSON object containing an array of alarms
	 */
	@Security.Authenticated(Authorization.Authorized.class)
	@Authorization.PrivilegeLevel(Authorization.FIELD_OPERATOR)
	public static Result alarmsAssignedToMe() {
		ObjectNode jsonAlarms = Json.newObject();
		AlarmAttendant currentUser = AlarmAttendant.get(Long.parseLong(session().get("id")));
		List<Alarm> alarms;

		if (currentUser.role == 3) {
			alarms = Alarm.openAlarmsAssignedToMobileCareTaker(currentUser);
		} else {
			alarms = Alarm.assignedToUser(currentUser);
		}

		jsonAlarms.put("userId", currentUser.id);
		jsonAlarms.put("username", currentUser.username);
		jsonAlarms.put("role", currentUser.role);

		ArrayNode assignedToUser = new ArrayNode(JsonNodeFactory.instance);
		for (Alarm a : alarms) {
			ObjectNode alarm = Alarm.toJson(a);
			assignedToUser.add(alarm);
		}

		// Add the alarm array to the wrapper object
		jsonAlarms.put("alarms", assignedToUser);

		return ok(jsonAlarms);
	}

	/**
	 * Retrieve all open alarms as JSON
	 * @return A JSON Result
	 */
	public static Result allOpenAlarmsJson() {
		List<Alarm> alarms = Alarm.allOpenAlarms();
		ObjectNode wrapper = Json.newObject();
		wrapper.put("total", alarms.size());
		ArrayNode jsonAlarms = wrapper.putArray("alarms");
		for (Alarm a : alarms) {
			jsonAlarms.add(Alarm.toJson(a));
		}
		return ok(wrapper);
	}

	/**
	 * Create a new alarm from POST request body
	 * @return 200 OK or Bad request if callee was not provided
	 */
	public static Result newAlarm() {
		Form<Alarm> filledForm = alarmForm.bindFromRequest(); // create a new form with the request data
		if (filledForm.hasErrors()) {
			return badRequest();
		} else {
			Alarm formAlarm = filledForm.get();

			if (null != formAlarm.callee) {
				formAlarm.callee = Callee.getOrCreate(formAlarm.callee);
				//formAlarm.patient = Patient.getOrCreate(formAlarm.patient);
				Alarm a = Alarm.create(formAlarm);

				return ok(Alarm.toJson(a));
			} else {
				System.out.println("calle was not found in the form");
				return badRequest();
			}

		}
	}

	/**
	 * Retrieve an Alarm object based on its ID
	 * @param id The ID of the Alarm requested
	 * @return An Alarm instance as JSON
	 */
	public static Result getAlarm(Long id) {
		Alarm a = Alarm.get(id);
		ObjectNode jsonAlarm = Alarm.toJson(a);

		return ok(jsonAlarm);
	}

	/**
	 * Update an Alarm object with latitude and longitude coordinates
	 * @param id The ID of the Alarm to update
	 * @return An Alarm instance as JSON
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result setLocationOfAlarm(Long id) {
		JsonNode latLng = request().body().asJson();
		Double latitude = latLng.findPath("latitude").asDouble();
		Double longitude = latLng.findPath("longitude").asDouble();
		String location = latLng.findPath("location").asText();
		Alarm a = Alarm.setLocationFromResolvedAddress(id, location, latitude, longitude);
		return ok(Alarm.toJson(a));
	}

	/**
	 * Update an Alarm object with a new patient
	 * @param id The ID of the Alarm to update
	 * @return An Alarm instance as JSON
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result setPatientOfAlarm(Long id) {
		Alarm a = Alarm.find.byId(id);
		if (a == null || a.id == 0) return notFound("Alarm does not exist");

		JsonNode patient = request().body().asJson();
		Long patientId = patient.get("id").asLong();

		Patient p;
		if (patientId != 0) {
			p = Patient.find.byId(patientId);
			if (p == null || p.id == 0) return notFound("Patient does not exist");
		} else {
			p = new Patient();
			p.name = patient.findPath("name").textValue();
			p.personalNumber = patient.findPath("personalNumber").textValue();
			p.phoneNumber = patient.findPath("phoneNumber").textValue();
			p.address = patient.findPath("address").textValue();
			p.age = patient.findPath("age").asInt();

			// inserts on the db and return the db instance (which will include the id of the patient)
			p = Patient.getOrCreate(p);
		}

		a.patient = p;
		a.save();

		return ok(Patient.toJson(p));
	}

	/**
	 * Return the type and date of each one of the past alarms of the callee
	 * @param calleeId The ID of the Callee requested
	 * @return A pre-formatted alarm log as JSON
	 */
	public static Result getPastAlarmsFromCallee(Long calleeId) {
		List<Alarm> alarmList = Alarm.pastAlarmsFromCallee(calleeId);
		return alarmListToJsonAlarmLog(alarmList);
	}

	/**
	 * Return the type and date of each one of the past alarms of the patient
	 * @param patientId The ID of the Patient requested
	 * @return A pre-formatted alarm log as JSON
	 */
	public static Result getPastAlarmsFromPatient(Long patientId) {
		List<Alarm> alarmList = Alarm.pastAlarmsFromPatient(patientId);
		return alarmListToJsonAlarmLog(alarmList);
	}

	/**
	 * Convert a list of alarms into json alarm logs containing date and type of alarms
	 * @param alarmList A list of Alarm objects
	 * @return A formatted array of alarm log instances as JSON
	 */
	private static Result alarmListToJsonAlarmLog(List<Alarm> alarmList) {

		SimpleDateFormat ddMMyy = new SimpleDateFormat ("dd/MM yyyy");
		SimpleDateFormat hhMin = new SimpleDateFormat ("HH:mm");

		ObjectNode result = Json.newObject();

		ArrayNode alarmArray = new ArrayNode(JsonNodeFactory.instance);
		for (Alarm temp : alarmList) {
			ObjectNode  alarm = Json.newObject();
			alarm.put("day", ddMMyy.format(temp.closingTime));
			alarm.put("hour", hhMin.format(temp.closingTime));
			alarm.put("type", temp.type);
			alarm.put("notes", temp.notes);
			alarmArray.add(alarm);
		}
		result.put("alarmArray",alarmArray);
		return ok(result);

	}

	/**
	 * Retrieve a list of potential patients based on an Alarm instance
	 * @param id The ID of the alarm to use as query
	 * @return An arrey of Patients as JSON
	 */
	public static Result getProspectPatients(Long id) {
		List<Patient> patientList = Patient.prospectPatientsFromAlarm(id);

		ObjectNode result = Json.newObject();
		ArrayNode patients = result.putArray("patients");
		for (Patient p : patientList) {
			patients.add(Patient.toJson(p));
		}
		result.put("patients", patients);

		return ok(result);
	}

	/**
	 * Endpoint for handling patient searching
	 * @return A JSON array of Patient objects
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result patientSearch() {
		JsonNode searchData = request().body().asJson();
		String searchString = searchData.findPath("query").asText();
		List<Patient> results = Patient.search(searchString);
		ObjectNode jsonResult = Json.newObject();
		jsonResult.put("total", results.size());
		ArrayNode patients = jsonResult.putArray("results");
		for (Patient p : results) {
			patients.add(Patient.toJson(p));
		}
		return ok(jsonResult);
	}

	/**
	 * Retrieve a Callee based on an Alarm ID
	 * @param id The ID of the Alarm to use as query
	 * @return A Callee object as JSON
	 */
	public static Result getCalleeFromAlarm(Long id) {
		Alarm a = Alarm.get(id);

		ObjectNode calle = Json.newObject();
		calle.put("id", a.callee.id);
		calle.put("name", a.callee.name);
		calle.put("phoneNumber", a.callee.phoneNumber);
		calle.put("address", a.callee.address);

		return ok(calle);
	}

	/**
	 * Create and insert a Patient into the database from the POST request body
	 * @return A Patient object as JSON
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result insertPatientFromJson() {
		JsonNode json = request().body().asJson();
		Patient p = new Patient();
		p.name = json.findPath("name").textValue();
		p.personalNumber = json.findPath("personalNumber").textValue();
		p.phoneNumber = json.findPath("phoneNumber").textValue();
		p.address = json.findPath("address").textValue();
		p.age = json.findPath("age").asInt();

		// inserts on the db and return the db instance (which will include the id of the patient)
		Patient retObj = Patient.getOrCreate(p);
		ObjectNode patient = Patient.toJson(retObj);

		// Trigger the event
		EventHandler.dispatch(new MonitorEvent(EventType.PATIENT_NEW, null, null, retObj));

		return ok(patient);
	}

	/**
	 * Close a case based on provided alarm data from POST request body
	 * @return 200 OK
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result closeCase() {
		JsonNode json = request().body().asJson();
		JsonNode patient = json.get("patient");
		Long patientId = 0L;
		if (!patient.isNull()) patientId = json.get("patient").get("id").asLong();
		String notes = json.findPath("notes").asText();
		String alarmOccurance = json.findPath("occuranceAddress").asText();
		long alarmId = json.findPath("id").asLong();

		Alarm a = new Alarm();
		a.occuranceAddress = alarmOccurance;
		a.id = alarmId;

		// If we have addition to the notes, append and template it
		if (notes != null && notes.length() > 0) {
			a.setNotes(session().getOrDefault("username", "unknown"), notes);
		}

		if (0 != patientId) {
			a.patient = Patient.getFromId(patientId);
		}

		Alarm.closeAlarm(a);

		return ok();
	}

	/**
	 * Flag an Alarm object as being finished by the mobile care taker
	 * @param id The ID of the Alarm in question
	 * @return The Alarm object as JSON if ok, unauthorized if mismatch between reporting caretaker and
	 * registered caretaker. Not found if alarm id does not exist.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result finishCase(Long id) {
		String careTaker = session().get("username");
		Alarm a = Alarm.get(id);
		if (a != null) {
			Logger.debug("finishAlarm called on: " + id.toString());

			/* TODO: ENABLE BELOW CODE DURING REGULAR OPERATION (It's disabled for development purposes)
			if (!a.mobileCareTaker.username.equals(careTaker)) {
				Logger.debug("Attempt to finish a case not designated for that user");
				return unauthorized("Attempt to finish a case not designated for that user");
			}
			*/
			a.finished = true;
			a.mobileCareTaker = null;
			a.save();

			// Trigger the event
			EventHandler.dispatch(new MonitorEvent(EventType.ALARM_FINISHED, a, null, null));

			return ok(Alarm.toJson(a));
		} else {
			Logger.debug("Attempt to finish a non-existant case: " + id);
			return notFound();
		}
	}

	/**
	 * Update the data of an alarm based on the POST request body
	 * @return An updated Alarm object as JSON
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result saveAndFollowupCase() {
		AlarmAttendant at = AlarmAttendant.find.byId(Long.parseLong(session().getOrDefault("id", "0")));
		Logger.debug(request().body().toString());
		JsonNode json = request().body().asJson();
		long patientId;
		JsonNode patient = json.get("patient");
		if (!patient.isNull()) patientId = patient.get("id").asLong();
		else patientId = 0;
		String notes = json.findPath("notes").asText();
		String alarmOccurance = json.findPath("occuranceAddress").asText();
		long alarmId = json.findPath("id").asLong();
		Long mobileCareTaker = json.findPath("mobileCareTaker").asLong();
		boolean finished = json.findPath("finished").asBoolean();

		Alarm a = Alarm.get(alarmId);
		a.occuranceAddress = alarmOccurance;
		a.finished = finished;

		// Do we have a provided assessment from attendant?
		if (json.hasNonNull("assessment")) {
			JsonNode attendantAssessment = json.get("assessment");
			a.assessment.sensorsChecked = attendantAssessment.get("sensorsChecked").asBoolean();
			a.assessment.patientInformationChecked = attendantAssessment.get("patientInformationChecked").asBoolean();
			if (attendantAssessment.hasNonNull("nmi")) {
				JsonNode nmi = attendantAssessment.get("nmi");
				if (!nmi.get("conscious").isNull()) a.assessment.nmi.conscious = nmi.get("conscious").asBoolean();
				else a.assessment.nmi.conscious = null;
				if (!nmi.get("breathing").isNull()) a.assessment.nmi.breathing = nmi.get("breathing").asBoolean();
				else a.assessment.nmi.breathing = null;
				if (!nmi.get("movement").isNull()) a.assessment.nmi.movement = nmi.get("movement").asBoolean();
				else a.assessment.nmi.movement = null;
				if (!nmi.get("standing").isNull()) a.assessment.nmi.standing = nmi.get("standing").asBoolean();
				else a.assessment.nmi.standing = null;
				if (!nmi.get("talking").isNull()) a.assessment.nmi.talking = nmi.get("talking").asBoolean();
				else a.assessment.nmi.talking = null;
			}
		}

		// Do we have a provided assessment from field operator?
		if (json.hasNonNull("fieldAssessment")) {
			JsonNode fieldOperatorAssessment = json.get("fieldAssessment");
			a.fieldAssessment.sensorsChecked = fieldOperatorAssessment.get("sensorsChecked").asBoolean();
			a.fieldAssessment.patientInformationChecked = fieldOperatorAssessment.get("patientInformationChecked").asBoolean();
			if (fieldOperatorAssessment.hasNonNull("nmi")) {
				JsonNode nmi = fieldOperatorAssessment.get("nmi");
				if (!nmi.get("conscious").isNull()) a.fieldAssessment.nmi.conscious = nmi.get("conscious").asBoolean();
				else a.fieldAssessment.nmi.conscious = null;
				if (!nmi.get("breathing").isNull()) a.fieldAssessment.nmi.breathing = nmi.get("breathing").asBoolean();
				else a.fieldAssessment.nmi.breathing = null;
				if (!nmi.get("movement").isNull()) a.fieldAssessment.nmi.movement = nmi.get("movement").asBoolean();
				else a.fieldAssessment.nmi.movement = null;
				if (!nmi.get("standing").isNull()) a.fieldAssessment.nmi.standing = nmi.get("standing").asBoolean();
				else a.fieldAssessment.nmi.standing = null;
				if (!nmi.get("talking").isNull()) a.fieldAssessment.nmi.talking = nmi.get("talking").asBoolean();
				else a.fieldAssessment.nmi.talking = null;
			}
		}

		if (0 != patientId) {
			a.patient = new Patient();
			a.patient.id = patientId;
		}

		// If we have assigned a field operator of type mobileCareTaker (role == 3)
		if (mobileCareTaker != null && mobileCareTaker > 0) {
			a.mobileCareTaker = AlarmAttendant.get(mobileCareTaker);
			// Notify the Field Operator through GCM
			F.Promise<WS.Response> resp = GoogleCloudMessaging.dispatchAlarm(a.mobileCareTaker);
			if (resp != null) Logger.debug("GCM Response: " + resp.get(5000).getBody());
		}

		// If we have addition to the notes, append and template it
		if (notes != null && notes.length() > 0) {
			a.setNotes(session().getOrDefault("username", "unknown"), notes);
		}

		Alarm.saveAndFollowupAlarm(a);

		return ok(Alarm.toJson(a));
	}


	/**
	 * Assign an attendant to an Alarm based on the POST request body and active user
	 * @return 200 OK, or Unauthorized if lacking privileges
	 */
	@Security.Authenticated(Authorization.Authorized.class)
	@Authorization.PrivilegeLevel(Authorization.ATTENDANT)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result assignAlarmFromJson() {
		JsonNode json = request().body().asJson();
		Long alarmId =  json.findPath("alarmId").asLong();
		AlarmAttendant a = AlarmAttendant.getAttendantFromUsername(session().get("username"));

		return ok(Alarm.toJson(Alarm.assignAttendantToAlarm(alarmId, a)));
	}

	/**
	 * Update an Alarm with external data in notes, and notify all web socket connections of
	 * the updated alarm from external source. Retrieves updated notes from POST request body.
	 * @param id The ID of the Alarm in question
	 * @return 200 OK or Bad request if state of the Alarm is non-existent, not dispatched or closed.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result notifyFollowup(Long id) {
		JsonNode alarmNotes = request().body().asJson();
		String notes = alarmNotes.findPath("notes").asText();
		Alarm a = Alarm.get(id);
		// Test if alarm exists and is on following up list
		if (null == a || a.dispatchingTime == null || a.closingTime != null) {
			return badRequest();
		}
		else {
			if (notes != null && notes.length() > 0) {
				a.setNotes(session().getOrDefault("username", "unknown"), notes);
				a.save();
			}

			// Trigger the event
			EventHandler.dispatch(new MonitorEvent(EventType.ALARM_EXTERNAL_FOLLOWUP_NOTIFY, a, null, null));

			return ok(Alarm.toJson(a));
		}
	}

	/**
	 * Dynamic rendered JavaScript routes exposure
	 * @return A JavaScript router
	 */
	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(
			Routes.javascriptRouter("myJsRoutes",
				controllers.routes.javascript.Application.getPastAlarmsFromCallee(),
				controllers.routes.javascript.Application.saveAndFollowupCase(),
				controllers.routes.javascript.Application.closeCase(),
				controllers.routes.javascript.Application.insertPatientFromJson(),
				controllers.routes.javascript.Application.setPatientOfAlarm(),
				controllers.routes.javascript.Application.assignAlarmFromJson(),
				controllers.routes.javascript.Application.getCalleeFromAlarm(),
				controllers.routes.javascript.Application.getProspectPatients(),
				controllers.routes.javascript.Application.patientSearch(),
				controllers.routes.javascript.Application.notifyFollowup(),
				controllers.routes.javascript.Application.finishCase(),
				controllers.routes.javascript.Application.getAlarm(),
				controllers.routes.javascript.Application.setLocationOfAlarm()
			)
		);
	}

	/**
	 * Generic WebSocket JavaScript renderer
	 * @return A rendered JavaScript file
	 */
	public static Result wsJs() {
		return ok(views.js.ws.render());
	}

	/**
	 * Generic Alarm selection JavaScript renderer
	 * @return A rendered JavaScript file
	 */
	public static Result getalarmSelectTemplateJs() {
		return ok(views.js.alarmSelectTemplate.render());
	}

	/**
	 * Generic Patient scripts JavaScript renderer
	 * @return A rendered JavaScript file
	 */
	public static Result getpatientTemplateScriptsJs() {
		return ok(views.js.patientTemplateScripts.render());
	}

	/**
	 * Generic Actions scripts JavaScript renderer
	 * @return A rendered JavaScript file
	 */
	public static Result getactionsAndClosingScriptsJs() {
		return ok(views.js.actionsAndClosingScripts.render());
	}

	/**
	 * Generic Assessment scripts JavaScript renderer
	 * @return A rendered JavaScript file
	 */
	public static Result getassesmentPageScriptsJs() {
		return ok(views.js.assesmentPageScripts.render());
	}

	/**
	 * Generic MapView JavaScript renderer
	 * @return A rendered JavaScript file
	 */
	public static Result getmapViewScriptsJs() { return ok(views.js.mapViewScripts.render()); }

	/**
	 * Generic SensorReading JavaScript renderer
	 * @return
	 */
	public static Result getsensorReadingScriptsJs() { return ok(views.js.sensorReadingScripts.render()); }

	/**
	 * WebSocket interface renderer
	 * @return A WebSocket endpoint for handshaking and connection handling
	 */
	@Security.Authenticated(Authorization.Authorized.class)
	public static WebSocket<JsonNode> wsInterface() {
		// Prefetch the username as it is not available in the returned WebSocket object's scope.
		String username = session().get("username");

		return new WebSocket<JsonNode>() {
			// Called when WebSocket handshake is done
			public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
				WS.start(username, in, out);
			}
		};
	}
}
