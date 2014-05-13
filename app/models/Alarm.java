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
	
	@ManyToOne(cascade=CascadeType.ALL)
	public Patient patient;
	
	  public static Finder<Long,Alarm> find = new Finder(
	    Long.class, Alarm.class
	  );
	  

	  
	  public static List<Alarm> all() {
		  return find.all();
		}
	  
	  public static List<Alarm> allOpenAlarms() {
		  return find.where().isNull("closingTime").findList();
		}

	    // assumes that the calle and patient object from the incoming alarm has already
	    // been saved on the db
		public static void create(Alarm alarm) {
			alarm.openingTime = new Date();
			alarm.save();
			Global.alarmList.list.put(alarm.id, alarm);
			Global.localMonitor.registerNewAlert(alarm.id);
			MyWebSocketManager.notifyNewAlarm(alarm);
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
	    	return find.where().eq("callee.id",calleeId).isNotNull("closingTime").findList();
	    }
		
	    public static Alarm assignAttendantToAlarm(Long alarmId, AlarmAttendant attendant){
	    	Alarm a = find.ref(alarmId);
	    	a.attendant = attendant;
	    	a.save();
	    	Global.alarmList.list.put(a.id, a);// it will replace the alarm in the list with a new one with attendant and
	    	// clean expired flag
	    	Global.localMonitor.registerAssignment(alarmId);
	    	// TODO: possibly add checks
	    	// TODO: add websocket call
	    	return a;
	    }
	  
	    public static Alarm dispatchAlarm(Long alarmId){
	    	Alarm a = find.ref(alarmId);
	    	a.dispatchingTime = new Date();
	    	a.closingTime = new Date();
	    	a.save(); // at the moment we are dispatching and closing all alarms
	    	// since we are already closing the alarm, we will automatically remove it from the list
	    	// in the future TODO: we should rather repalce the hash item and delete when the item is closed
	    	Global.alarmList.list.remove(alarmId); 
	    	// TODO: possibly add checks
	    	// TODO: add websocket call
	    	return a;
	    }
}
