package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.NbtFileHelper;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ClientMapFrontiersStorageHelper {
    private static final String MOD_DIR_NAME = "mapfrontiers";
    private static final String LEGACY_MOD_DIR_NAME = "mapfrontier";
    private static final String CONFLICTS_DIR_NAME = "mapfrontiers_conflicts";

    public static File resolveJourneyMapModDir(Minecraft client) {
        File jmWorldDir = Services.JOURNEYMAP.getJMWorldDir(client);
        File modDir = new File(jmWorldDir, MOD_DIR_NAME);
        File legacyModDir = new File(jmWorldDir, LEGACY_MOD_DIR_NAME);

        normalizeJourneyMapModDir(modDir, legacyModDir, jmWorldDir);

        //noinspection ResultOfMethodCallIgnored
        modDir.mkdirs();
        return modDir;
    }

    public static @Nullable File resolveSingleplayerModDir(Minecraft client) {
        if (!client.isLocalServer()) {
            return null;
        }

        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            return null;
        }

        File modDir = new File(NbtFileHelper.resolveServerRootDir(server), MOD_DIR_NAME);
        //noinspection ResultOfMethodCallIgnored
        modDir.mkdirs();
        return modDir;
    }

    public static File resolvePreferredModDir(Minecraft client) {
        File singleplayerModDir = resolveSingleplayerModDir(client);
        if (singleplayerModDir != null) {
            return singleplayerModDir;
        }

        return resolveJourneyMapModDir(client);
    }

    private static void normalizeJourneyMapModDir(File modDir, File legacyModDir, File jmWorldDir) {
        if (!legacyModDir.exists()) {
            return;
        }

        if (!modDir.exists()) {
            try {
                Files.move(legacyModDir.toPath(), modDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
                MapFrontiers.LOGGER.info("Renamed legacy JourneyMap data directory from {} to {}", legacyModDir, modDir);
            } catch (IOException exception) {
                MapFrontiers.LOGGER.warn("Failed to rename legacy JourneyMap data directory from {} to {}", legacyModDir, modDir, exception);
            }
            return;
        }

        File conflictsRootDir = new File(jmWorldDir, CONFLICTS_DIR_NAME);
        File conflictDir = new File(conflictsRootDir, Long.toString(System.currentTimeMillis()));
        try {
            Files.createDirectories(conflictDir.toPath());
            Files.move(legacyModDir.toPath(), conflictDir.toPath().resolve(LEGACY_MOD_DIR_NAME), StandardCopyOption.REPLACE_EXISTING);
            MapFrontiers.LOGGER.warn("Detected both {} and legacy {} JourneyMap data directories. Preserved legacy directory at {}",
                    modDir, legacyModDir, conflictDir);
        } catch (IOException exception) {
            MapFrontiers.LOGGER.warn("Failed to move legacy JourneyMap data directory {} to conflicts", legacyModDir, exception);
        }
    }

    private static void deleteIfEmpty(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        String[] children = dir.list();
        if (children == null || children.length > 0) {
            return;
        }

        try {
            Files.deleteIfExists(dir.toPath());
        } catch (IOException ignored) {
        }
    }

    private ClientMapFrontiersStorageHelper() {
    }
}
