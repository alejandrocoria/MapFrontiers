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
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ContainerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;

@ParametersAreNonnullByDefault
public class FrontiersManager {
    public static FrontiersManager instance;

    private HashMap<UUID, FrontierData> allFrontiers;
    private HashMap<Integer, ArrayList<FrontierData>> dimensionsGlobalFrontiers;
    private HashMap<SettingsUser, HashMap<Integer, ArrayList<FrontierData>>> usersDimensionsPersonalFrontiers;
    private FrontierSettings frontierSettings;
    private Random rand = new Random();
    private File WorldDir;
    private boolean frontierOwnersChecked = false;

    public static final int dataVersion = 4;

    public FrontiersManager() {
        instance = this;
        allFrontiers = new HashMap<UUID, FrontierData>();
        dimensionsGlobalFrontiers = new HashMap<Integer, ArrayList<FrontierData>>();
        usersDimensionsPersonalFrontiers = new HashMap<SettingsUser, HashMap<Integer, ArrayList<FrontierData>>>();
        frontierSettings = new FrontierSettings();
    }

    public void setSettings(FrontierSettings frontierSettings) {
        this.frontierSettings = frontierSettings;
        saveData();
    }

    public FrontierSettings getSettings() {
        return frontierSettings;
    }

    public Map<Integer, ArrayList<FrontierData>> getAllGlobalFrontiers() {
        return dimensionsGlobalFrontiers;
    }

    public List<FrontierData> getAllGlobalFrontiers(int dimension) {
        ArrayList<FrontierData> frontiers = dimensionsGlobalFrontiers.get(Integer.valueOf(dimension));

        if (frontiers == null) {
            frontiers = new ArrayList<FrontierData>();
            dimensionsGlobalFrontiers.put(Integer.valueOf(dimension), frontiers);
        }

        return frontiers;
    }

    public Map<Integer, ArrayList<FrontierData>> getAllPersonalFrontiers(SettingsUser user) {
        HashMap<Integer, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);

        if (dimensionsPersonalFrontiers == null) {
            dimensionsPersonalFrontiers = new HashMap<Integer, ArrayList<FrontierData>>();
            usersDimensionsPersonalFrontiers.put(user, dimensionsPersonalFrontiers);
        }

