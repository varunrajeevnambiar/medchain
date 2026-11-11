package com.medchain.contract;

import com.medchain.blockchain.*;
import com.medchain.crypto.CryptoUtils;
import com.medchain.model.*;
import com.medchain.storage.EncryptedDataStore;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

public class AccessControlContract {
    private final Blockchain blockchain;
    private final EncryptedDataStore dataStore;

    public AccessControlContract(Blockchain blockchain, EncryptedDataStore dataStore) {
        this.blockchain = blockchain;
        this.dataStore = dataStore;
    }

    public synchronized Transaction registerPatient(Patient p) throws Exception {
        String payloadJson = p.toJson();
        String dataHash = CryptoUtils.sha256(payloadJson);

        Transaction tx = new Transaction(
                TransactionType.REGISTER_PATIENT,
                p.getPublicKeyString(),
                "SYSTEM",
                dataHash,
                CryptoUtils.toBase64(payloadJson.getBytes())
        );
        tx.sign(p.getPrivateKey());

        dataStore.savePatient(p);
        blockchain.addTransaction(tx);
        return tx;
    }

    public synchronized Transaction registerDoctor(Doctor d) throws Exception {
        String payloadJson = d.toJson();
        String dataHash = CryptoUtils.sha256(payloadJson);

        Transaction tx = new Transaction(
                TransactionType.REGISTER_DOCTOR,
                d.getPublicKeyString(),
                "SYSTEM",
                dataHash,
                CryptoUtils.toBase64(payloadJson.getBytes())
        );
        tx.sign(d.getPrivateKey());

        dataStore.saveDoctor(d);
        blockchain.addTransaction(tx);
        return tx;
    }

    public synchronized Transaction addMedicalRecord(MedicalRecord r, Patient p) throws Exception {
        // Encrypt the record with AES key
        SecretKey aesKey = CryptoUtils.generateAESKey();
        String plainJson = r.toPlainJson();
        byte[] encryptedBytes = CryptoUtils.encryptAES(plainJson.getBytes("UTF-8"), aesKey);
        String encryptedData = CryptoUtils.toBase64(encryptedBytes);

        // Encrypt AES key with patient's RSA public key
        byte[] encKeyForPatient = CryptoUtils.encryptKeyWithRSA(aesKey, p.getPublicKey());
        String encKeyStr = CryptoUtils.toBase64(encKeyForPatient);

        String dataHash = CryptoUtils.sha256(plainJson);

        r.setEncryptedData(encryptedData);
        r.setEncryptedKeyForPatient(encKeyStr);
        r.setDataHash(dataHash);
        r.setPatientId(p.getPatientId());

        Transaction tx = new Transaction(
                TransactionType.ADD_MEDICAL_RECORD,
                p.getPublicKeyString(),
                null,
                dataHash,
                encryptedData.substring(0, Math.min(100, encryptedData.length()))
        );
        tx.sign(p.getPrivateKey());
        r.setTransactionId(tx.getTransactionId());

        dataStore.saveRecord(r);
        blockchain.addTransaction(tx);
        return tx;
    }

