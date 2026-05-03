import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import java.util.*;

public class AdvisorDashboard {

    private Stage  stage;
    private String advisorName;
    private int    advisorId;

    private StackPane contentArea;
    private GridPane  scheduleGrid;

    private VBox selectedCell = null;
    private LocalDate selectedDate = null;
    private String selectedTime = null;

    public AdvisorDashboard(Stage stage, String advisorName, int advisorId) {
        this.stage       = stage;
        this.advisorName = advisorName;
        this.advisorId   = advisorId;
    }

    private final String URL = "jdbc:mysql://localhost:3306/BIM216_Project?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci";
    private final String USER = "root";
    private final String PASS = "";

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#5C6B7A; -fx-font-family:'Segoe UI';");

        buildTopBar(root);
        buildSideBar(root);

        contentArea = new StackPane();
        root.setCenter(contentArea);

        showHome();

        stage.setScene(new Scene(root, 1300, 840));
        stage.setTitle("FocusUp – " + advisorName + " (Mentor)");
        stage.show();
    }

    private void buildTopBar(BorderPane root) {
        HBox bar = new HBox();
        bar.setPadding(new Insets(0, 24, 0, 24));
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPrefHeight(52);
        bar.setStyle("-fx-background-color:#1e272e;");

        Label nameL = new Label(advisorName + " (Mentor)");
        nameL.setStyle("-fx-text-fill:#fff; -fx-font-weight:700; -fx-font-size:13px;");
        Circle av = new Circle(17, Color.web("#1abc9c"));

        HBox chip = new HBox(10, nameL, av);
        chip.setAlignment(Pos.CENTER);

        MenuButton mb = new MenuButton();
        mb.setGraphic(chip);
        mb.setStyle("-fx-background-color:transparent; -fx-mark-color:transparent; -fx-cursor:hand;");

        MenuItem logout = new MenuItem("Güvenli Çıkış");
        logout.setOnAction(e -> {
            try {
                new LoginApp().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        mb.getItems().addAll(
                new MenuItem("Profilim"),
                new MenuItem("Ayarlar"),
                new SeparatorMenuItem(),
                logout
        );

        bar.getChildren().add(mb);
        root.setTop(bar);
    }

    private void buildSideBar(BorderPane root) {
        VBox sb = new VBox(4);
        sb.setPadding(new Insets(28, 14, 20, 14));
        sb.setPrefWidth(230);
        sb.setStyle("-fx-background-color:#1e272e;");

        Label logo = new Label("FOCUSUP");
        logo.setStyle("-fx-text-fill:#00cec9; -fx-font-size:22px; -fx-font-weight:900; -fx-letter-spacing:3px;");
        VBox.setMargin(logo, new Insets(0, 0, 32, 6));

        Button b1 = navBtn("ANASAYFA");
        b1.setOnAction(e -> showHome());

        Button b2 = navBtn("ÖĞRENCİ YÖNETİMİ");
        b2.setOnAction(e -> showStudentList());

        Button b3 = navBtn("DERS TANIMLARI");
        b3.setOnAction(e -> showSubjects());

        sb.getChildren().addAll(logo, b1, b2, b3);
        root.setLeft(sb);
    }

    private void showHome() {
        VBox page = page();

        Label title = new Label("Hoş geldin, " + advisorName + "! 👋");
        title.setStyle("-fx-font-size:26px; -fx-font-weight:800; -fx-text-fill:#fff;");

        HBox stats = new HBox(16);
        stats.getChildren().addAll(
                statCard("Toplam Öğrenci", String.valueOf(dbStudents().size()), "#0984e3"),
                statCard("Tanımlı Ders",   String.valueOf(dbSubjects().size()), "#6c5ce7")
        );

        VBox notKutu = card();
        Label notBas = new Label("📝 Mentor Notlarım");
        notBas.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#2d3436;");

        ListView<String> notList = new ListView<>();
        notList.setPrefHeight(150);
        notList.getItems().addAll(
                "Deniz'in AYT Mat notları %18 arttı — pozitif geribildirim ver.",
                "Mert Kaya seans sayısını 7'den 12'ye çıkardı. Tükenmişlik riski.",
                "Sınav dönemi yaklaşıyor. Strateji planla."
        );

        TextField notGir = tf("Yeni not ekle...");
        Button notEkle   = btn("+ Ekle", "#00b894");
        Button notSil    = btn("Sil",    "#ff7675");

        notEkle.setOnAction(e -> {
            if (!notGir.getText().isBlank()) {
                notList.getItems().add(0, notGir.getText().trim());
                notGir.clear();
            }
        });

        notSil.setOnAction(e -> {
            int i = notList.getSelectionModel().getSelectedIndex();
            if (i >= 0) {
                notList.getItems().remove(i);
            }
        });

        FlowPane notForm = flow();
        notForm.getChildren().addAll(notGir, notEkle, notSil);
        HBox.setHgrow(notGir, Priority.ALWAYS);

        notKutu.getChildren().addAll(notBas, notList, notForm);

        page.getChildren().addAll(title, stats, notKutu);
        wrap(page);
    }

    private void showStudentList() {
        VBox page = page();
        page.getChildren().add(pageTitle("Öğrenci Yönetimi"));

        TextField filter = tf("Öğrenci ID ile filtrele...");
        filter.setMaxWidth(280);

        TableView<StudentRow> tbl = new TableView<>();
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        styleTable(tbl);

        tbl.getColumns().addAll(
                col("ID",       "id"),
                col("Ad Soyad", "name"),
                col("E-posta",  "email")
        );

        ObservableList<StudentRow> data = dbStudents();
        FilteredList<StudentRow> filt = new FilteredList<>(data, p -> true);

        filter.textProperty().addListener((o, old, nv) ->
                filt.setPredicate(s -> nv == null || nv.isBlank()
                        || String.valueOf(s.getId()).contains(nv)
                        || s.getName().toLowerCase().contains(nv.toLowerCase()))
        );

        tbl.setItems(filt);

        tbl.setRowFactory(tv -> {
            TableRow<StudentRow> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    showStudentPanel(row.getItem());
                }
            });
            return row;
        });

        TextField tfName = tf("Ad Soyad");
        TextField tfEmail = tf("E-posta");

        PasswordField tfPass = new PasswordField();
        tfPass.setPromptText("Şifre");
        styleCtrl(tfPass);

        Button addBtn = btn("+ Ekle", "#00b894");
        Button delBtn = btn("Sil",    "#ff7675");

        addBtn.setOnAction(e -> {
            if (!tfName.getText().isBlank() && !tfEmail.getText().isBlank()) {
                dbAddStudent(tfName.getText(), tfEmail.getText(), tfPass.getText());
                tfName.clear();
                tfEmail.clear();
                tfPass.clear();

                ObservableList<StudentRow> newData = dbStudents();
                FilteredList<StudentRow> newFilt = new FilteredList<>(newData, p -> true);
                tbl.setItems(newFilt);
            }
        });

        delBtn.setOnAction(e -> {
            StudentRow s = tbl.getSelectionModel().getSelectedItem();
            if (s != null) {
                dbDelStudent(s.getId());

                ObservableList<StudentRow> newData = dbStudents();
                FilteredList<StudentRow> newFilt = new FilteredList<>(newData, p -> true);
                tbl.setItems(newFilt);
            }
        });

        VBox formBox = card();
        FlowPane fp = flow();
        fp.getChildren().addAll(
                lbl("Ad Soyad:"), tfName,
                lbl("E-posta:"), tfEmail,
                lbl("Şifre:"), tfPass,
                addBtn, delBtn
        );

        formBox.getChildren().add(fp);

        Label hint = new Label("ℹ Satıra çift tıklayarak kontrol panelini açabilirsiniz.");
        hint.setStyle("-fx-font-size:11px; -fx-text-fill:#dfe6e9;");

        page.getChildren().addAll(filter, tbl, formBox, hint);
        contentArea.getChildren().setAll(page);
    }

    private void showStudentPanel(StudentRow student) {
        VBox page = new VBox(12);
        page.setPadding(new Insets(20, 28, 20, 28));
        page.setStyle("-fx-background-color:transparent;");

        Button back = btn("← Listeye Dön", "#636e72");
        back.setOnAction(e -> showStudentList());

        Label info = new Label("Yönetilen Öğrenci: " + student.getName() +
                "  (ID: " + student.getId() + ")");
        info.setStyle("-fx-font-size:13px; -fx-text-fill:#dfe6e9;");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        // "Haftalık Gelişim" sekmesi buradan çıkarıldı.
        tabs.getTabs().addAll(
                new Tab("📅 Program",          tabProgram(student)),
                new Tab("📊 Quiz",             tabQuiz(student)),
                new Tab("💡 Tavsiye",          tabAdvice(student))
        );

        page.getChildren().addAll(back, info, tabs);
        contentArea.getChildren().setAll(page);
    }

    // --- TAVSİYE SEKMESİNİN GÖRÜNÜMÜNÜ OLUŞTURAN METOT ---
    private VBox tabAdvice(StudentRow student) {
        VBox box = card(); // Senin projende kullandığın kart yardımcı metodu

        Label hdr = new Label("💡 " + student.getName() + "'e Tavsiye Gönder");
        hdr.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:#2d3436;");

        TextField titleFld = tf("Başlık"); // Senin TextField yardımcı metodun

        TextArea msgArea = new TextArea();
        msgArea.setPromptText("Tavsiyenizi yazın...");
        msgArea.setPrefRowCount(5);

        Button send = btn("📤 Gönder", "#0984e3"); // Senin Buton yardımcı metodun

        Label ok = new Label();
        ok.setStyle("-fx-text-fill:#00b894; -fx-font-weight:700;");

        send.setOnAction(e -> {
            if (!titleFld.getText().isBlank() && !msgArea.getText().isBlank()) {
                // Bir önceki adımda düzelttiğimiz veritabanı metodunu çağırıyoruz
                dbAdvice(student.getId(), titleFld.getText(), msgArea.getText());

                ok.setText("✓ Gönderildi.");
                titleFld.clear();
                msgArea.clear();
            }
        });

        box.getChildren().addAll(
                hdr,
                lbl("Başlık:"), titleFld,
                lbl("Mesaj:"), msgArea,
                new HBox(10, send, ok)
        );

        return box;
    }

    private VBox tabProgram(StudentRow student) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(12));

        scheduleGrid = new GridPane();
        scheduleGrid.setHgap(5);
        scheduleGrid.setVgap(5);
        scheduleGrid.setStyle(
                "-fx-background-color:#fff; -fx-background-radius:14; " +
                        "-fx-padding:12; -fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.05),12,0,0,4);"
        );
        VBox.setVgrow(scheduleGrid, Priority.ALWAYS);

        // Hafta referans tarihi (bugünün haftası)
        final LocalDate[] weekRef = { LocalDate.now() };

        Label weekLabel = new Label();
        weekLabel.setStyle("-fx-font-weight:700; -fx-text-fill:#2d3436; -fx-font-size:13px;");

        Runnable refreshWeek = () -> {
            LocalDate monday = weekRef[0].with(java.time.DayOfWeek.MONDAY);
            LocalDate sunday = monday.plusDays(6);
            weekLabel.setText("📅 " + monday + "  —  " + sunday);
            drawGrid(student.getId(), weekRef[0]);
        };

        Button prevWeek = btn("← Önceki Hafta", "#636e72");
        Button nextWeek = btn("Sonraki Hafta →", "#636e72");
        prevWeek.setOnAction(e -> { weekRef[0] = weekRef[0].minusWeeks(1); refreshWeek.run(); });
        nextWeek.setOnAction(e -> { weekRef[0] = weekRef[0].plusWeeks(1); refreshWeek.run(); });

        HBox weekNav = new HBox(10, prevWeek, weekLabel, nextWeek);
        weekNav.setAlignment(Pos.CENTER);

        refreshWeek.run();

        DatePicker dp = new DatePicker(LocalDate.now());
        styleCtrl(dp);
        dp.setOnAction(e -> {
            if (dp.getValue() != null) { weekRef[0] = dp.getValue(); refreshWeek.run(); }
        });

        ComboBox<String> timeSel = new ComboBox<>(FXCollections.observableArrayList(
                "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00"
        ));
        timeSel.setPromptText("Saat Seç");
        styleCtrl(timeSel);

        ComboBox<String> subSel = new ComboBox<>();
        subSel.setPromptText("Ders Seçimi");
        styleCtrl(subSel);
        fillSubCombo(subSel);

        Button addBtn = btn("+ Ekle",  "#00b894");
        Button delBtn = btn("Temizle", "#ff7675");

        addBtn.setOnAction(e -> {
            if (dp.getValue() != null && subSel.getValue() != null && timeSel.getValue() != null) {
                dbAddSession(student.getId(), dp.getValue(), timeSel.getValue(), subSel.getValue());
                weekRef[0] = dp.getValue();
                refreshWeek.run();
            }
        });

        delBtn.setOnAction(e -> {
            if (selectedDate != null && selectedTime != null) {
                dbDelSession(student.getId(), selectedDate, selectedTime);
                selectedDate = null;
                selectedTime = null;
                selectedCell = null;
                refreshWeek.run();
            } else if (dp.getValue() != null && timeSel.getValue() != null) {
                dbDelSession(student.getId(), dp.getValue(), timeSel.getValue());
                refreshWeek.run();
            }
        });

        Button repeatBtn = btn("🔁 Tüm Haftalara Uygula", "#6c5ce7");
        repeatBtn.setOnAction(e -> {
            int copied = dbRepeatWeek(student.getId(), weekRef[0], 8);
            if (copied > 0) {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Başarılı");
                info.setHeaderText(null);
                info.setContentText("Bu haftanın programı gelecek 8 haftaya kopyalandı! (" + copied + " ders eklendi)");
                info.showAndWait();
            }
            refreshWeek.run();
        });

        VBox actionBox = card();
        FlowPane fp = flow();
        fp.getChildren().addAll(
                lbl("Tarih:"), dp,
                lbl("Saat:"), timeSel,
                subSel,
                addBtn,
                delBtn,
                repeatBtn
        );

        actionBox.getChildren().add(fp);
        content.getChildren().addAll(weekNav, scheduleGrid, actionBox);
        return content;
    }

    private void drawGrid(int stId, LocalDate refDate) {
        scheduleGrid.getChildren().clear();
        scheduleGrid.getColumnConstraints().clear();
        scheduleGrid.getRowConstraints().clear();

        // Haftanın Pazartesi ve Pazar tarihlerini hesapla
        LocalDate monday = refDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

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

        String[] days = {
                "Saat", "Pazartesi", "Salı", "Çarşamba",
                "Perşembe", "Cuma", "Cumartesi", "Pazar"
        };

        for (int i = 0; i < days.length; i++) {
            Label lbl = new Label(days[i]);
            lbl.setStyle("-fx-font-weight:800; -fx-text-fill:#2d3436; -fx-font-size:12px;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            scheduleGrid.add(lbl, i, 0);
        }

        for (int h = 8; h <= 15; h++) {
            Label tl = new Label(String.format("%02d:00", h));
            tl.setStyle("-fx-font-weight:700; -fx-text-fill:#636e72;");
            tl.setMaxWidth(Double.MAX_VALUE);
            tl.setAlignment(Pos.CENTER);
            scheduleGrid.add(tl, 0, h - 7);

            for (int d = 1; d <= 7; d++) {
                VBox cell = new VBox();
                cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                cell.setStyle("-fx-background-color:#fff; -fx-background-radius:10; " +
                        "-fx-border-color:#dfe6e9; -fx-border-radius:10; -fx-border-width:1.5;");
                scheduleGrid.add(cell, d, h - 7);
            }
        }

        String sql = "SELECT ss.SessionDate, ss.StartTime, s.SubjectName, s.Category " +
                "FROM STUDY_SESSIONS ss JOIN SUBJECT s ON ss.SubjectID=s.SubjectID " +
                "WHERE ss.StudentID=? AND ss.SessionDate BETWEEN ? AND ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, stId);
            ps.setDate(2, java.sql.Date.valueOf(monday));
            ps.setDate(3, java.sql.Date.valueOf(sunday));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LocalDate date = rs.getDate("SessionDate").toLocalDate();
                String trDay = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("tr"));
                int col = dayCol(trDay);

                String startStr = rs.getString("StartTime");
                if (startStr == null || !startStr.contains(":")) continue;

                int hour = Integer.parseInt(startStr.split(":")[0]);
                int row = hour - 7;

                if (col > 0 && row > 0 && row <= 8) {
                    VBox cell = new VBox();
                    cell.setAlignment(Pos.CENTER);
                    cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                    String cat = rs.getString("Category");
                    if (cat == null) cat = "Sayısal";

                    String bg = cat.equalsIgnoreCase("Sayısal") ? "#D6EAF8" : "#F2D7D5";
                    String bdr = cat.equalsIgnoreCase("Sayısal") ? "#3498DB" : "#E74C3C";

                    String defaultStyle = "-fx-background-color:" + bg + "; -fx-background-radius:10; " +
                            "-fx-border-color:" + bdr + "; -fx-border-radius:10; -fx-border-width:1; -fx-cursor:hand;";
                    cell.setStyle(defaultStyle);

                    Label l = new Label(rs.getString("SubjectName"));
                    l.setStyle("-fx-text-fill:#2d3436; -fx-font-weight:800; -fx-font-size:10px;");
                    l.setWrapText(true);
                    l.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

                    String timeForDel = String.format("%02d:00", hour);
                    cell.setOnMouseClicked(e -> {
                        if (selectedCell != null) {
                            selectedCell.setOpacity(1.0);
                            selectedCell.setBorder(null);
                        }
                        selectedCell = cell;
                        selectedDate = date;
                        selectedTime = timeForDel;
                        cell.setStyle(defaultStyle + "-fx-border-width:3; -fx-border-color:#2d3436;");
                    });

                    cell.getChildren().add(l);
                    scheduleGrid.add(cell, col, row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox tabQuiz(StudentRow student) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));

        TableView<QuizRow> tbl = new TableView<>();
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        styleTable(tbl);
        VBox.setVgrow(tbl, Priority.ALWAYS);

        tbl.getColumns().addAll(
                col("Ders",  "subjectName"),
                col("Net",   "scoreStr"),
                col("Tarih", "date")
        );

        tbl.setItems(dbQuiz(student.getId()));
        box.getChildren().add(tbl);

        return box;
    }


    private void showSubjects() {
        VBox page = page();
        page.getChildren().add(pageTitle("Ders Tanımları"));

        TableView<SubjectRow> tbl = new TableView<>();
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        styleTable(tbl);
        VBox.setVgrow(tbl, Priority.ALWAYS);

        tbl.getColumns().addAll(
                col("ID",       "id"),
                col("Ders Adı", "name"),
                col("Kategori", "category")
        );

        tbl.setItems(dbSubjects());

        page.getChildren().add(tbl);
        contentArea.getChildren().setAll(page);
    }

    private ObservableList<StudentRow> dbStudents() {
        ObservableList<StudentRow> list = FXCollections.observableArrayList();
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement("SELECT StudentID, ST_FullName, ST_Email FROM STUDENT WHERE AdvisorID=?")) {
            ps.setInt(1, advisorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new StudentRow(rs.getInt("StudentID"), rs.getString("ST_FullName"), rs.getString("ST_Email")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private void dbAddStudent(String name, String email, String pass) {
        String sql = "INSERT INTO STUDENT(StudentID, ST_FullName, ST_Email, ST_Password, AdvisorID, RegistrationDate) " +
                "VALUES((SELECT COALESCE(MAX(StudentID),0)+1 FROM STUDENT), ?, ?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, pass);
            ps.setInt(4, advisorId);
            ps.setDate(5, java.sql.Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void dbDelStudent(int id) {
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement("DELETE FROM STUDENT WHERE StudentID=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private ObservableList<SubjectRow> dbSubjects() {
        ObservableList<SubjectRow> list = FXCollections.observableArrayList();
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement("SELECT SubjectID, SubjectName, Category FROM SUBJECT WHERE AdvisorID=?")) {
            ps.setInt(1, advisorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new SubjectRow(rs.getInt("SubjectID"), rs.getString("SubjectName"), rs.getString("Category") != null ? rs.getString("Category") : ""));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private void fillSubCombo(ComboBox<String> cb) {
        cb.getItems().clear();
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement("SELECT SubjectName FROM SUBJECT WHERE AdvisorID=? ORDER BY SubjectName")) {
            ps.setInt(1, advisorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cb.getItems().add(rs.getString("SubjectName"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void dbAddSession(int stId, LocalDate date, String time, String subName) {
        String sql = "INSERT INTO STUDY_SESSIONS (StudentID, SubjectID, SessionDate, StartTime, EndTime) " +
                "VALUES (?, (SELECT SubjectID FROM SUBJECT WHERE SubjectName=? AND AdvisorID=? LIMIT 1), ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, stId); ps.setString(2, subName); ps.setInt(3, advisorId);
            ps.setDate(4, java.sql.Date.valueOf(date)); ps.setString(5, time + ":00"); ps.setString(6, time + ":40");
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void dbDelSession(int stId, LocalDate date, String time) {
        if (date == null || time == null) return;
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement("DELETE FROM STUDY_SESSIONS WHERE StudentID=? AND SessionDate=? AND StartTime LIKE ?")) {
            ps.setInt(1, stId); ps.setDate(2, java.sql.Date.valueOf(date)); ps.setString(3, time + "%");
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private int dbRepeatWeek(int stId, LocalDate refDate, int weekCount) {
        LocalDate monday = refDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        int totalCopied = 0;

        String fetchSql = "SELECT ss.SessionDate, ss.StartTime, ss.SubjectID " +
                "FROM STUDY_SESSIONS ss WHERE ss.StudentID=? AND ss.SessionDate BETWEEN ? AND ? " +
                "AND HOUR(ss.StartTime) BETWEEN 8 AND 15";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement fetchStmt = conn.prepareStatement(fetchSql)) {
            fetchStmt.setInt(1, stId);
            fetchStmt.setDate(2, java.sql.Date.valueOf(monday));
            fetchStmt.setDate(3, java.sql.Date.valueOf(sunday));
            ResultSet rs = fetchStmt.executeQuery();

            java.util.List<Object[]> entries = new java.util.ArrayList<>();
            while (rs.next()) {
                LocalDate date = rs.getDate("SessionDate").toLocalDate();
                int dayOffset = (int) java.time.temporal.ChronoUnit.DAYS.between(monday, date);
                entries.add(new Object[]{ dayOffset, rs.getString("StartTime"), rs.getInt("SubjectID") });
            }

            if (entries.isEmpty()) return 0;

            String insertSql = "INSERT INTO STUDY_SESSIONS (StudentID, SubjectID, SessionDate, StartTime, EndTime) VALUES (?, ?, ?, ?, ?)";
            String checkSql  = "SELECT COUNT(*) FROM STUDY_SESSIONS WHERE StudentID=? AND SessionDate=? AND StartTime LIKE ?";

            for (int w = 1; w <= weekCount; w++) {
                LocalDate targetMonday = monday.plusWeeks(w);
                for (Object[] entry : entries) {
                    int dayOff = (int) entry[0];
                    String startTime = (String) entry[1];
                    int subId = (int) entry[2];
                    LocalDate targetDate = targetMonday.plusDays(dayOff);
                    String hourPrefix = startTime.split(":")[0] + ":" + startTime.split(":")[1];

                    try (PreparedStatement chk = conn.prepareStatement(checkSql)) {
                        chk.setInt(1, stId);
                        chk.setString(2, targetDate.toString());
                        chk.setString(3, hourPrefix + "%");
                        ResultSet cr = chk.executeQuery();
                        if (cr.next() && cr.getInt(1) > 0) continue;
                    }

                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        ins.setInt(1, stId);
                        ins.setInt(2, subId);
                        ins.setString(3, targetDate.toString());
                        ins.setString(4, startTime);
                        ins.setString(5, startTime);
                        ins.executeUpdate();
                        totalCopied++;
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return totalCopied;
    }

    private ObservableList<QuizRow> dbQuiz(int stId) {
        ObservableList<QuizRow> list = FXCollections.observableArrayList();
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement("SELECT s.SubjectName, q.Score, q.QuizDate FROM QUIZ_RECORDS q JOIN SUBJECT s ON q.SubjectID=s.SubjectID WHERE q.StudentID=? ORDER BY q.QuizDate DESC")) {
            ps.setInt(1, stId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new QuizRow(rs.getString("SubjectName"), rs.getDouble("Score"), rs.getString("QuizDate")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private void dbAdvice(int stId, String title, String msg) {
        String sql = "INSERT INTO INSIGHT_RECORDS (StudentID, RecommendationType, RecommendationText, PatternDetected, GeneratedDate) " +
                "VALUES (?, ?, ?, 'Danışman Notu', CURDATE())";

        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, stId);
            ps.setString(2, title);
            ps.setString(3, msg);

            ps.executeUpdate();
            System.out.println("✅ Tavsiye başarıyla kaydedildi!");

        } catch (SQLException e) {
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Hata");
                alert.setHeaderText("Kayıt Başarısız");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private int dayCol(String day) {
        day = day.toLowerCase();
        if (day.contains("pazartesi")) return 1;
        if (day.contains("salı")) return 2;
        if (day.contains("çarşamba")) return 3;
        if (day.contains("perşembe")) return 4;
        if (day.contains("cuma")) return 5;
        if (day.contains("cumartesi")) return 6;
        if (day.contains("pazar")) return 7;
        return 0;
    }

    private void styleCtrl(Control c) {
        c.setStyle("-fx-background-color:#f5f6fa; -fx-border-color:#dcdde1; -fx-border-radius:7; -fx-background-radius:7; -fx-pref-height:34px; -fx-padding:0 9 0 9; -fx-font-size:12.5px; -fx-text-fill:#2d3436;");
    }

    private TextField tf(String prompt) {
        TextField t = new TextField(); t.setPromptText(prompt); styleCtrl(t); return t;
    }

    private Label lbl(String text) {
        Label l = new Label(text); l.setStyle("-fx-font-weight:600; -fx-text-fill:#636e72; -fx-font-size:12px;"); return l;
    }

    private Button btn(String text, String bg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:#fff; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:8 14 8 14; -fx-cursor:hand; -fx-font-size:12px;");
        b.setOnMouseEntered(e -> b.setOpacity(.85)); b.setOnMouseExited(e -> b.setOpacity(1.0));
        return b;
    }

    private Button navBtn(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE); b.setAlignment(Pos.CENTER_LEFT); b.setPadding(new Insets(12, 16, 12, 16));
        b.setStyle("-fx-background-color:transparent; -fx-text-fill:#a4b0be; -fx-font-weight:600; -fx-font-size:13px; -fx-cursor:hand; -fx-background-radius:9;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:#2f3640; -fx-text-fill:#fff; -fx-font-weight:600; -fx-font-size:13px; -fx-cursor:hand; -fx-background-radius:9;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color:transparent; -fx-text-fill:#a4b0be; -fx-font-weight:600; -fx-font-size:13px; -fx-background-radius:9;"));
        return b;
    }

    private VBox page() {
        VBox v = new VBox(14); v.setPadding(new Insets(26, 30, 26, 30)); v.setStyle("-fx-background-color:transparent;"); return v;
    }

    private VBox card() {
        VBox v = new VBox(10); v.setPadding(new Insets(16, 18, 16, 18));
        v.setStyle("-fx-background-color:#fff; -fx-background-radius:14; -fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.05),14,0,0,4);");
        return v;
    }

    private FlowPane flow() { FlowPane fp = new FlowPane(10, 10); fp.setAlignment(Pos.CENTER_LEFT); return fp; }

    private Label pageTitle(String t) {
        Label l = new Label(t); l.setStyle("-fx-font-size:26px; -fx-font-weight:800; -fx-text-fill:#fff;"); return l;
    }

    private void wrap(VBox page) {
        ScrollPane sp = new ScrollPane(page); sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-color:transparent;");
        contentArea.getChildren().setAll(sp);
    }

    private void styleTable(TableView<?> t) {
        t.setStyle("-fx-background-color:#fff; -fx-background-radius:14; -fx-border-radius:14; -fx-border-color:#dfe6e9; -fx-border-width:1.5;");
    }

    @SuppressWarnings("unchecked")
    private <T, S> TableColumn<T, S> col(String header, String field) {
        TableColumn<T, S> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(field));
        return c;
    }

    private VBox statCard(String label, String value, String color) {
        VBox card = new VBox(8); card.setPadding(new Insets(16, 14, 16, 14));
        card.setStyle("-fx-background-color:#fff; -fx-background-radius:14; -fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.06),18,0,0,5);");
        card.setPrefWidth(190);
        Label l = new Label(label); l.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#636e72;");
        Label v = new Label(value); v.setStyle("-fx-font-size:24px; -fx-font-weight:900; -fx-text-fill:" + color + ";");
        card.getChildren().addAll(l, v);
        return card;
    }

    public static class StudentRow {
        private final int id; private final String name; private final String email;
        public StudentRow(int id, String n, String e) { this.id = id; this.name = n; this.email = e; }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
    }

    public static class SubjectRow {
        private final int id; private final String name; private final String category;
        public SubjectRow(int id, String n, String c) { this.id = id; this.name = n; this.category = c; }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
    }

    public static class QuizRow {
        private final String subjectName; private final String scoreStr; private final String date;
        public QuizRow(String s, double sc, String d) { subjectName = s; scoreStr = String.format("%.2f", sc); date = d; }
        public String getSubjectName() { return subjectName; }
        public String getScoreStr() { return scoreStr; }
        public String getDate() { return date; }
    }
}