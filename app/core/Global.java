package core;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import core.event.EventHandler;
import models.Alarm;
import models.AlarmAttendant;
import monitor.LocalMonitor;
import play.Application;
import play.GlobalSettings;
import play.Play;
import play.libs.Yaml;

import com.avaje.ebean.Ebean;


public class Global extends GlobalSettings {

	public static EventHandler event;
	public static LocalMonitor localMonitor;
	// The API Key needed to use the Google Cloud Messaging Service
	public static String GCM_API_KEY;
	
    @Override
    public void onStart(Application app) {
		GCM_API_KEY = Play.application().configuration().getString("gcm.apikey");

    	// Check if the AlarmAttendant db is empty
        if (AlarmAttendant.find.findRowCount() == 0) {
            Ebean.save((List) Yaml.load("initial-data.yml"));
        }

    	// Fire up the event handler
		event = EventHandler.getInstance();
		event.start();

		// Initialize the local monitor and register it to the event handler
		localMonitor = new LocalMonitor();
		event.addEventListener(localMonitor);
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
}
