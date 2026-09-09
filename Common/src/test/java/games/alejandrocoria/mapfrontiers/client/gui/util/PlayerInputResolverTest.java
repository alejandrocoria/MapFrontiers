package games.alejandrocoria.mapfrontiers.client.gui.util;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerInputResolverTest {
    private static final UUID PLAYER_UUID = UUID.fromString("380df991-f603-344c-a090-369bad2a924a");
    private static final PlayerId PLAYER_ID = new PlayerId(PLAYER_UUID);

    @Test
    void allowingOfflineUuidNormalizesSeparators() {
        PlayerInputResolver.Result result = PlayerInputResolver.resolveAllowingOfflineUuid(
                "{380df991-f603-344c-a090-369bad2a924a}", null, new PlayerNameRepository());

        assertTrue(result.isSuccess());
        assertEquals(PLAYER_ID, result.requirePlayerId());
        assertNull(result.connectedUsername());
    }

    @Test
    void rejectsUuidWithIncorrectLength() {
        PlayerInputResolver.Result result = PlayerInputResolver.resolveAllowingOfflineUuid(
                "380df991-f603-344c-a090-369bad2a924", null, new PlayerNameRepository());

        assertFalse(result.isSuccess());
        assertEquals(PlayerInputResolver.Failure.UUID_SIZE, result.failure());
    }

    @Test
    void rejectsUsernameWithoutConnectedPlayer() {
        PlayerInputResolver.Result result = PlayerInputResolver.resolveOnline("Player", null,
                new PlayerNameRepository());

        assertFalse(result.isSuccess());
        assertEquals(PlayerInputResolver.Failure.USER_NOT_FOUND, result.failure());
    }

    @Test
    void offlineUuidPolicyAllowsUuidWithoutConnection() {
        PlayerInputResolver.Result result = PlayerInputResolver.resolveAllowingOfflineUuid(PLAYER_UUID.toString(), null,
                new PlayerNameRepository());

        assertTrue(result.isSuccess());
        assertEquals(PLAYER_ID, result.requirePlayerId());
    }

    @Test
    void onlinePolicyRejectsUuidWithoutConnection() {
        PlayerInputResolver.Result result = PlayerInputResolver.resolveOnline(PLAYER_UUID.toString(), null,
                new PlayerNameRepository());

        assertFalse(result.isSuccess());
        assertEquals(PlayerInputResolver.Failure.USER_NOT_FOUND, result.failure());
    }
}
