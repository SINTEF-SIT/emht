package controllers;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.Global;
import core.MyWebSocketManager;

import models.*;
import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {

	static Form<Alarm> alarmForm = Form.form(Alarm.class);
	
    public static Result index() {
    	return redirect(routes.Application.openAlarms());
        //return ok(index.render("Your new application is ready."));
    }

    public static Result  openAlarms(){
    	List<Alarm> object = Global.alarmList.getAlarmList();
    	/*System.out.println("test "); 
    	// debugging loop
    	Iterator<Alarm> iterator = object.iterator();
    	while (iterator.hasNext()) {
    		Alarm a  = iterator.next();
    		System.out.println("Adding alarm " + a.id + "with expiry flag as " + a.expired); 
    	}*/
    	
    	Content html = views.html.index.render(object, alarmForm);
    	 return ok(
    			    html
    			  );
    }
    
    public static Result  newAlarm(){
    	  Form<Alarm> filledForm = alarmForm.bindFromRequest(); // create a new form with the request data
    	  if(filledForm.hasErrors()) {
    	    return redirect(routes.Application.openAlarms());
    	  } else {
    		  Alarm formAlarm = filledForm.get();
    		if(null != formAlarm.callee){
    			formAlarm.callee = Callee.getOrCreate(formAlarm.callee);
    			//formAlarm.patient = Patient.getOrCreate(formAlarm.patient);
    			Alarm.create(formAlarm);//
    		}	
    		else{
    			System.out.println("calle was not found in the form");
    			// TODO: show error because callee was not set
    		}
    	    
    	    return redirect(routes.Application.openAlarms());  
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
    
    
    // Return the type and date of each one of the past alarms of the callee
    public static Result  getPastAlarmsFromCallee(Long calleeId){
    	List<Alarm> alarmList = Alarm.pastAlarmsFromCallee(calleeId); 	
    	return alarmListToJsonAlarmLog(alarmList);
    }
    // Return the type and date of each one of the past alarms of the patient
    public static Result  getPastAlarmsFromPatient(Long patientId){
    	List<Alarm> alarmList = Alarm.pastAlarmsFromPatient(patientId); 	
    	return alarmListToJsonAlarmLog(alarmList);
    }

    // convert a list of alarms into json alarm logs containing date and 
    // type of alarms
    private static Result alarmListToJsonAlarmLog(List<Alarm> alarmList){
    
	SimpleDateFormat ddMMyy = new SimpleDateFormat ("dd/MM yyyy");
	SimpleDateFormat hhMin = new SimpleDateFormat ("hh:mm");
	

	ObjectNode result = Json.newObject();

	ArrayNode alarmArray = new ArrayNode(JsonNodeFactory.instance);
	for (Alarm temp : alarmList) {
		ObjectNode  alarm = Json.newObject();
		alarm.put("day", ddMMyy.format(temp.closingTime));
		alarm.put("hour", hhMin.format(temp.closingTime));
		alarm.put("type", temp.type);
		alarmArray.add(alarm);
	}
	result.put("alarmArray",alarmArray);
	return ok(result);
    
    }
    
    // TO be discontinued once funtion below is working
/*    public static Result  getPatientsByAddress(String address){
    	List<Patient> patientList = Patient.patientFromAddress(address);
    	
		ObjectNode result = Json.newObject();

		ArrayNode patientArray = new ArrayNode(JsonNodeFactory.instance);
		if(null != patientList){
	    	for (Patient temp : patientList) {
	    		ObjectNode  patient = Json.newObject();
				patient.put("id", temp.id);
				patient.put("name", temp.name);
				patient.put("persoNumber", temp.personalNumber);
				patient.put("address", temp.address);
				patient.put("age", temp.age);
				patientArray.add(patient);
	    	}
		}
    	result.put("patientArray",patientArray);
    	return ok(result);

    }*/
    
    public static Result  getProspectPatients(Long id){
    	List<Patient> patientList = Patient.prospectPatientsFromAlarm(id);
    	
		ObjectNode result = Json.newObject();

		ArrayNode patientArray = new ArrayNode(JsonNodeFactory.instance);
		if(null != patientList){
	    	for (Patient temp : patientList) {
	    		ObjectNode  patient = Json.newObject();
				patient.put("id", temp.id);
				patient.put("name", temp.name);
				patient.put("persoNumber", temp.personalNumber);
				patient.put("address", temp.address);
				patient.put("age", temp.age);
				patientArray.add(patient);
	    	}
		}
    	result.put("patientArray",patientArray);
    	return ok(result);

    }
    
    public static Result  getCalleeFromAlarm(Long id){
    	Alarm a = Alarm.get(id);
    	
		ObjectNode calle = Json.newObject();
		calle.put("id", a.callee.id);
		calle.put("name", a.callee.name);
		calle.put("phoneNumber", a.callee.phoneNumber);
		calle.put("address", a.callee.address);
    	return ok(calle);

    }
    
    
    @BodyParser.Of(BodyParser.Json.class)
    public static Result  insertPatientFromJson(){
    	  JsonNode json = request().body().asJson();
  	      Patient p = new Patient();
    	  p.name = json.findPath("name").textValue();
    	  p.personalNumber = json.findPath("persoNumber").textValue();
		  p.address = json.findPath("address").textValue();
		  p.age = json.findPath("age").asInt();
    	  if(p.personalNumber == null) {
    	    return badRequest("Missing parameter [personal Number]"); // TODO: render this
    	  } else {
    		  Patient retObj = Patient.getOrCreate(p); // inserts on the db and return the db instance (which will include the id of the patient)
	    		ObjectNode  patient = Json.newObject();
				patient.put("id", retObj.id);
				patient.put("name", retObj.name);
				patient.put("persoNumber", retObj.personalNumber);
				patient.put("address", retObj.address);
				patient.put("age", retObj.age);
    		  return ok(patient);
    	  }

    }
    
    @BodyParser.Of(BodyParser.Json.class)
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
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    public static Result closeCase() {
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
        Alarm.closeAlarm(a);

    	return ok();
    }
    
 
    
    
    @BodyParser.Of(BodyParser.Json.class)
    public static Result  assignAlarmFromJson(){
  	  JsonNode json = request().body().asJson();

	  String attendantUserName = json.findPath("attendant").textValue();
	  Long alarmId =  json.findPath("alarmId").asLong();
	  AlarmAttendant a = AlarmAttendant.getAttendantFromUsername(attendantUserName);
	  Alarm.assignAttendantToAlarm(alarmId, a);   
	  return ok();
    }
    
    
    // TODO: discontinue this function once the above is running and tested
/*    public static Result  assignAlarm(){
    	DynamicForm dynamicForm = Form.form().bindFromRequest();
    	String attendantUserName = dynamicForm.get("attendantUserName");
    	Long alarmId =  Long.parseLong(dynamicForm.get("alarmId"));
       	AlarmAttendant a = AlarmAttendant.getAttendantFromUsername(attendantUserName);
    	Alarm alarm = Alarm.assignAttendantToAlarm(alarmId, a);    	
    	List<Alarm> l = Global.alarmList.getAlarmList();
   	 	return ok(
			    views.html.index.render(l, alarmForm, alarm)
			  );

    }*/
    /* 
    public static Result  dispatchAlarm(){
    	DynamicForm dynamicForm = Form.form().bindFromRequest();
    	Long alarmId =  Long.parseLong(dynamicForm.get("alarmId"));
    	Alarm alarm = Alarm.dispatchAlarm(alarmId);    	
    	List<Alarm> l = Global.alarmList.getAlarmList();
   	 	return ok(
			    views.html.index.render(l, alarmForm, alarm)
			  );

    }*/
    

    
    
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
            Routes.javascriptRouter("myJsRoutes",
                //controllers.routes.javascript.Application.deleteAlarm(),
                controllers.routes.javascript.Application.getPastAlarmsFromCallee(),
                //controllers.routes.javascript.Application.assignAlarm(),
                controllers.routes.javascript.Application.closeCase(),
                controllers.routes.javascript.Application.saveCase(),
            	controllers.routes.javascript.Application.insertPatientFromJson(),
            	controllers.routes.javascript.Application.assignAlarmFromJson(),
                controllers.routes.javascript.Application.getCalleeFromAlarm(),
                controllers.routes.javascript.Application.getProspectPatients()
                //controllers.routes.javascript.Application.getOpenAlarm()
            )
        );
    }
    
    // get the ws.js script
    public static Result wsJs() {
        return ok(views.js.ws.render());
    }
    
    // Websocket interface
    public static WebSocket<JsonNode> wsInterface(){
        return new WebSocket<JsonNode>(){
            
            // called when websocket handshake is done
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                MyWebSocketManager.start(in, out);
            }
        };   
    }   
    
}
