package games.alejandrocoria.mapfrontiers.server.identity;

import com.mojang.authlib.GameProfile;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerIdLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.StringUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public final class ServerPlayerIdLookup implements PlayerIdLookup {
    private final PlayerIdLookup localProfiles;
    private final PlayerIdLookup officialProfiles;

    public ServerPlayerIdLookup(MinecraftServer server) {
        this(username -> findLocalProfile(server, username),
                username -> findOfficialProfile(server, username));
    }

    ServerPlayerIdLookup(PlayerIdLookup localProfiles, PlayerIdLookup officialProfiles) {
        this.localProfiles = Objects.requireNonNull(localProfiles, "localProfiles");
        this.officialProfiles = Objects.requireNonNull(officialProfiles, "officialProfiles");
    }

    @Override
    @Nullable
    public PlayerId findByName(String username) {
        String normalized = username.trim();
        if (!StringUtil.isValidPlayerName(normalized)) {
            return null;
        }

        try {
            PlayerId local = localProfiles.findByName(normalized);
            return local == null ? officialProfiles.findByName(normalized) : local;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static PlayerId findLocalProfile(MinecraftServer server, String username) {
        GameProfile singleplayerProfile = server.getSingleplayerProfile();
        if (singleplayerProfile != null && singleplayerProfile.name().equalsIgnoreCase(username)) {
            return new PlayerId(singleplayerProfile.id());
        }

        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        return player == null ? null : new PlayerId(player.getGameProfile().id());
    }

    @Nullable
    private static PlayerId findOfficialProfile(MinecraftServer server, String username) {
        return server.services().profileRepository().findProfileByName(username)
                .map(NameAndId::new)
                .map(profile -> new PlayerId(profile.id()))
                .orElse(null);
    }
}
