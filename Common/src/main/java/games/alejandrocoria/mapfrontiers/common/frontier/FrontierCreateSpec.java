package games.alejandrocoria.mapfrontiers.common.frontier;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
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
    private final FrontierData.FrontierLifetime lifetime;
    private final @Nullable UUID collectionId;
    private final @Nullable String sourcePluginId;
    private final String name1;
    private final String name2;
    private final int color;
    private final FrontierData.VisibilityData visibility;
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
                               FrontierData.FrontierLifetime lifetime,
                               @Nullable UUID collectionId,
                               @Nullable String sourcePluginId,
                               String name1,
                               String name2,
                               int color,
                               FrontierData.VisibilityData visibility,
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
        this.sourcePluginId = sourcePluginId;
        this.name1 = Objects.requireNonNull(name1, "name1");
        this.name2 = Objects.requireNonNull(name2, "name2");
        this.color = color;
        this.visibility = new FrontierData.VisibilityData(Objects.requireNonNull(visibility, "visibility"));
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
                                            FrontierData.FrontierLifetime lifetime,
                                            @Nullable UUID collectionId,
                                            @Nullable String sourcePluginId,
                                            String name1,
                                            String name2,
                                            int color,
                                            FrontierData.VisibilityData visibility,
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
                                           FrontierData.FrontierLifetime lifetime,
                                           @Nullable UUID collectionId,
                                           @Nullable String sourcePluginId,
                                           String name1,
                                           String name2,
                                           int color,
                                           FrontierData.VisibilityData visibility,
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
                                          FrontierData.FrontierLifetime lifetime,
                                          @Nullable UUID collectionId,
                                          @Nullable String sourcePluginId,
                                          String name1,
                                          String name2,
                                          int color,
                                          FrontierData.VisibilityData visibility,
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

    public FrontierData.FrontierLifetime getLifetime() {
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

    public FrontierData.VisibilityData getVisibility() {
        return new FrontierData.VisibilityData(visibility);
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

    private static SettingsUser copyUser(SettingsUser owner) {
        SettingsUser copy = new SettingsUser();
        copy.username = owner.username;
        copy.uuid = owner.uuid;
        return copy;
    }

    private static void validateTypeAndLifetime(boolean personal, FrontierData.FrontierLifetime lifetime) {
        if (!personal && lifetime == FrontierData.FrontierLifetime.SESSION_ONLY) {
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
