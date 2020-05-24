package games.alejandrocoria.mapfrontiers.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;

@ParametersAreNonnullByDefault
public class FrontierData {
    public static final int NoSlice = -1;
    public static final int SurfaceSlice = 16;

    private static int lastID = 0;

    protected int id;
    protected List<BlockPos> vertices = new ArrayList<BlockPos>();
    protected boolean closed = false;
    protected String name1 = "New";
    protected String name2 = "Frontier";
    protected boolean nameVisible = true;
    protected int color = 0xffffff;
    protected int dimension = 0;
    protected int mapSlice = NoSlice;
    protected SettingsUser owner = new SettingsUser();
    protected BannerData banner;

    public FrontierData() {
        id = lastID++;
    }

    public FrontierData(FrontierData other) {
        id = other.id;
        if (id >= lastID) {
            lastID = id + 1;
        }

        vertices = other.vertices;
        closed = other.closed;
        name1 = other.name1;
        name2 = other.name2;
        nameVisible = other.nameVisible;
        color = other.color;
        dimension = other.dimension;
        mapSlice = other.mapSlice;
        owner = other.owner;
        banner = other.banner;
    }

    public void setOwner(SettingsUser owner) {
        this.owner = owner;
    }

    public void ensureOwner() {
        if (owner.isEmpty()) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server.isDedicatedServer()) {
                owner = new SettingsUser(server.getServerOwner());
            } else {
                List<EntityPlayerMP> playerList = server.getPlayerList().getPlayers();
                if (!playerList.isEmpty()) {
                    owner = new SettingsUser(playerList.get(0));
                }
            }
        } else {
            owner.fillMissingInfo(false);
        }
    }

    public SettingsUser getOwner() {
        return owner;
    }

    public void setId(int id) {
        this.id = id;
        if (id >= lastID) {
            lastID = id + 1;
        }
    }

    public int getId() {
        return id;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public BlockPos getVertex(int index) {
        return vertices.get(index);
    }

    public void addVertex(BlockPos pos, int index, int snapDistance) {
        if (snapDistance != 0) {
            pos = MapFrontiers.proxy.snapVertex(pos, snapDistance, this);
        }

        vertices.add(index, new BlockPos(pos.getX(), 70, pos.getZ()));
    }

    public void addVertex(BlockPos pos, int snapDistance) {
        addVertex(pos, vertices.size(), snapDistance);
    }

    public void removeVertex(int index) {
        if (index < 0 || index >= vertices.size()) {
            return;
        }

        vertices.remove(index);
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean getClosed() {
        return closed;
    }

    public void setName1(String name) {
        name1 = name;
    }

    public String getName1() {
        return name1;
    }

    public void setName2(String name) {
        name2 = name;
    }

    public String getName2() {
        return name2;
    }

    public void setNameVisible(boolean nameVisible) {
        this.nameVisible = nameVisible;
    }

    public boolean getNameVisible() {
        return nameVisible;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public int getDimension() {
        return dimension;
    }

    public void setMapSlice(int mapSlice) {
        this.mapSlice = mapSlice;
    }

    public int getMapSlice() {
        return mapSlice;
    }

    public void setBanner(@Nullable ItemStack itemBanner) {
        if (itemBanner == null) {
            banner = null;
            return;
        }

        banner = new BannerData(itemBanner);
    }

    public boolean hasBanner() {
        return banner != null;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        readFromNBT(nbt, FrontiersManager.dataVersion);
    }

    public void readFromNBT(NBTTagCompound nbt, int version) {
        closed = nbt.getBoolean("closed");
        color = nbt.getInteger("color");
        name1 = nbt.getString("name1");
        name2 = nbt.getString("name2");
        if (nbt.hasKey("nameVisible")) {
            nameVisible = nbt.getBoolean("nameVisible");
        }
        mapSlice = nbt.getInteger("slice");
        mapSlice = Math.min(Math.max(mapSlice, -1), 16);

        owner = new SettingsUser();
        if (version == 1) {
            if (nbt.hasKey("ownerUUID")) {
                try {
                    owner.uuid = UUID.fromString(nbt.getString("ownerUUID"));
                } catch (Exception e) {
                    MapFrontiers.LOGGER.error(e.getMessage(), e);
                }
            }

            if (nbt.hasKey("ownerName")) {
                owner.username = nbt.getString("ownerName");
            }

            owner.fillMissingInfo(true);
        } else {
            owner.readFromNBT(nbt.getCompoundTag("owner"));
        }

        if (nbt.hasKey("banner")) {
            banner = new BannerData();
            banner.readFromNBT(nbt.getCompoundTag("banner"));
        }

        NBTTagList verticesTagList = nbt.getTagList("vertices", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < verticesTagList.tagCount(); ++i) {
            vertices.add(NBTUtil.getPosFromTag(verticesTagList.getCompoundTagAt(i)));
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("closed", closed);
        nbt.setInteger("color", color);
        nbt.setString("name1", name1);
        nbt.setString("name2", name2);
        nbt.setBoolean("nameVisible", nameVisible);
        nbt.setInteger("slice", mapSlice);

        NBTTagCompound nbtOwner = new NBTTagCompound();
        owner.writeToNBT(nbtOwner);
        nbt.setTag("owner", nbtOwner);

        if (banner != null) {
            NBTTagCompound nbtBanner = new NBTTagCompound();
            banner.writeToNBT(nbtBanner);
            nbt.setTag("banner", nbtBanner);
        }

        NBTTagList verticesTagList = new NBTTagList();
        for (BlockPos pos : vertices) {
            verticesTagList.appendTag(NBTUtil.createPosTag(pos));
        }

        nbt.setTag("vertices", verticesTagList);
    }

    public class BannerData {
        public EnumDyeColor baseColor;
        public NBTTagList patterns;
        public List<BannerPattern> patternList;
        public List<EnumDyeColor> colorList;
        public String patternResourceLocation;

        public BannerData() {
            baseColor = EnumDyeColor.BLACK;
            patternList = new ArrayList<BannerPattern>();
            colorList = new ArrayList<EnumDyeColor>();
            patternResourceLocation = "";
        }

        public BannerData(ItemStack itemBanner) {
            NBTTagCompound blockEntityTag = itemBanner.getSubCompound("BlockEntityTag");

            if (blockEntityTag != null && blockEntityTag.hasKey("Patterns", Constants.NBT.TAG_LIST)) {
                patterns = blockEntityTag.getTagList("Patterns", Constants.NBT.TAG_COMPOUND);
            } else {
                return;
            }

            baseColor = ItemBanner.getBaseColor(itemBanner);
            initializeBannerData();
        }

        public void readFromNBT(NBTTagCompound nbt) {
            baseColor = EnumDyeColor.byDyeDamage(nbt.getInteger("Base"));
            patterns = nbt.getTagList("Patterns", 10);

            initializeBannerData();
        }

        public void writeToNBT(NBTTagCompound nbt) {
            nbt.setInteger("Base", baseColor.getDyeDamage());

            if (patterns != null) {
                nbt.setTag("Patterns", patterns);
            }
        }

        private void initializeBannerData() {
            patternList = new ArrayList<BannerPattern>();
            colorList = new ArrayList<EnumDyeColor>();
            patternList.add(BannerPattern.BASE);
            colorList.add(baseColor);
            patternResourceLocation = "b" + baseColor.getDyeDamage();

            if (patterns != null) {
                for (int i = 0; i < patterns.tagCount(); ++i) {
                    NBTTagCompound nbttagcompound = patterns.getCompoundTagAt(i);
                    BannerPattern bannerpattern = BannerPattern.byHash(nbttagcompound.getString("Pattern"));

                    if (bannerpattern != null) {
                        patternList.add(bannerpattern);
                        int j = nbttagcompound.getInteger("Color");
                        colorList.add(EnumDyeColor.byDyeDamage(j));
                        patternResourceLocation = patternResourceLocation + bannerpattern.getHashname() + j;
                    }
                }
            }
        }
    }
}