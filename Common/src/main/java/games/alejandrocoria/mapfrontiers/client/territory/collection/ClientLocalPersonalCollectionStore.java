package games.alejandrocoria.mapfrontiers.client.territory.collection;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.territory.ClientMapFrontiersStorageHelper;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtCompat;
import games.alejandrocoria.mapfrontiers.common.util.NbtFileHelper;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ParametersAreNonnullByDefault
public class ClientLocalPersonalCollectionStore {
    private File modDir;

    public List<CollectionData> loadCollections() {
        ensureDirectory();
        List<CollectionData> collections = new ArrayList<>();
        if (modDir == null) {
            return collections;
        }

        CompoundTag nbtCollections = loadFile("personal_collections.dat");
        if (!nbtCollections.isEmpty()) {
            if (readFromNBT(nbtCollections, collections)) {
                NbtFileHelper.createBackup(modDir, "personal_collections.dat");
                saveCollections(collections);
            }
        }

        return collections;
    }

    public void saveCollections(Collection<? extends CollectionData> collections) {
        ensureDirectory();
        if (modDir == null) {
            return;
        }

        List<? extends CollectionData> persistentPersonalCollections = collections.stream()
                .filter(ClientLocalPersonalCollectionStore::shouldPersist)
                .toList();

        CompoundTag nbtCollections = new CompoundTag();
        writeToNBT(nbtCollections, persistentPersonalCollections);
        saveFile("personal_collections.dat", nbtCollections);
    }

    public void saveOwnedCollectionMirror(Collection<? extends CollectionData> collections, SettingsUser currentPlayer) {
        saveCollections(collections.stream()
                .filter(ClientLocalPersonalCollectionStore::shouldPersist)
                .filter(collection -> collection.getOwner().equals(currentPlayer))
                .toList());
    }

    public void clear() {
        saveCollections(List.of());
    }

    private boolean readFromNBT(CompoundTag nbt, List<CollectionData> collections) {
        boolean needBackup = false;
        try {
            int version = NbtCompat.getIntOr(nbt, "Version", 0);
            if (version == 0) {
                MapFrontiers.LOGGER.warn("Data version in personal_collections not found, expected {}", MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            } else if (version > MapFrontiers.FRONTIER_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in personal_collections higher than expected. The mod uses {}", MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            }

            ListTag collectionsTagList = NbtCompat.getListOrEmpty(nbt, "collections");
            for (int i = 0; i < collectionsTagList.size(); ++i) {
                try {
                    CollectionData collection = new CollectionData();
                    CompoundTag collectionTag = NbtReadHelper.requireCompound(collectionsTagList, i, "collections");
                    collection.readFromNBT(collectionTag, version);
                    if (!shouldPersist(collection)) {
                        needBackup = true;
                        continue;
                    }
                    collections.add(collection);
                } catch (InvalidNbtFormatException e) {
                    MapFrontiers.LOGGER.warn("Skipping invalid personal collection at collections[{}]: {}", i, e.getMessage());
                    needBackup = true;
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.warn("Failed to read personal_collections.dat: {}", e.getMessage());
            return true;
        }

        return needBackup;
    }

    private void writeToNBT(CompoundTag nbt, Collection<? extends CollectionData> collections) {
        ListTag collectionsTagList = new ListTag();
        int skippedCollections = 0;
        for (CollectionData collection : collections) {
            try {
                CompoundTag collectionTag = new CompoundTag();
                collection.writeToNBT(collectionTag);
                collectionsTagList.add(collectionTag);
            } catch (RuntimeException e) {
                skippedCollections++;
                MapFrontiers.LOGGER.error("Skipping personal collection during local save because serialization failed. id={}, personal={}, lifetime={}",
                        collection.getId(), collection.getPersonal(), collection.getLifetime(), e);
            }
        }
        nbt.put("collections", collectionsTagList);
        nbt.putInt("Version", MapFrontiers.FRONTIER_DATA_VERSION);

        if (skippedCollections > 0) {
            MapFrontiers.LOGGER.warn("Local personal collection save skipped invalid entries. savedCollections={}, skippedCollections={}",
                    collectionsTagList.size(), skippedCollections);
        }
    }

    private void ensureDirectory() {
        if (modDir != null || Minecraft.getInstance().isLocalServer()) {
            return;
        }

        try {
            modDir = ClientMapFrontiersStorageHelper.resolveJourneyMapModDir(Minecraft.getInstance());
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
        NbtFileHelper.saveCompressedNbtSafely(modDir, filename, nbt);
    }

    private static boolean shouldPersist(CollectionData collection) {
        return collection.getPersonal() && collection.isPersistent();
    }
}
