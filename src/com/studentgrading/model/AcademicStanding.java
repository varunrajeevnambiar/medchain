package com.studentgrading.model;

/**
 * Enumeration representing Academic Honors and Standing.
 * Demonstrates: Enums with evaluation logic and classification.
 */
public enum AcademicStanding {
    DISTINCTION("First Class with Distinction", 3.75, "Eligible for Academic Honors / Dean's List"),
    FIRST_CLASS("First Class", 3.0, "Good Academic Standing"),
    SECOND_CLASS("Second Class", 2.0, "Satisfactory Academic Standing"),
    PASS_CLASS("Pass Class", 1.0, "Minimum Academic Requirement Met"),
    PROBATION("Academic Probation", 0.0, "Under Performing - Counseling Recommended");

    private final String label;
    private final double minGpa;
    private final String remark;

    AcademicStanding(String label, double minGpa, String remark) {
        this.label = label;
        this.minGpa = minGpa;
        this.remark = remark;
    }

    public String getLabel() {
        return label;
    }

    public double getMinGpa() {
        return minGpa;
    }

    public String getRemark() {
        return remark;
    }

    /**
     * Determines standing based on GPA.
     * Demonstrates unboxing when comparing Double wrapper to primitive double minGpa.
     */
    public static AcademicStanding fromGpa(Double gpa) {
        if (gpa == null) return PROBATION;
        for (AcademicStanding standing : AcademicStanding.values()) {
            if (gpa >= standing.minGpa) { // Unboxing: gpa wrapper unboxed to primitive
                return standing;
            }
        }
        return PROBATION;
    }
}

