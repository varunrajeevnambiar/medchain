package com.medchain.network;

import com.medchain.blockchain.Blockchain;
import com.medchain.contract.AccessControlContract;
import com.medchain.storage.EncryptedDataStore;

import java.util.UUID;

public class Node {
    private final String nodeId;
    private final String nodeName;
    private final Blockchain blockchain;
    private final EncryptedDataStore dataStore;
    private final AccessControlContract contract;

    public Node(String nodeName) {
        this.nodeId = UUID.randomUUID().toString();
        this.nodeName = nodeName;
        this.blockchain = new Blockchain(2, 5);
        this.dataStore = new EncryptedDataStore();
        this.contract = new AccessControlContract(this.blockchain, this.dataStore);
    }

    public Node(String nodeName, int difficulty, int blockSize) {
        this.nodeId = UUID.randomUUID().toString();
        this.nodeName = nodeName;
        this.blockchain = new Blockchain(difficulty, blockSize);
        this.dataStore = new EncryptedDataStore();
        this.contract = new AccessControlContract(this.blockchain, this.dataStore);
    }

    public String getNodeId() { return nodeId; }
    public String getNodeName() { return nodeName; }
    public Blockchain getBlockchain() { return blockchain; }
    public EncryptedDataStore getDataStore() { return dataStore; }
    public AccessControlContract getContract() { return contract; }

    public String getStatus() {
        return "{\n" +
               "  \"nodeId\": \"" + nodeId + "\",\n" +
               "  \"nodeName\": \"" + nodeName + "\",\n" +
               "  \"chainHeight\": " + blockchain.getChain().size() + ",\n" +
               "  \"difficulty\": " + blockchain.getDifficulty() + ",\n" +
               "  \"pendingTransactions\": " + blockchain.getPendingTransactions().size() + ",\n" +
               "  \"chainValid\": " + blockchain.isChainValid() + "\n" +
               "}";
    }
}
