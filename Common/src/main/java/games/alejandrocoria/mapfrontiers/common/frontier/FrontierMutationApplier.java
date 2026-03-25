package games.alejandrocoria.mapfrontiers.common.frontier;

import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.FrontierBanner;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShapeType;
import games.alejandrocoria.mapfrontiers.api.model.FrontierVisibilityFlag;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public final class FrontierMutationApplier {
    private FrontierMutationApplier() {
    }

    public static void applyCreationShape(FrontierData frontier,
                                          @Nullable List<BlockPos> vertices,
                                          @Nullable List<ChunkPos> chunks) {
        if (vertices != null) {
            frontier.setMode(FrontierData.Mode.Vertex);
            for (BlockPos vertex : vertices) {
                frontier.addVertex(vertex);
            }
        }

        if (chunks != null) {
            frontier.setMode(FrontierData.Mode.Chunk);
            for (ChunkPos chunk : chunks) {
                frontier.toggleChunk(chunk);
            }
        }
    }

    public static void applyShape(FrontierData frontier, FrontierShape shape) {
        frontier.clearVertices();
        frontier.clearChunks();

        if (shape.type() == FrontierShapeType.VERTEX) {
            frontier.setMode(FrontierData.Mode.Vertex);
            if (shape.vertices() != null) {
                for (Point2i vertex : shape.vertices()) {
                    frontier.addVertex(new BlockPos(vertex.x(), 0, vertex.z()));
                }
            }
        } else {
            frontier.setMode(FrontierData.Mode.Chunk);
            if (shape.chunks() != null) {
                for (ChunkCoord chunk : shape.chunks()) {
                    frontier.addChunk(new ChunkPos(chunk.x(), chunk.z()));
                }
            }
        }
    }

    public static void applyMutation(FrontierData frontier, FrontierMutation mutation) {
        mutation.name1().ifPresent(frontier::setName1);
        mutation.name2().ifPresent(frontier::setName2);
        mutation.color().ifPresent(frontier::setColor);
        mutation.shape().ifPresent(value -> applyShape(frontier, value));

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
    }

    public static Set<FrontierVisibilityFlag> fromVisibility(FrontierData.VisibilityData visibilityData) {
        EnumSet<FrontierVisibilityFlag> visibility = EnumSet.noneOf(FrontierVisibilityFlag.class);
        for (FrontierData.VisibilityData.Visibility value : FrontierData.VisibilityData.Visibility.values()) {
            if (visibilityData.getValue(value)) {
                visibility.add(FrontierVisibilityFlag.valueOf(value.name()));
            }
        }
        return visibility;
    }

    public static FrontierData.VisibilityData toVisibility(Set<FrontierVisibilityFlag> visibilityFlags) {
        FrontierData.VisibilityData visibilityData = new FrontierData.VisibilityData(false);
        for (FrontierVisibilityFlag flag : visibilityFlags) {
            visibilityData.setValue(FrontierData.VisibilityData.Visibility.valueOf(flag.name()), true);
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
}
