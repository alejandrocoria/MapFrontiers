package games.alejandrocoria.mapfrontiers.server.frontier;

import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.server.api.MapFrontiersServerAPIImpl;
import games.alejandrocoria.mapfrontiers.server.settings.ServerSettingsOperationService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ServerFrontierRuntime {
    private final MinecraftServer server;
    private final FrontiersManager frontiersManager;
    private final FrontierPermissionEvaluator permissionEvaluator;
    private final ServerFrontierOperationService operationService;
    private final ServerFrontierEvents frontierEvents;
    private final ServerCollectionEvents collectionEvents;
    private final ServerFrontierShareService shareService;
    private final ServerSettingsOperationService settingsOperationService;
    private final MapFrontiersServerAPIImpl serverApi;

    public ServerFrontierRuntime(MinecraftServer server) {
        this.server = server;
        this.frontiersManager = new FrontiersManager();
        this.frontiersManager.loadOrCreateData(server);
        this.permissionEvaluator = new FrontierPermissionEvaluator(frontiersManager);
        this.frontierEvents = new ServerFrontierEvents();
        this.collectionEvents = new ServerCollectionEvents();
        this.operationService = new ServerFrontierOperationService(server, frontiersManager, permissionEvaluator, frontierEvents, collectionEvents);
        this.shareService = new ServerFrontierShareService(server, frontiersManager, permissionEvaluator);
        this.settingsOperationService = new ServerSettingsOperationService(server, frontiersManager, permissionEvaluator);
        this.serverApi = new MapFrontiersServerAPIImpl(operationService, frontierEvents, collectionEvents);
    }

    public ServerFrontierOperationService getOperationService() {
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
        frontiersManager.ensureOwners(server);
    }

    public void onServerTick() {
        shareService.tickPendingInvitations();
        frontiersManager.flushPendingFrontierUpdates();
    }

    public PacketSettingsProfile createSettingsProfilePacket(ServerPlayer player) {
        return permissionEvaluator.createProfilePacket(player);
    }

    public PacketFrontiers createFrontiersSnapshot(ServerPlayer player) {
        PacketFrontiers packetFrontiers = new PacketFrontiers();
        SettingsUser playerUser = new SettingsUser(player);
        Set<UUID> includedPersonalCollectionIds = new HashSet<>();

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllGlobalFrontiers().values()) {
            packetFrontiers.addGlobalFrontiers(frontiers);
        }
        packetFrontiers.addGlobalCollections(frontiersManager.getAllGlobalCollections().stream()
                .filter(CollectionData::isPersistent)
                .toList());

        for (CollectionData collection : frontiersManager.getAllPersonalCollections(playerUser)) {
            if (!collection.isPersistent()) {
                continue;
            }
            if (includedPersonalCollectionIds.add(collection.getId())) {
                packetFrontiers.addPersonalCollection(collection);
            }
        }

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(playerUser).values()) {
            packetFrontiers.addPersonalFrontiers(frontiers);
            for (FrontierData frontier : frontiers) {
                if (!frontier.hasCollection()) {
                    continue;
                }

                CollectionData collection = frontiersManager.getCollectionFromID(frontier.getCollectionId());
                if (collection != null && collection.isPersistent() && includedPersonalCollectionIds.add(collection.getId())) {
                    packetFrontiers.addPersonalCollection(collection);
                }
            }
        }

        return packetFrontiers;
    }

    public void close() {
        serverApi.close();
        frontierEvents.close();
        collectionEvents.close();
        frontiersManager.close();
    }
}
