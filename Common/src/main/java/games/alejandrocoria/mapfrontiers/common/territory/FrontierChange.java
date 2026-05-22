package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class FrontierChange {
    private @Nullable NameChange name;
    private @Nullable VisibilityChange visibility;
    private @Nullable ColorChange color;
    private @Nullable BannerChange banner;
    private @Nullable ShapeChange shape;
    private @Nullable PathStyleChange pathStyle;
    private @Nullable CollectionIdChange collectionId;
    private @Nullable Long modifiedTime;

    public FrontierChange() {
    }

    public FrontierChange(FrontierChange other) {
        if (other.name != null) {
            name = new NameChange(other.name.name1, other.name.name2);
        }
        if (other.visibility != null) {
            visibility = new VisibilityChange(new VisibilityData(other.visibility.visibilityData));
        }
        if (other.color != null) {
            color = new ColorChange(other.color.color);
        }
        if (other.banner != null) {
            banner = new BannerChange(other.banner.banner == null ? null : new FrontierData.BannerData(other.banner.banner));
        }
        if (other.shape != null) {
            shape = new ShapeChange(other.shape.vertices, other.shape.chunks, other.shape.points, other.shape.frontierShape);
        }
        if (other.pathStyle != null) {
            pathStyle = new PathStyleChange(other.pathStyle.pathStyle);
        }
        if (other.collectionId != null) {
            collectionId = new CollectionIdChange(other.collectionId.collectionId);
        }
        modifiedTime = other.modifiedTime;
    }

    public FrontierChange(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            name = new NameChange(buf.readUtf(FrontierData.MAX_NAME_CHARACTERS), buf.readUtf(FrontierData.MAX_NAME_CHARACTERS));
        }

        if (buf.readBoolean()) {
            VisibilityData visibilityData = new VisibilityData();
            visibilityData.fromBytes(buf);
            visibility = new VisibilityChange(visibilityData);
        }

        if (buf.readBoolean()) {
            color = new ColorChange(buf.readInt());
        }

        if (buf.readBoolean()) {
            FrontierData.BannerData bannerData = null;
            if (buf.readBoolean()) {
                bannerData = new FrontierData.BannerData();
                bannerData.fromBytes(buf);
            }
            banner = new BannerChange(bannerData);
        }

        if (buf.readBoolean()) {
            FrontierShape frontierShape = FrontierShape.VALUES[buf.readInt()];
            List<BlockPos> vertices = new ArrayList<>();
            Set<ChunkPos> chunks = new HashSet<>();
            List<BlockPos> points = new ArrayList<>();

            switch (frontierShape) {
                case Vertex -> {
                    int verticesCount = buf.readInt();
                    vertices = new ArrayList<>(verticesCount);
                    for (int i = 0; i < verticesCount; ++i) {
                        vertices.add(BlockPos.of(buf.readLong()));
                    }
                }
                case Chunk -> {
                    int chunksCount = buf.readInt();
                    chunks = new HashSet<>(chunksCount);
                    for (int i = 0; i < chunksCount; ++i) {
                        chunks.add(ChunkPos.unpack(buf.readLong()));
                    }
                }
                case Path -> {
                    int pointsCount = buf.readInt();
                    points = new ArrayList<>(pointsCount);
                    for (int i = 0; i < pointsCount; ++i) {
                        points.add(BlockPos.of(buf.readLong()));
                    }
                }
            }

            shape = new ShapeChange(vertices, chunks, points, frontierShape);
        }

        if (buf.readBoolean()) {
            FrontierData.PathStyle value = new FrontierData.PathStyle();
            value.fromBytes(buf);
            pathStyle = new PathStyleChange(value);
        }

        if (buf.readBoolean()) {
            UUID value = null;
            if (buf.readBoolean()) {
                value = UUIDHelper.fromBytes(buf);
            }
            collectionId = new CollectionIdChange(value);
        }

        if (buf.readBoolean()) {
            modifiedTime = buf.readLong();
        }
    }

    public static FrontierChange fromMutation(FrontierData frontier, FrontierMutation mutation) {
        FrontierChange change = new FrontierChange();

        if (mutation.name1().isPresent() || mutation.name2().isPresent()) {
            String name1 = mutation.name1().orElse(frontier.getName1());
            String name2 = mutation.name2().orElse(frontier.getName2());
            if (!Objects.equals(frontier.getName1(), name1) || !Objects.equals(frontier.getName2(), name2)) {
                change.setName(name1, name2);
            }
        }

        mutation.color().filter(color -> color != frontier.getColor()).ifPresent(change::setColor);

        if (mutation.shape().isPresent()) {
            switch (mutation.shape().get().type()) {
                case VERTEX -> {
                    List<BlockPos> vertices = mutation.shape().get().vertices() == null
                            ? List.of()
                            : mutation.shape().get().vertices().stream()
                            .map(FrontierChange::toBlockPos)
                            .toList();
                    if (frontier.getShape() != FrontierShape.Vertex || !frontier.getVertices().equals(vertices)) {
                        change.setShape(vertices, Set.of(), List.of(), FrontierShape.Vertex);
                    }
                }
                case CHUNK -> {
                    Set<ChunkPos> chunks = mutation.shape().get().chunks() == null
                            ? Set.of()
                            : mutation.shape().get().chunks().stream()
                            .map(FrontierChange::toChunkPos)
                            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                    if (frontier.getShape() != FrontierShape.Chunk || !frontier.getChunks().equals(chunks)) {
                        change.setShape(List.of(), chunks, List.of(), FrontierShape.Chunk);
                    }
                }
                case PATH -> {
                    List<BlockPos> points = mutation.shape().get().points() == null
                            ? List.of()
                            : mutation.shape().get().points().stream()
                            .map(FrontierChange::toBlockPos)
                            .toList();
                    if (frontier.getShape() != FrontierShape.Path || !frontier.getPoints().equals(points)) {
                        change.setShape(List.of(), Set.of(), points, FrontierShape.Path);
                    }
                }
            }
        }

        if (mutation.visibility().isPresent() || !mutation.visibilityToAdd().isEmpty() || !mutation.visibilityToRemove().isEmpty()) {
            EnumSet<games.alejandrocoria.mapfrontiers.api.model.FrontierVisibilityFlag> visibility =
                    EnumSet.noneOf(games.alejandrocoria.mapfrontiers.api.model.FrontierVisibilityFlag.class);
            mutation.visibility().ifPresentOrElse(
                    visibility::addAll,
                    () -> visibility.addAll(FrontierMutationApplier.fromVisibility(frontier.getVisibilityData()))
            );
            visibility.addAll(mutation.visibilityToAdd());
            visibility.removeAll(mutation.visibilityToRemove());
            VisibilityData resolvedVisibility = FrontierMutationApplier.toVisibility(visibility);
            if (!frontier.getVisibilityData().equals(resolvedVisibility)) {
                change.setVisibility(resolvedVisibility);
            }
        }

        if (mutation.clearBanner()) {
            if (frontier.getbannerData() != null) {
                change.setBanner(null);
            }
        } else {
            mutation.banner().ifPresent(banner -> {
                FrontierData.BannerData resolvedBanner = FrontierMutationApplier.toBanner(banner);
                if (!Objects.equals(frontier.getbannerData(), resolvedBanner)) {
                    change.setBanner(resolvedBanner);
                }
            });
        }

        mutation.pathStyle().ifPresent(pathStyle -> {
            FrontierData.PathStyle resolvedPathStyle = FrontierMutationApplier.toPathStyle(pathStyle);
            if (!frontier.getPathStyle().equals(resolvedPathStyle)) {
                change.setPathStyle(resolvedPathStyle);
            }
        });

        if (mutation.clearCollection()) {
            if (frontier.getCollectionId() != null) {
                change.setCollectionId(null);
            }
        } else {
            mutation.collectionId().ifPresent(collectionId -> {
                if (!Objects.equals(frontier.getCollectionId(), collectionId.value())) {
                    change.setCollectionId(collectionId.value());
                }
            });
        }

        return change;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(name != null);
        if (name != null) {
            buf.writeUtf(name.name1, FrontierData.MAX_NAME_CHARACTERS);
            buf.writeUtf(name.name2, FrontierData.MAX_NAME_CHARACTERS);
        }

        buf.writeBoolean(visibility != null);
        if (visibility != null) {
            visibility.visibilityData.toBytes(buf);
        }

        buf.writeBoolean(color != null);
        if (color != null) {
            buf.writeInt(color.color);
        }

        buf.writeBoolean(banner != null);
        if (banner != null) {
            if (banner.banner == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                banner.banner.toBytes(buf);
            }
        }

        buf.writeBoolean(shape != null);
        if (shape != null) {
            buf.writeInt(shape.frontierShape.ordinal());

            switch (shape.frontierShape) {
                case Vertex -> {
                    buf.writeInt(shape.vertices.size());
                    for (BlockPos vertex : shape.vertices) {
                        buf.writeLong(vertex.asLong());
                    }
                }
                case Chunk -> {
                    buf.writeInt(shape.chunks.size());
                    for (ChunkPos chunk : shape.chunks) {
                        buf.writeLong(chunk.pack());
                    }
                }
                case Path -> {
                    buf.writeInt(shape.points.size());
                    for (BlockPos point : shape.points) {
                        buf.writeLong(point.asLong());
                    }
                }
            }
        }

        buf.writeBoolean(pathStyle != null);
        if (pathStyle != null) {
            pathStyle.pathStyle.toBytes(buf);
        }

        buf.writeBoolean(collectionId != null);
        if (collectionId != null) {
            if (collectionId.collectionId == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                UUIDHelper.toBytes(buf, collectionId.collectionId);
            }
        }

        buf.writeBoolean(modifiedTime != null);
        if (modifiedTime != null) {
            buf.writeLong(modifiedTime);
        }
    }

    public boolean isEmpty() {
        return name == null && visibility == null && color == null && banner == null && shape == null && pathStyle == null
                && collectionId == null && modifiedTime == null;
    }

    public @Nullable NameChange getName() {
        return name;
    }

    public @Nullable VisibilityChange getVisibility() {
        return visibility;
    }

    public @Nullable ColorChange getColor() {
        return color;
    }

    public @Nullable BannerChange getBanner() {
        return banner;
    }

    public @Nullable ShapeChange getShape() {
        return shape;
    }

    public @Nullable PathStyleChange getPathStyle() {
        return pathStyle;
    }

    public @Nullable CollectionIdChange getCollectionIdChange() {
        return collectionId;
    }

    public @Nullable Long getModifiedTime() {
        return modifiedTime;
    }

    public boolean hasNameChange() {
        return name != null;
    }

    public boolean hasVisibilityChange() {
        return visibility != null;
    }

    public boolean hasColorChange() {
        return color != null;
    }

    public boolean hasBannerChange() {
        return banner != null;
    }

    public boolean hasShapeChange() {
        return shape != null;
    }

    public boolean hasPathStyleChange() {
        return pathStyle != null;
    }

    public boolean hasCollectionIdChange() {
        return collectionId != null;
    }

    public boolean hasModifiedTime() {
        return modifiedTime != null;
    }

    public void setName(String name1, String name2) {
        name = new NameChange(name1, name2);
    }

    public void setVisibility(VisibilityData visibilityData) {
        visibility = new VisibilityChange(new VisibilityData(visibilityData));
    }

    public void setColor(int color) {
        this.color = new ColorChange(color);
    }

    public void setBanner(@Nullable FrontierData.BannerData banner) {
        this.banner = new BannerChange(banner == null ? null : new FrontierData.BannerData(banner));
    }

    public void setShape(List<BlockPos> vertices, Set<ChunkPos> chunks, List<BlockPos> points, FrontierShape frontierShape) {
        shape = new ShapeChange(vertices, chunks, points, frontierShape);
    }

    public void setPathStyle(FrontierData.PathStyle pathStyle) {
        this.pathStyle = new PathStyleChange(pathStyle);
    }

    public void setCollectionId(@Nullable UUID collectionId) {
        this.collectionId = new CollectionIdChange(collectionId);
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public static class NameChange {
        private final String name1;
        private final String name2;

        private NameChange(String name1, String name2) {
            this.name1 = name1;
            this.name2 = name2;
        }

        public String getName1() {
            return name1;
        }

        public String getName2() {
            return name2;
        }
    }

    public static class VisibilityChange {
        private final VisibilityData visibilityData;

        private VisibilityChange(VisibilityData visibilityData) {
            this.visibilityData = visibilityData;
        }

        public VisibilityData getVisibilityData() {
            return new VisibilityData(visibilityData);
        }
    }

    public static class ColorChange {
        private final int color;

        private ColorChange(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    public static class BannerChange {
        private final @Nullable FrontierData.BannerData banner;

        private BannerChange(@Nullable FrontierData.BannerData banner) {
            this.banner = banner;
        }

        public @Nullable FrontierData.BannerData getBanner() {
            return banner == null ? null : new FrontierData.BannerData(banner);
        }
    }

    public static class ShapeChange {
        private final List<BlockPos> vertices;
        private final Set<ChunkPos> chunks;
        private final List<BlockPos> points;
        private final FrontierShape frontierShape;

        private ShapeChange(List<BlockPos> vertices, Set<ChunkPos> chunks, List<BlockPos> points, FrontierShape frontierShape) {
            this.vertices = new ArrayList<>(vertices);
            this.chunks = new HashSet<>(chunks);
            this.points = new ArrayList<>(points);
            this.frontierShape = frontierShape;
        }

        public List<BlockPos> getVertices() {
            return new ArrayList<>(vertices);
        }

        public Set<ChunkPos> getChunks() {
            return new HashSet<>(chunks);
        }

        public List<BlockPos> getPoints() {
            return new ArrayList<>(points);
        }

        public FrontierShape getShape() {
            return frontierShape;
        }
    }

    public static class CollectionIdChange {
        private final @Nullable UUID collectionId;

        private CollectionIdChange(@Nullable UUID collectionId) {
            this.collectionId = collectionId;
        }

        public @Nullable UUID getCollectionId() {
            return collectionId;
        }
    }

    public static class PathStyleChange {
        private final FrontierData.PathStyle pathStyle;

        private PathStyleChange(FrontierData.PathStyle pathStyle) {
            this.pathStyle = new FrontierData.PathStyle(pathStyle);
        }

        public FrontierData.PathStyle getPathStyle() {
            return new FrontierData.PathStyle(pathStyle);
        }
    }

    private static BlockPos toBlockPos(Point2i point) {
        return new BlockPos(point.x(), 0, point.z());
    }

    private static ChunkPos toChunkPos(ChunkCoord chunk) {
        return new ChunkPos(chunk.x(), chunk.z());
    }
}
