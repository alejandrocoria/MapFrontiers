package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.core.BlockPos;

/** Keeps the cursor's unwrapped position independent of the snapped edit result. */
final class PointDragState {
    private BlockPos position;

    PointDragState(BlockPos selectedPoint) {
        position = selectedPoint;
    }

    BlockPos update(BlockPos cursor, WorldGeometry geometry) {
        position = geometry.nearestCopy(position, cursor);
        return position;
    }
}
