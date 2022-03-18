package games.alejandrocoria.mapfrontiers.common.util;

import net.minecraft.core.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockPosHelper {
    public static BlockPos atY(BlockPos pos, int y) {
        return pos.atY(y);
    }

    private BlockPosHelper() {

    }
}
