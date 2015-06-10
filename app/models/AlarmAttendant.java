package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import controllers.auth.Authentication;
import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

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

	public static Finder<Long,AlarmAttendant> find = new Finder(Long.class, AlarmAttendant.class);

	public static List<AlarmAttendant> all() {
		return find.all();
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

	public static void delete(Long id) {
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
}