        return dimensionsPersonalFrontiers;
    }

    public List<FrontierData> getAllPersonalFrontiers(SettingsUser user, int dimension) {
        HashMap<Integer, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);

        if (dimensionsPersonalFrontiers == null) {
            dimensionsPersonalFrontiers = new HashMap<Integer, ArrayList<FrontierData>>();
            usersDimensionsPersonalFrontiers.put(user, dimensionsPersonalFrontiers);
        }

        ArrayList<FrontierData> frontiers = dimensionsPersonalFrontiers.get(Integer.valueOf(dimension));

        if (frontiers == null) {
            frontiers = new ArrayList<FrontierData>();
            dimensionsPersonalFrontiers.put(Integer.valueOf(dimension), frontiers);
        }

        return frontiers;
    }

    public FrontierData getFrontierFromID(UUID id) {
        return allFrontiers.get(id);
    }

    public FrontierData createNewGlobalFrontier(int dimension, EntityPlayer player, @Nullable BlockPos vertex) {
        List<FrontierData> frontiers = getAllGlobalFrontiers(dimension);

        return createNewFrontier(frontiers, dimension, false, player, vertex);
    }

    public FrontierData createNewPersonalFrontier(int dimension, EntityPlayer player, @Nullable BlockPos vertex) {
        List<FrontierData> frontiers = getAllPersonalFrontiers(new SettingsUser(player), dimension);

        return createNewFrontier(frontiers, dimension, true, player, vertex);
    }

    private FrontierData createNewFrontier(List<FrontierData> frontiers, int dimension, boolean personal, EntityPlayer player,
            @Nullable BlockPos vertex) {
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

    public boolean deleteGlobalFrontier(int dimension, UUID id) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(Integer.valueOf(dimension));
        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.id.equals(id));
        deleted |= allFrontiers.remove(id) != null;
        saveData();

        return deleted;
    }

    public boolean deletePersonalFrontier(SettingsUser user, int dimension, UUID id) {
        Map<Integer, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(Integer.valueOf(dimension));
        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.id.equals(id));
        deleted |= allFrontiers.remove(id) != null;
        saveData();

        return deleted;
    }

    public boolean updateGlobalFrontier(FrontierData frontier) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(Integer.valueOf(frontier.getDimension()));
        if (frontiers == null) {
            return false;
        }

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(frontier.getId()));

        if (index < 0) {
            return false;
        }

        frontiers.set(index, frontier);
        allFrontiers.put(frontier.getId(), frontier);
        saveData();

        return true;
    }

    public boolean updatePersonalFrontier(FrontierData frontier) {
        Map<Integer, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers
                .get(frontier.getOwner());
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(Integer.valueOf(frontier.getDimension()));
        if (frontiers == null) {
            return false;
        }

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId() == frontier.getId());

        if (index < 0) {
            return false;
        }

        frontiers.set(index, frontier);
        allFrontiers.put(frontier.getId(), frontier);
        saveData();

        return true;
    }

    public int addShareMessage(SettingsUser playerSharing, SettingsUser owner, SettingsUser targetUser, int dimension, UUID id) {
        // @Incomplete
        return 0;
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

    private void readFromNBT(NBTTagCompound nbt) {
        int version = nbt.getInteger("Version");
        if (version == 0) {
            MapFrontiers.LOGGER.warn("Data version in frontiers not found, expected " + String.valueOf(dataVersion));
        } else if (version > dataVersion) {
            MapFrontiers.LOGGER
                    .warn("Data version in frontiers higher than expected. The mod uses " + String.valueOf(dataVersion));
        }

        if (version >= 4) {
            NBTTagList allFrontiersTagList = nbt.getTagList("frontiers", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < allFrontiersTagList.tagCount(); ++i) {
                FrontierData frontier = new FrontierData();
                NBTTagCompound frontierTag = allFrontiersTagList.getCompoundTagAt(i);
                frontier.readFromNBT(frontierTag, version);
                allFrontiers.put(frontier.getId(), frontier);
            }
        }

        NBTTagList dimensionsTagList = nbt.getTagList(version < 3 ? "MapFrontiers" : "global", Constants.NBT.TAG_COMPOUND);
        readFrontiersFromTagList(dimensionsTagList, dimensionsGlobalFrontiers, false, version);

        NBTTagList personalTagList = nbt.getTagList("personal", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < personalTagList.tagCount(); ++i) {
            NBTTagCompound personalTag = personalTagList.getCompoundTagAt(i);

            SettingsUser owner = new SettingsUser();
            NBTTagCompound ownerTag = personalTag.getCompoundTag("owner");
            if (ownerTag.hasNoTags()) {
                continue;
            }
            owner.readFromNBT(ownerTag);

            HashMap<Integer, ArrayList<FrontierData>> dimensionsPersonalFrontiers = new HashMap<Integer, ArrayList<FrontierData>>();
            dimensionsTagList = personalTag.getTagList("frontiers", Constants.NBT.TAG_COMPOUND);
            readFrontiersFromTagList(dimensionsTagList, dimensionsPersonalFrontiers, true, version);

            usersDimensionsPersonalFrontiers.put(owner, dimensionsPersonalFrontiers);
        }
    }

    private void readFrontiersFromTagList(NBTTagList dimensionsTagList, Map<Integer, ArrayList<FrontierData>> dimensionsFrontiers,
            boolean personal, int version) {
        for (int i = 0; i < dimensionsTagList.tagCount(); ++i) {
            NBTTagCompound dimensionTag = dimensionsTagList.getCompoundTagAt(i);
            int dimension = dimensionTag.getInteger("dimension");
            NBTTagList frontiersTagList = dimensionTag.getTagList("frontiers", Constants.NBT.TAG_COMPOUND);
            ArrayList<FrontierData> frontiers = new ArrayList<FrontierData>();
            for (int i2 = 0; i2 < frontiersTagList.tagCount(); ++i2) {
                if (version < 4) {
                    FrontierData frontier = new FrontierData();
                    frontier.setDimension(dimension);
                    frontier.setPersonal(personal);
                    NBTTagCompound frontierTag = frontiersTagList.getCompoundTagAt(i2);
                    frontier.readFromNBT(frontierTag, version);
                    frontier.setId(UUID.randomUUID());
                    frontiers.add(frontier);
                    allFrontiers.put(frontier.getId(), frontier);
                } else {
                    NBTTagCompound frontierTag = frontiersTagList.getCompoundTagAt(i2);
                    FrontierData frontier = allFrontiers.get(UUID.fromString(frontierTag.getString("id")));
                    if (frontier != null) {
                        frontiers.add(frontier);
                    } else {
                        String message = "Nonexistent frontier with UUID %1$s referenced in frontiers.dat";
                        message = String.format(message, frontierTag.getString("id"));
                        MapFrontiers.LOGGER.warn(message);
                    }
                }
            }
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
        }
    }

    private void writeToNBT(NBTTagCompound nbt) {
        NBTTagList allFrontiersTagList = new NBTTagList();
        for (FrontierData frontier : allFrontiers.values()) {
            NBTTagCompound frontierTag = new NBTTagCompound();
            frontier.writeToNBT(frontierTag);
            allFrontiersTagList.appendTag(frontierTag);
        }
        nbt.setTag("frontiers", allFrontiersTagList);

        NBTTagList dimensionsTagList = new NBTTagList();
        writeFrontiersToTagList(dimensionsTagList, dimensionsGlobalFrontiers);
        nbt.setTag("global", dimensionsTagList);

        NBTTagList personalTagList = new NBTTagList();
        for (Map.Entry<SettingsUser, HashMap<Integer, ArrayList<FrontierData>>> personal : usersDimensionsPersonalFrontiers
                .entrySet()) {
            if (personal.getValue().isEmpty()) {
                continue;
            }

            NBTTagCompound userFrontiers = new NBTTagCompound();

            dimensionsTagList = new NBTTagList();
            writeFrontiersToTagList(dimensionsTagList, personal.getValue());
            userFrontiers.setTag("frontiers", dimensionsTagList);

            if (dimensionsTagList.hasNoTags()) {
                continue;
            }

            NBTTagCompound nbtOwner = new NBTTagCompound();
            personal.getKey().writeToNBT(nbtOwner);
            userFrontiers.setTag("owner", nbtOwner);

            personalTagList.appendTag(userFrontiers);
        }
        nbt.setTag("personal", personalTagList);

        nbt.setInteger("Version", dataVersion);
    }

    private void writeFrontiersToTagList(NBTTagList dimensionsTagList,
            Map<Integer, ArrayList<FrontierData>> dimensionsFrontiers) {
        for (Map.Entry<Integer, ArrayList<FrontierData>> frontiers : dimensionsFrontiers.entrySet()) {
            NBTTagList frontiersTagList = new NBTTagList();
            for (FrontierData frontier : frontiers.getValue()) {
                NBTTagCompound frontierTag = new NBTTagCompound();
                frontierTag.setString("id", frontier.getId().toString());
                frontiersTagList.appendTag(frontierTag);
            }

            if (frontiersTagList.hasNoTags()) {
                continue;
            }

            NBTTagCompound dimensionTag = new NBTTagCompound();
            dimensionTag.setInteger("dimension", frontiers.getKey());
            dimensionTag.setTag("frontiers", frontiersTagList);
            dimensionsTagList.appendTag(dimensionTag);
        }
    }

    public void loadOrCreateData() {
        try {
            File mcDir;
            String typeFolder;
            String worldFolder;
            if (FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer()) {
                mcDir = FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory();
                typeFolder = "mp";
                worldFolder = "";
            } else {
                mcDir = Minecraft.getMinecraft().mcDataDir;
                typeFolder = "sp";
                worldFolder = Minecraft.getMinecraft().getIntegratedServer().getFolderName();
            }

            File ModDir = new File(mcDir, "mapfrontiers");
            File DataDir = new File(ModDir, "data");
            File TypeDir = new File(DataDir, typeFolder);
            WorldDir = new File(TypeDir, worldFolder);
            WorldDir.mkdirs();

            NBTTagCompound nbtFrontiers = loadFile("frontiers.dat");
            if (nbtFrontiers.hasNoTags()) {
                writeToNBT(nbtFrontiers);
                saveFile("frontiers.dat", nbtFrontiers);
            } else {
                readFromNBT(nbtFrontiers);
            }

            NBTTagCompound nbtSettings = loadFile("settings.dat");
            if (nbtSettings.hasNoTags()) {
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
        NBTTagCompound nbtFrontiers = new NBTTagCompound();
        writeToNBT(nbtFrontiers);
        saveFile("frontiers.dat", nbtFrontiers);

        NBTTagCompound nbtSettings = new NBTTagCompound();
        frontierSettings.writeToNBT(nbtSettings);
        saveFile("settings.dat", nbtSettings);
    }

    private NBTTagCompound loadFile(String filename) {
        File f = new File(WorldDir, filename);
        if (f.exists()) {
            try (FileInputStream inputStream = new FileInputStream(f)) {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(inputStream);
                return nbt;
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        }

        return new NBTTagCompound();
    }

    private void saveFile(String fileName, NBTTagCompound nbt) {
        try {
            File f = new File(WorldDir, fileName);
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
