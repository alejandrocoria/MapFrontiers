package games.alejandrocoria.mapfrontiers.common.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class UUIDHelper {
    public static UUID getUUIDFromName(String username, @Nullable MinecraftServer server) {
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(username);
            if (player != null) {
                return player.nameAndId().id();
            }
        } else {
            ClientPacketListener handler = Minecraft.getInstance().getConnection();
            if (handler != null) {
                PlayerInfo playerInfo = handler.getPlayerInfo(username);
                if (playerInfo != null) {
                    return playerInfo.getProfile().id();
                }
            }
        }

        return null;
    }

    public static String getNameFromUUID(UUID uuid, @Nullable MinecraftServer server) {
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                return player.nameAndId().name();
            }
        } else {
            ClientPacketListener handler = Minecraft.getInstance().getConnection();
            if (handler != null) {
                PlayerInfo playerInfo = handler.getPlayerInfo(uuid);
                if (playerInfo != null) {
                    return playerInfo.getProfile().name();
                }
            }
        }

        return null;
    }

    public static UUID fromBytes(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    public static void toBytes(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    private UUIDHelper() {

    }
}
