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
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;

@ParametersAreNonnullByDefault
public class FrontiersManager {
    public static FrontiersManager instance;

    private HashMap<Integer, ArrayList<FrontierData>> dimensionsFrontiers;
    private final Random rand = new Random();
    private File ModDir;
    private File DataDir;
    private File TypeDir;
    private File WorldDir;

    private static final int dataVersion = 1;

    public FrontiersManager() {
        instance = this;
        dimensionsFrontiers = new HashMap<Integer, ArrayList<FrontierData>>();

        ArrayList<FrontierData> frontiers = dimensionsFrontiers.get(Integer.valueOf(0));
        if (frontiers == null) {
            frontiers = new ArrayList<FrontierData>();
            dimensionsFrontiers.put(Integer.valueOf(0), frontiers);
        }

        FrontierData frontier = new FrontierData();
        frontier.setDimension(0);
        frontier.setColor(0xffff00);
        frontier.addVertex(new BlockPos(0, 70, 0));
        frontier.addVertex(new BlockPos(20, 70, 0));
        frontier.addVertex(new BlockPos(20, 70, 20));
        frontier.addVertex(new BlockPos(0, 70, 20));
        frontier.setClosed(true);

        frontiers.add(frontier);
        saveData();
    }

    public Map<Integer, ArrayList<FrontierData>> getAllFrontiers() {
        return dimensionsFrontiers;
    }

    public void createNewfrontier(int dimension, EntityPlayer player) {
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
        frontier.setDimension(dimension);
        frontier.setColor(color.getRGB());

        if (ConfigData.addVertexToNewFrontier) {
            frontier.addVertex(player.getPosition());
        }

        frontiers.add(frontier);
        saveData();
    }

    public void deleteFrontier(int dimension, int index) {
        List<FrontierData> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));
        if (frontiers == null) {
            return;
        }

        frontiers.remove(index);
        saveData();
    }

    public BlockPos snapVertex(BlockPos vertex, int snapDistance, FrontierData owner) {
        float snapDistanceSq = snapDistance * snapDistance;
        BlockPos closest = new BlockPos(vertex.getX(), 70, vertex.getZ());
        double closestDistance = Double.MAX_VALUE;
        for (FrontierData frontier : dimensionsFrontiers.get(Integer.valueOf(owner.getDimension()))) {
            if (frontier == owner) {
                continue;
            }

            for (int i = 0; i < frontier.getVertexCount(); ++i) {
                BlockPos v = frontier.getVertex(i);
                BlockPos v2 = new BlockPos(v.getX(), 70, v.getZ());
                double distance = v2.distanceSq(closest);
                if (distance < snapDistanceSq && distance < closestDistance) {
                    closestDistance = distance;
                    closest = v2;
                }
            }
        }

        return closest;
    }

    private void readFromNBT(NBTTagCompound nbt) {
        int version = nbt.getInteger("Version");
        if (version == 0) {
            MapFrontiers.LOGGER.warn("Data version not found, expected " + String.valueOf(dataVersion));
        } else if (version > dataVersion) {
            MapFrontiers.LOGGER.warn("Data version higher than expected. The mod uses " + String.valueOf(dataVersion));
        }

        NBTTagList dimensionsTagList = nbt.getTagList("MapFrontiers", 10);
        for (int i = 0; i < dimensionsTagList.tagCount(); ++i) {
            NBTTagCompound dimensionTag = dimensionsTagList.getCompoundTagAt(i);
            int dimension = dimensionTag.getInteger("dimension");
            NBTTagList frontiersTagList = dimensionTag.getTagList("frontiers", 10);
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
        if (true)
            return;

        try {
            if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
                ModDir = new File(Minecraft.getMinecraft().mcDataDir, "mapfrontiers");
                DataDir = new File(ModDir, "data");
                TypeDir = new File(DataDir, "sp");
                WorldDir = new File(TypeDir, Minecraft.getMinecraft().getIntegratedServer().getFolderName());
                if (WorldDir.isFile()) {
                    /// @Incomplete copy file to correct folder
                    File correctFolder = new File(FMLCommonHandler.instance().getSavesDirectory(),
                            Minecraft.getMinecraft().getIntegratedServer().getFolderName());
                }
            }
            WorldDir.mkdirs();

            File f = new File(WorldDir, "frontiers.dat");
            if (!f.exists()) {
                saveData();
            }
            NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(f));
            readFromNBT(nbt);
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    public void saveData() {
        try {
            File f = new File(WorldDir, "frontiers.dat");
            NBTTagCompound nbt = new NBTTagCompound();
            writeToNBT(nbt);
            CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(f));
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }
}
