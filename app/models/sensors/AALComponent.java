package models.sensors;

import javax.persistence.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Patient;
import play.Logger;
import play.db.ebean.Model;
import play.libs.Json;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "AALComponent")
public abstract class AALComponent extends Model {

	@Id
	public long id;

	@ManyToOne(cascade=CascadeType.ALL)
	public Patient patient;

	public long getId() {
		return id;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	/**
	 * Convert an AALComponent instance to JSON
	 * @param c The AALComponent in question
	 * @return A JSON ObjectNode
	 */
	public static ObjectNode toJson(AALComponent c) {
		ObjectNode wrapper = Json.newObject();
		wrapper.put("id", c.id);
		//ObjectNode patient = Patient.toJson(c.patient);
		//wrapper.put("patient", patient);
		return wrapper;
	}
}
