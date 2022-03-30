package games.alejandrocoria.mapfrontiers.common;

import java.util.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.BlockPosHelper;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraftforge.common.util.Constants;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BannerItem;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@ParametersAreNonnullByDefault
public class FrontierData {
    public enum Change {
        Name, Vertices, Banner, Shared, Other;

        public final static Change[] valuesArray = values();
    }

    protected UUID id;
    protected List<BlockPos> vertices = new ArrayList<>();
    protected boolean visible = true;
    protected String name1 = "New";
    protected String name2 = "Frontier";
    protected boolean nameVisible = true;
    protected int color = 0xffffffff;
    protected RegistryKey<World> dimension;
    protected SettingsUser owner = new SettingsUser();
    protected BannerData banner;
    protected boolean personal = false;
    protected List<SettingsUserShared> usersShared;
    protected Date created;
    protected Date modified;

    protected Set<Change> changes = EnumSet.noneOf(Change.class);

    public FrontierData() {
        id = new UUID(0, 0);
    }

    public FrontierData(FrontierData other) {
        id = other.id;
        dimension = other.dimension;
        owner = other.owner;
        personal = other.personal;

        visible = other.visible;
        nameVisible = other.nameVisible;
        color = other.color;

        name1 = other.name1;
        name2 = other.name2;

        banner = other.banner;

        usersShared = other.usersShared;

        vertices = other.vertices;

        created = other.created;
        modified = other.modified;

        changes = EnumSet.noneOf(Change.class);
    }

    public void updateFromData(FrontierData other) {
        id = other.id;
        dimension = other.dimension;
        owner = other.owner;
        personal = other.personal;

        if (other.changes.contains(Change.Other)) {
            visible = other.visible;
            nameVisible = other.nameVisible;
            color = other.color;
        }

        if (other.changes.contains(Change.Name)) {
            name1 = other.name1;
            name2 = other.name2;
        }

        if (other.changes.contains(Change.Banner)) {
            banner = other.banner;
        }

        if (other.changes.contains(Change.Shared)) {
            usersShared = other.usersShared;
        }

        if (other.changes.contains(Change.Vertices)) {
            vertices = other.vertices;
        }

        modified = other.modified;

        changes = EnumSet.noneOf(Change.class);
    }

    public void setOwner(SettingsUser owner) {
        this.owner = owner;
    }

    public void ensureOwner() {
        if (owner.isEmpty()) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            //noinspection StatementWithEmptyBody
            if (server.isDedicatedServer()) {
                // @Incomplete: I can't find a way to get the server owner.
                //owner = new SettingsUser(server.getServerOwner());
            } else {
                List<ServerPlayerEntity> playerList = server.getPlayerList().getPlayers();
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
        vertices.add(index, BlockPosHelper.atY(pos,70));
        changes.add(Change.Vertices);
    }

    public void addVertex(BlockPos pos) {
        addVertex(pos, vertices.size());
    }

    public void removeVertex(int index) {
        if (index < 0 || index >= vertices.size()) {
            return;
        }

        vertices.remove(index);
        changes.add(Change.Vertices);
    }

    public void moveVertex(BlockPos pos, int index) {
        if (index < 0 || index >= vertices.size()) {
            return;
        }

        vertices.set(index, pos);
        changes.add(Change.Vertices);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        changes.add(Change.Other);
    }

    public boolean getVisible() {
        return visible;
    }

    public void setName1(String name) {
        name1 = name;
        changes.add(Change.Name);
    }

    public String getName1() {
        return name1;
    }

    public void setName2(String name) {
        name2 = name;
        changes.add(Change.Name);
    }

    public String getName2() {
        return name2;
    }

    public void setNameVisible(boolean nameVisible) {
        this.nameVisible = nameVisible;
        changes.add(Change.Other);
    }

    public boolean getNameVisible() {
        return nameVisible;
    }

    public void setColor(int color) {
        this.color = color;
        changes.add(Change.Other);
    }

    public int getColor() {
        return color;
    }

    public void setDimension(RegistryKey<World> dimension) {
        this.dimension = dimension;
    }

    public RegistryKey<World> getDimension() {
        return dimension;
    }

    public void setBanner(@Nullable ItemStack itemBanner) {
        changes.add(Change.Banner);

        if (itemBanner == null) {
            banner = null;
        } else {
            banner = new BannerData(itemBanner);
        }
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
            usersShared = new ArrayList<>();
        }

        usersShared.add(userShared);
        changes.add(Change.Shared);
    }

    public void removeUserShared(int index) {
        if (usersShared == null) {
            return;
        }

        usersShared.remove(index);

        if (usersShared.isEmpty()) {
            usersShared = null;
        }

        changes.add(Change.Shared);
    }

    public void removeUserShared(SettingsUser user) {
        if (usersShared == null) {
            return;
        }

        usersShared.removeIf(x -> x.getUser().equals(user));
        changes.add(Change.Shared);
    }

