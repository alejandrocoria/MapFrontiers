package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionRuntime;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientLocalPersonalCollectionStore;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.ClientLocalPersonalFrontierStore;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketPersonalCollection;
import games.alejandrocoria.mapfrontiers.common.network.PacketPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.client.Minecraft;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ClientTerritorySyncService {
    private static final Minecraft mc = Minecraft.getInstance();

    private final ClientTerritoryRuntime runtime;
    private final FrontiersOverlayManager globalManager;
    private final FrontiersOverlayManager personalManager;
    private final ClientCollectionRuntime collectionRuntime;
    private final ClientLocalPersonalFrontierStore localPersonalStore;
    private final ClientLocalPersonalCollectionStore localPersonalCollectionStore;
    private boolean localPersonalFrontiersLoaded = false;
    private boolean localPersonalCollectionsLoaded = false;

    public ClientTerritorySyncService(ClientTerritoryRuntime runtime,
                                      FrontiersOverlayManager globalManager,
                                      FrontiersOverlayManager personalManager,
                                      ClientCollectionRuntime collectionRuntime,
                                      ClientLocalPersonalFrontierStore localPersonalStore,
                                      ClientLocalPersonalCollectionStore localPersonalCollectionStore) {
        this.runtime = runtime;
        this.globalManager = globalManager;
        this.personalManager = personalManager;
        this.collectionRuntime = collectionRuntime;
        this.localPersonalStore = localPersonalStore;
        this.localPersonalCollectionStore = localPersonalCollectionStore;
    }

    public void bootstrapLocalPersonalData() {
        if (mc.isLocalServer() || (localPersonalFrontiersLoaded && localPersonalCollectionsLoaded)) {
            return;
        }

        ensureLocalPersonalDataLoadedWithoutRebuild();
        replaceCollectionRuntimeFrontierIndexes();
    }

    public void applyServerSnapshot(List<FrontierData> globalFrontiers,
                                    List<FrontierData> personalFrontiers,
                                    List<CollectionData> globalCollections,
                                    List<CollectionData> personalCollections) {
        runtime.getOperationService().clearPendingOptimisticUpdates();
        ensureLocalPersonalDataLoadedWithoutRebuild();

        List<CollectionData> persistentGlobalCollections = globalCollections.stream()
                .filter(CollectionData::isPersistent)
                .toList();
        List<CollectionData> persistentPersonalCollections = personalCollections.stream()
                .filter(CollectionData::isPersistent)
                .toList();

        globalManager.replaceFrontiers(globalFrontiers);
        if (mc.isLocalServer()) {
            collectionRuntime.replaceCollections(persistentGlobalCollections, persistentPersonalCollections);
            personalManager.replaceFrontiers(personalFrontiers);
            replaceCollectionRuntimeFrontierIndexes();
            return;
        }

        PlayerId currentPlayer = mc.player == null ? null : new PlayerId(mc.player.getUUID());
        List<FrontierOverlay> existingLocalPersonal = personalManager.getAllFrontiers().values().stream()
                .flatMap(List::stream)
                .toList();
        List<CollectionData> existingLocalPersonalCollections = currentPlayer == null
                ? List.of()
                : collectionRuntime.getCollections(CollectionScope.PERSONAL_PERSISTENT).stream()
                        .filter(collection -> collection.getOwner().equals(currentPlayer))
                        .map(CollectionData::new)
                        .toList();
        Set<UUID> serverFrontierIds = new HashSet<>();
        Set<UUID> serverCollectionIds = new HashSet<>();

        for (CollectionData collection : persistentPersonalCollections) {
            serverCollectionIds.add(collection.getId());
        }

        collectionRuntime.replaceCollections(persistentGlobalCollections, persistentPersonalCollections);

        for (FrontierData data : personalFrontiers) {
            serverFrontierIds.add(data.getId());
            FrontierOverlay existing = personalManager.getFrontier(data.getId());
            if (existing != null) {
                existing.updateFromData(data);
            } else {
                personalManager.addFrontier(data);
            }
        }

        List<FrontierOverlay> localOnlyOwnedFrontiers = new ArrayList<>();
        List<FrontierOverlay> localFrontiersToDiscard = new ArrayList<>();
        List<CollectionData> localOnlyOwnedCollections = new ArrayList<>();
        if (currentPlayer != null) {
            for (FrontierOverlay localFrontier : existingLocalPersonal) {
                if (serverFrontierIds.contains(localFrontier.getId())) {
                    continue;
                }

                if (!localFrontier.getOwner().equals(currentPlayer)) {
                    localFrontiersToDiscard.add(localFrontier);
                } else if (localFrontier.isPersistent()) {
                    localOnlyOwnedFrontiers.add(localFrontier);
                }
            }

            for (CollectionData localCollection : existingLocalPersonalCollections) {
                if (!serverCollectionIds.contains(localCollection.getId())) {
                    localOnlyOwnedCollections.add(localCollection);
                    collectionRuntime.onCollectionUpserted(localCollection);
                }
            }
        }

        for (FrontierOverlay frontier : localFrontiersToDiscard) {
            personalManager.deleteFrontier(frontier.getDimension(), frontier.getId());
        }

        for (CollectionData collection : localOnlyOwnedCollections) {
            PacketHandler.sendToServer(new PacketPersonalCollection(collection, runtime.getPlayerNameRepository()));
        }

        for (FrontierOverlay frontier : localOnlyOwnedFrontiers) {
            frontier.removeAllUserAccesses();
            PacketHandler.sendToServer(new PacketPersonalFrontier(frontier, runtime.getPlayerNameRepository()));
        }
        replaceCollectionRuntimeFrontierIndexes();
        markOwnedPersonalDataDirty();
    }

    public void close() {
        localPersonalFrontiersLoaded = false;
        localPersonalCollectionsLoaded = false;
        collectionRuntime.clear();
    }

    private void ensureLocalPersonalDataLoadedWithoutRebuild() {
        loadLocalPersonalFrontiersRaw();
        loadLocalPersonalCollectionsRaw();
    }

    private void loadLocalPersonalFrontiersRaw() {
        if (localPersonalFrontiersLoaded || mc.isLocalServer()) {
            return;
        }

        for (FrontierData frontier : localPersonalStore.loadFrontiers()) {
            if (!frontier.isPersistent()) {
                continue;
            }
            personalManager.addFrontier(frontier);
        }

        localPersonalFrontiersLoaded = true;
    }

    private void loadLocalPersonalCollectionsRaw() {
        if (localPersonalCollectionsLoaded || mc.isLocalServer()) {
            return;
        }

        for (CollectionData collection : localPersonalCollectionStore.loadCollections()) {
            collectionRuntime.onCollectionUpserted(collection);
        }

        localPersonalCollectionsLoaded = true;
    }

    private void replaceCollectionRuntimeFrontierIndexes() {
        collectionRuntime.replaceFrontierIndexes(globalManager, personalManager);
        markIndexedCollectionPresentationsDirty();
        runtime.getCollectionOverlayManager().syncFromCurrentRuntime();
    }

    private void markIndexedCollectionPresentationsDirty() {
        for (CollectionScope scope : CollectionScope.values()) {
            for (CollectionData collection : collectionRuntime.getCollections(scope)) {
                globalManager.markCollectionPresentationDirty(collection.getId());
                personalManager.markCollectionPresentationDirty(collection.getId());
            }
        }
    }

    private void markOwnedPersonalDataDirty() {
        if (mc.isLocalServer() || mc.player == null) {
            return;
        }

        runtime.markDirty();
    }
}
