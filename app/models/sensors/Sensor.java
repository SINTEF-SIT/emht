package models.sensors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("Sensor")
public class Sensor extends AALComponent {
	public String sensorType;

	// Finder object shortcut for this model
	@SuppressWarnings("unchecked")
	public static Finder<Long, Sensor> find =  new Finder(Long.class, Sensor.class);

	/**
	 * Convert a Sensor instance to JSON
	 * @param s The Sensor instance in question
	 * @return A JSON ObjectNode
	 */
	public static ObjectNode toJson(Sensor s) {
		ObjectNode wrapper = Json.newObject();
		wrapper.put("sensorType", s.sensorType);
		return wrapper;
	}
}
