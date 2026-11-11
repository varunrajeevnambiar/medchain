package com.medchain.blockchain;

import com.medchain.crypto.CryptoUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Blockchain {
    private List<Block> chain;
    private List<Transaction> pendingTransactions;
    private int difficulty;
    private int blockSize;

    public Blockchain(int difficulty, int blockSize) {
        this.difficulty = difficulty;
        this.blockSize = blockSize;
        this.chain = new ArrayList<>();
        this.pendingTransactions = new ArrayList<>();
        chain.add(createGenesisBlock());
    }

    public Blockchain() {
        this(2, 5);
    }

    private Block createGenesisBlock() {
        Block genesis = new Block(0, "0000000000000000000000000000000000000000000000000000000000000000");
        genesis.setMinedBy("genesis");
        try {
            Transaction genesisTx = new Transaction(TransactionType.REGISTER_PATIENT,
                    "SYSTEM", "SYSTEM", CryptoUtils.sha256("genesis"), "MedChain Genesis Block");
            genesisTx.setValid(true);
            genesis.addTransaction(genesisTx);
            genesis.mine(difficulty);
        } catch (Exception e) {
            genesis.setHash("genesis-hash");
        }
        return genesis;
    }

    public synchronized void addTransaction(Transaction tx) {
        if (tx == null) return;
        pendingTransactions.add(tx);
        if (pendingTransactions.size() >= blockSize) {
            try {
                minePendingTransactions("system");
            } catch (Exception e) {
                System.err.println("Auto-mining failed: " + e.getMessage());
            }
        }
    }

    public synchronized Block minePendingTransactions(String minerAddress) throws Exception {
        if (pendingTransactions.isEmpty()) {
            System.out.println("No pending transactions to mine.");
            return null;
        }
        Block newBlock = new Block(chain.size(), getLatestBlock().getHash());
        newBlock.setMinedBy(minerAddress);

        List<Transaction> toMine = new ArrayList<>();
        int count = Math.min(blockSize, pendingTransactions.size());
        for (int i = 0; i < count; i++) {
            toMine.add(pendingTransactions.get(i));
        }

        for (Transaction tx : toMine) {
            newBlock.addTransaction(tx);
        }

        System.out.println("  Mining block #" + newBlock.getIndex() + " with " + toMine.size() + " transactions...");
        newBlock.mine(difficulty);
        System.out.println("  Block mined! Hash: " + newBlock.getHash());

        chain.add(newBlock);
        pendingTransactions.subList(0, count).clear();
        return newBlock;
    }

    public synchronized boolean isChainValid() {
        try {
            for (int i = 1; i < chain.size(); i++) {
                Block current = chain.get(i);
                Block previous = chain.get(i - 1);

                // Check previous hash linkage
                if (!current.getPreviousHash().equals(previous.getHash())) {
                    System.err.println("Chain broken at block " + i + ": previousHash mismatch");
                    return false;
                }

                // Check block hash integrity
                String expectedHash = current.calculateHash();
                if (!expectedHash.equals(current.getHash())) {
                    System.err.println("Block " + i + " hash is invalid");
                    return false;
                }

                // Check proof-of-work
                if (!current.getHash().startsWith("0".repeat(difficulty))) {
                    System.err.println("Block " + i + " does not meet proof-of-work");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized List<Transaction> getTransactionsForPatient(String patientId) {
        List<Transaction> result = new ArrayList<>();
        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (patientId.equals(tx.getFromAddress()) || patientId.equals(tx.getToAddress())) {
                    result.add(tx);
                }
            }
        }
        // Also check pending
        for (Transaction tx : pendingTransactions) {
            if (patientId.equals(tx.getFromAddress()) || patientId.equals(tx.getToAddress())) {
                result.add(tx);
            }
        }
        return result;
    }

    public synchronized List<Transaction> getTransactionsForDoctor(String doctorId) {
        List<Transaction> result = new ArrayList<>();
        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (doctorId.equals(tx.getFromAddress()) || doctorId.equals(tx.getToAddress())) {
                    result.add(tx);
                }
            }
        }
        return result;
    }

    public synchronized Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public synchronized Transaction findTransaction(String txId) {
        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (txId.equals(tx.getTransactionId())) return tx;
            }
        }
        for (Transaction tx : pendingTransactions) {
            if (txId.equals(tx.getTransactionId())) return tx;
        }
        return null;
    }

    public synchronized List<Block> getChain() { return chain; }

    public synchronized List<Transaction> getPendingTransactions() { return pendingTransactions; }

    public int getDifficulty() { return difficulty; }
    public int getBlockSize() { return blockSize; }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"difficulty\": ").append(difficulty).append(",\n");
        sb.append("  \"blockSize\": ").append(blockSize).append(",\n");
        sb.append("  \"chainLength\": ").append(chain.size()).append(",\n");
        sb.append("  \"pendingTransactions\": ").append(pendingTransactions.size()).append(",\n");
        sb.append("  \"valid\": ").append(isChainValid()).append(",\n");
        sb.append("  \"chain\": [\n");
        for (int i = 0; i < chain.size(); i++) {
            sb.append("    ").append(chain.get(i).toJson().replace("\n", "\n    "));
            if (i < chain.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
}
