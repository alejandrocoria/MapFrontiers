package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Resolves a wrapped user edit; stored chunk operations themselves remain exact. */
record ChunkEdit(ChunkPos position, List<ChunkPos> existingCopies) {
    static ChunkEdit resolve(WorldGeometry geometry, Set<ChunkPos> chunks, ChunkPos target, boolean alignWithFrontier) {
        ChunkPos folded = geometry.foldChunk(target);
        List<ChunkPos> existing = new ArrayList<>();
        ChunkPos position = target;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (ChunkPos chunk : chunks) {
            if (geometry.foldChunk(chunk).equals(folded)) existing.add(chunk);
            if (alignWithFrontier) {
                ChunkPos copy = geometry.nearestChunkCopy(chunk, target);
                double dx = (double) copy.x() - chunk.x();
                double dz = (double) copy.z() - chunk.z();
                double distance = dx * dx + dz * dz;
                if (distance < closestDistance || (distance == closestDistance && comesBefore(copy, position))) {
                    position = copy;
                    closestDistance = distance;
                }
            }
        }
        return new ChunkEdit(position, existing);
    }

    private static boolean comesBefore(ChunkPos left, ChunkPos right) {
        return left.x() < right.x() || (left.x() == right.x() && left.z() < right.z());
    }
}
