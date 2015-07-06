package core.event;

import models.Alarm;
import models.AlarmAttendant;
import models.Patient;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Created by Aleksander Skraastad (myth) on 7/3/15.
 */
public abstract class Event {
    private EventType type;
    private Alarm alarm;
    private AlarmAttendant attendant;
    private Patient patient;
    private Instant timestamp;

    /**
     * Creates an Event with optional arguments. Not all events are directly Alarm related, thus AlarmAttendant
     * and Patient parameters need to be available for these (rare) cases.
     *
     * @param type EventType as specified in the EventType enum
     * @param alarm An Alarm object (can be null)
     * @param attendant An AlarmAttendant object (can be null)
     * @param patient A Patient object (can be null)
     */
    public Event(EventType type, @Nullable Alarm alarm,
                 @Nullable AlarmAttendant attendant, @Nullable Patient patient) {

        this.type = type;
        this.alarm = alarm;
        this.attendant = attendant;
        this.patient = patient;
        this.timestamp = Instant.now();
    }

    /**
     * Return the type of this event
     * @return An EventType
     */
    public EventType getType() {
        return type;
    }

    /**
     * Get the associated Alarm object of this event if any
     * @return An Alarm object, or null
     */
    public Alarm getAlarm() {
        return alarm;
    }

    /**
     * Get the associated AlarmAttendant object of this event if any
     * @return An AlarmAttendant object, or null
     */
    public AlarmAttendant getAttendant() {
        return attendant;
    }

    /**
     * Get the associated Patient object of this event if any
     * @return A Patient object, or null
     */
    public Patient getPatient() {
        return patient;
    }

    /**
     * Get the timestamp when this event was created
     * @return An Instant from the time this Event was created
     */
    public Instant getTimestamp() {
        return timestamp;
    }
}
