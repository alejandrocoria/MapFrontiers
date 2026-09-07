package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerIdentityDomainTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("minecraft", "overworld"));

    @Test
    void requiredPlayerReferencesRejectNullWhileCopiedFromUserMayBeAbsent() {
        assertThrows(NullPointerException.class, () -> new PlayerId(null));
        assertThrows(NullPointerException.class, () -> new FrontierData((PlayerId) null));
        assertThrows(NullPointerException.class, () -> new CollectionData((PlayerId) null));
        assertThrows(NullPointerException.class, () -> new FrontierUserAccess(null, false));
        assertThrows(NullPointerException.class, () -> new CopiedFromInfo((UUID) null));
        assertThrows(NullPointerException.class, () -> frontierSpec(null));

        UUID sourceId = UUID.randomUUID();
        FrontierData frontier = new FrontierData(playerId(1L));
        frontier.setCopiedFrom(sourceId, null);
        CollectionData collection = new CollectionData(playerId(1L));
        collection.setCopiedFrom(sourceId, null);

        assertNull(frontier.getCopiedFromUser());
        assertNull(collection.getCopiedFromUser());
    }

    @Test
    void synchronizedCollectionAndSharingStateUseOnlyPlayerIdsAndAccessState() {
        PlayerId firstInstance = playerId(2L);
        PlayerId sameUuid = playerId(2L);
        CollectionData collection = new CollectionData(firstInstance);
        collection.setId(UUID.randomUUID());
        CollectionData sameCollection = new CollectionData(collection);
        sameCollection.setOwner(sameUuid);

        assertTrue(collection.hasSameSynchronizedState(sameCollection));
        sameCollection.setOwner(playerId(3L));
        assertFalse(collection.hasSameSynchronizedState(sameCollection));

        FrontierUserAccess access = new FrontierUserAccess(firstInstance, false);
        access.addAction(FrontierUserAccess.Action.UpdateFrontier);
        FrontierSharingChange sharing = sharingChange(access);

        FrontierUserAccess sameAccess = new FrontierUserAccess(sameUuid, false);
        sameAccess.addAction(FrontierUserAccess.Action.UpdateFrontier);
        assertTrue(sharing.hasSameFunctionalState(sharingChange(sameAccess)));

        sameAccess.setPending(true);
        assertFalse(sharing.hasSameFunctionalState(sharingChange(sameAccess)));

        FrontierUserAccess differentPlayer = new FrontierUserAccess(playerId(3L), false);
        differentPlayer.addAction(FrontierUserAccess.Action.UpdateFrontier);
        assertFalse(sharing.hasSameFunctionalState(sharingChange(differentPlayer)));
    }

    private static FrontierSharingChange sharingChange(FrontierUserAccess access) {
        FrontierSharingChange change = new FrontierSharingChange();
        change.setUserAccesses(List.of(access));
        return change;
    }

    private static FrontierCreateSpec frontierSpec(PlayerId owner) {
        return FrontierCreateSpec.vertex(UUID.randomUUID(), owner, true, OVERWORLD,
                TerritoryLifetime.PERSISTENT, null, null, "", "", 0,
                new FrontierVisibilityData(), null, List.of(), new FrontierData.PathStyle());
    }

    private static PlayerId playerId(long value) {
        return new PlayerId(new UUID(0L, value));
    }
}
