package com.medchain;

import com.medchain.blockchain.Block;
import com.medchain.blockchain.Transaction;
import com.medchain.model.*;
import com.medchain.network.Node;
import com.medchain.network.PeerNetwork;
import com.medchain.service.AuditService;
import com.medchain.service.DoctorService;
import com.medchain.service.PatientService;
import com.medchain.web.MedChainWebServer;

import java.util.List;

public class MedChainDemo {

    public static void main(String[] args) throws Exception {

        printBanner();

        // ─── 1. Create Blockchain Node ─────────────────────────────────────────
        section("1. INITIALIZING BLOCKCHAIN NODE");
        Node node = new Node("MedChain-Primary", 2, 10);
        PeerNetwork network = new PeerNetwork("MedChain-TestNet");
        network.addPeer(node);
        System.out.println("  Node: " + node.getNodeName() + " | ID: " + node.getNodeId());
        System.out.println("  Blockchain initialized with difficulty=2, blockSize=10");
        System.out.println("  Genesis block mined.");

        PatientService patientService = new PatientService(node.getContract());
        DoctorService doctorService = new DoctorService(node.getContract());
        AuditService auditService = new AuditService(node.getContract());

        // ─── 2. Register Patients ──────────────────────────────────────────────
        section("2. REGISTERING PATIENTS");
        System.out.println("  Registering Alice...");
        Patient alice = patientService.registerPatient("Alice Johnson", "1990-05-15", "A+");

        System.out.println("  Registering Bob...");
        Patient bob = patientService.registerPatient("Bob Martinez", "1985-11-22", "O-");

        System.out.println("\n  ✓ Alice: " + alice.getPatientId());
        System.out.println("  ✓ Bob:   " + bob.getPatientId());

        // ─── 3. Register Doctors ──────────────────────────────────────────────
        section("3. REGISTERING DOCTORS");
        System.out.println("  Registering Dr. Smith...");
        Doctor drSmith = doctorService.registerDoctor("Dr. Emily Smith", "Cardiology", "LIC-CARD-001");

        System.out.println("  Registering Dr. Jones...");
        Doctor drJones = doctorService.registerDoctor("Dr. Marcus Jones", "Internal Medicine", "LIC-INT-002");

        System.out.println("  Registering Dr. Patel...");
        Doctor drPatel = doctorService.registerDoctor("Dr. Priya Patel", "Neurology", "LIC-NEURO-003");

        System.out.println("\n  ✓ Dr. Smith: " + drSmith.getDoctorId());
        System.out.println("  ✓ Dr. Jones: " + drJones.getDoctorId());
        System.out.println("  ✓ Dr. Patel: " + drPatel.getDoctorId());

        // ─── 4. Add Medical Records for Alice ─────────────────────────────────
        section("4. ADDING MEDICAL RECORDS FOR ALICE");
        System.out.println("  Adding Alice's records...");

        MedicalRecord aliceRecord1 = patientService.addMedicalRecord(alice,
                "DIAGNOSIS",
                "Hypertension Diagnosis",
                "Patient presents with elevated blood pressure readings over 3 months",
                "Essential Hypertension (I10)",
                "Lisinopril 10mg daily, Amlodipine 5mg daily",
                "Monitor BP weekly. Lifestyle modifications recommended.");

        MedicalRecord aliceRecord2 = patientService.addMedicalRecord(alice,
                "LAB_RESULT",
                "Annual Blood Panel",
                "Comprehensive metabolic panel and CBC",
                "Hyperlipidemia (E78.5)",
                "Atorvastatin 20mg daily",
                "Total cholesterol 240mg/dL. Recheck in 3 months.");

        MedicalRecord aliceRecord3 = patientService.addMedicalRecord(alice,
                "IMAGING",
                "Chest X-Ray",
                "Routine chest X-ray examination",
                "No acute cardiopulmonary process",
                null,
                "Heart size normal. Lungs clear.");

        System.out.println("\n  ✓ Record 1: " + aliceRecord1.getRecordId() + " [" + aliceRecord1.getRecordType() + "]");
        System.out.println("  ✓ Record 2: " + aliceRecord2.getRecordId() + " [" + aliceRecord2.getRecordType() + "]");
        System.out.println("  ✓ Record 3: " + aliceRecord3.getRecordId() + " [" + aliceRecord3.getRecordType() + "]");

        // ─── 5. Add Medical Records for Bob ───────────────────────────────────
        section("5. ADDING MEDICAL RECORDS FOR BOB");
        System.out.println("  Adding Bob's records...");

        MedicalRecord bobRecord1 = patientService.addMedicalRecord(bob,
                "DIAGNOSIS",
                "Type 2 Diabetes",
                "Elevated HbA1c levels consistent with Type 2 Diabetes",
                "Type 2 Diabetes Mellitus (E11.9)",
                "Metformin 500mg twice daily",
                "Dietary counseling initiated. HbA1c target < 7%");

        MedicalRecord bobRecord2 = patientService.addMedicalRecord(bob,
                "SURGERY",
                "Appendectomy",
                "Emergency laparoscopic appendectomy",
                "Acute appendicitis (K37)",
                "Amoxicillin-Clavulanate 875mg for 7 days post-op",
                "Procedure successful. No complications. Follow up in 2 weeks.");

        System.out.println("\n  ✓ Record 1: " + bobRecord1.getRecordId() + " [" + bobRecord1.getRecordType() + "]");
        System.out.println("  ✓ Record 2: " + bobRecord2.getRecordId() + " [" + bobRecord2.getRecordType() + "]");

        // ─── 6. Alice grants access ───────────────────────────────────────────
        section("6. ALICE GRANTS ACCESS TO DOCTORS");

        System.out.println("  Alice granting Dr. Smith READ_ONLY access to ALL records (no expiry)...");
        AccessGrant grant1 = patientService.grantAccessWithLevel(
                alice, drSmith.getDoctorId(), null, AccessLevel.READ_ONLY, 0);
        System.out.println("  ✓ Grant ID: " + (grant1 != null ? grant1.getGrantId() : "pending"));

        System.out.println("\n  Alice granting Dr. Jones READ_WRITE access to specific records (30-day expiry)...");
        String[] jonesRecords = {aliceRecord1.getRecordId(), aliceRecord2.getRecordId()};
        AccessGrant grant2 = patientService.grantAccessWithLevel(
                alice, drJones.getDoctorId(), jonesRecords, AccessLevel.READ_WRITE, 30);
        System.out.println("  ✓ Grant ID: " + (grant2 != null ? grant2.getGrantId() : "pending"));

        System.out.println("\n  Bob granting Dr. Patel READ_WRITE access to ALL records...");
        AccessGrant grant3 = patientService.grantAccessWithLevel(
                bob, drPatel.getDoctorId(), null, AccessLevel.READ_WRITE, 0);
        System.out.println("  ✓ Grant ID: " + (grant3 != null ? grant3.getGrantId() : "pending"));

        // ─── 7. Access demonstrations ─────────────────────────────────────────
        section("7. DEMONSTRATING RECORD ACCESS");

        // Dr. Smith reads Alice's records
        System.out.println("  [TEST 1] Dr. Smith reading Alice's hypertension record...");
        try {
            MedicalRecord rec = doctorService.accessPatientRecord(drSmith,
                    alice.getPatientId(), aliceRecord1.getRecordId());
            System.out.println("  ✅ SUCCESS: Dr. Smith accessed record: " + rec.getTitle());
            System.out.println("     Diagnosis: " + rec.getDiagnosis());
            System.out.println("     Prescription: " + rec.getPrescription());
        } catch (Exception e) {
            System.out.println("  ❌ FAILED: " + e.getMessage());
        }

        System.out.println();

        // Dr. Smith reads Alice's lab result
        System.out.println("  [TEST 2] Dr. Smith reading Alice's lab result...");
        try {
            MedicalRecord rec = doctorService.accessPatientRecord(drSmith,
                    alice.getPatientId(), aliceRecord2.getRecordId());
            System.out.println("  ✅ SUCCESS: Dr. Smith accessed record: " + rec.getTitle());
        } catch (Exception e) {
            System.out.println("  ❌ FAILED: " + e.getMessage());
        }

        System.out.println();

        // Dr. Jones (READ_WRITE) adds prescription for Alice
        System.out.println("  [TEST 3] Dr. Jones (READ_WRITE) adding prescription for Alice...");
        try {
            MedicalRecord prescription = doctorService.addPrescription(
                    drJones, alice.getPatientId(),
                    "Blood Pressure Follow-up Prescription",
                    "Increased Lisinopril to 20mg daily",
                    "BP still elevated at 150/95. Increasing dosage.");
            System.out.println("  ✅ SUCCESS: Dr. Jones added prescription: " + prescription.getTitle());
        } catch (Exception e) {
            System.out.println("  ❌ FAILED: " + e.getMessage());
        }

        System.out.println();

        // Dr. Patel tries to access Alice's records (should FAIL)
        System.out.println("  [TEST 4] Dr. Patel trying to access ALICE's records (should be DENIED)...");
        try {
            MedicalRecord rec = doctorService.accessPatientRecord(drPatel,
                    alice.getPatientId(), aliceRecord1.getRecordId());
            System.out.println("  ❌ SECURITY BREACH: Access should have been denied!");
        } catch (SecurityException e) {
            System.out.println("  ✅ ACCESS CORRECTLY DENIED: " + e.getMessage());
        }

        System.out.println();

        // Dr. Smith tries to access Bob's records (should FAIL)
        System.out.println("  [TEST 5] Dr. Smith trying to access BOB's records (should be DENIED)...");
        try {
            MedicalRecord rec = doctorService.accessPatientRecord(drSmith,
                    bob.getPatientId(), bobRecord1.getRecordId());
            System.out.println("  ❌ SECURITY BREACH: Access should have been denied!");
        } catch (SecurityException e) {
            System.out.println("  ✅ ACCESS CORRECTLY DENIED: " + e.getMessage());
        }

        System.out.println();

        // Dr. Patel accesses Bob's record (should SUCCEED)
        System.out.println("  [TEST 6] Dr. Patel reading Bob's diabetes record...");
        try {
            MedicalRecord rec = doctorService.accessPatientRecord(drPatel,
                    bob.getPatientId(), bobRecord1.getRecordId());
            System.out.println("  ✅ SUCCESS: Dr. Patel accessed record: " + rec.getTitle());
            System.out.println("     Diagnosis: " + rec.getDiagnosis());
        } catch (Exception e) {
            System.out.println("  ❌ FAILED: " + e.getMessage());
        }

        // ─── 8. Alice revokes Dr. Smith's access ──────────────────────────────
        section("8. ALICE REVOKES DR. SMITH'S ACCESS");
        System.out.println("  Revoking Dr. Smith's access...");
        boolean revoked = patientService.revokeAccessFromDoctor(alice, drSmith.getDoctorId());
        System.out.println("  ✓ Access revoked: " + revoked);

        System.out.println("\n  [TEST 7] Dr. Smith trying to access Alice's records after revocation...");
        try {
            MedicalRecord rec = doctorService.accessPatientRecord(drSmith,
                    alice.getPatientId(), aliceRecord1.getRecordId());
            System.out.println("  ❌ SECURITY BREACH: Revoked access was not enforced!");
        } catch (SecurityException e) {
            System.out.println("  ✅ ACCESS CORRECTLY DENIED after revocation: " + e.getMessage());
        }

        // ─── 9. Alice views her own records ───────────────────────────────────
        section("9. ALICE VIEWS HER OWN RECORDS");
        List<MedicalRecord> aliceRecords = patientService.viewMyRecords(alice);
        System.out.println("  Alice has " + aliceRecords.size() + " records:");
        for (MedicalRecord r : aliceRecords) {
            System.out.println("    → [" + r.getRecordType() + "] " + r.getTitle());
            if (r.getDiagnosis() != null && !r.getDiagnosis().isEmpty()) {
                System.out.println("      Diagnosis: " + r.getDiagnosis());
            }
        }

        // ─── 10. Mine all pending transactions ────────────────────────────────
        section("10. MINING ALL PENDING TRANSACTIONS");
        int pending = node.getBlockchain().getPendingTransactions().size();
        System.out.println("  Pending transactions: " + pending);
        if (pending > 0) {
            System.out.println("  Mining...");
            Block minedBlock = node.getBlockchain().minePendingTransactions("MedChain-Primary");
            if (minedBlock != null) {
                System.out.println("  ✓ Block #" + minedBlock.getIndex() + " mined!");
                System.out.println("    Hash: " + minedBlock.getHash());
                System.out.println("    Transactions: " + minedBlock.getTransactions().size());
            }
        }

        // ─── 11. Print Blockchain Summary ─────────────────────────────────────
        section("11. BLOCKCHAIN SUMMARY");
        List<Block> chain = node.getBlockchain().getChain();
        System.out.println("  Chain length: " + chain.size() + " blocks");
        System.out.println("  Difficulty: " + node.getBlockchain().getDifficulty());
        System.out.println();
        for (Block b : chain) {
            System.out.printf("  Block #%d | Txs: %d | Hash: %s...%n",
                    b.getIndex(),
                    b.getTransactions().size(),
                    b.getHash().substring(0, Math.min(32, b.getHash().length())));
        }

        // ─── 12. Audit Logs ────────────────────────────────────────────────────
        section("12. AUDIT LOGS");
        System.out.println(auditService.printAuditReport(alice.getPatientId()));
        System.out.println(auditService.printAuditReport(bob.getPatientId()));

        // ─── 13. Validate Blockchain ───────────────────────────────────────────
        section("13. BLOCKCHAIN INTEGRITY VALIDATION");
        boolean valid = node.getBlockchain().isChainValid();
        System.out.println("  Blockchain integrity check: " + (valid ? "✅ VALID" : "❌ INVALID"));
        if (valid) {
            System.out.println("  All block hashes verified.");
            System.out.println("  All previousHash links verified.");
            System.out.println("  All proof-of-work constraints verified.");
        }

        // ─── 14. Active Grants Summary ────────────────────────────────────────
        section("14. ACTIVE ACCESS GRANTS SUMMARY");
        List<AccessGrant> aliceGrants = patientService.viewMyGrants(alice);
        System.out.println("  Alice's active grants: " + aliceGrants.size());
        for (AccessGrant g : aliceGrants) {
            System.out.println("    → Doctor: " + g.getDoctorId().substring(0, 8) + "... | " +
                    g.getAccessLevel() + " | Expires: " + (g.getExpiresAt() == 0 ? "Never" : g.getExpiresAt()));
        }
        List<AccessGrant> bobGrants = patientService.viewMyGrants(bob);
        System.out.println("  Bob's active grants: " + bobGrants.size());
        for (AccessGrant g : bobGrants) {
            System.out.println("    → Doctor: " + g.getDoctorId().substring(0, 8) + "... | " +
                    g.getAccessLevel() + " | Expires: " + (g.getExpiresAt() == 0 ? "Never" : g.getExpiresAt()));
        }

        // ─── 15. Start Web Server ─────────────────────────────────────────────
        section("15. STARTING WEB SERVER");
        MedChainWebServer webServer = new MedChainWebServer(node, 8081);
        webServer.start();

        System.out.println();
        printFooter();
        System.out.println("\n  Web server is running. Press Ctrl+C to stop.\n");

        // Keep the server alive
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n  Shutdown requested. Stopping web server...");
            webServer.stop();
        }));

        // Keep JVM alive
        Thread.currentThread().join();
    }

    // ─── ASCII Art Helpers ─────────────────────────────────────────────────────

    static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  ███╗   ███╗███████╗██████╗  ██████╗██╗  ██╗ █████╗ ██╗███╗   ██╗  ║");
        System.out.println("║  ████╗ ████║██╔════╝██╔══██╗██╔════╝██║  ██║██╔══██╗██║████╗  ██║  ║");
        System.out.println("║  ██╔████╔██║█████╗  ██║  ██║██║     ███████║███████║██║██╔██╗ ██║  ║");
        System.out.println("║  ██║╚██╔╝██║██╔══╝  ██║  ██║██║     ██╔══██║██╔══██║██║██║╚██╗██║  ║");
        System.out.println("║  ██║ ╚═╝ ██║███████╗██████╔╝╚██████╗██║  ██║██║  ██║██║██║ ╚████║  ║");
        System.out.println("║  ╚═╝     ╚═╝╚══════╝╚═════╝  ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝  ║");
        System.out.println("║                                                                      ║");
        System.out.println("║      Blockchain-Based Patient-Controlled Medical Data Access         ║");
        System.out.println("║                     System  v1.0  (Java 25)                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    static void section(String title) {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────────────────────────────┐");
        System.out.printf( "│  STEP %s%n", title);
        System.out.println("└──────────────────────────────────────────────────────────────────────┘");
    }

    static void printFooter() {
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   MEDCHAIN DEMO COMPLETE                             ║");
        System.out.println("║                                                                      ║");
        System.out.println("║  ✅ RSA-2048 key pairs generated for all identities                  ║");
        System.out.println("║  ✅ AES-256-CBC encryption for all medical records                   ║");
        System.out.println("║  ✅ Merkle tree computed for each block                              ║");
        System.out.println("║  ✅ Proof-of-work mining (difficulty=2)                              ║");
        System.out.println("║  ✅ Digital signatures on all transactions                           ║");
        System.out.println("║  ✅ Patient-controlled access grants enforced                        ║");
        System.out.println("║  ✅ Blockchain integrity validated                                   ║");
        System.out.println("║  ✅ HTTP API server running on port 8081                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
    }
}
