package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StableUuidIdentityCharacterizationTest {
    private static final UUID OWNER_ID = new UUID(0L, 1L);
    private static final UUID SHARED_ID = new UUID(0L, 2L);

    @Test
    void personalFrontierLookupUsesStableUuidAfterUsernameChange() {
        TerritoriesManager manager = new TerritoriesManager();
        FrontierData frontier = manager.createNewPersonalFrontier(frontierSpec(user("OldOwner", OWNER_ID)));

        List<FrontierData> visibleToRenamedOwner = manager.getAllPersonalFrontiers(
                user("NewOwner", OWNER_ID), frontier.getDimension());

        assertTrue(visibleToRenamedOwner.contains(frontier));
        assertSame(frontier, manager.getFrontierFromID(frontier.getId()));
        assertFalse(manager.getAllPersonalFrontiers(user("NewOwner", UUID.randomUUID()), frontier.getDimension())
                .contains(frontier));
    }

    @Test
    void ownershipSharingAndSettingsMembershipUseStableUuidAfterUsernameChange() {
        SettingsUser oldOwner = user("OldOwner", OWNER_ID);
        SettingsUser renamedOwner = user("NewOwner", OWNER_ID);
        SettingsUser oldSharedUser = user("OldShared", SHARED_ID);
        SettingsUser renamedSharedUser = user("NewShared", SHARED_ID);
        FrontierData frontier = new FrontierData();
        frontier.setPersonal(true);
        frontier.setOwner(oldOwner);

        SettingsUserShared sharedAccess = new SettingsUserShared(oldSharedUser, false);
        sharedAccess.addAction(SettingsUserShared.Action.UpdateFrontier);
        frontier.addUserShared(sharedAccess);

        SettingsGroup group = new SettingsGroup("Builders", false);
        group.addAction(FrontierSettings.Action.UpdateGlobalFrontier);
        group.addUser(oldSharedUser);

        assertTrue(frontier.checkActionUserShared(renamedOwner, SettingsUserShared.Action.UpdateSettings));
        assertTrue(frontier.checkActionUserShared(renamedSharedUser, SettingsUserShared.Action.UpdateFrontier));
        assertTrue(group.hasUser(renamedSharedUser));
        assertFalse(group.hasUser(user("NewShared", UUID.randomUUID())));
    }

    private static FrontierCreateSpec frontierSpec(SettingsUser owner) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"));
        return FrontierCreateSpec.vertex(
                UUID.randomUUID(), owner, true, dimension, TerritoryLifetime.PERSISTENT, null, null,
                "Personal", "Frontier", 0xFFFFFF, new FrontierVisibilityData(), null, List.of(),
                new FrontierData.PathStyle());
    }

    private static SettingsUser user(String username, UUID uuid) {
        SettingsUser user = new SettingsUser();
        user.username = username;
        user.uuid = uuid;
        return user;
    }
}
