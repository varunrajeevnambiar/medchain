package com.medchain.model;

import java.util.Arrays;
import java.util.UUID;

public class AccessGrant {
    private String grantId;
    private String patientId;
    private String doctorId;
    private String doctorPublicKey;
    private String[] recordIds;      // null means all records
    private AccessLevel accessLevel;
    private long grantedAt;
    private long expiresAt;          // 0 = never
    private boolean active;
    private String encryptedRecordKey;
    private String grantTransactionId;

    public AccessGrant() {
        this.grantId = UUID.randomUUID().toString();
        this.grantedAt = System.currentTimeMillis();
        this.active = true;
    }

    public boolean isExpired() {
        if (expiresAt == 0) return false;
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isValidFor(String recordId) {
        if (!active) return false;
        if (isExpired()) return false;
        if (recordIds == null) return true; // access to all records
        for (String id : recordIds) {
            if (id.equals(recordId)) return true;
        }
        return false;
    }

    public String toJson() {
        String recordIdsStr = "null";
        if (recordIds != null) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < recordIds.length; i++) {
                sb.append("\"").append(recordIds[i]).append("\"");
                if (i < recordIds.length - 1) sb.append(", ");
            }
            sb.append("]");
            recordIdsStr = sb.toString();
        }
        return "{\n" +
               "  \"grantId\": \"" + grantId + "\",\n" +
               "  \"patientId\": \"" + patientId + "\",\n" +
               "  \"doctorId\": \"" + doctorId + "\",\n" +
               "  \"accessLevel\": \"" + accessLevel + "\",\n" +
               "  \"recordIds\": " + recordIdsStr + ",\n" +
               "  \"grantedAt\": " + grantedAt + ",\n" +
               "  \"expiresAt\": " + expiresAt + ",\n" +
               "  \"active\": " + active + ",\n" +
               "  \"expired\": " + isExpired() + ",\n" +
               "  \"grantTransactionId\": \"" + (grantTransactionId != null ? grantTransactionId : "") + "\"\n" +
               "}";
    }

    // Getters and setters
    public String getGrantId() { return grantId; }
    public void setGrantId(String grantId) { this.grantId = grantId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getDoctorPublicKey() { return doctorPublicKey; }
    public void setDoctorPublicKey(String doctorPublicKey) { this.doctorPublicKey = doctorPublicKey; }
    public String[] getRecordIds() { return recordIds; }
    public void setRecordIds(String[] recordIds) { this.recordIds = recordIds; }
    public AccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(AccessLevel accessLevel) { this.accessLevel = accessLevel; }
    public long getGrantedAt() { return grantedAt; }
    public void setGrantedAt(long grantedAt) { this.grantedAt = grantedAt; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getEncryptedRecordKey() { return encryptedRecordKey; }
    public void setEncryptedRecordKey(String encryptedRecordKey) { this.encryptedRecordKey = encryptedRecordKey; }
    public String getGrantTransactionId() { return grantTransactionId; }
    public void setGrantTransactionId(String grantTransactionId) { this.grantTransactionId = grantTransactionId; }
}
