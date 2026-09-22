package games.alejandrocoria.mapfrontiers.platform;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.api.v1.ToroidalWorldApi;
import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.core.BlockPos;
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
        public boolean hasWrappedAxes() {
            return true;
        }
    }

    private FabricToroidalWorldServer() {
    }
}
