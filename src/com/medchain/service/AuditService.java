package com.medchain.service;

import com.medchain.blockchain.Transaction;
import com.medchain.blockchain.TransactionType;
import com.medchain.contract.AccessControlContract;
import com.medchain.model.Patient;
import com.medchain.storage.EncryptedDataStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuditService {
    private final AccessControlContract contract;
    private final EncryptedDataStore dataStore;

    public AuditService(AccessControlContract contract) {
        this.contract = contract;
        this.dataStore = contract.getDataStore();
    }

    public List<Transaction> getFullAuditLog(String patientId) {
        return contract.getAuditLog(patientId);
    }

    public List<Transaction> getAccessHistory(String patientId) {
        List<Transaction> all = getFullAuditLog(patientId);
        List<Transaction> result = new ArrayList<>();
        for (Transaction tx : all) {
            if (tx.getType() == TransactionType.ACCESS_MEDICAL_RECORD) {
                result.add(tx);
            }
        }
        return result;
    }

    public List<Transaction> getGrantHistory(String patientId) {
        List<Transaction> all = getFullAuditLog(patientId);
        List<Transaction> result = new ArrayList<>();
        for (Transaction tx : all) {
            if (tx.getType() == TransactionType.GRANT_ACCESS ||
                    tx.getType() == TransactionType.REVOKE_ACCESS) {
                result.add(tx);
            }
        }
        return result;
    }

    public String printAuditReport(String patientId) {
        Patient patient = dataStore.getPatient(patientId);
        String patientName = patient != null ? patient.getName() : patientId;
        List<Transaction> all = getFullAuditLog(patientId);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║           AUDIT REPORT FOR: ").append(padRight(patientName, 34)).append("║\n");
        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        sb.append(String.format("  Total transactions: %d%n", all.size()));
        sb.append(String.format("  Access events:      %d%n", getAccessHistory(patientId).size()));
        sb.append(String.format("  Grant events:       %d%n", getGrantHistory(patientId).size()));
        sb.append("\n  Transaction Log:\n");
        sb.append("  ─────────────────────────────────────────────────────────────\n");

        for (Transaction tx : all) {
            String time = sdf.format(new Date(tx.getTimestamp()));
            sb.append(String.format("  [%s] %-25s | TX: %s...%n",
                    time,
                    tx.getType().name(),
                    tx.getTransactionId().substring(0, Math.min(16, tx.getTransactionId().length()))));
        }
        sb.append("  ─────────────────────────────────────────────────────────────\n");
        return sb.toString();
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
