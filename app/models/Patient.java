package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class Patient extends Model {

	@Id
	public Long id;
	public String name;
	public String address;
	public Integer age;
	
	@Column(unique=true)
	public String personalNumber; // TODO: add validation format to the personal Number
	
	  public static Finder<Long,Patient> find = new Finder(
			    Long.class, Patient.class
			  );
			  
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
					if (null == pat || pat.personalNumber.isEmpty())
						return null;
					//else
					Patient p = find.where().ieq("personalNumber", pat.personalNumber).findUnique(); 
					if(null != p)
						return p; // patient is already on db, no need to save it
					//else
					pat.save();
					return pat;
				}
				
}
