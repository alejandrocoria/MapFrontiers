package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.api.MapFrontiersAPIBootstrap;
import games.alejandrocoria.mapfrontiers.common.api.MapFrontiersApiLogAdapter;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandshake;
import games.alejandrocoria.mapfrontiers.server.event.ServerGlobalEvents;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MapFrontiers {
    public static final String MODID = "mapfrontiers";
    public static final Logger LOGGER = LogManager.getLogger("MapFrontiers");
    public static final int FRONTIER_DATA_VERSION = 12;
    public static final int SETTINGS_DATA_VERSION = 4;

    private static ServerTerritoryRuntime serverRuntime;

    public MapFrontiers() {

    }

    protected static void init() {
        PacketHandler.init();
        MapFrontiersAPIBootstrap.setLogger(new MapFrontiersApiLogAdapter());

        ServerGlobalEvents.subscribeServerStartingEvent(MapFrontiers.class, server -> {
            serverRuntime = new ServerTerritoryRuntime(server);
            MapFrontiersAPIBootstrap.setServerAPI(serverRuntime.getServerApi());

            LOGGER.info("MapFrontiers server runtime initialized");
        });

        ServerGlobalEvents.subscribeServerStoppingEvent(MapFrontiers.class, server -> {
            if (serverRuntime != null) {
                serverRuntime.onServerStopping();
                serverRuntime.close();
            }
            MapFrontiersAPIBootstrap.clearServerAPI();
            serverRuntime = null;

            LOGGER.info("MapFrontiers server runtime stopped");
        });

        ServerGlobalEvents.subscribePlayerPermissionLevelUpdatedEvent(MapFrontiers.class, (server, player) -> {
            if (serverRuntime == null) {
                return;
            }

            PacketHandler.sendTo(serverRuntime.createSettingsProfilePacket(player), player);
        });

        ServerGlobalEvents.subscribeServerTickEvent(MapFrontiers.class, server -> {
            if (serverRuntime != null) {
                serverRuntime.onServerTick();
            }
        });
    }

    public static void ReceiveHandshake(ServerPlayer player, long nonce) {
        if (serverRuntime == null) {
            return;
        }

        PacketHandler.sendTo(new PacketHandshake(nonce), player);
        PacketHandler.sendTo(serverRuntime.createSettingsProfilePacket(player), player);
        PacketHandler.sendTo(serverRuntime.createTerritoriesSnapshot(player), player);
    }

    public static boolean isOPorHost(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server.getPlayerList().isOp(player.nameAndId());
    }

    public static ServerTerritoryRuntime getServerRuntime() {
        return serverRuntime;
    }
}
