package games.alejandrocoria.mapfrontiers.testutil;

import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Translation-only test world; production delegates wrapping to Toroidal World's API. */
public record PeriodicGeometry(int periodX, int periodZ) implements WorldGeometry {
    @Override
    public BlockPos nearestCopy(BlockPos reference, BlockPos target) {
        return new BlockPos(nearest(reference.getX(), target.getX(), periodX), target.getY(),
                nearest(reference.getZ(), target.getZ(), periodZ));
    }

    private static int nearest(int reference, int target, int period) {
        if (period == 0) return target;
        return target + (int) Math.floor(((double) reference - target) / period + 0.5) * period;
    }

    @Override
    public BlockPos shortestDelta(BlockPos from, BlockPos to) {
        BlockPos copy = nearestCopy(from, to);
        return new BlockPos(copy.getX() - from.getX(), 0, copy.getZ() - from.getZ());
    }

    @Override
    public ChunkPos foldChunk(ChunkPos chunk) {
        return new ChunkPos(periodX == 0 ? chunk.x() : Math.floorMod(chunk.x(), periodX / 16),
                periodZ == 0 ? chunk.z() : Math.floorMod(chunk.z(), periodZ / 16));
    }

    @Override
    public ChunkPos nearestChunkCopy(ChunkPos reference, ChunkPos target) {
        return new ChunkPos(nearest(reference.x(), target.x(), periodX / 16),
                nearest(reference.z(), target.z(), periodZ / 16));
    }
}
