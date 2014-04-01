package controllers;

import models.*;
import play.*;
import play.data.*;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {

	static Form<Alarm> alarmForm = Form.form(Alarm.class);
	
    public static Result index() {
    	return redirect(routes.Application.alarms());
        //return ok(index.render("Your new application is ready."));
    }

    public static Result  alarms(){
    	 return ok(
    			    views.html.index.render(Alarm.all(), alarmForm)
    			  );
    }
    
    public static Result  newAlarm(){
    	  Form<Alarm> filledForm = alarmForm.bindFromRequest(); // create a new form with the request data
    	  if(filledForm.hasErrors()) {
    	    return badRequest(
    	      views.html.index.render(Alarm.all(), filledForm)
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
    	    
    	    return redirect(routes.Application.alarms());  
    	  }
    }
    
    public static Result  deleteAlarm(Long id){
    	  Alarm.delete(id);
    	  return redirect(routes.Application.alarms());
    }
    
    public static Result  getAlarm(Long id){
   	 	return ok(
			    views.html.requestInfoFrame.render(Alarm.get(id))
			  );
    }
    
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
            Routes.javascriptRouter("jsRoutes",
                controllers.routes.javascript.Application.deleteAlarm(),
                controllers.routes.javascript.Application.getAlarm()
            )
        );
    }
    
}
