package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionRuntime;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientLocalPersonalCollectionStore;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.ClientLocalPersonalFrontierStore;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import games.alejandrocoria.mapfrontiers.common.util.DebouncedPersistenceController;
import net.minecraft.client.Minecraft;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
final class ClientLocalPersistenceCoordinator {
    private static final long LOCAL_PERSISTENCE_SAVE_DEBOUNCE_MS = 10_000L;
    private static final long LOCAL_PERSISTENCE_SAVE_MAX_DELAY_MS = 60_000L;
    private static final Minecraft mc = Minecraft.getInstance();

    private final FrontiersOverlayManager personalManager;
    private final ClientCollectionRuntime collectionRuntime;
    private final ClientLocalPersonalFrontierStore localPersonalFrontierStore;
    private final ClientLocalPersonalCollectionStore localPersonalCollectionStore;
    private final DebouncedPersistenceController persistenceController;

    ClientLocalPersistenceCoordinator(FrontiersOverlayManager personalManager,
                                      ClientCollectionRuntime collectionRuntime,
                                      ClientLocalPersonalFrontierStore localPersonalFrontierStore,
                                      ClientLocalPersonalCollectionStore localPersonalCollectionStore) {
        this.personalManager = personalManager;
        this.collectionRuntime = collectionRuntime;
        this.localPersonalFrontierStore = localPersonalFrontierStore;
        this.localPersonalCollectionStore = localPersonalCollectionStore;
        this.persistenceController = new DebouncedPersistenceController(
                LOCAL_PERSISTENCE_SAVE_DEBOUNCE_MS,
                LOCAL_PERSISTENCE_SAVE_MAX_DELAY_MS
        );
    }

    public void markDirty() {
        persistenceController.markDirty(System.currentTimeMillis());
    }

    public void tickPersistence() {
        long now = System.currentTimeMillis();
        if (persistenceController.shouldFlushOnTick(now)) {
            flushSnapshot();
            persistenceController.markPersisted(now);
        }
    }

    public void flushOnClose() {
        if (persistenceController.hasPendingChanges()) {
            flushNow();
        }
    }

    public void flushNow() {
        flushSnapshot();
        persistenceController.markPersisted(System.currentTimeMillis());
    }

    public void reset() {
        persistenceController.reset();
    }

    private void flushSnapshot() {
        if (mc.isLocalServer() || mc.player == null) {
            return;
        }

        SettingsUser currentPlayer = new SettingsUser(mc.player);
        localPersonalFrontierStore.saveOwnedFrontierMirror(getPersistablePersonalFrontiers(), currentPlayer);
        localPersonalCollectionStore.saveOwnedCollectionMirror(getPersistentPersonalCollections(), currentPlayer);
    }

    private Collection<FrontierData> getPersistablePersonalFrontiers() {
        return personalManager.getAllFrontiers().values().stream()
                .flatMap(List::stream)
                .map(frontier -> sanitizePersistentPersonalFrontierForStorage(new FrontierData(frontier)))
                .toList();
    }

    private Collection<CollectionData> getPersistentPersonalCollections() {
        return collectionRuntime.getCollections(CollectionScope.PERSONAL_PERSISTENT);
    }

    private FrontierData sanitizePersistentPersonalFrontierForStorage(FrontierData frontier) {
        if (!frontier.isPersistent()) {
            return frontier;
        }

        UUID collectionId = frontier.getCollectionId();
        if (collectionId == null) {
            return frontier;
        }

        CollectionData collection = collectionRuntime.getCollection(collectionId);
        if (collection == null || !collection.isPersistent()
                || collection.getPersonal() != frontier.getPersonal()
                || collection.getLifetime() != frontier.getLifetime()
                || (frontier.getPersonal() && !collection.getOwner().equals(frontier.getOwner()))) {
            frontier.setCollectionId(null);
        }

        return frontier;
    }
}
