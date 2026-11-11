package com.medchain.model;

import java.util.UUID;

public class MedicalRecord {
    private String recordId;
    private String patientId;
    private String doctorId;
    private String recordType;   // "DIAGNOSIS", "PRESCRIPTION", "LAB_RESULT", "IMAGING", "SURGERY"
    private String title;
    private String description;
    private String diagnosis;
    private String prescription;
    private String notes;
    private long createdAt;
    private long updatedAt;
    private String encryptedData;
    private String encryptedKeyForPatient;
    private String dataHash;
    private String transactionId;

    public MedicalRecord() {
        this.recordId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public String toPlainJson() {
        return "{\n" +
               "  \"recordId\": \"" + recordId + "\",\n" +
               "  \"patientId\": \"" + patientId + "\",\n" +
               "  \"doctorId\": \"" + (doctorId != null ? doctorId : "") + "\",\n" +
               "  \"recordType\": \"" + (recordType != null ? recordType : "") + "\",\n" +
               "  \"title\": \"" + (title != null ? title : "") + "\",\n" +
               "  \"description\": \"" + (description != null ? escapeJson(description) : "") + "\",\n" +
               "  \"diagnosis\": \"" + (diagnosis != null ? escapeJson(diagnosis) : "") + "\",\n" +
               "  \"prescription\": \"" + (prescription != null ? escapeJson(prescription) : "") + "\",\n" +
               "  \"notes\": \"" + (notes != null ? escapeJson(notes) : "") + "\",\n" +
               "  \"createdAt\": " + createdAt + ",\n" +
               "  \"updatedAt\": " + updatedAt + "\n" +
               "}";
    }

    public String toJson() {
        return "{\n" +
               "  \"recordId\": \"" + recordId + "\",\n" +
               "  \"patientId\": \"" + patientId + "\",\n" +
               "  \"doctorId\": \"" + (doctorId != null ? doctorId : "") + "\",\n" +
               "  \"recordType\": \"" + (recordType != null ? recordType : "") + "\",\n" +
               "  \"title\": \"" + (title != null ? title : "") + "\",\n" +
               "  \"dataHash\": \"" + (dataHash != null ? dataHash : "") + "\",\n" +
               "  \"transactionId\": \"" + (transactionId != null ? transactionId : "") + "\",\n" +
               "  \"encryptedData\": \"" + (encryptedData != null ? encryptedData.substring(0, Math.min(30, encryptedData.length())) + "..." : "null") + "\",\n" +
               "  \"createdAt\": " + createdAt + ",\n" +
               "  \"updatedAt\": " + updatedAt + "\n" +
               "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // Getters and setters
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public String getEncryptedData() { return encryptedData; }
    public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }
    public String getEncryptedKeyForPatient() { return encryptedKeyForPatient; }
    public void setEncryptedKeyForPatient(String encryptedKeyForPatient) { this.encryptedKeyForPatient = encryptedKeyForPatient; }
    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
