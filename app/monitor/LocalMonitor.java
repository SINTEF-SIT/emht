package monitor;

import core.Global;
import core.MyWebSocketManager;
import core.event.Event;
import models.Alarm;
import play.Logger;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Aleksander Skraastad (myth) on 7/7/15.
 */
public class LocalMonitor extends AbstractMonitor {

    private static final Long ASSIGNMENT_TIME_THRESHOLD = 30L * 1000L;          // 30 Seconds
    private static final Long RESOLUTION_TIME_THRESHOLD = 120L * 60L * 1000L;   // 2 Hours

    private Timer timer;
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

        stats.incrementTotalAssignmentWaitingTimeBy(responseTime);
        if (responseTime > ASSIGNMENT_TIME_THRESHOLD) stats.incrementTotalIncidentsAboveAssignmentThreshold();

        if (t != null) {
            t.cancel();
            tasks.remove(t.alarmId);
        }

        String debug = "[MONITOR] Alarm " + e.getAlarm().id + " was assigned. ";
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

        Logger.debug("[MONITOR] Removed Alarm " + e.getAlarm().id + " from tasks.");
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

            Logger.debug("[MONITOR] Scheduled Alarm " + e.getAlarm().id + " for resolution expiry in " +
            RESOLUTION_TIME_THRESHOLD / 1000 + " seconds.");
        }
    }

    /**
     * Handles Events of type ALARM_FINISHED
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handleAlarmFinished(Event e) {

    }

    /**
     * Handles Events of type PATIENT_NEW
     * @param e Event object containing relevant data for the event type
     */
    @Override
    protected void handlePatientNew(Event e) {

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

            Logger.debug("[MONITOR] Triggering AssignmentReminder for Alarm " + alarmId);

            MyWebSocketManager.getInstance().addTimeIconToAlarm(alarmId);

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
            MyWebSocketManager.getInstance().addTimeIconToAlarm(alarmId);

            Logger.debug("[MONITOR] Triggering ResolutionReminder for Alarm " + alarmId);

            mon.stats.incrementTotalIncidentsAboveResolutionThreshold();
        }
    }

    /**
     * Support class that maintains statistics and provides statistics calculations
     */
    public static class MonitorStatistics {

        private Integer totalIncidents;
        private Integer totalIncidentsAboveAssignmentThreshold;
        private Integer totalIncidentsAboveResolutionThreshold;
        private Long totalAssignmentWaitingTime;
        private Long totalResolutionWaitingTime;
        private Long maximumAssignmentTime;

        public MonitorStatistics() {
            this.totalIncidents = 0;
            this.totalIncidentsAboveAssignmentThreshold = 0;
            this.totalIncidentsAboveResolutionThreshold = 0;
            this.totalAssignmentWaitingTime = 0L;
            this.totalResolutionWaitingTime = 0L;
            this.maximumAssignmentTime = 0L;
        }


        public Integer getTotalIncidents() {
            return totalIncidents;
        }

        public Integer getTotalIncidentsAboveAssignmentThreshold() {
            return totalIncidentsAboveAssignmentThreshold;
        }

        public Integer getTotalIncidentsAboveResolutionThreshold() {
            return totalIncidentsAboveResolutionThreshold;
        }

        public Long getTotalAssignmentWaitingTime() {
            return totalAssignmentWaitingTime;
        }

        public Long getTotalResolutionWaitingTime() {
            return totalResolutionWaitingTime;
        }

        public Long getMaximumAssignmentTime() {
            return maximumAssignmentTime;
        }

        public Double getAverageResponseTime() {
            if (totalAssignmentWaitingTime == 0) return 0d;
            return totalIncidents.doubleValue() / totalAssignmentWaitingTime.doubleValue();
        }

        public Double getAverageResolutionTime() {
            if (totalResolutionWaitingTime == 0) return 0d;
            return totalIncidents.doubleValue() / totalResolutionWaitingTime.doubleValue();
        }

        public void incrementTotalIncidents() {
            this.totalIncidents++;
        }

        public void incrementTotalIncidentsAboveAssignmentThreshold() {
            this.totalIncidentsAboveAssignmentThreshold++;
        }

        public void incrementTotalIncidentsAboveResolutionThreshold() {
            this.totalIncidentsAboveResolutionThreshold++;
        }

        public void incrementTotalAssignmentWaitingTimeBy(Long amount) {
            this.totalAssignmentWaitingTime += amount;
            if (amount > maximumAssignmentTime) maximumAssignmentTime = amount;
        }

        public void incrementTotalResolutionWaitingTimeBy(Long amount) {
            this.totalResolutionWaitingTime += amount;
        }
    }
}
