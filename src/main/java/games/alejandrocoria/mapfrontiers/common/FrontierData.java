package games.alejandrocoria.mapfrontiers.common;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

@ParametersAreNonnullByDefault
public class FrontierData {
    public enum Change {
        Name, Vertices, Banner, Shared, Other;

        public final static Change[] valuesArray = values();
    }

    public static final int NoSlice = -5;
    public static final int SurfaceSlice = 16;

    protected UUID id;
    protected List<BlockPos> vertices = new ArrayList<>();
    protected boolean closed = false;
    protected String name1 = "New";
    protected String name2 = "Frontier";
    protected boolean nameVisible = true;
    protected int color = 0xffffffff;
    protected ResourceKey<Level> dimension;
    protected int mapSlice = NoSlice;
    protected SettingsUser owner = new SettingsUser();
    protected BannerData banner;
    protected boolean personal = false;
    protected List<SettingsUserShared> usersShared;

    protected Set<Change> changes = EnumSet.noneOf(Change.class);

    public FrontierData() {
        id = new UUID(0, 0);
    }

    public FrontierData(FrontierData other) {
        id = other.id;
        dimension = other.dimension;
        owner = other.owner;
        personal = other.personal;

        closed = other.closed;
        nameVisible = other.nameVisible;
        color = other.color;
        mapSlice = other.mapSlice;

        name1 = other.name1;
        name2 = other.name2;

        banner = other.banner;

        usersShared = other.usersShared;

        vertices = other.vertices;

        changes = EnumSet.noneOf(Change.class);
    }

