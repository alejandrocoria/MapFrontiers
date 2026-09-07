package games.alejandrocoria.mapfrontiers.common.identity.network;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class PlayerIdNetworkCodec {
    public static PlayerId read(FriendlyByteBuf buf) {
        return new PlayerId(UUIDHelper.fromBytes(buf));
    }

    public static void write(FriendlyByteBuf buf, PlayerId playerId) {
        UUIDHelper.toBytes(buf, playerId.uuid());
    }

    private PlayerIdNetworkCodec() {
    }
}
