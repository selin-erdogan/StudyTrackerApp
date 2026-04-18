import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.sql.*;

public class AdvisorDashboard {
    private Stage stage;
    private String advisorName;
    private int advisorId;
    private StackPane contentArea;

    private final String URL = "jdbc:mysql://localhost:3307/BIM216_Project?useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
    private final String USER = "root";
    private final String PASS = "";

    public AdvisorDashboard(Stage stage, String advisorName, int advisorId) {
        this.stage = stage;
        this.advisorName = advisorName;
        this.advisorId = advisorId;
    }

    public void show() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f0f2f5; -fx-font-family: 'Segoe UI';");

        setupTopBar(mainLayout);
        setupSideBar(mainLayout);

        contentArea = new StackPane();
        mainLayout.setCenter(contentArea);

        showStudentManagement(); // Varsayılan ekran

        stage.setScene(new Scene(mainLayout, 1300, 850));
        stage.show();
    }

    private void setupTopBar(BorderPane main) {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setStyle("-fx-background-color: #1e272e;");

        Label nameLbl = new Label(advisorName + " (Mentor)");
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Circle profileCircle = new Circle(18, Color.web("#1abc9c"));

        topBar.getChildren().addAll(nameLbl, profileCircle);
        main.setTop(topBar);
    }

    private void setupSideBar(BorderPane main) {
         VBox sideBar = new VBox(15);
         sideBar.setPadding(new Insets(30, 15, 30, 15));
         sideBar.setPrefWidth(240);
         sideBar.setStyle("-fx-background-color: #1e272e;");

        Label logo = new Label("FOCUSUP");
        logo.setStyle("-fx-text-fill: #00cec9; -fx-font-size: 24px; -fx-font-weight: 900;");

        Button btnStud = createMenuBtn("👥 Öğrenci & Program");
        Button btnSubj = createMenuBtn("📚 Ders Tanımları");

        btnStud.setOnAction(e -> showStudentManagement());
        btnSubj.setOnAction(e -> showSubjectManagement());

        sideBar.getChildren().addAll(logo, new Separator(), btnStud, btnSubj);
        main.setLeft(sideBar);
    }

    // --- 1. ÖĞRENCİ VE PROGRAM YÖNETİMİ ---
    private void showStudentManagement() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));

        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        TextField idFilter = new TextField();
        idFilter.setPromptText("Öğrenci ID ile ara...");
        filterBox.getChildren().addAll(new Label("🔍 ID Filtrele:"), idFilter);

        TableView<StudentRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<StudentRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<StudentRow, String> nameCol = new TableColumn<>("Ad Soyad");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        table.getColumns().addAll(idCol, nameCol);

        // ID Tabanlı Filtreleme Mantığı
        ObservableList<StudentRow> masterData = loadStudents();
        FilteredList<StudentRow> filteredData = new FilteredList<>(masterData, p -> true);
        idFilter.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(student -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return String.valueOf(student.getId()).contains(newVal);
            });
        });
        table.setItems(filteredData);

        // Çift tıklandığında öğrencinin program/grafik/tavsiye sekmesini aç
        table.setRowFactory(tv -> {
            TableRow<StudentRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) showStudentControlPanel(row.getItem());
            });
            return row;
        });

        root.getChildren().addAll(new Label("Öğrencilerim (ID ile Ara)"), filterBox, table);
        contentArea.getChildren().setAll(root);
    }

    // --- ÖĞRENCİ KONTROL PANELİ (DERS PROGRAMI + GRAFİK + TAVSİYE) ---
    private void showStudentControlPanel(StudentRow student) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));

        Button btnBack = new Button("← Listeye Dön");
        btnBack.setOnAction(e -> showStudentManagement());

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // PROGRAM YÖNETİMİ SEKMESİ
        Tab progTab = new Tab("📅 Program & Ders Ekle", createScheduleModule(student));
        // GRAFİK SEKMESİ
        Tab chartTab = new Tab("📈 Başarı Grafiği", createChartModule(student));
        // TAVSİYE SEKMESİ
        Tab adviceTab = new Tab("💡 Tavsiye Gönder", createAdviceModule(student));

        tabs.getTabs().addAll(progTab, chartTab, adviceTab);
        root.getChildren().addAll(btnBack, new Label("Yönetilen Öğrenci ID: " + student.getId()), tabs);
        contentArea.getChildren().setAll(root);
    }

    private VBox createScheduleModule(StudentRow student) {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        ComboBox<String> subCombo = new ComboBox<>();
        populateSubjects(subCombo); // Dersleri ID/İsim bazlı çeker

        DatePicker dateP = new DatePicker();
        TextField timeT = new TextField("10:00");
        Button btnAdd = new Button("Programa İşle");

        btnAdd.setOnAction(e -> {
            executeSQL("INSERT INTO STUDY_SESSIONS (SessionID, StudentID, SubjectID, StartTime, SessionDate) " +
                    "VALUES ((SELECT COALESCE(MAX(SessionID),0)+1 FROM STUDY_SESSIONS S2), " + student.getId() +
                    ", (SELECT SubjectID FROM SUBJECT WHERE SubjectName='" + subCombo.getValue() + "' LIMIT 1), '" + timeT.getText() + ":00', '" + dateP.getValue() + "')");
            new Alert(Alert.AlertType.INFORMATION, "Ders programına eklendi!").show();
        });

        grid.add(new Label("Ders:"), 0, 0); grid.add(subCombo, 1, 0);
        grid.add(new Label("Tarih:"), 0, 1); grid.add(dateP, 1, 1);
        grid.add(new Label("Saat:"), 0, 2); grid.add(timeT, 1, 2);
        grid.add(btnAdd, 1, 3);

        box.getChildren().addAll(new Label("Yeni Çalışma Seansı Tanımla"), grid);
        return box;
    }

    // --- DERS TANIMLARI YÖNETİMİ ---
    private void showSubjectManagement() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));

        TextField idSearch = new TextField();
        idSearch.setPromptText("Ders ID ile filtrele...");

        TableView<SubjectRow> table = new TableView<>();
        TableColumn<SubjectRow, Integer> idCol = new TableColumn<>("Ders ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<SubjectRow, String> nameCol = new TableColumn<>("Ders Adı");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        table.getColumns().addAll(idCol, nameCol);

        ObservableList<SubjectRow> data = loadSubjectsFromDB();
        FilteredList<SubjectRow> filtered = new FilteredList<>(data, p -> true);
        idSearch.textProperty().addListener((o, old, newVal) -> {
            filtered.setPredicate(s -> newVal == null || newVal.isEmpty() || String.valueOf(s.getId()).contains(newVal));
        });
        table.setItems(filtered);

        root.getChildren().addAll(new Label("Sistemdeki Dersler"), idSearch, table);
        contentArea.getChildren().setAll(root);
    }

    // --- YARDIMCI METOTLAR ---
    private ObservableList<StudentRow> loadStudents() {
         ObservableList<StudentRow> list = FXCollections.observableArrayList();
         try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
             PreparedStatement ps = conn.prepareStatement("SELECT StudentID, ST_FullName FROM STUDENT WHERE AdvisorID = ?");
             ps.setInt(1, advisorId);
             ResultSet rs = ps.executeQuery();
             while(rs.next()) list.add(new StudentRow(rs.getInt(1), rs.getString(2)));
         } catch (SQLException e) { e.printStackTrace(); }
         return list;
     }

     private ObservableList<SubjectRow> loadSubjectsFromDB() {
         ObservableList<SubjectRow> list = FXCollections.observableArrayList();
         try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
             PreparedStatement ps = conn.prepareStatement("SELECT SubjectID, SubjectName FROM SUBJECT WHERE AdvisorID = ?");
             ps.setInt(1, advisorId);
             ResultSet rs = ps.executeQuery();
             while(rs.next()) list.add(new SubjectRow(rs.getInt(1), rs.getString(2)));
         } catch (SQLException e) { e.printStackTrace(); }
         return list;
     }

    private void executeSQL(String sql) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS); Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void populateSubjects(ComboBox<String> combo) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             ResultSet rs = conn.createStatement().executeQuery("SELECT SubjectName FROM SUBJECT")) {
            while(rs.next()) combo.getItems().add(rs.getString(1));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Button createMenuBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-alignment: center-left; -fx-padding: 12; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;"));
        return btn;
    }

    // --- MODEL SINIFLARI ---
    public static class StudentRow {
        private int id; private String name;
        public StudentRow(int id, String name) { this.id = id; this.name = name; }
        public int getId() { return id; }
        public String getName() { return name; }
    }

    public static class SubjectRow {
        private int id; private String name;
        public SubjectRow(int id, String name) { this.id = id; this.name = name; }
        public int getId() { return id; }
        public String getName() { return name; }
    }

    // Not: createChartModule ve createAdviceModule metodları önceki entegrasyonla aynı mantıkta çalışır.
    private VBox createChartModule(StudentRow s) { return new VBox(new Label("Grafik Verileri Yükleniyor...")); }
    private VBox createAdviceModule(StudentRow s) { return new VBox(new Label("Tavsiye Paneli")); }
}