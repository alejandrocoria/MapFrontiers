package games.alejandrocoria.mapfrontiers.common.identity.nbt;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerReferenceNbtPolicyTest {
    private static final UUID OWNER_UUID = UUID.fromString("22527f7f-b0ec-4a63-93b8-df8b6a33399c");
    private static final UUID COPIED_FROM_ID = UUID.fromString("4b5d6407-cb58-4f3a-bf06-d9ef017d1fc8");

    @Test
    void nameOnlyOwnerIsRepairedForFrontierAndCollection() {
        PlayerReferenceNbtReadContext context = resolvingContext("Owner", OWNER_UUID);

        CompoundTag frontierTag = frontierTag();
        makeNameOnly(frontierTag.getCompoundOrEmpty("owner"), "Owner");
        FrontierData.NbtReadResult frontierResult = FrontierData.readFromNBT(
                frontierTag, MapFrontiers.FRONTIER_DATA_VERSION, context);
        assertTrue(frontierResult.changedDuringLoad());
        assertEquals(OWNER_UUID, frontierResult.frontier().getOwner().uuid());

        CompoundTag collectionTag = collectionTag();
        makeNameOnly(collectionTag.getCompoundOrEmpty("owner"), "Owner");
        CollectionData.NbtReadResult collectionResult = CollectionData.readFromNBT(
                collectionTag, MapFrontiers.FRONTIER_DATA_VERSION, context);
        assertTrue(collectionResult.changedDuringLoad());
        assertEquals(OWNER_UUID, collectionResult.collection().getOwner().uuid());
    }

    @Test
    void unresolvedOwnerInvalidatesEntity() {
        PlayerReferenceNbtReadContext context = PlayerReferenceNbtReadContext.uuidOnly(new PlayerNameRepository());
        CompoundTag frontierTag = frontierTag();
        makeNameOnly(frontierTag.getCompoundOrEmpty("owner"), "Unknown");
        CompoundTag collectionTag = collectionTag();
        makeNameOnly(collectionTag.getCompoundOrEmpty("owner"), "Unknown");

        assertThrows(InvalidNbtFormatException.class, () -> FrontierData.readFromNBT(frontierTag,
                MapFrontiers.FRONTIER_DATA_VERSION, context));
        assertThrows(InvalidNbtFormatException.class, () -> CollectionData.readFromNBT(collectionTag,
                MapFrontiers.FRONTIER_DATA_VERSION, context));
    }

    @Test
    void unresolvedSharedUserIsSkippedWithoutLosingFrontier() {
        FrontierData source = frontier();
        CompoundTag nbt = new CompoundTag();
        source.writeToNBT(nbt, ignored -> "Owner");
        CompoundTag unresolvedUser = new CompoundTag();
        unresolvedUser.putString("username", "Unknown");
        unresolvedUser.put("actions", new ListTag());
        ListTag sharedUsers = new ListTag();
        sharedUsers.add(unresolvedUser);
        nbt.put("usersShared", sharedUsers);

        FrontierData.NbtReadResult result = FrontierData.readFromNBT(nbt, MapFrontiers.FRONTIER_DATA_VERSION,
                PlayerReferenceNbtReadContext.uuidOnly(new PlayerNameRepository()));
        FrontierData decoded = result.frontier();

        assertTrue(result.changedDuringLoad());
        assertTrue(decoded.getUserAccesses() == null || decoded.getUserAccesses().isEmpty());
        assertEquals(source.getId(), decoded.getId());
    }

    @Test
    void unresolvedGroupMemberIsSkippedWithoutLosingGroupActions() {
        SettingsGroup source = new SettingsGroup("Builders", false);
        source.addAction(games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings.Action.CreateGlobalFrontier);
        CompoundTag nbt = new CompoundTag();
        source.writeToNBT(nbt, ignored -> null);
        CompoundTag userTag = new CompoundTag();
        userTag.putString("username", "Unknown");
        ListTag usersTag = new ListTag();
        usersTag.add(userTag);
        nbt.put("users", usersTag);

        SettingsGroup decoded = new SettingsGroup();
        boolean changed = decoded.readFromNBT(nbt, MapFrontiers.SETTINGS_DATA_VERSION,
                PlayerReferenceNbtReadContext.uuidOnly(new PlayerNameRepository()));

        assertTrue(changed);
        assertTrue(decoded.getUsers().isEmpty());
        assertTrue(decoded.hasAction(games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings.Action.CreateGlobalFrontier));
    }

    @Test
    void invalidCopiedFromUserPreservesCopiedFromId() {
        CompoundTag nbt = frontierTag();
        CompoundTag copiedFrom = new CompoundTag();
        copiedFrom.putString("id", COPIED_FROM_ID.toString());
        CompoundTag copiedUser = new CompoundTag();
        copiedUser.putString("username", "Unknown");
        copiedFrom.put("user", copiedUser);
        nbt.put("copiedFrom", copiedFrom);

        FrontierData.NbtReadResult result = FrontierData.readFromNBT(nbt, MapFrontiers.FRONTIER_DATA_VERSION,
                PlayerReferenceNbtReadContext.uuidOnly(new PlayerNameRepository()));
        FrontierData decoded = result.frontier();

        assertTrue(result.changedDuringLoad());
        assertEquals(COPIED_FROM_ID, decoded.getCopiedFromId());
        assertTrue(decoded.getCopiedFromUser() == null);
    }

    @Test
    void missingCopiedFromUserIsValidAndWriterOmitsEmptyPlaceholder() {
        CompoundTag nbt = collectionTag();
        CompoundTag copiedFrom = new CompoundTag();
        copiedFrom.putString("id", COPIED_FROM_ID.toString());
        nbt.put("copiedFrom", copiedFrom);

        CollectionData.NbtReadResult result = CollectionData.readFromNBT(nbt, MapFrontiers.FRONTIER_DATA_VERSION,
                PlayerReferenceNbtReadContext.uuidOnly(new PlayerNameRepository()));
        CollectionData decoded = result.collection();
        assertFalse(result.changedDuringLoad());
        assertEquals(COPIED_FROM_ID, decoded.getCopiedFromId());
        assertTrue(decoded.getCopiedFromUser() == null);

        CompoundTag rewritten = new CompoundTag();
        decoded.writeToNBT(rewritten, ignored -> null);
        assertFalse(rewritten.getCompoundOrEmpty("copiedFrom").contains("user"));
    }

    private static PlayerReferenceNbtReadContext resolvingContext(String username, UUID uuid) {
        PlayerId id = new PlayerId(uuid);
        return new PlayerReferenceNbtReadContext(new PlayerNameRepository(),
                candidate -> candidate.equalsIgnoreCase(username) ? id : null);
    }

    private static CompoundTag frontierTag() {
        CompoundTag nbt = new CompoundTag();
        frontier().writeToNBT(nbt, ignored -> "Owner");
        return nbt;
    }

    private static FrontierData frontier() {
        FrontierData frontier = new FrontierData(new PlayerId(OWNER_UUID));
        frontier.setId(UUID.randomUUID());
        frontier.setDimension(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld")));
        frontier.setPersonal(true);
        return frontier;
    }

    private static CompoundTag collectionTag() {
        CollectionData collection = new CollectionData(new PlayerId(OWNER_UUID));
        collection.setId(UUID.randomUUID());
        CompoundTag nbt = new CompoundTag();
        collection.writeToNBT(nbt, ignored -> "Owner");
        return nbt;
    }

    private static void makeNameOnly(CompoundTag nbt, String username) {
        nbt.remove("UUID");
        nbt.putString("username", username);
    }
}
