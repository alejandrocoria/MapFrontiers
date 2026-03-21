package games.alejandrocoria.mapfrontiers.common.frontier.server;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.api.server.MapFrontiersServerAPIImpl;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@ParametersAreNonnullByDefault
public class ServerFrontierRuntime {
    private final MinecraftServer server;
    private final FrontiersManager frontiersManager;
    private final FrontierPermissionEvaluator permissionEvaluator;
    private final ServerFrontierCommandService commandService;
    private final ServerFrontierEvents frontierEvents;
    private final ServerFrontierShareService shareService;
    private final ServerSettingsService settingsService;
    private final MapFrontiersServerAPIImpl serverApi;

    public ServerFrontierRuntime(MinecraftServer server) {
        this.server = server;
        this.frontiersManager = new FrontiersManager();
        this.frontiersManager.loadOrCreateData(server);
        this.permissionEvaluator = new FrontierPermissionEvaluator(frontiersManager);
        this.commandService = new ServerFrontierCommandService(server, frontiersManager, permissionEvaluator);
        this.frontierEvents = new ServerFrontierEvents();
        this.shareService = new ServerFrontierShareService(server, frontiersManager, permissionEvaluator);
        this.settingsService = new ServerSettingsService(server, frontiersManager, permissionEvaluator);
        this.serverApi = new MapFrontiersServerAPIImpl(commandService, frontierEvents);
    }

    public ServerFrontierCommandService getCommandService() {
        return commandService;
    }

    public ServerFrontierShareService getShareService() {
        return shareService;
    }

    public ServerSettingsService getSettingsService() {
        return settingsService;
    }

    public MapFrontiersServerAPIImpl getServerApi() {
        return serverApi;
    }

    public void onPlayerJoined(ServerPlayer player) {
        frontiersManager.ensureOwners(server);
    }

    public void onServerTick() {
        shareService.tickPendingInvitations();
    }

    public PacketSettingsProfile createSettingsProfilePacket(ServerPlayer player) {
        return permissionEvaluator.createProfilePacket(player);
    }

    public PacketFrontiers createFrontiersSnapshot(ServerPlayer player) {
        PacketFrontiers packetFrontiers = new PacketFrontiers();

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllGlobalFrontiers().values()) {
            packetFrontiers.addGlobalFrontiers(frontiers);
        }

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(new SettingsUser(player)).values()) {
            packetFrontiers.addPersonalFrontiers(frontiers);
        }

        return packetFrontiers;
    }

    public void close() {
        serverApi.close();
        frontierEvents.close();
        frontiersManager.close();
    }
}
