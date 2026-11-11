package com.studentgrading.model;

import com.studentgrading.annotations.Auditable;
import com.studentgrading.annotations.Author;
import com.studentgrading.annotations.Column;
import com.studentgrading.annotations.Entity;
import com.studentgrading.annotations.GradeRange;
import com.studentgrading.annotations.Id;
import com.studentgrading.annotations.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Student Entity Model.
 * Demonstrates:
 * 1. Custom Annotations (@Author, @Entity, @Table, @Id, @Column, @GradeRange, @Auditable).
 * 2. Standard Built-in Annotations (@Override, @Deprecated, @SuppressWarnings).
 * 3. Enumerations (Grade, AcademicStanding, Subject).
 * 4. Autoboxing & Unboxing (Wrapper classes Integer/Double used for collections and metrics).
 */
@Author(
    name = "Java Lab Team",
    version = "2.5",
    institution = "Department of Computer Science",
    description = "Core student grading and academic performance entity"
)
@Entity(description = "Primary student academic record")
@Table(name = "students")
public class Student {

    @Id(autoIncrement = true)
    @Column(name = "id")
    private Integer id; // Wrapper Integer (Autoboxed)

    @Column(name = "roll_number", nullable = false, length = 30, unique = true)
    private String rollNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "semester")
    private Integer semester; // Wrapper Integer

    @Column(name = "total_score")
    @GradeRange(min = 0.0, max = 500.0, message = "Total score across 5 subjects must be 0-500")
    private Double totalScore; // Wrapper Double

    @Column(name = "percentage")
    @GradeRange(min = 0.0, max = 100.0, message = "Percentage must be between 0% and 100%")
    private Double percentage; // Wrapper Double

    @Column(name = "gpa")
    @GradeRange(min = 0.0, max = 4.0, message = "GPA must be between 0.0 and 4.0")
    private Double gpa; // Wrapper Double

    @Column(name = "final_grade", length = 10)
    private Grade finalGrade; // Enumeration

    @Column(name = "academic_standing", length = 50)
    private AcademicStanding academicStanding; // Enumeration

    private List<StudentMark> marksList = new ArrayList<>(); // Collection of subject marks

    public Student() {}

    public Student(String rollNumber, String fullName, String department, Integer semester) {
        this.rollNumber = rollNumber;
        this.fullName = fullName;
        this.department = department;
        this.semester = semester;
    }

    /**
     * Business method to calculate student grades and academic standings.
     * Annotated with @Auditable to demonstrate method-level annotation processing.
     */
    @Auditable(action = "CALCULATE_GRADES", level = "INFO", logToDatabase = true)
    public void calculateGrades() {
        if (marksList == null || marksList.isEmpty()) {
            this.totalScore = 0.0;
            this.percentage = 0.0;
            this.gpa = 0.0;
            this.finalGrade = Grade.F;
            this.academicStanding = AcademicStanding.PROBATION;
            return;
        }

        // AUTOBOXING / UNBOXING IN ACTION:
        // 'sum' is a primitive double.
        double sum = 0.0;
        double weightedGpaSum = 0.0;
        int totalCredits = 0;
        boolean hasFailedAnySubject = false;

        for (StudentMark mark : marksList) {
            // mark.getTotalMarks() returns Double (Wrapper).
            // Unboxing automatically extracts the primitive double value to add into 'sum'.
            if (mark.getTotalMarks() != null) {
                sum += mark.getTotalMarks(); // UNBOXING
            }

            if (mark.getGrade() == Grade.F) {
                hasFailedAnySubject = true;
            }

            Subject subj = mark.getSubject();
            int credits = (subj != null) ? subj.getCredits() : 3;
            totalCredits += credits;

            double subjectGradePoint = (mark.getGrade() != null) ? mark.getGrade().getGradePoint() : 0.0;
            weightedGpaSum += (subjectGradePoint * credits);
        }

        // AUTOBOXING: Primitive calculation result is automatically boxed into 'Double totalScore'.
        this.totalScore = sum; 

        // AUTOBOXING: Average percentage calculation
        int subjectCount = marksList.size();
        double calcPercentage = (subjectCount > 0) ? (sum / (subjectCount * 100.0)) * 100.0 : 0.0;
        this.percentage = Math.round(calcPercentage * 100.0) / 100.0;

        // GPA Calculation
        double calcGpa = (totalCredits > 0) ? (weightedGpaSum / totalCredits) : 0.0;
        this.gpa = Math.round(calcGpa * 100.0) / 100.0;

        // Determine Final Grade based on percentage (or failed subject rule)
        if (hasFailedAnySubject) {
            this.finalGrade = Grade.F;
            this.academicStanding = AcademicStanding.PROBATION;
        } else {
            // Enums & Unboxing:
            // percentage wrapper is automatically unboxed inside Grade.fromPercentage
            this.finalGrade = Grade.fromPercentage(this.percentage);
            this.academicStanding = AcademicStanding.fromGpa(this.gpa);
        }
    }

    /**
     * Demonstrates @Deprecated built-in annotation.
     * Legacy evaluation based on fixed 60% first class rule.
     */
    @Deprecated(since = "2.0", forRemoval = false)
    public String calculateLegacyDivision() {
        if (percentage == null) return "N/A";
        // Unboxing percentage in comparison
        if (percentage >= 70.0) return "First Class with Distinction (Legacy)";
        if (percentage >= 60.0) return "First Class (Legacy)";
        if (percentage >= 50.0) return "Second Class (Legacy)";
        return "Pass / Fail (Legacy)";
    }

    /**
     * Demonstrates @SuppressWarnings built-in annotation.
     */
    @SuppressWarnings("deprecation")
    public String getLegacyDivisionSafe() {
        return calculateLegacyDivision();
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public Double getGpa() {
        return gpa;
    }

    public void setGpa(Double gpa) {
        this.gpa = gpa;
    }

    public Grade getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(Grade finalGrade) {
        this.finalGrade = finalGrade;
    }

    public AcademicStanding getAcademicStanding() {
        return academicStanding;
    }

    public void setAcademicStanding(AcademicStanding academicStanding) {
        this.academicStanding = academicStanding;
    }

    public List<StudentMark> getMarksList() {
        return marksList;
    }

    public void setMarksList(List<StudentMark> marksList) {
        this.marksList = marksList;
    }

    public void addMark(StudentMark mark) {
        if (this.marksList == null) {
            this.marksList = new ArrayList<>();
        }
        this.marksList.add(mark);
    }

    /**
     * Demonstrates @Override built-in annotation.
     */
    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", rollNumber='" + rollNumber + '\'' +
                ", fullName='" + fullName + '\'' +
                ", department='" + department + '\'' +
                ", finalGrade=" + finalGrade +
                ", percentage=" + percentage +
                '}';
    }
}

