package games.alejandrocoria.mapfrontiers.client.territory.frontier;

final class PathRepeatedMarkerSelector {
    private static final int BASE_MARKER_SCALE = 2;
    private static final double SPACING_ZOOM_REFERENCE = 8192.0;

    private PathRepeatedMarkerSelector() {
    }

    static int getMarkerCount(double targetSpacing, double length, int availablePositionCount) {
        if (targetSpacing <= 0.0 || availablePositionCount <= 0) {
            return 0;
        }

        int intervalCount = (int) Math.round(length / targetSpacing);
        return Math.max(0, Math.min(intervalCount - 1, availablePositionCount));
    }

    static double getTargetSpacing(int minZoom, int markerScale, double spacingMultiplier) {
        return SPACING_ZOOM_REFERENCE / minZoom * markerScale / BASE_MARKER_SCALE * spacingMultiplier;
    }

    static int getMarkerIndex(int markerOrdinal, int markerCount, int availablePositionCount) {
        double step = (availablePositionCount + 1) / (double) (markerCount + 1);
        int markerIndex = Math.round((float) (markerOrdinal * step)) - 1;
        return Math.max(0, Math.min(markerIndex, availablePositionCount - 1));
    }
}
