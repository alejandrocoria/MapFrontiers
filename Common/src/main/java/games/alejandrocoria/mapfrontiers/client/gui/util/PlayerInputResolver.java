package games.alejandrocoria.mapfrontiers.client.gui.util;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public final class PlayerInputResolver {
    public enum Failure {
        UUID_SIZE("mapfrontiers.new_user_error_uuid_size"),
        UUID_FORMAT("mapfrontiers.new_user_error_uuid_format"),
        USER_NOT_FOUND("mapfrontiers.new_user_shared_error_user_not_found");

        private final String translationKey;

        Failure(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getMessage() {
            return Component.translatable(translationKey);
        }
    }

    public record Result(@Nullable PlayerId playerId, @Nullable String connectedUsername, @Nullable Failure failure) {
        public boolean isSuccess() {
            return failure == null;
        }

        public PlayerId requirePlayerId() {
            if (playerId == null) {
                throw new IllegalStateException("Player input resolution did not produce a player ID");
            }

            return playerId;
        }

        public String requireConnectedUsername() {
            if (connectedUsername == null) {
                throw new IllegalStateException("Player input resolution did not produce a connected username");
            }

            return connectedUsername;
        }
    }

    public static Result resolveOnline(String input, @Nullable ClientPacketListener connection,
                                       PlayerNameRepository playerNames) {
        return resolve(input, connection, playerNames, true);
    }

    public static Result resolveAllowingOfflineUuid(String input, @Nullable ClientPacketListener connection,
                                                    PlayerNameRepository playerNames) {
        return resolve(input, connection, playerNames, false);
    }

    private static Result resolve(String input, @Nullable ClientPacketListener connection,
                                  PlayerNameRepository playerNames, boolean requireOnline) {
        if (input.length() < 28) {
            PlayerInfo playerInfo = connection == null ? null : connection.getPlayerInfo(input);
            return playerInfo == null ? failure(Failure.USER_NOT_FOUND) : fromPlayerInfo(playerInfo, playerNames);
        }

        String compactUuid = input.replaceAll("[^0-9a-fA-F]", "");
        if (compactUuid.length() != 32) {
            return failure(Failure.UUID_SIZE);
        }

        String uuidText = compactUuid.substring(0, 8) + "-" + compactUuid.substring(8, 12) + "-"
                + compactUuid.substring(12, 16) + "-" + compactUuid.substring(16, 20) + "-"
                + compactUuid.substring(20, 32);
        try {
            UUID uuid = UUID.fromString(uuidText);
            PlayerInfo playerInfo = connection == null ? null : connection.getPlayerInfo(uuid);
            if (playerInfo != null) {
                return fromPlayerInfo(playerInfo, playerNames);
            }

            return requireOnline ? failure(Failure.USER_NOT_FOUND) : success(new PlayerId(uuid), null);
        } catch (IllegalArgumentException e) {
            return failure(Failure.UUID_FORMAT);
        }
    }

    private static Result fromPlayerInfo(PlayerInfo playerInfo, PlayerNameRepository playerNames) {
        PlayerId playerId = new PlayerId(playerInfo.getProfile().id());
        String username = playerInfo.getProfile().name();
        if (username == null || username.isBlank()) {
            return failure(Failure.USER_NOT_FOUND);
        }

        playerNames.observe(playerId, username, PlayerNameSource.CONNECTED_PROFILE);
        return success(playerId, username);
    }

    private static Result success(PlayerId playerId, @Nullable String connectedUsername) {
        return new Result(playerId, connectedUsername, null);
    }

    private static Result failure(Failure failure) {
        return new Result(null, null, failure);
    }

    private PlayerInputResolver() {
    }
}
