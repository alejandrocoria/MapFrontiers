package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.SourcePluginIdHelper;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public final class FrontierCreateSpec {
    private final UUID frontierId;
    private final SettingsUser owner;
    private final boolean personal;
    private final ResourceKey<Level> dimension;
    private final TerritoryLifetime lifetime;
    private final @Nullable UUID collectionId;
    private final @Nullable String sourcePluginId;
    private final String name1;
    private final String name2;
    private final int color;
    private final VisibilityData visibility;
    private final @Nullable FrontierData.BannerData banner;
    private final FrontierData.Mode mode;
    private final List<BlockPos> vertices;
    private final Set<ChunkPos> chunks;
    private final List<BlockPos> points;
    private final FrontierData.PathStyle pathStyle;

    private FrontierCreateSpec(UUID frontierId,
                               SettingsUser owner,
                               boolean personal,
                               ResourceKey<Level> dimension,
                               TerritoryLifetime lifetime,
                               @Nullable UUID collectionId,
                               @Nullable String sourcePluginId,
                               String name1,
                               String name2,
                               int color,
                               VisibilityData visibility,
                               @Nullable FrontierData.BannerData banner,
                               FrontierData.Mode mode,
                               List<BlockPos> vertices,
                               Set<ChunkPos> chunks,
                               List<BlockPos> points,
                               FrontierData.PathStyle pathStyle) {
        this.frontierId = Objects.requireNonNull(frontierId, "frontierId");
        this.owner = copyUser(Objects.requireNonNull(owner, "owner"));
        this.personal = personal;
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.lifetime = Objects.requireNonNull(lifetime, "lifetime");
        this.collectionId = collectionId;
        this.sourcePluginId = SourcePluginIdHelper.normalize(sourcePluginId);
        this.name1 = Objects.requireNonNull(name1, "name1");
        this.name2 = Objects.requireNonNull(name2, "name2");
        this.color = color;
        this.visibility = new VisibilityData(Objects.requireNonNull(visibility, "visibility"));
        this.banner = banner == null ? null : new FrontierData.BannerData(banner);
        this.mode = Objects.requireNonNull(mode, "mode");
        this.vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
        this.chunks = Set.copyOf(Objects.requireNonNull(chunks, "chunks"));
        this.points = List.copyOf(Objects.requireNonNull(points, "points"));
        this.pathStyle = new FrontierData.PathStyle(Objects.requireNonNull(pathStyle, "pathStyle"));

        validateTypeAndLifetime(personal, lifetime);
        validateShape(mode, this.vertices, this.chunks, this.points);
    }

    public static FrontierCreateSpec vertex(UUID frontierId,
                                            SettingsUser owner,
                                            boolean personal,
                                            ResourceKey<Level> dimension,
                                            TerritoryLifetime lifetime,
                                            @Nullable UUID collectionId,
                                            @Nullable String sourcePluginId,
                                            String name1,
                                            String name2,
                                            int color,
                                            VisibilityData visibility,
                                            @Nullable FrontierData.BannerData banner,
                                            List<BlockPos> vertices,
                                            FrontierData.PathStyle pathStyle) {
        return new FrontierCreateSpec(frontierId, owner, personal, dimension, lifetime, collectionId, sourcePluginId,
                name1, name2, color, visibility, banner, FrontierData.Mode.Vertex, vertices, Set.of(), List.of(), pathStyle);
    }

    public static FrontierCreateSpec chunk(UUID frontierId,
                                           SettingsUser owner,
                                           boolean personal,
                                           ResourceKey<Level> dimension,
                                           TerritoryLifetime lifetime,
                                           @Nullable UUID collectionId,
                                           @Nullable String sourcePluginId,
                                           String name1,
                                           String name2,
                                           int color,
                                           VisibilityData visibility,
                                           @Nullable FrontierData.BannerData banner,
                                           Set<ChunkPos> chunks,
                                           FrontierData.PathStyle pathStyle) {
        return new FrontierCreateSpec(frontierId, owner, personal, dimension, lifetime, collectionId, sourcePluginId,
                name1, name2, color, visibility, banner, FrontierData.Mode.Chunk, List.of(), chunks, List.of(), pathStyle);
    }

    public static FrontierCreateSpec path(UUID frontierId,
                                          SettingsUser owner,
                                          boolean personal,
                                          ResourceKey<Level> dimension,
                                          TerritoryLifetime lifetime,
                                          @Nullable UUID collectionId,
                                          @Nullable String sourcePluginId,
                                          String name1,
                                          String name2,
                                          int color,
                                          VisibilityData visibility,
                                          @Nullable FrontierData.BannerData banner,
                                          List<BlockPos> points,
                                          FrontierData.PathStyle pathStyle) {
        return new FrontierCreateSpec(frontierId, owner, personal, dimension, lifetime, collectionId, sourcePluginId,
                name1, name2, color, visibility, banner, FrontierData.Mode.Path, List.of(), Set.of(), points, pathStyle);
    }

    public UUID getFrontierId() {
        return frontierId;
    }

    public SettingsUser getOwner() {
        return copyUser(owner);
    }

    public boolean isPersonal() {
        return personal;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public TerritoryLifetime getLifetime() {
        return lifetime;
    }

    public @Nullable UUID getCollectionId() {
        return collectionId;
    }

    public @Nullable String getSourcePluginId() {
        return sourcePluginId;
    }

    public String getName1() {
        return name1;
    }

    public String getName2() {
        return name2;
    }

    public int getColor() {
        return color;
    }

    public VisibilityData getVisibility() {
        return new VisibilityData(visibility);
    }

    public @Nullable FrontierData.BannerData getBanner() {
        return banner == null ? null : new FrontierData.BannerData(banner);
    }

    public FrontierData.Mode getMode() {
        return mode;
    }

    public List<BlockPos> getVertices() {
        return vertices;
    }

    public Set<ChunkPos> getChunks() {
        return chunks;
    }

    public List<BlockPos> getPoints() {
        return points;
    }

    public FrontierData.PathStyle getPathStyle() {
        return new FrontierData.PathStyle(pathStyle);
    }

    public FrontierCreateSpec withOwner(SettingsUser owner) {
        return new FrontierCreateSpec(frontierId, owner, personal, dimension, lifetime, collectionId, sourcePluginId,
                name1, name2, color, visibility, banner, mode, vertices, chunks, points, pathStyle);
    }

    public void toBytes(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierId);
        owner.toBytes(buf);
        buf.writeBoolean(personal);
        buf.writeIdentifier(dimension.identifier());
        buf.writeInt(lifetime.ordinal());

        if (collectionId == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            UUIDHelper.toBytes(buf, collectionId);
        }

        if (sourcePluginId == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeUtf(sourcePluginId);
        }

        buf.writeUtf(name1, FrontierData.MAX_NAME_CHARACTERS);
        buf.writeUtf(name2, FrontierData.MAX_NAME_CHARACTERS);
        buf.writeInt(color);
        visibility.toBytes(buf);

        if (banner == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            banner.toBytes(buf);
        }

        buf.writeInt(mode.ordinal());

        switch (mode) {
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
            }
        }

        pathStyle.toBytes(buf);
    }

    public static FrontierCreateSpec fromBytes(FriendlyByteBuf buf) {
        UUID frontierId = UUIDHelper.fromBytes(buf);
        SettingsUser owner = new SettingsUser();
        owner.fromBytes(buf);
        boolean personal = buf.readBoolean();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readIdentifier());
        TerritoryLifetime lifetime = TerritoryLifetime.VALUES[buf.readInt()];
        UUID collectionId = buf.readBoolean() ? UUIDHelper.fromBytes(buf) : null;
        String sourcePluginId = buf.readBoolean() ? buf.readUtf() : null;
        String name1 = buf.readUtf(FrontierData.MAX_NAME_CHARACTERS);
        String name2 = buf.readUtf(FrontierData.MAX_NAME_CHARACTERS);
        int color = buf.readInt();

        VisibilityData visibility = new VisibilityData();
        visibility.fromBytes(buf);

        FrontierData.BannerData banner = null;
        if (buf.readBoolean()) {
            banner = new FrontierData.BannerData();
            banner.fromBytes(buf);
        }

        FrontierData.Mode mode = FrontierData.Mode.VALUES[buf.readInt()];
        FrontierData.PathStyle pathStyle = new FrontierData.PathStyle();

        return switch (mode) {
            case Vertex -> {
                int vertexCount = buf.readInt();
                List<BlockPos> vertices = new ArrayList<>(vertexCount);
                for (int i = 0; i < vertexCount; ++i) {
                    vertices.add(BlockPos.of(buf.readLong()));
                }
                pathStyle.fromBytes(buf);
                yield vertex(frontierId, owner, personal, dimension, lifetime, collectionId, sourcePluginId, name1, name2,
                        color, visibility, banner, vertices, pathStyle);
            }
            case Chunk -> {
                int chunkCount = buf.readInt();
                Set<ChunkPos> chunks = new LinkedHashSet<>(chunkCount);
                for (int i = 0; i < chunkCount; ++i) {
                    chunks.add(ChunkPos.unpack(buf.readLong()));
                }
                pathStyle.fromBytes(buf);
                yield chunk(frontierId, owner, personal, dimension, lifetime, collectionId, sourcePluginId, name1, name2,
                        color, visibility, banner, chunks, pathStyle);
            }
            case Path -> {
                int pointCount = buf.readInt();
                List<BlockPos> points = new ArrayList<>(pointCount);
                for (int i = 0; i < pointCount; ++i) {
                    points.add(BlockPos.of(buf.readLong()));
                }
                pathStyle.fromBytes(buf);
                yield path(frontierId, owner, personal, dimension, lifetime, collectionId, sourcePluginId, name1, name2,
                        color, visibility, banner, points, pathStyle);
            }
        };
    }

    private static SettingsUser copyUser(SettingsUser owner) {
        SettingsUser copy = new SettingsUser();
        copy.username = owner.username;
        copy.uuid = owner.uuid;
        return copy;
    }

    private static void validateTypeAndLifetime(boolean personal, TerritoryLifetime lifetime) {
        if (!personal && lifetime == TerritoryLifetime.SESSION_ONLY) {
            throw new IllegalArgumentException("SESSION_ONLY frontiers must be personal");
        }
    }

    private static void validateShape(FrontierData.Mode mode,
                                      List<BlockPos> vertices,
                                      Set<ChunkPos> chunks,
                                      List<BlockPos> points) {
        switch (mode) {
            case Vertex -> {
                if (!chunks.isEmpty() || !points.isEmpty()) {
                    throw new IllegalArgumentException("Vertex create specs can only carry vertices");
                }
            }
            case Chunk -> {
                if (!vertices.isEmpty() || !points.isEmpty()) {
                    throw new IllegalArgumentException("Chunk create specs can only carry chunks");
                }
            }
            case Path -> {
                if (!vertices.isEmpty() || !chunks.isEmpty()) {
                    throw new IllegalArgumentException("Path create specs can only carry points");
                }
            }
        }
    }
}
