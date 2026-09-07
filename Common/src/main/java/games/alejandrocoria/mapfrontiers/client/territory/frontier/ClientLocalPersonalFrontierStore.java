package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.territory.ClientMapFrontiersStorageHelper;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtReadContext;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
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
public class ClientLocalPersonalFrontierStore {
    private final PlayerNameRepository playerNames;
    private final PlayerReferenceNbtReadContext playerReferenceReadContext;
    private File modDir;

    public ClientLocalPersonalFrontierStore(PlayerNameRepository playerNames) {
        this.playerNames = playerNames;
        playerReferenceReadContext = PlayerReferenceNbtReadContext.uuidOnly(playerNames);
    }

    public List<FrontierData> loadFrontiers() {
        ensureDirectory();
        List<FrontierData> frontiers = new ArrayList<>();
        if (modDir == null) {
            return frontiers;
        }

        CompoundTag nbtFrontiers = loadFile("personal_frontiers.dat");
        if (!nbtFrontiers.isEmpty()) {
            if (readFromNBT(nbtFrontiers, frontiers)) {
                NbtFileHelper.createBackup(modDir, "personal_frontiers.dat");
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

        List<? extends FrontierData> persistentPersonalFrontiers = frontiers.stream()
                .filter(ClientLocalPersonalFrontierStore::shouldPersist)
                .toList();

        CompoundTag nbtFrontiers = new CompoundTag();
        writeToNBT(nbtFrontiers, persistentPersonalFrontiers);
        saveFile("personal_frontiers.dat", nbtFrontiers);
    }

    public void saveOwnedFrontierMirror(Collection<? extends FrontierData> frontiers, SettingsUser currentPlayer) {
        saveFrontiers(frontiers.stream()
                .filter(ClientLocalPersonalFrontierStore::shouldPersist)
                .filter(frontier -> frontier.getOwner().equals(currentPlayer))
                .toList());
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
                try {
                    CompoundTag frontierTag = NbtReadHelper.requireCompound(frontiersTagList, i, "frontiers");
                    FrontierData.NbtReadResult result = FrontierData.readFromNBT(frontierTag, version, playerReferenceReadContext);
                    FrontierData frontier = result.frontier();
                    needBackup |= result.changedDuringLoad();
                    if (!shouldPersist(frontier)) {
                        needBackup = true;
                        continue;
                    }
                    frontiers.add(frontier);
                } catch (InvalidNbtFormatException e) {
                    MapFrontiers.LOGGER.warn("Skipping invalid personal frontier at frontiers[{}]: {}", i, e.getMessage());
                    needBackup = true;
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.warn("Failed to read personal_frontiers.dat: {}", e.getMessage());
            return true;
        }

        return needBackup;
    }

    private void writeToNBT(CompoundTag nbt, Collection<? extends FrontierData> frontiers) {
        ListTag frontiersTagList = new ListTag();
        int skippedFrontiers = 0;
        for (FrontierData frontier : frontiers) {
            try {
                CompoundTag frontierTag = new CompoundTag();
                frontier.writeToNBT(frontierTag, playerNames);
                frontiersTagList.add(frontierTag);
            } catch (RuntimeException e) {
                skippedFrontiers++;
                MapFrontiers.LOGGER.error("Skipping personal frontier during local save because serialization failed. id={}, personal={}, lifetime={}",
                        frontier.getId(), frontier.getPersonal(), frontier.getLifetime(), e);
            }
        }
        nbt.put("frontiers", frontiersTagList);
        nbt.putInt("Version", MapFrontiers.FRONTIER_DATA_VERSION);

        if (skippedFrontiers > 0) {
            MapFrontiers.LOGGER.warn("Local personal frontier save skipped invalid entries. savedFrontiers={}, skippedFrontiers={}",
                    frontiersTagList.size(), skippedFrontiers);
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

    private static boolean shouldPersist(FrontierData frontier) {
        return frontier.getPersonal() && frontier.isPersistent();
    }
}
