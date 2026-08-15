package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathMarkerBandPlanTest {
    private static final int[] BAND_MIN_ZOOMS = {
            2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384
    };

    @Test
    void create_emptySelection_hasNoEffectiveBands() {
        assertTrue(PathMarkerBandPlan.create(0.0, 256, 2, 1.0).entries().isEmpty());
        assertTrue(PathMarkerBandPlan.create(10_000.0, 0, 2, 1.0).entries().isEmpty());
    }

    @Test
    void create_saturatedAdjacentBands_usesFirstOwnerAndUnboundedMaximum() {
        PathMarkerBandPlan plan = PathMarkerBandPlan.create(100_000.0, 256, 2, 1.0);

        PathMarkerBandPlan.Entry saturated = plan.entries().getLast();
        assertEquals(4, saturated.ownerIndex());
        assertEquals(32, saturated.minZoom());
        assertEquals(0, saturated.maxZoom());
        assertEquals(256, saturated.markerCount());
    }

    @Test
    void create_expandedPlanMatchesDirectSelectorAcrossInputMatrix() {
        double[] lengths = {0.0, 1.0, 32.0, 3_000.0, 22_500.0, 100_000.0};
        int[] availableCounts = {0, 1, 4, 24, 256};
        int[] markerScales = {1, 2, 5};
        double[] spacingMultipliers = {1.0, 1.5, 2.0};

        for (double length : lengths) {
            for (int availableCount : availableCounts) {
                for (int markerScale : markerScales) {
                    for (double spacingMultiplier : spacingMultipliers) {
                        PathMarkerBandPlan plan = PathMarkerBandPlan.create(
                                length, availableCount, markerScale, spacingMultiplier);
                        for (int minZoom : BAND_MIN_ZOOMS) {
                            int expected = PathRepeatedMarkerSelector.getMarkerCount(
                                    PathRepeatedMarkerSelector.getTargetSpacing(
                                            minZoom, markerScale, spacingMultiplier),
                                    length, availableCount);
                            assertEquals(expected, markerCountAt(plan.entries(), minZoom),
                                    () -> "Unexpected count at zoom " + minZoom
                                            + " for length=" + length
                                            + ", available=" + availableCount
                                            + ", scale=" + markerScale
                                            + ", multiplier=" + spacingMultiplier);
                        }
                    }
                }
            }
        }
    }

    private static int markerCountAt(List<PathMarkerBandPlan.Entry> entries, int zoom) {
        for (PathMarkerBandPlan.Entry entry : entries) {
            if (zoom >= entry.minZoom() && (entry.maxZoom() == 0 || zoom <= entry.maxZoom())) {
                return entry.markerCount();
            }
        }
        return 0;
    }
}
