package com.medchain.web;

import com.medchain.blockchain.Block;
import com.medchain.blockchain.Transaction;
import com.medchain.contract.AccessControlContract;
import com.medchain.model.*;
import com.medchain.network.Node;
import com.medchain.service.AuditService;
import com.medchain.service.DoctorService;
import com.medchain.service.PatientService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

public class MedChainWebServer {
    private final Node node;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AuditService auditService;
    private HttpServer server;
    private final int port;

    public MedChainWebServer(Node node, int port) {
        this.node = node;
        this.port = port;
        AccessControlContract contract = node.getContract();
        this.patientService = new PatientService(contract);
        this.doctorService = new DoctorService(contract);
        this.auditService = new AuditService(contract);
    }

    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        server.createContext("/status", this::handleStatus);
        server.createContext("/patients/register", this::handlePatientRegister);
        server.createContext("/doctors/register", this::handleDoctorRegister);
        server.createContext("/records/add", this::handleAddRecord);
        server.createContext("/access/grant", this::handleGrantAccess);
        server.createContext("/access/revoke", this::handleRevokeAccess);
        server.createContext("/records/", this::handleGetRecords);
        server.createContext("/grants/", this::handleGetGrants);
        server.createContext("/audit/", this::handleGetAudit);
        server.createContext("/blockchain/chain", this::handleGetChain);
        server.createContext("/mine", this::handleMine);

        server.start();
        System.out.println("  [WebServer] MedChain HTTP server started on port " + port);
        System.out.println("  [WebServer] Endpoints available:");
        System.out.println("    GET  http://localhost:" + port + "/status");
        System.out.println("    POST http://localhost:" + port + "/patients/register");
        System.out.println("    POST http://localhost:" + port + "/doctors/register");
        System.out.println("    POST http://localhost:" + port + "/records/add");
        System.out.println("    POST http://localhost:" + port + "/access/grant");
        System.out.println("    POST http://localhost:" + port + "/access/revoke");
        System.out.println("    GET  http://localhost:" + port + "/records/{patientId}");
        System.out.println("    GET  http://localhost:" + port + "/grants/{patientId}");
        System.out.println("    GET  http://localhost:" + port + "/audit/{patientId}");
        System.out.println("    GET  http://localhost:" + port + "/blockchain/chain");
        System.out.println("    POST http://localhost:" + port + "/mine");
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("  [WebServer] Server stopped.");
        }
    }

    // ─── Handlers ──────────────────────────────────────────────────────────────

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        sendResponse(exchange, 200, node.getStatus());
    }

    private void handlePatientRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            String body = readBody(exchange);
            String name = extractJsonString(body, "name");
            String dob = extractJsonString(body, "dateOfBirth");
            String blood = extractJsonString(body, "bloodType");
            if (name == null || name.isEmpty()) name = "Patient";
            if (dob == null || dob.isEmpty()) dob = "1990-01-01";
            if (blood == null || blood.isEmpty()) blood = "O+";

            Patient p = patientService.registerPatient(name, dob, blood);
            String resp = "{\n" +
                    "  \"success\": true,\n" +
                    "  \"patient\": " + p.toJson() + "\n" +
                    "}";
            sendResponse(exchange, 201, resp);
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleDoctorRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            String body = readBody(exchange);
            String name = extractJsonString(body, "name");
            String spec = extractJsonString(body, "specialization");
            String lic = extractJsonString(body, "licenseNumber");
            if (name == null) name = "Doctor";
            if (spec == null) spec = "General";
            if (lic == null) lic = "LIC-000";

            Doctor d = doctorService.registerDoctor(name, spec, lic);
            String resp = "{\n" +
                    "  \"success\": true,\n" +
                    "  \"doctor\": " + d.toJson() + "\n" +
                    "}";
            sendResponse(exchange, 201, resp);
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleAddRecord(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        sendResponse(exchange, 400,
                "{\"error\":\"Patient private key required - use PatientService directly for secure operations\"}");
    }

    private void handleGrantAccess(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        sendResponse(exchange, 400,
                "{\"error\":\"Patient private key required - use PatientService directly for secure operations\"}");
    }

    private void handleRevokeAccess(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        sendResponse(exchange, 400,
                "{\"error\":\"Patient private key required - use PatientService directly for secure operations\"}");
    }

    private void handleGetRecords(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 3) {
            sendResponse(exchange, 400, "{\"error\":\"Missing patientId\"}");
            return;
        }
        String patientId = parts[parts.length - 1];
        List<MedicalRecord> recs = node.getDataStore().getRecordsByPatient(patientId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < recs.size(); i++) {
            sb.append(recs.get(i).toJson());
            if (i < recs.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendResponse(exchange, 200, sb.toString());
    }

    private void handleGetGrants(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 3) {
            sendResponse(exchange, 400, "{\"error\":\"Missing patientId\"}");
            return;
        }
        String patientId = parts[parts.length - 1];
        List<AccessGrant> grants = node.getContract().getActiveGrants(patientId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < grants.size(); i++) {
            sb.append(grants.get(i).toJson());
            if (i < grants.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendResponse(exchange, 200, sb.toString());
    }

    private void handleGetAudit(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 3) {
            sendResponse(exchange, 400, "{\"error\":\"Missing patientId\"}");
            return;
        }
        String patientId = parts[parts.length - 1];
        String report = auditService.printAuditReport(patientId);
        sendResponse(exchange, 200, "{\"report\": \"" + escape(report) + "\"}");
    }

    private void handleGetChain(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        sendResponse(exchange, 200, node.getBlockchain().toJson());
    }

    private void handleMine(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            Block block = node.getBlockchain().minePendingTransactions("web-miner");
            if (block == null) {
                sendResponse(exchange, 200, "{\"message\":\"No pending transactions\"}");
            } else {
                sendResponse(exchange, 200, block.toJson());
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return null;
        int q1 = json.indexOf("\"", colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf("\"", q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private String escape(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
