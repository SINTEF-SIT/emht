package monitor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.event.Event;
import core.event.EventHandler;
import core.event.EventType;
import core.event.MonitorEvent;
import models.Alarm;
import play.Logger;
import play.libs.Json;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Aleksander Skraastad (myth) on 7/7/15.
 */
public class LocalMonitor extends AbstractMonitor {

    private static final Long ASSIGNMENT_TIME_THRESHOLD = 30L * 1000L;          // 30 Seconds
    private static final Long RESOLUTION_TIME_THRESHOLD = 120L * 60L * 1000L;   // 2 Hours
    private static final Long STATISTICS_UPDATE_INTERVAL = 15L * 1000L;

    private Timer timer;
    private ScheduledExecutorService periodic;
    // Mapping between Alarm ID and TimerTask associated with it
    private HashMap<Long, TimerTask> tasks;
    private MonitorStatistics stats;

    /**
     * Default constructor
     */
    public LocalMonitor() {
        timer = new Timer(this.getClass().getName());
        tasks = new HashMap<>();
        stats = new MonitorStatistics();
        periodic = Executors.newSingleThreadScheduledExecutor();

        periodic.scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    EventHandler.dispatch(new MonitorEvent(EventType.MONITOR_STATISTICS, null, null, null));
                }
            },
            STATISTICS_UPDATE_INTERVAL,
            STATISTICS_UPDATE_INTERVAL,
            TimeUnit.MILLISECONDS
        );

        // We need to see if there are any open alarms in the database (Could be system crash and restart etc)
        for (Alarm a : Alarm.allOpenAlarms()) {
            stats.incrementTotalIncidents();
        }

        Logger.info("[MONITOR] LocalMonitor started");
    }

    /**
     * Fetch monitor statistics object
     * @return A MonitorStatistics object
     */
    @Override
    public MonitorStatistics getStats() {
        return stats;
    }

    /* METHODS INHERITED FROM AbstractMonitor */

    /**
     * Handles Events of type ALARM_ASSESSMENT_SET
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmAssessmentSet(Event e) {

    }

    /**
     * Handles Events of type ALARM_ASSIGNED
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmAssigned(Event e) {
        AssignmentReminderTask t = (AssignmentReminderTask) tasks.get(e.getAlarm().id);
        Long responseTime = new Date().getTime() - e.getAlarm().openingTime.getTime();

        Logger.debug("[MONITOR] Assignment time was: " + responseTime / 1000 + " seconds.");

        stats.incrementTotalAssignmentWaitingTimeBy(responseTime);

        if (t != null) {
            t.cancel();
            tasks.remove(t.alarmId);
        }

        String debug = "[MONITOR] " + e.getAlarm() + " was assigned. ";
        if (responseTime > ASSIGNMENT_TIME_THRESHOLD) debug += "Alarm was above assignment threshold.";
        if (t != null) debug += "Alarm was removed from timer tasks.";
        Logger.debug(debug);
    }

    /**
     * Handles Events of type ALARM_NEW
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmNew(Event e) {
        AssignmentReminderTask t = new AssignmentReminderTask(this, e.getAlarm().id);
        tasks.put(e.getAlarm().id, t);
        timer.schedule(t, ASSIGNMENT_TIME_THRESHOLD);

        Logger.debug("[MONITOR] Scheduling new alarm trigger in " + ASSIGNMENT_TIME_THRESHOLD / 1000 + " seconds.");

        stats.incrementTotalIncidents();
    }

    /**
     * Handles Events of type ALARM_DELETE
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmDelete(Event e) {

    }

    /**
     * Handles Events of type ALARM_LOCATION_VERIFIED
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmLocationVerified(Event e) {

    }

    /**
     * Handles Events of type ALARM_PATIENT_SET
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmPatientSet(Event e) {

    }

    /**
     * Handles Events of type ALARM_FIELD_ASSESSMENT_SET
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmFieldAssessmentSet(Event e) {

    }

    /**
     * Handles Events of type ALARM_EXTERNAL_NOTIFY_FOLLOWUP
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmExternalFollowupNotify(Event e) {

    }

    /**
     * Handles Events of type ALARM_OPEN_EXPIRED
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmOpenExpired(Event e) {
        // This is an event that the monitor itself will dispatch
    }

    /**
     * Handles Events of type ALARM_RESOLUTION_EXPIRED
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmResolutionExpired(Event e) {
        // This is an event that the monitor itself will dispatch
    }

    /**
     * Handles Events of type ALARM_CLOSED
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmClosed(Event e) {
        TimerTask t = tasks.get(e.getAlarm().id);
        if (t != null) {
            t.cancel();
        }
        Long resolutionTime = new Date().getTime() - e.getAlarm().openingTime.getTime();
        stats.incrementTotalResolutionWaitingTimeBy(resolutionTime);

        Logger.debug("[MONITOR] Removed " + e.getAlarm() + " from tasks.");
    }

    /**
     * Handles Events of type ALARM_DISPATCHED
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmDispatched(Event e) {
        if (tasks.get(e.getAlarm().id) == null){
            ResolutionReminderTask t = new ResolutionReminderTask(this, e.getAlarm().id);
            tasks.put(e.getAlarm().id, t);
            timer.schedule(t, RESOLUTION_TIME_THRESHOLD);

            if (e.getAlarm().mobileCareTaker != null) stats.incrementTotalIncidentsAssignedToFieldOperator();

            Logger.debug("[MONITOR] Scheduled " + e.getAlarm() + " for resolution expiry in " +
            RESOLUTION_TIME_THRESHOLD / 1000 + " seconds.");
        }
    }

    /**
     * Handles Events of type ALARM_FINISHED
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmFinished(Event e) {
        if (e.getAlarm().mobileCareTaker != null) {
            Long workingTime = new Date().getTime() - e.getAlarm().dispatchingTime.getTime();
            stats.incrementTotalFieldOperatorWorkingTimeBy(workingTime);

            Logger.debug("[MONITOR] " + e.getAlarm().mobileCareTaker.username + " finished " + e.getAlarm() +
                         " in " + (workingTime / 1000) + " seconds.");
        }
    }

    /**
     * Handles Events of type PATIENT_NEW
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handlePatientNew(Event e) {

    }

    /**
     * Handles Events of type SYSTEM_SHUTDOWN
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleSystemShutdown(Event e) {
        // We need to turn off our scheduled tasks and clear timers
        periodic.shutdown();
        for (Map.Entry<Long, TimerTask> task : tasks.entrySet()) {
            task.getValue().cancel();
        }
    }

    /* SUPPORT CLASSES */

    public static class AssignmentReminderTask extends TimerTask {
        Long alarmId;
        LocalMonitor mon;
        Instant created;

        public AssignmentReminderTask(LocalMonitor mon, Long alarmId) {
            this.alarmId = alarmId;
            this.mon = mon;
            this.created = Instant.now();
        }

        @Override
        public void run() {
            mon.tasks.remove(alarmId);
            Alarm expiredAlarm = Alarm.get(this.alarmId);
            expiredAlarm.expired = true;

            Logger.debug("[MONITOR] Triggering AssignmentReminder for " + expiredAlarm);

            // Trigger the event
            EventHandler.dispatch(new MonitorEvent(EventType.ALARM_OPEN_EXPIRED, expiredAlarm, null, null));

            mon.stats.incrementTotalIncidentsAboveAssignmentThreshold();
        }
    }

    public static class ResolutionReminderTask extends TimerTask {
        Long alarmId;
        LocalMonitor mon;
        Instant created;

        public ResolutionReminderTask(LocalMonitor mon, Long alarmId) {
            this.alarmId = alarmId;
            this.mon = mon;
            this.created = Instant.now();
        }

        @Override
        public void run() {
            mon.tasks.remove(alarmId);
            Alarm expiredAlarm = Alarm.get(this.alarmId);
            expiredAlarm.expired = true;

            Logger.debug("[MONITOR] Triggering ResolutionReminder for " + expiredAlarm);

            // Trigger the event
            EventHandler.dispatch(new MonitorEvent(EventType.ALARM_RESOLUTION_EXPIRED, expiredAlarm, null, null));

            mon.stats.incrementTotalIncidentsAboveResolutionThreshold();
        }
    }
}
