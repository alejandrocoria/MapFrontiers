package games.alejandrocoria.mapfrontiers.common.util;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class UUIDHelper {
    public static UUID getUUIDFromName(String username) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        GameProfile profile = server.getServer().getPlayerProfileCache().getGameProfileForUsername(username);
        if (profile != null)
            return profile.getId();

        return null;
    }

    public static String getNameFromUUID(UUID uuid) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        GameProfile profile = server.getServer().getPlayerProfileCache().getProfileByUUID(uuid);
        if (profile != null)
            return profile.getName();

        return null;
    }

    private UUIDHelper() {
    }
}
