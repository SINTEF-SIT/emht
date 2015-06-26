package models.sensors;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.Global;
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
}
