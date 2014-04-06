package controllers;

import java.text.SimpleDateFormat;
import java.util.List;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    	 return ok(
    			    views.html.index.render(Alarm.allOpenAlarms(), alarmForm, null)
    			  );
    }
    
    public static Result  newAlarm(){
    	  Form<Alarm> filledForm = alarmForm.bindFromRequest(); // create a new form with the request data
    	  if(filledForm.hasErrors()) {
    	    return badRequest(
    	      views.html.index.render(Alarm.allOpenAlarms(), filledForm, null)
    	    );
    	  } else {
    		  Alarm formAlarm = filledForm.get();
    		if(null != formAlarm.callee && null != formAlarm.patient){
    			formAlarm.callee = Callee.getOrCreate(formAlarm.callee);
    			formAlarm.patient = Patient.getOrCreate(formAlarm.patient);
    			Alarm.create(formAlarm);//
    		}	
    		else{
    			System.out.println("calle was not found in the form");
    			// TODO: show error because callee was not set
    		}
    	    
    	    return redirect(routes.Application.openAlarms());  
    	  }
    }
    
    public static Result  deleteAlarm(Long id){
    	  Alarm.delete(id);
    	  return redirect(routes.Application.openAlarms());
    }
    
    public static Result  getAlarm(Long id){
   	 	return ok(
			    views.html.requestInfoFrame.render(Alarm.get(id))
			  );
    }
    
    public static Result  getPastAlarmsFromCallee(Long calleeId){
    	List<Alarm> alarmList = Alarm.pastAlarmsFromCallee(calleeId);
    	
    	SimpleDateFormat ddMMyy = new SimpleDateFormat ("dd/MM yyyy");
    	SimpleDateFormat hhMin = new SimpleDateFormat ("hh:mm");
    	

		ObjectNode result = Json.newObject();

		ArrayNode alarmArray = new ArrayNode(JsonNodeFactory.instance);
    	for (Alarm temp : alarmList) {
    		ObjectNode  alarm = Json.newObject();
			alarm.put("day", ddMMyy.format(temp.closingTime));
    		alarm.put("hour", hhMin.format(temp.closingTime));
    		alarmArray.add(alarm);
    	}
    	result.put("alarmArray",alarmArray);
    	return ok(result);

    }
    
    public static Result  assignAlarm(){
    	DynamicForm dynamicForm = Form.form().bindFromRequest();
    	String attendantUserName = dynamicForm.get("attendantUserName");
    	Long alarmId =  Long.parseLong(dynamicForm.get("alarmId"));
       	AlarmAttendant a = AlarmAttendant.getAttendantFromUsername(attendantUserName);
    	Alarm alarm = Alarm.assignAttendantToAlarm(alarmId, a);    	

   	 	return ok(
			    views.html.index.render(Alarm.allOpenAlarms(), alarmForm, alarm)
			  );

    }
    
    
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
            Routes.javascriptRouter("myJsRoutes",
                controllers.routes.javascript.Application.deleteAlarm(),
                controllers.routes.javascript.Application.getPastAlarmsFromCallee(),
                controllers.routes.javascript.Application.assignAlarm(),
                controllers.routes.javascript.Application.getAlarm()
            )
        );
    }
    
    
    
}
