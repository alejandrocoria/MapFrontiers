package games.alejandrocoria.mapfrontiers.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

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
    protected UUID ownerUUID;
    protected String ownerName;

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
        ownerUUID = other.ownerUUID;
        ownerName = other.ownerName;
    }

    public void setOwner(EntityPlayer player) {
        ownerName = player.getName();
        ownerUUID = player.getUniqueID();
    }

    public void setOwner(UUID uuid) {
        ownerUUID = uuid;
        String username = UUIDHelper.getNameFromUUID(ownerUUID);
        if (username != null && !username.isEmpty()) {
            ownerName = username;
        }
    }

    public void setOwner(String username) {
        ownerName = username;
        UUID uuid = UUIDHelper.getUUIDFromName(ownerName);
        if (uuid != null) {
            ownerUUID = uuid;
        }
    }

    public void ensureOwner() {
        if (ownerUUID == null && (ownerName == null || ownerName.isEmpty())) {
            if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
                List<EntityPlayerMP> playerList = Minecraft.getMinecraft().getIntegratedServer().getPlayerList().getPlayers();
                if (!playerList.isEmpty()) {
                    ownerName = playerList.get(0).getName();
                    ownerUUID = UUIDHelper.getUUIDFromName(ownerName);
                }
            } else {
                // @Incomplete?
            }
        } else if (ownerUUID == null) {
            ownerUUID = UUIDHelper.getUUIDFromName(ownerName);
        } else {
            ownerName = UUIDHelper.getNameFromUUID(ownerUUID);
        }
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
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
            pos = FrontiersManager.instance.snapVertex(pos, snapDistance, this);
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

    public void readFromNBT(NBTTagCompound nbt) {
        closed = nbt.getBoolean("closed");
        color = nbt.getInteger("color");
        name1 = nbt.getString("name1");
        name2 = nbt.getString("name2");
        if (nbt.hasKey("nameVisible")) {
            nameVisible = nbt.getBoolean("nameVisible");
        }
        mapSlice = nbt.getInteger("slice");
        mapSlice = Math.min(Math.max(mapSlice, -1), 16);
        if (nbt.hasKey("ownerUUID")) {
            try {
                ownerUUID = UUID.fromString(nbt.getString("ownerUUID"));
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        }
        if (nbt.hasKey("ownerName")) {
            ownerName = nbt.getString("ownerName");
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
        if (ownerUUID != null) {
            nbt.setString("ownerUUID", ownerUUID.toString());
        }
        if (ownerName != null && !ownerName.isEmpty()) {
            nbt.setString("ownerName", ownerName);
        }

        NBTTagList verticesTagList = new NBTTagList();
        for (BlockPos pos : vertices) {
            verticesTagList.appendTag(NBTUtil.createPosTag(pos));
        }

        nbt.setTag("vertices", verticesTagList);
    }
}