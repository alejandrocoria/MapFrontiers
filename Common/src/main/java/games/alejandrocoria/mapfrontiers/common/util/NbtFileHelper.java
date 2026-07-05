package games.alejandrocoria.mapfrontiers.common.util;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class NbtFileHelper {
    public static File resolveServerRootDir(MinecraftServer server) {
        File mcDir;
        if (server.isDedicatedServer()) {
            mcDir = server.getServerDirectory();
        } else {
            mcDir = server.getWorldPath(LevelResource.ROOT).toFile();
        }
        if (mcDir.getPath().isEmpty()) {
            mcDir = new File(".");
        }
        return mcDir;
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
            MapFrontiers.LOGGER.warn("Failed to back up file {}", file.toPath(), exception);
        }
    }

    public static void saveCompressedNbtSafely(File folder, String filename, CompoundTag nbt) {
        Path filePath = folder.toPath().resolve(filename);
        Path tempPath = filePath.resolveSibling(filename + ".tmp");
        Path oldFilePath = filePath.resolveSibling(filename + "_old");

        try {
            try (FileOutputStream outputStream = new FileOutputStream(tempPath.toFile())) {
                NbtIo.writeCompressed(nbt, outputStream);
                try {
                    outputStream.getFD().sync();
                } catch (IOException e) {
                    MapFrontiers.LOGGER.debug("Failed to sync temp file {}", tempPath);
                }
            }

            Files.deleteIfExists(oldFilePath);
            if (Files.exists(filePath)) {
                Files.move(filePath, oldFilePath, StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                Files.move(tempPath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error("Failed to save file {}", filePath, e);
        }
    }

    public static boolean hasRelatedFiles(File folder, String filename) {
        Path folderPath = folder.toPath();
        for (String relatedFilename : relatedFilenames(filename)) {
            if (Files.exists(folderPath.resolve(relatedFilename))) {
                return true;
            }
        }
        return false;
    }

    public static void moveRelatedFiles(File sourceFolder, File targetFolder, String filename) {
        try {
            Files.createDirectories(targetFolder.toPath());
            for (String relatedFilename : relatedFilenames(filename)) {
                Path sourcePath = sourceFolder.toPath().resolve(relatedFilename);
                if (Files.exists(sourcePath)) {
                    Files.move(sourcePath, targetFolder.toPath().resolve(relatedFilename), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException exception) {
            MapFrontiers.LOGGER.warn("Failed to move related files for {}", filename, exception);
        }
    }

    public static File moveRelatedFilesToConflictDir(File sourceFolder, File conflictRootFolder, String filename) {
        File conflictDir = new File(conflictRootFolder, Long.toString(System.currentTimeMillis()));
        moveRelatedFiles(sourceFolder, conflictDir, filename);
        return conflictDir;
    }

    private static String[] relatedFilenames(String filename) {
        String[] filenames = new String[12];
        filenames[0] = filename;
        filenames[1] = filename + "_old";
        for (int i = 1; i <= 10; ++i) {
            filenames[i + 1] = filename + ".bak" + i;
        }
        return filenames;
    }

    private NbtFileHelper() {
    }
}
