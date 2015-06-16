package models;

import java.util.Date;
import java.util.List;

import javax.persistence.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.Global;
import core.MyWebSocketManager;

import play.db.ebean.Model;
import play.libs.Json;

@Entity
public class Alarm extends Model { // the model extension serves for having access to Play built-in Ebean helper, such as the Finder

	@Id
	public Long id;

	public String type;

	@ManyToOne(cascade = CascadeType.ALL)
	public Callee callee;
	public Date openingTime;
	public Date dispatchingTime; // TODO: implement dispatching
	public Date closingTime; // at the moment we are dispatching and closing all alarms

	public String occuranceAddress; // address of where the incident took place

	@Transient
	public boolean expired = false;

	@ManyToOne(cascade = CascadeType.ALL)
	public AlarmAttendant attendant;

	@Lob
	public String alarmLog;

	@Lob
	public String notes;

	@ManyToOne(cascade = CascadeType.ALL)
	public Patient patient;

	public static Finder<Long, Alarm> find = new Finder(Long.class, Alarm.class);

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
	public static List<Alarm> pastAlarmsFromCallee(Long calleeId) {
		return find.where().eq("callee.id", calleeId).isNotNull("closingTime").orderBy("openingTime desc").findList();
	}

	// TODO: investigate if it makes sense to explicitly tell that im not loading subobjects
	public static List<Alarm> pastAlarmsFromPatient(Long patientId) {
		return find.where().eq("patient.id", patientId).isNotNull("closingTime").orderBy("openingTime desc").findList();
	}

	/**
	 * Retrieve all Alarms from the database that has been assigned to the provided AlarmAttendant object.
	 * This method does not filter on open alarms only, instead it returns every alarm ever assigned to a particular
	 * user.
	 *
	 * @param attendant The AlarmAttendant user object for which assigned Alarms are to be retrieved
	 * @return A list of alarms if any are found. An empty list is returned otherwise.
	 */
	public static List<Alarm> assignedToUser(AlarmAttendant attendant) {
		return find.where().eq("attendant.id", attendant.id).findList();
	}

	public static Alarm assignAttendantToAlarm(Long alarmId, AlarmAttendant attendant) {
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
	private static Alarm updateFromDummy(Alarm dummy) {
		Alarm a = Alarm.get(dummy.id);
		if (null != dummy.patient) {
			if (null == a.patient || (a.patient.id != dummy.patient.id)) { // no patient assigned to alarm, or different patient assgined
				Patient p = Patient.getFromId(dummy.patient.id);
				a.patient = p;
			}
		}
		if (null != dummy.notes) // Im assuming Ill alwasy update the notes
			a.notes = dummy.notes;

		if (null != dummy.occuranceAddress) // Im assuming Ill alwasy update the address
			a.occuranceAddress = dummy.occuranceAddress;

		// for the time fields, Ill not update them if they have already been set
		if (null != dummy.dispatchingTime && null == a.dispatchingTime)
			a.dispatchingTime = dummy.dispatchingTime;

		if (null != dummy.closingTime && null == a.closingTime)
			a.closingTime = dummy.closingTime;

		return a;
	}

	public static void saveAlarm(Alarm dummy) {
		Alarm a = Alarm.updateFromDummy(dummy);
		a.save();
	}

	public static void saveAndFollowupAlarm(Alarm dummy) {
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

	}

	public static void closeAlarm(Alarm dummy) {
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

	/**
	 * Translation method that takes in an Alarm instance A and converts it into a Jackson ObjectNode
	 * @param a An alarm instance that is to be translated to a ObjectNode
	 * @return An ObjectNode containing the Alarm data
	 */
	public static ObjectNode toJson(Alarm a) {

		ObjectNode alarm = Json.newObject();
		alarm.put("id", a.id);
		alarm.put("occuranceAddress", a.occuranceAddress);
		alarm.put("alarmLog", a.alarmLog);
		alarm.put("notes", a.notes);
		alarm.put("type", a.type);
		alarm.put("openingTime", a.openingTime != null ? Global.formatDateAsISO(a.openingTime) : null);
		alarm.put("dispatchingTime", a.dispatchingTime != null ? Global.formatDateAsISO(a.dispatchingTime) : null);
		alarm.put("closingTime", a.closingTime != null ? Global.formatDateAsISO(a.closingTime) : null);

		// Add the callee object if present, otherwise write a null
		if (a.callee != null) {
			ObjectNode callee = Json.newObject();
			callee.put("id", a.callee.id);
			callee.put("name", a.callee.name);
			callee.put("phoneNumber", a.callee.phoneNumber);
			callee.put("address", a.callee.address);
			alarm.put("callee", callee);
		} else {
			alarm.putNull("callee");
		}

		// Add the patient object if present, otherwise write a null
		if (a.patient != null) {
			ObjectNode patient = Json.newObject();
			patient.put("id", a.patient.id);
			patient.put("name", a.patient.name);
			patient.put("persoNumber", a.patient.personalNumber);
			patient.put("phoneNumber", a.patient.phoneNumber);
			patient.put("address", a.patient.address);
			patient.put("age", a.patient.age);
			alarm.put("patient", patient);
		} else {
			alarm.putNull("patient");
		}

		return alarm;
	}
}
