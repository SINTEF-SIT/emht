package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.ApiKey;
import controllers.auth.Authorization;
import models.AlarmAttendant;
import play.Logger;
import play.data.Form;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import java.util.Map;

@Security.Authenticated(Authorization.Authorized.class)
public class AttendantController extends Controller {

	static Form<AlarmAttendant> attendantForm = Form.form(AlarmAttendant.class);
	static Form<ApiKey> apiKeyForm = Form.form(ApiKey.class);

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
		return ok(views.html.attendantAdmin.render(AlarmAttendant.all(), attendantForm, ApiKey.all(), apiKeyForm));
	}

	/**
	 * New attendant creation controller of the /attendants endpoint
	 * @return A Result object containing the HTTP response
	 */
	@Authorization.PrivilegeLevel(Authorization.ADMINISTRATOR)
	public static Result newAttendant() {

		Form<AlarmAttendant> filledForm = attendantForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(views.html.attendantAdmin.render(AlarmAttendant.all(), attendantForm, ApiKey.all(), apiKeyForm));
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

	/**
	 * ApiKey creation controller of the /attendants endpoint
	 * @return A Result object containing the HTTP response
	 */
	@Authorization.PrivilegeLevel(Authorization.ADMINISTRATOR)
	public static Result newApiKey() {
		ApiKey a = new ApiKey();
		AlarmAttendant attendant = AlarmAttendant.getAttendantFromUsername(
			request().body().asFormUrlEncoded().get("user")[0]
		);
		a.user = attendant;
		ApiKey.create(a);

		return redirect(controllers.routes.AttendantController.attendants());

	}

	/**
	 * ApiKey deleteion controller of the /attendants endpoint
	 * @param id The ID of the ApiKye
	 * @return A Result object containing the HTTP response
	 */
	@Authorization.PrivilegeLevel(Authorization.ADMINISTRATOR)
	public static Result deleteApiKey(Long id) {
		ApiKey.find.byId(id).delete();
		return redirect(controllers.routes.AttendantController.attendants());
	}

	/**
	 * Endpoint for the Android App allowing it to update itself with the provided Google Cloud Messaging
	 * client key, which is needed when we are notifying the field operators of new alarms.
	 * @return 200 OK or 404 NOT FOUND if non-existing AlarmAttendant
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result setGcmRegId() {
		JsonNode content = request().body().asJson();
		AlarmAttendant fieldOperator = AlarmAttendant.find.byId(Long.parseLong(session().getOrDefault("id", "0")));
		Logger.debug("Received gcmRegId set req from " + fieldOperator.username);
		if (fieldOperator == null || fieldOperator.id == 0) return notFound();

		fieldOperator.gcmRegId = content.get("gcmRegId").asText();
		fieldOperator.save();

		return ok(AlarmAttendant.toJson(fieldOperator));
	}
}
