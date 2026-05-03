import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import java.util.List;
import java.util.Locale;

import java.util.HashMap;
import java.util.Map;

public class StudentDashboard {

    private Stage stage;
    private String studentName;
    private int studentId;

    private String selectedDate = null;
    private String selectedTime = null;

    private javafx.scene.chart.LineChart<String, Number> progressChart;

    private BorderPane mainLayout;
    private BorderPane centerLayout;

    private TableView<QuizRecord> table;
    private TableView<StudySessionRecord> sessionTable;
    private GridPane scheduleGrid;

    // HEDEFLER İÇİN SEÇİM TAKİBİ
    private int selectedGoalId = -1;

    // VERİTABANI BAĞLANTISI
    private final String URL = "jdbc:mysql://localhost:3306/BIM216_Project?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci";
    private final String USER = "root";
    private final String PASS = "";

    private Timeline timeline;
    private int secondsPassed = 0;
    private Label timerLabel = new Label("00:00:00");
    private boolean isRunning = false;

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

        Button btnTimer = createMenuBtn("KRONOMETRE");
        btnTimer.setOnAction(e -> showStopwatch());

        Button btnHome = createMenuBtn("ANASAYFA");
        btnHome.setOnAction(e -> showHomeContent());

        Button btnQuiz = createMenuBtn("QUİZ SONUÇLARIM");
        btnQuiz.setOnAction(e -> showQuizResults());

        Button btnGoals = createMenuBtn("HEDEFLERİM");
        btnGoals.setOnAction(e -> showGoals());

        Button btnSchedule = createMenuBtn("HAFTALIK PROGRAM");
        btnSchedule.setOnAction(e -> showWeeklySchedule());

        Button btnRecommendations = createMenuBtn("TAVSİYELERİM");
        btnRecommendations.setOnAction(e -> showRecommendations());

        sideBar.getChildren().addAll(logo, btnHome, btnQuiz,
                btnTimer,
                btnGoals,
                btnSchedule,
                btnRecommendations);

