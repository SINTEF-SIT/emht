package models;

import java.util.*;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.db.ebean.Model;
import play.libs.Json;


@Entity
public class Patient extends Model {

	@Id
	public Long id;
	public String name;
	public String address;
	public Double latitude;
	public Double longitude;
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
		Patient p;
		if (pat == null) return null;
		if (pat.id != null) {
			p = find.byId(pat.id);
			if (p == null || p.id.equals(0)) {
				p = new Patient();
				p.save();
			}
			else return p;
		}
		else {
			p = pat;
		}

		p.save();
		return p;
	}

	public static List<Patient> patientFromAddress(String address){
		if (null == address || address.isEmpty())
			return null;
		else {
			return find.where().ieq("address", address).findList();
		}
	}

	public static Patient getFromId(Long id) {
		Patient p = find.where().eq("id", id).findUnique();
		return p;
	}

	// this function will return: the patient of the Alarm or everybody that lives in the
	// same address of the callee
	public static List<Patient> prospectPatientsFromAlarm(Long id) {
		Alarm a = Alarm.get(id);
		List<Patient> list = new ArrayList<Patient>();
		String calleeAdr = a.callee.address;

		list.addAll(find.where().ilike("address", "%"+calleeAdr+"%").findList());
		if (a.patient != null && a.patient.address != null && !a.patient.address.equalsIgnoreCase(calleeAdr)) {
			list.add(a.patient);
		}

		return list;
	}

	/**
	 * Very simplistic search function that tests a string against name, phone and address fields of a patient
	 * @param searchString The search string
	 * @return A List of Patient objects matching the query. If none match, an empty list is returned.
	 */
	public static List<Patient> search(String searchString) {
		HashMap<Long, Patient> results = new HashMap<>();
		//
		for (Patient p: find.where().ilike("name", "%"+searchString+"%").findList()) {
			results.put(p.id, p);
		}
		for (Patient p: find.where().ilike("phoneNumber", "%"+searchString+"%").findList()) {
			results.put(p.id, p);
		}
		for (Patient p: find.where().ilike("address", "%"+searchString+"%").findList()) {
			results.put(p.id, p);
		}

		if (results.containsKey(1L)) results.remove(1L);

		return new ArrayList<>(results.values());
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
		wrapper.put("latitude", p.latitude);
		wrapper.put("longitude", p.longitude);
		wrapper.put("age", p.age);
		wrapper.put("phoneNumber", p.phoneNumber);
		wrapper.put("personalNumber", p.personalNumber);
		wrapper.put("obs", p.obs);
		return wrapper;
	}
}
