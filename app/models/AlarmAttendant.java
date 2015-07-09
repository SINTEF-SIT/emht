package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.auth.Authentication;
import play.cache.Cache;
import play.db.ebean.Model;
import play.db.ebean.Model.Finder;
import play.libs.Json;

@Entity
public class AlarmAttendant extends Model {

	public static final int ADMINISTRATOR = 0;
	public static final int ATTENDANT = 2;
	public static final int FIELDOPERATOR = 3;

	@Id
	public Long id;
	@Column(unique = true)
	public String username;
	@Column(nullable = false)
	public String password;
	@Column(nullable = false)
	public int role;

	@Column(unique = true)
	public String gcmRegId;

	public static Finder<Long,AlarmAttendant> find = new Finder(Long.class, AlarmAttendant.class);

	public static List<AlarmAttendant> all() {
		return find.all();
	}

	public static AlarmAttendant get(Long id) {
		return find.byId(id);
	}

	public static void create(AlarmAttendant attendant) {
		try {
			attendant.password = Authentication.generatePasswordHash(attendant.password);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		// Safe after we have hashed the password
		attendant.save();
	}

	/**
	 * Perform some simple data validity checks on the model fields
	 * @return A string containing an error, if occurred, null if no errors.
	 */
	public String validate() {
		if (password.length() < 6) return "Password must be more than 6 characters";
		if (username.length() < 2) return "Username must be at least 2 characters";
		if (!(role == ADMINISTRATOR || role == ATTENDANT || role == FIELDOPERATOR)) {
			return "User must be either Administrator, Attendant or Field Operator";
		}
		return null;
	}

	/**
	 * Retrieves a user object by its ID and injects it into the Cache
	 * @param id The user ID of the user to be retrieved
	 * @return The AlarmAttendant corresponding to the provided id
	 */
	public static AlarmAttendant getAndInjectIntoCache(Long id) {
		AlarmAttendant user = find.byId(id);
		if (Cache.get(id.toString()) == null) Cache.set(id.toString(), user);
		return user;
	}

	/**
	 * Retrieve a list of all Mobile Care Takers (Field operators). If no AlarmAttendants with that role
	 * exists, an empty list is returned.
	 * @return A List of AlarmAttendants with role == 3.
	 */
	public static List<AlarmAttendant> getMobileCareTakers() {
		return find.where().eq("role", FIELDOPERATOR).findList();
	}

	public static void delete(Long id) {
		// Due to problems with CascadeType for the Alarm FK's (which we could not figure out why, and the docs
		// did not provide much information, we null out all FK's before delete.
		AlarmAttendant att = find.ref(id);
		for (Alarm a : Alarm.all()) {
			if (a.attendant != null && a.attendant.id.equals(att.id)) {
				a.attendant = null;
			}
			if (a.mobileCareTaker != null && a.mobileCareTaker.id.equals(att.id)) {
				a.mobileCareTaker = null;
			}
			a.save();
		}
		find.ref(id).delete();
	}

	public static AlarmAttendant getAttendantFromUsername(String attendantUserName) {
		return find.where().eq("username",attendantUserName).findUnique();
	}

	public static boolean isAdministrator(AlarmAttendant attendant) {
		return attendant.role == ADMINISTRATOR;
	}
	public static boolean isAttendant(AlarmAttendant attendant) { return attendant.role == ATTENDANT; }
	public static boolean isFieldOperator(AlarmAttendant attendant) { return attendant.role == FIELDOPERATOR; }

	/**
	 * Helper method that returns an AlarmAttendant as JSON
	 * @param a The AlarmAttendant to serialize
	 * @return A JSON ObjectNode representing the AlarmAttendant
	 */
	public static ObjectNode toJson(AlarmAttendant a) {
		ObjectNode alarmAttendant = Json.newObject();
		alarmAttendant.put("id", a.id);
		alarmAttendant.put("username", a.username);
		alarmAttendant.put("role", a.role);
		alarmAttendant.put("gcmRegId", a.gcmRegId);
		return alarmAttendant;
	}
}
