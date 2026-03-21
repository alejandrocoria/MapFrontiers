package games.alejandrocoria.mapfrontiers.common.frontier.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ParametersAreNonnullByDefault
public class ClientLocalPersonalFrontierStore {
    private File modDir;

    public List<FrontierData> loadFrontiers() {
        ensureDirectory();
        List<FrontierData> frontiers = new ArrayList<>();
        if (modDir == null) {
            return frontiers;
        }

        CompoundTag nbtFrontiers = loadFile("personal_frontiers.dat");
        if (!nbtFrontiers.isEmpty()) {
            if (readFromNBT(nbtFrontiers, frontiers)) {
                MapFrontiers.createBackup(modDir, "personal_frontiers.dat");
                saveFrontiers(frontiers);
            }
        }

        return frontiers;
    }

    public void saveFrontiers(Collection<? extends FrontierData> frontiers) {
        ensureDirectory();
        if (modDir == null) {
            return;
        }

        CompoundTag nbtFrontiers = new CompoundTag();
        writeToNBT(nbtFrontiers, frontiers);
        saveFile("personal_frontiers.dat", nbtFrontiers);
    }

    public void clear() {
        saveFrontiers(List.of());
    }

    private boolean readFromNBT(CompoundTag nbt, List<FrontierData> frontiers) {
        boolean needBackup = false;
        try {
            int version = nbt.getIntOr("Version", 0);
            if (version == 0) {
                MapFrontiers.LOGGER.warn("Data version in personal_frontiers not found, expected {}", MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            } else if (version > MapFrontiers.FRONTIER_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in personal_frontiers higher than expected. The mod uses {}", MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            }

            ListTag frontiersTagList = nbt.getListOrEmpty("frontiers");
            for (int i = 0; i < frontiersTagList.size(); ++i) {
                FrontierData frontier = new FrontierData();
                CompoundTag frontierTag = frontiersTagList.getCompound(i).get();
                frontier.readFromNBT(frontierTag, version);
                frontiers.add(frontier);
            }
        } catch (Exception ignored) {
            return true;
        }

        return needBackup;
    }

    private void writeToNBT(CompoundTag nbt, Collection<? extends FrontierData> frontiers) {
        ListTag frontiersTagList = new ListTag();
        for (FrontierData frontier : frontiers) {
            CompoundTag frontierTag = new CompoundTag();
            frontier.writeToNBT(frontierTag);
            frontiersTagList.add(frontierTag);
        }
        nbt.put("frontiers", frontiersTagList);
        nbt.putInt("Version", MapFrontiers.FRONTIER_DATA_VERSION);
    }

    private void ensureDirectory() {
        if (modDir != null || Minecraft.getInstance().isLocalServer()) {
            return;
        }

        try {
            File jmDir = Services.JOURNEYMAP.getJMWorldDir(Minecraft.getInstance());
            modDir = new File(jmDir, "mapfrontier");
            //noinspection ResultOfMethodCallIgnored
            modDir.mkdirs();
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    private CompoundTag loadFile(String filename) {
        File file = new File(modDir, filename);
        if (file.exists()) {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                return NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        }

        return new CompoundTag();
    }

    private void saveFile(String filename, CompoundTag nbt) {
        try {
            File file = new File(modDir, filename);
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                NbtIo.writeCompressed(nbt, outputStream);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }
}
