package games.alejandrocoria.mapfrontiers.client.territory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
final class PendingOptimisticFrontierUpdates {
    private final Map<UUID, ArrayDeque<Long>> expectedSyncHashes = new HashMap<>();

    void expect(UUID frontierId, long expectedSyncHash) {
        expectedSyncHashes.computeIfAbsent(frontierId, ignored -> new ArrayDeque<>()).addLast(expectedSyncHash);
    }

    boolean acknowledge(UUID frontierId, long authoritativeSyncHash) {
        ArrayDeque<Long> frontierSyncHashes = expectedSyncHashes.get(frontierId);
        if (frontierSyncHashes == null || frontierSyncHashes.isEmpty()
                || frontierSyncHashes.getFirst() != authoritativeSyncHash) {
            return false;
        }

        frontierSyncHashes.removeFirst();
        if (frontierSyncHashes.isEmpty()) {
            expectedSyncHashes.remove(frontierId);
        }
        return true;
    }

    void clear(UUID frontierId) {
        expectedSyncHashes.remove(frontierId);
    }

    void clearAll() {
        expectedSyncHashes.clear();
    }
}
