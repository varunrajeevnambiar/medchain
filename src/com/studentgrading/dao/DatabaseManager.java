package com.studentgrading.dao;

import com.studentgrading.model.AcademicStanding;
import com.studentgrading.model.Grade;
import com.studentgrading.model.Subject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * JDBC Database Manager.
 * Manages JDBC connections, initializes schema, and seeds pre-inserted records.
 * Supports SQLite (default, embedded zero-config) and MySQL.
 */
public class DatabaseManager {

    private static String dbUrl = "jdbc:sqlite:student_grades.db";
    private static String dbUser = "";
    private static String dbPassword = "";
    private static String dbDriver = "org.sqlite.JDBC";
    private static boolean isInitialized = false;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream input = new FileInputStream(propFile)) {
                Properties prop = new Properties();
                prop.load(input);
                dbUrl = prop.getProperty("db.url", dbUrl);
                dbUser = prop.getProperty("db.user", dbUser);
                dbPassword = prop.getProperty("db.password", dbPassword);
                dbDriver = prop.getProperty("db.driver", dbDriver);
            } catch (Exception e) {
                System.err.println("Notice: Using default database properties. (" + e.getMessage() + ")");
            }
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            // Driver may already be registered via ServiceLoader in modern JDBC
        }
        if (dbUser.isEmpty() && dbPassword.isEmpty()) {
            return DriverManager.getConnection(dbUrl);
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Initializes database tables and automatically seeds pre-inserted student records.
     */
    public static synchronized void initializeDatabase() {
        if (isInitialized) return;

        boolean isSqlite = dbUrl.contains("sqlite");

        String createStudentsTable = isSqlite ?
                "CREATE TABLE IF NOT EXISTS students (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  roll_number VARCHAR(30) UNIQUE NOT NULL," +
                "  full_name VARCHAR(100) NOT NULL," +
                "  department VARCHAR(50)," +
                "  semester INTEGER," +
                "  total_score REAL," +
                "  percentage REAL," +
                "  gpa REAL," +
                "  final_grade VARCHAR(10)," +
                "  academic_standing VARCHAR(50)" +
                ");" :
                "CREATE TABLE IF NOT EXISTS students (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  roll_number VARCHAR(30) UNIQUE NOT NULL," +
                "  full_name VARCHAR(100) NOT NULL," +
                "  department VARCHAR(50)," +
                "  semester INT," +
                "  total_score DOUBLE," +
                "  percentage DOUBLE," +
                "  gpa DOUBLE," +
                "  final_grade VARCHAR(10)," +
                "  academic_standing VARCHAR(50)" +
                ");";

        String createMarksTable = isSqlite ?
                "CREATE TABLE IF NOT EXISTS student_marks (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  student_id INTEGER NOT NULL," +
                "  subject_code VARCHAR(20) NOT NULL," +
                "  internal_marks REAL," +
                "  external_marks REAL," +
                "  total_marks REAL," +
                "  grade VARCHAR(10)," +
                "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE" +
                ");" :
                "CREATE TABLE IF NOT EXISTS student_marks (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  student_id INT NOT NULL," +
                "  subject_code VARCHAR(20) NOT NULL," +
                "  internal_marks DOUBLE," +
                "  external_marks DOUBLE," +
                "  total_marks DOUBLE," +
                "  grade VARCHAR(10)," +
                "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createStudentsTable);
            stmt.execute(createMarksTable);

            // Check if data is already inserted
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM students");
            int existingCount = 0;
            if (rs.next()) {
                existingCount = rs.getInt("count");
            }

            if (existingCount == 0) {
                System.out.println("No existing records found. Pre-inserting realistic student datasets into database...");
                seedInitialData(conn);
                System.out.println("Pre-inserted dataset successfully loaded into JDBC database!");
            } else {
                System.out.println("JDBC Database already contains " + existingCount + " pre-inserted student records.");
            }

            isInitialized = true;
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Pre-inserts realistic student records across different grade categories
     * (Outstanding, Distinction, First Class, Average, Fail) so all enums are represented.
     */
    private static void seedInitialData(Connection conn) throws SQLException {
        // Sample Students data: roll, name, dept, sem, subjects marks
        Object[][] seedStudents = {
            {"4SU21CS001", "Aarav Sharma", "Computer Science & Engineering", 6,
             new double[][]{{38.0, 56.0}, {39.0, 57.0}, {36.0, 55.0}, {37.0, 58.0}, {38.0, 54.0}}}, // ~93.6% (A+)

            {"4SU21IS045", "Diya Patel", "Information Science & Engineering", 6,
             new double[][]{{34.0, 50.0}, {35.0, 52.0}, {32.0, 48.0}, {36.0, 51.0}, {33.0, 49.0}}}, // ~84.0% (A)

            {"4SU21AI012", "Rohan Joseph", "Artificial Intelligence & Data Science", 4,
             new double[][]{{31.0, 46.0}, {30.0, 45.0}, {28.0, 44.0}, {33.0, 47.0}, {29.0, 43.0}}}, // ~75.2% (B+)

            {"4SU21EC089", "Meera Nambiar", "Electronics & Communication", 6,
             new double[][]{{27.0, 41.0}, {26.0, 42.0}, {25.0, 40.0}, {28.0, 43.0}, {26.0, 41.0}}}, // ~67.8% (B)

            {"4SU21CS102", "Vikram Rao", "Computer Science & Engineering", 6,
             new double[][]{{22.0, 32.0}, {21.0, 33.0}, {20.0, 31.0}, {23.0, 34.0}, {21.0, 31.0}}}, // ~53.6% (C)

            {"4SU21CS115", "Kiran Kumar", "Computer Science & Engineering", 6,
             new double[][]{{12.0, 18.0}, {14.0, 20.0}, {10.0, 15.0}, {15.0, 22.0}, {11.0, 16.0}}}  // ~34.6% (F)
        };

        Subject[] subjects = Subject.values();

        String insertStudentSql = "INSERT INTO students (roll_number, full_name, department, semester, total_score, percentage, gpa, final_grade, academic_standing) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertMarkSql = "INSERT INTO student_marks (student_id, subject_code, internal_marks, external_marks, total_marks, grade) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pStudent = conn.prepareStatement(insertStudentSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pMark = conn.prepareStatement(insertMarkSql)) {

            for (Object[] row : seedStudents) {
                String roll = (String) row[0];
                String name = (String) row[1];
                String dept = (String) row[2];
                int sem = (int) row[3];
                double[][] marks = (double[][]) row[4];

                // Calculate metrics with autoboxing
                double totalScore = 0.0;
                double weightedGpaSum = 0.0;
                int totalCredits = 0;
                boolean anyFail = false;

                for (int i = 0; i < marks.length && i < subjects.length; i++) {
                    double tot = marks[i][0] + marks[i][1];
                    totalScore += tot;
                    Grade subGrade = Grade.fromPercentage(tot);
                    if (subGrade == Grade.F) anyFail = true;
                    int cred = subjects[i].getCredits();
                    totalCredits += cred;
                    weightedGpaSum += (subGrade.getGradePoint() * cred);
                }

                double percentage = Math.round((totalScore / (marks.length * 100.0) * 100.0) * 100.0) / 100.0;
                double gpa = (totalCredits > 0) ? Math.round((weightedGpaSum / totalCredits) * 100.0) / 100.0 : 0.0;
                Grade finalGrade = anyFail ? Grade.F : Grade.fromPercentage(percentage);
                AcademicStanding standing = anyFail ? AcademicStanding.PROBATION : AcademicStanding.fromGpa(gpa);

                pStudent.setString(1, roll);
                pStudent.setString(2, name);
                pStudent.setString(3, dept);
                pStudent.setInt(4, sem);
                pStudent.setDouble(5, totalScore);
                pStudent.setDouble(6, percentage);
                pStudent.setDouble(7, gpa);
                pStudent.setString(8, finalGrade.getLetter());
                pStudent.setString(9, standing.getLabel());
                pStudent.executeUpdate();

                ResultSet generatedKeys = pStudent.getGeneratedKeys();
                int studentId = 0;
                if (generatedKeys.next()) {
                    studentId = generatedKeys.getInt(1);
                }

                for (int i = 0; i < marks.length && i < subjects.length; i++) {
                    double internal = marks[i][0];
                    double external = marks[i][1];
                    double subjectTotal = internal + external;
                    Grade subGrade = Grade.fromPercentage(subjectTotal);

                    pMark.setInt(1, studentId);
                    pMark.setString(2, subjects[i].getCode());
                    pMark.setDouble(3, internal);
                    pMark.setDouble(4, external);
                    pMark.setDouble(5, subjectTotal);
                    pMark.setString(6, subGrade.getLetter());
                    pMark.executeUpdate();
                }
            }
        }
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbDriver() {
        return dbDriver;
    }
}

