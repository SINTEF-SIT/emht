package controllers;

import java.text.SimpleDateFormat;
import java.util.List;

import controllers.auth.Authorization;
import models.Alarm;
import models.AlarmAttendant;
import models.Callee;
import models.Patient;
import play.Logger;
import play.Routes;
import play.cache.Cache;
import play.data.*;
import play.libs.Json;
import play.mvc.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.Global;
import core.MyWebSocketManager;
import controllers.auth.Authentication;

public class Application extends Controller {

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

	@Security.Authenticated(Authorization.Authorized.class)
	public static Result openAlarms(){
		List<Alarm> object = Global.alarmList.getAlarmList();
		Content html = views.html.index.render(object, alarmForm);

		return ok(html);
	}

	@Security.Authenticated(Authorization.Authorized.class)
	@Authorization.PrivilegeLevel(Authorization.FIELD_OPERATOR)
	public static Result alarmsAssignedToMe() {
		ObjectNode jsonAlarms = Json.newObject();
		AlarmAttendant currentUser = AlarmAttendant.get(Long.parseLong(session().get("id")));
		List<Alarm> alarms = Alarm.assignedToUser(currentUser);

		jsonAlarms.put("userId", currentUser.id);
		jsonAlarms.put("username", currentUser.username);
		jsonAlarms.put("role", currentUser.role);

		ArrayNode assignedToUser = new ArrayNode(JsonNodeFactory.instance);
		for (Alarm a : alarms) {
			ObjectNode alarm = Json.newObject();
			alarm.put("alarmLog", a.alarmLog == null);
			alarm.put("notes", a.notes);
			alarm.put("type", a.type);
			alarm.put("openingTime", a.openingTime != null ? a.openingTime.toString() : null);
			alarm.put("dispatchingTime", a.dispatchingTime != null ? a.dispatchingTime.toString() : null);
			alarm.put("closingTime", a.closingTime != null ? a.closingTime.toString() : null);


			// Add the callee object if present, otherwise write a null
			if (a.callee != null) {
				ObjectNode callee = Json.newObject();
				callee.put("id", a.callee.id);
				callee.put("name", a.callee.name);
				callee.put("phoneNumber", a.callee.phoneNumber);
				callee.put("address", a.callee.address);
				alarm.put("callee", callee);
			} else {
				alarm.putNull("callee");
			}

			// Add the patient object if present, otherwise write a null
			if (a.patient != null) {
				ObjectNode patient = Json.newObject();
				patient.put("id", a.patient.id);
				patient.put("name", a.patient.name);
				patient.put("persoNumber", a.patient.personalNumber);
				patient.put("phoneNumber", a.patient.phoneNumber);
				patient.put("address", a.patient.address);
				patient.put("age", a.patient.age);
				alarm.put("patient", patient);
			} else {
				alarm.putNull("patient");
			}

			// Insert the alarm into the alarmarray
			assignedToUser.add(alarm);
		}

		// Add the alarm array to the wrapper object
		jsonAlarms.put("alarms", assignedToUser);

		return ok(jsonAlarms);
	}


	public static Result newAlarm(){
		Form<Alarm> filledForm = alarmForm.bindFromRequest(); // create a new form with the request data
		if (filledForm.hasErrors()) {
			return badRequest();
		} else {
			Alarm formAlarm = filledForm.get();

			ObjectNode jsonAlarm = Json.newObject();

			if (null != formAlarm.callee) {
				formAlarm.callee = Callee.getOrCreate(formAlarm.callee);
				//formAlarm.patient = Patient.getOrCreate(formAlarm.patient);
				Alarm a = Alarm.create(formAlarm);//
				jsonAlarm.put("type", a.type);
				jsonAlarm.put("alarmId", a.id);
				if (null != a.callee){
					ObjectNode calle = Json.newObject();
					calle.put("id", a.callee.id);
					calle.put("name", a.callee.name);
					calle.put("phoneNumber", a.callee.phoneNumber);
					calle.put("address", a.callee.address);
					jsonAlarm.put("calle", calle);
				}

				return ok(jsonAlarm);
			} else {
				System.out.println("calle was not found in the form");
				return badRequest();
			}

		}
	}
    
/*    public static Result  deleteAlarm(Long id){
    	  Alarm.delete(id);
    	  return redirect(routes.Application.openAlarms());
    }
    
    public static Result  getOpenAlarm(Long id){
   	 	return ok(
			    views.html.requestInfoFrame.render(Global.alarmList.list.get(id))
			  );
    }*/


