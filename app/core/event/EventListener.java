package core.event;

import java.util.Set;

/**
 * Created by Aleksander Skraastad (myth) on 7/3/15.
 */
public interface EventListener {
    /**
     * The method that will be triggered when a new event occurs
     * @param e The Event object containing the relevant data
     */
    void newEvent(Event e);

    /**
     * Must return a Set of all EventTypes this listener is interested in receiving events for
     * @return A Set of EventTypes
     */
    Set<EventType> listenFor();
}
