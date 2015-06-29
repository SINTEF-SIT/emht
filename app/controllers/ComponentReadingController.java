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

	/**
	 * Retrieves a list of ComponentReadings connected to a specific Patient
	 * @param patientId The ID of the Patient in question
	 * @return A custom JSON object of ComponentReadings
	 */
	public static Result getReadingsForPatient(Long patientId) {

		Patient p = Patient.find.byId(patientId);
		if (p == null || p.id <= 0) return notFound("Patient does not exist.");

		List<Sensor> sensors = Sensor.find.where().eq("patient", p).findList();
		List<ComponentReading> data = ComponentReading.find.where().in("component", sensors).orderBy("date desc").findList();
		ObjectNode wrapper = Json.newObject();
		wrapper.put("total", data.size());

		// Create the arrays for the different reading types
		ArrayNode heartRate = wrapper.putArray("heartRate");
		ArrayNode systolicPressure = wrapper.putArray("systolicPressure");
		ArrayNode diastolicPressure = wrapper.putArray("diastolicPressure");
		ArrayNode battery = wrapper.putArray("battery");

		// Time to iterate over all components
		for (ComponentReading cr : data) {
			switch (cr.readingType) {
				case "heartRate":
					heartRate.add(ComponentReading.toJson(cr));
					break;
				case "systolicPressure":
					systolicPressure.add(ComponentReading.toJson(cr));
					break;
				case "diastolicPressure":
					diastolicPressure.add(ComponentReading.toJson(cr));
					break;
				case "battery":
					battery.add(ComponentReading.toJson(cr));
					break;
			}
		}

		return ok(wrapper);
	}

	/**
	 * Start simulation of sensor readings for a specific sensor (which is connected to a certain patient)
	 * @return 200 OK
	 */
	public static Result startSimulator() {
		Sensor s = Sensor.find.byId(2L);
		ComponentReading.Generator.start(s, "battery", 60000L);
		ComponentReading.Generator.start(s, "heartRate", 30000L);
		ComponentReading.Generator.start(s, "systolicPressure", 20000L);
		ComponentReading.Generator.start(s, "diastolicPressure", 20000L);
		return ok("Sensor Simulator started on Sensor ID 2 (Patient " + s.patient.name + ")");
	}
}
