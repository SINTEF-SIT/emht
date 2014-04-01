package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
public class ComponentReading extends Model {

	@Id
	public Long id;
	public String readingType;// availability, etc
	public Date date;// date from the reading
	public double value;
	// TODO value
	
	@ManyToOne(cascade=CascadeType.ALL)  
	public AALComponent component;
	
	  public static Finder<Long,ComponentReading> find = new Finder(
			    Long.class, ComponentReading.class
			  );
	
	  public static List<ComponentReading> getReadingsOfType(String readingType) {
		  List<ComponentReading> l = find.where().ieq("readingType", readingType).findList();
		  return l;
		} 
	  
}
