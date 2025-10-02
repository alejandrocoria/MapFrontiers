package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.event.EventHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class MapFrontiers {
    public static final String MODID = "mapfrontiers";
    public static final Logger LOGGER = LogManager.getLogger("MapFrontiers");
    public static final int FRONTIER_DATA_VERSION = 10;
    public static final int SETTINGS_DATA_VERSION = 4;

    private static FrontiersManager frontiersManager;

    public MapFrontiers() {

    }

    protected static void init() {
        PacketHandler.init();

        EventHandler.subscribeServerStartingEvent(MapFrontiers.class, server -> {
            frontiersManager = new FrontiersManager();
            frontiersManager.loadOrCreateData(server);

            LOGGER.info("ServerStartingEvent done");
        });

        EventHandler.subscribeServerStoppingEvent(MapFrontiers.class, server -> {
            if (frontiersManager != null) {
                frontiersManager.close();
            }
            frontiersManager = null;

            LOGGER.info("ServerStoppingEvent done");
        });

        EventHandler.subscribePlayerJoinedEvent(MapFrontiers.class, (server, player) -> {
            if (frontiersManager == null) {
                return;
            }

            frontiersManager.ensureOwners(server);

            LOGGER.info("PlayerJoinedEvent done (" + player.getStringUUID() + ")");
        });
    }

    public static void ReceiveHandshake(ServerPlayer player) {
        PacketHandler.sendTo(new PacketSettingsProfile(frontiersManager.getSettings().getProfile(player)), player);

        PacketFrontiers packetFrontiers = new PacketFrontiers();
        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllGlobalFrontiers().values()) {
            packetFrontiers.addGlobalFrontiers(frontiers);
        }

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(new SettingsUser(player)).values()) {
            packetFrontiers.addPersonalFrontiers(frontiers);
        }

        PacketHandler.sendTo(packetFrontiers, player);
    }

    public static boolean isOPorHost(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server.getPlayerList().isOp(player.nameAndId());
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
