package com.studentgrading;

import com.studentgrading.annotations.Auditable;
import com.studentgrading.annotations.Author;
import com.studentgrading.annotations.Column;
import com.studentgrading.annotations.Entity;
import com.studentgrading.annotations.GradeRange;
import com.studentgrading.annotations.Id;
import com.studentgrading.annotations.Table;
import com.studentgrading.dao.DatabaseManager;
import com.studentgrading.dao.StudentDAO;
import com.studentgrading.model.AcademicStanding;
import com.studentgrading.model.Grade;
import com.studentgrading.model.Student;
import com.studentgrading.model.StudentMark;
import com.studentgrading.model.Subject;
import com.studentgrading.service.AnnotationProcessor;
import com.studentgrading.service.AutoboxingDemoService;
import com.studentgrading.service.GradeCalculatorService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Standalone Console Output Generator for Student Grading System.
 * Produces comprehensive, perfectly formatted academic lab demonstration output
 * covering JDBC Pre-inserted records, Enumerations, Autoboxing/Unboxing, and Annotations.
 */
public class DemoConsoleOutput {

    public static void main(String[] args) {
        System.out.println("========================================================================================================");
        System.out.println("                            STUDENT GRADING SYSTEM - ACADEMIC LAB OUTPUT                                ");
        System.out.println("                    Demonstrating: Enumerations, Autoboxing, Annotations & JDBC                         ");
        System.out.println("========================================================================================================");

        // -------------------------------------------------------------------------
        // PART 1: JDBC DATABASE CONNECTION & PRE-INSERTED RECORDS
        // -------------------------------------------------------------------------
        System.out.println("\n[PART 1] JDBC DATABASE CONNECTION & PRE-INSERTED RECORDS");
        System.out.println("--------------------------------------------------------------------------------------------------------");
        System.out.println("Connecting to JDBC Database...");
        DatabaseManager.initializeDatabase();
        System.out.println("Database URL          : " + DatabaseManager.getDbUrl());
        System.out.println("JDBC Driver           : " + DatabaseManager.getDbDriver());

        StudentDAO dao = new StudentDAO();
        List<Student> students = dao.getAllStudents();
        System.out.println("Pre-inserted Records  : " + students.size() + " Students found loaded in database\n");

        System.out.printf("%-4s %-12s %-18s %-32s %-5s %-7s %-7s %-5s %-6s %-28s%n",
                "ID", "ROLL NO", "STUDENT NAME", "DEPARTMENT", "SEM", "TOTAL", "PCT(%)", "GPA", "GRADE", "ACADEMIC STANDING");
        System.out.println("--------------------------------------------------------------------------------------------------------");

        for (Student s : students) {
            String gradeLetter = (s.getFinalGrade() != null) ? s.getFinalGrade().getLetter() : "N/A";
            String standing = (s.getAcademicStanding() != null) ? s.getAcademicStanding().getLabel() : "N/A";
            System.out.printf("%-4d %-12s %-18s %-32s %-5d %-7.1f %-6.1f%% %-5.2f %-6s %-28s%n",
                    s.getId(), s.getRollNumber(), s.getFullName(), s.getDepartment(),
                    s.getSemester(), s.getTotalScore(), s.getPercentage(), s.getGpa(),
                    gradeLetter, standing);
        }
        System.out.println("--------------------------------------------------------------------------------------------------------");

        // Subject Breakdown for top student
        if (!students.isEmpty()) {
            Student top = students.get(0);
            System.out.println("\nDetailed Subject Marks Breakdown for Top Student (" + top.getRollNumber() + " - " + top.getFullName() + "):");
            for (StudentMark m : top.getMarksList()) {
                String subjCode = (m.getSubject() != null) ? m.getSubject().getCode() : "N/A";
                String subjName = (m.getSubject() != null) ? m.getSubject().getTitle() : "N/A";
                String grade = (m.getGrade() != null) ? m.getGrade().getLetter() : "N/A";
                System.out.printf("  * %-6s (%-30s): Internal=%4.1f/40, External=%4.1f/60, Total=%5.1f/100 -> Grade: %s%n",
                        subjCode, subjName, m.getInternalMarks(), m.getExternalMarks(), m.getTotalMarks(), grade);
            }
        }

        GradeCalculatorService calcService = new GradeCalculatorService();
        Map<String, Object> stats = calcService.computeClassStatistics(students);
        System.out.println("\nClass Aggregate Statistics:");
        System.out.println("  • Class Average Score : " + stats.get("classAverage") + "%");
        System.out.println("  • Overall Pass Rate   : " + stats.get("passRate") + "% (" + stats.get("passCount") + " passed, " + stats.get("failCount") + " failed)");
        System.out.println("  • Highest Mark Scored : " + stats.get("highestScore") + "%");
        System.out.println("  • Lowest Mark Scored  : " + stats.get("lowestScore") + "%");

        // -------------------------------------------------------------------------
        // PART 2: ENUMERATIONS DEMONSTRATION
        // -------------------------------------------------------------------------
        System.out.println("\n========================================================================================================");
        System.out.println("[PART 2] ENUMERATIONS (ENUMS) DEMONSTRATION");
        System.out.println("--------------------------------------------------------------------------------------------------------");
        System.out.println("1. Grade Enum Definition (Constants, Cutoff Ranges, GPA, Passing State):");
        for (Grade g : Grade.values()) {
            System.out.printf("   - Grade.%-6s => Cutoff: [%5.1f%% - %5.1f%%], GPA: %3.1f, Passing: %-5b, Desc: %s%n",
                    g.name(), g.getMinScore(), g.getMaxScore(), g.getGradePoint(), g.isPassing(), g.getDescription());
        }

        System.out.println("\n2. Live Enum Evaluation via Grade.fromPercentage(Double):");
        double[] testPercentages = {95.5, 83.0, 76.5, 69.0, 55.0, 42.0, 28.5};
        for (double pct : testPercentages) {
            Grade resolved = Grade.fromPercentage(pct); // Unboxing happens internally
            System.out.printf("   * Percentage: %5.1f%% => Resolved Enum: Grade.%-6s [Letter: %-2s, GPA: %.1f, Standing: %s]%n",
                    pct, resolved.name(), resolved.getLetter(), resolved.getGradePoint(), resolved.getDescription());
        }

        System.out.println("\n3. Subject Enum Definition (Course Codes & Credits):");
        for (Subject sub : Subject.values()) {
            System.out.printf("   - Subject.%-17s => Code: %-6s, Title: %-30s, Credits: %d, MaxMarks: %d%n",
                    sub.name(), sub.getCode(), sub.getTitle(), sub.getCredits(), sub.getMaxMarks());
        }

        // -------------------------------------------------------------------------
        // PART 3: AUTOBOXING AND UNBOXING DEMONSTRATION
        // -------------------------------------------------------------------------
        System.out.println("\n========================================================================================================");
        System.out.println("[PART 3] AUTOBOXING AND UNBOXING DEMONSTRATION");
        System.out.println("--------------------------------------------------------------------------------------------------------");

        // 1. Primitive <-> Wrapper
        int primitiveMarks = 88;
        Integer boxedMarks = primitiveMarks; // Autoboxing
        int unboxedMarks = boxedMarks; // Unboxing
        System.out.println("1. Basic Autoboxing & Unboxing Conversion:");
        System.out.println("   * Primitive int value          : " + primitiveMarks);
        System.out.println("   * Autoboxed Integer Object     : " + boxedMarks + " (Class: " + boxedMarks.getClass().getName() + ")");
        System.out.println("   * Unboxed primitive int value  : " + unboxedMarks);

        // 2. Collections Autoboxing
        System.out.println("\n2. Collections Autoboxing & Arithmetic Unboxing:");
        List<Double> marksCollection = new ArrayList<>();
        double[] sampleScores = {92.5, 84.0, 78.5, 95.0, 89.0};
        for (double s : sampleScores) {
            marksCollection.add(s); // Autoboxing: double -> Double wrapper
        }
        System.out.println("   * List<Double> elements (boxed) : " + marksCollection);

        double totalSum = 0.0;
        for (Double markWrapper : marksCollection) {
            totalSum += markWrapper; // Unboxing: markWrapper unboxed to double for '+'
        }
        double avg = totalSum / marksCollection.size();
        System.out.printf("   * Total Sum (unboxed in loop)   : %.2f%n", totalSum);
        System.out.printf("   * Calculated Average Percentage : %.2f%%%n", avg);

        // 3. Wrapper Equality and Caching
        System.out.println("\n3. Wrapper Equality & Caching Behavior (-128 to 127):");
        Integer num1 = 100, num2 = 100;
        Integer num3 = 250, num4 = 250;
        System.out.println("   * Integer(100) == Integer(100)  : " + (num1 == num2) + "  (TRUE: Cached in IntegerCache)");
        System.out.println("   * Integer(250) == Integer(250)  : " + (num3 == num4) + " (FALSE: Distinct heap objects)");
        System.out.println("   * Integer(250).equals(Integer(250)): " + num3.equals(num4) + "  (TRUE: Compares primitive values)");

        // 4. Null Safety in Unboxing
        System.out.println("\n4. Null-Safety in Unboxing:");
        Double nullableScore = null;
        try {
            @SuppressWarnings("unused")
            double unsafe = nullableScore; // Throws NPE!
        } catch (NullPointerException npe) {
            System.out.println("   * Unboxing null wrapper caught  : java.lang.NullPointerException");
        }
        double safeScore = (nullableScore != null) ? nullableScore : 0.0;
        System.out.println("   * Safe unboxing fallback value  : " + safeScore);

        // -------------------------------------------------------------------------
        // PART 4: ANNOTATIONS & REFLECTION ENGINE DEMONSTRATION
        // -------------------------------------------------------------------------
        System.out.println("\n========================================================================================================");
        System.out.println("[PART 4] ANNOTATIONS & RUNTIME REFLECTION DEMONSTRATION");
        System.out.println("--------------------------------------------------------------------------------------------------------");

        Class<Student> studentClass = Student.class;
        System.out.println("1. Class-Level Annotations on Student.class:");
        if (studentClass.isAnnotationPresent(Author.class)) {
            Author author = studentClass.getAnnotation(Author.class);
            System.out.println("   * @Author     => Name: \"" + author.name() + "\", Version: \"" + author.version() +
                    "\", Institution: \"" + author.institution() + "\"");
        }
        if (studentClass.isAnnotationPresent(Entity.class)) {
            Entity ent = studentClass.getAnnotation(Entity.class);
            System.out.println("   * @Entity     => Description: \"" + ent.description() + "\"");
        }
        if (studentClass.isAnnotationPresent(Table.class)) {
            Table tbl = studentClass.getAnnotation(Table.class);
            System.out.println("   * @Table      => Table Name: \"" + tbl.name() + "\"");
        }

        System.out.println("\n2. Field-Level Annotations Discovered via Reflection:");
        for (Field f : studentClass.getDeclaredFields()) {
            StringBuilder sb = new StringBuilder();
            if (f.isAnnotationPresent(Id.class)) sb.append("@Id ");
            if (f.isAnnotationPresent(Column.class)) {
                Column c = f.getAnnotation(Column.class);
                sb.append("@Column(name=\"").append(c.name()).append("\") ");
            }
            if (f.isAnnotationPresent(GradeRange.class)) {
                GradeRange gr = f.getAnnotation(GradeRange.class);
                sb.append("@GradeRange(min=").append(gr.min()).append(", max=").append(gr.max()).append(") ");
            }
            if (sb.length() > 0) {
                System.out.printf("   * Field %-16s (%-7s) => %s%n", f.getName(), f.getType().getSimpleName(), sb.toString());
            }
        }

        System.out.println("\n3. Method-Level Annotations Discovered:");
        for (Method m : studentClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Auditable.class)) {
                Auditable a = m.getAnnotation(Auditable.class);
                System.out.println("   * Method " + m.getName() + "() => @Auditable(action=\"" + a.action() + "\", level=\"" + a.level() + "\")");
            }
            if (m.isAnnotationPresent(Deprecated.class)) {
                Deprecated d = m.getAnnotation(Deprecated.class);
                System.out.println("   * Method " + m.getName() + "() => @Deprecated(since=\"" + d.since() + "\")");
            }
        }

        AnnotationProcessor processor = new AnnotationProcessor();
        Map<String, Object> report = processor.inspectClass(Student.class);
        System.out.println("\n4. Dynamic SQL DDL Generated by Reflection Engine from Annotations:");
        System.out.println(report.get("generatedDDL"));

        // -------------------------------------------------------------------------
        // PART 5: WEB APPLICATION STATUS
        // -------------------------------------------------------------------------
        System.out.println("\n========================================================================================================");
        System.out.println("[PART 5] WEB APPLICATION ACCESSIBILITY");
        System.out.println("--------------------------------------------------------------------------------------------------------");
        System.out.println("Web Dashboard URL     : http://localhost:8080");
        System.out.println("Frontend Technology   : Modern Responsive HTML5 / CSS3 / JavaScript");
        System.out.println("Backend Engine        : Java SE Embedded HttpServer (Pure Java Standard Library)");
        System.out.println("Database Engine       : JDBC with SQLite (Embedded) / MySQL Ready");
        System.out.println("========================================================================================================");
        System.out.println("                               EXECUTION COMPLETED SUCCESSFULLY!                                        ");
        System.out.println("========================================================================================================");
    }
}

