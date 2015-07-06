package core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.AlarmAttendant;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import play.mvc.Result;

import static play.mvc.Controller.badRequest;

/**
 * Created by Aleksander Skraastad (myth) on 7/6/15.
 */
public class GoogleCloudMessaging {
    public static final String GCM_ENDPOINT = "https://android.googleapis.com/gcm/send";
    public static String ALARM_DISPATCHED = "sintef.android.emht.gcm.new_assignment";

    /**
     * Helper method that performs the POST request to the GCM API
     * @param message A JSON encoded payload containing registration IDS for the client and data message
     */
    private static F.Promise<WS.Response> pushMessage(JsonNode message) {
        F.Promise<WS.Response> response = WS.url(GCM_ENDPOINT)
            .setHeader("Authorization", "key=" + Global.GCM_API_KEY)
            .post(message);

        return response;
    }

    /**
     * Notifies a Field Operator that he/she has been assigned a new alarm.
     * @param attendant The AlarmAttendant object representing the field operator
     */
    public static F.Promise<WS.Response> dispatchAlarm(AlarmAttendant attendant) {
        if (attendant.gcmRegId == null) {
            return null;
        }
        ObjectNode payload = Json.newObject();
        ArrayNode regIds = payload.putArray("registration_ids");
        regIds.add(attendant.gcmRegId);
        ObjectNode data = Json.newObject();
        data.put("message", ALARM_DISPATCHED);
        payload.put("data", data);

        Logger.debug("GCM Dispatch alarm notification: " + payload.toString());

        return pushMessage(payload);
    }
}
