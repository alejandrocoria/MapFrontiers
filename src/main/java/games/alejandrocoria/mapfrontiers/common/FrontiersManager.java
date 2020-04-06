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
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
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
    private FrontierSettings frontierSettings;
    private Random rand = new Random();
    private File WorldDir;
    private boolean frontierOwnersChecked = false;

    private static final int dataVersion = 1;

    public FrontiersManager() {
        instance = this;
        dimensionsFrontiers = new HashMap<Integer, ArrayList<FrontierData>>();
        frontierSettings = new FrontierSettings();
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

    public FrontierData getFrontierFromIndex(int dimension, int index) {
        List<FrontierData> frontiers = getAllFrontiers(dimension);
        return frontiers.get(index);
    }

    public FrontierData getFrontierFromID(int dimension, int id) {
        List<FrontierData> frontiers = getAllFrontiers(dimension);

        // @Note: copied from FrontiersOverlayManager.deleteFrontier(int,int)
        int index = IntStream.range(0, frontiers.size()).filter(i -> frontiers.get(i).getId() == id).findFirst().orElse(-1);

        if (index < 0) {
            return null;
        }

        return frontiers.get(index);
    }

    public FrontierData createNewfrontier(int dimension, EntityPlayer player, boolean addVertex, int snapDistance) {
        ArrayList<FrontierData> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));
        if (frontiers == null) {
            frontiers = new ArrayList<FrontierData>();
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
        }

        final float hue = rand.nextFloat();
        final float saturation = (rand.nextInt(4000) + 6000) / 10000f;
        final float luminance = (rand.nextInt(3000) + 7000) / 10000f;
        Color color = Color.getHSBColor(hue, saturation, luminance);

        FrontierData frontier = new FrontierData();
        frontier.setOwner(player);
        frontier.setDimension(dimension);
        frontier.setColor(color.getRGB());

        if (addVertex) {
            frontier.addVertex(player.getPosition(), snapDistance);
        }

        frontiers.add(frontier);
        saveData();

        return frontier;
    }

    public void deleteFrontier(int dimension, int id) {
        List<FrontierData> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));
        if (frontiers == null) {
            return;
        }

        frontiers.removeIf(x -> x.id == id);
        saveData();
    }

    public boolean updateFrontier(FrontierData frontier) {
        List<FrontierData> frontiers = dimensionsFrontiers.get(Integer.valueOf(frontier.getDimension()));
        if (frontiers == null) {
            return false;
        }

        // @Note: copied from FrontiersOverlayManager.deleteFrontier(int,int)
        int index = IntStream.range(0, frontiers.size()).filter(i -> frontiers.get(i).getId() == frontier.getId()).findFirst()
                .orElse(-1);

        if (index < 0) {
            return false;
        }

        frontiers.set(index, frontier);
        saveData();

        return true;
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

        NBTTagList dimensionsTagList = nbt.getTagList("MapFrontiers", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < dimensionsTagList.tagCount(); ++i) {
            NBTTagCompound dimensionTag = dimensionsTagList.getCompoundTagAt(i);
            int dimension = dimensionTag.getInteger("dimension");
            NBTTagList frontiersTagList = dimensionTag.getTagList("frontiers", Constants.NBT.TAG_COMPOUND);
            ArrayList<FrontierData> frontiers = new ArrayList<FrontierData>();
            for (int i2 = 0; i2 < frontiersTagList.tagCount(); ++i2) {
                FrontierData frontier = new FrontierData();
                frontier.setDimension(dimension);
                NBTTagCompound frontierTag = frontiersTagList.getCompoundTagAt(i2);
                frontier.readFromNBT(frontierTag);
                frontiers.add(frontier);
            }
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
        }
    }

    private void writeToNBT(NBTTagCompound nbt) {
        NBTTagList dimensionsTagList = new NBTTagList();
        for (Map.Entry<Integer, ArrayList<FrontierData>> frontiers : dimensionsFrontiers.entrySet()) {
            NBTTagList frontiersTagList = new NBTTagList();
            for (FrontierData frontier : frontiers.getValue()) {
                NBTTagCompound frontierTag = new NBTTagCompound();
                frontier.writeToNBT(frontierTag);
                frontiersTagList.appendTag(frontierTag);
            }
            NBTTagCompound dimensionTag = new NBTTagCompound();
            dimensionTag.setInteger("dimension", frontiers.getKey());
            dimensionTag.setTag("frontiers", frontiersTagList);
            dimensionsTagList.appendTag(dimensionTag);
        }
        nbt.setTag("MapFrontiers", dimensionsTagList);

        nbt.setInteger("Version", dataVersion);
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
