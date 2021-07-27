package games.alejandrocoria.mapfrontiers.common;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ContainerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@ParametersAreNonnullByDefault
public class FrontiersManager {
    public static FrontiersManager instance;

    private final HashMap<UUID, FrontierData> allFrontiers;
    private final HashMap<RegistryKey<World>, ArrayList<FrontierData>> dimensionsGlobalFrontiers;
    private final HashMap<SettingsUser, HashMap<RegistryKey<World>, ArrayList<FrontierData>>> usersDimensionsPersonalFrontiers;
    private final HashMap<Integer, PendingShareFrontier> pendingShareFrontiers;
    private int pendingShareFrontiersTick = 0;
    private FrontierSettings frontierSettings;
    private final Random rand = new Random();
    private File ModDir;
    private boolean frontierOwnersChecked = false;

    public static final int dataVersion = 5;
    private static int pendingShareFrontierID = 0;
    private static final int pendingShareFrontierTickDuration = 1200;

    public FrontiersManager() {
        instance = this;
        allFrontiers = new HashMap<>();
        dimensionsGlobalFrontiers = new HashMap<>();
        usersDimensionsPersonalFrontiers = new HashMap<>();
        pendingShareFrontiers = new HashMap<>();
        frontierSettings = new FrontierSettings();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
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
                                PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier), frontier);
                            }
                        }
                    }
                }

                pendingShareFrontiers.entrySet().removeIf(x -> x.getValue().tickCount >= pendingShareFrontierTickDuration);
            }
        }
    }

    public void setSettings(FrontierSettings frontierSettings) {
        this.frontierSettings = frontierSettings;
        saveData();
    }

    public FrontierSettings getSettings() {
        return frontierSettings;
    }

    public Map<RegistryKey<World>, ArrayList<FrontierData>> getAllGlobalFrontiers() {
        return dimensionsGlobalFrontiers;
    }

    public List<FrontierData> getAllGlobalFrontiers(RegistryKey<World> dimension) {
        return dimensionsGlobalFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public Map<RegistryKey<World>, ArrayList<FrontierData>> getAllPersonalFrontiers(SettingsUser user) {
        return usersDimensionsPersonalFrontiers.computeIfAbsent(user, k -> new HashMap<>());
    }

    public List<FrontierData> getAllPersonalFrontiers(SettingsUser user, RegistryKey<World> dimension) {
        HashMap<RegistryKey<World>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers
                .computeIfAbsent(user, k -> new HashMap<>());

        return dimensionsPersonalFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public FrontierData getFrontierFromID(UUID id) {
        return allFrontiers.get(id);
    }

    public FrontierData createNewGlobalFrontier(RegistryKey<World> dimension, ServerPlayerEntity player,
            @Nullable BlockPos vertex) {
        List<FrontierData> frontiers = getAllGlobalFrontiers(dimension);

        return createNewFrontier(frontiers, dimension, false, player, vertex);
    }

    public FrontierData createNewPersonalFrontier(RegistryKey<World> dimension, ServerPlayerEntity player,
            @Nullable BlockPos vertex) {
        List<FrontierData> frontiers = getAllPersonalFrontiers(new SettingsUser(player), dimension);

        return createNewFrontier(frontiers, dimension, true, player, vertex);
    }

    private FrontierData createNewFrontier(List<FrontierData> frontiers, RegistryKey<World> dimension, boolean personal,
            ServerPlayerEntity player, @Nullable BlockPos vertex) {
        final float hue = rand.nextFloat();
        final float saturation = (rand.nextInt(4000) + 6000) / 10000f;
        final float luminance = (rand.nextInt(3000) + 7000) / 10000f;
        Color color = Color.getHSBColor(hue, saturation, luminance);

        FrontierData frontier = new FrontierData();
        frontier.setId(UUID.randomUUID());
        frontier.setOwner(new SettingsUser(player));
        frontier.setDimension(dimension);
        frontier.setPersonal(personal);
        frontier.setColor(color.getRGB());

        if (vertex != null) {
            frontier.addVertex(vertex);
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

    public boolean deleteGlobalFrontier(RegistryKey<World> dimension, UUID id) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(dimension);
        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.id.equals(id));
        deleted |= allFrontiers.remove(id) != null;
        saveData();

        return deleted;
    }

    public boolean deletePersonalFrontier(SettingsUser user, RegistryKey<World> dimension, UUID id) {
        Map<RegistryKey<World>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
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

        FrontierData frontier = frontiers.get(index);
        frontier.updateFromData(updatedFrontier);

        saveData();

        return true;
    }

    public boolean updatePersonalFrontier(SettingsUser user, FrontierData updatedFrontier) {
        Map<RegistryKey<World>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
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

    public boolean canSendCommandAcceptFrontier(ServerPlayerEntity player) {
        return frontierSettings.checkAction(FrontierSettings.Action.PersonalFrontier, new SettingsUser(player),
                MapFrontiers.isOPorHost(player), null);
    }

    public void ensureOwners() {
        if (frontierOwnersChecked) {
            return;
        }

        for (FrontierData frontier : allFrontiers.values()) {
            frontier.ensureOwner();
        }

        frontierOwnersChecked = true;
    }

    private void readFromNBT(CompoundNBT nbt) {
        int version = nbt.getInt("Version");
        if (version == 0) {
            MapFrontiers.LOGGER.warn("Data version in frontiers not found, expected " + dataVersion);
        } else if (version < 5) {
            MapFrontiers.LOGGER
                    .warn("Data version in frontiers lower than expected. The mod uses " + dataVersion);
        } else if (version > dataVersion) {
            MapFrontiers.LOGGER
                    .warn("Data version in frontiers higher than expected. The mod uses " + dataVersion);
        }

        ListNBT allFrontiersTagList = nbt.getList("frontiers", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < allFrontiersTagList.size(); ++i) {
            FrontierData frontier = new FrontierData();
            CompoundNBT frontierTag = allFrontiersTagList.getCompound(i);
            frontier.readFromNBT(frontierTag, version);
            allFrontiers.put(frontier.getId(), frontier);
        }

        ListNBT dimensionsTagList = nbt.getList("global", Constants.NBT.TAG_COMPOUND);
        readFrontiersFromTagList(dimensionsTagList, dimensionsGlobalFrontiers, false, version);

        ListNBT personalTagList = nbt.getList("personal", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < personalTagList.size(); ++i) {
            CompoundNBT personalTag = personalTagList.getCompound(i);

            SettingsUser owner = new SettingsUser();
            CompoundNBT ownerTag = personalTag.getCompound("owner");
            if (ownerTag.isEmpty()) {
                continue;
            }
            owner.readFromNBT(ownerTag);

            HashMap<RegistryKey<World>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = new HashMap<>();
            dimensionsTagList = personalTag.getList("frontiers", Constants.NBT.TAG_COMPOUND);
            readFrontiersFromTagList(dimensionsTagList, dimensionsPersonalFrontiers, true, version);

            usersDimensionsPersonalFrontiers.put(owner, dimensionsPersonalFrontiers);
        }
    }

    private void readFrontiersFromTagList(ListNBT dimensionsTagList,
            Map<RegistryKey<World>, ArrayList<FrontierData>> dimensionsFrontiers, boolean personal, int version) {
        for (int i = 0; i < dimensionsTagList.size(); ++i) {
            CompoundNBT dimensionTag = dimensionsTagList.getCompound(i);
            RegistryKey<World> dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY,
                    new ResourceLocation(dimensionTag.getString("dimension")));
            ListNBT frontiersTagList = dimensionTag.getList("frontiers", Constants.NBT.TAG_COMPOUND);
            ArrayList<FrontierData> frontiers = new ArrayList<>();
            for (int i2 = 0; i2 < frontiersTagList.size(); ++i2) {
                CompoundNBT frontierTag = frontiersTagList.getCompound(i2);
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

    private void writeToNBT(CompoundNBT nbt) {
        ListNBT allFrontiersTagList = new ListNBT();
        for (FrontierData frontier : allFrontiers.values()) {
            CompoundNBT frontierTag = new CompoundNBT();
            frontier.writeToNBT(frontierTag);
            allFrontiersTagList.add(frontierTag);
        }
        nbt.put("frontiers", allFrontiersTagList);

        ListNBT dimensionsTagList = new ListNBT();
        writeFrontiersToTagList(dimensionsTagList, dimensionsGlobalFrontiers);
        nbt.put("global", dimensionsTagList);

        ListNBT personalTagList = new ListNBT();
        for (Map.Entry<SettingsUser, HashMap<RegistryKey<World>, ArrayList<FrontierData>>> personal : usersDimensionsPersonalFrontiers
                .entrySet()) {
            if (personal.getValue().isEmpty()) {
                continue;
            }

            CompoundNBT userFrontiers = new CompoundNBT();

            dimensionsTagList = new ListNBT();
            writeFrontiersToTagList(dimensionsTagList, personal.getValue());
            userFrontiers.put("frontiers", dimensionsTagList);

            if (dimensionsTagList.isEmpty()) {
                continue;
            }

            CompoundNBT nbtOwner = new CompoundNBT();
            personal.getKey().writeToNBT(nbtOwner);
            userFrontiers.put("owner", nbtOwner);

            personalTagList.add(userFrontiers);
        }
        nbt.put("personal", personalTagList);

        nbt.putInt("Version", dataVersion);
    }

    private void writeFrontiersToTagList(ListNBT dimensionsTagList,
            Map<RegistryKey<World>, ArrayList<FrontierData>> dimensionsFrontiers) {
        for (Map.Entry<RegistryKey<World>, ArrayList<FrontierData>> frontiers : dimensionsFrontiers.entrySet()) {
            ListNBT frontiersTagList = new ListNBT();
            for (FrontierData frontier : frontiers.getValue()) {
                CompoundNBT frontierTag = new CompoundNBT();
                frontierTag.putString("id", frontier.getId().toString());
                frontiersTagList.add(frontierTag);
            }

            if (frontiersTagList.isEmpty()) {
                continue;
            }

            CompoundNBT dimensionTag = new CompoundNBT();
            dimensionTag.putString("dimension", frontiers.getKey().location().toString());
            dimensionTag.put("frontiers", frontiersTagList);
            dimensionsTagList.add(dimensionTag);
        }
    }

    public void loadOrCreateData() {
        try {
            if (ServerLifecycleHooks.getCurrentServer().isDedicatedServer()) {
                File mcDir = ServerLifecycleHooks.getCurrentServer().getServerDirectory();
                ModDir = new File(mcDir, "mapfrontiers");
                ModDir.mkdirs();
            } else {
                File WorldDir = Minecraft.getInstance().getSingleplayerServer().getWorldPath(FolderName.ROOT).toFile();
                ModDir = new File(WorldDir, "mapfrontiers");
                ModDir.mkdirs();
            }

            CompoundNBT nbtFrontiers = loadFile("frontiers.dat");
            if (nbtFrontiers.isEmpty()) {
                writeToNBT(nbtFrontiers);
                saveFile("frontiers.dat", nbtFrontiers);
            } else {
                readFromNBT(nbtFrontiers);
            }

            CompoundNBT nbtSettings = loadFile("settings.dat");
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
        CompoundNBT nbtFrontiers = new CompoundNBT();
        writeToNBT(nbtFrontiers);
        saveFile("frontiers.dat", nbtFrontiers);

        CompoundNBT nbtSettings = new CompoundNBT();
        frontierSettings.writeToNBT(nbtSettings);
        saveFile("settings.dat", nbtSettings);
    }

    private CompoundNBT loadFile(String filename) {
        File f = new File(ModDir, filename);
        if (f.exists()) {
            try (FileInputStream inputStream = new FileInputStream(f)) {
                return CompressedStreamTools.readCompressed(inputStream);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        }

        return new CompoundNBT();
    }

    private void saveFile(String fileName, CompoundNBT nbt) {
        try {
            File f = new File(ModDir, fileName);
            try (FileOutputStream outputStream = new FileOutputStream(f)) {
                CompressedStreamTools.writeCompressed(nbt, outputStream);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }
}
