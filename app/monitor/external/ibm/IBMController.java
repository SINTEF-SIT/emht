package monitor.external.ibm;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Created by Aleksander Skraastad (myth) on 7/10/15.
 */
public class IBMController extends Controller {

    /**
     * Endpoint for receiving callbacks from the IBM Monitor
     * @return A Result object HTTP Response
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result monitorCallback() {
        JsonNode message = request().body().asJson();
        return ok();
    }
}
