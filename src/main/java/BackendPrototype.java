import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BackendPrototype {

    public static void main(String[] args) {
        String connectionUrl = "jdbc:sqlserver://localhost;"
        + "user=sa;"
        + "password=Foxtrot6;"
        + "loginTimeout=30;"
        + "database=Prototype;";
    
        try (
            Connection connection = DriverManager.getConnection(connectionUrl);
            Statement statement = connection.createStatement();
        ) {
            ResultSet resultSet = null;

            String select = "SELECT * FROM PestsAndDiseases;";
            resultSet = statement.executeQuery(select);

            while (resultSet.next()) {
                for (int i = 1; i <= 9; i++) {
                    System.out.println(resultSet.getString(i));
                }                
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}