package games.alejandrocoria.mapfrontiers.common.api;

import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.CollectionDataView;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.CollectionMutation;
import games.alejandrocoria.mapfrontiers.api.model.CollectionVisibilityFlag;
import games.alejandrocoria.mapfrontiers.api.model.CollectionVisibilitySettings;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.EntityLifetime;
import games.alejandrocoria.mapfrontiers.api.model.FrontierBanner;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.FrontierType;
import games.alejandrocoria.mapfrontiers.api.model.FrontierVisibilityFlag;
import games.alejandrocoria.mapfrontiers.api.model.PathMarkerId;
import games.alejandrocoria.mapfrontiers.api.model.PathStyle;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.api.model.SharedUserAccess;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityField;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierMutationApplier;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ApiConverters {
    public static DimensionId fromDimension(ResourceKey<Level> dimension) {
        return new DimensionId(dimension.identifier().toString());
    }

    public static ResourceKey<Level> toDimension(DimensionId dimension) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension.value()));
    }

    public static FrontierShape toShape(FrontierData frontier) {
        List<Point2i> vertices = frontier.getVertices().stream().map(pos -> new Point2i(pos.getX(), pos.getZ())).toList();
        List<ChunkCoord> chunks = frontier.getChunks().stream().map(chunk -> new ChunkCoord(chunk.x(), chunk.z())).toList();
        List<Point2i> points = frontier.getPoints().stream().map(pos -> new Point2i(pos.getX(), pos.getZ())).toList();

        return switch (frontier.getShape()) {
            case Vertex -> games.alejandrocoria.mapfrontiers.api.model.FrontierShape.vertex(vertices);
            case Chunk -> games.alejandrocoria.mapfrontiers.api.model.FrontierShape.chunk(chunks);
            case Path -> games.alejandrocoria.mapfrontiers.api.model.FrontierShape.path(points);
        };
    }

    public static Set<FrontierVisibilityFlag> fromFrontierVisibility(FrontierVisibilityData visibilityData) {
        return FrontierMutationApplier.fromVisibility(visibilityData);
    }

    public static FrontierVisibilityData toFrontierVisibility(Set<FrontierVisibilityFlag> visibilityFlags) {
        return FrontierMutationApplier.toVisibility(visibilityFlags);
    }

    public static FrontierBanner fromBanner(BannerData bannerData) {
        if (bannerData == null) {
            return null;
        }

        ListTag patterns = BannerData.normalizePatterns(bannerData.patterns);

        return new FrontierBanner(
                bannerData.baseColor.getId(),
                patterns == null ? "[]" : patterns.toString(),
                bannerData.rotation
        );
    }

    public static BannerData toBanner(@Nullable FrontierBanner banner) {
        return FrontierMutationApplier.toBanner(banner);
    }

    public static PathStyle fromPathStyle(FrontierData.PathStyle pathStyle) {
        FrontierData.PathStyle normalized = new FrontierData.PathStyle(pathStyle);
        normalized.normalizeForPersistence();

        return new PathStyle(
                new PathMarkerId(normalized.startMarker.toString()),
                new PathMarkerId(normalized.innerMarker.toString()),
                new PathMarkerId(normalized.endMarker.toString()),
                new PathMarkerId(normalized.segmentMarker.toString()),
                normalized.labelAtStart,
                normalized.labelAtMiddle,
                normalized.labelAtEnd
        );
    }

    public static FrontierData.PathStyle toPathStyle(PathStyle pathStyle) {
        return FrontierMutationApplier.toPathStyle(pathStyle);
    }

    public static SharedUserAccess fromSharedUser(SettingsUserShared userShared) {
        EnumSet<FrontierSharePermission> permissions = EnumSet.noneOf(FrontierSharePermission.class);
        for (SettingsUserShared.Action action : userShared.getActions()) {
            permissions.add(toFrontierSharePermission(action));
        }

        return new SharedUserAccess(fromUser(userShared.getUser()), permissions, userShared.isPending());
    }

    public static CollectionVisibilitySettings fromCollectionVisibility(CollectionVisibilityData visibilityData) {
        return CollectionVisibilitySettings.builder()
                .visible(visibilityData.isVisible())
                .fullscreenZoom(visibilityData.getFullscreenZoom())
                .minimapZoom(visibilityData.getMinimapZoom())
                .webmapZoom(visibilityData.getWebmapZoom())
                .fullscreenName(visibilityData.getFullscreenName())
                .fullscreenOwner(visibilityData.getFullscreenOwner())
                .fullscreenBanner(visibilityData.getFullscreenBanner())
                .minimapName(visibilityData.getMinimapName())
                .minimapOwner(visibilityData.getMinimapOwner())
                .minimapBanner(visibilityData.getMinimapBanner())
                .webmapName(visibilityData.getWebmapName())
                .webmapOwner(visibilityData.getWebmapOwner())
                .webmapBanner(visibilityData.getWebmapBanner())
                .build();
    }

    public static CollectionVisibilityData toCollectionVisibility(CollectionVisibilitySettings visibility) {
        CollectionVisibilityData visibilityData = new CollectionVisibilityData();
        visibilityData.setVisible(visibility.visible());
        visibilityData.setFullscreenZoom(visibility.fullscreenZoom());
        visibilityData.setMinimapZoom(visibility.minimapZoom());
        visibilityData.setWebmapZoom(visibility.webmapZoom());
        visibilityData.setFullscreenName(visibility.fullscreenName());
        visibilityData.setFullscreenOwner(visibility.fullscreenOwner());
        visibilityData.setFullscreenBanner(visibility.fullscreenBanner());
        visibilityData.setMinimapName(visibility.minimapName());
        visibilityData.setMinimapOwner(visibility.minimapOwner());
        visibilityData.setMinimapBanner(visibility.minimapBanner());
        visibilityData.setWebmapName(visibility.webmapName());
        visibilityData.setWebmapOwner(visibility.webmapOwner());
        visibilityData.setWebmapBanner(visibility.webmapBanner());
        return visibilityData;
    }

    public static CollectionVisibilityData defaultCollectionVisibility() {
        return new CollectionData().getVisibilityData();
    }

    public static BannerData defaultCollectionBanner() {
        return new CollectionData().getBannerData();
    }

    private static void addCollectionVisibilityFlags(CollectionVisibilityData visibilityData,
                                                     Set<CollectionVisibilityFlag> visibilityFlags) {
        setCollectionVisibilityFlags(visibilityData, visibilityFlags, true);
    }

    private static void removeCollectionVisibilityFlags(CollectionVisibilityData visibilityData,
                                                        Set<CollectionVisibilityFlag> visibilityFlags) {
        setCollectionVisibilityFlags(visibilityData, visibilityFlags, false);
    }

    public static CollectionDataView fromCollection(CollectionData collection) {
        return new CollectionDataView(
                new CollectionId(collection.getId()),
                collection.getPersonal() ? FrontierType.PERSONAL : FrontierType.GLOBAL,
                fromLifetime(collection.getLifetime()),
                fromUser(collection.getOwner()),
                collection.getName(),
                collection.getColor(),
                fromCollectionVisibility(collection.getVisibilityData()),
                fromBanner(collection.getBannerData()),
                Optional.ofNullable(collection.getSourcePluginId())
        );
    }

    public static FrontierDataView fromFrontier(FrontierData frontier) {
        UserRef owner = fromUser(frontier.getOwner());
        List<SharedUserAccess> sharedUsers = new ArrayList<>();
        if (frontier.getUsersShared() != null) {
            for (SettingsUserShared userShared : frontier.getUsersShared()) {
                sharedUsers.add(fromSharedUser(userShared));
            }
        }

        return new FrontierDataView(
                new FrontierId(frontier.getId()),
                frontier.getPersonal() ? FrontierType.PERSONAL : FrontierType.GLOBAL,
                fromLifetime(frontier.getLifetime()),
                fromDimension(frontier.getDimension()),
                frontier.getColor(),
                frontier.getName1(),
                frontier.getName2(),
                toShape(frontier),
                fromFrontierVisibility(frontier.getVisibilityData()),
                fromBanner(frontier.getBannerData()),
                frontier.getShape() == games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape.Path ? Optional.of(fromPathStyle(frontier.getPathStyle())) : Optional.empty(),
                Optional.ofNullable(frontier.getCollectionId()).map(CollectionId::new),
                Optional.ofNullable(frontier.getSourcePluginId()),
                owner,
                sharedUsers
        );
    }

    public static EntityLifetime fromLifetime(TerritoryLifetime lifetime) {
        return switch (lifetime) {
            case PERSISTENT -> EntityLifetime.PERSISTENT;
            case SESSION_ONLY -> EntityLifetime.SESSION_ONLY;
        };
    }

    public static TerritoryLifetime toLifetime(EntityLifetime lifetime) {
        EntityLifetime checkedLifetime = lifetime == null ? EntityLifetime.PERSISTENT : lifetime;
        return switch (checkedLifetime) {
            case PERSISTENT -> TerritoryLifetime.PERSISTENT;
            case SESSION_ONLY -> TerritoryLifetime.SESSION_ONLY;
        };
    }

    public static UserRef fromUser(SettingsUser user) {
        return new UserRef(user.uuid, user.username);
    }

    public static SettingsUser toUser(UserRef user) {
        SettingsUser result = new SettingsUser();
        result.uuid = user.id();
        result.username = user.name() == null ? "" : user.name();
        return result;
    }

    public static FrontierSharePermission toFrontierSharePermission(SettingsUserShared.Action action) {
        return switch (action) {
            case UpdateFrontier -> FrontierSharePermission.UpdateFrontier;
            case UpdateSettings -> FrontierSharePermission.UpdateSettings;
        };
    }

    public static SettingsUserShared.Action toSharedUserAction(FrontierSharePermission permission) {
        return switch (permission) {
            case UpdateFrontier -> SettingsUserShared.Action.UpdateFrontier;
            case UpdateSettings -> SettingsUserShared.Action.UpdateSettings;
        };
    }

    public static void applyMutation(FrontierData frontier, FrontierMutation mutation) {
        FrontierMutationApplier.applyMutation(frontier, mutation);
    }

    public static void applyCollectionMutation(CollectionData collection, CollectionMutation mutation) {
        mutation.name().ifPresent(collection::setName);
        mutation.color().ifPresent(collection::setColor);
        if (mutation.visibility().isPresent()) {
            collection.setVisibilityData(toCollectionVisibility(mutation.visibility().get()));
        } else if (!mutation.visibilityToAdd().isEmpty()
                || !mutation.visibilityToRemove().isEmpty()
                || mutation.fullscreenZoom().isPresent()
                || mutation.minimapZoom().isPresent()
                || mutation.webmapZoom().isPresent()) {
            CollectionVisibilityData visibilityData = new CollectionVisibilityData(collection.getVisibilityData());
            addCollectionVisibilityFlags(visibilityData, mutation.visibilityToAdd());
            removeCollectionVisibilityFlags(visibilityData, mutation.visibilityToRemove());
            mutation.fullscreenZoom().ifPresent(visibilityData::setFullscreenZoom);
            mutation.minimapZoom().ifPresent(visibilityData::setMinimapZoom);
            mutation.webmapZoom().ifPresent(visibilityData::setWebmapZoom);
            collection.setVisibilityData(visibilityData);
        }
        if (mutation.clearBanner()) {
            collection.setBannerData(null);
        } else {
            mutation.banner().ifPresent(value -> collection.setBannerData(toBanner(value)));
        }
    }

    private static void setCollectionVisibilityFlags(CollectionVisibilityData visibilityData,
                                                     Set<CollectionVisibilityFlag> visibilityFlags,
                                                     boolean enabled) {
        for (CollectionVisibilityFlag visibilityFlag : visibilityFlags) {
            visibilityData.setBoolean(toCollectionVisibilityField(visibilityFlag), enabled);
        }
    }

    private static CollectionVisibilityField toCollectionVisibilityField(CollectionVisibilityFlag visibilityFlag) {
        return switch (visibilityFlag) {
            case Visible -> CollectionVisibilityField.Visible;
            case FullscreenName -> CollectionVisibilityField.FullscreenName;
            case FullscreenOwner -> CollectionVisibilityField.FullscreenOwner;
            case FullscreenBanner -> CollectionVisibilityField.FullscreenBanner;
            case MinimapName -> CollectionVisibilityField.MinimapName;
            case MinimapOwner -> CollectionVisibilityField.MinimapOwner;
            case MinimapBanner -> CollectionVisibilityField.MinimapBanner;
            case WebmapName -> CollectionVisibilityField.WebmapName;
            case WebmapOwner -> CollectionVisibilityField.WebmapOwner;
            case WebmapBanner -> CollectionVisibilityField.WebmapBanner;
        };
    }

    private ApiConverters() {
    }
}
