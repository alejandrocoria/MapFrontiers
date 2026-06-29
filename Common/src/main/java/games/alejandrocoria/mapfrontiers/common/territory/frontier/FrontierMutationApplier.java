package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import com.mojang.brigadier.StringReader;
import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.FrontierBanner;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierVisibilityFlag;
import games.alejandrocoria.mapfrontiers.api.model.PathStyle;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
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
                frontier.setShape(games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape.Vertex);
                if (shape.vertices() != null) {
                    for (Point2i vertex : shape.vertices()) {
                        frontier.addVertex(new BlockPos(vertex.x(), 0, vertex.z()));
                    }
                }
            }
            case CHUNK -> {
                frontier.setShape(games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape.Chunk);
                if (shape.chunks() != null) {
                    for (ChunkCoord chunk : shape.chunks()) {
                        frontier.addChunk(new ChunkPos(chunk.x(), chunk.z()));
                    }
                }
            }
            case PATH -> {
                frontier.setShape(games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape.Path);
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
        if (frontier.getShape() != games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape.Path) {
            throw new IllegalArgumentException("Path style can only be applied to path frontiers");
        }

        frontier.setPathStyle(toPathStyle(pathStyle));
    }

    public static Set<FrontierVisibilityFlag> fromVisibility(FrontierVisibilityData visibilityData) {
        EnumSet<FrontierVisibilityFlag> visibility = EnumSet.noneOf(FrontierVisibilityFlag.class);
        for (FrontierVisibility value : FrontierVisibility.VALUES) {
            if (visibilityData.get(value)) {
                visibility.add(toVisibilityFlag(value));
            }
        }
        return visibility;
    }

    public static FrontierVisibilityData toVisibility(Set<FrontierVisibilityFlag> visibilityFlags) {
        FrontierVisibilityData visibilityData = new FrontierVisibilityData(false);
        for (FrontierVisibilityFlag flag : visibilityFlags) {
            visibilityData.set(toFrontierVisibility(flag), true);
        }
        return visibilityData;
    }

    private static FrontierVisibilityFlag toVisibilityFlag(FrontierVisibility visibility) {
        return switch (visibility) {
            case Frontier -> FrontierVisibilityFlag.Frontier;
            case AnnounceInChat -> FrontierVisibilityFlag.AnnounceInChat;
            case AnnounceInTitle -> FrontierVisibilityFlag.AnnounceInTitle;
            case MentionCollection -> FrontierVisibilityFlag.MentionCollection;
            case Fullscreen -> FrontierVisibilityFlag.Fullscreen;
            case FullscreenName -> FrontierVisibilityFlag.FullscreenName;
            case FullscreenCollection -> FrontierVisibilityFlag.FullscreenCollection;
            case FullscreenOwner -> FrontierVisibilityFlag.FullscreenOwner;
            case FullscreenBanner -> FrontierVisibilityFlag.FullscreenBanner;
            case FullscreenDay -> FrontierVisibilityFlag.FullscreenDay;
            case FullscreenNight -> FrontierVisibilityFlag.FullscreenNight;
            case FullscreenUnderground -> FrontierVisibilityFlag.FullscreenUnderground;
            case FullscreenTopo -> FrontierVisibilityFlag.FullscreenTopo;
            case FullscreenBiome -> FrontierVisibilityFlag.FullscreenBiome;
            case Minimap -> FrontierVisibilityFlag.Minimap;
            case MinimapName -> FrontierVisibilityFlag.MinimapName;
            case MinimapCollection -> FrontierVisibilityFlag.MinimapCollection;
            case MinimapOwner -> FrontierVisibilityFlag.MinimapOwner;
            case MinimapBanner -> FrontierVisibilityFlag.MinimapBanner;
            case MinimapDay -> FrontierVisibilityFlag.MinimapDay;
            case MinimapNight -> FrontierVisibilityFlag.MinimapNight;
            case MinimapUnderground -> FrontierVisibilityFlag.MinimapUnderground;
            case MinimapTopo -> FrontierVisibilityFlag.MinimapTopo;
            case MinimapBiome -> FrontierVisibilityFlag.MinimapBiome;
            case Webmap -> FrontierVisibilityFlag.Webmap;
            case WebmapName -> FrontierVisibilityFlag.WebmapName;
            case WebmapCollection -> FrontierVisibilityFlag.WebmapCollection;
            case WebmapOwner -> FrontierVisibilityFlag.WebmapOwner;
            case WebmapBanner -> FrontierVisibilityFlag.WebmapBanner;
            case WebmapDay -> FrontierVisibilityFlag.WebmapDay;
            case WebmapNight -> FrontierVisibilityFlag.WebmapNight;
            case WebmapUnderground -> FrontierVisibilityFlag.WebmapUnderground;
            case WebmapTopo -> FrontierVisibilityFlag.WebmapTopo;
            case WebmapBiome -> FrontierVisibilityFlag.WebmapBiome;
        };
    }

    private static FrontierVisibility toFrontierVisibility(FrontierVisibilityFlag visibilityFlag) {
        return switch (visibilityFlag) {
            case Frontier -> FrontierVisibility.Frontier;
            case AnnounceInChat -> FrontierVisibility.AnnounceInChat;
            case AnnounceInTitle -> FrontierVisibility.AnnounceInTitle;
            case MentionCollection -> FrontierVisibility.MentionCollection;
            case Fullscreen -> FrontierVisibility.Fullscreen;
            case FullscreenName -> FrontierVisibility.FullscreenName;
            case FullscreenCollection -> FrontierVisibility.FullscreenCollection;
            case FullscreenOwner -> FrontierVisibility.FullscreenOwner;
            case FullscreenBanner -> FrontierVisibility.FullscreenBanner;
            case FullscreenDay -> FrontierVisibility.FullscreenDay;
            case FullscreenNight -> FrontierVisibility.FullscreenNight;
            case FullscreenUnderground -> FrontierVisibility.FullscreenUnderground;
            case FullscreenTopo -> FrontierVisibility.FullscreenTopo;
            case FullscreenBiome -> FrontierVisibility.FullscreenBiome;
            case Minimap -> FrontierVisibility.Minimap;
            case MinimapName -> FrontierVisibility.MinimapName;
            case MinimapCollection -> FrontierVisibility.MinimapCollection;
            case MinimapOwner -> FrontierVisibility.MinimapOwner;
            case MinimapBanner -> FrontierVisibility.MinimapBanner;
            case MinimapDay -> FrontierVisibility.MinimapDay;
            case MinimapNight -> FrontierVisibility.MinimapNight;
            case MinimapUnderground -> FrontierVisibility.MinimapUnderground;
            case MinimapTopo -> FrontierVisibility.MinimapTopo;
            case MinimapBiome -> FrontierVisibility.MinimapBiome;
            case Webmap -> FrontierVisibility.Webmap;
            case WebmapName -> FrontierVisibility.WebmapName;
            case WebmapCollection -> FrontierVisibility.WebmapCollection;
            case WebmapOwner -> FrontierVisibility.WebmapOwner;
            case WebmapBanner -> FrontierVisibility.WebmapBanner;
            case WebmapDay -> FrontierVisibility.WebmapDay;
            case WebmapNight -> FrontierVisibility.WebmapNight;
            case WebmapUnderground -> FrontierVisibility.WebmapUnderground;
            case WebmapTopo -> FrontierVisibility.WebmapTopo;
            case WebmapBiome -> FrontierVisibility.WebmapBiome;
        };
    }

    public static BannerData toBanner(@Nullable FrontierBanner banner) {
        if (banner == null) {
            return null;
        }

        BannerData data = new BannerData();
        data.baseColor = DyeColor.byId(banner.baseColorId());
        try {
            Tag parsed = new TagParser(new StringReader(banner.patternsNbt())).readValue();
            if (parsed instanceof ListTag listTag) {
                data.patterns = BannerData.normalizePatterns(listTag);
            }
        } catch (Exception ignored) {
        }
        data.rotation = banner.rotation();
        return data;
    }

    public static FrontierData.PathStyle toPathStyle(PathStyle pathStyle) {
        FrontierData.PathStyle result = new FrontierData.PathStyle();
        result.startMarker = ResourceLocation.parse(pathStyle.startMarker().value());
        result.innerMarker = ResourceLocation.parse(pathStyle.innerMarker().value());
        result.endMarker = ResourceLocation.parse(pathStyle.endMarker().value());
        result.segmentMarker = ResourceLocation.parse(pathStyle.segmentMarker().value());
        result.labelAtStart = pathStyle.labelAtStart();
        result.labelAtMiddle = pathStyle.labelAtMiddle();
        result.labelAtEnd = pathStyle.labelAtEnd();
        result.normalizeForPersistence();
        return result;
    }

    private FrontierMutationApplier() {
    }
}
