package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.api.MapFrontiersClientAPIImpl;
import games.alejandrocoria.mapfrontiers.client.settings.ClientSettingsProfileEvents;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionEvents;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientCollectionRuntime;
import games.alejandrocoria.mapfrontiers.client.territory.collection.ClientLocalPersonalCollectionStore;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionLocalOverrides;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionOverlayManager;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionUiStateStore;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.ClientFrontierEvents;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.ClientLocalPersonalFrontierStore;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierLocalOverrides;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontiersOverlayManager;
import journeymap.api.v2.client.IClientAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ClientTerritoryRuntime {
    private final IClientAPI journeyMapApi;
    private FrontiersOverlayManager globalFrontiersOverlayManager;
    private FrontiersOverlayManager personalFrontiersOverlayManager;
    private CollectionOverlayManager collectionOverlayManager;
    private ClientCollectionRuntime collectionRuntime;
    private ClientLocalPersonalFrontierStore localPersonalFrontierStore;
    private ClientLocalPersonalCollectionStore localPersonalCollectionStore;
    private ClientFrontierEvents frontierEvents;
    private ClientCollectionEvents collectionEvents;
    private ClientSettingsProfileEvents settingsProfileEvents;
    private ClientTerritoryOperationService operationService;
    private ClientTerritorySyncService syncService;
    private ClientLocalPersistenceCoordinator localPersistenceCoordinator;
    private FrontierLocalOverrides localOverrides;
    private CollectionLocalOverrides collectionLocalOverrides;
    private CollectionUiStateStore collectionUiStateStore;
    private MapFrontiersClientAPIImpl clientApi;

    public ClientTerritoryRuntime(IClientAPI journeyMapApi) {
        this.journeyMapApi = journeyMapApi;
    }

    public void ensureInitialized() {
        if (globalFrontiersOverlayManager == null) {
            globalFrontiersOverlayManager = new FrontiersOverlayManager(journeyMapApi);
        }

        if (personalFrontiersOverlayManager == null) {
            personalFrontiersOverlayManager = new FrontiersOverlayManager(journeyMapApi);
        }

        if (collectionRuntime == null) {
            collectionRuntime = new ClientCollectionRuntime();
        }

        if (collectionOverlayManager == null) {
            collectionOverlayManager = new CollectionOverlayManager(collectionRuntime, journeyMapApi);
        }

        if (localPersonalFrontierStore == null) {
            localPersonalFrontierStore = new ClientLocalPersonalFrontierStore();
        }

        if (localPersonalCollectionStore == null) {
            localPersonalCollectionStore = new ClientLocalPersonalCollectionStore();
        }

        if (frontierEvents == null) {
            frontierEvents = new ClientFrontierEvents();
        }

        if (collectionEvents == null) {
            collectionEvents = new ClientCollectionEvents();
        }

        if (settingsProfileEvents == null) {
            settingsProfileEvents = new ClientSettingsProfileEvents();
        }

        if (localPersistenceCoordinator == null) {
            localPersistenceCoordinator = new ClientLocalPersistenceCoordinator(
                    personalFrontiersOverlayManager,
                    collectionRuntime,
                    localPersonalFrontierStore,
                    localPersonalCollectionStore
            );
        }

        if (operationService == null) {
            operationService = new ClientTerritoryOperationService(this, globalFrontiersOverlayManager,
                    personalFrontiersOverlayManager, collectionRuntime, frontierEvents, collectionEvents);
        }

        if (syncService == null) {
            syncService = new ClientTerritorySyncService(this, globalFrontiersOverlayManager,
                    personalFrontiersOverlayManager, collectionRuntime, localPersonalFrontierStore, localPersonalCollectionStore);
            syncService.bootstrapLocalPersonalData();
        }

        if (localOverrides == null) {
            localOverrides = new FrontierLocalOverrides();
        }

        if (collectionLocalOverrides == null) {
            collectionLocalOverrides = new CollectionLocalOverrides();
        }

        if (collectionUiStateStore == null) {
            collectionUiStateStore = new CollectionUiStateStore();
        }
    }

    public boolean hasInitializedManagers() {
        return globalFrontiersOverlayManager != null && personalFrontiersOverlayManager != null;
    }

    public FrontiersOverlayManager getGlobalFrontiersOverlayManager() {
        ensureInitialized();
        return globalFrontiersOverlayManager;
    }

    public FrontiersOverlayManager getPersonalFrontiersOverlayManager() {
        ensureInitialized();
        return personalFrontiersOverlayManager;
    }

    public FrontierLocalOverrides getLocalOverrides() {
        ensureInitialized();
        return localOverrides;
    }

    public ClientCollectionRuntime getCollectionRuntime() {
        ensureInitialized();
        return collectionRuntime;
    }

    public CollectionLocalOverrides getCollectionLocalOverrides() {
        ensureInitialized();
        return collectionLocalOverrides;
    }

    public ClientTerritoryOperationService getOperationService() {
        ensureInitialized();
        return operationService;
    }

    public CollectionOverlayManager getCollectionOverlayManager() {
        ensureInitialized();
        return collectionOverlayManager;
    }

    public ClientFrontierEvents getFrontierEvents() {
        ensureInitialized();
        return frontierEvents;
    }

    public ClientSettingsProfileEvents getSettingsProfileEvents() {
        ensureInitialized();
        return settingsProfileEvents;
    }

    public ClientCollectionEvents getCollectionEvents() {
        ensureInitialized();
        return collectionEvents;
    }

    public ClientTerritorySyncService getSyncService() {
        ensureInitialized();
        return syncService;
    }

    public CollectionUiStateStore getCollectionUiStateStore() {
        ensureInitialized();
        return collectionUiStateStore;
    }

    public MapFrontiersClientAPIImpl getOrCreateClientApi() {
        ensureInitialized();
        if (clientApi == null) {
            clientApi = new MapFrontiersClientAPIImpl(frontierEvents, collectionEvents);
        }

        return clientApi;
    }

    void markDirty() {
        ensureInitialized();
        localPersistenceCoordinator.markDirty();
    }

    public void tickPersistence() {
        ensureInitialized();
        localPersistenceCoordinator.tickPersistence();
    }

    public void processOverlayManagers() {
        ensureInitialized();
        globalFrontiersOverlayManager.processDirtyOverlays();
        personalFrontiersOverlayManager.processDirtyOverlays();
        collectionOverlayManager.processDirtyOverlays();
    }

    public void close() {
        FrontiersOverlayManager globalManager = globalFrontiersOverlayManager;
        FrontiersOverlayManager personalManager = personalFrontiersOverlayManager;
        CollectionOverlayManager collectionManager = collectionOverlayManager;
        ClientCollectionRuntime collections = collectionRuntime;
        ClientTerritorySyncService sync = syncService;
        ClientLocalPersistenceCoordinator persistence = localPersistenceCoordinator;
        MapFrontiersClientAPIImpl api = clientApi;
        ClientFrontierEvents events = frontierEvents;
        ClientCollectionEvents collectionEventsState = collectionEvents;
        ClientSettingsProfileEvents settingsEvents = settingsProfileEvents;

        if (persistence != null) {
            closeStep("local persistence flush", this::flushPendingLocalPersistenceOnClose);
        }

        globalFrontiersOverlayManager = null;
        personalFrontiersOverlayManager = null;
        collectionOverlayManager = null;
        collectionRuntime = null;
        syncService = null;
        localPersistenceCoordinator = null;
        clientApi = null;
        frontierEvents = null;
        collectionEvents = null;
        settingsProfileEvents = null;
        operationService = null;
        localPersonalFrontierStore = null;
        localPersonalCollectionStore = null;
        localOverrides = null;
        collectionLocalOverrides = null;
        collectionUiStateStore = null;

        closeStep("global frontier overlays", () -> {
            if (globalManager != null) {
                globalManager.close();
            }
        });
        closeStep("personal frontier overlays", () -> {
            if (personalManager != null) {
                personalManager.close();
            }
        });
        closeStep("collection overlays", () -> {
            if (collectionManager != null) {
                collectionManager.close();
            }
        });
        closeStep("client collection runtime", () -> {
            if (collections != null) {
                collections.clear();
            }
        });
        closeStep("frontier sync service", () -> {
            if (sync != null) {
                sync.close();
            }
        });
        closeStep("local persistence coordinator", () -> {
            if (persistence != null) {
                persistence.reset();
            }
        });
        closeStep("client API", () -> {
            if (api != null) {
                api.close();
            }
        });
        closeStep("frontier events", () -> {
            if (events != null) {
                events.close();
            }
        });
        closeStep("collection events", () -> {
            if (collectionEventsState != null) {
                collectionEventsState.close();
            }
        });
        closeStep("settings profile events", () -> {
            if (settingsEvents != null) {
                settingsEvents.close();
            }
        });
    }

    private void flushPendingLocalPersistenceOnClose() {
        ensureInitialized();
        localPersistenceCoordinator.flushOnClose();
    }

    private static void closeStep(String name, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to close {}", name, t);
        }
    }
}