    public void updateFromData(FrontierData other) {
        id = other.id;
        dimension = other.dimension;
        owner = other.owner;
        personal = other.personal;

        if (other.changes.contains(Change.Other)) {
            closed = other.closed;
            nameVisible = other.nameVisible;
            color = other.color;
            mapSlice = other.mapSlice;
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
                List<ServerPlayer> playerList = server.getPlayerList().getPlayers();
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

    public void setClosed(boolean closed) {
        this.closed = closed;
        changes.add(Change.Other);
    }

    public boolean getClosed() {
        return closed;
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

    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public void setMapSlice(int mapSlice) {
        this.mapSlice = mapSlice;
        changes.add(Change.Other);
    }

    public int getMapSlice() {
        return mapSlice;
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

    public void readFromNBT(CompoundTag nbt, int version) {
        id = UUID.fromString(nbt.getString("id"));
        closed = nbt.getBoolean("closed");
        color = nbt.getInt("color");
        dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(nbt.getString("dimension")));
        name1 = nbt.getString("name1");
        name2 = nbt.getString("name2");
        if (nbt.contains("nameVisible")) {
            nameVisible = nbt.getBoolean("nameVisible");
        }

        mapSlice = nbt.getInt("slice");
        if (version < 6 && mapSlice == -1) {
            mapSlice = NoSlice;
        }
        mapSlice = Math.min(Math.max(mapSlice, NoSlice), SurfaceSlice);

        personal = nbt.getBoolean("personal");

        owner = new SettingsUser();
        owner.readFromNBT(nbt.getCompound("owner"));

        if (nbt.contains("banner")) {
            banner = new BannerData();
            banner.readFromNBT(nbt.getCompound("banner"));
        }

        if (personal) {
            ListTag usersSharedTagList = nbt.getList("usersShared", Tag.TAG_COMPOUND);
            if (!usersSharedTagList.isEmpty()) {
                usersShared = new ArrayList<>();

                for (int i = 0; i < usersSharedTagList.size(); ++i) {
                    SettingsUserShared userShared = new SettingsUserShared();
                    userShared.readFromNBT(usersSharedTagList.getCompound(i));
                    usersShared.add(userShared);
                }
            }
        }

        ListTag verticesTagList = nbt.getList("vertices", Tag.TAG_COMPOUND);
        for (int i = 0; i < verticesTagList.size(); ++i) {
            vertices.add(NbtUtils.readBlockPos(verticesTagList.getCompound(i)));
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("id", id.toString());
        nbt.putBoolean("closed", closed);
        nbt.putInt("color", color);
        nbt.putString("dimension", dimension.location().toString());
        nbt.putString("name1", name1);
        nbt.putString("name2", name2);
        nbt.putBoolean("nameVisible", nameVisible);
        nbt.putInt("slice", mapSlice);
        nbt.putBoolean("personal", personal);

        CompoundTag nbtOwner = new CompoundTag();
        owner.writeToNBT(nbtOwner);
        nbt.put("owner", nbtOwner);

        if (banner != null) {
            CompoundTag nbtBanner = new CompoundTag();
            banner.writeToNBT(nbtBanner);
            nbt.put("banner", nbtBanner);
        }

        if (personal && usersShared != null) {
            ListTag usersSharedTagList = new ListTag();
            for (SettingsUserShared userShared : usersShared) {
                CompoundTag nbtUserShared = new CompoundTag();
                userShared.writeToNBT(nbtUserShared);
                usersSharedTagList.add(nbtUserShared);
            }

            nbt.put("usersShared", usersSharedTagList);
        }

        ListTag verticesTagList = new ListTag();
        for (BlockPos pos : vertices) {
            verticesTagList.add(NbtUtils.writeBlockPos(pos));
        }

        nbt.put("vertices", verticesTagList);
    }

    public void fromBytes(FriendlyByteBuf buf) {
        changes.clear();
        for (Change change : Change.valuesArray) {
            if (buf.readBoolean()) {
                changes.add(change);
            }
        }

        id = UUIDHelper.fromBytes(buf);
        dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
        personal = buf.readBoolean();
        owner = new SettingsUser();
        owner.fromBytes(buf);

        if (changes.contains(Change.Other)) {
            closed = buf.readBoolean();
            color = buf.readInt();
            nameVisible = buf.readBoolean();
            mapSlice = buf.readInt();
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
    }

    public void toBytes(FriendlyByteBuf buf) {
        toBytes(buf, true);
    }

    public void toBytes(FriendlyByteBuf buf, boolean onlyChanges) {
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
            buf.writeBoolean(closed);
            buf.writeInt(color);
            buf.writeBoolean(nameVisible);
            buf.writeInt(mapSlice);
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
    }

    public static class BannerData {
        public DyeColor baseColor;
        public ListTag patterns;

        public BannerData() {
            baseColor = DyeColor.WHITE;
        }

        public BannerData(ItemStack itemBanner) {
            CompoundTag blockEntityTag = itemBanner.getTagElement("BlockEntityTag");

            if (blockEntityTag != null && blockEntityTag.contains("Patterns", Tag.TAG_LIST)) {
                patterns = blockEntityTag.getList("Patterns", Tag.TAG_COMPOUND);
            }

            if (itemBanner.getItem() instanceof BannerItem) {
                baseColor = ((BannerItem) itemBanner.getItem()).getColor();
            }
        }

        public void readFromNBT(CompoundTag nbt) {
            baseColor = DyeColor.byId(nbt.getInt("Base"));
            patterns = nbt.getList("Patterns", Tag.TAG_COMPOUND);
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putInt("Base", baseColor.getId());

            if (patterns != null) {
                nbt.put("Patterns", patterns);
            }
        }

        public void fromBytes(FriendlyByteBuf buf) {
            baseColor = DyeColor.byId(buf.readInt());

            CompoundTag nbt = buf.readNbt();
            if (nbt != null) {
                patterns = nbt.getList("Patterns", Tag.TAG_COMPOUND);
            }
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeInt(baseColor.getId());

            if (patterns == null) {
                buf.writeNbt(null);
            } else {
                CompoundTag nbt = new CompoundTag();
                nbt.put("Patterns", patterns);
                buf.writeNbt(nbt);
            }
        }
    }
}
