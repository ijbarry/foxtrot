import static spark.Spark.get;

import org.json.JSONException;
import org.json.JSONObject;

public class API {
    public static void main(String[] args) {


        get("api/map/*", (request, response) -> {
            JSONObject reqBody = new JSONObject( request.body());
            JSONObject res = new JSONObject();
            try {
                if (inUK( reqBody.getFloat("longitude"),  reqBody.getFloat("longitude"))) {
                    //DB request for info on body.get("pest"); in range
                }
            }
            catch(JSONException e){
                res.append("error","Request parsing error");
                response.body(res.toString());
            }
            catch(DBError e){
                res.append("error","Database error");
                response.body(res.toString());
            }
            return response;
        });
    }
    private static boolean inUK(float lat,float lang){
        if(lat <59.5 && lat > 49.5 && lang > -8.95 && lang <1.75){
            return true;
        }
        return false;
    }
}