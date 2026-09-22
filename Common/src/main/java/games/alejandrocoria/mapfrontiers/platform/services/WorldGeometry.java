package games.alejandrocoria.mapfrontiers.platform.services;

import net.minecraft.core.BlockPos;

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
}
