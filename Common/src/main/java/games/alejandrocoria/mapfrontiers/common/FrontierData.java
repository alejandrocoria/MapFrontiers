package games.alejandrocoria.mapfrontiers.common;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.StringHelper;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public class FrontierData {
    public enum Change {
        Name, Vertices, Banner, Shared, Visibility, Color;

        public final static Change[] valuesArray = values();
    }

    public enum Mode {
        Vertex, Chunk
    }

    protected UUID id;
    protected final List<BlockPos> vertices = new ArrayList<>();
    protected Set<ChunkPos> chunks = new HashSet<>();
    protected Mode mode = Mode.Vertex;
    protected String name1 = "New";
    protected String name2 = "Frontier";
    protected VisibilityData visibilityData;
    protected int color = 0xffffffff;
    protected ResourceKey<Level> dimension;
    protected SettingsUser owner = new SettingsUser();
    protected BannerData banner;
    protected boolean personal = false;
    protected List<SettingsUserShared> usersShared;
    protected Date created;
    protected Date modified;

    protected Set<Change> changes = EnumSet.noneOf(Change.class);

    public FrontierData() {
        id = new UUID(0, 0);
        visibilityData = new VisibilityData();
    }

    public FrontierData(FrontierData other) {
        id = other.id;
        dimension = other.dimension;
        owner = other.owner;
        personal = other.personal;

        visibilityData = new VisibilityData(other.visibilityData);
        color = other.color;

        name1 = other.name1;
        name2 = other.name2;

        banner = other.banner;

        usersShared = other.usersShared;

        vertices.clear();
        vertices.addAll(other.vertices);
        chunks.clear();
        chunks.addAll(other.chunks);
        mode = other.mode;

        created = other.created;
        modified = other.modified;

        changes = EnumSet.noneOf(Change.class);
    }

    public void updateFromData(FrontierData other) {
        if (other == this) {
            changes = EnumSet.noneOf(Change.class);
            return;
        }

        id = other.id;
        dimension = other.dimension;
        owner = other.owner;
        personal = other.personal;

        if (other.changes.contains(Change.Visibility)) {
            visibilityData = other.visibilityData;
        }

        if (other.changes.contains(Change.Color)) {
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
            vertices.clear();
            vertices.addAll(other.vertices);
            chunks.clear();
            chunks.addAll(other.chunks);
            mode = other.mode;
        }

        modified = other.modified;

        changes = EnumSet.noneOf(Change.class);
    }

    public void setOwner(SettingsUser owner) {
        this.owner = owner;
    }

    public void ensureOwner(MinecraftServer server) {
        if (owner.isEmpty()) {
            //noinspection StatementWithEmptyBody
            if (server.isDedicatedServer()) {
                // @Incomplete: I can't find a way to get the server owner.
                //owner = new SettingsUser(server.getServerOwner());
            } else {
                List<ServerPlayer> playerList = server.getPlayerList().getPlayers();
                if (!playerList.isEmpty()) {
                    owner = new SettingsUser(playerList.getFirst());
                }
            }
        } else {
            owner.fillMissingInfo(false, server);
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

    protected void addVertex(BlockPos pos, int index) {
        synchronized (vertices) {
            vertices.add(index, pos.atY(70));
        }
        changes.add(Change.Vertices);
    }

    public void addVertex(BlockPos pos) {
        synchronized (vertices) {
            addVertex(pos, vertices.size());
        }
    }

    public void removeVertex(int index) {
        if (index < 0 || index >= vertices.size()) {
            return;
        }

        synchronized (vertices) {
            vertices.remove(index);
        }
        changes.add(Change.Vertices);
    }

    protected void moveVertex(BlockPos pos, int index) {
        if (index < 0 || index >= vertices.size()) {
            return;
        }

        synchronized (vertices) {
            vertices.set(index, pos);
        }
        changes.add(Change.Vertices);
    }

    public void moveAllVertices(BlockPos delta) {
        synchronized (vertices) {
            vertices.replaceAll(blockPos -> blockPos.offset(delta));
        }
        changes.add(Change.Vertices);
    }

    public boolean toggleChunk(ChunkPos chunk) {
        boolean added = false;
        synchronized (chunks) {
            if (!chunks.remove(chunk)) {
                chunks.add(chunk);
                added = true;
            }
        }

        changes.add(Change.Vertices);
        return added;
    }

    public boolean addChunk(ChunkPos chunk) {
        synchronized (chunks) {
            if (chunks.add(chunk)) {
                changes.add(Change.Vertices);
                return true;
            }
        }

        return false;
    }

    public boolean removeChunk(ChunkPos chunk) {
        synchronized (chunks) {
            if (chunks.remove(chunk)) {
                changes.add(Change.Vertices);
                return true;
            }
        }

        return false;
    }

    public int getChunkCount() {
        return chunks.size();
    }

    public void moveAllChunks(ChunkPos delta) {
        synchronized (chunks) {
            chunks = chunks.stream().map(chunk -> new ChunkPos(chunk.x + delta.x, chunk.z + delta.z)).collect(Collectors.toSet());
        }
        changes.add(Change.Vertices);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        changes.add(Change.Vertices);
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isEmpty() {
        if (mode == Mode.Vertex) {
            return vertices.isEmpty();
        } else {
            return chunks.isEmpty();
        }
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

    public boolean isNamed() {
        return !StringUtils.isBlank(name1) || !StringUtils.isBlank(name2);
    }

    public void setVisibility(VisibilityData.Visibility visibility, boolean enable) {
        this.visibilityData.setValue(visibility, enable);
        changes.add(Change.Visibility);
    }

    public void toggleVisibility(VisibilityData.Visibility visibility) {
        this.visibilityData.setValue(visibility, !this.visibilityData.getValue(visibility));
        changes.add(Change.Visibility);
    }

    public boolean getVisibility(VisibilityData.Visibility visibility) {
        return visibilityData.getValue(visibility);
    }

    public void setVisibilityData(VisibilityData visibilityData) {
        this.visibilityData = visibilityData;
        changes.add(Change.Visibility);
    }

    public VisibilityData getVisibilityData() {
        return visibilityData;
    }

    public void setColor(int color) {
        this.color = color;
        changes.add(Change.Color);
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

    public void setBanner(@Nullable ItemStack itemBanner) {
        changes.add(Change.Banner);

        if (itemBanner == null) {
            banner = null;
        } else {
            banner = new BannerData(itemBanner);
        }
    }

    public void setBanner(DyeColor base, BannerPatternLayers bannerPatterns) {
        banner = new BannerData(base, bannerPatterns);
        changes.add(Change.Banner);
    }

    public boolean hasBanner() {
        return banner != null;
    }

    public void setBannerData(@Nullable BannerData bannerData) {
        banner = bannerData;
    }

    public BannerData getbannerData() {
        return banner;
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

    public void removeAllUserShared() {
        if (usersShared == null) {
            return;
        }

        usersShared = null;
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

    public Set<Change> getChanges() {
        return EnumSet.copyOf(changes);
    }

    public void readFromNBT(CompoundTag nbt, int version) {
        id = UUID.fromString(nbt.getString("id"));
        color = nbt.getInt("color");
        dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(nbt.getString("dimension")));
        name1 = nbt.getString("name1");
        name2 = nbt.getString("name2");

        visibilityData.readFromNBT(nbt, version);

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
            CompoundTag posTag = verticesTagList.getCompound(i);
            vertices.add(new BlockPos(posTag.getInt("X"), 70, posTag.getInt("Z")));
        }

        ListTag chunksTagList = nbt.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < chunksTagList.size(); ++i) {
            chunks.add(new ChunkPos(chunksTagList.getCompound(i).getInt("X"), chunksTagList.getCompound(i).getInt("Z")));
        }

        String modeTag = nbt.getString("mode");
        if (modeTag.isEmpty()) {
            mode = Mode.Vertex;
        } else {
            try {
                mode = Mode.valueOf(modeTag);
            } catch (IllegalArgumentException e) {
                if (chunks.size() > 0) {
                    mode = Mode.Chunk;
                } else {
                    mode = Mode.Vertex;
                }

                String availableModes = StringHelper.enumValuesToString(Arrays.asList(Mode.values()));

                MapFrontiers.LOGGER.warn(String.format("Unknown mode in frontier %1$s. Found: \"%2$s\". Expected: %3$s",
                        id, modeTag, availableModes));
            }
        }

        if (nbt.contains("created")) {
            created = new Date(nbt.getLong("created"));
        }

        if (nbt.contains("modified")) {
            modified = new Date(nbt.getLong("modified"));
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("id", id.toString());
        nbt.putInt("color", color);
        nbt.putString("dimension", dimension.location().toString());
        nbt.putString("name1", name1);
        nbt.putString("name2", name2);
        visibilityData.writeToNBT(nbt);
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
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putInt("X", pos.getX());
            compoundtag.putInt("Y", pos.getY());
            compoundtag.putInt("Z", pos.getZ());
            verticesTagList.add(compoundtag);
        }

        nbt.put("vertices", verticesTagList);

        ListTag chunksTagList = new ListTag();
        for (ChunkPos pos : chunks) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putInt("X", pos.x);
            compoundtag.putInt("Z", pos.z);
            chunksTagList.add(compoundtag);
        }

        nbt.put("chunks", chunksTagList);

        nbt.putString("mode", mode.name());

        if (created != null) {
            nbt.putLong("created", created.getTime());
        }

        if (modified != null) {
            nbt.putLong("modified", modified.getTime());
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        changes.clear();
        for (Change change : Change.valuesArray) {
            if (buf.readBoolean()) {
                changes.add(change);
            }
        }

        id = UUIDHelper.fromBytes(buf);
        dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        personal = buf.readBoolean();
        owner = new SettingsUser();
        owner.fromBytes(buf);

        if (changes.contains(Change.Visibility)) {
            visibilityData.fromBytes(buf);
        }

        if (changes.contains(Change.Color)) {
            color = buf.readInt();
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
            vertices.clear();
            int vertexCount = buf.readInt();
            for (int i = 0; i < vertexCount; ++i) {
                BlockPos vertex = BlockPos.of(buf.readLong());
                vertices.add(vertex);
            }

            chunks.clear();
            int chunkCount = buf.readInt();
            for (int i = 0; i < chunkCount; ++i) {
                ChunkPos chunk = new ChunkPos(buf.readLong());
                chunks.add(chunk);
            }

            mode = Mode.values()[buf.readInt()];
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

    public void toBytes(FriendlyByteBuf buf) {
        toBytes(buf, true);
    }

    public void toBytes(FriendlyByteBuf buf, boolean onlyChanges) {
        toBytes(buf, onlyChanges ? changes : null);
    }

    public void toBytes(FriendlyByteBuf buf, @Nullable Set<FrontierData.Change> withChanges) {
        for (Change change : Change.valuesArray) {
            if (withChanges != null) {
                buf.writeBoolean(withChanges.contains(change));
            } else {
                buf.writeBoolean(true);
            }
        }

        UUIDHelper.toBytes(buf, id);
        buf.writeResourceLocation(dimension.location());
        buf.writeBoolean(personal);
        owner.toBytes(buf);

        if (withChanges == null || withChanges.contains(Change.Visibility)) {
            visibilityData.toBytes(buf);
        }

        if (withChanges == null || withChanges.contains(Change.Color)) {
            buf.writeInt(color);
        }

        if (withChanges == null || withChanges.contains(Change.Name)) {
            int maxCharacters = 17;
            int maxBytes = maxCharacters * 4;
            buf.writeUtf(name1, maxBytes);
            buf.writeUtf(name2, maxBytes);
        }

        if (withChanges == null || withChanges.contains(Change.Banner)) {
            if (banner == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                banner.toBytes(buf);
            }
        }

        if (withChanges == null || withChanges.contains(Change.Shared)) {
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

        if (withChanges == null || withChanges.contains(Change.Vertices)) {
            buf.writeInt(vertices.size());
            for (BlockPos pos : vertices) {
                buf.writeLong(pos.asLong());
            }

            buf.writeInt(chunks.size());
            for (ChunkPos pos : chunks) {
                buf.writeLong(pos.toLong());
            }

            buf.writeInt(mode.ordinal());
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
        public ListTag patterns;

        public BannerData() {
            baseColor = DyeColor.WHITE;
        }

        public BannerData(ItemStack item) {
            this(getDyeColor(item), getBannerPatternLayers(item));
        }

        public BannerData(DyeColor base, BannerPatternLayers bannerPatterns) {
            baseColor = base;
            ClientLevel level = Minecraft.getInstance().level;
            Optional<Tag> patternsOptional = BannerPatternLayers.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), bannerPatterns).result();
            patternsOptional.ifPresent(tag -> {
                if (tag.getType().equals(ListTag.TYPE)) {
                    patterns = (ListTag) tag.copy();
                }
            });
        }

        private static DyeColor getDyeColor(ItemStack item) {
            if (item.getItem() instanceof BannerItem itemBanner) {
                return itemBanner.getColor();
            }
            return DyeColor.BLACK;
        }

        private static BannerPatternLayers getBannerPatternLayers(ItemStack item) {
            if (item.getComponents().has(DataComponents.BANNER_PATTERNS)) {
                return item.getComponents().get(DataComponents.BANNER_PATTERNS);
            }
            return BannerPatternLayers.EMPTY;
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

    public static class VisibilityData {
        public enum Visibility {
            Frontier,
            AnnounceInChat,
            AnnounceInTitle,
            Fullscreen,
            FullscreenName,
            FullscreenOwner,
            FullscreenDay,
            FullscreenNight,
            FullscreenUnderground,
            FullscreenTopo,
            FullscreenBiome,
            Minimap,
            MinimapName,
            MinimapOwner,
            MinimapDay,
            MinimapNight,
            MinimapUnderground,
            MinimapTopo,
            MinimapBiome,
            Webmap,
            WebmapName,
            WebmapOwner,
            WebmapDay,
            WebmapNight,
            WebmapUnderground,
            WebmapTopo,
            WebmapBiome,
        }

        private final EnumSet<Visibility> values;

        public VisibilityData() {
            values = EnumSet.of(
                    Visibility.Frontier,
                    Visibility.Fullscreen,
                    Visibility.FullscreenName,
                    Visibility.FullscreenDay,
                    Visibility.FullscreenNight,
                    Visibility.FullscreenUnderground,
                    Visibility.FullscreenTopo,
                    Visibility.FullscreenBiome,
                    Visibility.Minimap,
                    Visibility.MinimapName,
                    Visibility.MinimapDay,
                    Visibility.MinimapNight,
                    Visibility.MinimapUnderground,
                    Visibility.MinimapTopo,
                    Visibility.MinimapBiome,
                    Visibility.Webmap,
                    Visibility.WebmapName,
                    Visibility.WebmapDay,
                    Visibility.WebmapNight,
                    Visibility.WebmapUnderground,
                    Visibility.WebmapTopo,
                    Visibility.WebmapBiome
            );
        }

        public VisibilityData(boolean setAll) {
            if (setAll) {
                values = EnumSet.allOf(Visibility.class);
            } else {
                values = EnumSet.noneOf(Visibility.class);
            }
        }

        public VisibilityData(VisibilityData other) {
            values = other.values.clone();
        }

        public int getHash() {
            return values.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (other instanceof VisibilityData otherVisibility) {
                return values.equals(otherVisibility.values);
            }

            return false;
        }

        public void setValue(Visibility value, boolean set) {
            if (set) {
                values.add(value);
            } else {
                values.remove(value);
            }
        }

        public boolean getValue(Visibility value) {
            return values.contains(value);
        }

        public boolean hasSome() {
            return !values.isEmpty();
        }

        public void readFromNBT(CompoundTag nbt, int version) {
            boolean splitVisibility = version >= 10;

            setValue(Visibility.Frontier, nbt.getBoolean("visible"));
            if (splitVisibility)
            {
                setValue(Visibility.Fullscreen, nbt.getBoolean("fullscreenVisible"));
                setValue(Visibility.FullscreenName, nbt.getBoolean("fullscreenNameVisible"));
                setValue(Visibility.FullscreenOwner, nbt.getBoolean("fullscreenOwnerVisible"));
                setValue(Visibility.FullscreenDay, nbt.contains("fullscreenDay") ? nbt.getBoolean("fullscreenDay") : true);
                setValue(Visibility.FullscreenNight, nbt.contains("fullscreenNight") ? nbt.getBoolean("fullscreenNight") : true);
                setValue(Visibility.FullscreenUnderground, nbt.contains("fullscreenUnderground") ? nbt.getBoolean("fullscreenUnderground") : true);
                setValue(Visibility.FullscreenTopo, nbt.contains("fullscreenTopo") ? nbt.getBoolean("fullscreenTopo") : true);
                setValue(Visibility.FullscreenBiome, nbt.contains("fullscreenBiome") ? nbt.getBoolean("fullscreenBiome") : true);
                setValue(Visibility.Minimap, nbt.getBoolean("minimapVisible"));
                setValue(Visibility.MinimapName, nbt.getBoolean("minimapNameVisible"));
                setValue(Visibility.MinimapOwner, nbt.getBoolean("minimapOwnerVisible"));
                setValue(Visibility.MinimapDay, nbt.contains("minimapDay") ? nbt.getBoolean("minimapDay") : true);
                setValue(Visibility.MinimapNight, nbt.contains("minimapNight") ? nbt.getBoolean("minimapNight") : true);
                setValue(Visibility.MinimapUnderground, nbt.contains("minimapUnderground") ? nbt.getBoolean("minimapUnderground") : true);
                setValue(Visibility.MinimapTopo, nbt.contains("minimapTopo") ? nbt.getBoolean("minimapTopo") : true);
                setValue(Visibility.MinimapBiome, nbt.contains("minimapBiome") ? nbt.getBoolean("minimapBiome") : true);
                setValue(Visibility.Webmap, nbt.contains("webmapVisible") ? nbt.getBoolean("webmapVisible") : getValue(Visibility.Minimap));
                setValue(Visibility.WebmapName, nbt.contains("webmapNameVisible") ? nbt.getBoolean("webmapNameVisible") : getValue(Visibility.MinimapName));
                setValue(Visibility.WebmapOwner, nbt.contains("webmapOwnerVisible") ? nbt.getBoolean("webmapOwnerVisible") : getValue(Visibility.MinimapOwner));
                setValue(Visibility.WebmapDay, nbt.contains("webmapDay") ? nbt.getBoolean("webmapDay") : getValue(Visibility.MinimapDay));
                setValue(Visibility.WebmapNight, nbt.contains("webmapNight") ? nbt.getBoolean("webmapNight") : getValue(Visibility.MinimapNight));
                setValue(Visibility.WebmapUnderground, nbt.contains("webmapUnderground") ? nbt.getBoolean("webmapUnderground") : getValue(Visibility.MinimapUnderground));
                setValue(Visibility.WebmapTopo, nbt.contains("webmapTopo") ? nbt.getBoolean("webmapTopo") : getValue(Visibility.MinimapTopo));
                setValue(Visibility.WebmapBiome, nbt.contains("webmapBiome") ? nbt.getBoolean("webmapBiome") : getValue(Visibility.MinimapBiome));
            }
            else
            {
                setValue(Visibility.Fullscreen, nbt.getBoolean("visible"));
                setValue(Visibility.FullscreenName, nbt.getBoolean("visible"));
                setValue(Visibility.FullscreenOwner, nbt.getBoolean("nameVisible"));
                setValue(Visibility.FullscreenDay, nbt.getBoolean("visible"));
                setValue(Visibility.FullscreenNight, nbt.getBoolean("visible"));
                setValue(Visibility.FullscreenUnderground, nbt.getBoolean("visible"));
                setValue(Visibility.FullscreenTopo, nbt.getBoolean("visible"));
                setValue(Visibility.FullscreenBiome, nbt.getBoolean("visible"));
                setValue(Visibility.Minimap, nbt.getBoolean("visible"));
                setValue(Visibility.MinimapName, nbt.getBoolean("nameVisible"));
                setValue(Visibility.MinimapOwner, nbt.getBoolean("ownerVisible"));
                setValue(Visibility.MinimapDay, nbt.getBoolean("visible"));
                setValue(Visibility.MinimapNight, nbt.getBoolean("visible"));
                setValue(Visibility.MinimapUnderground, nbt.getBoolean("visible"));
                setValue(Visibility.MinimapTopo, nbt.getBoolean("visible"));
                setValue(Visibility.MinimapBiome, nbt.getBoolean("visible"));
                setValue(Visibility.Webmap, nbt.getBoolean("visible"));
                setValue(Visibility.WebmapName, nbt.getBoolean("nameVisible"));
                setValue(Visibility.WebmapOwner, nbt.getBoolean("ownerVisible"));
                setValue(Visibility.WebmapDay, nbt.getBoolean("visible"));
                setValue(Visibility.WebmapNight, nbt.getBoolean("visible"));
                setValue(Visibility.WebmapUnderground, nbt.getBoolean("visible"));
                setValue(Visibility.WebmapTopo, nbt.getBoolean("visible"));
                setValue(Visibility.WebmapBiome, nbt.getBoolean("visible"));
            }

            if (nbt.contains("announceInChat")) {
                setValue(Visibility.AnnounceInChat, nbt.getBoolean("announceInChat"));
            }
            if (nbt.contains("announceInTitle")) {
                setValue(Visibility.AnnounceInTitle, nbt.getBoolean("announceInTitle"));
            }
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putBoolean("visible", getValue(Visibility.Frontier));
            nbt.putBoolean("announceInChat", getValue(Visibility.AnnounceInChat));
            nbt.putBoolean("announceInTitle", getValue(Visibility.AnnounceInTitle));
            nbt.putBoolean("fullscreenVisible", getValue(Visibility.Fullscreen));
            nbt.putBoolean("fullscreenNameVisible", getValue(Visibility.FullscreenName));
            nbt.putBoolean("fullscreenOwnerVisible", getValue(Visibility.FullscreenOwner));
            nbt.putBoolean("fullscreenDay", getValue(Visibility.FullscreenDay));
            nbt.putBoolean("fullscreenNight", getValue(Visibility.FullscreenNight));
            nbt.putBoolean("fullscreenUnderground", getValue(Visibility.FullscreenUnderground));
            nbt.putBoolean("fullscreenTopo", getValue(Visibility.FullscreenTopo));
            nbt.putBoolean("fullscreenBiome", getValue(Visibility.FullscreenBiome));
            nbt.putBoolean("minimapVisible", getValue(Visibility.Minimap));
            nbt.putBoolean("minimapNameVisible", getValue(Visibility.MinimapName));
            nbt.putBoolean("minimapOwnerVisible", getValue(Visibility.MinimapOwner));
            nbt.putBoolean("minimapDay", getValue(Visibility.MinimapDay));
            nbt.putBoolean("minimapNight", getValue(Visibility.MinimapNight));
            nbt.putBoolean("minimapUnderground", getValue(Visibility.MinimapUnderground));
            nbt.putBoolean("minimapTopo", getValue(Visibility.MinimapTopo));
            nbt.putBoolean("minimapBiome", getValue(Visibility.MinimapBiome));
            nbt.putBoolean("webmapVisible", getValue(Visibility.Webmap));
            nbt.putBoolean("webmapNameVisible", getValue(Visibility.WebmapName));
            nbt.putBoolean("webmapOwnerVisible", getValue(Visibility.WebmapOwner));
            nbt.putBoolean("webmapDay", getValue(Visibility.WebmapDay));
            nbt.putBoolean("webmapNight", getValue(Visibility.WebmapNight));
            nbt.putBoolean("webmapUnderground", getValue(Visibility.WebmapUnderground));
            nbt.putBoolean("webmapTopo", getValue(Visibility.WebmapTopo));
            nbt.putBoolean("webmapBiome", getValue(Visibility.WebmapBiome));
        }

        public void fromBytes(FriendlyByteBuf buf) {
            setValue(Visibility.Frontier, buf.readBoolean());
            setValue(Visibility.AnnounceInChat, buf.readBoolean());
            setValue(Visibility.AnnounceInTitle, buf.readBoolean());
            setValue(Visibility.Fullscreen, buf.readBoolean());
            setValue(Visibility.FullscreenName, buf.readBoolean());
            setValue(Visibility.FullscreenOwner, buf.readBoolean());
            setValue(Visibility.FullscreenDay, buf.readBoolean());
            setValue(Visibility.FullscreenNight, buf.readBoolean());
            setValue(Visibility.FullscreenUnderground, buf.readBoolean());
            setValue(Visibility.FullscreenTopo, buf.readBoolean());
            setValue(Visibility.FullscreenBiome, buf.readBoolean());
            setValue(Visibility.Minimap, buf.readBoolean());
            setValue(Visibility.MinimapName, buf.readBoolean());
            setValue(Visibility.MinimapOwner, buf.readBoolean());
            setValue(Visibility.MinimapDay, buf.readBoolean());
            setValue(Visibility.MinimapNight, buf.readBoolean());
            setValue(Visibility.MinimapUnderground, buf.readBoolean());
            setValue(Visibility.MinimapTopo, buf.readBoolean());
            setValue(Visibility.MinimapBiome, buf.readBoolean());
            setValue(Visibility.Webmap, buf.readBoolean());
            setValue(Visibility.WebmapName, buf.readBoolean());
            setValue(Visibility.WebmapOwner, buf.readBoolean());
            setValue(Visibility.WebmapDay, buf.readBoolean());
            setValue(Visibility.WebmapNight, buf.readBoolean());
            setValue(Visibility.WebmapUnderground, buf.readBoolean());
            setValue(Visibility.WebmapTopo, buf.readBoolean());
            setValue(Visibility.WebmapBiome, buf.readBoolean());
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeBoolean(getValue(Visibility.Frontier));
            buf.writeBoolean(getValue(Visibility.AnnounceInChat));
            buf.writeBoolean(getValue(Visibility.AnnounceInTitle));
            buf.writeBoolean(getValue(Visibility.Fullscreen));
            buf.writeBoolean(getValue(Visibility.FullscreenName));
            buf.writeBoolean(getValue(Visibility.FullscreenOwner));
            buf.writeBoolean(getValue(Visibility.FullscreenDay));
            buf.writeBoolean(getValue(Visibility.FullscreenNight));
            buf.writeBoolean(getValue(Visibility.FullscreenUnderground));
            buf.writeBoolean(getValue(Visibility.FullscreenTopo));
            buf.writeBoolean(getValue(Visibility.FullscreenBiome));
            buf.writeBoolean(getValue(Visibility.Minimap));
            buf.writeBoolean(getValue(Visibility.MinimapName));
            buf.writeBoolean(getValue(Visibility.MinimapOwner));
            buf.writeBoolean(getValue(Visibility.MinimapDay));
            buf.writeBoolean(getValue(Visibility.MinimapNight));
            buf.writeBoolean(getValue(Visibility.MinimapUnderground));
            buf.writeBoolean(getValue(Visibility.MinimapTopo));
            buf.writeBoolean(getValue(Visibility.MinimapBiome));
            buf.writeBoolean(getValue(Visibility.Webmap));
            buf.writeBoolean(getValue(Visibility.WebmapName));
            buf.writeBoolean(getValue(Visibility.WebmapOwner));
            buf.writeBoolean(getValue(Visibility.WebmapDay));
            buf.writeBoolean(getValue(Visibility.WebmapNight));
            buf.writeBoolean(getValue(Visibility.WebmapUnderground));
            buf.writeBoolean(getValue(Visibility.WebmapTopo));
            buf.writeBoolean(getValue(Visibility.WebmapBiome));
        }
    }
}
