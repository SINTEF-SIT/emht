package models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.db.ebean.Model;
import play.libs.Json;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

/**
 * Created by Aleksander Skraastad (myth) on 6/19/15.
 */
@Entity
public class NMI extends Model {
    @Id
    public Long id;
    public Boolean conscious;
    public Boolean breathing;
    public Boolean movement;
    public Boolean standing;
    public Boolean talking;

    public static Finder<Long, NMI> find = new Finder(Long.class, NMI.class);

    public static List<NMI> all() { return find.all(); }

    public static NMI getOrCreate(NMI n) {
        if (n == null) return null;
        NMI dbNMI;
        if (n.id == null) {
            dbNMI = n;
        } else {
            dbNMI = find.ref(n.id);
            if (dbNMI == null) {
                n.id = null;
            } else {
                return dbNMI;
            }
        }
        dbNMI.save();
        return dbNMI;
    }

    /**
     * Transform an NMI object into a JSON ObjectNode
     * @param n An instance of NMI
     * @return An ObjectNode containing the NMI object data
     */
    public static ObjectNode toJson(NMI n) {
        ObjectNode nmi = Json.newObject();
        if (n.id == null) nmi.putNull("id");
        else nmi.put("id", n.id);
        if (n.conscious == null) nmi.putNull("conscious");
        else nmi.put("conscious", n.conscious);
        if (n.breathing == null) nmi.putNull("breathing");
        else nmi.put("breathing", n.breathing);
        if (n.movement == null) nmi.putNull("movement");
        else nmi.put("movement", n.movement);
        if (n.standing == null) nmi.putNull("standing");
        else nmi.put("standing", n.standing);
        if (n.talking == null) nmi.putNull("talking");
        else nmi.put("talking", n.talking);

        return nmi;
    }
}
