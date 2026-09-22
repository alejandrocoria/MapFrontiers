package games.alejandrocoria.mapfrontiers.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Geometry helpers for a level whose horizontal axes may wrap. */
public interface WorldGeometry {
    WorldGeometry FLAT = new WorldGeometry() {
        @Override
        public BlockPos nearestCopy(BlockPos reference, BlockPos target) {
            return target;
        }

        @Override
        public BlockPos shortestDelta(BlockPos from, BlockPos to) {
            return new BlockPos(to.getX() - from.getX(), 0, to.getZ() - from.getZ());
        }
    };

    BlockPos nearestCopy(BlockPos reference, BlockPos target);

    BlockPos shortestDelta(BlockPos from, BlockPos to);

    /** Period in blocks, or zero for an axis without wrapping. */
    default int periodX() { return 0; }

    default int periodZ() { return 0; }

    default ChunkPos foldChunk(ChunkPos chunk) { return chunk; }

    default ChunkPos nearestChunkCopy(ChunkPos reference, ChunkPos target) { return target; }

    default boolean hasWrappedAxes() {
        return periodX() != 0 || periodZ() != 0;
    }
}
