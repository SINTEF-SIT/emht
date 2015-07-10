package models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.db.ebean.Model;
import play.libs.Json;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.util.List;
import java.util.UUID;

/**
 * Created by Aleksander Skraastad (myth) on 7/10/15.
 */
@Entity
public class ApiKey extends Model {
    @Id
    public Long id;

    @OneToOne(cascade = CascadeType.PERSIST)
    public AlarmAttendant user;
    public String key;

    public static ApiKey create(ApiKey a) {
        AlarmAttendant attendant = AlarmAttendant.find.byId(a.user.id);
        if (attendant == null || attendant.id < 1) return null;
        else {
            a.key = UUID.randomUUID().toString();
            a.save();
        }
        return a;
    }

    public static Finder<Long, ApiKey> find = new Finder<Long, ApiKey>(Long.class, ApiKey.class);

    public static List<ApiKey> all() {
        return find.all();
    }

    public static ApiKey getByUser(AlarmAttendant a) {
        return find.where().eq("user", a).findUnique();
    }

    public static ObjectNode toJson(ApiKey a) {
        ObjectNode wrapper = Json.newObject();
        wrapper.put("id", a.id);
        wrapper.put("user", AlarmAttendant.toJson(a.user));
        wrapper.put("key", a.key);

        return wrapper;
    }
}
