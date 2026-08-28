package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.CopiedFromInfo;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.common.util.SourcePluginIdHelper;
import games.alejandrocoria.mapfrontiers.common.util.StringHelper;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public class FrontierData {
    public static final int MAX_NAME_CHARACTERS = 48;
    private static final long FNV64_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV64_PRIME = 0x100000001b3L;
    private static final Comparator<ChunkPos> CHUNK_SYNC_HASH_ORDER = Comparator
            .comparingLong((ChunkPos chunk) -> chunk.pack());

    protected UUID id;
    protected final List<BlockPos> vertices = new ArrayList<>();
    protected final Set<ChunkPos> chunks = new ObjectOpenHashSet<>();
    protected final List<BlockPos> points = new ArrayList<>();
    protected FrontierShape frontierShape = FrontierShape.Vertex;
    protected String name1 = "New";
    protected String name2 = "Frontier";
    protected FrontierVisibilityData visibilityData;
    protected int color = ColorConstants.WHITE;
    protected ResourceKey<Level> dimension;
    protected SettingsUser owner = new SettingsUser();
    protected BannerData banner;
    protected boolean inheritCollectionBanner = true;
    protected boolean personal = false;
    protected TerritoryLifetime lifetime = TerritoryLifetime.PERSISTENT;
    protected List<SettingsUserShared> usersShared;
    protected CopiedFromInfo copiedFrom;
    protected @Nullable UUID collectionId;
    protected @Nullable String sourcePluginId;
    protected PathStyle pathStyle;
    protected Date created;
    protected Date modified;
    private boolean syncHashDirty = true;
    private long cachedSyncHash;
    private boolean chunksSyncHashDirty = true;
    private long cachedChunksSyncHash;

    public FrontierData() {
        id = new UUID(0, 0);
        visibilityData = new FrontierVisibilityData();
        pathStyle = new PathStyle();
    }

    public FrontierData(FrontierData other) {
        id = other.id;
        dimension = other.dimension;
        owner = other.owner;
        personal = other.personal;
        lifetime = other.lifetime;

        visibilityData = new FrontierVisibilityData(other.visibilityData);
        color = other.color;

        name1 = other.name1;
        name2 = other.name2;

        if (other.banner == null) {
            banner = null;
        } else {
            banner = new BannerData(other.banner);
        }
        inheritCollectionBanner = other.inheritCollectionBanner;

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
        invalidateSyncHash();
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
        visibilityData = new FrontierVisibilityData(other.visibilityData);
        color = other.color;
        name1 = other.name1;
        name2 = other.name2;
        banner = other.banner == null ? null : new BannerData(other.banner);
        inheritCollectionBanner = other.inheritCollectionBanner;
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
        invalidateChunksSyncHash();
    }

    public FrontierChangeApplicationResult stageChange(FrontierChange change) {
        if (change.hasShapeChange() && change.hasGeometryChanges()) {
            return FrontierChangeApplicationResult.rejected("Shape replacement and incremental geometry changes cannot coexist");
        }

        FrontierData stagedFrontier = new FrontierData(this);
        try {
            stagedFrontier.applyChangeUnchecked(change);
        } catch (IllegalArgumentException exception) {
            return FrontierChangeApplicationResult.rejected(exception.getMessage());
        }

        FrontierChange effectiveChange = new FrontierChange(change);
        if (change.hasGeometryChanges() && hasSameGeometry(stagedFrontier)) {
            effectiveChange.clearGeometryChanges();
        }
        if (hasSameFunctionalState(stagedFrontier)) {
            return FrontierChangeApplicationResult.noChange(this);
        }
        return FrontierChangeApplicationResult.applied(stagedFrontier, effectiveChange);
    }

    public FrontierChangeApplicationResult applyChange(FrontierChange change) {
        FrontierChangeApplicationResult result = stageChange(change);
        if (!result.isApplied()) {
            return result;
        }

        updateFromData(Objects.requireNonNull(result.frontier()));
        return FrontierChangeApplicationResult.applied(this, result.effectiveChange());
    }

    private void applyChangeUnchecked(FrontierChange change) {
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
            BannerData bannerData = change.getBanner().getBanner();
            banner = bannerData == null ? null : new BannerData(bannerData);
            inheritCollectionBanner = change.getBanner().inheritCollectionBanner();
        }

        if (change.hasShapeChange()) {
            FrontierChange.ShapeChange shapeChange = change.getShape();
            applyShapeData(shapeChange.getShape(), shapeChange.getVertices(), shapeChange.getChunks(), shapeChange.getPoints());
        }

        if (change.hasGeometryChanges()) {
            GeometryChangeApplier.apply(this, change.getGeometryChanges());
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

        invalidateSyncHash();
    }

    private boolean hasSameGeometry(FrontierData other) {
        return frontierShape == other.frontierShape
                && vertices.equals(other.vertices)
                && chunks.equals(other.chunks)
                && points.equals(other.points);
    }

    private boolean hasSameFunctionalState(FrontierData other) {
        return hasSameGeometry(other)
                && Objects.equals(visibilityData, other.visibilityData)
                && color == other.color
                && Objects.equals(name1, other.name1)
                && Objects.equals(name2, other.name2)
                && Objects.equals(banner, other.banner)
                && inheritCollectionBanner == other.inheritCollectionBanner
                && Objects.equals(pathStyle, other.pathStyle)
                && Objects.equals(collectionId, other.collectionId);
    }

    public void applySharingChange(FrontierSharingChange sharingChange) {
        usersShared = sharingChange.getUsersShared();
        sanitizeSharedUsers();
        invalidateSyncHash();
    }

    public void setOwner(SettingsUser owner) {
        this.owner = owner;
        invalidateSyncHash();
    }

    public void ensureOwner(MinecraftServer server) {
        String previousUsername = owner.username;
        UUID previousUuid = owner.uuid;
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
        if (!Objects.equals(previousUsername, owner.username) || !Objects.equals(previousUuid, owner.uuid)) {
            invalidateSyncHash();
        }
    }

    public SettingsUser getOwner() {
        return owner;
    }

    public void setId(UUID id) {
        this.id = id;
        invalidateSyncHash();
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
        invalidateSyncHash();
    }

    protected void addVertex(BlockPos pos, int index) {
        synchronized (vertices) {
            vertices.add(index, pos.atY(70));
        }
        invalidateSyncHash();
    }

    public void addVertex(BlockPos pos) {
        addVertex(pos, vertices.size());
    }

    public void removeVertex(int index) {
        if (index < 0 || index >= vertices.size()) {
            return;
        }

        synchronized (vertices) {
            vertices.remove(index);
        }
        invalidateSyncHash();
    }

    protected void moveVertex(BlockPos pos, int index) {
        if (index < 0 || index >= vertices.size()) {
            return;
        }

        synchronized (vertices) {
            vertices.set(index, pos);
        }
        invalidateSyncHash();
    }

    public void moveAllVertices(BlockPos delta) {
        synchronized (vertices) {
            vertices.replaceAll(blockPos -> blockPos.offset(delta));
        }
        invalidateSyncHash();
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
        invalidateSyncHash();
    }

    protected void addPoint(BlockPos pos, int index) {
        synchronized (points) {
            points.add(index, pos.atY(70));
        }
        invalidateSyncHash();
    }

    public void addPoint(BlockPos pos) {
        addPoint(pos, points.size());
    }

    public void removePoint(int index) {
        if (index < 0 || index >= points.size()) {
            return;
        }

        synchronized (points) {
            points.remove(index);
        }
        invalidateSyncHash();
    }

    protected void movePoint(BlockPos pos, int index) {
        if (index < 0 || index >= points.size()) {
            return;
        }

        synchronized (points) {
            points.set(index, pos);
        }
        invalidateSyncHash();
    }

    public void moveAllPoints(BlockPos delta) {
        synchronized (points) {
            points.replaceAll(blockPos -> blockPos.offset(delta));
        }
        invalidateSyncHash();
    }

    public boolean toggleChunk(ChunkPos chunk) {
        boolean added = false;
        synchronized (chunks) {
            if (!chunks.remove(chunk)) {
                chunks.add(chunk);
                added = true;
            }
        }
        invalidateChunksSyncHash();
        return added;
    }

    public boolean addChunk(ChunkPos chunk) {
        synchronized (chunks) {
            if (chunks.add(chunk)) {
                invalidateChunksSyncHash();
                return true;
            }
        }

        return false;
    }

    public boolean removeChunk(ChunkPos chunk) {
        synchronized (chunks) {
            if (chunks.remove(chunk)) {
                invalidateChunksSyncHash();
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
            return new ObjectOpenHashSet<>(chunks);
        }
    }

    public void clearChunks() {
        synchronized (chunks) {
            chunks.clear();
        }
        invalidateChunksSyncHash();
    }

    public void moveAllChunks(ChunkPos delta) {
        synchronized (chunks) {
            Set<ChunkPos> movedChunks = chunks.stream()
                    .map(chunk -> new ChunkPos(chunk.x() + delta.x(), chunk.z() + delta.z()))
                    .collect(Collectors.toCollection(ObjectOpenHashSet::new));
            chunks.clear();
            chunks.addAll(movedChunks);
        }
        invalidateChunksSyncHash();
    }

    public void setShape(FrontierShape frontierShape) {
        this.frontierShape = frontierShape;
        invalidateSyncHash();
    }

    public FrontierShape getShape() {
        return frontierShape;
    }

    public void setPathStyle(PathStyle pathStyle) {
        this.pathStyle = new PathStyle(pathStyle);
        invalidateSyncHash();
    }

    public PathStyle getPathStyle() {
        return new PathStyle(pathStyle);
    }

    public void setName1(String name) {
        name1 = name;
        invalidateSyncHash();
    }

    public String getName1() {
        return name1;
    }

    public void setName2(String name) {
        name2 = name;
        invalidateSyncHash();
    }

    public String getName2() {
        return name2;
    }

    public boolean isNamed() {
        return !StringUtils.isBlank(name1) || !StringUtils.isBlank(name2);
    }

    public void setVisibility(FrontierVisibility visibility, boolean enable) {
        this.visibilityData.set(visibility, enable);
        invalidateSyncHash();
    }

    public void toggleVisibility(FrontierVisibility visibility) {
        boolean set = !this.visibilityData.get(visibility);
        this.visibilityData.set(visibility, set);
        invalidateSyncHash();
    }

    public boolean getVisibility(FrontierVisibility visibility) {
        return visibilityData.get(visibility);
    }

    public void setVisibilityData(FrontierVisibilityData visibilityData) {
        this.visibilityData = visibilityData;
        invalidateSyncHash();
    }

    public FrontierVisibilityData getVisibilityData() {
        return visibilityData;
    }

    public void setColor(int color) {
        this.color = color;
        invalidateSyncHash();
    }

    public int getColor() {
        return color;
    }

    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
        invalidateSyncHash();
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
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
        invalidateSyncHash();
    }

    public BannerData getBannerData() {
        return banner;
    }

    public boolean getInheritCollectionBanner() {
        return inheritCollectionBanner;
    }

    public void setInheritCollectionBanner(boolean inheritCollectionBanner) {
        this.inheritCollectionBanner = inheritCollectionBanner;
        invalidateSyncHash();
    }

    public void setBannerRotation(int rotation) {
        banner.rotation = rotation;
        invalidateSyncHash();
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
        invalidateSyncHash();
    }

    public boolean getPersonal() {
        return personal;
    }

    public void setLifetime(TerritoryLifetime lifetime) {
        TerritoryLifetime checkedLifetime = Objects.requireNonNull(lifetime, "lifetime");
        validateTypeAndLifetime(personal, checkedLifetime);
        this.lifetime = checkedLifetime;
        sanitizeSharedUsers();
        invalidateSyncHash();
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
        invalidateSyncHash();
    }

    public void removeUserShared(SettingsUser user) {
        if (usersShared == null) {
            return;
        }

        usersShared.removeIf(x -> x.getUser().equals(user));
        invalidateSyncHash();
    }

    public void removeAllUserShared() {
        if (usersShared == null) {
            return;
        }

        usersShared = null;
        invalidateSyncHash();
    }

    public void removePendingUsersShared() {
        if (usersShared == null) {
            return;
        }

        usersShared.removeIf(SettingsUserShared::isPending);
        sanitizeSharedUsers();
        invalidateSyncHash();
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
        invalidateSyncHash();
    }

    public Date getCreated() {
        return created;
    }

    public boolean wasCopied() {
        return copiedFrom != null;
    }

    public void removeCopiedFromInfo() {
        copiedFrom = null;
        invalidateSyncHash();
    }

    public void setCopiedFromId(UUID id) {
        if (!wasCopied()) {
            copiedFrom = new CopiedFromInfo();
        }
        copiedFrom.setId(id);
        invalidateSyncHash();
    }

    public UUID getCopiedFromId() {
        if (copiedFrom == null) {
            return id;
        }
        return copiedFrom.getId();
    }

    public void setCopiedFromUser(SettingsUser user) {
        if (!wasCopied()) {
            copiedFrom = new CopiedFromInfo();
        }
        copiedFrom.setUser(user);
        invalidateSyncHash();
    }

    public SettingsUser getCopiedFromUser() {
        if (copiedFrom == null) {
            return owner;
        }
        return copiedFrom.getUser();
    }

    public void setModified(Date modified) {
        this.modified = modified;
        invalidateSyncHash();
    }

    public Date getModified() {
        return modified;
    }

    public void setCollectionId(@Nullable UUID collectionId) {
        this.collectionId = collectionId;
        invalidateSyncHash();
    }

    public @Nullable UUID getCollectionId() {
        return collectionId;
    }

    public boolean hasCollection() {
        return collectionId != null;
    }

    public void setSourcePluginId(@Nullable String sourcePluginId) {
        this.sourcePluginId = SourcePluginIdHelper.normalize(sourcePluginId);
        invalidateSyncHash();
    }

    public long computeSyncHash() {
        if (syncHashDirty) {
            long hash = FNV64_OFFSET_BASIS;
            hash = mixUuid(hash, id);
            hash = mixEnum(hash, frontierShape);
            hash = mixBoolean(hash, personal);
            hash = mixEnum(hash, lifetime);
            hash = mixIdentifier(hash, dimension == null ? null : dimension.identifier());
            hash = mixSettingsUser(hash, owner);
            hash = mixString(hash, name1);
            hash = mixString(hash, name2);
            hash = mixInt(hash, color);
            hash = mixVisibilityData(hash, visibilityData);
            hash = mixBannerData(hash, banner);
            hash = mixBoolean(hash, inheritCollectionBanner);
            hash = mixUuid(hash, collectionId);
            hash = mixString(hash, sourcePluginId);
            switch (frontierShape) {
                case Vertex -> hash = mixVertices(hash, vertices);
                case Chunk -> hash = mixLong(hash, getOrComputeChunksSyncHash());
                case Path -> {
                    hash = mixPoints(hash, points);
                    hash = mixPathStyle(hash, pathStyle);
                }
            }
            cachedSyncHash = hash;
            syncHashDirty = false;
        }

        return cachedSyncHash;
    }

    public @Nullable String getSourcePluginId() {
        return sourcePluginId;
    }

    public boolean readFromNBT(CompoundTag nbt, int version) {
        boolean changedDuringLoad = false;
        vertices.clear();
        chunks.clear();
        points.clear();
        pathStyle = new PathStyle();
        copiedFrom = null;
        usersShared = null;
        banner = null;
        inheritCollectionBanner = true;

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
            changedDuringLoad |= banner.readFromNBT(NbtReadHelper.requireCompound(nbt, "banner"));
        }
        inheritCollectionBanner = nbt.getBooleanOr("inheritCollectionBanner", true);

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
        invalidateSyncHash();
        return changedDuringLoad;
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
        nbt.putBoolean("inheritCollectionBanner", inheritCollectionBanner);

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
                    compoundtag.putInt("X", pos.x());
                    compoundtag.putInt("Z", pos.z());
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
        inheritCollectionBanner = buf.readBoolean();

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
                    chunks.add(ChunkPos.unpack(buf.readLong()));
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
        invalidateSyncHash();
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
        buf.writeBoolean(inheritCollectionBanner);

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
                    buf.writeLong(pos.pack());
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
        invalidateSyncHash();
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

    private void invalidateSyncHash() {
        syncHashDirty = true;
    }

    void invalidateGeometryHash() {
        invalidateSyncHash();
    }

    private void invalidateChunksSyncHash() {
        chunksSyncHashDirty = true;
        syncHashDirty = true;
    }

    private long getOrComputeChunksSyncHash() {
        if (chunksSyncHashDirty) {
            List<ChunkPos> orderedChunks;
            synchronized (chunks) {
                orderedChunks = new ArrayList<>(chunks);
            }
            orderedChunks.sort(CHUNK_SYNC_HASH_ORDER);
            long hash = FNV64_OFFSET_BASIS;
            hash = mixInt(hash, orderedChunks.size());
            for (ChunkPos chunk : orderedChunks) {
                hash = mixLong(hash, chunk.pack());
            }
            cachedChunksSyncHash = hash;
            chunksSyncHashDirty = false;
        }

        return cachedChunksSyncHash;
    }

    private static long mixVertices(long hash, List<BlockPos> vertices) {
        hash = mixInt(hash, vertices.size());
        for (BlockPos vertex : vertices) {
            hash = mixLong(hash, vertex.asLong());
        }
        return hash;
    }

    private static long mixPoints(long hash, List<BlockPos> points) {
        hash = mixInt(hash, points.size());
        for (BlockPos point : points) {
            hash = mixLong(hash, point.asLong());
        }
        return hash;
    }

    private static long mixVisibilityData(long hash, @Nullable FrontierVisibilityData visibilityData) {
        if (visibilityData == null) {
            return mixBoolean(hash, false);
        }
        hash = mixBoolean(hash, true);
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            hash = mixBoolean(hash, visibilityData.get(visibility));
        }
        return hash;
    }

    private static long mixBannerData(long hash, @Nullable BannerData banner) {
        if (banner == null) {
            return mixBoolean(hash, false);
        }

        hash = mixBoolean(hash, true);
        hash = mixInt(hash, banner.baseColor.getId());
        hash = mixInt(hash, banner.rotation);
        return mixTag(hash, banner.patterns);
    }

    private static long mixPathStyle(long hash, @Nullable PathStyle pathStyle) {
        if (pathStyle == null) {
            return mixBoolean(hash, false);
        }

        hash = mixBoolean(hash, true);
        hash = mixIdentifier(hash, pathStyle.startMarker);
        hash = mixIdentifier(hash, pathStyle.innerMarker);
        hash = mixIdentifier(hash, pathStyle.endMarker);
        hash = mixIdentifier(hash, pathStyle.segmentMarker);
        hash = mixBoolean(hash, pathStyle.labelAtStart);
        hash = mixBoolean(hash, pathStyle.labelAtMiddle);
        hash = mixBoolean(hash, pathStyle.labelAtEnd);
        return hash;
    }

    private static long mixSettingsUser(long hash, @Nullable SettingsUser user) {
        if (user == null) {
            return mixBoolean(hash, false);
        }

        hash = mixBoolean(hash, true);
        hash = mixString(hash, user.username);
        return mixUuid(hash, user.uuid);
    }

    private static long mixTag(long hash, @Nullable Tag tag) {
        if (tag == null) {
            return mixBoolean(hash, false);
        }

        hash = mixBoolean(hash, true);
        hash = mixInt(hash, tag.getId());
        if (tag instanceof CompoundTag compoundTag) {
            List<String> keys = new ArrayList<>(compoundTag.keySet());
            Collections.sort(keys);
            long mixedHash = mixInt(hash, keys.size());
            for (String key : keys) {
                mixedHash = mixString(mixedHash, key);
                mixedHash = mixTag(mixedHash, compoundTag.get(key));
            }
            return mixedHash;
        }
        if (tag instanceof ListTag listTag) {
            long mixedHash = mixInt(hash, listTag.size());
            for (Tag child : listTag) {
                mixedHash = mixTag(mixedHash, child);
            }
            return mixedHash;
        }
        return mixString(hash, tag.toString());
    }

    private static long mixIdentifier(long hash, @Nullable Identifier identifier) {
        return mixString(hash, identifier == null ? null : identifier.toString());
    }

    private static long mixEnum(long hash, @Nullable Enum<?> value) {
        if (value == null) {
            return mixInt(hash, -1);
        }
        return mixInt(hash, value.ordinal());
    }

    private static long mixUuid(long hash, @Nullable UUID value) {
        if (value == null) {
            hash = mixBoolean(hash, false);
            return hash;
        }

        hash = mixBoolean(hash, true);
        hash = mixLong(hash, value.getMostSignificantBits());
        return mixLong(hash, value.getLeastSignificantBits());
    }

    private static long mixString(long hash, @Nullable String value) {
        if (value == null) {
            return mixBoolean(hash, false);
        }

        hash = mixBoolean(hash, true);
        hash = mixInt(hash, value.length());
        for (int i = 0; i < value.length(); ++i) {
            hash = mixInt(hash, value.charAt(i));
        }
        return hash;
    }

    private static long mixBoolean(long hash, boolean value) {
        return mixByte(hash, value ? 1 : 0);
    }

    private static long mixInt(long hash, int value) {
        hash = mixByte(hash, value);
        hash = mixByte(hash, value >>> 8);
        hash = mixByte(hash, value >>> 16);
        return mixByte(hash, value >>> 24);
    }

    private static long mixLong(long hash, long value) {
        hash = mixByte(hash, (int) value);
        hash = mixByte(hash, (int) (value >>> 8));
        hash = mixByte(hash, (int) (value >>> 16));
        hash = mixByte(hash, (int) (value >>> 24));
        hash = mixByte(hash, (int) (value >>> 32));
        hash = mixByte(hash, (int) (value >>> 40));
        hash = mixByte(hash, (int) (value >>> 48));
        return mixByte(hash, (int) (value >>> 56));
    }

    private static long mixByte(long hash, int value) {
        hash ^= value & 0xffL;
        return hash * FNV64_PRIME;
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
