#!/usr/bin/env python3
"""
generate_sql.py
Reads CSV files in the project root and generates a complete database_schema.sql
that includes DDL + all INSERT statements. Fills gaps so every student has
at least 1 quiz, 1 study session, and 1 insight record.
"""

import csv
import random
import os

random.seed(42)  # Reproducible

PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_PATH = os.path.join(PROJECT_DIR, "src", "main", "java", "database_schema.sql")

# ---------------------------------------------------------------------------
# Helper: escape single quotes for SQL strings
# ---------------------------------------------------------------------------
def esc(s):
    if s is None:
        return "NULL"
    return s.replace("'", "''").replace("\\", "\\\\")

# ---------------------------------------------------------------------------
# 1. Read CSVs
# ---------------------------------------------------------------------------

# --- ADVISOR (sabit 2 danışman — öğrenciler ve dersler sadece ID 1 ve 2'ye referans veriyor) ---
advisors = [
    {"AdvisorID": 1, "AD_FullName": "Hakan Aydın",  "AD_Email": "hakan.hoca@rehberlik.com", "AD_Password": "hkn_pass123"},
    {"AdvisorID": 2, "AD_FullName": "Elif Şahin",   "AD_Email": "elif.hoca@rehberlik.com",  "AD_Password": "elf_pass456"},
]

# --- SUBJECT (SUBJECT.csv — no header) ---
subjects = []
with open(os.path.join(PROJECT_DIR, "SUBJECT.csv"), encoding="utf-8") as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        parts = line.split(",", 4)
        subjects.append({
            "SubjectID": int(parts[0]),
            "AdvisorID": int(parts[1]),
            "SubjectName": parts[2],
            "Category": parts[3],
            "DifficultyLevel": int(parts[4]),
        })

subject_ids = [s["SubjectID"] for s in subjects]  # 200..216

