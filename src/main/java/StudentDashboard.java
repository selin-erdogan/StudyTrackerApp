import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class StudentDashboard {

    private Stage stage;
    private String studentName;
    private int studentId;

    private BorderPane mainLayout;
    private BorderPane centerLayout;

    private TableView<QuizRecord> table;
    private GridPane scheduleGrid;

    // HEDEFLER İÇİN SEÇİM TAKİBİ
    private int selectedGoalId = -1;

    // VERİTABANI BAĞLANTISI
    private final String URL = "jdbc:mysql://localhost:3306/BIM216_Project?useUnicode=true&characterEncoding=UTF-8";
    private final String USER = "root";
    private final String PASS = "";

    public StudentDashboard(Stage stage, String studentName, int studentId) {
        this.stage = stage;
        this.studentName = studentName;
        this.studentId = studentId;
    }

    public void show() {
        mainLayout = new BorderPane();
        centerLayout = new BorderPane();
        mainLayout.setCenter(centerLayout);

        mainLayout.setStyle("-fx-font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Arial, sans-serif; -fx-background-color: #5C6B7A");
        stage.setTitle("FocusUp - " + studentName);

        setupTopBar();
        setupSideBar();
        showHomeContent();

        Scene scene = new Scene(mainLayout, 1200, 800);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    // --- 1. ÜST BAR ---
    private void setupTopBar() {
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(15, 40, 15, 30));
        topBar.setAlignment(Pos.CENTER);
        topBar.setStyle("-fx-background-color: #1e272e; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 3);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox userWidget = new HBox(12);
        userWidget.setAlignment(Pos.CENTER);
        Label nameLbl = new Label(studentName);
        nameLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #ffffff;");

        Circle pImg = new Circle(18, Color.web("#0984e3"));
        userWidget.getChildren().addAll(nameLbl, pImg);

        MenuButton profileMenu = new MenuButton();
        profileMenu.setGraphic(userWidget);
        profileMenu.setStyle("-fx-background-color: transparent; -fx-mark-color: transparent; -fx-cursor: hand;");

        MenuItem logout = new MenuItem("Güvenli Çıkış");
        logout.setOnAction(e -> new LoginApp().start(stage));
        profileMenu.getItems().addAll(new MenuItem("Profilim"), new MenuItem("Ayarlar"), new SeparatorMenuItem(), logout);

        topBar.getChildren().addAll(spacer, profileMenu);
        centerLayout.setTop(topBar);
    }

    // --- 2. SOL MENÜ ---
    private void setupSideBar() {
        VBox sideBar = new VBox(10);
        sideBar.setPadding(new Insets(35, 20, 25, 20));
        sideBar.setPrefWidth(260);
        sideBar.setStyle("-fx-background-color: #1e272e; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 2, 0);");

        Label logo = new Label("FOCUSUP");
        logo.setStyle("-fx-text-fill: #00cec9; -fx-font-size: 28px; -fx-font-weight: 900; -fx-letter-spacing: 2px;");
        VBox.setMargin(logo, new Insets(0, 0, 50, 10));

        Button btnHome = createMenuBtn("ANASAYFA");
        btnHome.setOnAction(e -> showHomeContent());

        Button btnQuiz = createMenuBtn("QUİZ SONUÇLARIM");
        btnQuiz.setOnAction(e -> showQuizResults());

        Button btnGoals = createMenuBtn("HEDEFLERİM");
        btnGoals.setOnAction(e -> showGoals());

        Button btnSchedule = createMenuBtn("HAFTALIK PROGRAM");
        btnSchedule.setOnAction(e -> showWeeklySchedule());

        sideBar.getChildren().addAll(logo, btnHome, btnQuiz,
                btnGoals,
                btnSchedule,
                createMenuBtn("TAVSİYELERİM"));

        mainLayout.setLeft(sideBar);
    }

    // --- 3. ANA SAYFA ---
    private void showHomeContent() {
        VBox content = new VBox(35);
        content.setPadding(new Insets(40, 50, 40, 50));
        content.setStyle("-fx-background-color: transparent;");

        Label welcome = new Label("Hoş geldin, " + studentName + "! 👋");
        welcome.setStyle("-fx-font-size: 32px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        // İstatistik kartlarını dar ekranlarda sığdırmak için FlowPane'e çevirdik
        FlowPane stats = new FlowPane(25, 25);
        stats.getChildren().addAll(
                createStatCard("Haftalık Odaklanma", "32.5 Saat", "#0984e3", "⚡"),
                createStatCard("Soru Hedefi", "740 / 1000", "#00b894", "🎯"),
                createStatCard("Sistem ID", String.valueOf(studentId), "#e17055", "🆔")
        );

        VBox progressSection = new VBox(20);
        progressSection.setPadding(new Insets(35));
        progressSection.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 18; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 15, 0, 0, 8);");

        Label prTitle = new Label("🎯 Haftalık İlerleme Durumu");
        prTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2d3436;");

        ProgressBar pb = new ProgressBar(0.74);
        pb.setPrefWidth(Double.MAX_VALUE);
        pb.setPrefHeight(20);
        pb.setStyle("-fx-accent: #00b894; -fx-control-inner-background: #dfe6e9; -fx-background-radius: 10; -fx-border-radius: 10;");

        Label pbText = new Label("%74 tamamlandı");
        pbText.setStyle("-fx-text-fill: #636e72; -fx-font-weight: 600;");

        progressSection.getChildren().addAll(prTitle, pb, pbText);
        content.getChildren().addAll(welcome, stats, progressSection);

        centerLayout.setCenter(content);

        // Mentor notunu veritabanından çekme ve gösterme
        VBox advisorNote = new VBox(10);
        advisorNote.setPadding(new Insets(20));
        advisorNote.setStyle("-fx-background-color: #ffeaa7; -fx-background-radius: 15;");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            PreparedStatement ps = conn.prepareStatement("SELECT SessionNote FROM STUDY_SESSIONS WHERE StudentID = ? AND SessionNote LIKE '[MENTOR NOTU]%' ORDER BY SessionDate DESC LIMIT 1");
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                Label msg = new Label("📣 Mentor Notu: " + rs.getString(1).replace("[MENTOR NOTU]: ", ""));
                msg.setStyle("-fx-font-weight: bold; -fx-text-fill: #d35400;");
                advisorNote.getChildren().add(msg);
                content.getChildren().add(0, advisorNote); // En üste ekle
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- 4. HEDEFLERİM SAYFASI ---
    private void showGoals() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(25, 40, 25, 40));
        content.setStyle("-fx-background-color: transparent;");

        Label title = new Label("🎯 Hedeflerim ve İlerleyişim");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        FlowPane goalsGrid = new FlowPane();
        goalsGrid.setHgap(15);
        goalsGrid.setVgap(15);
        goalsGrid.setPadding(new Insets(20));
        goalsGrid.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 15, 0, 0, 5);");

        ScrollPane scrollPane = new ScrollPane(goalsGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(380);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox actionBox = new VBox(12);
        actionBox.setPadding(new Insets(20, 25, 20, 25));
        actionBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 5);");

        TextField subInput = new TextField(); subInput.setPromptText("Örn: 203"); styleInput(subInput); subInput.setPrefWidth(90);
        TextField hoursInput = new TextField(); hoursInput.setPromptText("Saat"); styleInput(hoursInput); hoursInput.setPrefWidth(90);
        TextField scoreInput = new TextField(); scoreInput.setPromptText("Skor"); styleInput(scoreInput); scoreInput.setPrefWidth(90);

        DatePicker startInput = new DatePicker(LocalDate.now()); styleInput(startInput); startInput.setPrefWidth(125);
        DatePicker endInput = new DatePicker(LocalDate.now().plusDays(7)); styleInput(endInput); endInput.setPrefWidth(125);

        ComboBox<String> statusInput = new ComboBox<>(FXCollections.observableArrayList("Devam Ediyor", "Tamamlandı"));
        statusInput.setPromptText("Durum Seç"); styleInput(statusInput); statusInput.setPrefWidth(130);

        Button addBtn = createModernBtn("➕ Ekle", "#00b894");
        Button updateBtn = createModernBtn("🔄 Güncelle", "#0984e3");
        Button delBtn = createModernBtn("🗑 Sil", "#ff7675");
        Button clearBtn = createModernBtn("🧹 Seçimi Temizle", "#b2bec3");

        addBtn.setOnAction(e -> {
            if(subInput.getText().isEmpty() || hoursInput.getText().isEmpty() || scoreInput.getText().isEmpty() || statusInput.getValue() == null){
                showAlert("Eksik Bilgi", "Lütfen tüm alanları doldurun!");
                return;
            }
            try {
                int subId = Integer.parseInt(subInput.getText());
                double hours = Double.parseDouble(hoursInput.getText());
                double score = Double.parseDouble(scoreInput.getText());
                LocalDate startD = startInput.getValue();
                LocalDate endD = endInput.getValue();

                if (startD.isAfter(endD)) {
                    showAlert("Tarih Hatası", "Başlangıç tarihi, bitiş tarihinden sonra olamaz!");
                    return;
                }
                if (!checkSubjectExists(subId)) {
                    showAlert("Geçersiz Ders", "Girdiğiniz Ders ID (" + subId + ") sistemde bulunamadı!");
                    return;
                }

                int isComp = statusInput.getValue().equals("Tamamlandı") ? 1 : 0;
                addGoalToDB(subId, startD.toString(), hours, score, endD.toString(), isComp);
                selectedGoalId = -1;
                updateGoalsDisplay(goalsGrid, subInput, hoursInput, scoreInput, startInput, endInput, statusInput);
            } catch (NumberFormatException ex) {
                showAlert("Format Hatası", "Ders ID, Saat ve Skor alanlarına sadece sayı giriniz!");
            }
        });

        updateBtn.setOnAction(e -> {
            if(selectedGoalId == -1) {
                showAlert("Seçim Hatası", "Lütfen güncellemek için listeden bir hedef seçin.");
                return;
            }
            if(subInput.getText().isEmpty() || hoursInput.getText().isEmpty() || scoreInput.getText().isEmpty() || statusInput.getValue() == null) {
                showAlert("Eksik Bilgi", "Lütfen tüm alanları doldurun!");
                return;
            }
            try {
                int subId = Integer.parseInt(subInput.getText());
                double hours = Double.parseDouble(hoursInput.getText());
                double score = Double.parseDouble(scoreInput.getText());
                LocalDate startD = startInput.getValue();
                LocalDate endD = endInput.getValue();

                if (startD.isAfter(endD)) {
                    showAlert("Tarih Hatası", "Başlangıç tarihi, bitiş tarihinden sonra olamaz!");
                    return;
                }
                if (!checkSubjectExists(subId)) {
                    showAlert("Geçersiz Ders", "Girdiğiniz Ders ID (" + subId + ") sistemde bulunamadı!");
                    return;
                }

                int isComp = statusInput.getValue().equals("Tamamlandı") ? 1 : 0;
                updateGoalInDB(selectedGoalId, subId, startD.toString(), hours, score, endD.toString(), isComp);
                selectedGoalId = -1;
                updateGoalsDisplay(goalsGrid, subInput, hoursInput, scoreInput, startInput, endInput, statusInput);
            } catch (NumberFormatException ex) {
                showAlert("Format Hatası", "Ders ID, Saat ve Skor alanlarına sadece sayı giriniz!");
            }
        });

        delBtn.setOnAction(e -> {
            if(selectedGoalId != -1) {
                deleteGoalFromDB(selectedGoalId);
                selectedGoalId = -1;
                updateGoalsDisplay(goalsGrid, subInput, hoursInput, scoreInput, startInput, endInput, statusInput);
            }
        });

        clearBtn.setOnAction(e -> {
            selectedGoalId = -1;
            subInput.clear(); hoursInput.clear(); scoreInput.clear(); statusInput.setValue(null);
            updateGoalsDisplay(goalsGrid, subInput, hoursInput, scoreInput, startInput, endInput, statusInput);
        });

        // EKRAN KÜÇÜLÜNCE KIRPILMAYI ÖNLEYEN EFEKTİF YAPI (FlowPane)
        FlowPane formLayout = new FlowPane(15, 15);
        formLayout.setAlignment(Pos.CENTER_LEFT);
        formLayout.getChildren().addAll(
                createFormLabel("Ders ID:"), subInput,
                createFormLabel("Hedef Saat:"), hoursInput,
                createFormLabel("Hedef Skor:"), scoreInput,
                createFormLabel("Başlangıç:"), startInput,
                createFormLabel("Bitiş:"), endInput,
                statusInput,
                addBtn, updateBtn, delBtn, clearBtn
        );

        actionBox.getChildren().add(formLayout);
        updateGoalsDisplay(goalsGrid, subInput, hoursInput, scoreInput, startInput, endInput, statusInput);

        content.getChildren().addAll(title, scrollPane, actionBox);
        centerLayout.setCenter(content);
    }

    private void updateGoalsDisplay(FlowPane grid, TextField subInput, TextField hoursInput, TextField scoreInput, DatePicker startInput, DatePicker endInput, ComboBox<String> statusInput) {
        grid.getChildren().clear();

        // SİSTEM TEMASINA UYUMLU, SOĞUK VE PROFESYONEL RENK PALETİ
        String[] palette = {"#E0F7FA", "#D6EAF8", "#E8F8F5", "#F2F3F4", "#E1F5FE", "#EAECEE", "#D1F2EB", "#EBF5FB", "#F8F9F9", "#D4E6F1"};
        String[] borderPalette = {"#00BCD4", "#3498DB", "#1ABC9C", "#BDC3C7", "#03A9F4", "#95A5A6", "#16A085", "#2980B9", "#7F8C8D", "#2471A3"};

        String sql = "SELECT * FROM GOALS WHERE StudentID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int gId = rs.getInt("GoalID");
                int subId = rs.getInt("SubjectID");
                String start = rs.getString("WeekStartDate");
                double hours = rs.getDouble("TargetStudyHours");
                double score = rs.getDouble("TargetQuizScore");
                String end = rs.getString("EndDate");
                int isComp = rs.getInt("IsCompleted");

                VBox card = new VBox(8);
                card.setAlignment(Pos.CENTER);
                card.setPrefSize(160, 110);
                card.setCursor(javafx.scene.Cursor.HAND);

                int colorIndex = Math.abs(subId) % palette.length;
                String bgColor = palette[colorIndex];
                String baseBorderColor = borderPalette[colorIndex];

                if (selectedGoalId == gId) {
                    card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-border-color: #2d3436; -fx-border-radius: 12; -fx-border-width: 3; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 12, 0, 0, 6);");
                } else {
                    String borderColor = isComp == 1 ? "#00b894" : baseBorderColor;
                    double borderW = isComp == 1 ? 2.5 : 1.5;
                    card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-border-color: " + borderColor + "; -fx-border-radius: 12; -fx-border-width: " + borderW + "; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4);");
                }

                String titleText = "Ders: " + subId + (isComp == 1 ? " ✅" : "");
                Label l1 = new Label(titleText);
                l1.setStyle("-fx-text-fill: #2d3436; -fx-font-weight: 800; -fx-font-size: 15px;");

                Label l2 = new Label(hours + " Saat | " + score + " Puan");
                l2.setStyle("-fx-text-fill: #2d3436; -fx-font-weight: 700; -fx-font-size: 12px;");

                Label l3 = new Label(start + " -> " + end);
                l3.setStyle("-fx-text-fill: #636e72; -fx-font-weight: 600; -fx-font-size: 10px;");

                card.getChildren().addAll(l1, l2, l3);

                card.setOnMouseClicked(e -> {
                    selectedGoalId = gId;
                    subInput.setText(String.valueOf(subId));
                    hoursInput.setText(String.valueOf(hours));
                    scoreInput.setText(String.valueOf(score));
                    startInput.setValue(LocalDate.parse(start));
                    endInput.setValue(LocalDate.parse(end));
                    statusInput.setValue(isComp == 1 ? "Tamamlandı" : "Devam Ediyor");
                    updateGoalsDisplay(grid, subInput, hoursInput, scoreInput, startInput, endInput, statusInput);
                });

                grid.getChildren().add(card);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }


    // --- 5. HAFTALIK PROGRAM ---
    private void showWeeklySchedule() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(25, 40, 25, 40));
        content.setStyle("-fx-background-color: transparent;");

        Label title = new Label("Haftalık Çalışma Programım");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        scheduleGrid = new GridPane();
        scheduleGrid.setHgap(8);
        scheduleGrid.setVgap(8);
        scheduleGrid.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16; -fx-border-radius: 16; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 15, 0, 0, 5);");

        VBox.setVgrow(scheduleGrid, Priority.ALWAYS);

        updateScheduleDisplay();

        VBox actionBox = new VBox(12);
        actionBox.setPadding(new Insets(20, 25, 20, 25));
        actionBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 5);");

        DatePicker dateInp = new DatePicker(LocalDate.now());
        styleInput(dateInp);

        ComboBox<String> timeInp = new ComboBox<>(FXCollections.observableArrayList("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00"));
        timeInp.setPromptText("Saat Seç");
        styleInput(timeInp);

        ComboBox<String> subInp = new ComboBox<>();
        subInp.setPromptText("Ders Seçimi");
        styleInput(subInp);
        populateSubjectComboBox(subInp);

        Button addBtn = createModernBtn("➕ Ekle", "#00b894");
        addBtn.setOnAction(e -> {
            if(subInp.getValue() != null && timeInp.getValue() != null) {
                addScheduleToDB(dateInp.getValue().toString(), timeInp.getValue(), subInp.getValue());
                showWeeklySchedule();
            }
        });

        Button delBtn = createModernBtn("🗑 Temizle", "#ff7675");
        delBtn.setOnAction(e -> {
            if(timeInp.getValue() != null) {
                deleteScheduleFromDB(dateInp.getValue().toString(), timeInp.getValue());
                showWeeklySchedule();
            }
        });

        // Ekran daraldığında elemanların alt satıra geçmesi için FlowPane kullanımı
        FlowPane formLayout = new FlowPane(15, 15);
        formLayout.setAlignment(Pos.CENTER_LEFT);
        formLayout.getChildren().addAll(
                createFormLabel("Tarih:"), dateInp,
                createFormLabel("Saat:"), timeInp,
                subInp, addBtn, delBtn
        );

        actionBox.getChildren().add(formLayout);

        content.getChildren().addAll(title, scheduleGrid, actionBox);
        centerLayout.setCenter(content);
    }

    private void updateScheduleDisplay() {
        scheduleGrid.getChildren().clear();
        scheduleGrid.getColumnConstraints().clear();
        scheduleGrid.getRowConstraints().clear();

        for (int i = 0; i < 8; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 8.0);
            scheduleGrid.getColumnConstraints().add(cc);
        }

        for (int i = 0; i < 9; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / 9.0);
            scheduleGrid.getRowConstraints().add(rc);
        }

        String[] days = {"Saat", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};

        for (int i = 0; i < days.length; i++) {
            Label lbl = new Label(days[i]);
            lbl.setStyle("-fx-font-weight: 800; -fx-text-fill: #2d3436; -fx-font-size: 13px;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            scheduleGrid.add(lbl, i, 0);
        }

        for(int hour = 8; hour <= 15; hour++) {
            Label tLbl = new Label(String.format("%02d:00", hour));
            tLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #636e72;");
            tLbl.setMaxWidth(Double.MAX_VALUE);
            tLbl.setAlignment(Pos.CENTER);
            scheduleGrid.add(tLbl, 0, hour - 7);

            for(int d = 1; d <= 7; d++) {
                VBox emptyCell = new VBox();
                emptyCell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                emptyCell.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-color: #dfe6e9; -fx-border-radius: 12; -fx-border-width: 1.5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 5, 0, 0, 2);");
                scheduleGrid.add(emptyCell, d, hour - 7);
            }
        }

        String sql = "SELECT ss.SessionDate, ss.StartTime, s.SubjectName, s.Category " +
                "FROM STUDY_SESSIONS ss JOIN SUBJECT s ON ss.SubjectID = s.SubjectID " +
                "WHERE ss.StudentID = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalDate date = rs.getDate("SessionDate").toLocalDate();
                String trDay = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("tr"));
                int col = getDayColumn(trDay);
                int row = Integer.parseInt(rs.getString("StartTime").split(":")[0]) - 7;

                if (col > 0 && row > 0 && row <= 8) {
                    VBox cell = new VBox();
                    cell.setAlignment(Pos.CENTER);
                    cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                    String color = rs.getString("Category").equalsIgnoreCase("Sayısal") ? "#D6EAF8" : "#F2D7D5";
                    String borderColor = rs.getString("Category").equalsIgnoreCase("Sayısal") ? "#3498DB" : "#E74C3C";

                    cell.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12; -fx-border-color: " + borderColor + "; -fx-border-radius: 12; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4);");

                    Label l = new Label(rs.getString("SubjectName"));
                    l.setStyle("-fx-text-fill: #2d3436; -fx-font-weight: 800; -fx-font-size: 11px;");
                    l.setWrapText(true);
                    l.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

                    cell.getChildren().add(l);
                    scheduleGrid.add(cell, col, row);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- 6. QUİZ SONUÇLARI ---
    private void showQuizResults() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30, 40, 30, 40));
        content.setStyle("-fx-background-color: transparent;");

        Label title = new Label("Deneme Sınavı Kayıtlarım");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        // FİLTRE KUTUSU
        FlowPane filterBox = new FlowPane(15, 15);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setPadding(new Insets(15, 20, 15, 20));
        filterBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 10, 0, 0, 4);");

        TextField filterInput = new TextField();
        filterInput.setPromptText("Örn: 201");
        styleInput(filterInput);

        Button filterBtn = createModernBtn("🔍 Filtrele", "#0984e3");
        Button clearBtn = createModernBtn("🔄 Temizle", "#b2bec3");

        filterBtn.setOnAction(e -> {
            if (!filterInput.getText().isEmpty()) {
                refreshTable(Integer.parseInt(filterInput.getText()));
            }
        });
        clearBtn.setOnAction(e -> {
            filterInput.clear();
            refreshTable(null);
        });

        filterBox.getChildren().addAll(createFormLabel("Ders ID ile Ara:"), filterInput, filterBtn, clearBtn);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(
                "-fx-background-color: #ffffff; " +
                        "-fx-background-radius: 16; " +
                        "-fx-border-radius: 16; " +
                        "-fx-border-color: #dfe6e9; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-padding: 6; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 15, 0, 0, 5);"
        );

        TableColumn<QuizRecord, Integer> idCol = new TableColumn<>("Quiz ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("quizId"));
        TableColumn<QuizRecord, Integer> subCol = new TableColumn<>("Ders ID");
        subCol.setCellValueFactory(new PropertyValueFactory<>("subjectId"));
        TableColumn<QuizRecord, Double> scoreCol = new TableColumn<>("Skor");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        TableColumn<QuizRecord, String> dateCol = new TableColumn<>("Tarih");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("quizDate"));

        table.getColumns().addAll(idCol, subCol, scoreCol, dateCol);
        refreshTable(null);

        // İŞLEM PANELİ
        VBox actionBox = new VBox(12);
        actionBox.setPadding(new Insets(20));
        actionBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 5);");

        TextField subInput = new TextField(); subInput.setPromptText("Ders ID"); styleInput(subInput); subInput.setPrefWidth(90);
        TextField scoreInput = new TextField(); scoreInput.setPromptText("Skor"); styleInput(scoreInput); scoreInput.setPrefWidth(90);
        DatePicker dateInput = new DatePicker(LocalDate.now()); styleInput(dateInput);

        Button addBtn = createModernBtn("➕ Ekle", "#00b894");
        Button updateBtn = createModernBtn("🔄 Güncelle", "#0984e3");
        Button delBtn = createModernBtn("🗑 Sil", "#d63031");

        addBtn.setOnAction(e -> {
            if(!subInput.getText().isEmpty() && !scoreInput.getText().isEmpty()){
                addRecord(Integer.parseInt(subInput.getText()), Double.parseDouble(scoreInput.getText()), dateInput.getValue().toString());
                subInput.clear(); scoreInput.clear();
                refreshTable(null);
            }
        });

        updateBtn.setOnAction(e -> {
            QuizRecord selected = table.getSelectionModel().getSelectedItem();
            if(selected != null && !scoreInput.getText().isEmpty()) {
                updateRecord(selected.getQuizId(), Double.parseDouble(scoreInput.getText()));
                scoreInput.clear();
                refreshTable(null);
            }
        });

        delBtn.setOnAction(e -> {
            QuizRecord selected = table.getSelectionModel().getSelectedItem();
            if(selected != null) {
                deleteRecord(selected.getQuizId());
                refreshTable(null);
            }
        });

        FlowPane formLayout = new FlowPane(15, 15);
        formLayout.setAlignment(Pos.CENTER_LEFT);
        formLayout.getChildren().addAll(subInput, scoreInput, dateInput, addBtn, updateBtn, delBtn);

        actionBox.getChildren().add(formLayout);

        content.getChildren().addAll(title, filterBox, table, actionBox);
        centerLayout.setCenter(content);
    }


    // --- HEDEFLER VERİTABANI İŞLEMLERİ ---
    private void addGoalToDB(int subId, String start, double hours, double score, String end, int isComp) {
        String sql = "INSERT INTO GOALS (StudentID, SubjectID, WeekStartDate, TargetStudyHours, TargetQuizScore, EndDate, IsCompleted) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId); stmt.setInt(2, subId); stmt.setString(3, start);
            stmt.setDouble(4, hours); stmt.setDouble(5, score); stmt.setString(6, end); stmt.setInt(7, isComp);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateGoalInDB(int goalId, int subId, String start, double hours, double score, String end, int isComp) {
        String sql = "UPDATE GOALS SET SubjectID = ?, WeekStartDate = ?, TargetStudyHours = ?, TargetQuizScore = ?, EndDate = ?, IsCompleted = ? WHERE GoalID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subId); stmt.setString(2, start); stmt.setDouble(3, hours);
            stmt.setDouble(4, score); stmt.setString(5, end); stmt.setInt(6, isComp); stmt.setInt(7, goalId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void deleteGoalFromDB(int goalId) {
        String sql = "DELETE FROM GOALS WHERE GoalID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, goalId); stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }


    // --- DİĞER VERİTABANI İŞLEMLERİ ---
    private void refreshTable(Integer filterSubId) {
        ObservableList<QuizRecord> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM QUIZ_RECORDS WHERE StudentID = ?";

        if (filterSubId != null) sql += " AND SubjectID = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            if (filterSubId != null) stmt.setInt(2, filterSubId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new QuizRecord(rs.getInt("QuizID"), rs.getInt("SubjectID"), rs.getDouble("Score"), rs.getString("QuizDate")));
            }
            table.setItems(list);
            if(list.isEmpty()) {
                Label ph = new Label("Gösterilecek kayıt bulunamadı.");
                ph.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 14px;");
                table.setPlaceholder(ph);
            }
        } catch (SQLException e) { System.err.println("Tablo yükleme hatası: " + e.getMessage()); }
    }

    private void addRecord(int sub, double score, String date) {
        String sql = "INSERT INTO QUIZ_RECORDS (StudentID, SubjectID, Score, MaxScore, MinScore, QuizDate) VALUES (?, ?, ?, 40, 0, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId); stmt.setInt(2, sub); stmt.setDouble(3, score); stmt.setString(4, date);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateRecord(int qId, double newScore) {
        String sql = "UPDATE QUIZ_RECORDS SET Score = ? WHERE QuizID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newScore); stmt.setInt(2, qId); stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void deleteRecord(int qId) {
        String sql = "DELETE FROM QUIZ_RECORDS WHERE QuizID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, qId); stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addScheduleToDB(String date, String time, String subName) {
        String sql = "INSERT INTO STUDY_SESSIONS (StudentID, SubjectID, SessionDate, StartTime, EndTime) " +
                "VALUES (?, (SELECT SubjectID FROM SUBJECT WHERE SubjectName = ? LIMIT 1), ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId); stmt.setString(2, subName); stmt.setString(3, date);
            stmt.setString(4, time + ":00"); stmt.setString(5, time + ":40");
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void deleteScheduleFromDB(String date, String time) {
        String sql = "DELETE FROM STUDY_SESSIONS WHERE StudentID = ? AND SessionDate = ? AND StartTime LIKE ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId); stmt.setString(2, date); stmt.setString(3, time + "%");
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void populateSubjectComboBox(ComboBox<String> box) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SubjectName FROM SUBJECT")) {
            while (rs.next()) box.getItems().add(rs.getString("SubjectName"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private int getDayColumn(String day) {
        day = day.toLowerCase();
        if (day.contains("pazartesi")) return 1; if (day.contains("salı")) return 2;
        if (day.contains("çarşamba")) return 3; if (day.contains("perşembe")) return 4;
        if (day.contains("cuma")) return 5; if (day.contains("cumartesi")) return 6;
        if (day.contains("pazar")) return 7; return 0;
    }

    // --- YARDIMCI GÖRSEL VE KONTROL METODLARI ---
    private void styleInput(Control input) {
        input.setStyle("-fx-background-color: #f5f6fa; -fx-border-color: #dcdde1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-pref-height: 36px; -fx-padding: 0 10 0 10; -fx-font-size: 13px; -fx-text-fill: #2d3436;");
    }

    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #636e72;");
        return lbl;
    }

    private Button createModernBtn(String text, String bgColor) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8; -fx-padding: 8 18 8 18; -fx-cursor: hand; -fx-font-size: 13px;");
        // Minimum genişlik atanarak butonların ezilmesi / 3 nokta olması önlendi
        btn.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private VBox createStatCard(String title, String value, String color, String icon) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(25));
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 18; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 20, 0, 0, 8);");

        HBox topArea = new HBox();
        topArea.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #636e72; -fx-font-size: 14px; -fx-font-weight: 600;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label i = new Label(icon); i.setStyle("-fx-font-size: 18px;");
        topArea.getChildren().addAll(t, spacer, i);

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + color + ";");
        card.getChildren().addAll(topArea, v);
        return card;
    }

    private Button createMenuBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(14, 20, 14, 20));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a4b0be; -fx-font-weight: 600; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 10;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2f3640; -fx-text-fill: #ffffff; -fx-font-weight: 600; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 10;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a4b0be; -fx-font-weight: 600; -fx-font-size: 14px; -fx-background-radius: 10;"));
        return btn;
    }

    // Mac'te uyarıların arka plana atmasını engelleyen (initOwner) özel uyarı metodu
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(stage); // <-- ÇÖZÜM BURADA: Artık ana pencereye kilitli
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private boolean checkSubjectExists(int subjectId) {
        String sql = "SELECT 1 FROM SUBJECT WHERE SubjectID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class QuizRecord {
        private int quizId, subjectId;
        private double score;
        private String quizDate;
        public QuizRecord(int qId, int sId, double s, String d) { this.quizId = qId; this.subjectId = sId; this.score = s; this.quizDate = d; }
        public int getQuizId() { return quizId; }
        public int getSubjectId() { return subjectId; }
        public double getScore() { return score; }
        public String getQuizDate() { return quizDate; }
    }
}