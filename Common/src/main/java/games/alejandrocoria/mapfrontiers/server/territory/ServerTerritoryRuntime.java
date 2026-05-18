package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.network.PacketTerritoriesSnapshot;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import games.alejandrocoria.mapfrontiers.server.api.MapFrontiersServerAPIImpl;
import games.alejandrocoria.mapfrontiers.server.settings.ServerSettingsOperationService;
import games.alejandrocoria.mapfrontiers.server.territory.collection.ServerCollectionEvents;
import games.alejandrocoria.mapfrontiers.server.territory.frontier.ServerFrontierEvents;
import games.alejandrocoria.mapfrontiers.server.territory.frontier.ServerFrontierShareService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ServerTerritoryRuntime {
    private final MinecraftServer server;
    private final TerritoriesManager territoriesManager;
    private final TerritoryPermissionEvaluator permissionEvaluator;
    private final ServerTerritoryOperationService operationService;
    private final ServerFrontierEvents frontierEvents;
    private final ServerCollectionEvents collectionEvents;
    private final ServerFrontierShareService shareService;
    private final ServerSettingsOperationService settingsOperationService;
    private final MapFrontiersServerAPIImpl serverApi;

    public ServerTerritoryRuntime(MinecraftServer server) {
        this.server = server;
        this.territoriesManager = new TerritoriesManager();
        this.territoriesManager.loadOrCreateData(server);
        this.permissionEvaluator = new TerritoryPermissionEvaluator(territoriesManager);
        this.frontierEvents = new ServerFrontierEvents();
        this.collectionEvents = new ServerCollectionEvents();
        this.operationService = new ServerTerritoryOperationService(server, territoriesManager, permissionEvaluator, frontierEvents, collectionEvents);
        this.shareService = new ServerFrontierShareService(server, territoriesManager, permissionEvaluator);
        this.settingsOperationService = new ServerSettingsOperationService(server, territoriesManager, permissionEvaluator);
        this.serverApi = new MapFrontiersServerAPIImpl(operationService, frontierEvents, collectionEvents);
    }

    public ServerTerritoryOperationService getOperationService() {
        return operationService;
    }

    public ServerFrontierShareService getShareService() {
        return shareService;
    }

    public ServerSettingsOperationService getSettingsOperationService() {
        return settingsOperationService;
    }

    public MapFrontiersServerAPIImpl getServerApi() {
        return serverApi;
    }

    public void onPlayerJoined() {
        territoriesManager.ensureOwners(server);
    }

    public void onServerTick() {
        shareService.tickPendingInvitations();
        territoriesManager.tickPersistence();
    }

    public PacketSettingsProfile createSettingsProfilePacket(ServerPlayer player) {
        return permissionEvaluator.createProfilePacket(player);
    }

    public PacketTerritoriesSnapshot createTerritoriesSnapshot(ServerPlayer player) {
        PacketTerritoriesSnapshot packetTerritoriesSnapshot = new PacketTerritoriesSnapshot();
        SettingsUser playerUser = new SettingsUser(player);
        Set<UUID> includedPersonalCollectionIds = new HashSet<>();

        for (ArrayList<FrontierData> frontiers : territoriesManager.getAllGlobalFrontiers().values()) {
            packetTerritoriesSnapshot.addGlobalFrontiers(frontiers);
        }
        packetTerritoriesSnapshot.addGlobalCollections(territoriesManager.getAllGlobalCollections().stream()
                .filter(CollectionData::isPersistent)
                .toList());

        for (CollectionData collection : territoriesManager.getAllPersonalCollections(playerUser)) {
            if (!collection.isPersistent()) {
                continue;
            }
            if (includedPersonalCollectionIds.add(collection.getId())) {
                packetTerritoriesSnapshot.addPersonalCollection(collection);
            }
        }

        for (ArrayList<FrontierData> frontiers : territoriesManager.getAllPersonalFrontiers(playerUser).values()) {
            packetTerritoriesSnapshot.addPersonalFrontiers(frontiers);
            for (FrontierData frontier : frontiers) {
                if (!frontier.hasCollection()) {
                    continue;
                }

                CollectionData collection = territoriesManager.getCollectionFromID(frontier.getCollectionId());
                if (collection != null && collection.isPersistent() && includedPersonalCollectionIds.add(collection.getId())) {
                    packetTerritoriesSnapshot.addPersonalCollection(collection);
                }
            }
        }

        return packetTerritoriesSnapshot;
    }

    public void close() {
        serverApi.close();
        frontierEvents.close();
        collectionEvents.close();
        territoriesManager.close();
    }
}
