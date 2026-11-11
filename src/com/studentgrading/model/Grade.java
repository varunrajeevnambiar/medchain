package com.studentgrading.model;

/**
 * Enumeration representing Academic Grades with associated properties and utility methods.
 * Demonstrates:
 * 1. Enum declaration with constants.
 * 2. Enum constructor and private instance variables.
 * 3. Custom methods within Enum (fromPercentage, isPassing).
 * 4. Autoboxing/Unboxing integration in business logic.
 */
public enum Grade {
    A_PLUS("A+", 90.0, 100.0, 4.0, "Outstanding / Exemplary", "#10b981"),
    A("A", 80.0, 89.99, 3.7, "Excellent Performance", "#059669"),
    B_PLUS("B+", 75.0, 79.99, 3.3, "Very Good Performance", "#3b82f6"),
    B("B", 65.0, 74.99, 3.0, "Good Performance", "#2563eb"),
    C("C", 50.0, 64.99, 2.0, "Satisfactory / Average", "#f59e0b"),
    D("D", 40.0, 49.99, 1.0, "Pass / Minimum Acceptable", "#ea580c"),
    F("F", 0.0, 39.99, 0.0, "Fail / Below Requirements", "#ef4444");

    private final String letter;
    private final double minScore;
    private final double maxScore;
    private final double gradePoint;
    private final String description;
    private final String badgeColor;

    /**
     * Enum Constructor
     */
    Grade(String letter, double minScore, double maxScore, double gradePoint, String description, String badgeColor) {
        this.letter = letter;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.gradePoint = gradePoint;
        this.description = description;
        this.badgeColor = badgeColor;
    }

    public String getLetter() {
        return letter;
    }

    public double getMinScore() {
        return minScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public double getGradePoint() {
        return gradePoint;
    }

    public String getDescription() {
        return description;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public boolean isPassing() {
        return this != F;
    }

    /**
     * Lookup Grade enum based on numerical percentage.
     * Demonstrates Unboxing: The parameter Double wrapper is automatically
     * unboxed into primitive double during relational comparisons (<, >=).
     *
     * @param percentage Double wrapper object
     * @return Corresponding Grade enum
     */
    public static Grade fromPercentage(Double percentage) {
        if (percentage == null) {
            return F;
        }

        // Unboxing happens automatically here: 'percentage' is a Double wrapper
        // but compared against primitive double bounds (minScore, maxScore).
        double unboxedPercentage = percentage; // Explicit or implicit unboxing

        for (Grade g : Grade.values()) {
            if (unboxedPercentage >= g.minScore && unboxedPercentage <= g.maxScore) {
                return g;
            }
        }
        
        // Edge cases
        if (unboxedPercentage > 100.0) return A_PLUS;
        return F;
    }

    /**
     * Lookup Grade enum by letter string.
     */
    public static Grade fromLetter(String letter) {
        if (letter == null || letter.trim().isEmpty()) {
            return F;
        }
        String clean = letter.trim().toUpperCase();
        for (Grade g : Grade.values()) {
            if (g.letter.equalsIgnoreCase(clean) || g.name().equalsIgnoreCase(clean)) {
                return g;
            }
        }
        return F;
    }

    @Override
    public String toString() {
        return String.format("%s (GPA: %.1f, %s)", letter, gradePoint, description);
    }
}

