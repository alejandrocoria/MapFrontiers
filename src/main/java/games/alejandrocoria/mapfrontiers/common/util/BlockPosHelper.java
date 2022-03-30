package games.alejandrocoria.mapfrontiers.common.util;

import net.minecraft.util.math.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockPosHelper {
    public static BlockPos atY(BlockPos pos, int y) {
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    private BlockPosHelper() {

    }
}
