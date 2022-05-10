package games.alejandrocoria.mapfrontiers.common.util;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class UUIDHelper {
    public static UUID getUUIDFromName(String username) {
        GameProfile profile = null;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            profile = server.getProfileCache().get(username);
        } else {
            ClientPlayNetHandler handler = Minecraft.getInstance().getConnection();
            if (handler != null) {
                NetworkPlayerInfo playerInfo = handler.getPlayerInfo(username);
                if (playerInfo != null) {
                    profile = playerInfo.getProfile();
                }
            }
        }

        if (profile != null)
            return profile.getId();

        return null;
    }

    public static String getNameFromUUID(UUID uuid) {
        GameProfile profile = null;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            profile = server.getProfileCache().get(uuid);
        } else {
            ClientPlayNetHandler handler = Minecraft.getInstance().getConnection();
            if (handler != null) {
                NetworkPlayerInfo playerInfo = handler.getPlayerInfo(uuid);
                if (playerInfo != null) {
                    profile = playerInfo.getProfile();
                }
            }
        }

        if (profile != null)
            return profile.getName();

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
