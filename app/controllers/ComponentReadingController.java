package controllers;

import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Patient;
import models.sensors.ComponentReading;
import models.sensors.Sensor;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class ComponentReadingController extends Controller {

	public static final Integer MAX_READINGS = 50;

	/**
	 * Retrieves a list of ComponentReadings connected to a specific Patient
	 * @param patientId The ID of the Patient in question
	 * @return A custom JSON object of ComponentReadings
	 */
	public static Result getReadingsForPatient(Long patientId) {

		Patient p = Patient.find.byId(patientId);
		if (p == null || p.id <= 0) return notFound("Patient does not exist.");

		List<Sensor> sensors = Sensor.find.where().eq("patient", p).findList();
		List<ComponentReading> data = ComponentReading.find.where()
			.in("component", sensors)
			.orderBy("date desc").setMaxRows(MAX_READINGS)
			.findList();
		ObjectNode wrapper = Json.newObject();
		wrapper.put("total", data.size());

		// TODO: This should be dynamic
		wrapper.put("type", "vitals");

		// Create the arrays for the different reading types
		ArrayNode readings = wrapper.putArray("readings");

		// Time to iterate over all components
		for (ComponentReading cr : data) {
			readings.add(ComponentReading.toJson(cr));
		}

		return ok(wrapper);
	}

	/**
	 * Start simulation of sensor readings for a specific sensor (which is connected to a certain patient)
	 * @return 200 OK
	 */
	public static Result startSimulator() {
		Sensor s = Sensor.find.byId(2L);
		ComponentReading.Generator.start(s, "battery", 10000L);
		ComponentReading.Generator.start(s, "heartRate", 10000L);
		ComponentReading.Generator.start(s, "systolicPressure", 10000L);
		ComponentReading.Generator.start(s, "diastolicPressure", 10000L);
		return ok("Sensor Simulator started on Sensor ID 2 (Patient " + s.patient.name + ")");
	}

	/**
	 * Halts execution of all simulation jobs
	 * @return 200 OK
	 */
	public static Result stopSimulator() {
		ComponentReading.Generator.stopAll();
		return ok("Simulator stopped. All generators cancelled.");
	}
}
