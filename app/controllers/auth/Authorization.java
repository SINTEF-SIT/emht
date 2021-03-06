package controllers.auth;

import controllers.routes;

import models.AlarmAttendant;
import models.ApiKey;
import play.Logger;
import play.cache.Cache;
import play.libs.F;
import play.mvc.*;
import scala.collection.immutable.Map;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.NoSuchElementException;

/**
 * Created by Aleksander Skraastad (myth) on 6/8/15.
 */
public class Authorization {

    // ACL levels
    public static final int ADMINISTRATOR = 0;
    public static final int API_FULL = 1;
    public static final int ATTENDANT = 2;
    public static final int FIELD_OPERATOR = 3;
    public static final int API_MEDIUM = 4;
    public static final int API_BASIC = 5;

    /**
     * PrivilegeLevel annotation can be used on controller methods that should
     * specify different access levels.
     */
    @With(PrivilegeLevelAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrivilegeLevel {
        int value() default FIELD_OPERATOR;
    }

    /**
     * Action performed when using the PrivilegeLevel annotation
     */
    public static class PrivilegeLevelAction extends Action<PrivilegeLevel> {
        @Override
        public F.Promise<SimpleResult> call(Http.Context ctx) throws Throwable {
            Long role = Long.parseLong(ctx.session().getOrDefault("role", "999"));
            // Perform the ACL check
            if (role > configuration.value()) {
                Logger.debug("Failed authorization for user ID " + ctx.session().get("id") + " on " + ctx.request().uri());
                return F.Promise.pure(unauthorized("Insufficient access privileges."));
            }

            return delegate.call(ctx);
        }
    }

    /**
     * The Authorized class is a Play Authenticator extension
     * that allows decoration of actions.
     */
    public static class Authorized extends Security.Authenticator {
        /**
         * Base invocation method of the Authenticator decorator. A return value of
         * null indicates that a user is not logged in, and onUnauthorized will be
         * triggered.
         *
         * @param ctx Request context provided by Play
         * @return The username of the logged in user or otherwise authorized entity, null if not logged in.
         */
        @Override
        public String getUsername(Http.Context ctx) {

            String id = ctx.session().getOrDefault("id", null);

            // Do an API-KEY check and return API-key canonical name if valid
            if (id == null) {
                Map headerMap = ctx._requestHeader().headers().toSimpleMap();
                String headerApiKey;
                try {
                    headerApiKey = (String) headerMap.get("Authorization").get();
                } catch (NoSuchElementException e) {
                    headerApiKey = "";
                }

                String[] keyParts = headerApiKey.split(" ");

                if (keyParts.length != 2) return null;
                else headerApiKey = keyParts[1];

                if (headerApiKey != null && !headerApiKey.equals("")) {
                    ApiKey a = ApiKey.find.where().eq("key", headerApiKey).findUnique();

                    if (a == null || a.id < 1) return null;
                    else {
                        AlarmAttendant user = AlarmAttendant.get(a.user.id);
                        if (user == null) {
                            ctx.session().clear();
                            id = null;
                        } else {
                            // Update cache if server has restarted
                            if (Cache.get(user.id.toString()) == null) Cache.set(user.id.toString(), user);
                            id = user.id.toString();
                            ctx.session().put("role", Long.toString(user.role));
                        }
                    }
                }
            } else {
                // TODO: For now, just check if user still exists in database until a proper hook
                // for per-user session invalidation on user delete is implemented.
                AlarmAttendant user = AlarmAttendant.get(Long.parseLong(id));
                if (user == null) {
                    ctx.session().clear();
                    id = null;
                } else {
                    // Update cache if server has restarted
                    if (Cache.get(id) == null) Cache.set(id, user);
                }
            }

            return id;
        }

        @Override
        public Result onUnauthorized(Http.Context ctx) {
            return redirect(routes.Application.login());
        }
    }
}
