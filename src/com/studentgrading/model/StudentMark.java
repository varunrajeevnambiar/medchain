package com.studentgrading.model;

import com.studentgrading.annotations.Column;
import com.studentgrading.annotations.Entity;
import com.studentgrading.annotations.GradeRange;
import com.studentgrading.annotations.Id;
import com.studentgrading.annotations.Table;

/**
 * Model representing a student's marks in a particular subject.
 * Strongly demonstrates:
 * 1. Autoboxing & Unboxing (Wrapper Double/Integer conversions with primitives).
 * 2. Custom Annotations (@Table, @Column, @GradeRange).
 */
@Entity
@Table(name = "student_marks")
public class StudentMark {

    @Id
    @Column(name = "id")
    private Integer id; // Wrapper Integer (Autoboxed from int)

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "subject_code", nullable = false, length = 20)
    private Subject subject; // Enum

    @Column(name = "internal_marks")
    @GradeRange(min = 0, max = 40, message = "Internal marks must be 0-40")
    private Double internalMarks; // Wrapper Double

    @Column(name = "external_marks")
    @GradeRange(min = 0, max = 60, message = "External marks must be 0-60")
    private Double externalMarks; // Wrapper Double

    @Column(name = "total_marks")
    @GradeRange(min = 0, max = 100, message = "Total marks must be 0-100")
    private Double totalMarks; // Wrapper Double

    private Grade grade; // Enum

    public StudentMark() {}

    public StudentMark(Integer studentId, Subject subject, double internal, double external) {
        this.studentId = studentId;
        this.subject = subject;
        
        // AUTOBOXING DEMONSTRATION:
        // 'internal' (primitive double) is automatically boxed into 'Double' object.
        this.internalMarks = internal; 
        this.externalMarks = external; 

        // UNBOXING & AUTOBOXING COMBINED:
        // 'internalMarks' and 'externalMarks' (Double wrappers) are unboxed to primitives
        // to compute (+), then the result primitive double is autoboxed back into 'totalMarks' (Double).
        this.totalMarks = this.internalMarks + this.externalMarks;

        // Enum lookup using the computed percentage
        this.grade = Grade.fromPercentage(this.totalMarks);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Double getInternalMarks() {
        return internalMarks;
    }

    public void setInternalMarks(Double internalMarks) {
        this.internalMarks = internalMarks;
        recalculate();
    }

    public Double getExternalMarks() {
        return externalMarks;
    }

    public void setExternalMarks(Double externalMarks) {
        this.externalMarks = externalMarks;
        recalculate();
    }

    public Double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Double totalMarks) {
        this.totalMarks = totalMarks;
        this.grade = Grade.fromPercentage(totalMarks);
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    private void recalculate() {
        double in = (this.internalMarks != null) ? this.internalMarks : 0.0; // Unboxing
        double ex = (this.externalMarks != null) ? this.externalMarks : 0.0; // Unboxing
        this.totalMarks = in + ex; // Primitive sum autoboxed into Double
        this.grade = Grade.fromPercentage(this.totalMarks);
    }

    @Override
    public String toString() {
        return "StudentMark{" +
                "subject=" + (subject != null ? subject.getTitle() : "N/A") +
                ", internal=" + internalMarks +
                ", external=" + externalMarks +
                ", total=" + totalMarks +
                ", grade=" + (grade != null ? grade.getLetter() : "N/A") +
                '}';
    }
}

