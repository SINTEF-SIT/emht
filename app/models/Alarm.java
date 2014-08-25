package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import core.Global;
import core.MyWebSocketManager;

import play.db.ebean.Model;
import play.mvc.Result;

@Entity
public class Alarm extends Model { // the model extension serves for having access to Play built-in Ebean helper, such as the Finder

	@Id
	public Long id;
	
	public String type;
	
	@ManyToOne(cascade=CascadeType.ALL)  
	public Callee callee;
	public Date openingTime;
	public Date dispatchingTime; // TODO: implement dispatching
	public Date closingTime; // at the moment we are dispatching and closing all alarms
	
	public String occuranceAddress; // address of where the incident took place
	
	@Transient 
	public boolean expired = false; 
	
	@ManyToOne(cascade=CascadeType.ALL)  
	public AlarmAttendant attendant;
	
	@Lob
	public String alarmLog;
	
	@Lob
	public String notes;
	
	@ManyToOne(cascade=CascadeType.ALL)
	public Patient patient;
	
	  public static Finder<Long,Alarm> find = new Finder(
	    Long.class, Alarm.class
	  );
	  

	  
	  public static List<Alarm> all() {
		  return find.orderBy("openingTime desc").findList();
		}
	  
	  public static List<Alarm> allOpenAlarms() {
		  return find.where().isNull("closingTime").orderBy("openingTime asc").findList();
		}

	    // assumes that the calle and patient object from the incoming alarm has already
	    // been saved on the db
		public static Alarm create(Alarm alarm) {
			alarm.openingTime = new Date();
			alarm.save();
			Global.alarmList.list.put(alarm.id, alarm);
			Global.localMonitor.registerNewAlert(alarm.id);
			MyWebSocketManager.notifyNewAlarm(alarm);
			return alarm;
		}

		public static void delete(Long id) {
		  find.ref(id).delete();
			Global.alarmList.list.remove(id);
		}
	  
		public static Alarm get(Long id) {
			  return find.ref(id);
		}
		
	    // TODO: investigate if it makes sense to explicitly tell that im not loading subobjects
	    public static List<Alarm>  pastAlarmsFromCallee(Long calleeId){
	    	return find.where().eq("callee.id",calleeId).isNotNull("closingTime").orderBy("openingTime desc").findList();
	    }
	    // TODO: investigate if it makes sense to explicitly tell that im not loading subobjects
	    public static List<Alarm>  pastAlarmsFromPatient(Long patientId){
	    	return find.where().eq("patient.id",patientId).isNotNull("closingTime").orderBy("openingTime desc").findList();
	    }
		
	    public static Alarm assignAttendantToAlarm(Long alarmId, AlarmAttendant attendant){
	    	Alarm a = find.ref(alarmId);
	    	a.attendant = attendant;
	    	a.save();
	    	Global.alarmList.list.put(a.id, a);// it will replace the alarm in the list with a new one with attendant and
	    	// clean expired flag
	    	Global.localMonitor.registerAssignment(alarmId);
	    	// TODO: possibly add checks
	    	// TODO: add websocket call in the case of a real multi-user
	    	return a;
	    }
	  


	    
	    // receives some data in the dummy object a, and updates the data from A
	    // which is not yet in the database into an mirror from the DB object to
	    // be saved by the function calling this one
	    // the dummy object must contain at least the alarm id
	    private static Alarm updateFromDummy(Alarm dummy){
	    	Alarm a = Alarm.get(dummy.id);
	    	if(null != dummy.patient){
	    		if(null == a.patient || (a.patient.id != dummy.patient.id)){ // no patient assigned to alarm, or different patient assgined
	    			Patient p = Patient.getFromId(dummy.patient.id);
	    			a.patient = p;
	    		}
	    	}
	    	if(null != dummy.notes) // Im assuming Ill alwasy update the notes
	    		a.notes = dummy.notes; 
	    	
	    	if(null != dummy.occuranceAddress) // Im assuming Ill alwasy update the address
	    		a.occuranceAddress = dummy.occuranceAddress; 
	    	
	    	// for the time fields, Ill not update them if they have already been set
	    	if(null != dummy.dispatchingTime && null == a.dispatchingTime)
	    		a.dispatchingTime = dummy.dispatchingTime; 
	    	
	    	if(null != dummy.closingTime && null == a.closingTime)
	    		a.closingTime = dummy.closingTime; 
	    	
	    	return a;	
	    }
	    
	    public static void saveAlarm(Alarm dummy){
	    	Alarm a = Alarm.updateFromDummy(dummy);
	    	a.save();
	    	return;
	    }
	    
	    public static void saveAndFollowupAlarm(Alarm dummy){
	    	Date dispatchTime = new Date();
	    	dummy.dispatchingTime = dispatchTime;
	    	// TODO: we should not update the dispatching time, if an incident is being save for followup
	    	// more than once. Conceptually this would be wrong. However, since such behavior will not affect
	    	// the demo, it has not been implemented
	    	Alarm a = Alarm.updateFromDummy(dummy);
	    	a.save();
	    	Alarm listItem = Global.alarmList.list.get(dummy.id);
	    	listItem.dispatchingTime = dispatchTime;
	    	Global.localMonitor.registerFollowUp(listItem.id);
	    	// TODO: add websocket call in the case of a real multi-user
	    	return;
	    }
	    
	    
	    public static void closeAlarm(Alarm dummy){
	    	dummy.closingTime = new Date();
	    	
	    	// since we are already closing the alarm, we will automatically remove it from the list
	    	// in the future TODO: we should rather repalce the hash item and delete when the item is closed
	    	Global.localMonitor.registerClosing(dummy.id);
	    	Global.alarmList.list.remove(dummy.id);
	    	
	    	Alarm a = Alarm.updateFromDummy(dummy);

	    	// ill just call the websocket if the attendant id is null,
	    	//though in a real multi user environment Id need to do it for everyone
	    	// and handle the GUI just based on the websocket
	    	MyWebSocketManager.notifyCloseAlarm(dummy);
	    	a.save();
	    }
}