    public void setUsersShared(List<SettingsUserShared> usersShared) {
        this.usersShared = usersShared;
        changes.add(Change.Shared);
    }

    public void removePendingUsersShared() {
        if (usersShared == null) {
            return;
        }

        usersShared.removeIf(SettingsUserShared::isPending);
        changes.add(Change.Shared);
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

    public void setCreated(Date created) {
        this.created = created;
        modified = created;
    }

    public Date getCreated() {
        return created;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public Date getModified() {
        return modified;
    }

    // @Note: To record changes if done outside this class.
    // It would be better to change that.
    public void addChange(Change change) {
        changes.add(change);
    }

    public void removeChange(Change change) {
        changes.remove(change);
    }

    public void removeChanges() {
        changes.clear();
    }

    public boolean hasChange(Change change) {
        return changes.contains(change);
    }

    public void readFromNBT(CompoundNBT nbt, int version) {
        id = UUID.fromString(nbt.getString("id"));
        if (nbt.contains("visible")) {
            visible = nbt.getBoolean("visible");
        }
        color = nbt.getInt("color");
        dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(nbt.getString("dimension")));
        name1 = nbt.getString("name1");
        name2 = nbt.getString("name2");
        if (nbt.contains("nameVisible")) {
            nameVisible = nbt.getBoolean("nameVisible");
        }

        personal = nbt.getBoolean("personal");

        owner = new SettingsUser();
        owner.readFromNBT(nbt.getCompound("owner"));

        if (nbt.contains("banner")) {
            banner = new BannerData();
            banner.readFromNBT(nbt.getCompound("banner"));
        }

        if (personal) {
            ListNBT usersSharedTagList = nbt.getList("usersShared", Constants.NBT.TAG_COMPOUND);
            if (!usersSharedTagList.isEmpty()) {
                usersShared = new ArrayList<>();

                for (int i = 0; i < usersSharedTagList.size(); ++i) {
                    SettingsUserShared userShared = new SettingsUserShared();
                    userShared.readFromNBT(usersSharedTagList.getCompound(i));
                    usersShared.add(userShared);
                }
            }
        }

        ListNBT verticesTagList = nbt.getList("vertices", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < verticesTagList.size(); ++i) {
            vertices.add(NBTUtil.readBlockPos(verticesTagList.getCompound(i)));
        }

        if (nbt.contains("created")) {
            created = new Date(nbt.getLong("created"));
        }

        if (nbt.contains("modified")) {
            modified = new Date(nbt.getLong("modified"));
        }
    }

    public void writeToNBT(CompoundNBT nbt) {
        nbt.putString("id", id.toString());
        nbt.putBoolean("visible", visible);
        nbt.putInt("color", color);
        nbt.putString("dimension", dimension.location().toString());
        nbt.putString("name1", name1);
        nbt.putString("name2", name2);
        nbt.putBoolean("nameVisible", nameVisible);
        nbt.putBoolean("personal", personal);

        CompoundNBT nbtOwner = new CompoundNBT();
        owner.writeToNBT(nbtOwner);
        nbt.put("owner", nbtOwner);

        if (banner != null) {
            CompoundNBT nbtBanner = new CompoundNBT();
            banner.writeToNBT(nbtBanner);
            nbt.put("banner", nbtBanner);
        }

        if (personal && usersShared != null) {
            ListNBT usersSharedTagList = new ListNBT();
            for (SettingsUserShared userShared : usersShared) {
                CompoundNBT nbtUserShared = new CompoundNBT();
                userShared.writeToNBT(nbtUserShared);
                usersSharedTagList.add(nbtUserShared);
            }

            nbt.put("usersShared", usersSharedTagList);
        }

        ListNBT verticesTagList = new ListNBT();
        for (BlockPos pos : vertices) {
            verticesTagList.add(NBTUtil.writeBlockPos(pos));
        }

        nbt.put("vertices", verticesTagList);

        if (created != null) {
            nbt.putLong("created", created.getTime());
        }

        if (modified != null) {
            nbt.putLong("modified", modified.getTime());
        }
    }

    public void fromBytes(PacketBuffer buf) {
        changes.clear();
        for (Change change : Change.valuesArray) {
            if (buf.readBoolean()) {
                changes.add(change);
            }
        }

        id = UUIDHelper.fromBytes(buf);
        dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
        personal = buf.readBoolean();
        owner = new SettingsUser();
        owner.fromBytes(buf);

        if (changes.contains(Change.Other)) {
            visible = buf.readBoolean();
            color = buf.readInt();
            nameVisible = buf.readBoolean();
        }


        if (changes.contains(Change.Name)) {
            int maxCharacters = 17;
            int maxBytes = maxCharacters * 4;
            name1 = buf.readUtf(maxBytes);
            name2 = buf.readUtf(maxBytes);

            if (name1.length() > maxCharacters) {
                name1 = name1.substring(0, maxCharacters);
            }
            if (name2.length() > maxCharacters) {
                name2 = name2.substring(0, maxCharacters);
            }
        }

        if (changes.contains(Change.Banner)) {
            if (buf.readBoolean()) {
                banner = new BannerData();
                banner.fromBytes(buf);
            } else {
                banner = null;
            }
        }

        if (changes.contains(Change.Shared)) {
            if (buf.readBoolean()) {
                usersShared = new ArrayList<>();
                int usersCount = buf.readInt();
                for (int i = 0; i < usersCount; ++i) {
                    SettingsUserShared userShared = new SettingsUserShared();
                    userShared.fromBytes(buf);
                    usersShared.add(userShared);
                }
            } else {
                usersShared = null;
            }
        }

        if (changes.contains(Change.Vertices)) {
            vertices = new ArrayList<>();
            int vertexCount = buf.readInt();
            for (int i = 0; i < vertexCount; ++i) {
                BlockPos vertex = BlockPos.of(buf.readLong());
                vertices.add(vertex);
            }
        }

        if (buf.readBoolean()) {
            created = new Date(buf.readLong());
        } else {
            created = null;
        }

        if (buf.readBoolean()) {
            modified = new Date(buf.readLong());
        } else {
            modified = null;
        }
    }

    public void toBytes(PacketBuffer buf) {
        toBytes(buf, true);
    }

    public void toBytes(PacketBuffer buf, boolean onlyChanges) {
        for (Change change : Change.valuesArray) {
            if (onlyChanges) {
                buf.writeBoolean(changes.contains(change));
            } else {
                buf.writeBoolean(true);
            }
        }

        UUIDHelper.toBytes(buf, id);
        buf.writeResourceLocation(dimension.location());
        buf.writeBoolean(personal);
        owner.toBytes(buf);

        if (!onlyChanges || changes.contains(Change.Other)) {
            buf.writeBoolean(visible);
            buf.writeInt(color);
            buf.writeBoolean(nameVisible);
        }

        if (!onlyChanges || changes.contains(Change.Name)) {
            int maxCharacters = 17;
            int maxBytes = maxCharacters * 4;
            buf.writeUtf(name1, maxBytes);
            buf.writeUtf(name2, maxBytes);
        }

        if (!onlyChanges || changes.contains(Change.Banner)) {
            if (banner == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                banner.toBytes(buf);
            }
        }

        if (!onlyChanges || changes.contains(Change.Shared)) {
            if (personal && usersShared != null) {
                buf.writeBoolean(true);

                buf.writeInt(usersShared.size());
                for (SettingsUserShared userShared : usersShared) {
                    userShared.toBytes(buf);
                }
            } else {
                buf.writeBoolean(false);
            }
        }

        if (!onlyChanges || changes.contains(Change.Vertices)) {
            buf.writeInt(vertices.size());
            for (BlockPos pos : vertices) {
                buf.writeLong(pos.asLong());
            }
        }

        if (created == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeLong(created.getTime());
        }

        if (modified == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeLong(modified.getTime());
        }
    }

    public static class BannerData {
        public DyeColor baseColor;
        public ListNBT patterns;

        public BannerData() {
            baseColor = DyeColor.WHITE;
        }

        public BannerData(ItemStack itemBanner) {
            CompoundNBT blockEntityTag = itemBanner.getTagElement("BlockEntityTag");

            if (blockEntityTag != null && blockEntityTag.contains("Patterns", Constants.NBT.TAG_LIST)) {
                patterns = blockEntityTag.getList("Patterns", Constants.NBT.TAG_COMPOUND);
            }

            if (itemBanner.getItem() instanceof BannerItem) {
                baseColor = ((BannerItem) itemBanner.getItem()).getColor();
            }
        }

        public void readFromNBT(CompoundNBT nbt) {
            baseColor = DyeColor.byId(nbt.getInt("Base"));
            patterns = nbt.getList("Patterns", Constants.NBT.TAG_COMPOUND);
        }

        public void writeToNBT(CompoundNBT nbt) {
            nbt.putInt("Base", baseColor.getId());

            if (patterns != null) {
                nbt.put("Patterns", patterns);
            }
        }

        public void fromBytes(PacketBuffer buf) {
            baseColor = DyeColor.byId(buf.readInt());

            CompoundNBT nbt = buf.readNbt();
            if (nbt != null) {
                patterns = nbt.getList("Patterns", Constants.NBT.TAG_COMPOUND);
            }
        }

        public void toBytes(PacketBuffer buf) {
            buf.writeInt(baseColor.getId());

            if (patterns == null) {
                buf.writeNbt(null);
            } else {
                CompoundNBT nbt = new CompoundNBT();
                nbt.put("Patterns", patterns);
                buf.writeNbt(nbt);
            }
        }
    }
}
