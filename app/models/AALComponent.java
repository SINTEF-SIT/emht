package models;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Inheritance
@DiscriminatorColumn(name="COMPONENT_TYPE")
@Table(name="AALCOMPONENT")
public abstract class AALComponent extends Model {

	@Id
	  private long id;
	
	@ManyToOne(cascade=CascadeType.ALL)
	public Patient patient;

	public long getId() {
		return id;
	}

	/*public void setId(long id) {
		this.id = id;
	}*/

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	// TODO: create a component reading table, where each component reading has a time and reading and type
	
	
}
