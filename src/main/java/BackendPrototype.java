import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class BackendPrototype {

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void main(String[] args) {
        String connectionUrl = "jdbc:sqlserver://localhost;"
        + "user=sa;"
        + "password=Foxtrot6;"
        + "loginTimeout=30;"
        + "database=Prototype;";
    
        try (Connection connection = DriverManager.getConnection(connectionUrl)) {
            // File f = new File("test_image.jpg");
            // FileInputStream fis = new FileInputStream(f);

            // int report_id = new Random().nextInt(999999);
            // PreparedStatement idCheck = connection.prepareStatement("SELECT COUNT(*) FROM PestsAndDiseases WHERE report_id = ?");
            // idCheck.setInt(1, report_id);
            // ResultSet rs = idCheck.executeQuery();
            // rs.next();
            // while (rs.getInt(1) > 0){
            //     report_id = new Random().nextInt(999999);
            //     idCheck.setInt(1, report_id);
            //     rs.close();
            //     rs = idCheck.executeQuery();
            //     rs.next();
            // }

            // String sql = "INSERT INTO PestsAndDiseases(report_id, category, date, latitude, longitude, name, description, image, solved,severity)"
            //     + "VALUES (?,?,?,?,?,?,?,?,?,?)";
            // PreparedStatement p = connection.prepareStatement(sql);
            // p.setInt(1, new Random().nextInt(1000000));
            // p.setString(2, "Pest");
            // p.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
            // p.setFloat(4, Double.valueOf(3.5/2.6).floatValue());
            // p.setFloat(5, Double.valueOf(Math.PI).floatValue());
            // p.setString(6, "Example");
            // p.setString(7, "This example is an example.");
            // p.setString(8, null);
            // p.setString(9, "complete");
            // p.setString(10, "3");
            // p.executeUpdate();

            // int month = LocalDate.now().getMonthValue();
            // String sql = "SELECT name, category, crop FROM PestAndDiseaseInfo " +
            //     "JOIN Timeline ON (Timeline.pd_id = PestAndDiseaseInfo.pd_id)" +
            //     "WHERE month = ?";
            // PreparedStatement p = connection.prepareStatement(sql);
            // p.setInt(1, month);
            // ResultSet rs = p.executeQuery();

            // while (rs.next()) {
            //     System.out.println(
            //         "Name: " + rs.getString("name") +
            //         " Category: " + rs.getString("category") +
            //         " Crop: " + rs.getString("crop")
            //     );
            // }

            String req = "SELECT name, date, severity FROM PestsAndDiseases WHERE date > ? AND ABS (latitude - ?) < 0.5 AND ABS (longitude - ?) < 0.5" +
                "ORDER BY ABS ((latitude - ?) * (latitude - ?) + (longitude - ?) * (longitude - ?))";
            PreparedStatement p = connection.prepareStatement(req);
            p.setDate(1, Date.valueOf(LocalDate.now().minusDays(14)));
            p.setFloat(2, 1.34f);
            p.setFloat(3, 3.14f);
            p.setFloat(4, 1.34f);
            p.setFloat(5, 1.34f);
            p.setFloat(6, 3.14f);
            p.setFloat(7, 3.14f);

            ResultSet resultSet = p.executeQuery();

            while (resultSet.next()) {
                System.out.println("Name: " + resultSet.getString("name"));
                System.out.println("Date: " + resultSet.getString("date"));
                System.out.println("Severity: " + resultSet.getString("severity"));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}