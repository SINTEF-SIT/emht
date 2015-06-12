package controllers;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import play.libs.Yaml;
import static org.junit.Assert.*;
import play.mvc.*;
import play.libs.*;
import play.test.*;
import static play.test.Helpers.*;

import java.util.List;

import static org.junit.Assert.*;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.inMemoryDatabase;

/**
 * Created by Aleksander Skraastad (myth) on 6/12/15.
 * <p>
 * emht is licenced under the MIT licence.
 */
public class ApplicationTest extends WithApplication {

    @Before
    public void setUp() throws Exception {
        start(fakeApplication(inMemoryDatabase(), fakeGlobal()));
        Ebean.save((List) Yaml.load("test-data.yml"));
    }

    @Test
    public void testAlarmsAssignedToMe() throws Exception {
        // Perform a fake request from Karin (which has no assigned alarms)
        Result result = callAction(controllers.routes.ref.Application.alarmsAssignedToMe(),
            fakeRequest().withSession("id", "1").withSession("username", "Karin").withSession("role", "2"));
        assertEquals(200, status(result));
        JsonNode json = Json.parse(contentAsString(result));
        assertNotNull(json.findPath("alarms"));
        assertEquals(json.findPath("alarms").size(), 0);

        // Perform a fake request from Bernt (which has one of the two alarms assigned)
        result = callAction(controllers.routes.ref.Application.alarmsAssignedToMe(),
            fakeRequest().withSession("id", "3").withSession("username", "Bernt").withSession("role", "3"));
        assertEquals(200, status(result));
        json = Json.parse(contentAsString(result));
        assertNotNull(json.findPath("alarms"));
        assertEquals(json.findPath("alarms").size(), 1);
    }
}