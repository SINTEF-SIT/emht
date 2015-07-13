package monitor.external.ibm;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.event.Event;
import models.Alarm;
import monitor.AbstractMonitor;
import monitor.MonitorStatistics;
import play.Logger;
import play.Play;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

/**
 * Created by Aleksander Skraastad (myth) on 7/10/15.
 */
public class IBMMonitor extends AbstractMonitor {

    private MonitorStatistics stats;
    private HashSet<Long> assigned, dispatched;
    private String monitorHost;
    private Integer monitorPort;
    private String monitorEndpoint;
    private Integer monitorResponseTimeout;

    public IBMMonitor() {
        stats = new MonitorStatistics();
        assigned = new HashSet<>();
        dispatched = new HashSet<>();

        monitorHost = Play.application().configuration().getString("monitor.external.ibm.host");
        monitorPort = Play.application().configuration().getInt("monitor.external.ibm.port");
        monitorEndpoint = Play.application().configuration().getString("monitor.external.ibm.endpoint");
        monitorResponseTimeout = Play.application().configuration().getInt("monitor.external.ibm.timeout");

        Logger.info("[MONITOR] IBM Monitor started");
    }

    /* Helpers */

    /**
     * Sends a JSON encoded message to the IBM monitor reporting endpoint
     * @param message A JSON ObjectNode containing the message and data to be sent
     * @return A HTTP Response Promise
     */
    private F.Promise<WS.Response> reportEvent(ObjectNode message) {
        return WS.url(monitorHost + ":" + monitorPort.toString() + monitorEndpoint).post(message);
    }

    /**
     * Helper that creates a JSON Object node and fills in the required values needed by the IBM monitor.
     * @param a The Alarm object to report
     * @return A JSON ObjectNode containing the required fields for the request, 'name', 'sourceAssetId' and
     * 'sourceAssetType'
     */
    private ObjectNode preparePayload(Alarm a) {
        ObjectNode payload = Json.newObject();
        payload.put("name", "EmergencyHandling");
        payload.put("sourceAssetId", a.id.toString());
        payload.put("sourceAssetType", a.type);
        payload.put("detectionTime", IBMMonitor.toIBMDateTimeFormat(new Date()));

        return payload;
    }

    /**
     * Helper that transforms a Date object into a String literal of the format required by the
     * IBM Monitor report endpoint
     * @param d A Date instance
     * @return A String Date literal
     */
    public static String toIBMDateTimeFormat(Date d) {
        TimeZone tz = TimeZone.getDefault();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy-HH:mm:ss");
        sdf.setTimeZone(tz);
        return sdf.format(d);
    }

    /* Event handlers */

    /**
     * Retrieve the associated MonitorStatistics object for this Monitor
     * @return
     */
    @Override
    protected MonitorStatistics getStats() {
        return stats;
    }

    @Override
    protected void handleAlarmAssessmentSet(Event e) {

    }

    /**
     * Invoked by ALARM_OPEN_EXPIRED events triggered by the EMHT system on assignment
     * @param e An Event object containing relevant data for this event type
     */
    @Override
    protected void handleAlarmAssigned(Event e) {
        // If it is already in assigned for some reason, return
        if (assigned.contains(e.getAlarm().id)) return;
        else assigned.add(e.getAlarm().id);

        ObjectNode payload = preparePayload(e.getAlarm());
        payload.put("type", "ALARM_ASSIGNED");

        F.Promise<WS.Response> res = reportEvent(payload);
        if (res != null) {
            Logger.debug("[MONITOR] IBM Monitor response code: " + res.get(monitorResponseTimeout).getStatus());
        }

        Long responseTime = new Date().getTime() - e.getAlarm().openingTime.getTime();
        stats.incrementTotalAssignmentWaitingTimeBy(responseTime);
    }

    /**
     * Invoked by ALARM_OPEN_EXPIRED events triggered by the EMHT system on new alarm arrivals
     * @param e An Event object containing relevant data for this event type
     */
    @Override
    protected void handleAlarmNew(Event e) {
        ObjectNode payload = preparePayload(e.getAlarm());
        payload.put("type", "ALARM_NEW");

        F.Promise<WS.Response> res = reportEvent(payload);
        if (res != null) {
            Logger.debug("[MONITOR] IBM Monitor response code: " + res.get(monitorResponseTimeout).getStatus());
        }


        Logger.debug("[MONITOR] New alarm: " + e.getAlarm() + ". Starting timer.");
        stats.incrementTotalIncidents();
    }

