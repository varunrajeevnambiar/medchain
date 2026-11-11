package com.medchain.blockchain;

import com.medchain.crypto.CryptoUtils;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Transaction {
    private String transactionId;
    private TransactionType type;
    private String fromAddress;
    private String toAddress;
    private String dataHash;
    private String encryptedPayload;
    private long timestamp;
    private String signature;
    private boolean valid;

    public Transaction(TransactionType type, String fromAddress, String toAddress,
                       String dataHash, String encryptedPayload) {
        this.type = type;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.dataHash = dataHash;
        this.encryptedPayload = encryptedPayload;
        this.timestamp = System.currentTimeMillis();
        this.valid = false;
        try {
            this.transactionId = CryptoUtils.sha256(type.name() + fromAddress + dataHash + timestamp);
        } catch (Exception e) {
            this.transactionId = java.util.UUID.randomUUID().toString();
        }
    }

    public void sign(PrivateKey privateKey) throws Exception {
        String sigData = getSignableData();
        byte[] sigBytes = CryptoUtils.sign(sigData, privateKey);
        this.signature = CryptoUtils.toBase64(sigBytes);
        this.valid = true;
    }

    public boolean verify(PublicKey publicKey) throws Exception {
        if (signature == null || signature.isEmpty()) return false;
        String sigData = getSignableData();
        byte[] sigBytes = CryptoUtils.fromBase64(signature);
        boolean result = CryptoUtils.verify(sigData, sigBytes, publicKey);
        this.valid = result;
        return result;
    }

    private String getSignableData() {
        return transactionId + type.name() + fromAddress +
               (toAddress != null ? toAddress : "") +
               (dataHash != null ? dataHash : "") +
               timestamp;
    }

    public String toJson() {
        return "{\n" +
               "  \"transactionId\": \"" + transactionId + "\",\n" +
               "  \"type\": \"" + type + "\",\n" +
               "  \"fromAddress\": \"" + shorten(fromAddress) + "\",\n" +
               "  \"toAddress\": \"" + (toAddress != null ? shorten(toAddress) : "null") + "\",\n" +
               "  \"dataHash\": \"" + (dataHash != null ? dataHash : "") + "\",\n" +
               "  \"timestamp\": " + timestamp + ",\n" +
               "  \"valid\": " + valid + ",\n" +
               "  \"signature\": \"" + (signature != null ? signature.substring(0, Math.min(20, signature.length())) + "..." : "null") + "\"\n" +
               "}";
    }

    private String shorten(String s) {
        if (s == null) return "null";
        return s.length() > 30 ? s.substring(0, 30) + "..." : s;
    }

    // Getters and setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }
    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }
    public String getEncryptedPayload() { return encryptedPayload; }
    public void setEncryptedPayload(String encryptedPayload) { this.encryptedPayload = encryptedPayload; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
}
