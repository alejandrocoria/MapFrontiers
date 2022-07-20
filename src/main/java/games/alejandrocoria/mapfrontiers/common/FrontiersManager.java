package games.alejandrocoria.mapfrontiers.common;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.common.util.ContainerHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

@ParametersAreNonnullByDefault
public class FrontiersManager {
    public static FrontiersManager instance;

    private final HashMap<UUID, FrontierData> allFrontiers;
    private final HashMap<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsGlobalFrontiers;
    private final HashMap<SettingsUser, HashMap<ResourceKey<Level>, ArrayList<FrontierData>>> usersDimensionsPersonalFrontiers;
    private final HashMap<Integer, PendingShareFrontier> pendingShareFrontiers;
    private int pendingShareFrontiersTick = 0;
    private FrontierSettings frontierSettings;
    private File ModDir;
    private boolean frontierOwnersChecked = false;

    public static final int dataVersion = 8;
    private static int pendingShareFrontierID = 0;
    private static final int pendingShareFrontierTickDuration = 1200;

    public FrontiersManager() {
        instance = this;
        allFrontiers = new HashMap<>();
        dimensionsGlobalFrontiers = new HashMap<>();
        usersDimensionsPersonalFrontiers = new HashMap<>();
        pendingShareFrontiers = new HashMap<>();
        frontierSettings = new FrontierSettings();

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            ++pendingShareFrontiersTick;

            if (pendingShareFrontiersTick >= 100) {
                pendingShareFrontiersTick -= 100;

                for (PendingShareFrontier pending : pendingShareFrontiers.values()) {
                    pending.tickCount += 100;

                    if (pending.tickCount >= pendingShareFrontierTickDuration) {
                        FrontierData frontier = getFrontierFromID(pending.frontierID);
                        if (frontier == null) {
                            continue;
                        }

                        if (frontier.getUsersShared() != null) {
                            boolean removed = frontier.getUsersShared().removeIf(x -> x.getUser().equals(pending.targetUser));
                            if (removed) {
                                PacketHandler.sendToUsersWithAccess(PacketFrontierUpdated.class, new PacketFrontierUpdated(frontier), frontier, server);
                            }
                        }
                    }
                }

                pendingShareFrontiers.entrySet().removeIf(x -> x.getValue().tickCount >= pendingShareFrontierTickDuration);
            }
        });
    }

    public void setSettings(FrontierSettings frontierSettings) {
        this.frontierSettings = frontierSettings;
        saveData();
    }

    public FrontierSettings getSettings() {
        return frontierSettings;
    }

    public Map<ResourceKey<Level>, ArrayList<FrontierData>> getAllGlobalFrontiers() {
        return dimensionsGlobalFrontiers;
    }

    public List<FrontierData> getAllGlobalFrontiers(ResourceKey<Level> dimension) {
        return dimensionsGlobalFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public Map<ResourceKey<Level>, ArrayList<FrontierData>> getAllPersonalFrontiers(SettingsUser user) {
        return usersDimensionsPersonalFrontiers.computeIfAbsent(user, k -> new HashMap<>());
    }

    public List<FrontierData> getAllPersonalFrontiers(SettingsUser user, ResourceKey<Level> dimension) {
        HashMap<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers
                .computeIfAbsent(user, k -> new HashMap<>());

        return dimensionsPersonalFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public FrontierData getFrontierFromID(UUID id) {
        return allFrontiers.get(id);
    }

    public FrontierData createNewGlobalFrontier(ResourceKey<Level> dimension, ServerPlayer player, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        List<FrontierData> frontiers = getAllGlobalFrontiers(dimension);

        return createNewFrontier(frontiers, dimension, false, player, vertices, chunks);
    }

    public FrontierData createNewPersonalFrontier(ResourceKey<Level> dimension, ServerPlayer player, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        List<FrontierData> frontiers = getAllPersonalFrontiers(new SettingsUser(player), dimension);

        return createNewFrontier(frontiers, dimension, true, player, vertices, chunks);
    }

    private FrontierData createNewFrontier(List<FrontierData> frontiers, ResourceKey<Level> dimension, boolean personal, ServerPlayer player, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        FrontierData frontier = new FrontierData();
        frontier.setId(UUID.randomUUID());
        frontier.setOwner(new SettingsUser(player));
        frontier.setDimension(dimension);
        frontier.setPersonal(personal);
        frontier.setColor(ColorHelper.getRandomColor());
        frontier.setCreated(new Date());

        if (vertices != null) {
            frontier.setMode(FrontierData.Mode.Vertex);
            for (BlockPos vertex : vertices) {
                frontier.addVertex(vertex);
            }
        }

        if (chunks != null) {
            frontier.setMode(FrontierData.Mode.Chunk);
            for (ChunkPos chunk : chunks) {
                frontier.toggleChunk(chunk);
            }
        }

        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);
        saveData();

        return frontier;
    }

    public void addPersonalFrontier(SettingsUser user, FrontierData frontier) {
        List<FrontierData> frontiers = this.getAllPersonalFrontiers(user, frontier.getDimension());
        frontiers.add(frontier);
        saveData();
    }

    public boolean deleteGlobalFrontier(ResourceKey<Level> dimension, UUID id) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(dimension);
        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.id.equals(id));
        deleted |= allFrontiers.remove(id) != null;
        saveData();

        return deleted;
    }

    public boolean deletePersonalFrontier(SettingsUser user, ResourceKey<Level> dimension, UUID id) {
        Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(dimension);
        if (frontiers == null) {
            return false;
        }

        for (FrontierData frontier : frontiers) {
            if (frontier.getOwner().equals(user)) {
                allFrontiers.remove(id);
                break;
            }
        }

        boolean deleted = frontiers.removeIf(x -> x.id.equals(id));

        saveData();

        return deleted;
    }

    public boolean updateGlobalFrontier(FrontierData updatedFrontier) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(updatedFrontier.getDimension());
        if (frontiers == null) {
            return false;
        }

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(updatedFrontier.getId()));

        if (index < 0) {
            return false;
        }

        updatedFrontier.setModified(new Date());

        FrontierData frontier = frontiers.get(index);
        frontier.updateFromData(updatedFrontier);

        saveData();

        return true;
    }

    public boolean updatePersonalFrontier(SettingsUser user, FrontierData updatedFrontier) {
        Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(updatedFrontier.getDimension());
        if (frontiers == null) {
            return false;
        }

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(updatedFrontier.getId()));

        if (index < 0) {
            return false;
        }

        updatedFrontier.setModified(new Date());

        FrontierData frontier = frontiers.get(index);
        frontier.updateFromData(updatedFrontier);

        saveData();

        return true;
    }

    public boolean hasPersonalFrontier(SettingsUser user, UUID frontierID) {
        for (List<FrontierData> frontiers : getAllPersonalFrontiers(user).values()) {
            for (FrontierData frontier : frontiers) {
                if (frontier.getId().equals(frontierID)) {
                    return true;
                }
            }
        }

        return false;
    }

    public int addShareMessage(SettingsUser targetUser, UUID frontierID) {
        ++pendingShareFrontierID;
        pendingShareFrontiers.put(pendingShareFrontierID, new PendingShareFrontier(frontierID, targetUser));

        return pendingShareFrontierID;
    }

    public PendingShareFrontier getPendingShareFrontier(int messageID) {
        return pendingShareFrontiers.get(messageID);
    }

    public void removePendingShareFrontier(int messageID) {
        pendingShareFrontiers.remove(messageID);
    }

    public void removePendingShareFrontier(SettingsUser user) {
        pendingShareFrontiers.entrySet().removeIf(x -> x.getValue().targetUser.equals(user));
    }

    public boolean canSendCommandAcceptFrontier(ServerPlayer player) {
        return frontierSettings.checkAction(FrontierSettings.Action.PersonalFrontier, new SettingsUser(player),
                MapFrontiers.isOPorHost(player), null);
    }

    public void ensureOwners(MinecraftServer server) {
        if (frontierOwnersChecked) {
            return;
        }

        for (FrontierData frontier : allFrontiers.values()) {
            frontier.ensureOwner(server);
        }

        frontierOwnersChecked = true;
    }

    private void readFromNBT(CompoundTag nbt) {
        int version = nbt.getInt("Version");
        if (version == 0) {
            MapFrontiers.LOGGER.warn("Data version in frontiers not found, expected " + dataVersion);
        } else if (version < 5) {
            MapFrontiers.LOGGER
                    .warn("Data version in frontiers lower than expected. The mod support from 5 to " + dataVersion);
        } else if (version > dataVersion) {
            MapFrontiers.LOGGER
                    .warn("Data version in frontiers higher than expected. The mod uses " + dataVersion);
        }

        ListTag allFrontiersTagList = nbt.getList("frontiers", Tag.TAG_COMPOUND);
        for (int i = 0; i < allFrontiersTagList.size(); ++i) {
            FrontierData frontier = new FrontierData();
            CompoundTag frontierTag = allFrontiersTagList.getCompound(i);
            frontier.readFromNBT(frontierTag, version);
            allFrontiers.put(frontier.getId(), frontier);
        }

        ListTag dimensionsTagList = nbt.getList("global", Tag.TAG_COMPOUND);
        readFrontiersFromTagList(dimensionsTagList, dimensionsGlobalFrontiers, version);

        ListTag personalTagList = nbt.getList("personal", Tag.TAG_COMPOUND);
        for (int i = 0; i < personalTagList.size(); ++i) {
            CompoundTag personalTag = personalTagList.getCompound(i);

            SettingsUser owner = new SettingsUser();
            CompoundTag ownerTag = personalTag.getCompound("owner");
            if (ownerTag.isEmpty()) {
                continue;
            }
            owner.readFromNBT(ownerTag);

            HashMap<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = new HashMap<>();
            dimensionsTagList = personalTag.getList("frontiers", Tag.TAG_COMPOUND);
            readFrontiersFromTagList(dimensionsTagList, dimensionsPersonalFrontiers, version);

            usersDimensionsPersonalFrontiers.put(owner, dimensionsPersonalFrontiers);
        }
    }

    private void readFrontiersFromTagList(ListTag dimensionsTagList, Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsFrontiers, int version) {
        for (int i = 0; i < dimensionsTagList.size(); ++i) {
            CompoundTag dimensionTag = dimensionsTagList.getCompound(i);
            ResourceKey<Level> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimensionTag.getString("dimension")));
            ListTag frontiersTagList = dimensionTag.getList("frontiers", Tag.TAG_COMPOUND);
            ArrayList<FrontierData> frontiers = new ArrayList<>();
            for (int i2 = 0; i2 < frontiersTagList.size(); ++i2) {
                CompoundTag frontierTag = frontiersTagList.getCompound(i2);
                FrontierData frontier = allFrontiers.get(UUID.fromString(frontierTag.getString("id")));
                if (frontier != null) {
                    frontier.removePendingUsersShared();
                    frontiers.add(frontier);
                } else {
                    String message = "Nonexistent frontier with UUID %1$s referenced in frontiers.dat";
                    message = String.format(message, frontierTag.getString("id"));
                    MapFrontiers.LOGGER.warn(message);
                }
            }
            dimensionsFrontiers.put(dimension, frontiers);
        }
    }

    private void writeToNBT(CompoundTag nbt) {
        ListTag allFrontiersTagList = new ListTag();
        for (FrontierData frontier : allFrontiers.values()) {
            CompoundTag frontierTag = new CompoundTag();
            frontier.writeToNBT(frontierTag);
            allFrontiersTagList.add(frontierTag);
        }
        nbt.put("frontiers", allFrontiersTagList);

        ListTag dimensionsTagList = new ListTag();
        writeFrontiersToTagList(dimensionsTagList, dimensionsGlobalFrontiers);
        nbt.put("global", dimensionsTagList);

        ListTag personalTagList = new ListTag();
        for (Map.Entry<SettingsUser, HashMap<ResourceKey<Level>, ArrayList<FrontierData>>> personal : usersDimensionsPersonalFrontiers
                .entrySet()) {
            if (personal.getValue().isEmpty()) {
                continue;
            }

            CompoundTag userFrontiers = new CompoundTag();

            dimensionsTagList = new ListTag();
            writeFrontiersToTagList(dimensionsTagList, personal.getValue());
            userFrontiers.put("frontiers", dimensionsTagList);

            if (dimensionsTagList.isEmpty()) {
                continue;
            }

            CompoundTag nbtOwner = new CompoundTag();
            personal.getKey().writeToNBT(nbtOwner);
            userFrontiers.put("owner", nbtOwner);

            personalTagList.add(userFrontiers);
        }
        nbt.put("personal", personalTagList);

        nbt.putInt("Version", dataVersion);
    }

    private void writeFrontiersToTagList(ListTag dimensionsTagList,
            Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsFrontiers) {
        for (Map.Entry<ResourceKey<Level>, ArrayList<FrontierData>> frontiers : dimensionsFrontiers.entrySet()) {
            ListTag frontiersTagList = new ListTag();
            for (FrontierData frontier : frontiers.getValue()) {
                CompoundTag frontierTag = new CompoundTag();
                frontierTag.putString("id", frontier.getId().toString());
                frontiersTagList.add(frontierTag);
            }

            if (frontiersTagList.isEmpty()) {
                continue;
            }

            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("dimension", frontiers.getKey().location().toString());
            dimensionTag.put("frontiers", frontiersTagList);
            dimensionsTagList.add(dimensionTag);
        }
    }

    public void loadOrCreateData(MinecraftServer server) {
        try {
            if (server.isDedicatedServer()) {
                File mcDir = server.getServerDirectory();
                ModDir = new File(mcDir, "mapfrontiers");
                //noinspection ResultOfMethodCallIgnored
                ModDir.mkdirs();
            } else {
                File WorldDir = Minecraft.getInstance().getSingleplayerServer().getWorldPath(LevelResource.ROOT).toFile();
                ModDir = new File(WorldDir, "mapfrontiers");
                //noinspection ResultOfMethodCallIgnored
                ModDir.mkdirs();
            }

            CompoundTag nbtFrontiers = loadFile("frontiers.dat");
            if (nbtFrontiers.isEmpty()) {
                writeToNBT(nbtFrontiers);
                saveFile("frontiers.dat", nbtFrontiers);
            } else {
                readFromNBT(nbtFrontiers);
            }

            CompoundTag nbtSettings = loadFile("settings.dat");
            if (nbtSettings.isEmpty()) {
                frontierSettings.resetToDefault();
                frontierSettings.writeToNBT(nbtSettings);
                saveFile("settings.dat", nbtSettings);
            } else {
                frontierSettings.readFromNBT(nbtSettings);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    public void saveData() {
        CompoundTag nbtFrontiers = new CompoundTag();
        writeToNBT(nbtFrontiers);
        saveFile("frontiers.dat", nbtFrontiers);

        CompoundTag nbtSettings = new CompoundTag();
        frontierSettings.writeToNBT(nbtSettings);
        saveFile("settings.dat", nbtSettings);
    }

    private CompoundTag loadFile(String filename) {
        File f = new File(ModDir, filename);
        if (f.exists()) {
            try (FileInputStream inputStream = new FileInputStream(f)) {
                return NbtIo.readCompressed(inputStream);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        }

        return new CompoundTag();
    }

    private void saveFile(String fileName, CompoundTag nbt) {
        try {
            File f = new File(ModDir, fileName);
            try (FileOutputStream outputStream = new FileOutputStream(f)) {
                NbtIo.writeCompressed(nbt, outputStream);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }
}
