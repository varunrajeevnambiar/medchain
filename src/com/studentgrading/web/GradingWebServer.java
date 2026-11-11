package com.studentgrading.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.studentgrading.annotations.Author;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Web Server for Student Grading System.
 * Built using Java standard library HttpServer (no external application server required).
 * Serves modern interactive UI and REST API endpoints.
 */
@Author(
    name = "Student Developer",
    version = "1.0",
    institution = "University Department of Computer Applications",
    description = "Embedded HTTP Web Server and REST API controller"
)
public class GradingWebServer {

    private static final int PORT = 8080;
    private final StudentDAO studentDAO = new StudentDAO();
    private final GradeCalculatorService calculatorService = new GradeCalculatorService();
    private final AutoboxingDemoService autoboxingService = new AutoboxingDemoService();
    private final AnnotationProcessor annotationProcessor = new AnnotationProcessor();

    public static void main(String[] args) throws IOException {
        // 1. Initialize JDBC database and seed pre-inserted data
        System.out.println("===============================================================");
        System.out.println("   STUDENT GRADING SYSTEM (Enums, Autoboxing, Annotations)   ");
        System.out.println("===============================================================");
        System.out.println("Connecting to JDBC Database...");
        DatabaseManager.initializeDatabase();

        // 2. Start Web Server
        GradingWebServer server = new GradingWebServer();
        server.start(PORT);
    }

    public void start(int port) throws IOException {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            // If 8080 is occupied, try 8085
            int fallbackPort = 8085;
            System.out.println("Port " + port + " busy. Falling back to port " + fallbackPort);
            server = HttpServer.create(new InetSocketAddress(fallbackPort), 0);
            port = fallbackPort;
        }

        // Static files handler
        server.createContext("/", new StaticFileHandler());

        // REST API routes
        server.createContext("/api/students", new StudentsApiHandler());
        server.createContext("/api/stats", new StatsApiHandler());
        server.createContext("/api/demo/enums", new EnumsDemoHandler());
        server.createContext("/api/demo/autoboxing", new AutoboxingDemoHandler());
        server.createContext("/api/demo/annotations", new AnnotationsDemoHandler());
        server.createContext("/api/db/info", new DatabaseInfoHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("Web Server successfully running at:");
        System.out.println(">>> http://localhost:" + port + " <<<");
        System.out.println("Ready to serve requests and demonstrate Java core concepts!");
        System.out.println("===============================================================");
    }

    // ==========================================
    // HTTP HANDLERS
    // ==========================================

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File("web" + path);
            if (!file.exists() || file.isDirectory()) {
                file = new File("web/index.html");
            }

