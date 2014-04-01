import java.util.List;

import com.avaje.ebean.Ebean;

import models.AlarmAttendant;
import play.Application;
import play.GlobalSettings;
import play.libs.Yaml;


public class Global extends GlobalSettings {

    @Override
    public void onStart(Application app) {
        // Check if the AlarmAttendant db is empty
        if (AlarmAttendant.find.findRowCount() == 0) {
            Ebean.save((List) Yaml.load("initial-data.yml"));
        }
    }
}
