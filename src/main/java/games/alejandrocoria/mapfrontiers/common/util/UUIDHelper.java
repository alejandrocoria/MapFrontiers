package games.alejandrocoria.mapfrontiers.common.util;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.authlib.GameProfile;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

@ParametersAreNonnullByDefault
public class UUIDHelper {
    public static UUID getUUIDFromName(String username) {
        GameProfile profile = null;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            profile = server.getServer().getPlayerProfileCache().getGameProfileForUsername(username);
        } else {
            NetHandlerPlayClient handler = Minecraft.getMinecraft().getConnection();
            if (handler != null) {
                NetworkPlayerInfo playerInfo = handler.getPlayerInfo(username);
                if (playerInfo != null) {
                    profile = playerInfo.getGameProfile();
                }
            }
        }

        if (profile != null)
            return profile.getId();

        return null;
    }

    public static String getNameFromUUID(UUID uuid) {
        GameProfile profile = null;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            profile = server.getServer().getPlayerProfileCache().getProfileByUUID(uuid);
        } else {
            NetHandlerPlayClient handler = Minecraft.getMinecraft().getConnection();
            if (handler != null) {
                NetworkPlayerInfo playerInfo = handler.getPlayerInfo(uuid);
                if (playerInfo != null) {
                    profile = playerInfo.getGameProfile();
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
