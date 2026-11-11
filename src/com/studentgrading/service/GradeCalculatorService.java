package com.studentgrading.service;

import com.studentgrading.model.AcademicStanding;
import com.studentgrading.model.Grade;
import com.studentgrading.model.Student;
import com.studentgrading.model.Subject;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service handling Grade Aggregations and Statistical Analysis.
 * Demonstrates:
 * 1. EnumMap<Grade, Integer> usage (specialized high-performance map for Enums).
 * 2. Autoboxing when updating counts and averages.
 */
public class GradeCalculatorService {

    /**
     * Computes batch statistics for a list of students.
     */
    public Map<String, Object> computeClassStatistics(List<Student> students) {
        Map<String, Object> stats = new HashMap<>();

        if (students == null || students.isEmpty()) {
            stats.put("totalStudents", 0);
            stats.put("classAverage", 0.0);
            stats.put("highestScore", 0.0);
            stats.put("lowestScore", 0.0);
            stats.put("passRate", 0.0);
            return stats;
        }

        // Wrapper autoboxing into stats
        stats.put("totalStudents", students.size());

        double totalMarksSum = 0.0;
        double highest = Double.MIN_VALUE;
        double lowest = Double.MAX_VALUE;
        int passCount = 0;

        // Specialized EnumMap keyed by Grade enum
        Map<Grade, Integer> gradeDistribution = new EnumMap<>(Grade.class);
        for (Grade g : Grade.values()) {
            gradeDistribution.put(g, 0); // Autoboxing 0 -> Integer
        }

        for (Student s : students) {
            // Unboxing percentage Double wrapper
            Double pctWrapper = s.getPercentage();
            double pct = (pctWrapper != null) ? pctWrapper : 0.0; // Defensive unboxing

            totalMarksSum += pct;
            if (pct > highest) highest = pct;
            if (pct < lowest) lowest = pct;

            Grade finalGrade = s.getFinalGrade();
            if (finalGrade != null) {
                // Autoboxing: count is incremented via unboxing and autoboxed back
                int currentCount = gradeDistribution.get(finalGrade); // Unboxing
                gradeDistribution.put(finalGrade, currentCount + 1); // Autoboxing
                if (finalGrade.isPassing()) {
                    passCount++;
                }
            }
        }

        double classAvg = totalMarksSum / students.size();
        double passRate = (students.size() > 0) ? ((double) passCount / students.size()) * 100.0 : 0.0;

        stats.put("classAverage", Math.round(classAvg * 100.0) / 100.0);
        stats.put("highestScore", Math.round(highest * 100.0) / 100.0);
        stats.put("lowestScore", (lowest == Double.MAX_VALUE) ? 0.0 : Math.round(lowest * 100.0) / 100.0);
        stats.put("passRate", Math.round(passRate * 100.0) / 100.0);
        stats.put("passCount", passCount);
        stats.put("failCount", students.size() - passCount);

        // Convert enum map to simple string keys for JSON serialization
        Map<String, Integer> distOutput = new HashMap<>();
        for (Map.Entry<Grade, Integer> entry : gradeDistribution.entrySet()) {
            distOutput.put(entry.getKey().getLetter(), entry.getValue());
        }
        stats.put("gradeDistribution", distOutput);

        return stats;
    }
}

