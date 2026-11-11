package com.medchain.storage;

import com.medchain.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EncryptedDataStore {
    private final Map<String, Patient> patients = new ConcurrentHashMap<>();
    private final Map<String, Doctor> doctors = new ConcurrentHashMap<>();
    private final Map<String, MedicalRecord> records = new ConcurrentHashMap<>();
    private final Map<String, List<AccessGrant>> grants = new ConcurrentHashMap<>();
    private final Map<String, String> publicKeyIndex = new ConcurrentHashMap<>();

    // Patient CRUD
    public synchronized void savePatient(Patient patient) {
        patients.put(patient.getPatientId(), patient);
        if (patient.getPublicKeyString() != null) {
            publicKeyIndex.put(patient.getPublicKeyString(), patient.getPatientId());
        }
    }

    public Patient getPatient(String patientId) {
        return patients.get(patientId);
    }

    public Patient getPatientByPublicKey(String publicKeyString) {
        String id = publicKeyIndex.get(publicKeyString);
        return id != null ? patients.get(id) : null;
    }

    public Collection<Patient> getAllPatients() {
        return patients.values();
    }

    // Doctor CRUD
    public synchronized void saveDoctor(Doctor doctor) {
        doctors.put(doctor.getDoctorId(), doctor);
        if (doctor.getPublicKeyString() != null) {
            publicKeyIndex.put(doctor.getPublicKeyString(), doctor.getDoctorId());
        }
    }

    public Doctor getDoctor(String doctorId) {
        return doctors.get(doctorId);
    }

    public Doctor getDoctorByPublicKey(String publicKeyString) {
        String id = publicKeyIndex.get(publicKeyString);
        return id != null ? doctors.get(id) : null;
    }

    public Collection<Doctor> getAllDoctors() {
        return doctors.values();
    }

    // MedicalRecord CRUD
    public synchronized void saveRecord(MedicalRecord record) {
        records.put(record.getRecordId(), record);
    }

    public MedicalRecord getRecord(String recordId) {
        return records.get(recordId);
    }

    public List<MedicalRecord> getRecordsByPatient(String patientId) {
        List<MedicalRecord> result = new ArrayList<>();
        for (MedicalRecord r : records.values()) {
            if (patientId.equals(r.getPatientId())) {
                result.add(r);
            }
        }
        return result;
    }

    public List<MedicalRecord> getRecordsByDoctor(String doctorId) {
        List<MedicalRecord> result = new ArrayList<>();
        for (MedicalRecord r : records.values()) {
            if (doctorId.equals(r.getDoctorId())) {
                result.add(r);
            }
        }
        return result;
    }

    // AccessGrant CRUD
    public synchronized void saveGrant(AccessGrant grant) {
        grants.computeIfAbsent(grant.getPatientId(), k -> new ArrayList<>()).add(grant);
    }

    public synchronized void deactivateGrants(String patientId, String doctorId) {
        List<AccessGrant> patientGrants = grants.getOrDefault(patientId, Collections.emptyList());
        for (AccessGrant g : patientGrants) {
            if (doctorId.equals(g.getDoctorId())) {
                g.setActive(false);
            }
        }
    }

    public List<AccessGrant> getGrantsForPatient(String patientId) {
        return grants.getOrDefault(patientId, Collections.emptyList());
    }

    public List<AccessGrant> getActiveGrantsForPatient(String patientId) {
        List<AccessGrant> result = new ArrayList<>();
        for (AccessGrant g : grants.getOrDefault(patientId, Collections.emptyList())) {
            if (g.isActive() && !g.isExpired()) {
                result.add(g);
            }
        }
        return result;
    }

    public List<AccessGrant> getGrantsForDoctor(String doctorId, String patientId) {
        List<AccessGrant> result = new ArrayList<>();
        List<AccessGrant> patientGrants = patientId != null
                ? grants.getOrDefault(patientId, Collections.emptyList())
                : getAllGrants();
        for (AccessGrant g : patientGrants) {
            if (doctorId.equals(g.getDoctorId()) && g.isActive() && !g.isExpired()) {
                result.add(g);
            }
        }
        return result;
    }

    private List<AccessGrant> getAllGrants() {
        List<AccessGrant> all = new ArrayList<>();
        for (List<AccessGrant> list : grants.values()) {
            all.addAll(list);
        }
        return all;
    }

    public String resolveUserId(String publicKeyString) {
        return publicKeyIndex.get(publicKeyString);
    }
}
