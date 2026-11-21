import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.*;

public class LoginApp extends Application {

    private Stage window;
    private String selectedRole = ""; // "STUDENT" veya "ADVISOR"
    private String loggedInUserName = "";
    private int loggedInId = 0; // Hem StudentID hem AdvisorID için ortak kullanacağız

    // Veritabanı bağlantı bilgileri
    private static final String URL = "jdbc:mysql://localhost:3306/BIM216_Project?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci";
    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    @Override
    public void start(Stage primaryStage) {
        this.window = primaryStage;
        window.setTitle("FocusUp - Giriş Sistemi");
        showWelcomeScene();
    }

    // İlk Karşılama Ekranı (Rol Seçimi)
    public void showWelcomeScene() {
        VBox layout = new VBox(25);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.setStyle("-fx-background-color: #f4f4f4;");

        Label title = new Label("FocusUp");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Button studentBtn = new Button("ÖĞRENCİ GİRİŞİ");
        studentBtn.setPrefSize(250, 50);
        studentBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        studentBtn.setOnAction(e -> {
            selectedRole = "STUDENT";
            showLoginScene("Öğrenci Giriş Paneli");
        });

        Button advisorBtn = new Button("DANIŞMAN GİRİŞİ");
        advisorBtn.setPrefSize(250, 50);
        advisorBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        advisorBtn.setOnAction(e -> {
            selectedRole = "ADVISOR";
            showLoginScene("Danışman Giriş Paneli");
        });

        layout.getChildren().addAll(title, studentBtn, advisorBtn);
        window.setScene(new Scene(layout, 450, 550));
        window.show();
    }

    // Giriş Bilgileri Ekranı
    public void showLoginScene(String headerText) {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));

        Button backBtn = new Button("← Geri Dön");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d;");
        backBtn.setOnAction(e -> showWelcomeScene());

        Label title = new Label(headerText);
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        TextField emailField = new TextField();
        emailField.setPromptText("E-posta");
        emailField.setMaxWidth(250);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Şifre");
        passField.setMaxWidth(250);

        Button loginBtn = new Button("GİRİŞ YAP");
        loginBtn.setPrefWidth(250);
        loginBtn.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Label msgLabel = new Label();

        loginBtn.setOnAction(e -> {
            if (authenticate(emailField.getText(), passField.getText())) {
                if (selectedRole.equals("STUDENT")) {
                    System.out.println("Öğrenci Girişi Başarılı! ID: " + loggedInId);
                    StudentDashboard dashboard = new StudentDashboard(window, loggedInUserName, loggedInId);
                    dashboard.show();
                } else if (selectedRole.equals("ADVISOR")) {
                    System.out.println("Danışman Girişi Başarılı! ID: " + loggedInId);
                    // Advisor sayfasına yönlendirme
                    AdvisorDashboard advisorDashboard = new AdvisorDashboard(window, loggedInUserName, loggedInId);
                    advisorDashboard.show();
                }
            } else {
                msgLabel.setText("Hatalı e-posta veya şifre!");
                msgLabel.setStyle("-fx-text-fill: red;");
            }
        });

        layout.getChildren().addAll(backBtn, title, emailField, passField, loginBtn, msgLabel);
        window.setScene(new Scene(layout, 450, 550));
    }

    // Dinamik Kimlik Doğrulama (STUDENT veya ADVISOR tablosu)
    private boolean authenticate(String email, String pass) {
        String query;
        if (selectedRole.equals("STUDENT")) {
            query = "SELECT StudentID, ST_FullName FROM STUDENT WHERE ST_Email = ? AND ST_Password = ?";
        } else {
            // Görseldeki ADVISOR tablosu yapısına göre
            query = "SELECT AdvisorID, AD_FullName FROM ADVISOR WHERE AD_Email = ? AND AD_Password = ?";
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, pass);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                if (selectedRole.equals("STUDENT")) {
                    this.loggedInId = rs.getInt("StudentID");
                    this.loggedInUserName = rs.getString("ST_FullName");
                } else {
                    this.loggedInId = rs.getInt("AdvisorID");
                    this.loggedInUserName = rs.getString("AD_FullName");
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}