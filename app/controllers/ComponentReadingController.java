package controllers;

import java.util.List;

import models.sensors.ComponentReading;
import play.mvc.Controller;
import play.mvc.Result;

public class ComponentReadingController extends Controller {

	public static Result getReadingsPerType(String readingType) {
		return ok(views.html.sensorReading.render());
	}

}
