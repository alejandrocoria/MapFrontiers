package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathRepeatedMarkerSelectorTest {
    @Test
    void getMarkerCount_invalidOrShortInput_returnsZero() {
        assertEquals(0, PathRepeatedMarkerSelector.getMarkerCount(0.0, 100.0, 10));
        assertEquals(0, PathRepeatedMarkerSelector.getMarkerCount(10.0, 100.0, 0));
        assertEquals(0, PathRepeatedMarkerSelector.getMarkerCount(100.0, 20.0, 10));
    }

    @Test
    void getMarkerCount_longSegment_clampsToAvailablePositions() {
        assertEquals(4, PathRepeatedMarkerSelector.getMarkerCount(20.0, 100.0, 20));
        assertEquals(6, PathRepeatedMarkerSelector.getMarkerCount(1.0, 100.0, 6));
    }

    @Test
    void getTargetSpacing_zoomScaleAndMarkerMultiplier_preservesLegacyFormula() {
        assertEquals(2048.0, PathRepeatedMarkerSelector.getTargetSpacing(2, 1, 1.0));
        assertEquals(576.0, PathRepeatedMarkerSelector.getTargetSpacing(32, 3, 1.5));
    }

    @Test
    void getMarkerIndex_evenDistribution_preservesLegacyRounding() {
        assertEquals(2, PathRepeatedMarkerSelector.getMarkerIndex(1, 3, 10));
        assertEquals(5, PathRepeatedMarkerSelector.getMarkerIndex(2, 3, 10));
        assertEquals(7, PathRepeatedMarkerSelector.getMarkerIndex(3, 3, 10));
    }

    @Test
    void getMarkerIndex_largeFloatRounding_repeatsAdjacentIndex() {
        int first = PathRepeatedMarkerSelector.getMarkerIndex(16_777_216, 20_000_000, 20_000_000);
        int second = PathRepeatedMarkerSelector.getMarkerIndex(16_777_217, 20_000_000, 20_000_000);
        int third = PathRepeatedMarkerSelector.getMarkerIndex(16_777_218, 20_000_000, 20_000_000);

        assertEquals(first, second);
        assertTrue(third >= second);
    }

    @Test
    void getMarkerIndex_consecutiveDedup_matchesLegacySetDedup() {
        for (int available = 1; available <= 100; available++) {
            for (int markerCount = 0; markerCount <= available; markerCount++) {
                assertEquals(legacySelection(available, markerCount),
                        consecutiveSelection(available, markerCount));
            }
        }
    }

    private static List<Integer> legacySelection(int available, int markerCount) {
        List<Integer> selected = new ArrayList<>();
        Set<Integer> added = new HashSet<>();
        for (int marker = 1; marker <= markerCount; marker++) {
            int index = PathRepeatedMarkerSelector.getMarkerIndex(marker, markerCount, available);
            if (added.add(index)) {
                selected.add(index);
            }
        }
        return selected;
    }

    private static List<Integer> consecutiveSelection(int available, int markerCount) {
        List<Integer> selected = new ArrayList<>();
        int previous = -1;
        for (int marker = 1; marker <= markerCount; marker++) {
            int index = PathRepeatedMarkerSelector.getMarkerIndex(marker, markerCount, available);
            if (index != previous) {
                selected.add(index);
                previous = index;
            }
        }
        return selected;
    }
}
