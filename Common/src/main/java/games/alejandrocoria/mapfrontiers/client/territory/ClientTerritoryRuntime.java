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
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameEvents;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import journeymap.api.v2.client.IClientAPI;
import net.minecraft.client.Minecraft;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ClientTerritoryRuntime {
    private final IClientAPI journeyMapApi;
    private PlayerNameRepository playerNameRepository;
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
        if (playerNameRepository == null) {
            playerNameRepository = new PlayerNameRepository();
        }

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
            localPersonalFrontierStore = new ClientLocalPersonalFrontierStore(playerNameRepository);
        }

        if (localPersonalCollectionStore == null) {
            localPersonalCollectionStore = new ClientLocalPersonalCollectionStore(playerNameRepository);
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

        playerNameRepository.getEvents().subscribeChanged(this, this::onPlayerNameChanged);
        observeLocalPlayerProfile();

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

    public PlayerNameRepository getPlayerNameRepository() {
        ensureInitialized();
        return playerNameRepository;
    }

    public PlayerNameEvents getPlayerNameEvents() {
        ensureInitialized();
        return playerNameRepository.getEvents();
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
        ClientTerritoryOperationService operations = operationService;
        PlayerNameRepository names = playerNameRepository;

        if (operations != null) {
            operations.clearPendingOptimisticUpdates();
        }

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
        playerNameRepository = null;

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
        closeStep("player name repository", () -> {
            if (names != null) {
                names.close();
            }
        });
    }

    private void flushPendingLocalPersistenceOnClose() {
        ensureInitialized();
        localPersistenceCoordinator.flushOnClose();
    }

    private void observeLocalPlayerProfile() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        playerNameRepository.observe(new PlayerId(minecraft.player.getUUID()), minecraft.player.getGameProfile().name(),
                PlayerNameSource.CONNECTED_PROFILE);
    }

    private void onPlayerNameChanged(PlayerId playerId) {
        localPersistenceCoordinator.onPlayerNameChanged(playerId);
        globalFrontiersOverlayManager.markPlayerNamePresentationDirty(playerId);
        personalFrontiersOverlayManager.markPlayerNamePresentationDirty(playerId);
        collectionOverlayManager.markPlayerNamePresentationDirty(playerId);
    }

    private static void closeStep(String name, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to close {}", name, t);
        }
    }
}
