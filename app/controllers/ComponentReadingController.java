package controllers;

import java.util.List;

import models.sensors.ComponentReading;
import play.mvc.Controller;
import play.mvc.Result;

public class ComponentReadingController extends Controller {

	public static Result getReadingsPerType(String readingType) {
		List<ComponentReading> l = ComponentReading.getReadingsOfType(readingType);
		if (l.isEmpty()) return badRequest();
		else return ok(views.html.sensorReading.render(l));
	}

}
