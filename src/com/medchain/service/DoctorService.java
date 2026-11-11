package com.medchain.service;

import com.medchain.blockchain.Transaction;
import com.medchain.contract.AccessControlContract;
import com.medchain.crypto.CryptoUtils;
import com.medchain.model.*;
import com.medchain.storage.EncryptedDataStore;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DoctorService {
    private final AccessControlContract contract;
    private final EncryptedDataStore dataStore;

    public DoctorService(AccessControlContract contract) {
        this.contract = contract;
        this.dataStore = contract.getDataStore();
    }

    public Doctor registerDoctor(String name, String specialization, String licenseNumber) throws Exception {
        KeyPair keyPair = CryptoUtils.generateRSAKeyPair();
        Doctor doctor = new Doctor(name, specialization, licenseNumber);
        doctor.setPublicKey(keyPair.getPublic());
        doctor.setPrivateKey(keyPair.getPrivate());

        Transaction tx = contract.registerDoctor(doctor);
        System.out.println("  [DoctorService] Registered doctor: " + name + " (id=" + doctor.getDoctorId() + ")");
        System.out.println("  [DoctorService] Registration tx: " + tx.getTransactionId());
        return doctor;
    }

    public MedicalRecord accessPatientRecord(Doctor doctor, String patientId,
                                              String recordId) throws Exception {
        return contract.accessRecord(doctor, recordId, patientId);
    }

    public List<String> getAuthorizedPatients(Doctor doctor) {
        Set<String> patientIds = new HashSet<>();
        // Scan all grants in datastore for this doctor
        for (Patient p : dataStore.getAllPatients()) {
            List<AccessGrant> grants = dataStore.getGrantsForDoctor(doctor.getDoctorId(), p.getPatientId());
            if (!grants.isEmpty()) {
                patientIds.add(p.getPatientId());
            }
        }
        return new ArrayList<>(patientIds);
    }

    public MedicalRecord addPrescription(Doctor doctor, String patientId,
                                          String title, String prescription,
                                          String notes) throws Exception {
        // Check that the doctor has READ_WRITE access
        List<AccessGrant> grants = dataStore.getGrantsForDoctor(doctor.getDoctorId(), patientId);
        boolean hasWriteAccess = false;
        for (AccessGrant grant : grants) {
            if (grant.getAccessLevel() == AccessLevel.READ_WRITE && grant.isActive() && !grant.isExpired()) {
                hasWriteAccess = true;
                break;
            }
        }
        if (!hasWriteAccess) {
            throw new SecurityException("ACCESS DENIED: Doctor " + doctor.getName() +
                    " does not have READ_WRITE access for patient " + patientId);
        }

        Patient patient = dataStore.getPatient(patientId);
        if (patient == null) throw new IllegalArgumentException("Patient not found: " + patientId);

        MedicalRecord record = new MedicalRecord();
        record.setPatientId(patientId);
        record.setDoctorId(doctor.getDoctorId());
        record.setRecordType("PRESCRIPTION");
        record.setTitle(title);
        record.setPrescription(prescription);
        record.setNotes(notes);
        record.setDescription("Prescription added by Dr. " + doctor.getName());

        Transaction tx = new Transaction(
                com.medchain.blockchain.TransactionType.ADD_MEDICAL_RECORD,
                doctor.getPublicKeyString(),
                patient.getPublicKeyString(),
                CryptoUtils.sha256(record.toPlainJson()),
                null
        );
        tx.sign(doctor.getPrivateKey());
        record.setTransactionId(tx.getTransactionId());
        record.setDataHash(CryptoUtils.sha256(record.toPlainJson()));

        // For simplicity, store without encryption since doctor added it
        // In production, would re-encrypt with patient's key
        record.setEncryptedData(CryptoUtils.toBase64(record.toPlainJson().getBytes("UTF-8")));

        dataStore.saveRecord(record);
        contract.getBlockchain().addTransaction(tx);

        System.out.println("  [DoctorService] Dr. " + doctor.getName() +
                " added prescription for patient " + patientId);
        return record;
    }
}
