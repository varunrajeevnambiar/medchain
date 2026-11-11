package com.studentgrading.model;

/**
 * Enumeration representing Academic Course Subjects.
 * Demonstrates: Enums with custom fields, credits, and subject codes.
 */
public enum Subject {
    JAVA_PROGRAMMING("CS301", "Advanced Java Programming", 4, 100),
    DATA_STRUCTURES("CS302", "Data Structures & Algorithms", 4, 100),
    DATABASE_SYSTEMS("CS303", "Database Management Systems", 3, 100),
    WEB_TECHNOLOGIES("CS304", "Web Technologies & Services", 3, 100),
    COMPUTER_NETWORKS("CS305", "Computer Networks & Security", 3, 100);

    private final String code;
    private final String title;
    private final int credits;
    private final int maxMarks;

    Subject(String code, String title, int credits, int maxMarks) {
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.maxMarks = maxMarks;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public int getCredits() {
        return credits;
    }

    public int getMaxMarks() {
        return maxMarks;
    }

    public static Subject fromCodeOrName(String identifier) {
        if (identifier == null) return null;
        for (Subject s : Subject.values()) {
            if (s.code.equalsIgnoreCase(identifier) ||
                s.name().equalsIgnoreCase(identifier) ||
                s.title.equalsIgnoreCase(identifier)) {
                return s;
            }
        }
        return null;
    }
}

