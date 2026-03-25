package games.alejandrocoria.mapfrontiers.common.util;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class NbtFileHelper {
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

    private NbtFileHelper() {
    }
}