	// returns a json alarm such as
	// { "type": string, "notes": string, "alarmId": long,
	//  "callee" : { "phoneNumber": string, "name": string, "address": string, "id": long },
	//  "patient" : { "persoNumber": string, "name": string, "address": string,  "age": int, "id": long }}
	public static Result getAlarm(Long id){

		Alarm a = Alarm.get(id);

		ObjectNode jsonAlarm = Json.newObject();
		jsonAlarm.put("type", a.type);
		jsonAlarm.put("notes", a.notes);
		jsonAlarm.put("alarmId", a.id);
		jsonAlarm.put("occuranceAddress", a.occuranceAddress);

		if (null != a.callee) {
			ObjectNode calle = Json.newObject();
			calle.put("id", a.callee.id);
			calle.put("name", a.callee.name);
			calle.put("phoneNumber", a.callee.phoneNumber);
			calle.put("address", a.callee.address);
			jsonAlarm.put("calle", calle);
		}

		if (null != a.patient) {
			ObjectNode  patient = Json.newObject();
			patient.put("id", a.patient.id);
			patient.put("name", a.patient.name);
			patient.put("persoNumber", a.patient.personalNumber);
			patient.put("phoneNumber", a.patient.phoneNumber);
			patient.put("address", a.patient.address);
			patient.put("age", a.patient.age);
			jsonAlarm.put("patient", patient);
		}

		return ok(jsonAlarm);

	}


	// Return the type and date of each one of the past alarms of the callee
	public static Result  getPastAlarmsFromCallee(Long calleeId) {
		List<Alarm> alarmList = Alarm.pastAlarmsFromCallee(calleeId);
		return alarmListToJsonAlarmLog(alarmList);
	}
	// Return the type and date of each one of the past alarms of the patient
	public static Result  getPastAlarmsFromPatient(Long patientId) {
		List<Alarm> alarmList = Alarm.pastAlarmsFromPatient(patientId);
		return alarmListToJsonAlarmLog(alarmList);
	}

	// convert a list of alarms into json alarm logs containing date and
	// type of alarms
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



	public static Result getProspectPatients(Long id) {
		List<Patient> patientList = Patient.prospectPatientsFromAlarm(id);

		ObjectNode result = Json.newObject();

		ArrayNode patientArray = new ArrayNode(JsonNodeFactory.instance);
		if (null != patientList) {
			for (Patient temp : patientList) {
				ObjectNode  patient = Json.newObject();
				patient.put("id", temp.id);
				patient.put("name", temp.name);
				patient.put("persoNumber", temp.personalNumber);
				patient.put("phoneNumber", temp.phoneNumber);
				patient.put("address", temp.address);
				patient.put("age", temp.age);
				if(null != temp.obs)
					patient.put("obs", temp.obs);
				else
					patient.put("obs", "");
				patientArray.add(patient);
			}
		}
		result.put("patientArray",patientArray);
		return ok(result);

	}

	public static Result getCalleeFromAlarm(Long id) {
		Alarm a = Alarm.get(id);

		ObjectNode calle = Json.newObject();
		calle.put("id", a.callee.id);
		calle.put("name", a.callee.name);
		calle.put("phoneNumber", a.callee.phoneNumber);
		calle.put("address", a.callee.address);

		return ok(calle);
	}


	@BodyParser.Of(BodyParser.Json.class)
	public static Result insertPatientFromJson(){
		JsonNode json = request().body().asJson();
		Patient p = new Patient();
		p.name = json.findPath("name").textValue();
		p.personalNumber = json.findPath("persoNumber").textValue();
		p.phoneNumber = json.findPath("phoneNumber").textValue();
		p.address = json.findPath("address").textValue();
		p.age = json.findPath("age").asInt();

		// inserts on the db and return the db instance (which will include the id of the patient)
		Patient retObj = Patient.getOrCreate(p);
		ObjectNode  patient = Json.newObject();
		patient.put("id", retObj.id);
		patient.put("name", retObj.name);
		patient.put("phoneNumber", retObj.phoneNumber);
		patient.put("persoNumber", retObj.personalNumber);
		patient.put("address", retObj.address);
		patient.put("age", retObj.age);

		return ok(patient);
	}
    
/*    @BodyParser.Of(BodyParser.Json.class)
    public static Result saveCase(){
  	  	JsonNode json = request().body().asJson();
        long patientId = json.findPath("patientId").asLong();
        String notes = json.findPath("notes").asText();
        long alarmId = json.findPath("alarmId").asLong();
        
        Alarm a = new Alarm();
        a.id = alarmId;
        a.notes = notes;
        if(0 != patientId){
        	a.patient = new Patient();
        	a.patient.id = patientId;
        }
        Alarm.saveAlarm(a);

    	return ok();
    }*/

