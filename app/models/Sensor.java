package models;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("Sensor")
public class Sensor extends AALComponent {

	public String sensorType;
	
}