    @Override
    protected void handleAlarmDelete(Event e) {

    }

    @Override
    protected void handleAlarmLocationVerified(Event e) {

    }

    @Override
    protected void handleAlarmPatientSet(Event e) {

    }

    @Override
    protected void handleAlarmFieldAssessmentSet(Event e) {

    }

    @Override
    protected void handleAlarmExternalFollowupNotify(Event e) {

    }

    /**
     * Invoked by ALARM_OPEN_EXPIRED events triggered by the IBMController
     * @param e An Event object containing relevant data for this event type
     */
    @Override
    protected void handleAlarmOpenExpired(Event e) {
        Logger.debug("[MONITOR] Triggering AssignmentReminder for " + e.getAlarm());

        stats.incrementTotalIncidentsAboveAssignmentThreshold();
    }

    /**
     * Invoked by ALARM_RESOLUTION_EXPIRED events triggered by the IBMController
     * @param e An Event object containing relevant data for this event type
     */
    @Override
    protected void handleAlarmResolutionExpired(Event e) {
        Logger.debug("[MONITOR] Triggering ResolutionReminder for " + e.getAlarm());

        stats.incrementTotalIncidentsAboveResolutionThreshold();
    }

    /**
     * Invoked by ALARM_OPEN_EXPIRED events triggered by the EMHT system on closing of alarms
     * @param e An Event object containing relevant data for this event type
     */
    @Override
    protected void handleAlarmClosed(Event e) {
        ObjectNode payload = preparePayload(e.getAlarm());
        payload.put("type", "ALARM_CLOSED");

        F.Promise<WS.Response> res = reportEvent(payload);
        if (res != null) {
            Logger.debug("[MONITOR] IBM Monitor response code: " + res.get(monitorResponseTimeout).getStatus());
        }

        assigned.remove(e.getAlarm().id);
        dispatched.remove(e.getAlarm().id);

        Long resolutionTime = new Date().getTime() - e.getAlarm().openingTime.getTime();
        stats.incrementTotalResolutionWaitingTimeBy(resolutionTime);

        Logger.debug("[MONITOR] " + e.getAlarm() + " was closed.");
    }

    /**
     * Invoked by ALARM_OPEN_EXPIRED events triggered by the EMHT system on dispatching of alarms
     * @param e An Event object containing relevant data for this event type
     */
    @Override
    protected void handleAlarmDispatched(Event e) {
        // If it is already in assigned for some reason, return
        if (dispatched.contains(e.getAlarm().id)) return;
        else dispatched.add(e.getAlarm().id);

        ObjectNode payload = preparePayload(e.getAlarm());
        payload.put("type", "ALARM_DISPATCHED");

        F.Promise<WS.Response> res = reportEvent(payload);
        if (res != null) {
            Logger.debug("[MONITOR] IBM Monitor response code: " + res.get(monitorResponseTimeout).getStatus());
        }

        if (e.getAlarm().mobileCareTaker != null) stats.incrementTotalIncidentsAssignedToFieldOperator();

        Logger.debug("[MONITOR] " + e.getAlarm() + " dispatched.");
    }

    /**
     * Invoked by ALARM_OPEN_EXPIRED events triggered by the EMHT system on notifications from field operators
     * that the alarm has been finished and is ready for closing or further dispatching.
     * @param e An Event object containing relevant data for this event type
     */
    @Override
    protected void handleAlarmFinished(Event e) {
        dispatched.remove(e.getAlarm().id);

        if (e.getAlarm().mobileCareTaker != null) {
            Long workingTime = new Date().getTime() - e.getAlarm().dispatchingTime.getTime();
            stats.incrementTotalFieldOperatorWorkingTimeBy(workingTime);

            Logger.debug("[MONITOR] " + e.getAlarm().mobileCareTaker.username + " finished " + e.getAlarm() +
                " in " + (workingTime / 1000) + " seconds.");
        }
    }

    @Override
    protected void handlePatientNew(Event e) {

    }

    @Override
    protected void handleSystemShutdown(Event e) {

    }
}
