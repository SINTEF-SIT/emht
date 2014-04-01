package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class Callee extends Model {

	@Id
	public Long id;
	public String name;
	public String address;
	
	@Column(unique=true)
	public String phoneNumber; // TODO: add validation format to the phoneNumber
	
	  public static Finder<Long,Callee> find = new Finder(
			    Long.class, Callee.class
			  );
	  

			  public static List<Callee> all() {
				  return find.all();
				}

				public static void create(Callee calee) {
					calee.save();
				}
				
				public static Callee getOrCreate(Callee calee) {
					if (null == calee || calee.phoneNumber.isEmpty())
						return null;
					//else
					Callee c = find.where().ieq("phoneNumber", calee.phoneNumber).findUnique(); 
					if(null != c)
						return c; // callee is already on db, no need to save it
					//else
					calee.save();
					return calee;
				}
				

				public static void delete(Long id) {
				  find.ref(id).delete();
				}
}
