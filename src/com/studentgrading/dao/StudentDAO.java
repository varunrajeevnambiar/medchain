package com.studentgrading.dao;

import com.studentgrading.model.AcademicStanding;
import com.studentgrading.model.Grade;
import com.studentgrading.model.Student;
import com.studentgrading.model.StudentMark;
import com.studentgrading.model.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Student and StudentMark entities.
 * Demonstrates:
 * 1. JDBC PreparedStatements and ResultSets.
 * 2. Mapping relational database rows to Java Enum types (Grade, Subject, AcademicStanding).
 * 3. Autoboxing when reading columns into wrapper objects.
 */
public class StudentDAO {

    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY total_score DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Student s = mapStudent(rs);
                // Load associated subject marks
                s.setMarksList(getMarksForStudent(conn, s.getId()));
                students.add(s);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all students: " + e.getMessage());
        }
        return students;
    }

    public Student getStudentById(int id) {
        String sql = "SELECT * FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Student s = mapStudent(rs);
                    s.setMarksList(getMarksForStudent(conn, s.getId()));
                    return s;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching student by ID: " + e.getMessage());
        }
        return null;
    }

    public boolean insertStudentWithMarks(Student student, List<StudentMark> marks) {
        String insertStudent = "INSERT INTO students (roll_number, full_name, department, semester, total_score, percentage, gpa, final_grade, academic_standing) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertMark = "INSERT INTO student_marks (student_id, subject_code, internal_marks, external_marks, total_marks, grade) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Transaction

            try (PreparedStatement pStudent = conn.prepareStatement(insertStudent, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement pMark = conn.prepareStatement(insertMark)) {

                student.setMarksList(marks);
                student.calculateGrades();

                pStudent.setString(1, student.getRollNumber());
                pStudent.setString(2, student.getFullName());
                pStudent.setString(3, student.getDepartment());
                pStudent.setInt(4, student.getSemester() != null ? student.getSemester() : 1);
                pStudent.setDouble(5, student.getTotalScore() != null ? student.getTotalScore() : 0.0);
                pStudent.setDouble(6, student.getPercentage() != null ? student.getPercentage() : 0.0);
                pStudent.setDouble(7, student.getGpa() != null ? student.getGpa() : 0.0);
                pStudent.setString(8, student.getFinalGrade() != null ? student.getFinalGrade().getLetter() : "F");
                pStudent.setString(9, student.getAcademicStanding() != null ? student.getAcademicStanding().getLabel() : "Probation");
                pStudent.executeUpdate();

                ResultSet keys = pStudent.getGeneratedKeys();
                int studentId = 0;
                if (keys.next()) {
                    studentId = keys.getInt(1);
                    student.setId(studentId); // Autoboxing
                }

                if (marks != null) {
                    for (StudentMark mark : marks) {
                        pMark.setInt(1, studentId);
                        pMark.setString(2, mark.getSubject() != null ? mark.getSubject().getCode() : "UNKNOWN");
                        pMark.setDouble(3, mark.getInternalMarks() != null ? mark.getInternalMarks() : 0.0);
                        pMark.setDouble(4, mark.getExternalMarks() != null ? mark.getExternalMarks() : 0.0);
                        pMark.setDouble(5, mark.getTotalMarks() != null ? mark.getTotalMarks() : 0.0);
                        pMark.setString(6, mark.getGrade() != null ? mark.getGrade().getLetter() : "F");
                        pMark.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Transaction rolled back: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteStudent(int id) {
        String deleteMarks = "DELETE FROM student_marks WHERE student_id = ?";
        String deleteStudent = "DELETE FROM students WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pMarks = conn.prepareStatement(deleteMarks);
                 PreparedStatement pStudent = conn.prepareStatement(deleteStudent)) {

                pMarks.setInt(1, id);
                pMarks.executeUpdate();

                pStudent.setInt(1, id);
                int affected = pStudent.executeUpdate();

                conn.commit();
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Rollback delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Delete error: " + e.getMessage());
            return false;
        }
    }

    private List<StudentMark> getMarksForStudent(Connection conn, int studentId) throws SQLException {
        List<StudentMark> marks = new ArrayList<>();
        String sql = "SELECT * FROM student_marks WHERE student_id = ? ORDER BY id ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StudentMark m = new StudentMark();
                    m.setId(rs.getInt("id")); // Autoboxing int -> Integer
                    m.setStudentId(rs.getInt("student_id"));
                    m.setSubject(Subject.fromCodeOrName(rs.getString("subject_code")));
                    m.setInternalMarks(rs.getDouble("internal_marks")); // Autoboxing double -> Double
                    m.setExternalMarks(rs.getDouble("external_marks"));
                    m.setTotalMarks(rs.getDouble("total_marks"));
                    m.setGrade(Grade.fromLetter(rs.getString("grade")));
                    marks.add(m);
                }
            }
        }
        return marks;
    }

    private Student mapStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id")); // Autoboxing
        s.setRollNumber(rs.getString("roll_number"));
        s.setFullName(rs.getString("full_name"));
        s.setDepartment(rs.getString("department"));
        s.setSemester(rs.getInt("semester"));
        s.setTotalScore(rs.getDouble("total_score"));
        s.setPercentage(rs.getDouble("percentage"));
        s.setGpa(rs.getDouble("gpa"));

        // Enumerations mapping
        String gradeStr = rs.getString("final_grade");
        s.setFinalGrade(Grade.fromLetter(gradeStr));

        // Academic Standing mapping
        String standingStr = rs.getString("academic_standing");
        if (standingStr != null) {
            for (AcademicStanding as : AcademicStanding.values()) {
                if (as.getLabel().equalsIgnoreCase(standingStr)) {
                    s.setAcademicStanding(as);
                    break;
                }
            }
        }
        if (s.getAcademicStanding() == null) {
            s.setAcademicStanding(AcademicStanding.fromGpa(s.getGpa()));
        }

        return s;
    }
}

