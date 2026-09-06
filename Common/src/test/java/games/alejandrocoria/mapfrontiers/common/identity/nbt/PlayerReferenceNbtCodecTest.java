package games.alejandrocoria.mapfrontiers.common.identity.nbt;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerReferenceNbtCodecTest {
    private static final UUID UUID_VALUE = UUID.fromString("22527f7f-b0ec-4a63-93b8-df8b6a33399c");
    private static final PlayerId PLAYER_ID = new PlayerId(UUID_VALUE);

    @Test
    void uuidAndUsernameUseUuidAndObserveHint() {
        PlayerNameRepository names = new PlayerNameRepository();
        CompoundTag nbt = reference(UUID_VALUE.toString(), " Alice ");

        PlayerReferenceNbtCodec.ReadResult result = PlayerReferenceNbtCodec.read(nbt,
                PlayerReferenceNbtReadContext.uuidOnly(names));

        assertEquals(PLAYER_ID, result.playerId());
        assertFalse(result.repaired());
        assertEquals("Alice", names.resolveName(PLAYER_ID));
    }

    @Test
    void uuidOnlyDoesNotInventName() {
        PlayerNameRepository names = new PlayerNameRepository();
        CompoundTag nbt = new CompoundTag();
        nbt.putString("UUID", UUID_VALUE.toString());

        PlayerReferenceNbtCodec.ReadResult result = PlayerReferenceNbtCodec.read(nbt,
                PlayerReferenceNbtReadContext.uuidOnly(names));

        assertEquals(PLAYER_ID, result.playerId());
        assertFalse(result.repaired());
        assertNull(names.resolveName(PLAYER_ID));
    }

    @Test
    void usernameOnlyUsesLookupAndMarksRepair() {
        PlayerNameRepository names = new PlayerNameRepository();
        CompoundTag nbt = new CompoundTag();
        nbt.putString("username", "Alice");

        PlayerReferenceNbtCodec.ReadResult result = PlayerReferenceNbtCodec.read(nbt,
                new PlayerReferenceNbtReadContext(names, username -> PLAYER_ID));

        assertEquals(PLAYER_ID, result.playerId());
        assertTrue(result.repaired());
        assertEquals("Alice", names.resolveName(PLAYER_ID));
    }

    @Test
    void usernameOnlyRequiresSuccessfulLookup() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("username", "Alice");

        assertThrows(InvalidNbtFormatException.class, () -> PlayerReferenceNbtCodec.read(nbt,
                PlayerReferenceNbtReadContext.uuidOnly(new PlayerNameRepository())));
        assertThrows(InvalidNbtFormatException.class, () -> PlayerReferenceNbtCodec.read(nbt,
                new PlayerReferenceNbtReadContext(new PlayerNameRepository(), username -> null)));
    }

    @Test
    void rejectsEmptyReferenceAndMalformedPresentUuid() {
        assertThrows(InvalidNbtFormatException.class, () -> PlayerReferenceNbtCodec.read(new CompoundTag(),
                new PlayerReferenceNbtReadContext(new PlayerNameRepository(), username -> PLAYER_ID)));

        CompoundTag malformed = reference("not-a-uuid", "Alice");
        assertThrows(InvalidNbtFormatException.class, () -> PlayerReferenceNbtCodec.read(malformed,
                new PlayerReferenceNbtReadContext(new PlayerNameRepository(), username -> PLAYER_ID)));
    }

    @Test
    void writerAlwaysWritesUuidAndOnlyKnownName() {
        CompoundTag named = new CompoundTag();
        PlayerReferenceNbtCodec.write(named, PLAYER_ID, ignored -> "Alice");
        assertEquals(UUID_VALUE.toString(), named.getStringOr("UUID", ""));
        assertEquals("Alice", named.getStringOr("username", ""));

        CompoundTag unnamed = new CompoundTag();
        unnamed.putString("username", "stale");
        PlayerReferenceNbtCodec.write(unnamed, PLAYER_ID, ignored -> null);
        assertEquals(UUID_VALUE.toString(), unnamed.getStringOr("UUID", ""));
        assertFalse(unnamed.contains("username"));
    }

    @Test
    void legacyReaderCanReadNewWriterOutput() {
        CompoundTag nbt = new CompoundTag();
        PlayerReferenceNbtCodec.write(nbt, PLAYER_ID, ignored -> "Alice");

        SettingsUser legacy = new SettingsUser();
        legacy.readFromNBT(nbt);
        assertEquals(UUID_VALUE, legacy.uuid);
        assertEquals("Alice", legacy.username);
    }

    @Test
    void legacyAdapterMaterializesAndWritesRepositoryName() {
        PlayerNameRepository names = new PlayerNameRepository();
        names.observe(PLAYER_ID, "CurrentName", PlayerNameSource.MINECRAFT_CACHE);
        CompoundTag nbt = reference(UUID_VALUE.toString(), "OldHint");

        SettingsUser adapter = new SettingsUser();
        assertFalse(adapter.readFromNBT(nbt, PlayerReferenceNbtReadContext.uuidOnly(names)));
        assertEquals("CurrentName", adapter.username);

        CompoundTag rewritten = new CompoundTag();
        adapter.writeToNBT(rewritten, names);
        assertEquals("CurrentName", rewritten.getStringOr("username", ""));
        assertEquals(UUID_VALUE.toString(), rewritten.getStringOr("UUID", ""));
    }

    private static CompoundTag reference(String uuid, String username) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("UUID", uuid);
        nbt.putString("username", username);
        return nbt;
    }
}
