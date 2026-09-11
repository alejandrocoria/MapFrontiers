package games.alejandrocoria.mapfrontiers.common.identity;

import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerReferenceCollectorTest {
    @Test
    void collectsAllReferencesPresentInEachPayloadType() {
        PlayerId frontierOwner = playerId(1L);
        PlayerId sharedUser = playerId(2L);
        PlayerId copiedFrom = playerId(3L);
        FrontierData frontier = new FrontierData(frontierOwner);
        frontier.setPersonal(true);
        frontier.addUserAccess(new FrontierUserAccess(sharedUser, true));
        frontier.setCopiedFrom(UUID.randomUUID(), copiedFrom);

        PlayerId collectionOwner = playerId(4L);
        PlayerId collectionCopiedFrom = playerId(5L);
        CollectionData collection = new CollectionData(collectionOwner);
        collection.setCopiedFrom(UUID.randomUUID(), collectionCopiedFrom);

        FrontierSharingChange sharingChange = new FrontierSharingChange();
        sharingChange.setUserAccesses(List.of(new FrontierUserAccess(sharedUser, false)));

        PlayerId settingsUser = playerId(6L);
        FrontierSettings settings = new FrontierSettings();
        SettingsGroup group = settings.createCustomGroup("Builders");
        group.addUser(settingsUser);

        assertEquals(Set.of(frontierOwner, sharedUser, copiedFrom), PlayerReferenceCollector.collect(frontier));
        assertEquals(Set.of(collectionOwner, collectionCopiedFrom), PlayerReferenceCollector.collect(collection));
        assertEquals(Set.of(sharedUser), PlayerReferenceCollector.collect(sharingChange));
        assertEquals(Set.of(settingsUser), PlayerReferenceCollector.collect(settings));
    }

    private static PlayerId playerId(long value) {
        return new PlayerId(new UUID(0L, value));
    }
}
