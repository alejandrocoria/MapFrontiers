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
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;

@ParametersAreNonnullByDefault
public class FrontiersManager {
    public static FrontiersManager instance;

    private HashMap<Integer, ArrayList<FrontierData>> dimensionsFrontiers;
    private HashMap<SettingsUser, HashMap<Integer, ArrayList<FrontierData>>> usersDimensionsPersonalFrontiers;
    private FrontierSettings frontierSettings;
    private Random rand = new Random();
    private File WorldDir;
    private boolean frontierOwnersChecked = false;

    public static final int dataVersion = 3;

    public FrontiersManager() {
        instance = this;
        dimensionsFrontiers = new HashMap<Integer, ArrayList<FrontierData>>();
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

    public Map<Integer, ArrayList<FrontierData>> getAllFrontiers() {
        return dimensionsFrontiers;
    }

    public List<FrontierData> getAllFrontiers(int dimension) {
        ArrayList<FrontierData> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));

        if (frontiers == null) {
            frontiers = new ArrayList<FrontierData>();
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
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

    // @Note: unnused
    public FrontierData getFrontierFromIndex(int dimension, int index) {
        List<FrontierData> frontiers = getAllFrontiers(dimension);
        return frontiers.get(index);
    }

    public FrontierData getFrontierFromID(int dimension, int id) {
        List<FrontierData> frontiers = getAllFrontiers(dimension);

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId() == id);

        if (index < 0) {
            return null;
        }

        return frontiers.get(index);
    }

    public FrontierData getPersonalFrontierFromID(SettingsUser user, int dimension, int id) {
        List<FrontierData> frontiers = getAllPersonalFrontiers(user, dimension);

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId() == id);

        if (index < 0) {
            return null;
        }

        return frontiers.get(index);
    }

    public FrontierData createNewFrontier(int dimension, EntityPlayer player, boolean addVertex, int snapDistance) {
        List<FrontierData> frontiers = getAllFrontiers(dimension);

        return createNewFrontier(frontiers, dimension, false, player, addVertex, snapDistance);
    }

    public FrontierData createNewPersonalFrontier(int dimension, EntityPlayer player, boolean addVertex, int snapDistance) {
        List<FrontierData> frontiers = getAllPersonalFrontiers(new SettingsUser(player), dimension);

        return createNewFrontier(frontiers, dimension, true, player, addVertex, snapDistance);
    }

    private FrontierData createNewFrontier(List<FrontierData> frontiers, int dimension, boolean personal, EntityPlayer player,
            boolean addVertex, int snapDistance) {
        final float hue = rand.nextFloat();
        final float saturation = (rand.nextInt(4000) + 6000) / 10000f;
        final float luminance = (rand.nextInt(3000) + 7000) / 10000f;
        Color color = Color.getHSBColor(hue, saturation, luminance);

        FrontierData frontier = new FrontierData();
        frontier.setOwner(new SettingsUser(player));
        frontier.setDimension(dimension);
        frontier.setPersonal(personal);
        frontier.setColor(color.getRGB());

        if (addVertex) {
            frontier.addVertex(player.getPosition(), snapDistance);
        }

        frontiers.add(frontier);
        saveData();

        return frontier;
    }

    public boolean deleteFrontier(int dimension, int id) {
        List<FrontierData> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));
        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.id == id);
        saveData();

        return deleted;
    }

    public boolean deletePersonalFrontier(SettingsUser user, int dimension, int id) {
        Map<Integer, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(Integer.valueOf(dimension));
        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.id == id);
        saveData();

        return deleted;
    }

    public boolean updateFrontier(FrontierData frontier) {
        List<FrontierData> frontiers = dimensionsFrontiers.get(Integer.valueOf(frontier.getDimension()));
        if (frontiers == null) {
            return false;
        }

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId() == frontier.getId());

        if (index < 0) {
            return false;
        }

        frontiers.set(index, frontier);
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
        saveData();

        return true;
    }

    public int addShareMessage(SettingsUser playerSharing, SettingsUser owner, SettingsUser targetUser, int dimension, int id) {
        // @Incomplete
        return 0;
    }

    public void ensureOwners() {
        if (frontierOwnersChecked) {
            return;
        }

        for (ArrayList<FrontierData> frontiers : dimensionsFrontiers.values()) {
            for (FrontierData frontier : frontiers) {
                frontier.ensureOwner();
            }
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

        NBTTagList dimensionsTagList = nbt.getTagList(version < 3 ? "MapFrontiers" : "global", Constants.NBT.TAG_COMPOUND);
        readFrontiersFromTagList(dimensionsTagList, dimensionsFrontiers, false, version);

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
                FrontierData frontier = new FrontierData();
                frontier.setDimension(dimension);
                frontier.setPersonal(personal);
                NBTTagCompound frontierTag = frontiersTagList.getCompoundTagAt(i2);
                frontier.readFromNBT(frontierTag, version);
                frontiers.add(frontier);
            }
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
        }
    }

    private void writeToNBT(NBTTagCompound nbt) {
        NBTTagList dimensionsTagList = new NBTTagList();
        writeFrontiersToTagList(dimensionsTagList, dimensionsFrontiers);
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
                frontier.writeToNBT(frontierTag);
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
