package monitor.external.ibm;

import com.fasterxml.jackson.databind.JsonNode;
import core.event.EventHandler;
import core.event.EventType;
import core.event.MonitorEvent;
import models.Alarm;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Created by Aleksander Skraastad (myth) on 7/10/15.
 */
public class IBMController extends Controller {

    /**
     * Endpoint for receiving callbacks from the IBM Monitor
     * @return A Result object HTTP Response
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result monitorCallback() {
        JsonNode message = request().body().asJson();

        Long alarmId = message.get("affectedAssetId").asLong();
        Alarm a = Alarm.get(alarmId);

        String type = message.get("misbehaviourTypeId").asText();
        if (a != null && a.id != 0) {
            switch (type) {
                case "LateAssignment":
                    EventHandler.dispatch(new MonitorEvent(EventType.ALARM_OPEN_EXPIRED, a, null, null));
                    break;
                case "LateClosing":
                    EventHandler.dispatch(new MonitorEvent(EventType.ALARM_RESOLUTION_EXPIRED, a, null, null));
                    break;
            }
        }

        return ok();
    }
}
