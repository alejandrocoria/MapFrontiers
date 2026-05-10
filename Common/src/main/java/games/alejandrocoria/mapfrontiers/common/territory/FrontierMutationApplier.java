package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.FrontierBanner;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierVisibilityFlag;
import games.alejandrocoria.mapfrontiers.api.model.PathStyle;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.Set;

@ParametersAreNonnullByDefault
public final class FrontierMutationApplier {
    public static void applyShape(FrontierData frontier, FrontierShape shape) {
        frontier.clearVertices();
        frontier.clearChunks();
        frontier.clearPoints();

        switch (shape.type()) {
            case VERTEX -> {
                frontier.setMode(FrontierData.Mode.Vertex);
                if (shape.vertices() != null) {
                    for (Point2i vertex : shape.vertices()) {
                        frontier.addVertex(new BlockPos(vertex.x(), 0, vertex.z()));
                    }
                }
            }
            case CHUNK -> {
                frontier.setMode(FrontierData.Mode.Chunk);
                if (shape.chunks() != null) {
                    for (ChunkCoord chunk : shape.chunks()) {
                        frontier.addChunk(new ChunkPos(chunk.x(), chunk.z()));
                    }
                }
            }
            case PATH -> {
                frontier.setMode(FrontierData.Mode.Path);
                if (shape.points() != null) {
                    for (Point2i point : shape.points()) {
                        frontier.addPoint(new BlockPos(point.x(), 0, point.z()));
                    }
                }
            }
        }
    }

    public static void applyMutation(FrontierData frontier, FrontierMutation mutation) {
        mutation.name1().ifPresent(frontier::setName1);
        mutation.name2().ifPresent(frontier::setName2);
        mutation.color().ifPresent(frontier::setColor);
        mutation.shape().ifPresent(value -> applyShape(frontier, value));
        mutation.pathStyle().ifPresent(value -> applyPathStyle(frontier, value));

        if (mutation.visibility().isPresent() || !mutation.visibilityToAdd().isEmpty() || !mutation.visibilityToRemove().isEmpty()) {
            EnumSet<FrontierVisibilityFlag> visibility = EnumSet.noneOf(FrontierVisibilityFlag.class);
            mutation.visibility().ifPresentOrElse(
                    visibility::addAll,
                    () -> visibility.addAll(fromVisibility(frontier.getVisibilityData()))
            );
            visibility.addAll(mutation.visibilityToAdd());
            visibility.removeAll(mutation.visibilityToRemove());
            frontier.setVisibilityData(toVisibility(visibility));
        }

        if (mutation.clearBanner()) {
            frontier.setBannerData(null);
        } else {
            mutation.banner().ifPresent(value -> frontier.setBannerData(toBanner(value)));
        }

        if (mutation.clearCollection()) {
            frontier.setCollectionId(null);
        } else {
            mutation.collectionId().ifPresent(value -> frontier.setCollectionId(value.value()));
        }
    }

    private static void applyPathStyle(FrontierData frontier, PathStyle pathStyle) {
        if (frontier.getMode() != FrontierData.Mode.Path) {
            throw new IllegalArgumentException("Path style can only be applied to path frontiers");
        }

        frontier.setPathStyle(toPathStyle(pathStyle));
    }

    public static Set<FrontierVisibilityFlag> fromVisibility(VisibilityData visibilityData) {
        EnumSet<FrontierVisibilityFlag> visibility = EnumSet.noneOf(FrontierVisibilityFlag.class);
        for (FrontierVisibility value : FrontierVisibility.VALUES) {
            if (visibilityData.getValue(value)) {
                visibility.add(FrontierVisibilityFlag.valueOf(value.name()));
            }
        }
        return visibility;
    }

    public static VisibilityData toVisibility(Set<FrontierVisibilityFlag> visibilityFlags) {
        VisibilityData visibilityData = new VisibilityData(false);
        for (FrontierVisibilityFlag flag : visibilityFlags) {
            visibilityData.setValue(FrontierVisibility.valueOf(flag.name()), true);
        }
        return visibilityData;
    }

    public static FrontierData.BannerData toBanner(@Nullable FrontierBanner banner) {
        if (banner == null) {
            return null;
        }

        FrontierData.BannerData data = new FrontierData.BannerData();
        data.baseColor = DyeColor.byId(banner.baseColorId());
        try {
            Object parsed = TagParser.create(NbtOps.INSTANCE).parseFully(banner.patternsNbt());
            if (parsed instanceof ListTag listTag) {
                data.patterns = FrontierData.BannerData.normalizePatterns(listTag);
            }
        } catch (Exception ignored) {
        }
        data.rotation = banner.rotation();
        return data;
    }

    public static FrontierData.PathStyle toPathStyle(PathStyle pathStyle) {
        FrontierData.PathStyle result = new FrontierData.PathStyle();
        result.startMarker = Identifier.parse(pathStyle.startMarker().value());
        result.innerMarker = Identifier.parse(pathStyle.innerMarker().value());
        result.endMarker = Identifier.parse(pathStyle.endMarker().value());
        result.segmentMarker = Identifier.parse(pathStyle.segmentMarker().value());
        result.labelAtStart = pathStyle.labelAtStart();
        result.labelAtMiddle = pathStyle.labelAtMiddle();
        result.labelAtEnd = pathStyle.labelAtEnd();
        result.normalizeForPersistence();
        return result;
    }

    private FrontierMutationApplier() {
    }
}
