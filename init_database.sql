CREATE DATABASE IF NOT EXISTS BIM216_Project;
USE BIM216_Project;

CREATE TABLE ADVISOR
(
    AdvisorID   int          not null
        primary key,
    AD_FullName varchar(100) not null,
    AD_Email    varchar(100) not null,
    AD_Password varchar(100) not null,
    constraint AD_Email
        unique (AD_Email)
);

CREATE TABLE STUDENT
(
    StudentID        int          not null
        primary key,
    AdvisorID        int          null,
    ST_FullName      varchar(100) not null,
    ST_Email         varchar(100) not null,
    RegistrationDate date         null,
    ST_Password      varchar(100) not null,
    constraint ST_Email
        unique (ST_Email),
    constraint student_ibfk_1
        foreign key (AdvisorID) references BIM216_Project.ADVISOR (AdvisorID)
            on delete set null
);

create index AdvisorID
    on STUDENT (AdvisorID);

CREATE TABLE SUBJECT
(
    SubjectID       int          not null
        primary key,
    AdvisorID       int          null,
    SubjectName     varchar(100) not null,
    Category        varchar(50)  null,
    DifficultyLevel int          null,
    constraint subject_ibfk_1
        foreign key (AdvisorID) references BIM216_Project.ADVISOR (AdvisorID)
            on delete set null
);

create index AdvisorID
    on SUBJECT (AdvisorID);

CREATE TABLE GOALS
(
    GoalID           int auto_increment
        primary key,
    StudentID        int                  null,
    SubjectID        int                  null,
    WeekStartDate    date                 null,
    TargetStudyHours decimal(5, 2)        null,
    TargetQuizScore  decimal(5, 2)        null,
    EndDate          date                 null,
    IsCompleted      tinyint(1) default 0 null,
    constraint goals_ibfk_1
        foreign key (StudentID) references BIM216_Project.STUDENT (StudentID)
            on delete cascade,
    constraint goals_ibfk_2
        foreign key (SubjectID) references BIM216_Project.SUBJECT (SubjectID)
            on delete cascade
);

create index StudentID
    on GOALS (StudentID);

create index SubjectID
    on GOALS (SubjectID);

CREATE TABLE INSIGHT_RECORDS
(
    RecommendationID   int          not null
        primary key,
    StudentID          int          null,
    RecommendationText text         null,
    RecommendationType varchar(50)  null,
    PatternDetected    varchar(100) null,
    GeneratedDate      datetime     null,
    constraint insight_records_ibfk_1
        foreign key (StudentID) references BIM216_Project.STUDENT (StudentID)
            on delete cascade
);

create index StudentID
    on INSIGHT_RECORDS (StudentID);

CREATE TABLE QUIZ_RECORDS
(
    QuizID    int auto_increment
        primary key,
    StudentID int           null,
    SubjectID int           null,
    Score     decimal(5, 2) null,
    MaxScore  decimal(5, 2) null,
    MinScore  decimal(5, 2) null,
    QuizDate  date          null,
    constraint quiz_records_ibfk_1
        foreign key (StudentID) references BIM216_Project.STUDENT (StudentID)
            on delete cascade,
    constraint quiz_records_ibfk_2
        foreign key (SubjectID) references BIM216_Project.SUBJECT (SubjectID)
            on delete cascade
);

create index StudentID
    on QUIZ_RECORDS (StudentID);

create index SubjectID
    on QUIZ_RECORDS (SubjectID);

CREATE TABLE STUDY_SESSIONS
(
    SessionID   int auto_increment
        primary key,
    StudentID   int  null,
    SubjectID   int  null,
    StartTime   time null,
    EndTime     time null,
    SessionDate date null,
    FocusLevel  int  null,
    SessionNote text null,
    constraint study_sessions_ibfk_1
        foreign key (StudentID) references BIM216_Project.STUDENT (StudentID)
            on delete cascade,
    constraint study_sessions_ibfk_2
        foreign key (SubjectID) references BIM216_Project.SUBJECT (SubjectID)
            on delete cascade
);

create index StudentID
    on STUDY_SESSIONS (StudentID);

create index SubjectID
    on STUDY_SESSIONS (SubjectID);

-- Sample Data
INSERT INTO ADVISOR (AdvisorID, AD_FullName, AD_Email, AD_Password) VALUES
(1, 'Hakan Aydın', 'hakan.hoca@rehberlik.com', 'hkn_pass123'),
(2, 'Elif Şahin', 'elif.hoca@rehberlik.com', 'elf_pass456');

INSERT INTO STUDENT (StudentID, AdvisorID, ST_FullName, ST_Email, RegistrationDate, ST_Password) VALUES
(101, 1, 'Deniz Yılmaz', 'deniz.yilmaz@yks2026.com', '2026-01-10', 'dnz_yks26'),
(102, 2, 'Mert Kaya', 'mert.kaya@yks2026.com', '2026-01-15', 'mrt_hedef10K');

INSERT INTO SUBJECT (SubjectID, AdvisorID, SubjectName, Category, DifficultyLevel) VALUES
(201, 1, 'TYT Türkçe', 'Sözel', 3),
(202, 1, 'TYT Matematik', 'Sayısal', 4),
(203, 2, 'AYT Matematik', 'Sayısal', 5),
(204, 2, 'AYT Fizik', 'Sayısal', 5);

INSERT INTO STUDY_SESSIONS (SessionID, StudentID, SubjectID, StartTime, EndTime, SessionDate, FocusLevel, SessionNote) VALUES
(301, 101, 201, '09:00:00', '10:30:00', '2026-04-01', 8, 'Paragraf rutin testleri çözüldü, dil bilgisi ses olayları tekrarı yapıldı.'),
(302, 101, 203, '14:00:00', '16:00:00', '2026-04-02', 9, 'Türev alma kuralları fasikülü bitirildi, çok verimliydi.'),
(303, 102, 202, '10:00:00', '12:30:00', '2026-04-03', 7, 'Problemler denemesi çözüldü, hız problemlerinde hala zaman kaybediyorum.');

INSERT INTO QUIZ_RECORDS (QuizID, StudentID, SubjectID, Score, MaxScore, MinScore, QuizDate) VALUES
(401, 101, 201, 32.50, 40.00, 0.00, '2026-04-03'),
(402, 102, 202, 25.25, 40.00, 0.00, '2026-04-04');

INSERT INTO GOALS (GoalID, StudentID, SubjectID, WeekStartDate, TargetStudyHours, TargetQuizScore, EndDate, IsCompleted) VALUES
(501, 101, 203, '2026-03-30', 12.00, 35.00, '2026-04-05', TRUE),
(502, 102, 204, '2026-03-30', 10.00, 10.00, '2026-04-05', FALSE);

INSERT INTO INSIGHT_RECORDS (RecommendationID, StudentID, RecommendationText, RecommendationType, PatternDetected, GeneratedDate) VALUES
(601, 101, 'AYT Matematik çalışmalarında öğleden sonraları çok daha yüksek odaklanma (Focus Seviyesi: 9) gösteriyorsun. Limit ve Türev gibi zor konuları sabah yerine bu saatlere alarak verimini artırabilirsin.', 'Program Optimizasyonu', 'Öğleden Sonra Yüksek Verim', '2026-04-04 18:30:00');

