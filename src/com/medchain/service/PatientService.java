package com.medchain.service;

import com.medchain.blockchain.Transaction;
import com.medchain.contract.AccessControlContract;
import com.medchain.crypto.CryptoUtils;
import com.medchain.model.*;
import com.medchain.storage.EncryptedDataStore;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class PatientService {
    private final AccessControlContract contract;
    private final EncryptedDataStore dataStore;

    public PatientService(AccessControlContract contract) {
        this.contract = contract;
        this.dataStore = contract.getDataStore();
    }

    public Patient registerPatient(String name, String dob, String bloodType) throws Exception {
        KeyPair keyPair = CryptoUtils.generateRSAKeyPair();
        Patient patient = new Patient(name, dob, bloodType);
        patient.setPublicKey(keyPair.getPublic());
        patient.setPrivateKey(keyPair.getPrivate());

        Transaction tx = contract.registerPatient(patient);
        System.out.println("  [PatientService] Registered patient: " + name + " (id=" + patient.getPatientId() + ")");
        System.out.println("  [PatientService] Registration tx: " + tx.getTransactionId());
        return patient;
    }

    public MedicalRecord addMedicalRecord(Patient patient, String recordType, String title,
                                           String description, String diagnosis,
                                           String prescription, String notes) throws Exception {
        MedicalRecord record = new MedicalRecord();
        record.setPatientId(patient.getPatientId());
        record.setRecordType(recordType);
        record.setTitle(title);
        record.setDescription(description);
        record.setDiagnosis(diagnosis);
        record.setPrescription(prescription);
        record.setNotes(notes);

        Transaction tx = contract.addMedicalRecord(record, patient);
        System.out.println("  [PatientService] Added medical record: " + title + " (id=" + record.getRecordId() + ")");
        return record;
    }

    public AccessGrant grantAccessToDoctor(Patient patient, String doctorId,
                                            String[] recordIds, int expiresInDays) throws Exception {
        long expiresAt = expiresInDays > 0
                ? System.currentTimeMillis() + (long) expiresInDays * 24 * 60 * 60 * 1000L
                : 0L;

        Transaction tx = contract.grantAccess(patient, doctorId, recordIds, expiresAt);

        List<AccessGrant> grants = dataStore.getActiveGrantsForPatient(patient.getPatientId());
        AccessGrant latest = null;
        for (AccessGrant g : grants) {
            if (doctorId.equals(g.getDoctorId())) {
                if (latest == null || g.getGrantedAt() > latest.getGrantedAt()) {
                    latest = g;
                }
            }
        }
        System.out.println("  [PatientService] Granted access to doctor " + doctorId +
                " (expires: " + (expiresInDays > 0 ? expiresInDays + " days" : "never") + ")");
        return latest;
    }

    public AccessGrant grantAccessWithLevel(Patient patient, String doctorId,
                                             String[] recordIds, AccessLevel level,
                                             int expiresInDays) throws Exception {
        long expiresAt = expiresInDays > 0
                ? System.currentTimeMillis() + (long) expiresInDays * 24 * 60 * 60 * 1000L
                : 0L;

        Transaction tx = contract.grantAccessWithLevel(patient, doctorId, recordIds, level, expiresAt);

        List<AccessGrant> grants = dataStore.getActiveGrantsForPatient(patient.getPatientId());
        AccessGrant latest = null;
        for (AccessGrant g : grants) {
            if (doctorId.equals(g.getDoctorId())) {
                if (latest == null || g.getGrantedAt() > latest.getGrantedAt()) {
                    latest = g;
                }
            }
        }
        System.out.println("  [PatientService] Granted " + level + " access to doctor " + doctorId);
        return latest;
    }

    public boolean revokeAccessFromDoctor(Patient patient, String doctorId) throws Exception {
        Transaction tx = contract.revokeAccess(patient, doctorId);
        System.out.println("  [PatientService] Revoked access from doctor " + doctorId);
        return tx != null;
    }

    public List<MedicalRecord> viewMyRecords(Patient patient) throws Exception {
        List<MedicalRecord> encrypted = dataStore.getRecordsByPatient(patient.getPatientId());
        List<MedicalRecord> decrypted = new ArrayList<>();

        for (MedicalRecord rec : encrypted) {
            if (rec.getEncryptedData() != null && rec.getEncryptedKeyForPatient() != null) {
                try {
                    byte[] encKeyBytes = CryptoUtils.fromBase64(rec.getEncryptedKeyForPatient());
                    SecretKey aesKey = CryptoUtils.decryptKeyWithRSA(encKeyBytes, patient.getPrivateKey());
                    byte[] encDataBytes = CryptoUtils.fromBase64(rec.getEncryptedData());
                    byte[] plainBytes = CryptoUtils.decryptAES(encDataBytes, aesKey);
                    String plainJson = new String(plainBytes, "UTF-8");

                    MedicalRecord dr = new MedicalRecord();
                    dr.setRecordId(rec.getRecordId());
                    dr.setPatientId(rec.getPatientId());
                    dr.setDoctorId(rec.getDoctorId());
                    dr.setRecordType(rec.getRecordType());
                    dr.setTitle(rec.getTitle());
                    dr.setDataHash(rec.getDataHash());
                    dr.setTransactionId(rec.getTransactionId());
                    dr.setCreatedAt(rec.getCreatedAt());
                    dr.setUpdatedAt(rec.getUpdatedAt());
                    dr.setDescription(extractField(plainJson, "description"));
                    dr.setDiagnosis(extractField(plainJson, "diagnosis"));
                    dr.setPrescription(extractField(plainJson, "prescription"));
                    dr.setNotes(extractField(plainJson, "notes"));
                    decrypted.add(dr);
                } catch (Exception e) {
                    decrypted.add(rec); // return encrypted if decryption fails
                }
            } else {
                decrypted.add(rec);
            }
        }
        return decrypted;
    }

    public List<AccessGrant> viewMyGrants(Patient patient) {
        return dataStore.getActiveGrantsForPatient(patient.getPatientId());
    }

    public List<Transaction> viewAuditLog(Patient patient) {
        return contract.getAuditLog(patient.getPatientId());
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\": \"";
        int start = json.indexOf(key);
        if (start < 0) return "";
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end);
    }
}
