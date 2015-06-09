package controllers.auth;

import controllers.routes;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

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
         * This method additionally checks for an authorization token in the form of a
         * Authorization header bearer key.
         *
         * @param ctx Request context provided by Play
         * @return The username of the logged in user or otherwise authorized entity, null if not logged in.
         */
        @Override
        public String getUsername(Http.Context ctx) {
            String user = ctx.session().get("username");
            // Do an API-KEY check
            if (user == null) {
                // TODO: Implement API-Key support
            }

            return user;
        }

        @Override
        public Result onUnauthorized(Http.Context ctx) {
            return redirect(routes.Application.login());
        }
    }
}
