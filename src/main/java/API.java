import static spark.Spark.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.sql.DriverManager;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


import java.math.BigDecimal;

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
            + "loginTimeout=30;" // <- This is timeout to establish connection, not keeping it.
            + "database=Prototype;";

            // + "encrypt=true;"
            // + "trustServerCertificate=false;"
            // Security doesn't work for time being, can work on later.
            private static int UPDATE_LIMIT = 9;

    // private static String DATABASE_NAME = "PestsAndDisease";


    private static boolean inUK(float lat,float lang){
        if(lat <59.5 && lat > 49.5 && lang > -8.95 && lang <1.75){
            return true;
        }
        return false;
    }

    private static boolean inRange(float centreLat,float centreLang, float newLat,float newLang){
        if(newLat <centreLat+0.5 && newLat > centreLat-0.5 && newLang <centreLang+0.5 && newLang > centreLang-0.5){
            return true;
        }
        return false;
    }

    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private static void connectDB(){
        try {
                connection = DriverManager.getConnection(DATABASE_URL);
        }
        catch (SQLException e){
        log.severe(dtf.format(LocalDateTime.now())+ ":Failed to connect to database with SQL error: " + e.getMessage());
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
                    
                    String sql = "INSERT INTO PestsAndDiseases(report_id, category, date, latitude, longitude, name, description, image, solved, severity, crop)"
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

                    PreparedStatement p = connection.prepareStatement(sql);
                    p.setInt(1, reqBody.getInt("report_id"));
                    p.setString(2, reqBody.optString("category"));
                    p.setDate(3, Date.valueOf(LocalDate.now()));
                    p.setFloat(4, reqBody.optFloat("latitude"));
                    p.setFloat(5, reqBody.optFloat("longitude"));
                    p.setString(6, reqBody.optString("name"));
                    p.setString(7, reqBody.optString("description"));
                    p.setString(8, reqBody.optString("image"));
                    p.setString(9, reqBody.optString("solved"));
                    p.setString(10, reqBody.optString("severity"));
                    p.setString(11, reqBody.optString("crop"));

                    p.execute(sql);

                    res.put("complete",true);
                }
                else{
                    res.put("complete",false);
                    res.put("error","Outside UK");
                }
            }
            catch(JSONException e){
                log.warning(dtf.format(LocalDateTime.now())+":Error in parsing POST request to /api/new");
                res.put("error","Request parsing error");
                res.put("complete",false);
                response.body(res.toString());
            }
            return response;
        });

        post("/api/update",(request, response) -> {
            JSONObject reqBody = new JSONObject(request.body());
            JSONObject res = new JSONObject();
            try {
                    String query = "SELECT COUNT(*) AS total from ReportUpdates"
                            + "WHERE report_id=?";

                    PreparedStatement prep = connection.prepareStatement(query);
                    prep.setInt(1, reqBody.getInt("report_id"));
                    ResultSet resultSet = prep.executeQuery();
                    if(resultSet.getInt("total") >= UPDATE_LIMIT){
                        res.append("error","Update limit reached");
                        res.append("complete",false);
                        response.body(res.toString());
                        return response;
                    }
                String insert = "INSERT INTO UpdateInfo(update_id,description,image,date,severity) VALUES(?,?,?,?,?) ";

                PreparedStatement preparedStatement = connection.prepareStatement(insert);
                preparedStatement.setInt(1, reqBody.optInt("report_id")*10 + (resultSet.getInt("total")+1));
                preparedStatement.setString(2, reqBody.optString("description"));
                preparedStatement.setString(3, reqBody.optString("image"));
                preparedStatement.setDate(4, Date.valueOf(LocalDate.now()));
                preparedStatement.setInt(3, reqBody.optInt("severity"));
                preparedStatement.execute();

                String reportupdate = "INSERT INTO ReportUpdates(report_id,update_id) VALUES(?,?) ";

                PreparedStatement update = connection.prepareStatement(reportupdate);
                update.setInt(1, reqBody.getInt("report_id"));
                update.setInt(2, reqBody.getInt("report_id")*10 + (resultSet.getInt("total")+1));
                update.execute();

                res.append("complete",true);
                response.body(res.toString());
            }
            catch(JSONException e){
                log.warning(dtf.format(LocalDateTime.now())+":Error in parsing POST request to /api/update" + e.getMessage());
                res.put("error","Request parsing error");
                res.put("complete",false);
                response.body(res.toString());
            }
            catch (SQLException f){
                log.warning(dtf.format(LocalDateTime.now())+":Error in database connection: " + f.getMessage());
                res.put("error","Database connection error");
                res.put("complete",false);
                response.body(res.toString());
            }
            return response;
        });

        get("/api/map/pest", (request, response) -> {
            JSONObject reqBody = new JSONObject(request.body());
            JSONArray results = new JSONArray();
            try {
                if (inUK( reqBody.getFloat("latitude"),  reqBody.getFloat("longitude"))) {
                    //DB request for info on body.get("pest"); in range
                    String req = "SELECT (date, latitude, longitude, severity) FROM PestsAndDiseases WHERE name = ? AND category = 'Pest'";
                    PreparedStatement p = connection.prepareStatement(req);
                    p.setString(1, reqBody.optString("name"));
                    ResultSet resultSet = p.executeQuery();

                    // TODO: Filter by location and date
                    while (resultSet.next()) {
                        if(resultSet.getDate("date").toLocalDate().isAfter(LocalDate.now().minusDays(60))) {
                            if(inRange(reqBody.getFloat("latitude"),  reqBody.getFloat("longitude"),resultSet.getFloat("latitude"),resultSet.getFloat("longitude"))) {
                                JSONObject result = new JSONObject();
                                result.put("date", resultSet.getDate("date").toString());
                                result.put("latitude", Math.round(resultSet.getFloat("latitude")*1000)/1000 );
                                result.put("longitude", Math.round(resultSet.getFloat("longitude")*1000)/1000);
                                result.put("severity", resultSet.getInt("severity"));
                                results.put(result);
                            }
                        }
                    }

                    response.body(results.toString());
                }
            }
            catch(JSONException e){
                log.warning(dtf.format(LocalDateTime.now())+":Error in parsing GET request to /api/map/*");
                results = new JSONArray();
                JSONObject result = new JSONObject();
                result.put("error","Request parsing error");
                result.put("complete",false);
                results.put(result);
                response.body(results.toString());
            }
            catch (SQLException f){
                log.warning(dtf.format(LocalDateTime.now())+":Error in database connection: " + f.getMessage());
                results = new JSONArray();
                JSONObject result = new JSONObject();
                result.put("error","Database connection error");
                result.put("complete",false);
                results.put(result);
                response.body(results.toString());
            }
            return response;
        });


        get("/api/map/disease", (request, response) -> {
            JSONObject reqBody = new JSONObject(request.body());
            JSONArray results = new JSONArray();
            try {
                if (inUK( reqBody.getFloat("latitude"),  reqBody.getFloat("longitude"))) {
                    //DB request for info on body.get("disease"); in range
                    String req = "SELECT (date, latitude, longitude, severity) FROM PestsAndDiseases WHERE name = ? AND category = 'Disease'";
                    PreparedStatement p = connection.prepareStatement(req);
                    p.setString(1, reqBody.optString("name"));
                    ResultSet resultSet = p.executeQuery();

                    // TODO: Filter by location and date
                    while (resultSet.next()) {
                        if(resultSet.getDate("date").toLocalDate().isAfter(LocalDate.now().minusDays(60))) {
                            if(inRange(reqBody.getFloat("latitude"),  reqBody.getFloat("longitude"),resultSet.getFloat("latitude"),resultSet.getFloat("longitude"))) {
                                JSONObject result = new JSONObject();
                                result.put("date", resultSet.getDate("date").toString());
                                result.put("latitude", Math.round(resultSet.getFloat("latitude")*1000)/1000 );
                                result.put("longitude", Math.round(resultSet.getFloat("longitude")*1000)/1000);
                                result.put("severity", resultSet.getInt("severity"));
                                results.put(result);
                            }
                        }
                    }

                    response.body(results.toString());
                }
            }
            catch(JSONException e){
                log.warning(dtf.format(LocalDateTime.now())+":Error in parsing GET request to /api/map/disease");
                results = new JSONArray();
                JSONObject result = new JSONObject();
                result.put("error","Request parsing error");
                result.put("complete",false);
                results.put(result);
                response.body(results.toString());
            }
            catch (SQLException f){
                log.warning(dtf.format(LocalDateTime.now())+":Error in database connection: " + f.getMessage());
                results = new JSONArray();
                JSONObject result = new JSONObject();
                result.put("error","Database connection error");
                result.put("complete",false);
                results.put(result);
                response.body(results.toString());
            }
            return response;
        });

        // Rename if you want - not sure what to call it.
        get("/api/timeline", (request, response) -> {
            JSONArray results = new JSONArray();

            try {
                int month = LocalDate.now().getMonthValue();
                String sql = "SELECT name, category, crop FROM PestAndDiseaseInfo " +
                    "JOIN Timeline ON (Timeline.pd_id = PestAndDiseaseInfo.pd_id)" +
                    "WHERE month = ?";
                PreparedStatement p = connection.prepareStatement(sql);
                p.setInt(1, month);
                ResultSet rs = p.executeQuery();

                while (rs.next()) {
                    JSONObject result = new JSONObject();
                    result.put("name", rs.getString("name"));
                    result.put("category", rs.getString("category"));
                    result.put("crop", rs.getString("crop"));
                    results.put(result);
                }
                
                response.body(results.toString());
            } catch (SQLException e) {
                log.warning(dtf.format(LocalDateTime.now())+":Error in database connection: " + e.getMessage());
                results = new JSONArray();
                JSONObject result = new JSONObject();
                result.put("error","Database connection error");
                result.put("complete",false);
                results.put(result);
                response.body(results.toString());
            }

            return response.body();
        });

        // For testing - remember to remove?
        get("/hello", (request, response) -> "Hello.");
    }
}