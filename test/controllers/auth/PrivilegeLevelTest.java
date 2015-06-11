package controllers.auth;

import controllers.routes;
import static org.junit.Assert.*;
import org.junit.*;
import play.mvc.*;
import play.test.Helpers;
import play.libs.*;
import play.test.*;
import static play.test.Helpers.*;
import com.avaje.ebean.Ebean;
import com.google.common.collect.ImmutableMap;

import java.util.List;

/**
 * Created by Aleksander Skraastad (myth) on 6/10/15.
 * <p>
 * emht is licenced under the MIT licence.
 */
public class PrivilegeLevelTest extends WithApplication {

    @Before
    public void setUp() throws Exception {
        start(fakeApplication(inMemoryDatabase(), fakeGlobal()));
        Ebean.save((List) Yaml.load("test-data.yml"));
    }

    @Test
    public void testPrivilegeLevelSuccess() {
        Result result = callAction(
            routes.ref.Application.authenticate(),
            fakeRequest().withFormUrlEncodedBody(
                ImmutableMap.of("username", "Admin", "password", "password")
            )
        );

        assertEquals(303, status(result));
        assertEquals("2", session(result).get("id"));

        result = callAction(routes.ref.AttendantController.attendants(),
            fakeRequest().withSession("id", "2").withSession("role", "0"));

        assertEquals(200, status(result));
    }

    @Test
    public void testPrivilegeLevelFailure() {
        Result result = callAction(
            routes.ref.Application.authenticate(),
            fakeRequest().withFormUrlEncodedBody(
                ImmutableMap.of("username", "Karin", "password", "password")
            )
        );

        assertEquals(303, status(result));
        assertEquals("1", session(result).get("id"));

        result = callAction(routes.ref.AttendantController.attendants(),
            fakeRequest().withSession("id", "1").withSession("role", "2"));

        assertEquals(401, status(result));
    }
}