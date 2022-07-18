package games.alejandrocoria.mapfrontiers.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockPosHelper {
    public static BlockPos atY(BlockPos pos, int y) {
        return pos.atY(y);
    }

    public static BlockPos toBlockPos(ChunkPos chunk, int y) {
        return new BlockPos(chunk.getMinBlockX(), y, chunk.getMinBlockZ());
    }

    private BlockPosHelper() {

    }
}
