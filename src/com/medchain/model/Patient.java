package com.medchain.model;

import com.medchain.crypto.CryptoUtils;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

public class Patient {
    private String patientId;
    private String name;
    private String dateOfBirth;
    private String bloodType;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String publicKeyString;
    private long registeredAt;

    public Patient() {
        this.patientId = UUID.randomUUID().toString();
        this.registeredAt = System.currentTimeMillis();
    }

    public Patient(String name, String dateOfBirth, String bloodType) {
        this();
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.bloodType = bloodType;
    }

    public String toJson() {
        return "{\n" +
               "  \"patientId\": \"" + patientId + "\",\n" +
               "  \"name\": \"" + name + "\",\n" +
               "  \"dateOfBirth\": \"" + dateOfBirth + "\",\n" +
               "  \"bloodType\": \"" + bloodType + "\",\n" +
               "  \"publicKey\": \"" + (publicKeyString != null ? publicKeyString.substring(0, Math.min(40, publicKeyString.length())) + "..." : "null") + "\",\n" +
               "  \"registeredAt\": " + registeredAt + "\n" +
               "}";
    }

    // Getters and setters
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    public PublicKey getPublicKey() { return publicKey; }
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        this.publicKeyString = CryptoUtils.publicKeyToString(publicKey);
    }
    public PrivateKey getPrivateKey() { return privateKey; }
    public void setPrivateKey(PrivateKey privateKey) { this.privateKey = privateKey; }
    public String getPublicKeyString() { return publicKeyString; }
    public void setPublicKeyString(String publicKeyString) { this.publicKeyString = publicKeyString; }
    public long getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(long registeredAt) { this.registeredAt = registeredAt; }
}
