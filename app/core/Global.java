package core;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import core.event.Event;
import core.event.EventHandler;
import core.event.EventType;
import core.event.MonitorEvent;
import models.Alarm;
import models.AlarmAttendant;
import monitor.AbstractMonitor;
import monitor.LocalMonitor;
import monitor.external.ibm.IBMMonitor;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.libs.Yaml;

import com.avaje.ebean.Ebean;


public class Global extends GlobalSettings {

	public static EventHandler event;
	public static LocalMonitor localMonitor;
	public static IBMMonitor externalMonitor;
	// The API Key needed to use the Google Cloud Messaging Service
	public static String GCM_API_KEY;
	
    @Override
    public void onStart(Application app) {
		GCM_API_KEY = Play.application().configuration().getString("gcm.apikey");

    	// Check if the AlarmAttendant db is empty
        if (AlarmAttendant.find.findRowCount() == 0) {
            Ebean.save((List) Yaml.load("initial-data.yml"));
        }

		// Due to dynamic reloading, we need to check this
		if (event == null) {
			Logger.debug("[SYSTEM] OnStart debug: EventHandler was null, initializing...");
			// Fire up the event handler
			event = EventHandler.getInstance();
			event.start();
		}

		// Due to dynamic reloading, we need to check this
		if (localMonitor == null) {
			Logger.debug("[SYSTEM] OnStart debug: LocalMonitor was null, initializing...");
			// Initialize the local monitor and register it to the event handler
			localMonitor = new LocalMonitor();
			event.addEventListener(localMonitor);
		}

		/* UNCOMMENT this and comment out localMonitor to enable IBM External Monitor
		// Due to dynamic reloading, we need ot check this
		if (externalMonitor == null) {
			Logger.debug("[SYSTEM] OnStart debug: ExternalMonitor was null, initializing...");
			externalMonitor = new IBMMonitor();
			event.addEventListener(externalMonitor);
		}*/
    }

	@Override
	public void onStop(Application app) {
		// We need to signal that we are stopping / restarting to allow threads to shut down.
		EventHandler.dispatch(new MonitorEvent(EventType.SYSTEM_SHUTDOWN, null, null, null));
	}

	/**
	 * Helper method that translates a Date object into an ISO8601 format
	 * @param date A Date instance
	 * @return The Date instance represented as ISO8601
	 */
	public static String formatDateAsISO (Date date) {
		TimeZone tz = TimeZone.getDefault();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		df.setTimeZone(tz);
		return df.format(date);
	}

	/**
	 * If external monitor is null, we use the internal
	 * @return A Monitor object
	 */
	public static AbstractMonitor getMonitor() {
		if (externalMonitor == null) return localMonitor;
		return externalMonitor;
	}
}
