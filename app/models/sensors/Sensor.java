package models.sensors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.sensors.AALComponent;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("Sensor")
public class Sensor extends AALComponent {
	public String sensorType;

	/**
	 * Convert a Sensor instance to JSON
	 * @param s The Sensor instance in question
	 * @return A JSON ObjectNode
	 */
	public static ObjectNode toJson(Sensor s) {
		ObjectNode wrapper = AALComponent.toJson(s);
		wrapper.put("sensorType", s.sensorType);
		return wrapper;
	}
}
