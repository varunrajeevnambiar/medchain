package com.medchain.model;

import com.medchain.crypto.CryptoUtils;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

public class Doctor {
    private String doctorId;
    private String name;
    private String specialization;
    private String licenseNumber;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String publicKeyString;
    private long registeredAt;

    public Doctor() {
        this.doctorId = UUID.randomUUID().toString();
        this.registeredAt = System.currentTimeMillis();
    }

    public Doctor(String name, String specialization, String licenseNumber) {
        this();
        this.name = name;
        this.specialization = specialization;
        this.licenseNumber = licenseNumber;
    }

    public String toJson() {
        return "{\n" +
               "  \"doctorId\": \"" + doctorId + "\",\n" +
               "  \"name\": \"" + name + "\",\n" +
               "  \"specialization\": \"" + specialization + "\",\n" +
               "  \"licenseNumber\": \"" + licenseNumber + "\",\n" +
               "  \"publicKey\": \"" + (publicKeyString != null ? publicKeyString.substring(0, Math.min(40, publicKeyString.length())) + "..." : "null") + "\",\n" +
               "  \"registeredAt\": " + registeredAt + "\n" +
               "}";
    }

    // Getters and setters
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
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
