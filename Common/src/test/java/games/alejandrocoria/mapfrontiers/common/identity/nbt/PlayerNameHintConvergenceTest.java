package games.alejandrocoria.mapfrontiers.common.identity.nbt;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerNameHintConvergenceTest {
    private static final PlayerId OWNER = playerId(1L);
    private static final PlayerId SHARED = playerId(2L);
    private static final PlayerId COPIED_FROM = playerId(3L);
    private static final UUID FRONTIER_COPY_ID = new UUID(0L, 10L);
    private static final UUID COLLECTION_COPY_ID = new UUID(0L, 11L);

    @Test
    void loadedHintsConvergeToHigherPriorityNamesWithoutMutatingRuntimeState() {
        PlayerNameResolver oldNames = playerId -> {
            if (playerId.equals(OWNER)) {
                return "OldOwner";
            }
            if (playerId.equals(SHARED)) {
                return "OldShared";
            }
            if (playerId.equals(COPIED_FROM)) {
                return "OldCopiedFrom";
            }
            return null;
        };

        CompoundTag frontierNbt = new CompoundTag();
        frontier().writeToNBT(frontierNbt, oldNames);
        CompoundTag collectionNbt = new CompoundTag();
        collection().writeToNBT(collectionNbt, oldNames);
        CompoundTag settingsNbt = new CompoundTag();
        settings().writeToNBT(settingsNbt, oldNames);

        PlayerNameRepository names = new PlayerNameRepository();
        PlayerReferenceNbtReadContext context = PlayerReferenceNbtReadContext.uuidOnly(names);
        FrontierData.NbtReadResult frontierResult = FrontierData.readFromNBT(
                frontierNbt, MapFrontiers.FRONTIER_DATA_VERSION, context);
        CollectionData.NbtReadResult collectionResult = CollectionData.readFromNBT(
                collectionNbt, MapFrontiers.FRONTIER_DATA_VERSION, context);
        FrontierSettings loadedSettings = new FrontierSettings();
        boolean settingsChangedDuringLoad = loadedSettings.readFromNBT(settingsNbt, context);

        assertFalse(frontierResult.changedDuringLoad());
        assertFalse(collectionResult.changedDuringLoad());
        assertFalse(settingsChangedDuringLoad);
        assertEquals("OldOwner", names.resolveName(OWNER));
        assertEquals("OldShared", names.resolveName(SHARED));
        assertEquals("OldCopiedFrom", names.resolveName(COPIED_FROM));

        FrontierData loadedFrontier = frontierResult.frontier();
        CollectionData loadedCollection = collectionResult.collection();
        loadedFrontier.setSharingRevision(7L);
        loadedCollection.setCollectionRevision(11L);
        long frontierSyncHash = loadedFrontier.computeSyncHash();

        names.observe(OWNER, "CurrentOwner", PlayerNameSource.CONNECTED_PROFILE);
        names.observe(SHARED, "CurrentShared", PlayerNameSource.CONNECTED_PROFILE);
        names.observe(COPIED_FROM, "CurrentCopiedFrom", PlayerNameSource.CONNECTED_PROFILE);

        CompoundTag rewrittenFrontier = new CompoundTag();
        loadedFrontier.writeToNBT(rewrittenFrontier, names);
        CompoundTag rewrittenCollection = new CompoundTag();
        loadedCollection.writeToNBT(rewrittenCollection, names);
        CompoundTag rewrittenSettings = new CompoundTag();
        loadedSettings.writeToNBT(rewrittenSettings, names);

        assertReference(rewrittenFrontier.getCompoundOrEmpty("owner"), OWNER, "CurrentOwner");
        assertReference(rewrittenFrontier.getListOrEmpty("usersShared").getCompoundOrEmpty(0), SHARED,
                "CurrentShared");
        assertReference(rewrittenFrontier.getCompoundOrEmpty("copiedFrom").getCompoundOrEmpty("user"), COPIED_FROM,
                "CurrentCopiedFrom");
        assertReference(rewrittenCollection.getCompoundOrEmpty("owner"), OWNER, "CurrentOwner");
        assertReference(rewrittenCollection.getCompoundOrEmpty("copiedFrom").getCompoundOrEmpty("user"),
                COPIED_FROM, "CurrentCopiedFrom");
        CompoundTag group = rewrittenSettings.getListOrEmpty("customGroups").getCompoundOrEmpty(0);
        assertReference(group.getListOrEmpty("users").getCompoundOrEmpty(0), SHARED, "CurrentShared");

        assertEquals(OWNER, loadedFrontier.getOwner());
        assertEquals(SHARED, loadedFrontier.getUserAccesses().getFirst().getPlayerId());
        assertEquals(COPIED_FROM, loadedFrontier.getCopiedFromUser());
        assertEquals(FRONTIER_COPY_ID, loadedFrontier.getCopiedFromId());
        assertEquals(OWNER, loadedCollection.getOwner());
        assertEquals(COPIED_FROM, loadedCollection.getCopiedFromUser());
        assertEquals(COLLECTION_COPY_ID, loadedCollection.getCopiedFromId());
        assertEquals(SHARED, loadedSettings.getCustomGroups().getFirst().getUsers().getFirst());
        assertEquals(frontierSyncHash, loadedFrontier.computeSyncHash());
        assertEquals(7L, loadedFrontier.getSharingRevision());
        assertEquals(11L, loadedCollection.getCollectionRevision());
    }

    @Test
    void authoritativeConfirmationRefreshesAContradictorySettingsHintWhenTheWinningNameMatches() {
        CompoundTag frontierNbt = new CompoundTag();
        frontier().writeToNBT(frontierNbt, playerId -> playerId.equals(OWNER) ? "Dev" : null);
        CompoundTag settingsNbt = new CompoundTag();
        FrontierSettings settings = new FrontierSettings();
        settings.createCustomGroup("Builders").addUser(OWNER);
        settings.writeToNBT(settingsNbt, playerId -> playerId.equals(OWNER) ? "OldDev" : null);

        PlayerNameRepository names = new PlayerNameRepository();
        PlayerReferenceNbtReadContext context = PlayerReferenceNbtReadContext.uuidOnly(names);
        FrontierData.readFromNBT(frontierNbt, MapFrontiers.FRONTIER_DATA_VERSION, context);
        FrontierSettings loadedSettings = new FrontierSettings();
        loadedSettings.readFromNBT(settingsNbt, context);

        boolean wasKnownOnlyFromHint = names.isKnownOnlyFromHint(OWNER);
        boolean nameChanged = names.observe(OWNER, "Dev", PlayerNameSource.MINECRAFT_CACHE);

        assertTrue(wasKnownOnlyFromHint);
        assertFalse(nameChanged);
        assertFalse(names.isKnownOnlyFromHint(OWNER));

        CompoundTag rewrittenSettings = new CompoundTag();
        loadedSettings.writeToNBT(rewrittenSettings, names);
        CompoundTag group = rewrittenSettings.getListOrEmpty("customGroups").getCompoundOrEmpty(0);
        assertReference(group.getListOrEmpty("users").getCompoundOrEmpty(0), OWNER, "Dev");
    }

    private static FrontierData frontier() {
        FrontierData frontier = new FrontierData(OWNER);
        frontier.setId(new UUID(0L, 20L));
        frontier.setDimension(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld")));
        frontier.setPersonal(true);
        frontier.addUserAccess(new FrontierUserAccess(SHARED, false));
        frontier.setCopiedFrom(FRONTIER_COPY_ID, COPIED_FROM);
        return frontier;
    }

    private static CollectionData collection() {
        CollectionData collection = new CollectionData(OWNER);
        collection.setId(new UUID(0L, 21L));
        collection.setPersonal(true);
        collection.setCopiedFrom(COLLECTION_COPY_ID, COPIED_FROM);
        return collection;
    }

    private static FrontierSettings settings() {
        FrontierSettings settings = new FrontierSettings();
        SettingsGroup group = settings.createCustomGroup("Builders");
        group.addUser(SHARED);
        return settings;
    }

    private static void assertReference(CompoundTag reference, PlayerId playerId, String username) {
        assertEquals(playerId.uuid().toString(), reference.getStringOr("UUID", ""));
        assertEquals(username, reference.getStringOr("username", ""));
    }

    private static PlayerId playerId(long value) {
        return new PlayerId(new UUID(0L, value));
    }
}
