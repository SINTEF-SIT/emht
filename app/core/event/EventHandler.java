package core.event;

import play.Logger;
import play.Play;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.LinkedBlockingQueue;
import static core.event.EventType.*;

/**
 * Created by Aleksander Skraastad (myth) on 7/3/15.
 */
public class EventHandler extends Thread {
    private static boolean _invoked = false;
    private static boolean _running = false;
    private static EventHandler _singleton = null;
    private HashSet<EventListener> listeners;
    private LinkedBlockingQueue<Event> eventQueue;
    private boolean supressMonitorEvents = false;

    /**
     * Private constructor, since we are using Singleton pattern
     */
    private EventHandler() {
        listeners = new LinkedHashSet<>();
        eventQueue = new LinkedBlockingQueue<>();
        _invoked = true;
        supressMonitorEvents = Play.application().configuration().getBoolean("events.output.supressmonitorstatsevents");
    }

    /**
     * Singleton based object factory
     * @return The EventHandler instance
     */
    public static EventHandler getInstance() {
        if (!_invoked) _singleton = new EventHandler();
        return _singleton;
    }

    /**
     * Main Event thread run loop
     */
    @Override
    public void run() {
        _running = true;
        Logger.info("[EVENT] EventHandler started");
        while (_running) {
            try {
                fireEvent(eventQueue.take());
            } catch (InterruptedException e) {
                Logger.warn("Event thread got interrupted while fetching next event from queue");
            }
        }
        Logger.info("[EVENT] EventHandler stopped");
    }

    /**
     * Register an object as an event listener
     * @param e An object implementing the EventListener interface
     */
    public synchronized void addEventListener(EventListener e) {
        listeners.add(e);
    }

    /**
     * Unregister an object as an event listener
     * @param e An object implementing the EventListener interface
     */
    public synchronized void removeEventListener(EventListener e) {
        listeners.remove(e);
    }

    /**
     * Main method for signaling events
     * @param e The Event object that has occurred
     */
    public static void dispatch(Event e) {
        try {
            getInstance().eventQueue.put(e);
        } catch (InterruptedException interrupt) {
            Logger.warn("Interrupted while attempting to put an event into the queue.");
        }
    }

    /**
     * Helper to fire an event
     * @param e The Event to process
     */
    private void fireEvent(Event e) {
        if (e.getType() != MONITOR_STATISTICS || !supressMonitorEvents) {
            Logger.debug("[EVENT] Firing " + e);
        }
        for (EventListener listener : listeners) {
            if (listener.listenFor().contains(e.getType())) {
                listener.newEvent(e);
            }
        }
        // SYSTEM EVENTS
        if (e.getType() == SYSTEM_SHUTDOWN) {
            _running = false;
        }
    }
}
