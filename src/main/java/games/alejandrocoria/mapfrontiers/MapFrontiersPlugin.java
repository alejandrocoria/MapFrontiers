package games.alejandrocoria.mapfrontiers;

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

import games.alejandrocoria.mapfrontiers.gui.GuiFrontierBook;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@journeymap.client.api.ClientPlugin
public class MapFrontiersPlugin implements IClientPlugin {
    public static MapFrontiersPlugin instance;
    private IClientAPI jmAPI = null;

    private File ModDir;
    private File DataDir;
    private File TypeDir;
    private File WorldDir;

    private final ResourceLocation openBookSoundRes = new ResourceLocation(MapFrontiers.MODID, "open_book");
    private final SoundEvent openBookSoundEvent = new SoundEvent(openBookSoundRes);

    private final ResourceLocation turnPageSoundRes = new ResourceLocation(MapFrontiers.MODID, "turn_page");
    private final SoundEvent turnPageSoundEvent = new SoundEvent(turnPageSoundRes);

    private final Random rand = new Random();

    private HashMap<Integer, ArrayList<Frontier>> dimensionsFrontiers;
    private HashMap<Integer, Integer> frontiersSelected;

    private static final int dataVersion = 1;

    @Override
    public void initialize(final IClientAPI jmAPI) {
        instance = this;
        this.jmAPI = jmAPI;

        MinecraftForge.EVENT_BUS.register(this);

        dimensionsFrontiers = new HashMap<Integer, ArrayList<Frontier>>();
        frontiersSelected = new HashMap<Integer, Integer>();
    }

    @Override
    public String getModId() {
        return MapFrontiers.MODID;
    }

    @Override
    public void onEvent(ClientEvent event) {
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(openBookSoundEvent);
        event.getRegistry().register(turnPageSoundEvent);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void loadPlayer(PlayerLoggedInEvent event) {
        loadOrCreateData();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void unloadPlayer(PlayerLoggedOutEvent event) {
        jmAPI.removeAll(MapFrontiers.MODID);
        dimensionsFrontiers.clear();
        frontiersSelected.clear();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(MapFrontiers.MODID)) {
            ConfigManager.sync(MapFrontiers.MODID, Config.Type.INSTANCE);
            updateAllOverlays();
        }
    }

    @SideOnly(Side.CLIENT)
    public void openGUIFrontierBook(int dimension) {
        ArrayList<Frontier> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));

        if (frontiers == null) {
            frontiers = new ArrayList<Frontier>();
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
        }

