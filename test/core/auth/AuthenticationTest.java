package core.auth;

import controllers.routes;
import static org.junit.Assert.*;
import org.junit.*;
import java.util.*;
import play.mvc.*;
import play.libs.*;
import play.test.*;
import static play.test.Helpers.*;
import com.avaje.ebean.Ebean;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public class AuthenticationTest extends WithApplication {

    @Before
    public void setUp() throws Exception {
        start(fakeApplication(inMemoryDatabase(), fakeGlobal()));
        Ebean.save((List) Yaml.load("test-data.yml"));
    }

    @Test
    public void testValidateSuccess() throws Exception {
        Result result = callAction(
            routes.ref.Application.authenticate(),
            fakeRequest().withFormUrlEncodedBody(
                ImmutableMap.of("username", "Karin", "password", "password")
            )
        );

        assertEquals(303, status(result));
        assertEquals("Karin", session(result).get("username"));
    }

    @Test
    public void testValidateFailure() throws Exception {
        Result result = callAction(
                routes.ref.Application.authenticate(),
                fakeRequest().withFormUrlEncodedBody(
                        ImmutableMap.of("username", "Karin", "password", "wrongpassword")
                )
        );

        assertEquals(400, status(result));
        assertNull(session(result).get("username"));
    }
}