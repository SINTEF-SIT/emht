package models.sensors;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.Global;
import models.Patient;
import play.Logger;
import play.db.ebean.Model;
import play.libs.Json;

@Entity
public class ComponentReading extends Model {

	@Id
	public Long id;

	// String representation of what type of reading (e.g Battery, Heart rate etc)
	public String readingType;
	// Datetime instance of the reading
	public Date date;
	public Double value;

	// The component this reading is related to
	@ManyToOne(cascade=CascadeType.ALL)
	public AALComponent component;

	@SuppressWarnings("unchecked")
	public static Finder<Long,ComponentReading> find = new Finder(Long.class, ComponentReading.class);

	public static List<ComponentReading> getReadingsOfType(String readingType) {
		return find.where().ieq("readingType", readingType).findList();
	}

	/**
	 * Convert a ComponentReading to JSON
	 * @param c The ComponentReading in question
	 * @return A JSON ObjectNode
	 */
	public static ObjectNode toJson(ComponentReading c) {
		ObjectNode wrapper = Json.newObject();
		wrapper.put("id", c.id);
		wrapper.put("readingType", c.readingType);
		wrapper.put("date", Global.formatDateAsISO(c.date));
		wrapper.put("value", c.value);
		ObjectNode component = AALComponent.toJson(c.component);
		wrapper.put("component", component);
		return wrapper;
	}

	/**
	 * The Generator class simulates component readings in frequent intervals
	 */
	public static class Generator {
		// Set up a single thread exec service we can inject tasks into
		public static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

		// We keep a hash mapping between sensor and battery status, since ideally this should drain
		// progressively and not randomly fluctuate.
		public static ConcurrentHashMap<Sensor, Double> batteryStatus = new ConcurrentHashMap<Sensor, Double>();

		/**
		 * Start simulation of sensor readings for a given sensor (whic is related to a Patient).
		 * The interval for generation of new component readings are in milliseconds.
		 * @param s A Sensor instance
		 * @param intervalInMilliseconds Reading interval in milliseconds
		 */
		public static void start(Sensor s, String readingType, Long intervalInMilliseconds) {
			// Create a new battery status at 100%
			batteryStatus.put(s, 100.0d);

			// Instantiate a new recurring job
			Runnable job = new Runnable() {
				@Override
				public void run() {

					ComponentReading cr = new ComponentReading();
					cr.component = s;
					cr.date = new Date();
					cr.readingType = readingType;

					// Set value based on the readingType we are simulating
					switch (cr.readingType) {
						case "heartRate":
							cr.value = 70.0d + (Math.random() * 30.0d);
							break;
						case "systolicPressure":
							cr.value = 110.0d + (Math.random() * 20.0d);
							break;
						case "diastolicPressure":
							cr.value = 70.0d + (Math.random() * 20.0d);
							break;
						case "battery":
							// Slowly drain the battery and update the hash map
							cr.value = batteryStatus.get(s) - (Math.random() / 5.0d);
							if (cr.value < 0.0) cr.value = 0.0d;
							batteryStatus.put(s, cr.value);
							break;
					}

					// Save it to the database
					cr.save();
					Logger.debug("Sensor reading for " + cr.readingType + " simulated with value: " + cr.value +
					" on patient " + cr.component.patient.name);
				}
			};
			
			// Schedule the job
			executorService.scheduleAtFixedRate(job, 0L, intervalInMilliseconds, TimeUnit.MILLISECONDS);
		}
	}
}