        int currentDimension = Minecraft.getMinecraft().player.dimension;
        Minecraft.getMinecraft().displayGuiScreen(
                new GuiFrontierBook(frontiers, currentDimension, dimension, getFrontierIndexSelected(dimension)));
    }

    @SideOnly(Side.CLIENT)
    public void createNewfrontier(int dimension) {
        ArrayList<Frontier> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));
        if (frontiers == null) {
            frontiers = new ArrayList<Frontier>();
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
        }

        final float hue = rand.nextFloat();
        final float saturation = (rand.nextInt(4000) + 6000) / 10000f;
        final float luminance = (rand.nextInt(3000) + 7000) / 10000f;
        Color color = Color.getHSBColor(hue, saturation, luminance);

        Frontier frontier = new Frontier(jmAPI);
        frontier.dimension = dimension;
        frontier.color = color.getRGB();

        if (ConfigData.addVertexToNewFrontier) {
            frontier.addVertex(Minecraft.getMinecraft().player.getPosition());
        }

        frontiers.add(frontier);
        saveData();
    }

    @SideOnly(Side.CLIENT)
    public void deleteFrontier(int dimension, int index) {
        List<Frontier> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));
        if (frontiers == null) {
            return;
        }

        frontiers.get(index).removeOverlay();
        frontiers.remove(index);
        saveData();
    }

    @SideOnly(Side.CLIENT)
    public void updateAllOverlays() {
        for (List<Frontier> frontiers : dimensionsFrontiers.values()) {
            for (Frontier frontier : frontiers) {
                frontier.updateOverlay();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public int getFrontierIndexSelected(int dimension) {
        Integer selected = frontiersSelected.get(Integer.valueOf(dimension));
        if (selected == null)
            return -1;
        return selected;
    }

    @SideOnly(Side.CLIENT)
    public void setFrontierIndexSelected(int dimension, int frontier) {
        Integer dim = Integer.valueOf(dimension);
        int prevSelected = frontiersSelected.getOrDefault(dim, -1);
        List<Frontier> frontiers = dimensionsFrontiers.get(dimension);

        if (prevSelected >= 0 && prevSelected < frontiers.size()) {
            Frontier f = frontiers.get(prevSelected);
            f.selected = false;
            f.updateOverlay();
        }

        frontiersSelected.put(dim, Integer.valueOf(frontier));

        if (frontier >= 0) {
            Frontier f = frontiers.get(frontier);
            f.selected = true;
            f.updateOverlay();
        }

        saveData();
    }

    @SideOnly(Side.CLIENT)
    public void playSoundOpenBook() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(openBookSoundEvent, 1.0F));
    }

    @SideOnly(Side.CLIENT)
    public void playSoundTurnPage() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(turnPageSoundEvent, 1.0F));
    }

    public BlockPos snapVertex(BlockPos vertex, int snapDistance, Frontier owner) {
        float snapDistanceSq = snapDistance * snapDistance;
        BlockPos closest = new BlockPos(vertex.getX(), 70, vertex.getZ());
        double closestDistance = Double.MAX_VALUE;
        for (Frontier frontier : dimensionsFrontiers.get(Integer.valueOf(owner.dimension))) {
            if (frontier == owner) {
                continue;
            }

            for (BlockPos v : frontier.vertices) {
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

    @SideOnly(Side.CLIENT)
    public void readFromNBT(NBTTagCompound nbt) {
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
            ArrayList<Frontier> frontiers = new ArrayList<Frontier>();
            for (int i2 = 0; i2 < frontiersTagList.tagCount(); ++i2) {
                Frontier frontier = new Frontier(jmAPI);
                frontier.dimension = dimension;
                NBTTagCompound frontierTag = frontiersTagList.getCompoundTagAt(i2);
                frontier.readFromNBT(frontierTag);
                frontiers.add(frontier);
            }
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
        }

        NBTTagList frontiersSelectedList = nbt.getTagList("FrontiersSelected", 10);
        for (int i = 0; i < frontiersSelectedList.tagCount(); ++i) {
            NBTTagCompound frontierTag = frontiersSelectedList.getCompoundTagAt(i);
            if (frontierTag.hasKey("dimension") && frontierTag.hasKey("frontier")) {
                int dimension = frontierTag.getInteger("dimension");
                int frontier = frontierTag.getInteger("frontier");
                setFrontierIndexSelected(dimension, frontier);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList dimensionsTagList = new NBTTagList();
        for (Map.Entry<Integer, ArrayList<Frontier>> frontiers : dimensionsFrontiers.entrySet()) {
            NBTTagList frontiersTagList = new NBTTagList();
            for (Frontier frontier : frontiers.getValue()) {
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

        NBTTagList frontiersSelectedList = new NBTTagList();
        for (Map.Entry<Integer, Integer> frontier : frontiersSelected.entrySet()) {
            NBTTagCompound frontierTag = new NBTTagCompound();
            frontierTag.setInteger("dimension", frontier.getKey());
            frontierTag.setInteger("frontier", frontier.getValue());
            frontiersSelectedList.appendTag(frontierTag);
        }
        nbt.setTag("FrontiersSelected", frontiersSelectedList);

        nbt.setInteger("Version", dataVersion);
    }

    @SideOnly(Side.CLIENT)
    void loadOrCreateData() {
        try {
            ModDir = new File(Minecraft.getMinecraft().mcDataDir, "mapfrontiers");
            DataDir = new File(ModDir, "data");
            TypeDir = new File(DataDir, "sp");
            WorldDir = new File(TypeDir, Minecraft.getMinecraft().getIntegratedServer().getFolderName());
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

    @SideOnly(Side.CLIENT)
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