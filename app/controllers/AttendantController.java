package controllers;

import models.AlarmAttendant;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;

public class AttendantController extends Controller {

	static Form<AlarmAttendant> attendantForm = Form.form(AlarmAttendant.class);

	public static Result index() {
		return redirect(controllers.routes.AttendantController.attendants());
		//return ok(index.render("Your new application is ready."));
	}

	public static Result attendants(){
		return ok(
			views.html.attendantAdmin.render(AlarmAttendant.all(), attendantForm)
		);
	}

	public static Result newAttendant(){
		Form<AlarmAttendant> filledForm = attendantForm.bindFromRequest(); // create a new form with the request data
		if(filledForm.hasErrors()) {
			return badRequest(
				views.html.attendantAdmin.render(AlarmAttendant.all(), filledForm)
			);
		} else {
			AlarmAttendant.create(filledForm.get());//
			return redirect(controllers.routes.AttendantController.attendants());
		}
	}

	public static Result deleteAttendant(Long id){
		AlarmAttendant.delete(id);
		return redirect(controllers.routes.AttendantController.attendants());
	}

}
