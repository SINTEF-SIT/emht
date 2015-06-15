package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.AlarmAttendant;
import models.FieldOperatorLocation;
import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Aleksander Skraastad (myth) on 6/15/15.
 * <p>
 * emht is licenced under the MIT licence.
 */
public class Location extends Controller {

    /* Helper methods */

    /**
     * Create a JSON object node from a FieldOperatorLocation
     * @param loc A FieldOperatorLocation instance
     * @return A JSON object node with necessary information
     */
    private static ObjectNode createLocationObjectNode(FieldOperatorLocation loc) {
        ObjectNode location = Json.newObject();
        location.put("latitude", loc.latitude);
        location.put("longitude", loc.longitude);
        location.put("timestamp", loc.timestamp.toString());
        return location;
    }

    /**
     * Create a JSON object node for a specific user based on the users list of locations
     * @param locs An ArrayList of FieldOperatorLocation instances belonging to the same user
     * @return A JSON object node with necessary information
     */
    private static ObjectNode createUserObjectNode(List<FieldOperatorLocation> locs) {
        ObjectNode user = Json.newObject();
        user.put("id", locs.get(0).fieldOperator.id);
        user.put("username", locs.get(0).fieldOperator.username);
        ArrayNode users = user.putArray("locations");

        for (FieldOperatorLocation loc : locs) {
            users.add(createLocationObjectNode(loc));
        }

        return user;
    }

    /**
     * Retrieve a JSON object with a list of all location entries in the database for all field operators
     * @return A JSON object with all location entries
     */
    public static Result all() {
        List<FieldOperatorLocation> locations = FieldOperatorLocation.all();
        // Map individual users to location object lists
        HashMap<AlarmAttendant, ArrayList<FieldOperatorLocation>> userToLocationEntry = new HashMap<>();
        for (FieldOperatorLocation loc : locations) {
            if (!userToLocationEntry.containsKey(loc.fieldOperator)) {
                userToLocationEntry.put(loc.fieldOperator, new ArrayList<>());
            }
            userToLocationEntry.get(loc.fieldOperator).add(loc);
        }
        ObjectNode wrapper = Json.newObject();
        wrapper.put("total", locations.size());
        ArrayNode users = wrapper.putArray("fieldOperators");
        for (Map.Entry<AlarmAttendant, ArrayList<FieldOperatorLocation>> e : userToLocationEntry.entrySet()) {
            users.add(createUserObjectNode(e.getValue()));
        }

        return ok(wrapper);
    }

    /**
     * Retrieve a JSON object of all location entries for a specific user
     * @param userId The user ID of the user to query
     * @return A JSON object with all location entries of the specified user
     */
    public static Result byUserId(Long userId) {
        List<FieldOperatorLocation> locations = FieldOperatorLocation.byUserId(userId, 25);
        ObjectNode jsonListObject;
        if (!locations.isEmpty()) {
            jsonListObject = createUserObjectNode(locations);
        } else {
            AlarmAttendant a = AlarmAttendant.get(userId);
            if (a == null) return notFound("Invalid user");
            jsonListObject = Json.newObject();
            jsonListObject.put("username", a.username);
            jsonListObject.put("id", a.id);
            jsonListObject.putArray("locations");
        }
        return ok(jsonListObject);
    }

    /**
     * Retrieve the last location entry of a specific user
     * @param userId The user ID of the user to query
     * @return A JSON object with the latest location entry of the specified user
     */
    public static Result current(Long userId) {
        FieldOperatorLocation loc = FieldOperatorLocation.current(userId);
        ObjectNode jsonLoc = createLocationObjectNode(loc);
        jsonLoc.put("username", loc.fieldOperator.username);
        jsonLoc.put("id", loc.fieldOperator.id);
        return ok(jsonLoc);
    }

    /**
     * Retrieve the last location entry for all users
     * @return A JSON object with the latest location entry for all users
     */
    public static Result currentAll() {
        List<FieldOperatorLocation> currentLocations = FieldOperatorLocation.current();
        ObjectNode wrapper = Json.newObject();
        wrapper.put("total", currentLocations.size());
        ArrayNode users = wrapper.putArray("users");
        for (FieldOperatorLocation loc : currentLocations) {
            ObjectNode currentUserLoc = createLocationObjectNode(loc);
            currentUserLoc.put("id", loc.fieldOperator.id);
            currentUserLoc.put("username", loc.fieldOperator.username);
            users.add(currentUserLoc);
        }
        return ok(wrapper);
    }

    /**
     * Register the location of the currently logged in user. Anticipates a plain JSON object with latitude
     * and longitude fields.
     * @return A JSON object node containing the same information that was provided
     */
    //@Security.Authenticated(Authorization.Authorized.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Result report() {
        JsonNode json = request().body().asJson();
        String currentUserFromSession = session().get("id");
        Logger.debug("Location report from: " + session().get("username"));
        AlarmAttendant currentUser;

        // For dev testing
        if (currentUserFromSession == null) {
            Logger.debug("Failed to fetch current user from session, defaulting to provided id");
            Long providedUserId = json.findPath("fieldOperator").asLong();
            currentUser = AlarmAttendant.get(providedUserId);
            // return unauthorized("Not authenticated");
        } else {
            currentUser = AlarmAttendant.get(Long.parseLong(currentUserFromSession));
        }

        Double latitude = json.findPath("latitude").asDouble();
        Double longitude = json.findPath("longitude").asDouble();

        FieldOperatorLocation fol = new FieldOperatorLocation();
        fol.latitude = latitude;
        fol.longitude = longitude;
        fol.fieldOperator = currentUser;

        // Save and check if everything is A-OK.
        FieldOperatorLocation savedFol = FieldOperatorLocation.create(fol);
        if (savedFol == null) return badRequest("Missing required fields.");

        ObjectNode jsonFol = createLocationObjectNode(savedFol);

        return ok(jsonFol);
    }
}