    public synchronized Transaction grantAccess(Patient p, String doctorId,
                                                  String[] recordIds, long expiresAt) throws Exception {
        Doctor doctor = dataStore.getDoctor(doctorId);
        if (doctor == null) throw new IllegalArgumentException("Doctor not found: " + doctorId);

        // Create access grant
        AccessGrant grant = new AccessGrant();
        grant.setPatientId(p.getPatientId());
        grant.setDoctorId(doctorId);
        grant.setDoctorPublicKey(doctor.getPublicKeyString());
        grant.setRecordIds(recordIds);
        grant.setAccessLevel(AccessLevel.READ_ONLY);
        grant.setExpiresAt(expiresAt);

        // If granting access to specific records, encrypt the AES keys for the doctor
        if (recordIds != null) {
            StringBuilder encKeys = new StringBuilder();
            for (String recId : recordIds) {
                MedicalRecord rec = dataStore.getRecord(recId);
                if (rec != null && rec.getEncryptedKeyForPatient() != null) {
                    // Decrypt AES key with patient's private key, re-encrypt with doctor's public key
                    byte[] encKeyForPatient = CryptoUtils.fromBase64(rec.getEncryptedKeyForPatient());
                    SecretKey aesKey = CryptoUtils.decryptKeyWithRSA(encKeyForPatient, p.getPrivateKey());
                    byte[] encKeyForDoctor = CryptoUtils.encryptKeyWithRSA(aesKey, doctor.getPublicKey());
                    encKeys.append(recId).append(":").append(CryptoUtils.toBase64(encKeyForDoctor)).append("|");
                }
            }
            grant.setEncryptedRecordKey(encKeys.toString());
        } else {
            // Grant access to ALL records — encrypt each record's key for the doctor
            List<MedicalRecord> patientRecords = dataStore.getRecordsByPatient(p.getPatientId());
            StringBuilder encKeys = new StringBuilder();
            for (MedicalRecord rec : patientRecords) {
                if (rec.getEncryptedKeyForPatient() != null) {
                    byte[] encKeyForPatient = CryptoUtils.fromBase64(rec.getEncryptedKeyForPatient());
                    SecretKey aesKey = CryptoUtils.decryptKeyWithRSA(encKeyForPatient, p.getPrivateKey());
                    byte[] encKeyForDoctor = CryptoUtils.encryptKeyWithRSA(aesKey, doctor.getPublicKey());
                    encKeys.append(rec.getRecordId()).append(":").append(CryptoUtils.toBase64(encKeyForDoctor)).append("|");
                }
            }
            grant.setEncryptedRecordKey(encKeys.toString());
        }

        String grantJson = grant.toJson();
        String dataHash = CryptoUtils.sha256(grantJson);

        Transaction tx = new Transaction(
                TransactionType.GRANT_ACCESS,
                p.getPublicKeyString(),
                doctor.getPublicKeyString(),
                dataHash,
                CryptoUtils.toBase64(grantJson.getBytes())
        );
        tx.sign(p.getPrivateKey());
        grant.setGrantTransactionId(tx.getTransactionId());

        dataStore.saveGrant(grant);
        blockchain.addTransaction(tx);
        return tx;
    }

    public synchronized Transaction grantAccessWithLevel(Patient p, String doctorId,
                                                           String[] recordIds, AccessLevel level,
                                                           long expiresAt) throws Exception {
        Doctor doctor = dataStore.getDoctor(doctorId);
        if (doctor == null) throw new IllegalArgumentException("Doctor not found: " + doctorId);

        AccessGrant grant = new AccessGrant();
        grant.setPatientId(p.getPatientId());
        grant.setDoctorId(doctorId);
        grant.setDoctorPublicKey(doctor.getPublicKeyString());
        grant.setRecordIds(recordIds);
        grant.setAccessLevel(level);
        grant.setExpiresAt(expiresAt);

        List<MedicalRecord> patientRecords;
        if (recordIds != null) {
            patientRecords = new ArrayList<>();
            for (String recId : recordIds) {
                MedicalRecord rec = dataStore.getRecord(recId);
                if (rec != null) patientRecords.add(rec);
            }
        } else {
            patientRecords = dataStore.getRecordsByPatient(p.getPatientId());
        }

        StringBuilder encKeys = new StringBuilder();
        for (MedicalRecord rec : patientRecords) {
            if (rec.getEncryptedKeyForPatient() != null) {
                byte[] encKeyForPatient = CryptoUtils.fromBase64(rec.getEncryptedKeyForPatient());
                SecretKey aesKey = CryptoUtils.decryptKeyWithRSA(encKeyForPatient, p.getPrivateKey());
                byte[] encKeyForDoctor = CryptoUtils.encryptKeyWithRSA(aesKey, doctor.getPublicKey());
                encKeys.append(rec.getRecordId()).append(":").append(CryptoUtils.toBase64(encKeyForDoctor)).append("|");
            }
        }
        grant.setEncryptedRecordKey(encKeys.toString());

        String grantJson = grant.toJson();
        String dataHash = CryptoUtils.sha256(grantJson);

        Transaction tx = new Transaction(
                TransactionType.GRANT_ACCESS,
                p.getPublicKeyString(),
                doctor.getPublicKeyString(),
                dataHash,
                CryptoUtils.toBase64(grantJson.getBytes())
        );
        tx.sign(p.getPrivateKey());
        grant.setGrantTransactionId(tx.getTransactionId());

        dataStore.saveGrant(grant);
        blockchain.addTransaction(tx);
        return tx;
    }

