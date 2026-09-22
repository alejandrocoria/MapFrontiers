package games.alejandrocoria.mapfrontiers.platform;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.api.v1.ToroidalWorldApi;
import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class FabricToroidalWorldServer {
    static WorldGeometry geometryOf(Level level) {
        return ToroidalWorldApi.shapeOf(level).<WorldGeometry>map(Shape::new).orElse(WorldGeometry.FLAT);
    }

    private record Shape(ToroidalShape shape) implements WorldGeometry {
        @Override
        public BlockPos nearestCopy(BlockPos reference, BlockPos target) {
            return shape.nearestCopy(reference, target);
        }

        @Override
        public BlockPos shortestDelta(BlockPos from, BlockPos to) {
            Vec3 delta = shape.shortestDelta(Vec3.atLowerCornerOf(from), Vec3.atLowerCornerOf(to));
            return BlockPos.containing(delta.x, 0.0, delta.z);
        }

        @Override
        public int periodX() {
            return shape.loops(Direction.Axis.X) ? shape.widthBlocks(Direction.Axis.X) : 0;
        }

        @Override
        public int periodZ() {
            return shape.loops(Direction.Axis.Z) ? shape.widthBlocks(Direction.Axis.Z) : 0;
        }

        @Override
        public ChunkPos foldChunk(ChunkPos chunk) {
            return shape.fold(chunk);
        }

        @Override
        public ChunkPos nearestChunkCopy(ChunkPos reference, ChunkPos target) {
            BlockPos from = new BlockPos(reference.getMinBlockX(), 0, reference.getMinBlockZ());
            BlockPos to = new BlockPos(target.getMinBlockX(), 0, target.getMinBlockZ());
            return shape.shiftToNearestCopy(from, to).apply(target);
        }
    }

    private FabricToroidalWorldServer() {
    }
}