        mainLayout.setLeft(sideBar);
    }

    // --- KRONOMETRE VE TABLO EKRANI ---
    // --- KRONOMETRE VE TABLO EKRANI ---
    private void showStopwatch() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30, 50, 30, 50));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: transparent;");

        Label title = new Label("Odaklanma Zamanı 🚀");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        timerLabel.setStyle("-fx-font-size: 70px; -fx-font-weight: 900; -fx-text-fill: #00cec9; -fx-font-family: 'Courier New';");

        ComboBox<String> subjectSelect = new ComboBox<>();
        subjectSelect.setPromptText("Çalıştığın Dersi Seç");
        styleInput(subjectSelect);
        subjectSelect.setPrefWidth(250);
        populateSubjectComboBox(subjectSelect);

        Button startPauseBtn = createModernBtn("▶ Başlat", "#00b894");
        Button stopSaveBtn = createModernBtn("⏹ Bitir ve Kaydet", "#d63031");
        stopSaveBtn.setDisable(true);

        if (timeline != null) timeline.stop(); // Önceki timeline varsa durdur
        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            secondsPassed++;
            int h = secondsPassed / 3600;
            int m = (secondsPassed % 3600) / 60;
            int s = secondsPassed % 60;
            timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        }));
        timeline.setCycleCount(Animation.INDEFINITE);

        startPauseBtn.setOnAction(e -> {
            if (!isRunning) {
                timeline.play(); isRunning = true;
                startPauseBtn.setText("⏸ Duraklat");
                startPauseBtn.setStyle("-fx-background-color: #fdcb6e; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8;");
                stopSaveBtn.setDisable(false);
            } else {
                timeline.pause(); isRunning = false;
                startPauseBtn.setText("▶ Devam Et");
                startPauseBtn.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8;");
            }
        });

        stopSaveBtn.setOnAction(e -> {
            if (subjectSelect.getValue() == null) {
                showAlert("Ders Seçimi", "Lütfen bu çalışmayı hangi ders için yaptığını seç!");
                return;
            }
            timeline.stop();
            saveStudySessionToDB(subjectSelect.getValue(), secondsPassed);

            secondsPassed = 0; timerLabel.setText("00:00:00"); isRunning = false;
            startPauseBtn.setText("▶ Başlat"); stopSaveBtn.setDisable(true);
            startPauseBtn.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8;");

            refreshSessionTable(null); // Tabloyu GÜNCELLE
            showAlert("Başarılı", "Çalışma seansın başarıyla kaydedildi!");
        });

        HBox controls = new HBox(20, startPauseBtn, stopSaveBtn);
        controls.setAlignment(Pos.CENTER);
        VBox topControls = new VBox(20, title, subjectSelect, timerLabel, controls);
        topControls.setAlignment(Pos.CENTER);

        // --- TABLO VE FİLTRELEME BÖLÜMÜ ---
        VBox tableSection = new VBox(15);
        tableSection.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-padding: 20;");

        // Filtre Araçları
        FlowPane filterBox = new FlowPane(15, 15);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> filterSubInp = new ComboBox<>();
        filterSubInp.setPromptText("Filtrelemek için ders seç...");
        styleInput(filterSubInp);
        populateSubjectComboBox(filterSubInp);

        Button btnFilter = createModernBtn("🔍 Filtrele", "#0984e3");
        Button btnClearFilter = createModernBtn("🔄 Temizle", "#b2bec3");

        btnFilter.setOnAction(e -> {
            if(filterSubInp.getValue() != null) refreshSessionTable(filterSubInp.getValue());
        });
        btnClearFilter.setOnAction(e -> {
            filterSubInp.setValue(null); refreshSessionTable(null);
        });
        filterBox.getChildren().addAll(createFormLabel("Ders Filtresi:"), filterSubInp, btnFilter, btnClearFilter);

        // Tablo Kurulumu
        sessionTable = new TableView<>();
        sessionTable.setPrefHeight(200);
        sessionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<StudySessionRecord, String> colSub = new TableColumn<>("Ders"); colSub.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        TableColumn<StudySessionRecord, String> colDate = new TableColumn<>("Tarih"); colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<StudySessionRecord, String> colStart = new TableColumn<>("Başlangıç"); colStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        TableColumn<StudySessionRecord, String> colEnd = new TableColumn<>("Bitiş"); colEnd.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        TableColumn<StudySessionRecord, String> colDur = new TableColumn<>("Toplam Süre"); colDur.setCellValueFactory(new PropertyValueFactory<>("duration"));
        TableColumn<StudySessionRecord, String> colFocus = new TableColumn<>("Odak"); colFocus.setCellValueFactory(new PropertyValueFactory<>("focusLevelStr"));
        TableColumn<StudySessionRecord, Void> colNote = new TableColumn<>("Not");
        colNote.setCellFactory(col -> new javafx.scene.control.TableCell<StudySessionRecord, Void>() {
            private final Button btn = new Button("📝 Görüntüle");
            {
                btn.setStyle("-fx-background-color: #0984e3; -fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-size: 11px; -fx-cursor: hand;");
                btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
                btn.setOnMouseExited(e -> btn.setOpacity(1.0));
                btn.setOnAction(e -> {
                    StudySessionRecord item = getTableView().getItems().get(getIndex());
                    showNotePopup(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                StudySessionRecord rec = getTableView().getItems().get(getIndex());
                String note = rec.getSessionNote();
                setGraphic((note != null && !note.trim().isEmpty()) ? btn : null);
            }
        });

        sessionTable.getColumns().addAll(colSub, colDate, colStart, colEnd, colDur, colFocus, colNote);

        // Satıra çift tıklanınca da notu popup olarak göster
        sessionTable.setRowFactory(tv -> {
            TableRow<StudySessionRecord> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    showNotePopup(row.getItem());
                }
            });
            return row;
        });

        refreshSessionTable(null);

        // Silme Butonu
        // Silme Butonu Aksiyonu
        Button btnDeleteSession = createModernBtn("🗑 Seçili Seansı Sil", "#d63031");
        btnDeleteSession.setOnAction(e -> {
            StudySessionRecord selected = sessionTable.getSelectionModel().getSelectedItem();
            if(selected != null) {
                // Artık isim, tarih veya saat göndermiyoruz. Doğrudan benzersiz ID'yi gönderiyoruz!
                deleteStudySessionFromDB(selected.getSessionId());
                refreshSessionTable(filterSubInp.getValue());
            } else {
                showAlert("Uyarı", "Lütfen silmek için tablodan bir kayıt seçin!");
            }
        });
        HBox bottomBox = new HBox(btnDeleteSession);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        Label tableTitle = new Label("Geçmiş Odaklanma Seanslarım");
        tableTitle.setStyle("-fx-font-weight: 800; -fx-text-fill: #2d3436; -fx-font-size: 16px;");

        tableSection.getChildren().addAll(tableTitle, filterBox, sessionTable, bottomBox);

        content.getChildren().addAll(topControls, tableSection);
        centerLayout.setCenter(content);
    }

    // --- KRONOMETREYİ VERİTABANINA KAYDETME YARDIMCI METODU (GÜNCELLENDİ) ---
    private void saveStudySessionToDB(String subjectName, int totalSeconds) {
        // 1. ADIM: Ekrana Şık Bir Değerlendirme Penceresi (Dialog) Çıkar
        javafx.scene.control.Dialog<javafx.util.Pair<Integer, String>> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Tebrikler! Seans Bitti 🚀");
        dialog.setHeaderText(subjectName + " çalışmasını tamamladın. Bu seans nasıldı?");

        // Butonları Ayarla (Kaydet ve Geç)
        javafx.scene.control.ButtonType saveBtnType = new javafx.scene.control.ButtonType("Kaydet", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType skipBtnType = new javafx.scene.control.ButtonType("Değerlendirmeden Geç", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, skipBtnType);

        // İçerik Tasarımı (GridPane)
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20, 50, 10, 10));

        // Odaklanma Seviyesi için Spinner (1-10 arası, varsayılan 7)
        javafx.scene.control.Label focusLabel = new javafx.scene.control.Label("Odaklanma Seviyen (1-10):");
        focusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436;");
        javafx.scene.control.Spinner<Integer> focusSpinner = new javafx.scene.control.Spinner<>(1, 10, 7);

        // Çalışma Notları için Metin Alanı
        javafx.scene.control.Label noteLabel = new javafx.scene.control.Label("Çalışma Notların:");
        noteLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436;");
        javafx.scene.control.TextArea noteArea = new javafx.scene.control.TextArea();
        noteArea.setPromptText("Örn: Hız problemlerinde zorlandım, formülleri tekrar etmem lazım...");
        noteArea.setPrefRowCount(3);
        noteArea.setWrapText(true);

        grid.add(focusLabel, 0, 0);
        grid.add(focusSpinner, 1, 0);
        grid.add(noteLabel, 0, 1);
        grid.add(noteArea, 1, 1);
        dialog.getDialogPane().setContent(grid);

        // Butona basıldığında verileri al
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                return new javafx.util.Pair<>(focusSpinner.getValue(), noteArea.getText());
            }
            return null; // "Geç" denirse null döner
        });

        // Pencereyi Göster ve Kullanıcının Seçimini Bekle
        java.util.Optional<javafx.util.Pair<Integer, String>> result = dialog.showAndWait();

        Integer finalFocusLevel = null;
        String finalSessionNote = null;

        if (result.isPresent()) {
            finalFocusLevel = result.get().getKey();
            String note = result.get().getValue();
            finalSessionNote = (note != null && !note.trim().isEmpty()) ? note : null;
        }

        // 2. ADIM: Verileri Veritabanına Yaz (SQL Sorgusu Güncellendi)
        String sql = "INSERT INTO STUDY_SESSIONS (StudentID, SubjectID, SessionDate, StartTime, EndTime, FocusLevel, SessionNote) " +
                "VALUES (?, (SELECT SubjectID FROM SUBJECT WHERE SubjectName = ? LIMIT 1), CURDATE(), CURTIME(), ADDTIME(CURTIME(), SEC_TO_TIME(?)), ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            stmt.setString(2, subjectName);
            stmt.setInt(3, totalSeconds);

            // Kullanıcı odaklanma seviyesi girdiyse kaydet, girmediyse (Geç dediyse) veritabanına NULL yaz
            if (finalFocusLevel != null) {
                stmt.setInt(4, finalFocusLevel);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }

            // Kullanıcı not girdiyse kaydet, girmediyse veritabanına NULL yaz
            if (finalSessionNote != null) {
                stmt.setString(5, finalSessionNote);
            } else {
                stmt.setNull(5, java.sql.Types.VARCHAR);
            }

            stmt.executeUpdate();
            System.out.println("Seans başarıyla kaydedildi!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- VERİTABANINDAN ÇALIŞMA SEANSI SİLME METODU (PROFESYONEL VE KESİN ÇÖZÜM) ---
    private void deleteStudySessionFromDB(int sessionId) {
        // Sadece eşsiz kimlik (SessionID) aranır. Başka hiçbir şey silinemez!
        String sql = "DELETE FROM STUDY_SESSIONS WHERE SessionID = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- TABLOYU VERİTABANINDAN DOLDURAN VE FİLTRELEYEN METOT ---
    private void refreshSessionTable(String subjectFilter) {
        if (sessionTable == null) return;
        javafx.collections.ObservableList<StudySessionRecord> list = javafx.collections.FXCollections.observableArrayList();

        // SQL sorgusunun en başına "ss.SessionID" eklendi. Artık veritabanından kimlikleri de çekiyoruz.
        String sql = "SELECT ss.SessionID, s.SubjectName, ss.SessionDate, ss.StartTime, ss.EndTime, TIMEDIFF(ss.EndTime, ss.StartTime) as Duration, ss.FocusLevel, ss.SessionNote " +
                "FROM STUDY_SESSIONS ss JOIN SUBJECT s ON ss.SubjectID = s.SubjectID " + "WHERE ss.StudentID = ? AND ss.StartTime != ss.EndTime ";

        if (subjectFilter != null && !subjectFilter.isEmpty()) {
            sql += " AND s.SubjectName = ? ";
        }
        sql += " ORDER BY ss.SessionDate DESC, ss.StartTime DESC";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);

            if (subjectFilter != null && !subjectFilter.isEmpty()) {
                stmt.setString(2, subjectFilter);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int focusLevel = rs.getInt("FocusLevel");
                String sessionNote = rs.getString("SessionNote");
                list.add(new StudySessionRecord(
                        rs.getInt("SessionID"),
                        rs.getString("SubjectName"),
                        rs.getString("SessionDate"),
                        rs.getString("StartTime"),
                        rs.getString("EndTime"),
                        rs.getString("Duration"),
                        focusLevel,
                        sessionNote
                ));
            }
            sessionTable.setItems(list);
            if(list.isEmpty()) {
                sessionTable.setPlaceholder(new Label("Görüntülenecek odaklanma seansı bulunamadı."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // --- ANA SAYFA METODU (DÜZENLİ VE SIRALI HALİ) ---
    private void showHomeContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 40, 40, 40));
        content.setStyle("-fx-background-color: transparent;");

        // 1. HOŞ GELDİN YAZISI
        Label welcome = new Label("Hoş geldin, " + studentName + "! 👋");
        welcome.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        // 2. SİSTEM ANALİZİ (Ders Bazlı Profesyonel Rapor)
        VBox efficiencyCard = new VBox(8);
        efficiencyCard.setPadding(new Insets(15, 20, 15, 20));
        // Sol menüye çok yakışacak koyu gri/antrasit şık bir kutu tasarımı
        efficiencyCard.setStyle("-fx-background-color: #2d3436; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        Label titleLabel = new Label("📊 Ders Bazlı Verimlilik Raporu");
        titleLabel.setStyle("-fx-text-fill: #00cec9; -fx-font-weight: 800; -fx-font-size: 16px; -fx-padding: 0 0 5 0;");
        efficiencyCard.getChildren().add(titleLabel);

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            // Her dersin quiz puanlarını saatlere göre gruplar ve puanı en yüksekten düşüğe sıralar
            String sql = "SELECT s.SubjectName, HOUR(ss.StartTime) as StudyHour, AVG(qr.Score) as AvgScore " +
                    "FROM STUDY_SESSIONS ss " +
                    "JOIN QUIZ_RECORDS qr ON ss.SubjectID = qr.SubjectID AND ss.StudentID = qr.StudentID " +
                    "JOIN SUBJECT s ON ss.SubjectID = s.SubjectID " +
                    "WHERE ss.StudentID = ? AND ss.StartTime != ss.EndTime " +
                    "GROUP BY s.SubjectName, HOUR(ss.StartTime) " +
                    "ORDER BY s.SubjectName, AvgScore DESC";

            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                ResultSet rs = ps.executeQuery();

                String currentSubject = "";
                boolean hasData = false;

                while (rs.next()) {
                    String subj = rs.getString("SubjectName");
                    // ORDER BY AvgScore DESC olduğu için, bir dersi İLK defa gördüğümüz satır o dersin en iyi saatidir!
                    if (!subj.equals(currentSubject)) {
                        currentSubject = subj;
                        int hour = rs.getInt("StudyHour");
                        double score = rs.getDouble("AvgScore");

                        Label detailLabel = new Label(String.format("📌 %s: En verimli saat %02d:00 - %02d:00 arası (Ortalama Quiz: %%%.1f)", subj, hour, hour+1, score));
                        detailLabel.setStyle("-fx-text-fill: #dfe6e9; -fx-font-weight: 600; -fx-font-size: 13.5px;");
                        efficiencyCard.getChildren().add(detailLabel);
                        hasData = true;
                    }
                }

                if (!hasData) {
                    Label noData = new Label("Sistemin ders analizi yapabilmesi için kronometreyle çalışıp quiz çözmelisiniz.");
                    noData.setStyle("-fx-text-fill: #b2bec3; -fx-font-style: italic;");
                    efficiencyCard.getChildren().add(noData);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }



        // 3. TABLO VERİLERİNİ ÇEKME
        List<Object[]> goalList = new java.util.ArrayList<>();
        int completedCount = 0;
        int totalGoalsCount = 0;

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT g.SubjectID, s.SubjectName, g.TargetStudyHours, g.TargetQuizScore, g.IsCompleted " +
                    "FROM GOALS g JOIN SUBJECT s ON g.SubjectID = s.SubjectID WHERE g.StudentID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, studentId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    goalList.add(new Object[]{rs.getInt("SubjectID"), rs.getString("SubjectName"),
                            rs.getDouble("TargetStudyHours"), rs.getDouble("TargetQuizScore"),
                            rs.getDouble("IsCompleted")});
                    totalGoalsCount++;
                    if (rs.getDouble("IsCompleted") == 1.0) completedCount++;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // 4. İSTATİSTİK KARTLARI (FlowPane)
        FlowPane statsContainer = new FlowPane(15, 15);
        statsContainer.getChildren().addAll(
                createTableCard("Haftalık Odaklanma", "#0984e3", "⚡", goalList, true),
                createTableCard("Soru Hedefi", "#00b894", "🎯", goalList, false),
                createStatCard("Sistem ID", String.valueOf(studentId), "#e17055", "🆔")
        );

        // 5. İLERLEME ÇUBUĞU
        VBox progressSection = new VBox(10);
        progressSection.setPadding(new Insets(20));
        progressSection.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 10, 0, 0, 5);");

        Label prTitle = new Label("🎯 Genel Hedef Tamamlama Durumu");
        prTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2d3436;");

        double progressRatio = totalGoalsCount > 0 ? ((double) completedCount / totalGoalsCount) : 0;
        javafx.scene.control.ProgressBar pb = new javafx.scene.control.ProgressBar(progressRatio);
        pb.setMaxWidth(Double.MAX_VALUE); pb.setMinHeight(18); pb.setStyle("-fx-accent: #00cec9;");

        Label pbText = new Label(String.format(java.util.Locale.US, "%%%.1f tamamlandı", (progressRatio * 100)));
        pbText.setStyle("-fx-text-fill: #636e72; -fx-font-weight: 600;");
        progressSection.getChildren().addAll(prTitle, pb, pbText);

        // --- HER ŞEYİ KUTUYA DOLDURMA (SIRALAMA BURADA BELİRLENİR) ---
        content.getChildren().addAll(
                welcome,
                efficiencyCard, // <-- BURAYI DEĞİŞTİRDİK
                statsContainer,
                createProgressChartCard(),
                progressSection
        );

        // SAYFAYI KAYDIRABİLMEK İÇİN SCROLLPANE
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        centerLayout.setCenter(scrollPane);
    }


    // --- 📊 HEDEF VE GERÇEKLEŞEN GRAFİĞİ (SADECE GERÇEK VERİLER) ---
    private VBox createProgressChartCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 18; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 20, 0, 0, 8);");

        Label title = new Label("📊 Hedeflenen vs Gerçekleşen Çalışma (Saat)");
        title.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: #2d3436;");

        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel("Dersler");
        xAxis.setStyle("-fx-tick-label-fill: black; -fx-font-weight: bold; -fx-font-size: 12px;");

        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel("Saat");
        yAxis.setStyle("-fx-tick-label-fill: black; -fx-font-weight: bold; -fx-font-size: 12px;");

        javafx.scene.chart.BarChart<String, Number> barChart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        barChart.setPrefHeight(300);
        barChart.setAnimated(false);
        barChart.setLegendVisible(true);
        barChart.setStyle("-fx-background-color: transparent;");

        barChart.setHorizontalGridLinesVisible(true);
        barChart.setVerticalGridLinesVisible(false);
        barChart.setCategoryGap(50);
        barChart.setBarGap(5);

        javafx.scene.chart.XYChart.Series<String, Number> goalSeries = new javafx.scene.chart.XYChart.Series<>();
        goalSeries.setName("Hedeflenen (Saat)");

        javafx.scene.chart.XYChart.Series<String, Number> actualSeries = new javafx.scene.chart.XYChart.Series<>();
        actualSeries.setName("Gerçekleşen (Saat)");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {

            // HEDEFLENEN: GOALS tablosundan hedefleri alır.
            String sqlGoals = "SELECT g.SubjectID, s.SubjectName, SUM(g.TargetStudyHours) AS TotalTarget " +
                    "FROM GOALS g JOIN SUBJECT s ON g.SubjectID = s.SubjectID " +
                    "WHERE g.StudentID = ? GROUP BY g.SubjectID, s.SubjectName";

            try (PreparedStatement stmt1 = conn.prepareStatement(sqlGoals)) {
                stmt1.setInt(1, studentId);
                ResultSet rsGoals = stmt1.executeQuery();

                while (rsGoals.next()) {
                    int subId = rsGoals.getInt("SubjectID");
                    String subjectStr = subId + " - " + rsGoals.getString("SubjectName");
                    double target = rsGoals.getDouble("TotalTarget");

                    double actual = 0.0;

                    // TAMAMLANAN: STUDY_SESSIONS tablosundan StartTime ve EndTime farkına göre gerçek süreyi saniye bazında hesapla
                    String sqlActual = "SELECT SUM(TIME_TO_SEC(TIMEDIFF(EndTime, StartTime))) AS TotalSecs " +
                            "FROM STUDY_SESSIONS " +
                            "WHERE StudentID = ? AND SubjectID = ? AND EndTime > StartTime";

                    try (PreparedStatement stmt2 = conn.prepareStatement(sqlActual)) {
                        stmt2.setInt(1, studentId);
                        stmt2.setInt(2, subId);
                        ResultSet rsActual = stmt2.executeQuery();
                        if (rsActual.next()) {
                            // Saniyeyi saate çevir. Kronometre kaydı yoksa 0 kalır.
                            actual = rsActual.getDouble("TotalSecs") / 3600.0;
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }

                    // Test verileri silindi! Artık kronometrede ne varsa sadece onu grafiğe basacak.
                    goalSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(subjectStr, target));
                    actualSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(subjectStr, actual));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        barChart.getData().addAll(goalSeries, actualSeries);

        javafx.application.Platform.runLater(() -> {
            for (javafx.scene.Node n : barChart.lookupAll(".chart-legend-item")) {
                n.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
            }
        });

        card.getChildren().addAll(title, barChart);
        return card;
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
    private LocalDate[] scheduleWeekRef;

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

        // Hafta referans tarihi
        scheduleWeekRef = new LocalDate[]{ LocalDate.now() };

        Label weekLabel = new Label();
        weekLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #ffffff; -fx-font-size: 14px;");

        Runnable refreshWeek = () -> {
            LocalDate monday = scheduleWeekRef[0].with(java.time.DayOfWeek.MONDAY);
            LocalDate sunday = monday.plusDays(6);
            weekLabel.setText("📅 " + monday + "  —  " + sunday);
            updateScheduleDisplay();
        };

        Button prevWeek = createModernBtn("← Önceki Hafta", "#636e72");
        Button nextWeek = createModernBtn("Sonraki Hafta →", "#636e72");
        prevWeek.setOnAction(e -> { scheduleWeekRef[0] = scheduleWeekRef[0].minusWeeks(1); refreshWeek.run(); });
        nextWeek.setOnAction(e -> { scheduleWeekRef[0] = scheduleWeekRef[0].plusWeeks(1); refreshWeek.run(); });

        HBox weekNav = new HBox(12, prevWeek, weekLabel, nextWeek);
        weekNav.setAlignment(Pos.CENTER);

        refreshWeek.run();

        VBox actionBox = new VBox(12);
        actionBox.setPadding(new Insets(20, 25, 20, 25));
        actionBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 5);");

        DatePicker dateInp = new DatePicker(LocalDate.now());
        styleInput(dateInp);
        dateInp.setOnAction(e -> {
            if (dateInp.getValue() != null) { scheduleWeekRef[0] = dateInp.getValue(); refreshWeek.run(); }
        });

        ComboBox<String> timeInp = new ComboBox<>(FXCollections.observableArrayList("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00"));
        timeInp.setPromptText("Saat Seç");
        styleInput(timeInp);

        ComboBox<String> subInp = new ComboBox<>();
        subInp.setPromptText("Ders Seçimi");
        styleInput(subInp);
        populateSubjectComboBox(subInp);

        Button addBtn = createModernBtn("➕ Ekle", "#00b894");
        addBtn.setOnAction(e -> {
            if(subInp.getValue() != null && timeInp.getValue() != null && dateInp.getValue() != null) {
                String selDate = dateInp.getValue().toString();
                String selTime = timeInp.getValue();

                if (isSlotFull(selDate, selTime)) {
                    showAlert("Çakışma Uyarı", "Bu saatte zaten bir dersiniz var! Lütfen üst üste eklemek yerine önce mevcut dersi temizleyin.");
                } else {
                    addScheduleToDB(selDate, selTime, subInp.getValue());
                    scheduleWeekRef[0] = dateInp.getValue();
                    refreshWeek.run();
                }
            } else {
                showAlert("Uyarı", "Lütfen tarih, saat ve ders seçimini eksiksiz yapın.");
            }
        });

        Button delBtn = createModernBtn("🗑 Temizle", "#ff7675");
        delBtn.setOnAction(e -> {
            if (selectedDate != null && selectedTime != null) {
                deleteScheduleFromDB(selectedDate, selectedTime);
                selectedDate = null;
                selectedTime = null;
                refreshWeek.run();
                System.out.println("Seçili hücre silindi.");
            } else if (timeInp.getValue() != null) {
                deleteScheduleFromDB(dateInp.getValue().toString(), timeInp.getValue());
                refreshWeek.run();
            } else {
                showAlert("Uyarı", "Lütfen silmek istediğiniz dersin üzerine tıklayın veya formdan saat seçin!");
            }
        });

        Button repeatBtn = createModernBtn("🔁 Tüm Haftalara Uygula", "#6c5ce7");
        repeatBtn.setOnAction(e -> {
            int copied = repeatWeekSchedule(scheduleWeekRef[0], 8);
            if (copied > 0) {
                showInfo("Başarılı", "Bu haftanın programı gelecek 8 haftaya kopyalandı! (" + copied + " ders eklendi)");
            } else {
                showAlert("Uyarı", "Bu haftada kopyalanacak ders bulunamadı veya tüm haftalar zaten dolu.");
            }
            refreshWeek.run();
        });

        FlowPane formLayout = new FlowPane(15, 15);
        formLayout.setAlignment(Pos.CENTER_LEFT);
        formLayout.getChildren().addAll(
                createFormLabel("Tarih:"), dateInp,
                createFormLabel("Saat:"), timeInp,
                subInp, addBtn, delBtn, repeatBtn
        );

        actionBox.getChildren().add(formLayout);

        content.getChildren().addAll(title, weekNav, scheduleGrid, actionBox);
        centerLayout.setCenter(content);
    }

    // --- ÇAKIŞMA KONTROL METODU ---
    private boolean isSlotFull(String date, String time) {
        // Veritabanında aynı tarihte ve saatte kayıt var mı sayıyoruz
        String sql = "SELECT COUNT(*) FROM STUDY_SESSIONS WHERE StudentID = ? AND SessionDate = ? AND StartTime LIKE ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setString(2, date);
            stmt.setString(3, time + "%"); // Saat "08:00:00" formatında olsa bile eşleşmesi için % ekledik
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Eğer kayıt sayısı 0'dan büyükse TRUE (dolu) döner
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return false;
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

        // Haftanın tarih aralığını hesapla
        LocalDate monday = scheduleWeekRef[0].with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        String sql = "SELECT ss.SessionDate, ss.StartTime, s.SubjectName, s.Category " +
                "FROM STUDY_SESSIONS ss JOIN SUBJECT s ON ss.SubjectID = s.SubjectID " +
                "WHERE ss.StudentID = ? AND ss.SessionDate BETWEEN ? AND ? " +
                "AND HOUR(ss.StartTime) BETWEEN 8 AND 15";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setDate(2, java.sql.Date.valueOf(monday));
            stmt.setDate(3, java.sql.Date.valueOf(sunday));
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

                    // --- STİL KODLARIN (Dokunma) ---
                    String color = rs.getString("Category").equalsIgnoreCase("Sayısal") ? "#D6EAF8" : "#F2D7D5";
                    String borderColor = rs.getString("Category").equalsIgnoreCase("Sayısal") ? "#3498DB" : "#E74C3C";
                    cell.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12; -fx-border-color: " + borderColor + "; -fx-border-radius: 12; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4);");

                    // ********* BURADAN İTİBAREN EKLEME YAPILIYOR *********

                    // Lambda içinde kullanabilmek için o anki satırın tarih ve saatini sabitliyoruz
                    final String currentCellDate = date.toString();
                    final String currentCellTime = rs.getString("StartTime");

                    cell.setCursor(javafx.scene.Cursor.HAND); // Mouse üzerine gelince el işareti çıksın
                    cell.setOnMouseClicked(e -> {
                        // Sınıfın en üstünde tanımladığın değişkenlere atıyoruz
                        selectedDate = currentCellDate;
                        selectedTime = currentCellTime;

                        // Tabloyu temizle ve seçilen hücreye siyah çerçeve ekle
                        updateScheduleDisplay();
                        cell.setStyle(cell.getStyle() + "-fx-border-color: #2d3436; -fx-border-width: 2.5;");

                        System.out.println("Seçildi: " + selectedDate + " " + selectedTime);
                    });

                    // ********* EKLEME BİTTİ, MEVCUT KODLARINA DEVAM ET *********

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

        FlowPane filterBox = new FlowPane(15, 15);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setPadding(new Insets(15, 20, 15, 20));
        filterBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 10, 0, 0, 4);");

        TextField filterInput = new TextField();
        filterInput.setPromptText("Örn: 201");
        styleInput(filterInput);

        Button filterBtn = createModernBtn("🔍 Filtrele", "#0984e3");
        Button clearBtn = createModernBtn("🔄 Temizle", "#b2bec3");

        // --- FİLTRE VE TEMİZLE BUTONLARINA GRAFİK GÜNCELLEMESİ EKLENDİ ---
        filterBtn.setOnAction(e -> {
            if (!filterInput.getText().isEmpty()) {
                int sId = Integer.parseInt(filterInput.getText());
                refreshTable(sId);
                updateQuizChart(sId); // Filtrelendiğinde grafiği çiz
            }
        });
        clearBtn.setOnAction(e -> {
            filterInput.clear();
            refreshTable(null);
            updateQuizChart(null); // Temizlendiğinde grafiği sıfırla
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
                // Eğer o an bir ders filtrelenmişse, grafiği de tazele
                if(!filterInput.getText().isEmpty()) updateQuizChart(Integer.parseInt(filterInput.getText()));
            }
        });

        updateBtn.setOnAction(e -> {
            QuizRecord selected = table.getSelectionModel().getSelectedItem();
            if(selected != null && !scoreInput.getText().isEmpty()) {
                updateRecord(selected.getQuizId(), Double.parseDouble(scoreInput.getText()));
                scoreInput.clear();
                refreshTable(null);
                if(!filterInput.getText().isEmpty()) updateQuizChart(Integer.parseInt(filterInput.getText()));
            }
        });

        delBtn.setOnAction(e -> {
            QuizRecord selected = table.getSelectionModel().getSelectedItem();
            if(selected != null) {
                deleteRecord(selected.getQuizId());
                refreshTable(null);
                if(!filterInput.getText().isEmpty()) updateQuizChart(Integer.parseInt(filterInput.getText()));
            }
        });

        FlowPane formLayout = new FlowPane(15, 15);
        formLayout.setAlignment(Pos.CENTER_LEFT);
        formLayout.getChildren().addAll(subInput, scoreInput, dateInput, addBtn, updateBtn, delBtn);
        actionBox.getChildren().add(formLayout);

        // --- GRAFİK ARAYÜZ KURULUMU EKLENDİ ---
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel("Tarih");
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis(0, 100, 10);
        yAxis.setLabel("Skor");

        progressChart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        progressChart.setPrefHeight(500);
        progressChart.setAnimated(false);
        progressChart.setLegendVisible(false);
        progressChart.setTitle("Gelişim Grafiğini Görmek İçin Ders ID Girin ve Filtreleyin");
        progressChart.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; -fx-padding: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 5);");

        // Her şeyi sayfaya ekle (Grafik en alta eklendi)
        content.getChildren().addAll(title, filterBox, table, actionBox, progressChart);
        centerLayout.setCenter(content);
    }

    // --- QUİZ GELİŞİM GRAFİĞİNİ GÜNCELLEYEN METOT (SIRALI VE HATASIZ) ---
    private void updateQuizChart(Integer subjectId) {
        if (progressChart == null) return;
        progressChart.getData().clear();

        if (subjectId == null) {
            progressChart.setTitle("Gelişim Grafiğini Görmek İçin Ders ID Girin ve Filtreleyin");
            return;
        }

        progressChart.setTitle("Ders ID: " + subjectId + " - Puan Gelişim İvmesi");
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Puanlar");

        // Hem Tarihe hem de QuizID'ye göre sıralıyoruz ki kronolojik sıra asla bozulmasın
        String sql = "SELECT QuizID, QuizDate, Score FROM QUIZ_RECORDS WHERE StudentID = ? AND SubjectID = ? ORDER BY QuizDate ASC, QuizID ASC";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            stmt.setInt(2, subjectId);
            ResultSet rs = stmt.executeQuery();

            int sayac = 1;
            while (rs.next()) {
                // Aynı güne birden fazla quiz girilirse grafikte üst üste binmesini engelliyoruz
                String dateLabel = rs.getString("QuizDate") + " (#" + sayac + ")";
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(dateLabel, rs.getDouble("Score")));
                sayac++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (!series.getData().isEmpty()) {
            progressChart.getData().add(series);
        }
    }

    // --- TAVSİYELERİM (INSIGHTS) SAYFASI ---
    private void showRecommendations() {
        // Katı arka plan rengini kullan — şeffaflık hilelerine gerek yok
        VBox page = new VBox(20);
        page.setPadding(new Insets(40, 50, 40, 50));
        page.setStyle("-fx-background-color: #5C6B7A;");

        Label title = new Label("Gelişim Tavsiyeleri 💡");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");
        page.getChildren().add(title);

        VBox cardsContainer = new VBox(15);
        boolean hasData = false;

        String sql = "SELECT RecommendationText, RecommendationType, PatternDetected, GeneratedDate " +
                "FROM INSIGHT_RECORDS " +
                "WHERE StudentID = ? " +
                "ORDER BY GeneratedDate DESC";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                hasData = true;
                String text    = rs.getString("RecommendationText");
                String type    = rs.getString("RecommendationType");
                String pattern = rs.getString("PatternDetected");
                String date    = rs.getString("GeneratedDate");

                VBox card = new VBox(10);
                card.setPadding(new Insets(20));
                card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 14; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.10), 12, 0, 0, 5);");

                Label headerLabel = new Label("📌 " + (type != null ? type : "Genel") + "   |   🗓 " + date);
                headerLabel.setStyle("-fx-text-fill: #0984e3; -fx-font-weight: 800; -fx-font-size: 13px;");

                Label messageLabel = new Label(text);
                messageLabel.setWrapText(true);
                messageLabel.setStyle("-fx-text-fill: #2d3436; -fx-font-size: 14px; -fx-line-spacing: 4px;");

                card.getChildren().addAll(headerLabel, messageLabel);

                if (pattern != null && !pattern.trim().isEmpty()) {
                    Label patternLabel = new Label("🔍 Tespit: " + pattern);
                    patternLabel.setStyle("-fx-text-fill: #e17055; -fx-font-weight: 700; -fx-font-size: 12px;");
                    card.getChildren().add(1, patternLabel);
                }

                cardsContainer.getChildren().add(card);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            hasData = true; // hata kartı göster
            VBox errCard = new VBox(8);
            errCard.setPadding(new Insets(16));
            errCard.setStyle("-fx-background-color: #ff7675; -fx-background-radius: 12;");
            Label errTitle = new Label("⚠️ Veritabanı Hatası");
            errTitle.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 14px;");
            Label errMsg = new Label(e.getMessage());
            errMsg.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            errMsg.setWrapText(true);
            errCard.getChildren().addAll(errTitle, errMsg);
            cardsContainer.getChildren().add(errCard);
        }

        if (!hasData) {
            VBox emptyCard = new VBox(10);
            emptyCard.setPadding(new Insets(24));
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setStyle("-fx-background-color: #2d3436; -fx-background-radius: 14;");

            Label emptyIcon = new Label("📭");
            emptyIcon.setStyle("-fx-font-size: 36px;");

            Label emptyTitle = new Label("Henüz tavsiye bulunmuyor");
            emptyTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 17px; -fx-font-weight: 800;");

            Label emptyMsg = new Label("Danışmanın sana tavsiye gönderdiğinde burada görünecek.");
            emptyMsg.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 13px;");
            emptyMsg.setWrapText(true);

            emptyCard.getChildren().addAll(emptyIcon, emptyTitle, emptyMsg);
            cardsContainer.getChildren().add(emptyCard);
        }

        page.getChildren().add(cardsContainer);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #5C6B7A; -fx-background: #5C6B7A; -fx-border-color: transparent;");
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        centerLayout.setCenter(scrollPane);
    }


    // --- VERİTABANI İŞLEMLERİ ---
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

    private void addScheduleToDB(String date, String time, String subjectName) {
        // EndTime sütununa da aynı saati (time) kaydediyoruz.
        String sql = "INSERT INTO STUDY_SESSIONS (StudentID, SubjectID, SessionDate, StartTime, EndTime) " +
                "VALUES (?, (SELECT SubjectID FROM SUBJECT WHERE SubjectName = ? LIMIT 1), ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setString(2, subjectName);
            stmt.setString(3, date);
            stmt.setString(4, time); // StartTime
            stmt.setString(5, time); // EndTime (Aynı!)
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

    private int repeatWeekSchedule(LocalDate refDate, int weekCount) {
        LocalDate monday = refDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        int totalCopied = 0;

        String fetchSql = "SELECT ss.SessionDate, ss.StartTime, ss.SubjectID " +
                "FROM STUDY_SESSIONS ss WHERE ss.StudentID = ? AND ss.SessionDate BETWEEN ? AND ? " +
                "AND HOUR(ss.StartTime) BETWEEN 8 AND 15";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement fetchStmt = conn.prepareStatement(fetchSql)) {
            fetchStmt.setInt(1, studentId);
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
            String checkSql = "SELECT COUNT(*) FROM STUDY_SESSIONS WHERE StudentID = ? AND SessionDate = ? AND StartTime LIKE ?";

            for (int w = 1; w <= weekCount; w++) {
                LocalDate targetMonday = monday.plusWeeks(w);
                for (Object[] entry : entries) {
                    int dayOffset = (int) entry[0];
                    String startTime = (String) entry[1];
                    int subjectIdVal = (int) entry[2];
                    LocalDate targetDate = targetMonday.plusDays(dayOffset);
                    String hourPrefix = startTime.split(":")[0] + ":" + startTime.split(":")[1];

                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setInt(1, studentId);
                        checkStmt.setString(2, targetDate.toString());
                        checkStmt.setString(3, hourPrefix + "%");
                        ResultSet cr = checkStmt.executeQuery();
                        if (cr.next() && cr.getInt(1) > 0) continue;
                    }

                    try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                        insStmt.setInt(1, studentId);
                        insStmt.setInt(2, subjectIdVal);
                        insStmt.setString(3, targetDate.toString());
                        insStmt.setString(4, startTime);
                        insStmt.setString(5, startTime);
                        insStmt.executeUpdate();
                        totalCopied++;
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return totalCopied;
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void populateSubjectComboBox(ComboBox<String> box) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SubjectName FROM SUBJECT")) {
            while (rs.next()) box.getItems().add(rs.getString("SubjectName"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- GÜNLERİ SÜTUNLARA YERLEŞTİREN GARANTİLİ METOT ---
    private int getDayColumn(String dayName) {
        if (dayName == null) return -1;

        // İşletim sisteminin dili ne olursa olsun Türkçe karakter kurallarına göre küçültür
        dayName = dayName.toLowerCase(new Locale("tr"));

        switch (dayName) {
            case "pazartesi": return 1;
            case "salı": return 2;
            case "çarşamba": return 3;
            case "perşembe": return 4;
            case "cuma": return 5;
            case "cumartesi": return 6;
            case "pazar": return 7;
            default: return -1;
        }
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

    // --- RESPONSIVE VE HEDEFLERDEN KONTROLLÜ TABLO KARTI ---
    private VBox createTableCard(String title, String color, String icon, List<Object[]> dataList, boolean isHours) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        // Ders isimleri sığsın diye kart genişliğini 350'den 420'ye çıkardık
        card.setMinWidth(420);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 18; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 20, 0, 0, 8);");

        Label t = new Label(title + " " + icon);
        t.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #2d3436;");

        VBox listContainer = new VBox(10);

        // BAŞLIKLAR
        HBox header = new HBox(20);
        Label h1 = new Label("Ders"); h1.setMinWidth(140); // Ders sütunu genişletildi
        Label h2 = new Label("Hedef"); h2.setMinWidth(70);
        Label h3 = new Label("Kontrol"); h3.setMinWidth(70);
        header.getChildren().addAll(h1, h2, h3);
        header.getChildren().forEach(n -> ((Label)n).setStyle("-fx-text-fill: #b2bec3; -fx-font-weight: 700; -fx-font-size: 12px;"));
        listContainer.getChildren().add(header);

        double totalTarget = 0;
        int completedCount = 0;
        int totalGoals = 0;

        for (Object[] entry : dataList) {
            int subId = (int) entry[0];
            String subName = (String) entry[1]; // Ders ismini listeden çekiyoruz
            double target = isHours ? (double) entry[2] : (double) entry[3];
            boolean isCompleted = (double) entry[4] == 1.0;

            totalTarget += target;
            totalGoals++;
            if (isCompleted) completedCount++;

            // VERİ SATIRI
            HBox row = new HBox(20);
            row.setAlignment(Pos.CENTER_LEFT);

            // Ders ID ve İsim birleştirildi: "201 - Matematik"
            Label l1 = new Label(subId + " - " + subName);
            l1.setMinWidth(140); // Ders sütunu genişletildi
            l1.setStyle("-fx-font-weight: 700; -fx-text-fill: #2d3436;");

            Label l2 = new Label(String.format(Locale.US, "%.1f", target));
            l2.setMinWidth(70);
            l2.setStyle("-fx-text-fill: #636e72;");

            Label l3 = new Label(isCompleted ? "✅" : "❌");
            l3.setMinWidth(70);
            l3.setStyle("-fx-font-size: 14px;");

            row.getChildren().addAll(l1, l2, l3);
            listContainer.getChildren().add(row);
        }

        // TOPLAM SATIRI
        HBox totalBox = new HBox(10);
        totalBox.setPadding(new Insets(10, 0, 0, 0));
        totalBox.setStyle("-fx-border-color: #f1f2f6; -fx-border-width: 1 0 0 0;");
        Label tLbl = new Label("TOPLAM:"); tLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 13px;");
        Label tVals = new Label(String.format(Locale.US, "Hedef: %.1f | Biten: %d/%d", totalTarget, completedCount, totalGoals));
        tVals.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 800;");

        totalBox.getChildren().addAll(tLbl, tVals);

        card.getChildren().addAll(t, listContainer, totalBox);
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

    // --- ÇALIŞMA NOTU POPUP METODU ---
    private void showNotePopup(StudySessionRecord item) {
        String note = item.getSessionNote();
        if (note != null && !note.trim().isEmpty()) {
            Alert noteAlert = new Alert(Alert.AlertType.INFORMATION);
            noteAlert.initOwner(stage);
            noteAlert.setTitle("Çalışma Notu");
            noteAlert.setHeaderText(item.getSubjectName() + " - " + item.getDate());
            noteAlert.setContentText(note);
            noteAlert.showAndWait();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(stage);
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

    public static class GoalRecord {
        private int goalId, subjectId, isCompleted;
        private double targetStudyHours, targetQuizScore;
        private String weekStartDate, endDate;

        public GoalRecord(int id, int subId, String start, double hours, double score, String end, int comp) {
            this.goalId = id; this.subjectId = subId; this.weekStartDate = start;
            this.targetStudyHours = hours; this.targetQuizScore = score;
            this.endDate = end; this.isCompleted = comp;
        }

        public int getGoalId() { return goalId; }
        public int getSubjectId() { return subjectId; }
        public String getWeekStartDate() { return weekStartDate; }
        public double getTargetStudyHours() { return targetStudyHours; }
        public double getTargetQuizScore() { return targetQuizScore; }
        public String getEndDate() { return endDate; }
        public String getStatus() { return isCompleted == 1 ? "Tamamlandı" : "Devam Ediyor"; }
        public int getIsCompleted() { return isCompleted; }
    }
    // --- KRONOMETRE KAYITLARI İÇİN YARDIMCI SINIF ---
    public static class StudySessionRecord {
        private int sessionId;
        private String subjectName;
        private String date;
        private String startTime;
        private String endTime;
        private String duration;
        private int focusLevel;
        private String sessionNote;

        public StudySessionRecord(int sessionId, String sub, String d, String st, String et, String dur, int focusLevel, String sessionNote) {
            this.sessionId = sessionId; this.subjectName = sub; this.date = d; this.startTime = st; this.endTime = et; this.duration = dur;
            this.focusLevel = focusLevel; this.sessionNote = sessionNote;
        }

        public int getSessionId() { return sessionId; }
        public String getSubjectName() { return subjectName; }
        public String getDate() { return date; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getDuration() { return duration; }
        public int getFocusLevel() { return focusLevel; }
        public String getSessionNote() { return sessionNote; }
        // Tablo sütunları için yardımcı getter'lar
        public String getFocusLevelStr() { return focusLevel > 0 ? String.valueOf(focusLevel) : "-"; }
        public String getNotePreview() { return (sessionNote != null && !sessionNote.isEmpty()) ? "📝 Görüntüle" : "-"; }
    }
}