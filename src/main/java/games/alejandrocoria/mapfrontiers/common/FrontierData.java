package games.alejandrocoria.mapfrontiers.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;

@ParametersAreNonnullByDefault
public class FrontierData {
    public static final int NoSlice = -1;
    public static final int SurfaceSlice = 16;

    protected UUID id;
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
    protected boolean personal = false;
    protected List<SettingsUserShared> usersShared;

    public FrontierData() {
        id = new UUID(0, 0);
    }

    public FrontierData(FrontierData other) {
        id = other.id;
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
        personal = other.personal;
        usersShared = other.usersShared;
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

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public BlockPos getVertex(int index) {
        return vertices.get(index);
    }

    public void addVertex(BlockPos pos, int index) {
        vertices.add(index, new BlockPos(pos.getX(), 70, pos.getZ()));
    }

    public void addVertex(BlockPos pos) {
        addVertex(pos, vertices.size());
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

    public void setPersonal(boolean personal) {
        this.personal = personal;
    }

    public boolean getPersonal() {
        return personal;
    }

    public void addUserShared(SettingsUserShared userShared) {
        if (usersShared == null) {
            usersShared = new ArrayList<SettingsUserShared>();
        }

        usersShared.add(userShared);
    }

    public void removeUserShared(int index) {
        if (usersShared == null) {
            return;
        }

        usersShared.remove(index);

        if (usersShared.isEmpty()) {
            usersShared = null;
        }
    }

    public void removeUserShared(SettingsUser user) {
        if (usersShared == null) {
            return;
        }

        usersShared.removeIf(x -> x.getUser().equals(user));
    }

    public void setUsersShared(List<SettingsUserShared> usersShared) {
        this.usersShared = usersShared;
    }

    public void removePendingUsersShared() {
        if (usersShared == null) {
            return;
        }

        usersShared.removeIf(x -> x.isPending());
    }

    public List<SettingsUserShared> getUsersShared() {
        return usersShared;
    }

    public SettingsUserShared getUserShared(SettingsUser user) {
        if (usersShared == null) {
            return null;
        }

        for (SettingsUserShared u : usersShared) {
            if (u.getUser().equals(user)) {
                return u;
            }
        }

        return null;
    }

    public boolean hasUserShared(SettingsUser user) {
        return getUserShared(user) != null;
    }

    public boolean checkActionUserShared(SettingsUser user, SettingsUserShared.Action action) {
        if (user.equals(owner)) {
            return true;
        }

        if (usersShared == null) {
            return false;
        }

        SettingsUserShared userShared = getUserShared(user);
        if (userShared == null) {
            return false;
        }

        return userShared.hasAction(action);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        readFromNBT(nbt, FrontiersManager.dataVersion);
    }

    public void readFromNBT(NBTTagCompound nbt, int version) {
        if (version >= 4) {
            id = UUID.fromString(nbt.getString("id"));
            dimension = nbt.getInteger("dimension");
            personal = nbt.getBoolean("personal");
        }

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

        if (personal) {
            NBTTagList usersSharedTagList = nbt.getTagList("usersShared", Constants.NBT.TAG_COMPOUND);
            if (!usersSharedTagList.hasNoTags()) {
                usersShared = new ArrayList<SettingsUserShared>();

                for (int i = 0; i < usersSharedTagList.tagCount(); ++i) {
                    SettingsUserShared userShared = new SettingsUserShared();
                    userShared.readFromNBT(usersSharedTagList.getCompoundTagAt(i));
                    usersShared.add(userShared);
                }
            }
        }

        NBTTagList verticesTagList = nbt.getTagList("vertices", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < verticesTagList.tagCount(); ++i) {
            vertices.add(NBTUtil.getPosFromTag(verticesTagList.getCompoundTagAt(i)));
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("id", id.toString());
        nbt.setBoolean("closed", closed);
        nbt.setInteger("color", color);
        nbt.setInteger("dimension", dimension);
        nbt.setString("name1", name1);
        nbt.setString("name2", name2);
        nbt.setBoolean("nameVisible", nameVisible);
        nbt.setInteger("slice", mapSlice);
        nbt.setBoolean("personal", personal);

        NBTTagCompound nbtOwner = new NBTTagCompound();
        owner.writeToNBT(nbtOwner);
        nbt.setTag("owner", nbtOwner);

        if (banner != null) {
            NBTTagCompound nbtBanner = new NBTTagCompound();
            banner.writeToNBT(nbtBanner);
            nbt.setTag("banner", nbtBanner);
        }

        if (personal && usersShared != null) {
            NBTTagList usersSharedTagList = new NBTTagList();
            for (SettingsUserShared userShared : usersShared) {
                NBTTagCompound nbtUserShared = new NBTTagCompound();
                userShared.writeToNBT(nbtUserShared);
                usersSharedTagList.appendTag(nbtUserShared);
            }

            nbt.setTag("usersShared", usersSharedTagList);
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

        public BannerData() {
            baseColor = EnumDyeColor.WHITE;
        }

        public BannerData(ItemStack itemBanner) {
            NBTTagCompound blockEntityTag = itemBanner.getSubCompound("BlockEntityTag");

            if (blockEntityTag != null && blockEntityTag.hasKey("Patterns", Constants.NBT.TAG_LIST)) {
                patterns = blockEntityTag.getTagList("Patterns", Constants.NBT.TAG_COMPOUND);
            }

            baseColor = ItemBanner.getBaseColor(itemBanner);
        }

        public void readFromNBT(NBTTagCompound nbt) {
            baseColor = EnumDyeColor.byDyeDamage(nbt.getInteger("Base"));
            patterns = nbt.getTagList("Patterns", 10);
        }

        public void writeToNBT(NBTTagCompound nbt) {
            nbt.setInteger("Base", baseColor.getDyeDamage());

            if (patterns != null) {
                nbt.setTag("Patterns", patterns);
            }
        }
    }
}