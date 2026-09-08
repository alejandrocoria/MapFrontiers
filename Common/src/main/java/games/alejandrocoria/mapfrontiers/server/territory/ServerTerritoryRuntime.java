package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketPlayerNameMappings;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.network.PacketTerritoriesSnapshot;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.server.api.MapFrontiersServerAPIImpl;
import games.alejandrocoria.mapfrontiers.server.identity.ServerPlayerIdFactory;
import games.alejandrocoria.mapfrontiers.server.identity.ServerPlayerIdLookup;
import games.alejandrocoria.mapfrontiers.server.settings.ServerSettingsOperationService;
import games.alejandrocoria.mapfrontiers.server.territory.collection.ServerCollectionEvents;
import games.alejandrocoria.mapfrontiers.server.territory.frontier.ServerFrontierEvents;
import games.alejandrocoria.mapfrontiers.server.territory.frontier.ServerFrontierShareService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ServerTerritoryRuntime {
    private final MinecraftServer server;
    private final PlayerNameRepository playerNameRepository;
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
        this.playerNameRepository = new PlayerNameRepository();
        this.territoriesManager = new TerritoriesManager(playerNameRepository, new ServerPlayerIdLookup(server));
        this.territoriesManager.loadOrCreateData(server);
        hydratePlayerNamesFromMinecraftCache();
        this.permissionEvaluator = new TerritoryPermissionEvaluator(territoriesManager);
        this.frontierEvents = new ServerFrontierEvents();
        this.collectionEvents = new ServerCollectionEvents();
        this.operationService = new ServerTerritoryOperationService(server, territoriesManager, permissionEvaluator, frontierEvents, collectionEvents, playerNameRepository);
        this.shareService = new ServerFrontierShareService(server, territoriesManager, permissionEvaluator, playerNameRepository);
        this.settingsOperationService = new ServerSettingsOperationService(server, territoriesManager, permissionEvaluator, playerNameRepository);
        this.serverApi = new MapFrontiersServerAPIImpl(operationService, frontierEvents, collectionEvents, playerNameRepository);
    }

    public ServerTerritoryOperationService getOperationService() {
        return operationService;
    }

    public PlayerNameRepository getPlayerNameRepository() {
        return playerNameRepository;
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

    public void onServerTick() {
        shareService.tickPendingInvitations();
        territoriesManager.tickPersistence();
    }

    public void onServerStopping() {
        territoriesManager.flushTerritoriesOnShutdown();
    }

    public void onPlayerJoined(ServerPlayer player) {
        PlayerId playerId = ServerPlayerIdFactory.from(player);
        if (!playerNameRepository.observe(playerId, player.getGameProfile().name(), PlayerNameSource.CONNECTED_PROFILE)) {
            return;
        }

        if (territoriesManager.getReferencedPlayerIds().contains(playerId)) {
            territoriesManager.markPlayerNameHintsDirty();
        }

        PacketPlayerNameMappings playerNameMappings = new PacketPlayerNameMappings(playerId, playerNameRepository.resolveName(playerId));
        if (!playerNameMappings.isEmpty()) {
            PacketHandler.sendToAll(playerNameMappings, server);
        }
    }

    public PacketSettingsProfile createSettingsProfilePacket(ServerPlayer player) {
        return permissionEvaluator.createProfilePacket(player);
    }

    public PacketTerritoriesSnapshot createTerritoriesSnapshot(ServerPlayer player) {
        PacketTerritoriesSnapshot packetTerritoriesSnapshot = new PacketTerritoriesSnapshot();
        PlayerId playerUser = ServerPlayerIdFactory.from(player);
        Set<UUID> includedPersonalCollectionIds = new HashSet<>();

        for (FrontierData frontier : territoriesManager.iterateGlobalFrontiers()) {
            packetTerritoriesSnapshot.addGlobalFrontier(frontier);
        }
        for (CollectionData collection : territoriesManager.iterateGlobalCollections()) {
            if (collection.isPersistent()) {
                packetTerritoriesSnapshot.addGlobalCollection(collection);
            }
        }

        for (CollectionData collection : territoriesManager.iteratePersonalCollections(playerUser)) {
            if (!collection.isPersistent()) {
                continue;
            }
            if (includedPersonalCollectionIds.add(collection.getId())) {
                packetTerritoriesSnapshot.addPersonalCollection(collection);
            }
        }

        for (FrontierData frontier : territoriesManager.iteratePersonalFrontiers(playerUser)) {
            packetTerritoriesSnapshot.addPersonalFrontier(frontier);
            if (!frontier.hasCollection()) {
                continue;
            }

            CollectionData collection = territoriesManager.getCollectionFromID(frontier.getCollectionId());
            if (collection != null && collection.isPersistent() && includedPersonalCollectionIds.add(collection.getId())) {
                packetTerritoriesSnapshot.addPersonalCollection(collection);
            }
        }

        return packetTerritoriesSnapshot;
    }

    public PacketPlayerNameMappings createPlayerNameMappings(Iterable<PlayerId> playerIds) {
        return new PacketPlayerNameMappings(playerIds, playerNameRepository);
    }

    public void close() {
        serverApi.close();
        frontierEvents.close();
        collectionEvents.close();
        playerNameRepository.close();
    }

    private void hydratePlayerNamesFromMinecraftCache() {
        boolean changed = false;
        for (PlayerId playerId : territoriesManager.getReferencedPlayerIds()) {
            Optional<String> username = server.services().nameToIdCache().get(playerId.uuid())
                    .map(nameAndId -> nameAndId.name());
            if (username.isPresent()) {
                changed |= playerNameRepository.observe(playerId, username.get(), PlayerNameSource.MINECRAFT_CACHE);
            }
        }

        if (changed) {
            territoriesManager.markPlayerNameHintsDirty();
        }
    }
}
