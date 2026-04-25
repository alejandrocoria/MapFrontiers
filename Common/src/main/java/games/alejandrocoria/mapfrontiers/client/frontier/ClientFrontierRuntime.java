package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.api.MapFrontiersClientAPIImpl;
import games.alejandrocoria.mapfrontiers.client.settings.ClientSettingsProfileEvents;
import journeymap.api.v2.client.IClientAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ClientFrontierRuntime {
    private final IClientAPI journeyMapApi;
    private FrontiersOverlayManager globalFrontiersOverlayManager;
    private FrontiersOverlayManager personalFrontiersOverlayManager;
    private ClientCollectionRuntime collectionRuntime;
    private ClientLocalPersonalFrontierStore localPersonalFrontierStore;
    private ClientLocalPersonalCollectionStore localPersonalCollectionStore;
    private ClientFrontierEvents frontierEvents;
    private ClientCollectionEvents collectionEvents;
    private ClientSettingsProfileEvents settingsProfileEvents;
    private ClientFrontierOperationService operationService;
    private ClientFrontierSyncService syncService;
    private FrontierLocalOverrides localOverrides;
    private MapFrontiersClientAPIImpl clientApi;

    public ClientFrontierRuntime(IClientAPI journeyMapApi) {
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

        if (operationService == null) {
            operationService = new ClientFrontierOperationService(globalFrontiersOverlayManager, personalFrontiersOverlayManager,
                    collectionRuntime, localPersonalFrontierStore, localPersonalCollectionStore, frontierEvents, collectionEvents);
        }

        if (syncService == null) {
            syncService = new ClientFrontierSyncService(globalFrontiersOverlayManager, personalFrontiersOverlayManager,
                    collectionRuntime, localPersonalFrontierStore, localPersonalCollectionStore);
            syncService.loadLocalPersonalFrontiers();
            syncService.loadLocalPersonalCollections();
        }

        if (localOverrides == null) {
            localOverrides = new FrontierLocalOverrides();
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

    public ClientFrontierOperationService getOperationService() {
        ensureInitialized();
        return operationService;
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

    public ClientFrontierSyncService getSyncService() {
        ensureInitialized();
        return syncService;
    }

    public MapFrontiersClientAPIImpl getOrCreateClientApi() {
        ensureInitialized();
        if (clientApi == null) {
            clientApi = new MapFrontiersClientAPIImpl(frontierEvents);
        }

        return clientApi;
    }

    public void close() {
        FrontiersOverlayManager globalManager = globalFrontiersOverlayManager;
        FrontiersOverlayManager personalManager = personalFrontiersOverlayManager;
        ClientCollectionRuntime collections = collectionRuntime;
        ClientFrontierSyncService sync = syncService;
        MapFrontiersClientAPIImpl api = clientApi;
        ClientFrontierEvents events = frontierEvents;
        ClientCollectionEvents collectionEventsState = collectionEvents;
        ClientSettingsProfileEvents settingsEvents = settingsProfileEvents;

        globalFrontiersOverlayManager = null;
        personalFrontiersOverlayManager = null;
        collectionRuntime = null;
        syncService = null;
        clientApi = null;
        frontierEvents = null;
        collectionEvents = null;
        settingsProfileEvents = null;
        operationService = null;
        localPersonalFrontierStore = null;
        localPersonalCollectionStore = null;
        localOverrides = null;

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

    private static void closeStep(String name, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to close {}", name, t);
        }
    }
}
