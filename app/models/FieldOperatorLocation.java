package models;

import play.db.ebean.Model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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

    public static FieldOperatorLocation current(Long userId) {
        List<FieldOperatorLocation> locations = byUserId(userId);
        if (locations.size() > 0) return locations.get(0);
        return null;
    }
}
