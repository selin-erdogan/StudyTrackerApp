# FocusUp – Study Tracker App

A JavaFX desktop application that helps students track their study sessions, goals and mock-exam results, and lets advisors follow and guide their students. Built as the term project for **BIM216**.

## Features

**Student**
- Focus timer that records study sessions per subject
- Weekly study schedule
- Goals with progress tracking (target vs. actual study hours)
- Mock-exam (quiz) records
- Per-subject productivity report with charts
- Advice received from the advisor

**Advisor**
- Add, update and delete managed students
- Open a detail panel with a student's sessions, quizzes and goals
- Send advice and schedules, keep private mentor notes

## Tech Stack

- Java 23, JavaFX 21
- MySQL 8 (JDBC, `mysql-connector-j`)
- Maven

## Database

7 tables: `ADVISOR`, `STUDENT`, `SUBJECT`, `STUDY_SESSIONS`, `QUIZ_RECORDS`, `GOALS`, `INSIGHT_RECORDS`.
[`init_database.sql`](init_database.sql) creates the `BIM216_Project` database, the tables and some sample data.

## Getting Started

1. Install JDK 23, Maven 3.8+ and MySQL 8.
2. Run `init_database.sql` in MySQL (Workbench or `mysql < init_database.sql`).
3. Set the database credentials as environment variables. The defaults are user `root` and an empty password.
   ```bash
   export DB_USER=root
   export DB_PASSWORD=your_password
   ```
4. Open the project in IntelliJ IDEA and run `main.java`.
5. Choose **ÖĞRENCİ GİRİŞİ** (student) or **DANIŞMAN GİRİŞİ** (advisor) and log in with one of the sample accounts inserted by `init_database.sql`.

> The database credentials are read from the `DB_USER` and `DB_PASSWORD` environment variables and are not stored in the source code.

## Project Structure

```text
src/main/java/
├── main.java               # Entry point
├── LoginApp.java           # Role-based login screen
├── StudentDashboard.java   # Student UI and logic
├── AdvisorDashboard.java   # Advisor UI and logic
├── DatabaseConnection.java # JDBC connection helper
└── Quiz.java               # Quiz model
init_database.sql           # Schema and sample data
```
