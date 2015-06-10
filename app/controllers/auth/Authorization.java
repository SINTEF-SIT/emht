package controllers.auth;

import controllers.routes;

import models.AlarmAttendant;
import play.Logger;
import play.mvc.*;

import java.util.Map;

/**
 * Created by Aleksander Skraastad (myth) on 6/8/15.
 */
public class Authorization {

    /**
     * The Authorized class is a Play Authenticator extension
     * that allows decoration of actions.
     */
    public static class Authorized extends Security.Authenticator {
        /**
         * Base invokation method of the Authenticator decorator. A return value of
         * null indicates that a user is not logged in, and onUnauthorized will be
         * triggered.
         *
         * @param ctx Request context provided by Play
         * @return The username of the logged in user or otherwise authorized entity, null if not logged in.
         */
        @Override
        public String getUsername(Http.Context ctx) {

            String username = ctx.session().get("username");

            // Do an API-KEY check and return API-key canonical name if valid
            if (username == null) {
                // TODO: Implement API-Key support
                return null;
            } else {
                // TODO: For now, just check if user still exists in database until a proper hook
                // for per-user session invalidation on user delete is implemented.
                AlarmAttendant user = AlarmAttendant.getAttendantFromUsername(username);
                if (user == null) {
                    ctx.session().clear();
                    username = null;
                }
            }

            return username;
        }

        @Override
        public Result onUnauthorized(Http.Context ctx) {
            return redirect(routes.Application.login());
        }
    }
}
