package games.alejandrocoria.mapfrontiers.common.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockPosHelper {
    public static BlockPos atY(BlockPos pos, int y) {
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    public static BlockPos toBlockPos(ChunkPos chunk, int y) {
        return new BlockPos(chunk.getMinBlockX(), y, chunk.getMinBlockZ());
    }

    private BlockPosHelper() {

    }
}
