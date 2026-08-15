package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import java.util.ArrayList;
import java.util.List;

final class PathMarkerBandPlan {
    private static final int MIN_ZOOM = 2;
    private static final int MAX_ZOOM = 16384;
    private static final List<ZoomBand> ORIGINAL_BANDS = createOriginalBands();

    record Entry(int ownerIndex, int minZoom, int maxZoom, int markerCount) {
    }

    private final List<Entry> entries;

    private PathMarkerBandPlan(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    static PathMarkerBandPlan create(double length, int availablePositionCount,
                                     int markerScale, double spacingMultiplier) {
        List<Entry> entries = new ArrayList<>(ORIGINAL_BANDS.size());
        Entry pending = null;
        for (int index = 0; index < ORIGINAL_BANDS.size(); index++) {
            ZoomBand band = ORIGINAL_BANDS.get(index);
            double targetSpacing = PathRepeatedMarkerSelector.getTargetSpacing(
                    band.minZoom(), markerScale, spacingMultiplier);
            int markerCount = PathRepeatedMarkerSelector.getMarkerCount(
                    targetSpacing, length, availablePositionCount);
            if (markerCount == 0) {
                if (pending != null) {
                    entries.add(pending);
                    pending = null;
                }
                continue;
            }

            // Within one segment, equal counts select equal indices because the available positions are shared.
            if (pending != null && pending.markerCount() == markerCount) {
                pending = new Entry(pending.ownerIndex(), pending.minZoom(), band.maxZoom(), markerCount);
            } else {
                if (pending != null) {
                    entries.add(pending);
                }
                pending = new Entry(index, band.minZoom(), band.maxZoom(), markerCount);
            }
        }
        if (pending != null) {
            entries.add(pending);
        }
        return new PathMarkerBandPlan(entries);
    }

    static int originalBandCount() {
        return ORIGINAL_BANDS.size();
    }

    List<Entry> entries() {
        return entries;
    }

    private static List<ZoomBand> createOriginalBands() {
        List<ZoomBand> bands = new ArrayList<>();
        for (int minZoom = MIN_ZOOM; minZoom <= MAX_ZOOM; minZoom *= 2) {
            int maxZoom = minZoom == MAX_ZOOM ? 0 : minZoom * 2 - 1;
            bands.add(new ZoomBand(minZoom, maxZoom));
        }
        return List.copyOf(bands);
    }

    private record ZoomBand(int minZoom, int maxZoom) {
    }
}
