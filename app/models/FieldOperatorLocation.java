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

    @OneToOne(cascade = CascadeType.ALL)
    public AlarmAttendant fieldOperator;
    public Date timestamp;
    public Double latitude;
    public Double longitude;

    /**
     * Create and save a FieldOperatorLocation entry.
     * @param fol An instance of FieldOperatorLocation
     * @return A saved version of FieldOperatorLocation that now has an ID.
     */
    public static FieldOperatorLocation create(FieldOperatorLocation fol) {
        if (fol.fieldOperator == null || fol.latitude == null || fol.longitude == null) {
            return null;
        }
        fol.timestamp = new Date();
        fol.save();
        return fol;
    }

    /**
     * Find helper class
     */
    public static Finder<Long, FieldOperatorLocation> find = new Finder(Long.class, FieldOperatorLocation.class);

    /**
     * Get all location entries in the database.
     * @return A list of FieldOperatorLocations
     */
    public static List<FieldOperatorLocation> all() {
        return find.orderBy("timestamp desc").findList();
    }

    /**
     * Get all location entries for a specific user
     * @param userId The User id of the user to query
     * @return A List of FieldOperatorLocations
     */
    public static List<FieldOperatorLocation> byUserId(Long userId) {
        return find.where().eq("fieldOperator.id", userId).orderBy("timestamp desc").findList();
    }

    /**
     * Get all location entries for a specific user, limited to maxEntries.
     * @param userId The User id of the user to query
     * @param maxEntries The maximum number of rows (entries) to retrieve
     * @return A List of FieldOperatorLocations
     */
    public static List<FieldOperatorLocation> byUserId(Long userId, Integer maxEntries) {
        return find.where().eq("fieldOperator.id", userId).orderBy("timestamp desc").setMaxRows(maxEntries).findList();
    }

    // This should be done otherwise, but EBean aggregation is not straight forward and i did not
    // want to spend too much time figuring out how to map raw SQL queries to custom aggregate models.

    /**
     * Retrieve the latest location entry for a specific mobile user.
     * @param userId The User id of the user to query
     * @return A FieldOperatorLocation if found, null otherwise.
     */
    public static FieldOperatorLocation current(Long userId) {
        List<FieldOperatorLocation> locs = byUserId(userId);
        if (locs.size() == 0) return null;
        return locs.get(0);
    }

    // This should be done otherwise, but EBean aggregation is not straight forward and i did not
    // want to spend too much time figuring out how to map raw SQL queries to custom aggregate models.

    /**
     * Retrieve the latest location entry for all mobile users. The returned list will
     * contain one FieldOperatorLocation per user (if they exist). If no location entries are found,
     * an empty list is returned.
     * @return A List of FieldOperatorLocations.
     */
    public static List<FieldOperatorLocation> current() {
        List<FieldOperatorLocation> locs = new ArrayList<>();
        for (AlarmAttendant a: AlarmAttendant.getMobileCareTakers()) {
            FieldOperatorLocation loc = current(a.id);
            if (loc == null) continue;
            locs.add(loc);
        }
        return locs;
    }
}