	@BodyParser.Of(BodyParser.Json.class)
	public static Result closeCase() {
		JsonNode json = request().body().asJson();
		long patientId = json.findPath("patientId").asLong();
		String notes = json.findPath("notes").asText();
		String alarmOccurance = json.findPath("occuranceAddress").asText();
		long alarmId = json.findPath("alarmId").asLong();

		Alarm a = new Alarm();
		a.occuranceAddress = alarmOccurance;
		a.id = alarmId;
		a.notes = notes;

		if (0 != patientId) {
			a.patient = new Patient();
			a.patient.id = patientId;
		}

		Alarm.closeAlarm(a);

		return ok();
	}


	@BodyParser.Of(BodyParser.Json.class)
	public static Result saveAndFollowupCase() {
		JsonNode json = request().body().asJson();
		long patientId = json.findPath("patientId").asLong();
		String notes = json.findPath("notes").asText();
		String alarmOccurance = json.findPath("occuranceAddress").asText();
		long alarmId = json.findPath("alarmId").asLong();

		Alarm a = new Alarm();
		a.occuranceAddress = alarmOccurance;
		a.id = alarmId;
		a.notes = notes;

		if (0 != patientId) {
			a.patient = new Patient();
			a.patient.id = patientId;
		}

		Alarm.saveAndFollowupAlarm(a);

		return ok();
	}


	@Security.Authenticated(Authorization.Authorized.class)
	@Authorization.PrivilegeLevel(Authorization.ATTENDANT)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result assignAlarmFromJson() {
		JsonNode json = request().body().asJson();
		Long alarmId =  json.findPath("alarmId").asLong();
		AlarmAttendant a = AlarmAttendant.getAttendantFromUsername(session().get("username"));
		Alarm.assignAttendantToAlarm(alarmId, a);

		return ok();
	}



	// to be called when a followup alarm is triggered back again
	// in other words, when the callee responsible for it call it back
	public static Result notifyFollowup(Long id){
		Alarm a = Alarm.get(id);
		// test if alarm exists and is on following up list
		if (null == a || a.dispatchingTime == null || a.closingTime != null) {
			return badRequest();
		}
		else {
			MyWebSocketManager.notifyFollowUpAlarm(id);
			return ok();
		}
	}

	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(
			Routes.javascriptRouter("myJsRoutes",
				//controllers.routes.javascript.Application.deleteAlarm(),
				controllers.routes.javascript.Application.getPastAlarmsFromCallee(),
				//controllers.routes.javascript.Application.assignAlarm(),
				controllers.routes.javascript.Application.saveAndFollowupCase(),
				controllers.routes.javascript.Application.closeCase(),
				//controllers.routes.javascript.Application.saveCase(),
				controllers.routes.javascript.Application.insertPatientFromJson(),
				controllers.routes.javascript.Application.assignAlarmFromJson(),
				controllers.routes.javascript.Application.getCalleeFromAlarm(),
				controllers.routes.javascript.Application.getProspectPatients(),
				controllers.routes.javascript.Application.notifyFollowup(),
				controllers.routes.javascript.Application.getAlarm()
			)
		);
	}

	// get the ws.js script
	public static Result wsJs() {
		return ok(views.js.ws.render());
	}

	// get the other scripts
	public static Result getalarmSelectTemplateJs() {
		return ok(views.js.alarmSelectTemplate.render());
	}

	public static Result getpatientTemplateScriptsJs() {
		return ok(views.js.patientTemplateScripts.render());
	}
	public static Result getactionsAndClosingScriptsJs() {
		return ok(views.js.actionsAndClosingScripts.render());
	}
	public static Result getassesmentPageScriptsJs() {
		return ok(views.js.assesmentPageScripts.render());
	}

	@Security.Authenticated(Authorization.Authorized.class)
	public static WebSocket<JsonNode> wsInterface() {
		// Prefetch the username as it is not available in the returned WebSocket object's scope.
		String username = session().get("username");

		return new WebSocket<JsonNode>() {
			// Called when WebSocket handshake is done
			public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
				MyWebSocketManager.start(username, in, out);
			}
		};
	}
}
