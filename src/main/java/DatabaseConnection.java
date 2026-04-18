import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

     private static final String URL = "jdbc:mysql://localhost:3307/BIM216_Project?useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
     private static final String USER = "root";
     private static final String PASSWORD = "";

    public static void main(String[] args) {
         System.out.println("Bağlantı deneniyor...");

         try {
             // 2. MySQL'in kapısını çalıyoruz
             Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             System.out.println("Veritabanına başarıyla bağlandınız!");

             connection.close();

         } catch (SQLException e) {
             System.out.println("Bağlantı hatası: " + e.getMessage());
         }
     }
}