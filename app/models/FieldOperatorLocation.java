package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.SqlRow;
import play.Logger;
import play.db.ebean.Model;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Aleksander Skraastad (myth) on 6/15/15.
 */
@Entity
public class FieldOperatorLocation extends Model {
    @Id
    public Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    public AlarmAttendant fieldOperator;
    public Date timestamp;
    public Double latitude;
    public Double longitude;

    public static FieldOperatorLocation create(FieldOperatorLocation fol) {
        if (fol.fieldOperator == null || fol.latitude == null || fol.longitude == null) {
            return null;
        }
        fol.timestamp = new Date();
        fol.save();
        return fol;
    }

    public static Finder<Long, FieldOperatorLocation> find = new Finder(Long.class, FieldOperatorLocation.class);

    public static List<FieldOperatorLocation> all() {
        return find.orderBy("timestamp desc").findList();
    }

    public static List<FieldOperatorLocation> byUserId(Long userId) {
        return find.where().eq("fieldOperator.id", userId).orderBy("timestamp desc").findList();
    }

    public static List<FieldOperatorLocation> byUserId(Long userId, Integer maxEntries) {
        return find.where().eq("fieldOperator.id", userId).orderBy("timestamp desc").setMaxRows(maxEntries).findList();
    }

    // This should be done otherwise, but EBean aggregation is not straight forward and i did not
    // want to spend too much time figuring out how to map raw SQL queries to custom aggregate models.
    public static FieldOperatorLocation current(Long userId) {
        List<FieldOperatorLocation> locs = byUserId(userId);
        if (locs.size() == 0) return null;
        return locs.get(0);
    }

    // This should be done otherwise, but EBean aggregation is not straight forward and i did not
    // want to spend too much time figuring out how to map raw SQL queries to custom aggregate models.
    public static List<FieldOperatorLocation> current() {
        List<FieldOperatorLocation> locs = new ArrayList<>();
        for (AlarmAttendant a: AlarmAttendant.all()) {
            FieldOperatorLocation loc = current(a.id);
            if (loc == null) continue;
            locs.add(loc);
        }
        return locs;
    }
}
