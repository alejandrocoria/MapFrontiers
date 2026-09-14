package games.alejandrocoria.mapfrontiers.server.identity;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class ServerPlayerIdFactory {
    private ServerPlayerIdFactory() {
    }

    public static PlayerId from(ServerPlayer player) {
        return new PlayerId(player.getGameProfile().id());
    }
}