    public synchronized Transaction revokeAccess(Patient p, String doctorId) throws Exception {
        Doctor doctor = dataStore.getDoctor(doctorId);
        if (doctor == null) throw new IllegalArgumentException("Doctor not found: " + doctorId);

        dataStore.deactivateGrants(p.getPatientId(), doctorId);

        String revokeInfo = p.getPatientId() + ":REVOKE:" + doctorId;
        String dataHash = CryptoUtils.sha256(revokeInfo);

        Transaction tx = new Transaction(
                TransactionType.REVOKE_ACCESS,
                p.getPublicKeyString(),
                doctor.getPublicKeyString(),
                dataHash,
                CryptoUtils.toBase64(revokeInfo.getBytes())
        );
        tx.sign(p.getPrivateKey());

        blockchain.addTransaction(tx);
        return tx;
    }

    public synchronized MedicalRecord accessRecord(Doctor d, String recordId,
                                                     String patientId) throws Exception {
        // Check access
        if (!isAccessGranted(d.getDoctorId(), patientId, recordId)) {
            throw new SecurityException("ACCESS DENIED: Doctor " + d.getName() +
                    " does not have access to record " + recordId + " for patient " + patientId);
        }

        MedicalRecord rec = dataStore.getRecord(recordId);
        if (rec == null) throw new IllegalArgumentException("Record not found: " + recordId);

        // Find the encrypted key for this doctor
        List<AccessGrant> doctorGrants = dataStore.getGrantsForDoctor(d.getDoctorId(), patientId);
        String encKeyForDoctor = null;
        for (AccessGrant grant : doctorGrants) {
            if (grant.getEncryptedRecordKey() != null) {
                String[] keyMappings = grant.getEncryptedRecordKey().split("\\|");
                for (String mapping : keyMappings) {
                    if (mapping.startsWith(recordId + ":")) {
                        encKeyForDoctor = mapping.substring(recordId.length() + 1);
                        break;
                    }
                }
            }
            if (encKeyForDoctor != null) break;
        }

        if (encKeyForDoctor == null) {
            throw new SecurityException("No decryption key available for doctor " + d.getName());
        }

        // Decrypt the AES key with doctor's private key
        byte[] encKeyBytes = CryptoUtils.fromBase64(encKeyForDoctor);
        SecretKey aesKey = CryptoUtils.decryptKeyWithRSA(encKeyBytes, d.getPrivateKey());

        // Decrypt the record data
        byte[] encDataBytes = CryptoUtils.fromBase64(rec.getEncryptedData());
        byte[] plainBytes = CryptoUtils.decryptAES(encDataBytes, aesKey);
        String plainJson = new String(plainBytes, "UTF-8");

        // Log the access on blockchain
        String dataHash = CryptoUtils.sha256(d.getDoctorId() + ":ACCESS:" + recordId);
        Transaction accessTx = new Transaction(
                TransactionType.ACCESS_MEDICAL_RECORD,
                d.getPublicKeyString(),
                patientId,
                dataHash,
                null
        );
        accessTx.sign(d.getPrivateKey());
        blockchain.addTransaction(accessTx);

        // Create a decrypted copy for return
        MedicalRecord decrypted = new MedicalRecord();
        decrypted.setRecordId(rec.getRecordId());
        decrypted.setPatientId(rec.getPatientId());
        decrypted.setDoctorId(rec.getDoctorId());
        decrypted.setRecordType(rec.getRecordType());
        decrypted.setTitle(rec.getTitle());
        decrypted.setDataHash(rec.getDataHash());
        decrypted.setTransactionId(rec.getTransactionId());
        decrypted.setCreatedAt(rec.getCreatedAt());
        decrypted.setUpdatedAt(rec.getUpdatedAt());
        // Parse basic fields from plain JSON
        decrypted.setDescription(extractField(plainJson, "description"));
        decrypted.setDiagnosis(extractField(plainJson, "diagnosis"));
        decrypted.setPrescription(extractField(plainJson, "prescription"));
        decrypted.setNotes(extractField(plainJson, "notes"));
        return decrypted;
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

    public List<AccessGrant> getActiveGrants(String patientId) {
        return dataStore.getActiveGrantsForPatient(patientId);
    }

    public List<Transaction> getAuditLog(String patientId) {
        Patient p = dataStore.getPatient(patientId);
        if (p == null) return new ArrayList<>();
        return blockchain.getTransactionsForPatient(p.getPublicKeyString());
    }

    public boolean isAccessGranted(String doctorId, String patientId, String recordId) {
        List<AccessGrant> grants = dataStore.getGrantsForDoctor(doctorId, patientId);
        for (AccessGrant grant : grants) {
            if (grant.isValidFor(recordId)) return true;
        }
        return false;
    }

    public Blockchain getBlockchain() { return blockchain; }
    public EncryptedDataStore getDataStore() { return dataStore; }
}