            String contentType = "text/html; charset=UTF-8";
            if (path.endsWith(".css")) {
                contentType = "text/css; charset=UTF-8";
            } else if (path.endsWith(".js")) {
                contentType = "application/javascript; charset=UTF-8";
            } else if (path.endsWith(".png")) {
                contentType = "image/png";
            } else if (path.endsWith(".ico")) {
                contentType = "image/x-icon";
            }

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fs = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = fs.read(buffer)) != -1) {
                    os.write(buffer, 0, count);
                }
            }
        }
    }

    private class StudentsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                String path = exchange.getRequestURI().getPath();
                if (path.startsWith("/api/students/")) {
                    try {
                        int id = Integer.parseInt(path.substring("/api/students/".length()));
                        Student student = studentDAO.getStudentById(id);
                        if (student != null) {
                            sendJsonResponse(exchange, 200, studentToJson(student));
                        } else {
                            sendJsonResponse(exchange, 404, "{\"error\": \"Student not found\"}");
                        }
                    } catch (NumberFormatException e) {
                        sendJsonResponse(exchange, 400, "{\"error\": \"Invalid student ID\"}");
                    }
                } else {
                    List<Student> students = studentDAO.getAllStudents();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < students.size(); i++) {
                        json.append(studentToJson(students.get(i)));
                        if (i < students.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                // Parse request body
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseFormOrJson(body);

                try {
                    String roll = params.getOrDefault("rollNumber", "STU" + System.currentTimeMillis() % 10000);
                    String name = params.getOrDefault("fullName", "New Student");
                    String dept = params.getOrDefault("department", "Computer Science");
                    int sem = Integer.parseInt(params.getOrDefault("semester", "6"));

                    Student s = new Student(roll, name, dept, sem);
                    List<StudentMark> marks = new ArrayList<>();

                    for (Subject subj : Subject.values()) {
                        String inKey = "internal_" + subj.getCode();
                        String exKey = "external_" + subj.getCode();
                        double internal = Double.parseDouble(params.getOrDefault(inKey, "30.0"));
                        double external = Double.parseDouble(params.getOrDefault(exKey, "45.0"));
                        marks.add(new StudentMark(null, subj, internal, external));
                    }

                    boolean ok = studentDAO.insertStudentWithMarks(s, marks);
                    if (ok) {
                        sendJsonResponse(exchange, 201, "{\"success\": true, \"message\": \"Student created successfully\", \"studentId\": " + s.getId() + "}");
                    } else {
                        sendJsonResponse(exchange, 500, "{\"error\": \"Database insertion failed (Check for duplicate roll number)\"}");
                    }
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"error\": \"Invalid input: " + e.getMessage() + "\"}");
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                String path = exchange.getRequestURI().getPath();
                if (path.startsWith("/api/students/")) {
                    try {
                        int id = Integer.parseInt(path.substring("/api/students/".length()));
                        boolean ok = studentDAO.deleteStudent(id);
                        if (ok) {
                            sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Student deleted\"}");
                        } else {
                            sendJsonResponse(exchange, 404, "{\"error\": \"Student not found or deletion failed\"}");
                        }
                    } catch (Exception e) {
                        sendJsonResponse(exchange, 400, "{\"error\": \"Invalid request\"}");
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class StatsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            List<Student> students = studentDAO.getAllStudents();
            Map<String, Object> stats = calculatorService.computeClassStatistics(students);
            sendJsonResponse(exchange, 200, mapToJson(stats));
        }
    }

    private class EnumsDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            StringBuilder json = new StringBuilder("{");

            // Grades Enum
            json.append("\"grades\": [");
            Grade[] grades = Grade.values();
            for (int i = 0; i < grades.length; i++) {
                Grade g = grades[i];
                json.append(String.format("{\"enumName\":\"%s\",\"letter\":\"%s\",\"min\":%.1f,\"max\":%.1f,\"gpa\":%.1f,\"description\":\"%s\",\"color\":\"%s\",\"isPassing\":%b}",
                        g.name(), g.getLetter(), g.getMinScore(), g.getMaxScore(), g.getGradePoint(), g.getDescription(), g.getBadgeColor(), g.isPassing()));
                if (i < grades.length - 1) json.append(",");
            }
            json.append("],");

            // Subjects Enum
            json.append("\"subjects\": [");
            Subject[] subjects = Subject.values();
            for (int i = 0; i < subjects.length; i++) {
                Subject s = subjects[i];
                json.append(String.format("{\"code\":\"%s\",\"title\":\"%s\",\"credits\":%d,\"maxMarks\":%d}",
                        s.getCode(), s.getTitle(), s.getCredits(), s.getMaxMarks()));
                if (i < subjects.length - 1) json.append(",");
            }
            json.append("],");

            // Academic Standing Enum
            json.append("\"academicStandings\": [");
            AcademicStanding[] standings = AcademicStanding.values();
            for (int i = 0; i < standings.length; i++) {
                AcademicStanding as = standings[i];
                json.append(String.format("{\"name\":\"%s\",\"label\":\"%s\",\"minGpa\":%.2f,\"remark\":\"%s\"}",
                        as.name(), as.getLabel(), as.getMinGpa(), as.getRemark()));
                if (i < standings.length - 1) json.append(",");
            }
            json.append("]");

            json.append("}");
            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    private class AutoboxingDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            Map<String, Object> demos = autoboxingService.runAutoboxingDemos();
            sendJsonResponse(exchange, 200, mapToJson(demos));
        }
    }

    private class AnnotationsDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            Map<String, Object> reportStudent = annotationProcessor.inspectClass(Student.class);
            Map<String, Object> reportMarks = annotationProcessor.inspectClass(StudentMark.class);

            Map<String, Object> fullReport = new HashMap<>();
            fullReport.put("studentEntityReport", reportStudent);
            fullReport.put("studentMarkEntityReport", reportMarks);

            // Also test live validation
            Student sampleStudent = new Student();
            sampleStudent.setPercentage(125.0); // Invalid score violating @GradeRange(0, 100)
            sampleStudent.setGpa(4.8); // Invalid violating @GradeRange(0, 4)
            List<String> violations = annotationProcessor.validateGradeRanges(sampleStudent);
            fullReport.put("sampleValidationViolations", violations);

            sendJsonResponse(exchange, 200, mapToJson(fullReport));
        }
    }

    private class DatabaseInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            List<Student> students = studentDAO.getAllStudents();
            Map<String, Object> info = new HashMap<>();
            info.put("status", "CONNECTED");
            info.put("driver", DatabaseManager.getDbDriver());
            info.put("url", DatabaseManager.getDbUrl());
            info.put("preInsertedCount", students.size());
            info.put("tables", new String[]{"students", "student_marks"});
            info.put("message", "Database contains already-inserted student records with marks across all enum categories.");
            sendJsonResponse(exchange, 200, mapToJson(info));
        }
    }

    // ==========================================
    // UTILITIES
    // ==========================================

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String studentToJson(Student s) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(s.getId()).append(",");
        sb.append("\"rollNumber\":\"").append(escape(s.getRollNumber())).append("\",");
        sb.append("\"fullName\":\"").append(escape(s.getFullName())).append("\",");
        sb.append("\"department\":\"").append(escape(s.getDepartment())).append("\",");
        sb.append("\"semester\":").append(s.getSemester()).append(",");
        sb.append("\"totalScore\":").append(s.getTotalScore()).append(",");
        sb.append("\"percentage\":").append(s.getPercentage()).append(",");
        sb.append("\"gpa\":").append(s.getGpa()).append(",");

        // Enum serialization
        Grade g = s.getFinalGrade();
        if (g != null) {
            sb.append("\"finalGrade\":{")
              .append("\"letter\":\"").append(g.getLetter()).append("\",")
              .append("\"description\":\"").append(escape(g.getDescription())).append("\",")
              .append("\"color\":\"").append(g.getBadgeColor()).append("\",")
              .append("\"isPassing\":").append(g.isPassing())
              .append("},");
        } else {
            sb.append("\"finalGrade\":null,");
        }

        AcademicStanding as = s.getAcademicStanding();
        if (as != null) {
            sb.append("\"academicStanding\":{")
              .append("\"label\":\"").append(escape(as.getLabel())).append("\",")
              .append("\"remark\":\"").append(escape(as.getRemark())).append("\"")
              .append("},");
        } else {
            sb.append("\"academicStanding\":null,");
        }

        // Marks list
        sb.append("\"marks\":[");
        List<StudentMark> marks = s.getMarksList();
        if (marks != null) {
            for (int i = 0; i < marks.size(); i++) {
                StudentMark m = marks.get(i);
                sb.append("{");
                sb.append("\"subjectCode\":\"").append(m.getSubject() != null ? m.getSubject().getCode() : "").append("\",");
                sb.append("\"subjectTitle\":\"").append(m.getSubject() != null ? escape(m.getSubject().getTitle()) : "").append("\",");
                sb.append("\"credits\":").append(m.getSubject() != null ? m.getSubject().getCredits() : 3).append(",");
                sb.append("\"internalMarks\":").append(m.getInternalMarks()).append(",");
                sb.append("\"externalMarks\":").append(m.getExternalMarks()).append(",");
                sb.append("\"totalMarks\":").append(m.getTotalMarks()).append(",");
                sb.append("\"grade\":\"").append(m.getGrade() != null ? m.getGrade().getLetter() : "N/A").append("\"");
                sb.append("}");
                if (i < marks.size() - 1) sb.append(",");
            }
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count > 0) sb.append(",");
            sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
            sb.append(objectToJson(entry.getValue()));
            count++;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String objectToJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Map) return mapToJson((Map<?, ?>) obj);
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(objectToJson(list.get(i)));
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            Object[] arr = (Object[]) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                sb.append(objectToJson(arr[i]));
                if (i < arr.length - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(obj.toString()) + "\"";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static Map<String, String> parseFormOrJson(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        body = body.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            // Simple JSON parser
            String inner = body.substring(1, body.length() - 1).trim();
            String[] pairs = inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replaceAll("^\"|\"$", "");
                    String v = kv[1].trim().replaceAll("^\"|\"$", "");
                    map.put(k, v);
                }
            }
        } else {
            // URL Encoded form
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    try {
                        String k = java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                        String v = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        map.put(k, v);
                    } catch (Exception ignored) {}
                }
            }
        }
        return map;
    }
}

