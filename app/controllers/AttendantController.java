package controllers;

import controllers.auth.Authorization;
import models.AlarmAttendant;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@Security.Authenticated(Authorization.Authorized.class)
public class AttendantController extends Controller {

	static Form<AlarmAttendant> attendantForm = Form.form(AlarmAttendant.class);

	/**
	 * Main controller of the /attendants endpoint
	 * @return A Result object containing the HTTP response
	 */

	@Authorization.PrivilegeLevel(Authorization.ADMINISTRATOR)
	public static Result index() {
		return redirect(controllers.routes.AttendantController.attendants());
	}

	/**
	 * Listing controller of the /attendants endpoint
	 * @return A Result object containing the HTTP response
	 */
	@Authorization.PrivilegeLevel(Authorization.ADMINISTRATOR)
	public static Result attendants() {
		return ok(views.html.attendantAdmin.render(AlarmAttendant.all(), attendantForm));
	}

	/**
	 * New attendant creation controller of the /attendants endpoint
	 * @return A Result object containing the HTTP response
	 */
	@Authorization.PrivilegeLevel(Authorization.ADMINISTRATOR)
	public static Result newAttendant() {

		Form<AlarmAttendant> filledForm = attendantForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(views.html.attendantAdmin.render(AlarmAttendant.all(), filledForm));
		} else {
			AlarmAttendant.create(filledForm.get());
			return redirect(controllers.routes.AttendantController.attendants());
		}
	}

	/**
	 * Attendant deletion controller of the /attendants endpoint
	 * @return A Result object containing the HTTP response
	 */
	@Authorization.PrivilegeLevel(Authorization.ADMINISTRATOR)
	public static Result deleteAttendant(Long id) {
		AlarmAttendant.delete(id);
		return redirect(controllers.routes.AttendantController.attendants());
	}
}
