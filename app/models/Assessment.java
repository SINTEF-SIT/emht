package models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.db.ebean.Model;
import play.libs.Json;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Aleksander Skraastad (myth) on 6/19/15.
 */
@Entity
public class Assessment extends Model {
    @Id
    public Long id;
    @ManyToOne(cascade = CascadeType.ALL)
    public NMI nmi;
    public boolean sensorsChecked;
    public boolean patientInformationChecked;

    public static Finder<Long, Assessment> find = new Finder(Long.class, Assessment.class);

    public static List<Assessment> all() { return find.all(); }

    /**
     * Transform an Assessment object into a JSON ObjectNode
     * @param a An instance of Assessment
     * @return An ObjectNode containing the Assessment object data
     */
    public static ObjectNode toJson(Assessment a) {
        ObjectNode assessment = Json.newObject();
        assessment.put("id", a.id);
        assessment.put("sensorsChecked", a.sensorsChecked);
        assessment.put("patientInformationChecked", a.patientInformationChecked);
        if (a.nmi != null) {
            assessment.put("nmi", NMI.toJson(a.nmi));
        } else {
            assessment.putNull("nmi");
        }
        return assessment;
    }
}
