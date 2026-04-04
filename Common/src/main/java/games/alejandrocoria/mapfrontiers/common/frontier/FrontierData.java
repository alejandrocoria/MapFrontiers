package games.alejandrocoria.mapfrontiers.common.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public class FrontierData {
    public enum Mode {
        Vertex, Chunk
    }

    public enum FrontierLifetime {
        PERSISTENT, SESSION_ONLY
    }

    protected UUID id;
    protected final List<BlockPos> vertices = new ArrayList<>();
    protected final Set<ChunkPos> chunks = new HashSet<>();
    protected Mode mode = Mode.Vertex;
    protected String name1 = "New";
    protected String name2 = "Frontier";
    protected VisibilityData visibilityData;
    protected int color = 0xffffffff;
    protected ResourceKey<Level> dimension;
    protected SettingsUser owner = new SettingsUser();
    protected BannerData banner;
    protected boolean personal = false;
    protected FrontierLifetime lifetime = FrontierLifetime.PERSISTENT;
    protected List<SettingsUserShared> usersShared;
    protected CopiedFrom copiedFrom;
    protected @Nullable String sourcePluginId;
    protected Date created;
    protected Date modified;

    public FrontierData() {
        id = new UUID(0, 0);
        visibilityData = new VisibilityData();
    }

    public FrontierData(FrontierData other) {
        id = other.id;
        dimension = other.dimension;
        owner = other.owner;
        personal = other.personal;
        lifetime = other.lifetime;

        visibilityData = new VisibilityData(other.visibilityData);
        color = other.color;

        name1 = other.name1;
        name2 = other.name2;

        if (other.banner == null) {
            banner = null;
        } else {
            banner = new BannerData(other.banner);
        }

        usersShared = other.usersShared;

        vertices.clear();
        vertices.addAll(other.vertices);
        chunks.clear();
        chunks.addAll(other.chunks);
        mode = other.mode;

        copiedFrom = other.copiedFrom;
        sourcePluginId = other.sourcePluginId;

        created = other.created;
        modified = other.modified;

        validateTypeAndLifetime(personal, lifetime);
        sanitizeSharedUsers();
    }

    public void updateFromData(FrontierData other) {
        if (other == this) {
            return;
        }

        id = other.id;
        dimension = other.dimension;
        owner = other.owner;
        personal = other.personal;
        lifetime = other.lifetime;
        visibilityData = new VisibilityData(other.visibilityData);
        color = other.color;
        name1 = other.name1;
        name2 = other.name2;
        banner = other.banner == null ? null : new BannerData(other.banner);
        usersShared = other.usersShared;
        vertices.clear();
        vertices.addAll(other.vertices);
        chunks.clear();
        chunks.addAll(other.chunks);
        mode = other.mode;

        copiedFrom = other.copiedFrom;
        sourcePluginId = other.sourcePluginId;
        created = other.created;

        modified = other.modified;

        validateTypeAndLifetime(personal, lifetime);
        sanitizeSharedUsers();
    }

    public void applyChange(FrontierChange change) {
        if (change.hasVisibilityChange()) {
            visibilityData = change.getVisibility().getVisibilityData();
        }

        if (change.hasColorChange()) {
            color = change.getColor().getColor();
        }

        if (change.hasNameChange()) {
            name1 = change.getName().getName1();
            name2 = change.getName().getName2();
        }

        if (change.hasBannerChange()) {
            FrontierData.BannerData bannerData = change.getBanner().getBanner();
            banner = bannerData == null ? null : new BannerData(bannerData);
        }

        if (change.hasShapeChange()) {
            FrontierChange.ShapeChange shapeChange = change.getShape();
            vertices.clear();
            vertices.addAll(shapeChange.getVertices());
            chunks.clear();
            chunks.addAll(shapeChange.getChunks());
            mode = shapeChange.getMode();
        }

        if (change.hasModifiedTime()) {
            modified = new Date(change.getModifiedTime());
        }
    }

    public void applySharingChange(FrontierSharingChange sharingChange) {
        usersShared = sharingChange.getUsersShared();
        sanitizeSharedUsers();
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

    public List<BlockPos> getVertices() {
        synchronized (vertices) {
            return new ArrayList<>(vertices);
        }
    }

    public void clearVertices() {
        synchronized (vertices) {
            vertices.clear();
        }
    }

    protected void addVertex(BlockPos pos, int index) {
        synchronized (vertices) {
            vertices.add(index, pos.atY(70));
        }
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
    }

    protected void moveVertex(BlockPos pos, int index) {
        if (index < 0 || index >= vertices.size()) {
            return;
        }

        synchronized (vertices) {
            vertices.set(index, pos);
        }
    }

    public void moveAllVertices(BlockPos delta) {
        synchronized (vertices) {
            vertices.replaceAll(blockPos -> blockPos.offset(delta));
        }
    }

    public boolean toggleChunk(ChunkPos chunk) {
        boolean added = false;
        synchronized (chunks) {
            if (!chunks.remove(chunk)) {
                chunks.add(chunk);
                added = true;
            }
        }
        return added;
    }

    public boolean addChunk(ChunkPos chunk) {
        synchronized (chunks) {
            if (chunks.add(chunk)) {
                return true;
            }
        }

        return false;
    }

    public boolean removeChunk(ChunkPos chunk) {
        synchronized (chunks) {
            if (chunks.remove(chunk)) {
                return true;
            }
        }

        return false;
    }

    public int getChunkCount() {
        return chunks.size();
    }

    public Set<ChunkPos> getChunks() {
        synchronized (chunks) {
            return new HashSet<>(chunks);
        }
    }

    public void clearChunks() {
        synchronized (chunks) {
            chunks.clear();
        }
    }

    public void moveAllChunks(ChunkPos delta) {
        synchronized (chunks) {
            Set<ChunkPos> movedChunks = chunks.stream()
                    .map(chunk -> new ChunkPos(chunk.x + delta.x, chunk.z + delta.z))
                    .collect(Collectors.toSet());
            chunks.clear();
            chunks.addAll(movedChunks);
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
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

    public boolean isNamed() {
        return !StringUtils.isBlank(name1) || !StringUtils.isBlank(name2);
    }

    public void setVisibility(VisibilityData.Visibility visibility, boolean enable) {
        this.visibilityData.setValue(visibility, enable);
    }

    public void toggleVisibility(VisibilityData.Visibility visibility) {
        this.visibilityData.setValue(visibility, !this.visibilityData.getValue(visibility));
    }

    public boolean getVisibility(VisibilityData.Visibility visibility) {
        return visibilityData.getValue(visibility);
    }

    public void setVisibilityData(VisibilityData visibilityData) {
        this.visibilityData = visibilityData;
    }

    public VisibilityData getVisibilityData() {
        return visibilityData;
    }

    public void setColor(int color) {
        this.color = color;
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
        if (itemBanner == null) {
            banner = null;
        } else {
            banner = new BannerData(itemBanner);
        }
    }

    public void setBanner(DyeColor base, BannerPatternLayers bannerPatterns) {
        banner = new BannerData(base, bannerPatterns);
    }

    public boolean hasBanner() {
        return banner != null;
    }

    public void setBannerData(@Nullable BannerData bannerData) {
        if (bannerData == null) {
            banner = null;
        } else {
            banner = new BannerData(bannerData);
        }
    }

    public BannerData getbannerData() {
        return banner;
    }

    public void setBannerRotation(int rotation) {
        banner.rotation = rotation;
    }

    public int getBannerRotation() {
        if (banner == null) {
            return 0;
        }
        return banner.rotation;
    }

    public void setPersonal(boolean personal) {
        validateTypeAndLifetime(personal, lifetime);
        this.personal = personal;
        sanitizeSharedUsers();
    }

    public boolean getPersonal() {
        return personal;
    }

    public void setLifetime(FrontierLifetime lifetime) {
        FrontierLifetime checkedLifetime = Objects.requireNonNull(lifetime, "lifetime");
        validateTypeAndLifetime(personal, checkedLifetime);
        this.lifetime = checkedLifetime;
        sanitizeSharedUsers();
    }

    public FrontierLifetime getLifetime() {
        return lifetime;
    }

    public boolean isPersistent() {
        return lifetime == FrontierLifetime.PERSISTENT;
    }

    public boolean isSessionOnly() {
        return lifetime == FrontierLifetime.SESSION_ONLY;
    }

    public void addUserShared(SettingsUserShared userShared) {
        if (!canHaveSharedUsers()) {
            return;
        }

        if (usersShared == null) {
            usersShared = new ArrayList<>();
        }

        usersShared.add(userShared);
    }

    public void removeUserShared(SettingsUser user) {
        if (usersShared == null) {
            return;
        }

        usersShared.removeIf(x -> x.getUser().equals(user));
    }

    public void removeAllUserShared() {
        if (usersShared == null) {
            return;
        }

        usersShared = null;
    }

    public void removePendingUsersShared() {
        if (usersShared == null) {
            return;
        }

        usersShared.removeIf(SettingsUserShared::isPending);
        sanitizeSharedUsers();
    }

    public List<SettingsUserShared> getUsersShared() {
        if (!canHaveSharedUsers()) {
            return null;
        }
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

    public boolean wasCopied() {
        return copiedFrom != null;
    }

    public void removeCopiedFromInfo() {
        copiedFrom = null;
    }

    public void setCopiedFromId(UUID id) {
        if (!wasCopied()) {
            copiedFrom = new CopiedFrom();
        }
        copiedFrom.id = id;
    }

    public UUID getCopiedFromId() {
        if (copiedFrom == null) {
            return id;
        }
        return copiedFrom.id;
    }

    public void setCopiedFromUser(SettingsUser user) {
        if (!wasCopied()) {
            copiedFrom = new CopiedFrom();
        }
        copiedFrom.user = user;
    }

    public SettingsUser getCopiedFromUser() {
        if (copiedFrom == null) {
            return owner;
        }
        return copiedFrom.user;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public Date getModified() {
        return modified;
    }

    public void setSourcePluginId(@Nullable String sourcePluginId) {
        this.sourcePluginId = sourcePluginId;
    }

    public @Nullable String getSourcePluginId() {
        return sourcePluginId;
    }

    public void readFromNBT(CompoundTag nbt, int version) {
        id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));
        color = NbtReadHelper.requireInt(nbt, "color");
        dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(NbtReadHelper.requireString(nbt, "dimension")));
        name1 = nbt.getStringOr("name1", "");
        name2 = nbt.getStringOr("name2", "");

        visibilityData.readFromNBT(nbt, version);

        personal = nbt.getBooleanOr("personal", true);
        lifetime = readLifetimeFromNbt(nbt);
        try {
            validateTypeAndLifetime(personal, lifetime);
        } catch (IllegalArgumentException e) {
            throw new InvalidNbtFormatException("Invalid lifetime for frontier " + id + ": " + e.getMessage(), e);
        }
        sourcePluginId = nbt.getStringOr("sourcePluginId", null);

        owner = new SettingsUser();
        owner.readFromNBT(nbt.getCompoundOrEmpty("owner"));

        if (nbt.contains("banner")) {
            banner = new BannerData();
            banner.readFromNBT(NbtReadHelper.requireCompound(nbt, "banner"));
        }

        if (personal) {
            ListTag usersSharedTagList = nbt.getListOrEmpty("usersShared");
            if (!usersSharedTagList.isEmpty()) {
                usersShared = new ArrayList<>();

                for (int i = 0; i < usersSharedTagList.size(); ++i) {
                    try {
                        SettingsUserShared userShared = new SettingsUserShared();
                        userShared.readFromNBT(NbtReadHelper.requireCompound(usersSharedTagList, i, "usersShared"));
                        usersShared.add(userShared);
                    } catch (InvalidNbtFormatException e) {
                        throw new InvalidNbtFormatException("Invalid shared user at usersShared[" + i + "] for frontier " + id + ": "
                                + e.getMessage(), e);
                    }
                }
            }
        }

        ListTag verticesTagList = nbt.getListOrEmpty("vertices");
        for (int i = 0; i < verticesTagList.size(); ++i) {
            try {
                CompoundTag posTag = NbtReadHelper.requireCompound(verticesTagList, i, "vertices");
                vertices.add(new BlockPos(NbtReadHelper.requireInt(posTag, "X"), 70, NbtReadHelper.requireInt(posTag, "Z")));
            } catch (InvalidNbtFormatException e) {
                throw new InvalidNbtFormatException("Invalid vertex at vertices[" + i + "] for frontier " + id + ": " + e.getMessage(), e);
            }
        }

        ListTag chunksTagList = nbt.getListOrEmpty("chunks");
        for (int i = 0; i < chunksTagList.size(); ++i) {
            try {
                CompoundTag posTag = NbtReadHelper.requireCompound(chunksTagList, i, "chunks");
                chunks.add(new ChunkPos(NbtReadHelper.requireInt(posTag, "X"), NbtReadHelper.requireInt(posTag, "Z")));
            } catch (InvalidNbtFormatException e) {
                throw new InvalidNbtFormatException("Invalid chunk at chunks[" + i + "] for frontier " + id + ": " + e.getMessage(), e);
            }
        }

        String modeTag = nbt.getStringOr("mode", "");
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

        if (nbt.contains("copiedFrom")) {
            copiedFrom = new CopiedFrom();
            copiedFrom.readFromNBT(NbtReadHelper.requireCompound(nbt, "copiedFrom"), version);
        }

        if (nbt.contains("created")) {
            created = new Date(NbtReadHelper.requireLong(nbt, "created"));
        }

        if (nbt.contains("modified")) {
            modified = new Date(NbtReadHelper.requireLong(nbt, "modified"));
        }

        sanitizeSharedUsers();
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("id", id.toString());
        nbt.putInt("color", color);
        nbt.putString("dimension", dimension.identifier().toString());
        nbt.putString("name1", name1);
        nbt.putString("name2", name2);
        visibilityData.writeToNBT(nbt);
        nbt.putBoolean("personal", personal);
        nbt.putString("lifetime", lifetime.name());
        if (sourcePluginId != null) {
            nbt.putString("sourcePluginId", sourcePluginId);
        }

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

        if (wasCopied()) {
            CompoundTag nbtCopiedFrom = new CompoundTag();
            copiedFrom.writeToNBT(nbtCopiedFrom);
            nbt.put("copiedFrom", nbtCopiedFrom);
        }

        if (created != null) {
            nbt.putLong("created", created.getTime());
        }

        if (modified != null) {
            nbt.putLong("modified", modified.getTime());
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        id = UUIDHelper.fromBytes(buf);
        dimension = ResourceKey.create(Registries.DIMENSION, buf.readIdentifier());
        personal = buf.readBoolean();
        lifetime = readLifetimeFromBytes(buf);
        validateTypeAndLifetime(personal, lifetime);
        if (buf.readBoolean()) {
            sourcePluginId = buf.readUtf();
        } else {
            sourcePluginId = null;
        }
        owner = new SettingsUser();
        owner.fromBytes(buf);
        visibilityData.fromBytes(buf);
        color = buf.readInt();

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

        if (buf.readBoolean()) {
            banner = new BannerData();
            banner.fromBytes(buf);
        } else {
            banner = null;
        }

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

        if (buf.readBoolean()) {
            copiedFrom = new CopiedFrom();
            copiedFrom.fromBytes(buf);
        } else {
            copiedFrom = null;
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

        sanitizeSharedUsers();
    }

    public void toBytes(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, id);
        buf.writeIdentifier(dimension.identifier());
        buf.writeBoolean(personal);
        buf.writeInt(lifetime.ordinal());
        if (sourcePluginId == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeUtf(sourcePluginId);
        }
        owner.toBytes(buf);
        visibilityData.toBytes(buf);
        buf.writeInt(color);

        int maxCharacters = 17;
        int maxBytes = maxCharacters * 4;
        buf.writeUtf(name1, maxBytes);
        buf.writeUtf(name2, maxBytes);

        if (banner == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            banner.toBytes(buf);
        }

        if (personal && usersShared != null) {
            buf.writeBoolean(true);

            buf.writeInt(usersShared.size());
            for (SettingsUserShared userShared : usersShared) {
                userShared.toBytes(buf);
            }
        } else {
            buf.writeBoolean(false);
        }

        buf.writeInt(vertices.size());
        for (BlockPos pos : vertices) {
            buf.writeLong(pos.asLong());
        }

        buf.writeInt(chunks.size());
        for (ChunkPos pos : chunks) {
            buf.writeLong(pos.toLong());
        }

        buf.writeInt(mode.ordinal());

        if (wasCopied()) {
            buf.writeBoolean(true);
            copiedFrom.toBytes(buf);
        } else {
            buf.writeBoolean(false);
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

    private static void validateTypeAndLifetime(boolean personal, FrontierLifetime lifetime) {
        if (!personal && lifetime == FrontierLifetime.SESSION_ONLY) {
            throw new IllegalArgumentException("SESSION_ONLY frontiers must be personal");
        }
    }

    private boolean canHaveSharedUsers() {
        return personal && isPersistent();
    }

    private void sanitizeSharedUsers() {
        if (!canHaveSharedUsers()) {
            usersShared = null;
        }
    }

    private static FrontierLifetime readLifetimeFromNbt(CompoundTag nbt) {
        String lifetimeTag = nbt.getStringOr("lifetime", "");
        if (lifetimeTag.isEmpty()) {
            return FrontierLifetime.PERSISTENT;
        }

        try {
            return FrontierLifetime.valueOf(lifetimeTag);
        } catch (IllegalArgumentException e) {
            String availableLifetimes = StringHelper.enumValuesToString(Arrays.asList(FrontierLifetime.values()));
            MapFrontiers.LOGGER.warn("Unknown lifetime in frontier {}. Found: \"{}\". Expected: {}", idFromTag(nbt), lifetimeTag, availableLifetimes);
            return FrontierLifetime.PERSISTENT;
        }
    }

    private static FrontierLifetime readLifetimeFromBytes(FriendlyByteBuf buf) {
        int lifetimeOrdinal = buf.readInt();
        FrontierLifetime[] values = FrontierLifetime.values();
        if (lifetimeOrdinal < 0 || lifetimeOrdinal >= values.length) {
            MapFrontiers.LOGGER.warn("Unknown lifetime ordinal in frontier packet. Found: {}. Defaulting to {}", lifetimeOrdinal,
                    FrontierLifetime.PERSISTENT);
            return FrontierLifetime.PERSISTENT;
        }

        return values[lifetimeOrdinal];
    }

    private static String idFromTag(CompoundTag nbt) {
        return nbt.getStringOr("id", "<unknown>");
    }


    public static class BannerData {
        public DyeColor baseColor;
        public ListTag patterns;
        public int rotation;

        public static @Nullable ListTag normalizePatterns(@Nullable ListTag patterns) {
            if (patterns == null || patterns.isEmpty()) {
                return null;
            }

            return patterns;
        }

        public BannerData() {
            baseColor = DyeColor.WHITE;
            rotation = 0;
        }

        public BannerData(BannerData other) {
            baseColor = other.baseColor;
            patterns = normalizePatterns(other.patterns == null ? null : other.patterns.copy());
            rotation = other.rotation;
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
                    patterns = normalizePatterns((ListTag) tag.copy());
                }
            });
            rotation = 0;
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
            baseColor = DyeColor.byId(NbtReadHelper.requireInt(nbt, "Base"));
            patterns = normalizePatterns(nbt.getListOrEmpty("Patterns"));
            rotation = nbt.getIntOr("Rotation", 0);
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putInt("Base", baseColor.getId());

            if (patterns != null) {
                nbt.put("Patterns", patterns);
            }

            nbt.putInt("Rotation", rotation);
        }

        public void fromBytes(FriendlyByteBuf buf) {
            baseColor = DyeColor.byId(buf.readInt());

            CompoundTag nbt = buf.readNbt();
            if (nbt != null) {
                patterns = normalizePatterns(nbt.getListOrEmpty("Patterns"));
            }

            rotation = buf.readInt();
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

            buf.writeInt(rotation);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BannerData that)) {
                return false;
            }
            return rotation == that.rotation && baseColor == that.baseColor && Objects.equals(patterns, that.patterns);
        }

        @Override
        public int hashCode() {
            return Objects.hash(baseColor, patterns, rotation);
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
            FullscreenBanner,
            FullscreenDay,
            FullscreenNight,
            FullscreenUnderground,
            FullscreenTopo,
            FullscreenBiome,
            Minimap,
            MinimapName,
            MinimapOwner,
            MinimapBanner,
            MinimapDay,
            MinimapNight,
            MinimapUnderground,
            MinimapTopo,
            MinimapBiome,
            Webmap,
            WebmapName,
            WebmapOwner,
            WebmapBanner,
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

        @Override
        public int hashCode() {
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

            setValue(Visibility.Frontier, NbtReadHelper.requireBoolean(nbt, "visible"));
            if (splitVisibility)
            {
                setValue(Visibility.Fullscreen, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenVisible", true));
                setValue(Visibility.FullscreenName, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenNameVisible", true));
                setValue(Visibility.FullscreenOwner, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenOwnerVisible", false));
                setValue(Visibility.FullscreenBanner, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenBannerVisible", false));
                setValue(Visibility.FullscreenDay, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenDay", true));
                setValue(Visibility.FullscreenNight, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenNight", true));
                setValue(Visibility.FullscreenUnderground, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenUnderground", true));
                setValue(Visibility.FullscreenTopo, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenTopo", true));
                setValue(Visibility.FullscreenBiome, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenBiome", true));
                setValue(Visibility.Minimap, NbtReadHelper.getBooleanOrDefault(nbt, "minimapVisible", true));
                setValue(Visibility.MinimapName, NbtReadHelper.getBooleanOrDefault(nbt, "minimapNameVisible", true));
                setValue(Visibility.MinimapOwner, NbtReadHelper.getBooleanOrDefault(nbt, "minimapOwnerVisible", false));
                setValue(Visibility.MinimapBanner, NbtReadHelper.getBooleanOrDefault(nbt, "minimapBannerVisible", false));
                setValue(Visibility.MinimapDay, NbtReadHelper.getBooleanOrDefault(nbt, "minimapDay", true));
                setValue(Visibility.MinimapNight, NbtReadHelper.getBooleanOrDefault(nbt, "minimapNight", true));
                setValue(Visibility.MinimapUnderground, NbtReadHelper.getBooleanOrDefault(nbt, "minimapUnderground", true));
                setValue(Visibility.MinimapTopo, NbtReadHelper.getBooleanOrDefault(nbt, "minimapTopo", true));
                setValue(Visibility.MinimapBiome, NbtReadHelper.getBooleanOrDefault(nbt, "minimapBiome", true));
                setValue(Visibility.Webmap, NbtReadHelper.getBooleanOrDefault(nbt, "webmapVisible", getValue(Visibility.Minimap)));
                setValue(Visibility.WebmapName, NbtReadHelper.getBooleanOrDefault(nbt, "webmapNameVisible", getValue(Visibility.MinimapName)));
                setValue(Visibility.WebmapOwner, NbtReadHelper.getBooleanOrDefault(nbt, "webmapOwnerVisible", getValue(Visibility.MinimapOwner)));
                setValue(Visibility.WebmapBanner, NbtReadHelper.getBooleanOrDefault(nbt, "webmapBannerVisible", getValue(Visibility.MinimapBanner)));
                setValue(Visibility.WebmapDay, NbtReadHelper.getBooleanOrDefault(nbt, "webmapDay", getValue(Visibility.MinimapDay)));
                setValue(Visibility.WebmapNight, NbtReadHelper.getBooleanOrDefault(nbt, "webmapNight", getValue(Visibility.MinimapNight)));
                setValue(Visibility.WebmapUnderground, NbtReadHelper.getBooleanOrDefault(nbt, "webmapUnderground", getValue(Visibility.MinimapUnderground)));
                setValue(Visibility.WebmapTopo, NbtReadHelper.getBooleanOrDefault(nbt, "webmapTopo", getValue(Visibility.MinimapTopo)));
                setValue(Visibility.WebmapBiome, NbtReadHelper.getBooleanOrDefault(nbt, "webmapBiome", getValue(Visibility.MinimapBiome)));
            }
            else
            {
                setValue(Visibility.Fullscreen, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.FullscreenName, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.FullscreenOwner, NbtReadHelper.requireBoolean(nbt, "nameVisible"));
                setValue(Visibility.FullscreenBanner, false);
                setValue(Visibility.FullscreenDay, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.FullscreenNight, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.FullscreenUnderground, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.FullscreenTopo, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.FullscreenBiome, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.Minimap, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.MinimapName, NbtReadHelper.requireBoolean(nbt, "nameVisible"));
                setValue(Visibility.MinimapOwner, NbtReadHelper.requireBoolean(nbt, "ownerVisible"));
                setValue(Visibility.MinimapBanner, false);
                setValue(Visibility.MinimapDay, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.MinimapNight, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.MinimapUnderground, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.MinimapTopo, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.MinimapBiome, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.Webmap, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.WebmapName, NbtReadHelper.requireBoolean(nbt, "nameVisible"));
                setValue(Visibility.WebmapOwner, NbtReadHelper.requireBoolean(nbt, "ownerVisible"));
                setValue(Visibility.WebmapBanner, false);
                setValue(Visibility.WebmapDay, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.WebmapNight, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.WebmapUnderground, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.WebmapTopo, NbtReadHelper.requireBoolean(nbt, "visible"));
                setValue(Visibility.WebmapBiome, NbtReadHelper.requireBoolean(nbt, "visible"));
            }

            setValue(Visibility.AnnounceInChat, NbtReadHelper.getBooleanOrDefault(nbt, "announceInChat", false));
            setValue(Visibility.AnnounceInTitle, NbtReadHelper.getBooleanOrDefault(nbt, "announceInTitle", false));
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putBoolean("visible", getValue(Visibility.Frontier));
            nbt.putBoolean("announceInChat", getValue(Visibility.AnnounceInChat));
            nbt.putBoolean("announceInTitle", getValue(Visibility.AnnounceInTitle));
            nbt.putBoolean("fullscreenVisible", getValue(Visibility.Fullscreen));
            nbt.putBoolean("fullscreenNameVisible", getValue(Visibility.FullscreenName));
            nbt.putBoolean("fullscreenOwnerVisible", getValue(Visibility.FullscreenOwner));
            nbt.putBoolean("fullscreenBannerVisible", getValue(Visibility.FullscreenBanner));
            nbt.putBoolean("fullscreenDay", getValue(Visibility.FullscreenDay));
            nbt.putBoolean("fullscreenNight", getValue(Visibility.FullscreenNight));
            nbt.putBoolean("fullscreenUnderground", getValue(Visibility.FullscreenUnderground));
            nbt.putBoolean("fullscreenTopo", getValue(Visibility.FullscreenTopo));
            nbt.putBoolean("fullscreenBiome", getValue(Visibility.FullscreenBiome));
            nbt.putBoolean("minimapVisible", getValue(Visibility.Minimap));
            nbt.putBoolean("minimapNameVisible", getValue(Visibility.MinimapName));
            nbt.putBoolean("minimapOwnerVisible", getValue(Visibility.MinimapOwner));
            nbt.putBoolean("minimapBannerVisible", getValue(Visibility.MinimapBanner));
            nbt.putBoolean("minimapDay", getValue(Visibility.MinimapDay));
            nbt.putBoolean("minimapNight", getValue(Visibility.MinimapNight));
            nbt.putBoolean("minimapUnderground", getValue(Visibility.MinimapUnderground));
            nbt.putBoolean("minimapTopo", getValue(Visibility.MinimapTopo));
            nbt.putBoolean("minimapBiome", getValue(Visibility.MinimapBiome));
            nbt.putBoolean("webmapVisible", getValue(Visibility.Webmap));
            nbt.putBoolean("webmapNameVisible", getValue(Visibility.WebmapName));
            nbt.putBoolean("webmapOwnerVisible", getValue(Visibility.WebmapOwner));
            nbt.putBoolean("webmapBannerVisible", getValue(Visibility.WebmapBanner));
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
            setValue(Visibility.FullscreenBanner, buf.readBoolean());
            setValue(Visibility.FullscreenDay, buf.readBoolean());
            setValue(Visibility.FullscreenNight, buf.readBoolean());
            setValue(Visibility.FullscreenUnderground, buf.readBoolean());
            setValue(Visibility.FullscreenTopo, buf.readBoolean());
            setValue(Visibility.FullscreenBiome, buf.readBoolean());
            setValue(Visibility.Minimap, buf.readBoolean());
            setValue(Visibility.MinimapName, buf.readBoolean());
            setValue(Visibility.MinimapOwner, buf.readBoolean());
            setValue(Visibility.MinimapBanner, buf.readBoolean());
            setValue(Visibility.MinimapDay, buf.readBoolean());
            setValue(Visibility.MinimapNight, buf.readBoolean());
            setValue(Visibility.MinimapUnderground, buf.readBoolean());
            setValue(Visibility.MinimapTopo, buf.readBoolean());
            setValue(Visibility.MinimapBiome, buf.readBoolean());
            setValue(Visibility.Webmap, buf.readBoolean());
            setValue(Visibility.WebmapName, buf.readBoolean());
            setValue(Visibility.WebmapOwner, buf.readBoolean());
            setValue(Visibility.WebmapBanner, buf.readBoolean());
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
            buf.writeBoolean(getValue(Visibility.FullscreenBanner));
            buf.writeBoolean(getValue(Visibility.FullscreenDay));
            buf.writeBoolean(getValue(Visibility.FullscreenNight));
            buf.writeBoolean(getValue(Visibility.FullscreenUnderground));
            buf.writeBoolean(getValue(Visibility.FullscreenTopo));
            buf.writeBoolean(getValue(Visibility.FullscreenBiome));
            buf.writeBoolean(getValue(Visibility.Minimap));
            buf.writeBoolean(getValue(Visibility.MinimapName));
            buf.writeBoolean(getValue(Visibility.MinimapOwner));
            buf.writeBoolean(getValue(Visibility.MinimapBanner));
            buf.writeBoolean(getValue(Visibility.MinimapDay));
            buf.writeBoolean(getValue(Visibility.MinimapNight));
            buf.writeBoolean(getValue(Visibility.MinimapUnderground));
            buf.writeBoolean(getValue(Visibility.MinimapTopo));
            buf.writeBoolean(getValue(Visibility.MinimapBiome));
            buf.writeBoolean(getValue(Visibility.Webmap));
            buf.writeBoolean(getValue(Visibility.WebmapName));
            buf.writeBoolean(getValue(Visibility.WebmapOwner));
            buf.writeBoolean(getValue(Visibility.WebmapBanner));
            buf.writeBoolean(getValue(Visibility.WebmapDay));
            buf.writeBoolean(getValue(Visibility.WebmapNight));
            buf.writeBoolean(getValue(Visibility.WebmapUnderground));
            buf.writeBoolean(getValue(Visibility.WebmapTopo));
            buf.writeBoolean(getValue(Visibility.WebmapBiome));
        }
    }

    public static class CopiedFrom {
        protected UUID id;
        protected SettingsUser user = new SettingsUser();

        public void readFromNBT(CompoundTag nbt, int version) {
            id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));

            user = new SettingsUser();
            user.readFromNBT(nbt.getCompoundOrEmpty("user"));
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, user);
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putString("id", id.toString());

            CompoundTag nbtOwner = new CompoundTag();
            user.writeToNBT(nbtOwner);
            nbt.put("user", nbtOwner);
        }

        public void fromBytes(FriendlyByteBuf buf) {
            id = UUIDHelper.fromBytes(buf);

            user = new SettingsUser();
            user.fromBytes(buf);
        }

        public void toBytes(FriendlyByteBuf buf) {
            UUIDHelper.toBytes(buf, id);

            user.toBytes(buf);
        }
    }
}
