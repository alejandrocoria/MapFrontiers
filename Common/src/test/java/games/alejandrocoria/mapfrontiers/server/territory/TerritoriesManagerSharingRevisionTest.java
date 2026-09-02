package games.alejandrocoria.mapfrontiers.server.territory;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerritoriesManagerSharingRevisionTest {
    @Test
    void everyCommittedSharingMutationAdvancesRevisionExactlyOnce() {
        TerritoriesManager manager = new TerritoriesManager();
        SettingsUser owner = user("Owner");
        SettingsUser target = user("Target");
        FrontierData frontier = manager.createNewPersonalFrontier(frontierSpec(owner));
        SettingsUserShared sharedUser = new SettingsUserShared(target, false);

        assertTrue(manager.addPendingPersonalFrontierShare(frontier.getId(), sharedUser));
        assertEquals(1L, frontier.getSharingRevision());
        assertFalse(manager.addPendingPersonalFrontierShare(frontier.getId(), sharedUser));
        assertEquals(1L, frontier.getSharingRevision());

        sharedUser.addAction(SettingsUserShared.Action.UpdateFrontier);
        assertTrue(manager.updatePersonalFrontierShare(frontier.getId(), sharedUser));
        assertEquals(2L, frontier.getSharingRevision());
        assertFalse(manager.updatePersonalFrontierShare(frontier.getId(), sharedUser));
        assertEquals(2L, frontier.getSharingRevision());

        assertTrue(manager.acceptPendingPersonalFrontierShare(target, frontier.getId()));
        assertEquals(3L, frontier.getSharingRevision());
        assertTrue(manager.removePersonalFrontierShare(frontier.getId(), target));
        assertEquals(4L, frontier.getSharingRevision());

        assertTrue(manager.addPendingPersonalFrontierShare(frontier.getId(), sharedUser));
        assertEquals(5L, frontier.getSharingRevision());
        assertTrue(manager.expirePendingPersonalFrontierShare(frontier.getId(), target));
        assertEquals(6L, frontier.getSharingRevision());
        assertFalse(manager.expirePendingPersonalFrontierShare(frontier.getId(), target));
        assertEquals(6L, frontier.getSharingRevision());
    }

    private static FrontierCreateSpec frontierSpec(SettingsUser owner) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"));
        return FrontierCreateSpec.vertex(
                UUID.randomUUID(), owner, true, dimension, TerritoryLifetime.PERSISTENT, null, null,
                "Personal", "Frontier", 0xFFFFFF, new FrontierVisibilityData(), null, List.of(),
                new FrontierData.PathStyle());
    }

    private static SettingsUser user(String username) {
        SettingsUser user = new SettingsUser();
        user.username = username;
        user.uuid = UUID.randomUUID();
        return user;
    }
}
