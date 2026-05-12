package games.alejandrocoria.mapfrontiers.client.territory.collection;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.territory.ClientMapFrontiersStorageHelper;
import games.alejandrocoria.mapfrontiers.common.util.NbtFileHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CollectionUiStateStore {
    private static final String FILENAME = "collection_ui_state.dat";

    private final Set<String> collapsedRows = new HashSet<>();
    private File modDir;

    public CollectionUiStateStore() {
        loadData();
    }

    public boolean isCollapsed(String rowId) {
        return collapsedRows.contains(rowId);
    }

    public void setCollapsed(String rowId, boolean collapsed) {
        boolean changed;
        if (collapsed) {
            changed = collapsedRows.add(rowId);
        } else {
            changed = collapsedRows.remove(rowId);
        }

        if (changed) {
            saveData();
        }
    }

    public void prune(Collection<String> validRowIds) {
        if (collapsedRows.retainAll(validRowIds)) {
            saveData();
        }
    }

    private void loadData() {
        try {
            Minecraft client = Minecraft.getInstance();
            modDir = ClientMapFrontiersStorageHelper.resolvePreferredModDir(client);
            if (client.isLocalServer() && client.getSingleplayerServer() == null) {
                MapFrontiers.LOGGER.warn("Singleplayer server unavailable while resolving {} storage. Falling back to JourneyMap directory.", FILENAME);
            }

            CompoundTag nbt = loadFile(FILENAME);
            if (!nbt.isEmpty() && readFromNBT(nbt)) {
                NbtFileHelper.createBackup(modDir, FILENAME);
                saveData();
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    private boolean readFromNBT(CompoundTag nbt) {
        boolean needBackup = false;
        try {
            int version = nbt.getIntOr("Version", 0);
            if (version == 0) {
                MapFrontiers.LOGGER.warn("Data version in {} not found, expected {}", FILENAME, MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            } else if (version > MapFrontiers.FRONTIER_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in {} higher than expected. The mod uses {}", FILENAME, MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            }

            ListTag collapsedTagList = nbt.getListOrEmpty("collapsed");
            for (int i = 0; i < collapsedTagList.size(); ++i) {
                Tag rowTag = collapsedTagList.get(i);
                if (rowTag instanceof StringTag collapsedRowTag) {
                    String rowId = collapsedRowTag.value();
                    if (rowId.isEmpty()) {
                        MapFrontiers.LOGGER.warn("Skipping invalid collapsed row entry at collapsed[{}]", i);
                        needBackup = true;
                        continue;
                    }
                    collapsedRows.add(rowId);
                } else {
                    MapFrontiers.LOGGER.warn("Skipping invalid collapsed row entry at collapsed[{}]", i);
                    needBackup = true;
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.warn("Failed to read {}: {}", FILENAME, e.getMessage());
            return true;
        }

        return needBackup;
    }

    private void writeToNBT(CompoundTag nbt) {
        ListTag collapsedTagList = new ListTag();
        for (String rowId : collapsedRows) {
            collapsedTagList.add(StringTag.valueOf(rowId));
        }

        nbt.put("collapsed", collapsedTagList);
        nbt.putInt("Version", MapFrontiers.FRONTIER_DATA_VERSION);
    }

    private void saveData() {
        if (modDir == null) {
            return;
        }

        CompoundTag nbt = new CompoundTag();
        writeToNBT(nbt);
        NbtFileHelper.saveCompressedNbtSafely(modDir, FILENAME, nbt);
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
}
