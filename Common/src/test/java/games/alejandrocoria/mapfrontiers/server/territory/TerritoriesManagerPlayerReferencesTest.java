package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TerritoriesManagerPlayerReferencesTest {
    @Test
    void collectsEveryPersistedPlayerReferenceWithoutUsingDerivedIndexKeys() {
        PlayerId frontierOwner = user();
        PlayerId sharedUser = user();
        PlayerId frontierCopiedFromUser = user();
        PlayerId collectionOwner = user();
        PlayerId collectionCopiedFromUser = user();
        PlayerId settingsGroupUser = user();
        PlayerId notReferenced = user();
        TerritoriesManager manager = new TerritoriesManager(new PlayerNameRepository(), username -> null);

        FrontierData frontier = manager.createNewPersonalFrontier(frontierSpec(frontierOwner));
        frontier.addUserAccess(new FrontierUserAccess(sharedUser, false));
        frontier.setCopiedFrom(UUID.randomUUID(), frontierCopiedFromUser);

        CollectionData collection = new CollectionData(collectionOwner);
        collection.setId(UUID.randomUUID());
        collection.setPersonal(true);
        collection.setCopiedFrom(UUID.randomUUID(), collectionCopiedFromUser);
        manager.addPersonalCollection(collection);

        SettingsGroup group = manager.getSettings().createCustomGroup("Builders");
        group.addAction(FrontierSettings.Action.UpdateGlobalFrontier);
        group.addUser(settingsGroupUser);

        Set<PlayerId> references = manager.getReferencedPlayerIds();

        assertEquals(Set.of(frontierOwner, sharedUser, frontierCopiedFromUser, collectionOwner,
                collectionCopiedFromUser, settingsGroupUser), references);
        assertFalse(references.contains(notReferenced));
    }

    private static FrontierCreateSpec frontierSpec(PlayerId owner) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"));
        return FrontierCreateSpec.vertex(
                UUID.randomUUID(), owner, true, dimension, TerritoryLifetime.PERSISTENT, null, null,
                "Personal", "Frontier", 0xFFFFFF, new FrontierVisibilityData(), null, List.of(),
                new FrontierData.PathStyle());
    }

    private static PlayerId user() {
        return new PlayerId(UUID.randomUUID());
    }
}
