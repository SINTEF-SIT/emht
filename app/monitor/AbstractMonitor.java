package monitor;

import core.event.Event;
import core.event.EventListener;
import core.event.EventType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by Aleksander Skraastad (myth) on 7/7/15.
 */
public abstract class AbstractMonitor implements EventListener {

    private Set<EventType> interestSet;

    public AbstractMonitor() {
        // Monitors should be interested in all events by default
        interestSet = EnumSet.allOf(EventType.class);
    }

    /**
     * EventListener interface method. When receiving a new Event, the proper abstract event handler method
     * is invoked on the subclass.
     *
     * @param e The Event object containing the relevant data
     */
    @Override
    public void newEvent(Event e) {
        switch (e.getType()) {
            case ALARM_ASSESSMENT_SET:
                handleAlarmAssessmentSet(e);
                break;
            case ALARM_ASSIGNED:
                handleAlarmAssigned(e);
                break;
            case ALARM_NEW:
                handleAlarmNew(e);
                break;
            case ALARM_DELETE:
                handleAlarmDelete(e);
                break;
            case ALARM_LOCATION_VERIFIED:
                handleAlarmLocationVerified(e);
                break;
            case ALARM_PATIENT_SET:
                handleAlarmPatientSet(e);
                break;
            case ALARM_FIELD_ASSESSMENT_SET:
                handleAlarmFieldAssessmentSet(e);
                break;
            case ALARM_EXTERNAL_FOLLOWUP_NOTIFY:
                handleAlarmExternalFollowupNotify(e);
                break;
            case ALARM_OPEN_EXPIRED:
                handleAlarmOpenExpired(e);
                break;
            case ALARM_RESOLUTION_EXPIRED:
                handleAlarmResolutionExpired(e);
                break;
            case ALARM_CLOSED:
                handleAlarmClosed(e);
                break;
            case ALARM_DISPATCHED:
                handleAlarmDispatched(e);
                break;
            case ALARM_FINISHED:
                handleAlarmFinished(e);
                break;
            case PATIENT_NEW:
                handlePatientNew(e);
                break;
            case SYSTEM_SHUTDOWN:
                handleSystemShutdown(e);
                break;
        }
    }

    /**
     * EventListener interface method. Can be overridden in subclasses to only listen to a subset
     * of events available from the EventHandler / EventType enum
     * @return A Set of EventTypes this monitor is interested in
     */
    @Override
    public Set<EventType> listenFor() {
        return interestSet;
    }

    /* Abstract methods */

    protected abstract MonitorStatistics getStats();
    protected abstract void handleAlarmAssessmentSet(Event e);
    protected abstract void handleAlarmAssigned(Event e);
    protected abstract void handleAlarmNew(Event e);
    protected abstract void handleAlarmDelete(Event e);
    protected abstract void handleAlarmLocationVerified(Event e);
    protected abstract void handleAlarmPatientSet(Event e);
    protected abstract void handleAlarmFieldAssessmentSet(Event e);
    protected abstract void handleAlarmExternalFollowupNotify(Event e);
    protected abstract void handleAlarmOpenExpired(Event e);
    protected abstract void handleAlarmResolutionExpired(Event e);
    protected abstract void handleAlarmClosed(Event e);
    protected abstract void handleAlarmDispatched(Event e);
    protected abstract void handleAlarmFinished(Event e);
    protected abstract void handlePatientNew(Event e);
    protected abstract void handleSystemShutdown(Event e);
}
