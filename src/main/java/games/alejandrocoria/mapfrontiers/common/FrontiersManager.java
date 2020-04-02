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
    private Random rand = new Random();
    private File WorldDir;
    private boolean frontierOwnersChecked = false;

    private static final int dataVersion = 1;

    public FrontiersManager() {
        instance = this;
        dimensionsFrontiers = new HashMap<Integer, ArrayList<FrontierData>>();
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

        FrontierData currentFrontier = frontiers.get(index);
        if (currentFrontier.getOwnerUUID() != null) {
            frontier.setOwner(currentFrontier.getOwnerUUID());
        }
        if ((frontier.getOwnerName() == null || frontier.getOwnerName().isEmpty()) && currentFrontier.getOwnerName() != null
                && !currentFrontier.getOwnerName().isEmpty()) {
            frontier.setOwner(currentFrontier.getOwnerName());
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
            MapFrontiers.LOGGER.warn("Data version not found, expected " + String.valueOf(dataVersion));
        } else if (version > dataVersion) {
            MapFrontiers.LOGGER.warn("Data version higher than expected. The mod uses " + String.valueOf(dataVersion));
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

            File f = new File(WorldDir, "frontiers.dat");
            if (!f.exists()) {
                saveData();
            }

            try (FileInputStream inputStream = new FileInputStream(f)) {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(inputStream);
                readFromNBT(nbt);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    public void saveData() {
        try {
            File f = new File(WorldDir, "frontiers.dat");
            NBTTagCompound nbt = new NBTTagCompound();
            writeToNBT(nbt);
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
