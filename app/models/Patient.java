package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.db.ebean.Model;
import play.libs.Json;


@Entity
public class Patient extends Model {

	@Id
	public Long id;
	public String name;
	public String address;
	public Integer age;
	public String phoneNumber;

	public String personalNumber;
	public String obs;

	@SuppressWarnings("unchecked")
	public static Finder<Long,Patient> find = new Finder(Long.class, Patient.class);

	public static List<Patient> all() {
		return find.all();
	}

	public static void create(Patient patient) {
		patient.save();
	}

	public static void delete(Long id) {
		find.ref(id).delete();
	}

	public static Patient getOrCreate(Patient pat) {
		if (null == pat)
			return null;
		//else
		Patient p = null;
		if(null != pat.personalNumber && false == pat.personalNumber.isEmpty())
			p = find.where().ieq("personalNumber", pat.personalNumber).findUnique();

		if(null != p)
			return p; // patient is already on db, no need to save it
		//else
		pat.save();
		return pat;
	}

	public static List<Patient> patientFromAddress(String address){
		if (null == address || address.isEmpty())
			return null;
		else{
			List<Patient> list = find.where().ieq("address", address).findList();
			return list;
		}
	}

	public static Patient getFromId(Long id) {
		Patient p = find.where().eq("id", id).findUnique();
		return p;
	}

	// this function will return: the patient of the Alarm or everybody that lives in the
	// same address of the callee
	public static List<Patient> prospectPatientsFromAlarm(Long id){
		Alarm a = Alarm.get(id);
		List<Patient> list = new ArrayList<Patient>();
		String calleeAdr = a.callee.address;

		if (null != a.patient){ // if the alarm has an assigned patient
			list.add(a.patient);
		} else { // otherwise I get the list of residents
			if (null != calleeAdr && (calleeAdr.isEmpty() == false)){ // if we have the callee address
				list.addAll(find.where().ieq("address", calleeAdr).findList());// add all people in that address
			}
		}

		return list;
	}

	/**
	 * Convert a Patient instance to JSON
	 * @param p The Patient instance in question
	 * @return A Json ObjectNode
	 */
	public static ObjectNode toJson(Patient p) {
		ObjectNode wrapper = Json.newObject();
		wrapper.put("id", p.id);
		wrapper.put("name", p.name);
		wrapper.put("address", p.address);
		wrapper.put("age", p.age);
		wrapper.put("phoneNumber", p.phoneNumber);
		wrapper.put("personalNumber", p.personalNumber);
		wrapper.put("obs", p.obs);
		return wrapper;
	}
}