# --- STUDENT (MOCK_DATA-2.csv) ---
students = []
with open(os.path.join(PROJECT_DIR, "MOCK_DATA-2.csv"), encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        students.append({
            "StudentID": int(row["StudentID"]),
            "ST_FullName": row["ST_FullName"],
            "ST_Email": row["ST_Email"],
            "ST_Password": row["ST_Password"],
            "AdvisorID": int(row["AdvisorID"]),
            "RegistrationDate": row["RegistrationDate"],
        })

student_ids = {s["StudentID"] for s in students}

# --- QUIZ_RECORDS (MOCK_DATA-3.csv) ---
quizzes = []
with open(os.path.join(PROJECT_DIR, "MOCK_DATA-3.csv"), encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        quizzes.append({
            "QuizID": int(row["QuizID"]),
            "StudentID": int(row["StudentID"]),
            "SubjectID": int(row["SubjectID"]),
            "Score": float(row["Score"]),
            "MaxScore": float(row["MaxScore"]),
            "MinScore": float(row["MinScore"]),
            "QuizDate": row["QuizDate"],
        })

quiz_student_ids = {q["StudentID"] for q in quizzes}

# --- STUDY_SESSIONS (MOCK_DATA-6.csv) ---
sessions = []
with open(os.path.join(PROJECT_DIR, "MOCK_DATA-6.csv"), encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        sessions.append({
            "SessionID": int(row["SessionID"]),
            "StudentID": int(row["StudentID"]),
            "SubjectID": int(row["SubjectID"]),
            "StartTime": row["StartTime"],
            "EndTime": row["EndTime"],
            "SessionDate": row["SessionDate"],
            "FocusLevel": int(row["FocusLevel"]),
            "SessionNote": row["SessionNote"],
        })

session_student_ids = {s["StudentID"] for s in sessions}

# --- INSIGHT_RECORDS (MOCK_DATA-4.csv) ---
insights = []
with open(os.path.join(PROJECT_DIR, "MOCK_DATA-4.csv"), encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        insights.append({
            "RecommendationID": int(row["RecommendationID"]),
            "StudentID": int(row["StudentID"]),
            "RecommendationText": row["RecommendationText"],
            "RecommendationType": row["RecommendationType"],
            "GeneratedDate": row["GeneratedDate"],
        })

insight_student_ids = {i["StudentID"] for i in insights}

# ---------------------------------------------------------------------------
# 2. Identify gaps and generate supplementary data
# ---------------------------------------------------------------------------

# --- Students missing quizzes ---
missing_quiz_students = student_ids - quiz_student_ids
print(f"Students missing quiz: {len(missing_quiz_students)}")

next_quiz_id = max(q["QuizID"] for q in quizzes) + 1
end_time_options = ["00:25:00", "00:30:00", "00:45:00", "01:00:00", "01:15:00", "01:30:00", "02:00:00"]

for sid in sorted(missing_quiz_students):
    quizzes.append({
        "QuizID": next_quiz_id,
        "StudentID": sid,
        "SubjectID": random.choice(subject_ids),
        "Score": round(random.uniform(10, 95), 0),
        "MaxScore": 100.0,
        "MinScore": 0.0,
        "QuizDate": f"{random.choice([2025, 2026, 2027])}-{random.randint(1,12):02d}-{random.randint(1,28):02d}",
    })
    next_quiz_id += 1

# --- Students missing sessions ---
missing_session_students = student_ids - session_student_ids
print(f"Students missing session: {len(missing_session_students)}")

session_notes = [
    "Bugünkü çalışma verimli geçti.",
    "Çalışma süresi hedefe yakındı.",
    "Bir sonraki çalışma için plan yapıldı.",
    "Zorlanılan konular tekrar edildi.",
    "Örnek sorular çözüldü.",
    "Ders notları gözden geçirildi.",
    "Quiz öncesi hazırlık yapıldı.",
    "Konu tekrarı yapıldı.",
    "Odaklanma seviyesi orta düzeydeydi.",
    "Eksik konular belirlendi.",
]

next_session_id = max(s["SessionID"] for s in sessions) + 1

for sid in sorted(missing_session_students):
    sessions.append({
        "SessionID": next_session_id,
        "StudentID": sid,
        "SubjectID": random.choice(subject_ids),
        "StartTime": "00:00:00",
        "EndTime": random.choice(end_time_options),
        "SessionDate": f"{random.choice([2025, 2026, 2027])}-{random.randint(1,12):02d}-{random.randint(1,28):02d}",
        "FocusLevel": random.randint(1, 5),
        "SessionNote": random.choice(session_notes),
    })
    next_session_id += 1

# --- Students missing insights ---
missing_insight_students = student_ids - insight_student_ids
print(f"Students missing insight: {len(missing_insight_students)}")

recommendation_texts = [
    "Bir sonraki quizden önce konuyu tekrar etmelisin.",
    "Her gün en az 30 dakika çalışmalısın.",
    "Zorlandığın derslere daha fazla zaman ayırmalısın.",
    "Düzenli bir çalışma programı oluşturmalısın.",
    "Daha fazla pratik soru çözmelisin.",
    "Çalışırken kısa molalar vermelisin.",
    "Düşük aldığın konuları tekrar etmelisin.",
    "Haftalık çalışma süreni artırmalısın.",
    "Önceki quiz hatalarını tekrar incelemelisin.",
    "Danışmanından ek destek istemelisin.",
]

recommendation_types = [
    "Hedef Takibi", "Zaman Yönetimi", "Çalışma Alışkanlığı", "Motivasyon",
    "Pratik Önerisi", "Danışman Desteği", "Genel Öneri", "Konu Tekrarı",
    "Düşük Puan", "Performans Uyarısı",
]

next_insight_id = max(i["RecommendationID"] for i in insights) + 1

for sid in sorted(missing_insight_students):
    insights.append({
        "RecommendationID": next_insight_id,
        "StudentID": sid,
        "RecommendationText": random.choice(recommendation_texts),
        "RecommendationType": random.choice(recommendation_types),
        "GeneratedDate": f"{random.choice([2025, 2026, 2027])}-{random.randint(1,12):02d}-{random.randint(1,28):02d}",
    })
    next_insight_id += 1

# ---------------------------------------------------------------------------
# 3. Write the SQL file
# ---------------------------------------------------------------------------

# Verify coverage before writing
final_quiz_students = {q["StudentID"] for q in quizzes}
final_session_students = {s["StudentID"] for s in sessions}
final_insight_students = {i["StudentID"] for i in insights}

assert student_ids <= final_quiz_students, "Some students still missing quizzes!"
assert student_ids <= final_session_students, "Some students still missing sessions!"
assert student_ids <= final_insight_students, "Some students still missing insights!"

print(f"\nFinal counts:")
print(f"  Advisors: {len(advisors)}")
print(f"  Students: {len(students)}")
print(f"  Subjects: {len(subjects)}")
print(f"  Quizzes:  {len(quizzes)}")
print(f"  Sessions: {len(sessions)}")
print(f"  Insights: {len(insights)}")
print(f"\nAll students covered: ✅")

with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
    # -----------------------------------------------------------------------
    # DDL
    # -----------------------------------------------------------------------
    f.write("-- ============================================================\n")
    f.write("-- BIM216_Project - Full Database Schema + Data\n")
    f.write("-- Generated by generate_sql.py from CSV files\n")
    f.write("-- ============================================================\n\n")

    f.write("-- PART 1: TABLE CREATION (DATA DEFINITION LANGUAGE - DDL)\n\n")
    f.write("-- Create and use database\n")
    f.write("DROP DATABASE IF EXISTS BIM216_Project;\n")
    f.write("CREATE DATABASE BIM216_Project CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n")
    f.write("USE BIM216_Project;\n\n")

    f.write("-- ADVISOR Table: Stores information about instructors or advisors who monitor student progress.\n")
    f.write("""CREATE TABLE ADVISOR (
    AdvisorID INT PRIMARY KEY,
    AD_FullName VARCHAR(100) NOT NULL,
    AD_Email VARCHAR(100) UNIQUE NOT NULL,
    AD_Password VARCHAR(100) NOT NULL
) DEFAULT CHARSET=utf8mb4;\n\n""")

    f.write("-- STUDENT Table: Stores the main users of the system. Includes a foreign key linking them to an Advisor.\n")
    f.write("""CREATE TABLE STUDENT (
    StudentID INT PRIMARY KEY,
    AdvisorID INT,
    ST_FullName VARCHAR(100) NOT NULL,
    ST_Email VARCHAR(100) UNIQUE NOT NULL,
    RegistrationDate DATE,
    ST_Password VARCHAR(100) NOT NULL,
    FOREIGN KEY (AdvisorID) REFERENCES ADVISOR(AdvisorID) ON DELETE SET NULL
) DEFAULT CHARSET=utf8mb4;\n\n""")

    f.write("-- SUBJECT Table: Stores the courses/topics students are studying. Created by an Advisor.\n")
    f.write("""CREATE TABLE SUBJECT (
    SubjectID INT PRIMARY KEY,
    AdvisorID INT,
    SubjectName VARCHAR(100) NOT NULL,
    Category VARCHAR(50),
    DifficultyLevel INT,
    FOREIGN KEY (AdvisorID) REFERENCES ADVISOR(AdvisorID) ON DELETE SET NULL
) DEFAULT CHARSET=utf8mb4;\n\n""")

    f.write("-- STUDY_SESSIONS Table: Records the focus timer logs for each student per subject.\n")
    f.write("""CREATE TABLE STUDY_SESSIONS (
    SessionID INT PRIMARY KEY AUTO_INCREMENT,
    StudentID INT,
    SubjectID INT,
    StartTime TIME,
    EndTime TIME,
    SessionDate DATE,
    FocusLevel INT,
    SessionNote TEXT,
    FOREIGN KEY (StudentID) REFERENCES STUDENT(StudentID) ON DELETE CASCADE,
    FOREIGN KEY (SubjectID) REFERENCES SUBJECT(SubjectID) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4;\n\n""")

    f.write("-- QUIZ_RECORDS Table: Stores the quiz performance data to track academic improvement.\n")
    f.write("""CREATE TABLE QUIZ_RECORDS (
    QuizID INT PRIMARY KEY AUTO_INCREMENT,
    StudentID INT,
    SubjectID INT,
    Score DECIMAL(5,2),
    MaxScore DECIMAL(5,2),
    MinScore DECIMAL(5,2),
    QuizDate DATE,
    FOREIGN KEY (StudentID) REFERENCES STUDENT(StudentID) ON DELETE CASCADE,
    FOREIGN KEY (SubjectID) REFERENCES SUBJECT(SubjectID) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4;\n\n""")

    f.write("-- GOALS Table: Allows students to set measurable weekly targets for study hours and quiz scores.\n")
    f.write("""CREATE TABLE GOALS (
    GoalID INT PRIMARY KEY AUTO_INCREMENT,
    StudentID INT,
    SubjectID INT,
    WeekStartDate DATE,
    TargetStudyHours DECIMAL(5,2),
    TargetQuizScore DECIMAL(5,2),
    EndDate DATE,
    IsCompleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (StudentID) REFERENCES STUDENT(StudentID) ON DELETE CASCADE,
    FOREIGN KEY (SubjectID) REFERENCES SUBJECT(SubjectID) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4;\n\n""")

    f.write("-- INSIGHT_RECORDS Table (Weak Entity): Stores system-generated productivity patterns and recommendations for a specific student.\n")
    f.write("""CREATE TABLE INSIGHT_RECORDS (
    RecommendationID INT PRIMARY KEY AUTO_INCREMENT,
    StudentID INT,
    RecommendationText TEXT,
    RecommendationType VARCHAR(50),
    PatternDetected VARCHAR(100),
    GeneratedDate DATETIME,
    FOREIGN KEY (StudentID) REFERENCES STUDENT(StudentID) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4;\n\n""")

    # -----------------------------------------------------------------------
    # DML — INSERT statements
    # -----------------------------------------------------------------------
    f.write("\n-- ============================================================\n")
    f.write("-- PART 2: DATA INSERTION (DATA MANIPULATION LANGUAGE - DML)\n")
    f.write("-- ============================================================\n\n")

    # --- ADVISOR ---
    f.write("-- Inserting Advisors (2 danışman: Hakan Aydın ve Elif Şahin)\n")
    for a in advisors:
        f.write(f"INSERT INTO ADVISOR (AdvisorID, AD_FullName, AD_Email, AD_Password) VALUES "
                f"({a['AdvisorID']}, '{esc(a['AD_FullName'])}', '{esc(a['AD_Email'])}', '{esc(a['AD_Password'])}');\n")
    f.write("\n")

    # --- SUBJECT ---
    f.write("-- Inserting Subjects (from SUBJECT.csv)\n")
    for s in subjects:
        f.write(f"INSERT INTO SUBJECT (SubjectID, AdvisorID, SubjectName, Category, DifficultyLevel) VALUES "
                f"({s['SubjectID']}, {s['AdvisorID']}, '{esc(s['SubjectName'])}', '{esc(s['Category'])}', {s['DifficultyLevel']});\n")
    f.write("\n")

    # --- STUDENT (batch insert for performance, 50 per statement) ---
    f.write("-- Inserting Students (from MOCK_DATA-2.csv, 1000 records)\n")
    batch_size = 50
    for i in range(0, len(students), batch_size):
        batch = students[i:i+batch_size]
        f.write("INSERT INTO STUDENT (StudentID, AdvisorID, ST_FullName, ST_Email, RegistrationDate, ST_Password) VALUES\n")
        lines = []
        for st in batch:
            lines.append(f"({st['StudentID']}, {st['AdvisorID']}, '{esc(st['ST_FullName'])}', '{esc(st['ST_Email'])}', '{st['RegistrationDate']}', '{esc(st['ST_Password'])}')")
        f.write(",\n".join(lines) + ";\n\n")

    # --- QUIZ_RECORDS (batch insert) ---
    f.write(f"-- Inserting Quiz Records ({len(quizzes)} records: 1000 from CSV + {len(missing_quiz_students)} generated)\n")
    for i in range(0, len(quizzes), batch_size):
        batch = quizzes[i:i+batch_size]
        f.write("INSERT INTO QUIZ_RECORDS (QuizID, StudentID, SubjectID, Score, MaxScore, MinScore, QuizDate) VALUES\n")
        lines = []
        for q in batch:
            lines.append(f"({q['QuizID']}, {q['StudentID']}, {q['SubjectID']}, {q['Score']:.2f}, {q['MaxScore']:.2f}, {q['MinScore']:.2f}, '{q['QuizDate']}')")
        f.write(",\n".join(lines) + ";\n\n")

    # --- STUDY_SESSIONS (batch insert) ---
    f.write(f"-- Inserting Study Sessions ({len(sessions)} records: 1000 from CSV + {len(missing_session_students)} generated)\n")
    for i in range(0, len(sessions), batch_size):
        batch = sessions[i:i+batch_size]
        f.write("INSERT INTO STUDY_SESSIONS (SessionID, StudentID, SubjectID, StartTime, EndTime, SessionDate, FocusLevel, SessionNote) VALUES\n")
        lines = []
        for s in batch:
            f_level = s['FocusLevel']
            note = esc(s['SessionNote'])
            lines.append(f"({s['SessionID']}, {s['StudentID']}, {s['SubjectID']}, '{s['StartTime']}', '{s['EndTime']}', '{s['SessionDate']}', {f_level}, '{note}')")
        f.write(",\n".join(lines) + ";\n\n")

    # --- INSIGHT_RECORDS (batch insert) ---
    f.write(f"-- Inserting Insight Records ({len(insights)} records: 1000 from CSV + {len(missing_insight_students)} generated)\n")
    for i in range(0, len(insights), batch_size):
        batch = insights[i:i+batch_size]
        f.write("INSERT INTO INSIGHT_RECORDS (RecommendationID, StudentID, RecommendationText, RecommendationType, PatternDetected, GeneratedDate) VALUES\n")
        lines = []
        for ins in batch:
            lines.append(f"({ins['RecommendationID']}, {ins['StudentID']}, '{esc(ins['RecommendationText'])}', '{esc(ins['RecommendationType'])}', NULL, '{ins['GeneratedDate']}')")
        f.write(",\n".join(lines) + ";\n\n")

    # --- GOALS (from original schema sample data) ---
    f.write("-- Inserting sample Goals (Haftalık TYT/AYT Hedefleri)\n")
    f.write("""INSERT INTO GOALS (GoalID, StudentID, SubjectID, WeekStartDate, TargetStudyHours, TargetQuizScore, EndDate, IsCompleted) VALUES
(501, 101, 201, '2026-03-30', 10.00, 35.00, '2026-04-05', TRUE),
(502, 101, 202, '2026-03-30', 12.00, 30.00, '2026-04-05', FALSE),
(503, 101, 203, '2026-03-30', 12.00, 35.00, '2026-04-05', TRUE),
(504, 101, 204, '2026-03-30', 8.00, 25.00, '2026-04-05', FALSE),
(505, 102, 201, '2026-03-30', 10.00, 30.00, '2026-04-05', FALSE),
(506, 102, 202, '2026-03-30', 10.00, 25.00, '2026-04-05', FALSE),
(507, 102, 203, '2026-03-30', 12.00, 32.00, '2026-04-05', FALSE),
(508, 102, 204, '2026-03-30', 8.00, 22.00, '2026-04-05', FALSE);
""")

    f.write("\n-- ============================================================\n")
    f.write("-- END OF SCRIPT\n")
    f.write("-- ============================================================\n")

print(f"\n✅ SQL file written to: {OUTPUT_PATH}")
