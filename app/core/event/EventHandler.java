package core.event;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 7/3/15.
 */
public class EventHandler extends Thread {
    private static boolean _invoked = false;
    private static EventHandler _singleton = null;
    private HashSet<EventListener> listeners;
    private LinkedBlockingQueue<Event> eventQueue;

    /**
     * Private constructor, since we are using Singleton pattern
     */
    private EventHandler() {
        listeners = new LinkedHashSet<>();
        eventQueue = new LinkedBlockingQueue<>();
        _invoked = true;
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
     * Register an object as an event listener
     * @param e An object implementing the EventListener interface
     */
    public synchronized void addEventListener(EventListener e, Set events) {
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
     *
     * @param e
     */
    public static void dispatch(Event e) {
        try {
            getInstance().eventQueue.put(e);
        } catch (InterruptedException interrupt) {
            interrupt.printStackTrace();
        }
    }

    /**
     * Helper to fire an event
     * @param e The Event to process
     */
    private void fireEvent(Event e) {
        for (EventListener listener : listeners) {
            if (listener.listenFor().contains(e.getType())) {
                listener.newEvent(e);
            }
        }
    }
}
