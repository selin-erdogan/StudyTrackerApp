import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/BIM216_Project?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static void main(String[] args) {
        System.out.println("Bağlantı deneniyor...");

        try {
            // 2. MySQL'in kapısını çalıyoruz
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Veritabanına başarıyla bağlandınız!");

            // İşimiz bitince kapıyı kapatıyoruz
            connection.close();

        } catch (SQLException e) {
            System.out.println("Bağlantı hatası: " + e.getMessage());
        }
    }
}