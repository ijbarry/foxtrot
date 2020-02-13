import static spark.Spark.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class API {
    private static final Logger log = Logger.getLogger(API.class.getName());
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static Handler errorHandler  = null;
    private static Handler configHandler  = null;
    private static Connection connection = null;

    //MACROS
    // TODO: Use a login other than server admin (sa).
    // TODO: Change from the prototype database.
    private static String DATABASE_URL = "jdbc:sqlserver://localhost;"
            + "user=sa;"
            + "password=Foxtrot6;"
            + "database=Prototype;";

            // No login timeout rn - do we want this, or make new connection each time with timeout?

            // + "encrypt=true;"
            // + "trustServerCertificate=false;"
            // Security doesn't work for time being, can work on later.

    // private static String DATABASE_NAME = "PestsAndDisease";


    private static boolean inUK(float lat,float lang){
        if(lat <59.5 && lat > 49.5 && lang > -8.95 && lang <1.75){
            return true;
        }
        return false;
    }

    private static void connectDB(){
        try {
            connection = DriverManager.getConnection(DATABASE_URL);
        }
        catch (SQLException e){
        log.severe(dtf.format(LocalDateTime.now())+ ":Failed to connect to database with error: " + e.getMessage());
        }
    }

    private static void setUpLogs(){
        try{
            //Creating consoleHandler and fileHandler
            errorHandler  = new FileHandler("./error.log",true);
            configHandler  = new FileHandler("./config.log",true);
            //Assigning handlers to LOGGER object
            log.addHandler(errorHandler);
            log.addHandler(configHandler);
            //Setting levels to handlers and LOGGER
            errorHandler.setLevel(Level.WARNING);
            configHandler.setLevel(Level.CONFIG);
            log.setLevel(Level.ALL);
            log.config(dtf.format(LocalDateTime.now())+": Log set up");
        }
        catch (IOException f){
            System.err.println(dtf.format(LocalDateTime.now())+":Log file failed"+ f.getMessage());
        }
    }


    public static void main(String[] args) {
        connectDB();
        setUpLogs();

        before((request, response) -> response.type("JSON"));

        post("/api/new",(request, response) -> {
            JSONObject reqBody = new JSONObject(request.body());
            JSONObject res = new JSONObject();
            try {
                if (inUK( reqBody.getFloat("latitude"),  reqBody.getFloat("longitude"))) {

                    String sql = "INSERT PestsAndDiseases"
                            + "(report_id, category, date, latitude, longitude, name, description, image, solved)"
                            + "VALUES (?,?,?,?,?,?,?,?,?)";

                    PreparedStatement p = connection.prepareStatement(sql);
                    p.setString(1, reqBody.getString("report_id"));
                    p.setString(2, reqBody.optString("category"));
                    p.setString(3, reqBody.optString("date",dtf.format(LocalDateTime.now())));
                    p.setString(4, reqBody.optString("latitude"));
                    p.setString(5, reqBody.optString("longitude"));
                    p.setString(6, reqBody.optString("name"));
                    p.setString(7, reqBody.optString("description"));
                    p.setString(8, reqBody.optString("image"));
                    p.setString(9, reqBody.optString("solved"));

                    p.execute(sql);

                    res.append("complete",true);
                }
                else{
                    res.append("complete",false);
                    res.append("error","Outside UK");
                }
            }
            catch(JSONException e){
                log.warning(dtf.format(LocalDateTime.now())+":Error in parsing POST request to /api/new");
                res.append("error","Request parsing error");
                res.append("complete",false);
                response.body(res.toString());
            }
            return response;
        });

        post("/api/update",(request, response) -> {
            JSONObject reqBody = new JSONObject(request.body());
            JSONObject res = new JSONObject();
            try {
                if (inUK( reqBody.getFloat("latitude"),  reqBody.getFloat("longitude"))) {

                    String query = "SELECT (description, image, solved) from PestsAndDiseases"
                            + "WHERE report_id=?";

                    PreparedStatement prep = connection.prepareStatement(query);
                    prep.setString(1, reqBody.getString("report_id"));
                    ResultSet resultSet = prep.executeQuery(query);

                    if(resultSet.getString("solved") != ""){
                        res.append("error","Case closed");
                        res.append("complete",false);
                    }
                    else {
                        // add new description/image
                        String insert = "UPDATE PestsAndDiseases SET description=?,image=?,solved=? WHERE report_id = ?";
                        PreparedStatement p = connection.prepareStatement(insert);
                        p.setString(2,resultSet.getString("description")+reqBody.optString("description"));
                        if(resultSet.getString("image") == ""){
                            p.setString(2, reqBody.optString("image"));
                        }
                        else{
                            p.setString(2,resultSet.getString("image"));
                        }
                        p.setString(4, reqBody.optString("solved"));
                        p.setString(4, reqBody.getString("report_id"));

                        res.append("complete", true);
                    }
                }
                else{
                    res.append("complete",false);
                    res.append("error","Outside UK");
                }
            }
            catch(JSONException e){
                log.warning(dtf.format(LocalDateTime.now())+":Error in parsing POST request to /api/update" + e.getMessage());
                res.append("error","Request parsing error");
                res.append("complete",false);
                response.body(res.toString());
            }
            return response;
        });

        get("/api/map/*", (request, response) -> {
            JSONObject reqBody = new JSONObject(request.body());
            JSONObject res = new JSONObject();
            try {
                if (inUK( reqBody.getFloat("longitude"),  reqBody.getFloat("longitude"))) {
                    //DB request for info on body.get("pest"); in range
                    Statement statement = connection.createStatement();
                    String sql = "SELECT date, latitude, longitude, description " +
                        "FROM PestsAndDiseases " +
                        "WHERE name = " + reqBody.getString("name") +
                        " AND category = 'Pest'";
                    // TODO: Filter by location.

                    ResultSet resultSet = statement.executeQuery(sql);
                    while (resultSet.next()) {
                        res.append("date", resultSet.getString(1));
                        res.append("latitude", resultSet.getString(2));
                        res.append("longitude", resultSet.getString(3));
                        res.append("description", resultSet.getString(4));
                    }
                }
            }
            catch(JSONException e){
                log.warning(dtf.format(LocalDateTime.now())+":Error in parsing GET request to /api/map/*");
                res.append("error","Request parsing error");
                response.body(res.toString());
            }

            return response;
        });
    }
}