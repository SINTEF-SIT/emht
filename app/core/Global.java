package core;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import models.Alarm;
import models.AlarmAttendant;
import play.Application;
import play.GlobalSettings;
import play.Play;
import play.libs.Yaml;

import com.avaje.ebean.Ebean;


public class Global extends GlobalSettings {

	public static LocalMonitor localMonitor;
	public static OpenAlarmList alarmList;
	// The API Key needed to use the Google Cloud Messaging Service
	public static String GCM_API_KEY;
	
    @Override
    public void onStart(Application app) {

        localMonitor = new LocalMonitor();
        alarmList = new OpenAlarmList();
		GCM_API_KEY = Play.application().configuration().getString("gcm.apikey");

    	// Check if the AlarmAttendant db is empty
        if (AlarmAttendant.find.findRowCount() == 0) {
            Ebean.save((List) Yaml.load("initial-data.yml"));
        }
        populateMemoryAlarmList();

    
    }
    
    //populates the list of open alarms which is cached in the memory
    void populateMemoryAlarmList(){
        List<Alarm> openAlarms = Alarm.allOpenAlarms();
    	Iterator<Alarm> iterator = openAlarms.iterator();
    	while (iterator.hasNext()) {
    		Alarm a  = iterator.next();
    		// setting the expiry flag
    		setExpiryFlagInAlarm(a);
    		alarmList.list.put(a.id, a);
    		System.out.println("Adding alarm " + a.id + "with expiry flag as " + a.expired); 
    		// TODO: test if removing the print does not affect the application
    	}
    }
    
    // set the expiry flag into the alarm
    void setExpiryFlagInAlarm(Alarm a){
    	if(a.closingTime != null) return; // that should not be called as we are dealing just with open alarms
    	//, but I leave just to make the method more generic
    	
    	if(a.dispatchingTime != null){// it has been dispatched
    		if( (System.currentTimeMillis() - a.dispatchingTime.getTime()) > LocalMonitor.resolutionTimeTreshold ){// now we check if the dispathing time is higher than the treshold 
    			a.expired = true;
    		}
			return;
    	}
    	
    	// then, if not dispatched nor closed
		if( (System.currentTimeMillis() - a.openingTime.getTime()) > LocalMonitor.assignmentTimeTreshold ){// now we check if the assignment time is higher than the treshold 
			a.expired = true;
		}
		return;
    	
    }

	/**
	 * Helper method that translates a Date object into an ISO8601 format
	 * @param date A Date instance
	 * @return The Date instance represented as ISO8601
	 */
	public static String formatDateAsISO (Date date) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(tz);
		String ISOtime = df.format(date);
		return ISOtime;
	}
}
