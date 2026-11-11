package com.medchain.blockchain;

import com.medchain.crypto.CryptoUtils;
import java.util.ArrayList;
import java.util.List;

public class Block {
    private int index;
    private long timestamp;
    private List<Transaction> transactions;
    private String previousHash;
    private String merkleRoot;
    private String hash;
    private int nonce;
    private String minedBy;

    public Block(int index, String previousHash) {
        this.index = index;
        this.previousHash = previousHash;
        this.timestamp = System.currentTimeMillis();
        this.transactions = new ArrayList<>();
        this.nonce = 0;
        this.minedBy = "system";
        this.merkleRoot = "";
        this.hash = "";
    }

    public void addTransaction(Transaction tx) {
        transactions.add(tx);
    }

    public String calculateHash() throws Exception {
        List<String> txHashes = new ArrayList<>();
        for (Transaction tx : transactions) {
            txHashes.add(tx.getDataHash() != null ? tx.getDataHash() : tx.getTransactionId());
        }
        this.merkleRoot = MerkleTree.computeRoot(txHashes);
        String blockData = index + timestamp + previousHash + merkleRoot + nonce;
        return CryptoUtils.sha256(blockData);
    }

    public void mine(int difficulty) throws Exception {
        String target = "0".repeat(difficulty);
        List<String> txHashes = new ArrayList<>();
        for (Transaction tx : transactions) {
            txHashes.add(tx.getDataHash() != null ? tx.getDataHash() : tx.getTransactionId());
        }
        this.merkleRoot = MerkleTree.computeRoot(txHashes);

        do {
            nonce++;
            String blockData = index + timestamp + previousHash + merkleRoot + nonce;
            this.hash = CryptoUtils.sha256(blockData);
        } while (!hash.startsWith(target));
    }

    public boolean isValid(int difficulty) throws Exception {
        String target = "0".repeat(difficulty);
        if (!hash.startsWith(target)) return false;
        String expectedHash = calculateHash();
        return expectedHash.equals(hash);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"index\": ").append(index).append(",\n");
        sb.append("  \"timestamp\": ").append(timestamp).append(",\n");
        sb.append("  \"nonce\": ").append(nonce).append(",\n");
        sb.append("  \"minedBy\": \"").append(minedBy).append("\",\n");
        sb.append("  \"previousHash\": \"").append(previousHash).append("\",\n");
        sb.append("  \"merkleRoot\": \"").append(merkleRoot).append("\",\n");
        sb.append("  \"hash\": \"").append(hash).append("\",\n");
        sb.append("  \"transactions\": [\n");
        for (int i = 0; i < transactions.size(); i++) {
            sb.append("    ").append(transactions.get(i).toJson().replace("\n", "\n    "));
            if (i < transactions.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    // Getters and setters
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }
    public String getMerkleRoot() { return merkleRoot; }
    public void setMerkleRoot(String merkleRoot) { this.merkleRoot = merkleRoot; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public int getNonce() { return nonce; }
    public void setNonce(int nonce) { this.nonce = nonce; }
    public String getMinedBy() { return minedBy; }
    public void setMinedBy(String minedBy) { this.minedBy = minedBy; }
}
