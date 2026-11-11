package com.medchain.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeerNetwork {
    private final List<Node> peers;
    private final String networkName;

    public PeerNetwork(String networkName) {
        this.networkName = networkName;
        this.peers = new ArrayList<>();
    }

    public synchronized void addPeer(Node node) {
        peers.add(node);
        System.out.println("[PeerNetwork] Node joined: " + node.getNodeName() + " (id=" + node.getNodeId() + ")");
    }

    public synchronized void removePeer(String nodeId) {
        peers.removeIf(n -> n.getNodeId().equals(nodeId));
    }

    public synchronized List<Node> getPeers() {
        return Collections.unmodifiableList(peers);
    }

    public int getPeerCount() {
        return peers.size();
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"networkName\": \"").append(networkName).append("\",\n");
        sb.append("  \"peerCount\": ").append(peers.size()).append(",\n");
        sb.append("  \"peers\": [\n");
        for (int i = 0; i < peers.size(); i++) {
            sb.append("    ").append(peers.get(i).getStatus().replace("\n", "\n    "));
            if (i < peers.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
}
