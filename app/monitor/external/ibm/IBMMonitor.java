package monitor.external.ibm;

import core.event.Event;
import monitor.AbstractMonitor;
import monitor.MonitorStatistics;
import play.Logger;

/**
 * Created by Aleksander Skraastad (myth) on 7/10/15.
 */
public class IBMMonitor extends AbstractMonitor {

    private MonitorStatistics stats;

    public IBMMonitor() {
        stats = new MonitorStatistics();
        Logger.info("[MONITOR] IBM Monitor started");
    }

    @Override
    protected MonitorStatistics getStats() {
        return stats;
    }

    @Override
    protected void handleAlarmAssessmentSet(Event e) {

    }

    @Override
    protected void handleAlarmAssigned(Event e) {

    }

    @Override
    protected void handleAlarmNew(Event e) {

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

    @Override
    protected void handleAlarmOpenExpired(Event e) {

    }

    @Override
    protected void handleAlarmResolutionExpired(Event e) {

    }

    @Override
    protected void handleAlarmClosed(Event e) {

    }

    @Override
    protected void handleAlarmDispatched(Event e) {

    }

    @Override
    protected void handleAlarmFinished(Event e) {

    }

    @Override
    protected void handlePatientNew(Event e) {

    }

    @Override
    protected void handleSystemShutdown(Event e) {

    }
}
