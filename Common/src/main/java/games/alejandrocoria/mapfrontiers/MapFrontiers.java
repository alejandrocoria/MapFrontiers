package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.api.MapFrontiersAPIBootstrap;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandshake;
import games.alejandrocoria.mapfrontiers.server.event.ServerGlobalEvents;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MapFrontiers {
    public static final String MODID = "mapfrontiers";
    public static final Logger LOGGER = LogManager.getLogger("MapFrontiers");
    public static final int FRONTIER_DATA_VERSION = 10;
    public static final int SETTINGS_DATA_VERSION = 4;

    private static ServerFrontierRuntime serverRuntime;

    public MapFrontiers() {

    }

    protected static void init() {
        PacketHandler.init();

        ServerGlobalEvents.subscribeServerStartingEvent(MapFrontiers.class, server -> {
            serverRuntime = new ServerFrontierRuntime(server);
            MapFrontiersAPIBootstrap.setServerAPI(serverRuntime.getServerApi());

            LOGGER.info("ServerStartingEvent done");
        });

        ServerGlobalEvents.subscribeServerStoppingEvent(MapFrontiers.class, server -> {
            if (serverRuntime != null) {
                serverRuntime.close();
            }
            MapFrontiersAPIBootstrap.clearServerAPI();
            serverRuntime = null;

            LOGGER.info("ServerStoppingEvent done");
        });

        ServerGlobalEvents.subscribePlayerJoinedEvent(MapFrontiers.class, (server, player) -> {
            if (serverRuntime == null) {
                return;
            }

            serverRuntime.onPlayerJoined();

            LOGGER.info("PlayerJoinedEvent done (" + player.getStringUUID() + ")");
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
        PacketHandler.sendTo(serverRuntime.createFrontiersSnapshot(player), player);
    }

    public static boolean isOPorHost(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server.getPlayerList().isOp(player.nameAndId());
    }

    public static ServerFrontierRuntime getServerRuntime() {
        return serverRuntime;
    }

    public static void createBackup(File folder, String filename) {
        File file = new File(folder, filename);
        if (!file.exists()) {
            return;
        }

        Path folderPath = folder.toPath();
        Path bakFile = folderPath.resolve(filename + ".bak1");
        try {
            for (int i = 10; i > 0; i--) {
                Path oldBak = folderPath.resolve(filename + ".bak" + i);
                if (Files.exists(oldBak)) {
                    if (i >= 10)
                        Files.delete(oldBak);
                    else
                        Files.move(oldBak, folderPath.resolve(filename + ".bak" + (i + 1)));
                }
            }
            Files.copy(file.toPath(), bakFile);
        } catch (IOException exception) {
            LOGGER.warn("Failed to back up file {}", file.toPath(), exception);
        }
    }
}

