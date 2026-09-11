package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StableUuidIdentityCharacterizationTest {
    private static final UUID OWNER_ID = new UUID(0L, 1L);
    private static final UUID SHARED_ID = new UUID(0L, 2L);

    @Test
    void personalFrontierLookupUsesStableUuidAfterUsernameChange() {
        PlayerNameRepository names = new PlayerNameRepository();
        PlayerId owner = user(OWNER_ID);
        names.observe(owner, "OldOwner", PlayerNameSource.CONNECTED_PROFILE);
        TerritoriesManager manager = new TerritoriesManager(names, username -> null);
        FrontierData frontier = manager.createNewPersonalFrontier(frontierSpec(user(OWNER_ID)));
        long syncHash = frontier.computeSyncHash();

        names.observe(owner, "RenamedOwner", PlayerNameSource.CONNECTED_PROFILE);

        List<FrontierData> visibleToRenamedOwner = manager.getAllPersonalFrontiers(
                user(OWNER_ID), frontier.getDimension());

        assertEquals("RenamedOwner", names.resolveName(owner));
        assertEquals(syncHash, frontier.computeSyncHash());
        assertTrue(visibleToRenamedOwner.contains(frontier));
        assertSame(frontier, manager.getFrontierFromID(frontier.getId()));
        assertFalse(manager.getAllPersonalFrontiers(user(UUID.randomUUID()), frontier.getDimension())
                .contains(frontier));
    }

    @Test
    void ownershipSharingAndSettingsMembershipUseStableUuidAfterUsernameChange() {
        PlayerId oldOwner = user(OWNER_ID);
        PlayerId renamedOwner = user(OWNER_ID);
        PlayerId oldSharedUser = user(SHARED_ID);
        PlayerId renamedSharedUser = user(SHARED_ID);
        PlayerId differentUserWithSameName = user(UUID.randomUUID());
        PlayerNameRepository names = new PlayerNameRepository();
        names.observe(oldOwner, "OldOwner", PlayerNameSource.CONNECTED_PROFILE);
        names.observe(oldSharedUser, "OldShared", PlayerNameSource.CONNECTED_PROFILE);
        names.observe(differentUserWithSameName, "RenamedOwner", PlayerNameSource.HINT);
        FrontierData frontier = new FrontierData(oldOwner);
        frontier.setPersonal(true);
        frontier.setOwner(oldOwner);
        frontier.setCopiedFrom(new UUID(0L, 3L), oldSharedUser);
        frontier.setSharingRevision(7L);

        FrontierUserAccess sharedAccess = new FrontierUserAccess(oldSharedUser, false);
        sharedAccess.addAction(FrontierUserAccess.Action.UpdateFrontier);
        frontier.addUserAccess(sharedAccess);

        CollectionData collection = new CollectionData(oldOwner);
        collection.setCopiedFrom(new UUID(0L, 4L), oldSharedUser);
        collection.setCollectionRevision(11L);

        FrontierSettings settings = new FrontierSettings();
        SettingsGroup group = settings.createCustomGroup("Builders");
        group.addAction(FrontierSettings.Action.UpdateGlobalFrontier);
        group.addUser(oldSharedUser);
        TerritoriesManager manager = new TerritoriesManager(names, username -> null);
        manager.setSettings(settings, 13L);

        long syncHash = frontier.computeSyncHash();
        names.observe(renamedOwner, "RenamedOwner", PlayerNameSource.CONNECTED_PROFILE);
        names.observe(renamedSharedUser, "RenamedShared", PlayerNameSource.CONNECTED_PROFILE);

        assertTrue(frontier.checkUserAccess(renamedOwner, FrontierUserAccess.Action.UpdateSettings));
        assertTrue(frontier.checkUserAccess(renamedSharedUser, FrontierUserAccess.Action.UpdateFrontier));
        assertTrue(manager.getSettings().getCustomGroups().getFirst().hasUser(renamedSharedUser));
        assertEquals(renamedOwner, collection.getOwner());
        assertEquals(renamedSharedUser, frontier.getCopiedFromUser());
        assertEquals(renamedSharedUser, collection.getCopiedFromUser());
        assertEquals(syncHash, frontier.computeSyncHash());
        assertEquals(7L, frontier.getSharingRevision());
        assertEquals(11L, collection.getCollectionRevision());
        assertEquals(13L, manager.getSettingsRevision());
        assertFalse(manager.getSettings().getCustomGroups().getFirst().hasUser(differentUserWithSameName));
        assertFalse(frontier.checkUserAccess(differentUserWithSameName,
                FrontierUserAccess.Action.UpdateFrontier));
        assertFalse(collection.getOwner().equals(differentUserWithSameName));
    }

    private static FrontierCreateSpec frontierSpec(PlayerId owner) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"));
        return FrontierCreateSpec.vertex(
                UUID.randomUUID(), owner, true, dimension, TerritoryLifetime.PERSISTENT, null, null,
                "Personal", "Frontier", 0xFFFFFF, new FrontierVisibilityData(), null, List.of(),
                new FrontierData.PathStyle());
    }

    private static PlayerId user(UUID uuid) {
        return new PlayerId(uuid);
    }
}
