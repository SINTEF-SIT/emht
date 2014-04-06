package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class AlarmAttendant extends Model {

	@Id
	public Long id;
	@Column(unique=true)
	public String username;
	
	
	  public static Finder<Long,AlarmAttendant> find = new Finder(
			    Long.class, AlarmAttendant.class
			  );
			  
			  public static List<AlarmAttendant> all() {
				  return find.all();
				}

				public static void create(AlarmAttendant attendant) {
					attendant.save();
				}

				public static void delete(Long id) {
				  find.ref(id).delete();
				}
			  
			  public static AlarmAttendant getAttendantFromUsername(String attendantUserName) {
				  return find.where().eq("username",attendantUserName).findUnique();
				}
	
}
