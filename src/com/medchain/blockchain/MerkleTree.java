package com.medchain.blockchain;

import com.medchain.crypto.CryptoUtils;
import java.util.ArrayList;
import java.util.List;

public class MerkleTree {

    public static String computeRoot(List<String> hashes) throws Exception {
        if (hashes == null || hashes.isEmpty()) {
            return CryptoUtils.sha256("empty");
        }
        if (hashes.size() == 1) {
            return hashes.get(0);
        }

        List<String> currentLevel = new ArrayList<>(hashes);

        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                // If odd number of nodes, duplicate the last one
                String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                String combined = CryptoUtils.sha256(left + right);
                nextLevel.add(combined);
            }
            currentLevel = nextLevel;
        }

        return currentLevel.get(0);
    }
}
