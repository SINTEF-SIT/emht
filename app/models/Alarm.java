package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import play.db.ebean.Model;

@Entity
public class Alarm extends Model { // the model extension serves for having access to Play built-in Ebean helper, such as the Finder

	@Id
	public Long id;
	
	public String type;
	
	@ManyToOne(cascade=CascadeType.ALL)  
	public Callee callee;
	public String address;
	public Date openingTime;
	public Date closingTime;
	
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

	    // assumes that the calle and patient object from the incoming alarm has already
	    // been saved on the db
		public static void create(Alarm alarm) {
			alarm.openingTime = new Date();
			alarm.save();
		}

		public static void delete(Long id) {
		  find.ref(id).delete();
		}
	  
		public static Alarm get(Long id) {
			  return find.ref(id);
		}
		  
	  
}
