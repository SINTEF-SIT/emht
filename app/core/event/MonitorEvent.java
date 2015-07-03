package core.event;

import models.Alarm;
import models.AlarmAttendant;
import models.Patient;

import javax.annotation.Nullable;

/**
 * Created by Aleksander Skraastad (myth) on 7/3/15.
 */
public class MonitorEvent extends Event {
    public MonitorEvent(EventType type, @Nullable Alarm alarm,
                        @Nullable AlarmAttendant attendant, @Nullable Patient patient) {

        super(type, alarm, attendant, patient);
    }
}
