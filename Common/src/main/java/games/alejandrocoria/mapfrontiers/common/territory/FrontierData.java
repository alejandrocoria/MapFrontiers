package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.SourcePluginIdHelper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public class FrontierData {
    public static final int MAX_NAME_CHARACTERS = 48;

    protected UUID id;
    protected final List<BlockPos> vertices = new ArrayList<>();
    protected final Set<ChunkPos> chunks = new HashSet<>();
    protected final List<BlockPos> points = new ArrayList<>();
    protected FrontierShape frontierShape = FrontierShape.Vertex;
    protected String name1 = "New";
    protected String name2 = "Frontier";
    protected VisibilityData visibilityData;
    protected int color = ColorConstants.WHITE;
    protected ResourceKey<Level> dimension;
    protected SettingsUser owner = new SettingsUser();
    protected BannerData banner;
    protected boolean personal = false;
    protected TerritoryLifetime lifetime = TerritoryLifetime.PERSISTENT;
    protected List<SettingsUserShared> usersShared;
    protected CopiedFromInfo copiedFrom;
    protected @Nullable UUID collectionId;
    protected @Nullable String sourcePluginId;
    protected PathStyle pathStyle;
    protected Date created;
    protected Date modified;

    public FrontierData() {
        id = new UUID(0, 0);
        visibilityData = new VisibilityData();
        pathStyle = new PathStyle();
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
        points.clear();
        points.addAll(other.points);
        frontierShape = other.frontierShape;
        pathStyle = other.pathStyle == null ? new PathStyle() : new PathStyle(other.pathStyle);

        copiedFrom = other.copiedFrom;
        collectionId = other.collectionId;
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
        points.clear();
        points.addAll(other.points);
        frontierShape = other.frontierShape;
        pathStyle = other.pathStyle == null ? new PathStyle() : new PathStyle(other.pathStyle);

        copiedFrom = other.copiedFrom;
        collectionId = other.collectionId;
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
            applyShapeData(shapeChange.getShape(), shapeChange.getVertices(), shapeChange.getChunks(), shapeChange.getPoints());
        }

        if (change.hasPathStyleChange()) {
            pathStyle = change.getPathStyle().getPathStyle();
        }

        if (change.hasCollectionIdChange()) {
            collectionId = change.getCollectionIdChange().getCollectionId();
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

    public int getPointCount() {
        return points.size();
    }

    public List<BlockPos> getPoints() {
        synchronized (points) {
            return new ArrayList<>(points);
        }
    }

    public void clearPoints() {
        synchronized (points) {
            points.clear();
        }
    }

    protected void addPoint(BlockPos pos, int index) {
        synchronized (points) {
            points.add(index, pos.atY(70));
        }
    }

    public void addPoint(BlockPos pos) {
        synchronized (points) {
            addPoint(pos, points.size());
        }
    }

    public void removePoint(int index) {
        if (index < 0 || index >= points.size()) {
            return;
        }

        synchronized (points) {
            points.remove(index);
        }
    }

    protected void movePoint(BlockPos pos, int index) {
        if (index < 0 || index >= points.size()) {
            return;
        }

        synchronized (points) {
            points.set(index, pos);
        }
    }

    public void moveAllPoints(BlockPos delta) {
        synchronized (points) {
            points.replaceAll(blockPos -> blockPos.offset(delta));
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

    public void setShape(FrontierShape frontierShape) {
        this.frontierShape = frontierShape;
    }

    public FrontierShape getShape() {
        return frontierShape;
    }

    public void setPathStyle(PathStyle pathStyle) {
        this.pathStyle = new PathStyle(pathStyle);
    }

    public PathStyle getPathStyle() {
        return new PathStyle(pathStyle);
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

    public void setVisibility(FrontierVisibility visibility, boolean enable) {
        this.visibilityData.setValue(visibility, enable);
    }

    public void toggleVisibility(FrontierVisibility visibility) {
        this.visibilityData.setValue(visibility, !this.visibilityData.getValue(visibility));
    }

    public boolean getVisibility(FrontierVisibility visibility) {
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
        if (this.personal != personal) {
            collectionId = null;
        }
        this.personal = personal;
        sanitizeSharedUsers();
    }

    public boolean getPersonal() {
        return personal;
    }

    public void setLifetime(TerritoryLifetime lifetime) {
        TerritoryLifetime checkedLifetime = Objects.requireNonNull(lifetime, "lifetime");
        validateTypeAndLifetime(personal, checkedLifetime);
        this.lifetime = checkedLifetime;
        sanitizeSharedUsers();
    }

    public TerritoryLifetime getLifetime() {
        return lifetime;
    }

    public boolean isPersistent() {
        return lifetime == TerritoryLifetime.PERSISTENT;
    }

    public boolean isSessionOnly() {
        return lifetime == TerritoryLifetime.SESSION_ONLY;
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
            copiedFrom = new CopiedFromInfo();
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
            copiedFrom = new CopiedFromInfo();
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

    public void setCollectionId(@Nullable UUID collectionId) {
        this.collectionId = collectionId;
    }

    public @Nullable UUID getCollectionId() {
        return collectionId;
    }

    public boolean hasCollection() {
        return collectionId != null;
    }

    public void setSourcePluginId(@Nullable String sourcePluginId) {
        this.sourcePluginId = SourcePluginIdHelper.normalize(sourcePluginId);
    }

    public @Nullable String getSourcePluginId() {
        return sourcePluginId;
    }

    public void readFromNBT(CompoundTag nbt, int version) {
        vertices.clear();
        chunks.clear();
        points.clear();
        pathStyle = new PathStyle();
        copiedFrom = null;
        usersShared = null;
        banner = null;

        id = UUID.fromString(NbtReadHelper.requireString(nbt, "id"));
        color = NbtReadHelper.requireInt(nbt, "color");
        dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(NbtReadHelper.requireString(nbt, "dimension")));
        name1 = nbt.getStringOr("name1", "");
        name2 = nbt.getStringOr("name2", "");

        if (version < 10) {
            visibilityData.readFromLegacyNBT(nbt);
        } else {
            CompoundTag visibilityTag = version == 10 ? nbt : nbt.getCompoundOrEmpty("visibility");
            visibilityData.readFromNBT(visibilityTag);
        }

        personal = nbt.getBooleanOr("personal", true);
        lifetime = readLifetimeFromNbt(nbt);
        try {
            validateTypeAndLifetime(personal, lifetime);
        } catch (IllegalArgumentException e) {
            throw new InvalidNbtFormatException("Invalid lifetime for frontier " + id + ": " + e.getMessage(), e);
        }
        setSourcePluginId(nbt.getStringOr("sourcePluginId", null));

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

        ListTag pointsTagList = nbt.getListOrEmpty("points");
        for (int i = 0; i < pointsTagList.size(); ++i) {
            try {
                CompoundTag posTag = NbtReadHelper.requireCompound(pointsTagList, i, "points");
                points.add(new BlockPos(NbtReadHelper.requireInt(posTag, "X"), 70, NbtReadHelper.requireInt(posTag, "Z")));
            } catch (InvalidNbtFormatException e) {
                throw new InvalidNbtFormatException("Invalid point at points[" + i + "] for frontier " + id + ": " + e.getMessage(), e);
            }
        }

        String modeTag = nbt.getStringOr("mode", "");
        if (modeTag.isEmpty()) {
            frontierShape = FrontierShape.Vertex;
        } else {
            try {
                frontierShape = FrontierShape.valueOf(modeTag);
            } catch (IllegalArgumentException e) {
                if (chunks.size() > 0) {
                    frontierShape = FrontierShape.Chunk;
                } else {
                    frontierShape = FrontierShape.Vertex;
                }

                String availableModes = StringHelper.enumValuesToString(Arrays.asList(FrontierShape.VALUES));

                MapFrontiers.LOGGER.warn("Unknown mode in frontier {}. Found: \"{}\". Expected: {}", id, modeTag, availableModes);
            }
        }

        if (frontierShape == FrontierShape.Path && nbt.contains("pathStyle")) {
            pathStyle.readFromNBT(NbtReadHelper.requireCompound(nbt, "pathStyle"));
        }

        if (nbt.contains("copiedFrom")) {
            copiedFrom = new CopiedFromInfo();
            copiedFrom.readFromNBT(NbtReadHelper.requireCompound(nbt, "copiedFrom"), version);
        }

        if (nbt.contains("collectionId")) {
            collectionId = UUID.fromString(NbtReadHelper.requireString(nbt, "collectionId"));
        } else {
            collectionId = null;
        }

        if (nbt.contains("created")) {
            created = new Date(NbtReadHelper.requireLong(nbt, "created"));
        }

        if (nbt.contains("modified")) {
            modified = new Date(NbtReadHelper.requireLong(nbt, "modified"));
        }

        normalizeDataForMode();
        sanitizeSharedUsers();
    }

    public void writeToNBT(CompoundTag nbt) {
        assertSerializableLifetime();

        nbt.putString("id", id.toString());
        nbt.putInt("color", color);
        nbt.putString("dimension", dimension.identifier().toString());
        nbt.putString("name1", name1);
        nbt.putString("name2", name2);
        CompoundTag visibilityTag = new CompoundTag();
        visibilityData.writeToNBT(visibilityTag);
        nbt.put("visibility", visibilityTag);
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

        nbt.putString("mode", frontierShape.name());

        switch (frontierShape) {
            case Vertex -> {
                ListTag verticesTagList = new ListTag();
                for (BlockPos pos : vertices) {
                    CompoundTag compoundtag = new CompoundTag();
                    compoundtag.putInt("X", pos.getX());
                    compoundtag.putInt("Y", pos.getY());
                    compoundtag.putInt("Z", pos.getZ());
                    verticesTagList.add(compoundtag);
                }
                nbt.put("vertices", verticesTagList);
            }
            case Chunk -> {
                ListTag chunksTagList = new ListTag();
                for (ChunkPos pos : chunks) {
                    CompoundTag compoundtag = new CompoundTag();
                    compoundtag.putInt("X", pos.x);
                    compoundtag.putInt("Z", pos.z);
                    chunksTagList.add(compoundtag);
                }
                nbt.put("chunks", chunksTagList);
            }
            case Path -> {
                ListTag pointsTagList = new ListTag();
                for (BlockPos pos : points) {
                    CompoundTag compoundtag = new CompoundTag();
                    compoundtag.putInt("X", pos.getX());
                    compoundtag.putInt("Y", pos.getY());
                    compoundtag.putInt("Z", pos.getZ());
                    pointsTagList.add(compoundtag);
                }
                nbt.put("points", pointsTagList);

                CompoundTag pathStyleTag = new CompoundTag();
                pathStyle.writeToNBT(pathStyleTag);
                nbt.put("pathStyle", pathStyleTag);
            }
        }

        if (wasCopied()) {
            CompoundTag nbtCopiedFrom = new CompoundTag();
            copiedFrom.writeToNBT(nbtCopiedFrom);
            nbt.put("copiedFrom", nbtCopiedFrom);
        }

        if (collectionId != null) {
            nbt.putString("collectionId", collectionId.toString());
        }

        if (created != null) {
            nbt.putLong("created", created.getTime());
        }

        if (modified != null) {
            nbt.putLong("modified", modified.getTime());
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        vertices.clear();
        chunks.clear();
        points.clear();
        pathStyle = new PathStyle();

        id = UUIDHelper.fromBytes(buf);
        dimension = ResourceKey.create(Registries.DIMENSION, buf.readIdentifier());
        personal = buf.readBoolean();
        lifetime = readLifetimeFromBytes(buf);
        validateTypeAndLifetime(personal, lifetime);
        setSourcePluginId(buf.readBoolean() ? buf.readUtf() : null);
        owner = new SettingsUser();
        owner.fromBytes(buf);
        visibilityData.fromBytes(buf);
        color = buf.readInt();

        name1 = buf.readUtf(MAX_NAME_CHARACTERS);
        name2 = buf.readUtf(MAX_NAME_CHARACTERS);

        if (name1.length() > MAX_NAME_CHARACTERS) {
            name1 = name1.substring(0, MAX_NAME_CHARACTERS);
        }
        if (name2.length() > MAX_NAME_CHARACTERS) {
            name2 = name2.substring(0, MAX_NAME_CHARACTERS);
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

        frontierShape = FrontierShape.VALUES[buf.readInt()];

        switch (frontierShape) {
            case Vertex -> {
                int vertexCount = buf.readInt();
                for (int i = 0; i < vertexCount; ++i) {
                    vertices.add(BlockPos.of(buf.readLong()));
                }
            }
            case Chunk -> {
                int chunkCount = buf.readInt();
                for (int i = 0; i < chunkCount; ++i) {
                    chunks.add(new ChunkPos(buf.readLong()));
                }
            }
            case Path -> {
                int pointCount = buf.readInt();
                for (int i = 0; i < pointCount; ++i) {
                    points.add(BlockPos.of(buf.readLong()));
                }
                pathStyle.fromBytes(buf);
            }
        }

        if (buf.readBoolean()) {
            copiedFrom = new CopiedFromInfo();
            copiedFrom.fromBytes(buf);
        } else {
            copiedFrom = null;
        }

        if (buf.readBoolean()) {
            collectionId = UUIDHelper.fromBytes(buf);
        } else {
            collectionId = null;
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

        normalizeDataForMode();
        sanitizeSharedUsers();
    }

    public void toBytes(FriendlyByteBuf buf) {
        assertSerializableLifetime();

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

        buf.writeUtf(name1, MAX_NAME_CHARACTERS);
        buf.writeUtf(name2, MAX_NAME_CHARACTERS);

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

        buf.writeInt(frontierShape.ordinal());

        switch (frontierShape) {
            case Vertex -> {
                buf.writeInt(vertices.size());
                for (BlockPos pos : vertices) {
                    buf.writeLong(pos.asLong());
                }
            }
            case Chunk -> {
                buf.writeInt(chunks.size());
                for (ChunkPos pos : chunks) {
                    buf.writeLong(pos.toLong());
                }
            }
            case Path -> {
                buf.writeInt(points.size());
                for (BlockPos pos : points) {
                    buf.writeLong(pos.asLong());
                }
                pathStyle.toBytes(buf);
            }
        }

        if (wasCopied()) {
            buf.writeBoolean(true);
            copiedFrom.toBytes(buf);
        } else {
            buf.writeBoolean(false);
        }

        if (collectionId == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            UUIDHelper.toBytes(buf, collectionId);
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

    private void applyShapeData(FrontierShape frontierShape, List<BlockPos> vertices, Set<ChunkPos> chunks, List<BlockPos> points) {
        this.frontierShape = frontierShape;

        clearVertices();
        clearChunks();
        clearPoints();

        switch (frontierShape) {
            case Vertex -> {
                synchronized (this.vertices) {
                    this.vertices.addAll(vertices);
                }
            }
            case Chunk -> {
                synchronized (this.chunks) {
                    this.chunks.addAll(chunks);
                }
            }
            case Path -> {
                synchronized (this.points) {
                    this.points.addAll(points);
                }
            }
        }
    }

    private void normalizeDataForMode() {
        switch (frontierShape) {
            case Vertex -> {
                clearChunks();
                clearPoints();
            }
            case Chunk -> {
                clearVertices();
                clearPoints();
            }
            case Path -> {
                clearVertices();
                clearChunks();
            }
        }
    }

    private static void validateTypeAndLifetime(boolean personal, TerritoryLifetime lifetime) {
        if (!personal && lifetime == TerritoryLifetime.SESSION_ONLY) {
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

    private static TerritoryLifetime readLifetimeFromNbt(CompoundTag nbt) {
        String lifetimeTag = nbt.getStringOr("lifetime", "");
        if (lifetimeTag.isEmpty()) {
            return TerritoryLifetime.PERSISTENT;
        }

        try {
            return TerritoryLifetime.valueOf(lifetimeTag);
        } catch (IllegalArgumentException e) {
            String availableLifetimes = StringHelper.enumValuesToString(Arrays.asList(TerritoryLifetime.values()));
            MapFrontiers.LOGGER.warn("Unknown lifetime in frontier {}. Found: \"{}\". Expected: {}", idFromTag(nbt), lifetimeTag, availableLifetimes);
            return TerritoryLifetime.PERSISTENT;
        }
    }

    private static TerritoryLifetime readLifetimeFromBytes(FriendlyByteBuf buf) {
        int lifetimeOrdinal = buf.readInt();
        if (lifetimeOrdinal < 0 || lifetimeOrdinal >= TerritoryLifetime.VALUES.length) {
            MapFrontiers.LOGGER.warn("Unknown lifetime ordinal in frontier packet. Found: {}. Defaulting to {}", lifetimeOrdinal,
                    TerritoryLifetime.PERSISTENT);
            return TerritoryLifetime.PERSISTENT;
        }

        return TerritoryLifetime.VALUES[lifetimeOrdinal];
    }

    private void assertSerializableLifetime() {
        if (isSessionOnly()) {
            throw new IllegalStateException("Cannot serialize SESSION_ONLY frontier. id=" + id + ", personal=" + personal + ", lifetime=" + lifetime);
        }
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

    public static class PathStyle {
        public static final Identifier NONE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "none");
        public static final Identifier BIG_DOT = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "big_dot");
        public static final Identifier SMALL_DOT = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "small_dot");
        public static final Identifier RING = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "ring");
        public static final Identifier BIG_SQUARE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "big_square");
        public static final Identifier SMALL_SQUARE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "small_square");
        public static final Identifier DIAMOND = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "diamond");
        public static final Identifier TRIANGLE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "triangle");
        public static final Identifier ARROW = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "arrow");
        public static final Identifier CHEVRON = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "chevron");
        public static final Identifier X_CROSS = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "x_cross");

        public Identifier startMarker;
        public Identifier innerMarker;
        public Identifier endMarker;
        public Identifier segmentMarker;
        public boolean labelAtStart;
        public boolean labelAtMiddle;
        public boolean labelAtEnd;

        public PathStyle() {
            startMarker = BIG_DOT;
            innerMarker = NONE;
            endMarker = BIG_DOT;
            segmentMarker = SMALL_DOT;
            labelAtStart = true;
            labelAtMiddle = false;
            labelAtEnd = false;
        }

        public PathStyle(PathStyle other) {
            startMarker = other.startMarker;
            innerMarker = other.innerMarker;
            endMarker = other.endMarker;
            segmentMarker = other.segmentMarker;
            labelAtStart = other.labelAtStart;
            labelAtMiddle = other.labelAtMiddle;
            labelAtEnd = other.labelAtEnd;
        }

        public void readFromNBT(CompoundTag nbt) {
            startMarker = readMarkerFromNBT(nbt, "start", BIG_DOT);
            innerMarker = readMarkerFromNBT(nbt, "inner", NONE);
            endMarker = readMarkerFromNBT(nbt, "end", BIG_DOT);
            segmentMarker = readMarkerFromNBT(nbt, "segment", SMALL_DOT);
            labelAtStart = nbt.getBooleanOr("labelAtStart", true);
            labelAtMiddle = nbt.getBooleanOr("labelAtMiddle", false);
            labelAtEnd = nbt.getBooleanOr("labelAtEnd", false);
            normalizeForPersistence();
        }

        public void writeToNBT(CompoundTag nbt) {
            normalizeForPersistence();
            nbt.putString("start", startMarker.toString());
            nbt.putString("inner", innerMarker.toString());
            nbt.putString("end", endMarker.toString());
            nbt.putString("segment", segmentMarker.toString());
            nbt.putBoolean("labelAtStart", labelAtStart);
            nbt.putBoolean("labelAtMiddle", labelAtMiddle);
            nbt.putBoolean("labelAtEnd", labelAtEnd);
        }

        public void fromBytes(FriendlyByteBuf buf) {
            startMarker = normalizeMarkerId(buf.readIdentifier(), BIG_DOT);
            innerMarker = normalizeMarkerId(buf.readIdentifier(), NONE);
            endMarker = normalizeMarkerId(buf.readIdentifier(), BIG_DOT);
            segmentMarker = normalizeMarkerId(buf.readIdentifier(), SMALL_DOT);
            labelAtStart = buf.readBoolean();
            labelAtMiddle = buf.readBoolean();
            labelAtEnd = buf.readBoolean();
            normalizeForPersistence();
        }

        public void toBytes(FriendlyByteBuf buf) {
            normalizeForPersistence();
            buf.writeIdentifier(startMarker);
            buf.writeIdentifier(innerMarker);
            buf.writeIdentifier(endMarker);
            buf.writeIdentifier(segmentMarker);
            buf.writeBoolean(labelAtStart);
            buf.writeBoolean(labelAtMiddle);
            buf.writeBoolean(labelAtEnd);
        }

        public void normalizeForPersistence() {
            startMarker = normalizeMarkerId(startMarker, BIG_DOT);
            innerMarker = normalizeMarkerId(innerMarker, NONE);
            endMarker = normalizeMarkerId(endMarker, BIG_DOT);
            segmentMarker = normalizeMarkerId(segmentMarker, SMALL_DOT);

            if (!labelAtStart && !labelAtMiddle && !labelAtEnd) {
                labelAtStart = true;
            }
        }

        private static Identifier readMarkerFromNBT(CompoundTag nbt, String key, Identifier fallback) {
            String markerId = nbt.getStringOr(key, fallback.toString());
            try {
                return normalizeMarkerId(Identifier.parse(markerId), fallback);
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private static Identifier normalizeMarkerId(@Nullable Identifier value, Identifier fallback) {
            return value == null ? fallback : value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof PathStyle otherPathStyle)) {
                return false;
            }

            return labelAtStart == otherPathStyle.labelAtStart
                    && labelAtMiddle == otherPathStyle.labelAtMiddle
                    && labelAtEnd == otherPathStyle.labelAtEnd
                    && startMarker.equals(otherPathStyle.startMarker)
                    && innerMarker.equals(otherPathStyle.innerMarker)
                    && endMarker.equals(otherPathStyle.endMarker)
                    && segmentMarker.equals(otherPathStyle.segmentMarker);
        }

        @Override
        public int hashCode() {
            return Objects.hash(startMarker, innerMarker, endMarker, segmentMarker, labelAtStart, labelAtMiddle, labelAtEnd);
        }
    }
}
