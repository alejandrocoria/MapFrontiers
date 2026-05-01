package games.alejandrocoria.mapfrontiers.common.api;

import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.CollectionDataView;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.CollectionMutation;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierBanner;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierLifetime;
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
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierMutationApplier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
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
    private ApiConverters() {
    }

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

        return switch (frontier.getMode()) {
            case Vertex -> FrontierShape.vertex(vertices);
            case Chunk -> FrontierShape.chunk(chunks);
            case Path -> FrontierShape.path(points);
        };
    }

    public static void applyShape(FrontierData frontier, FrontierShape shape) {
        FrontierMutationApplier.applyShape(frontier, shape);
    }

    public static Set<FrontierVisibilityFlag> fromVisibility(FrontierData.VisibilityData visibilityData) {
        return FrontierMutationApplier.fromVisibility(visibilityData);
    }

    public static FrontierData.VisibilityData toVisibility(Set<FrontierVisibilityFlag> visibilityFlags) {
        return FrontierMutationApplier.toVisibility(visibilityFlags);
    }

    public static FrontierBanner fromBanner(FrontierData.BannerData bannerData) {
        if (bannerData == null) {
            return null;
        }

        ListTag patterns = FrontierData.BannerData.normalizePatterns(bannerData.patterns);

        return new FrontierBanner(
                bannerData.baseColor.getId(),
                patterns == null ? "[]" : patterns.toString(),
                bannerData.rotation
        );
    }

    public static FrontierData.BannerData toBanner(@Nullable FrontierBanner banner) {
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
            permissions.add(FrontierSharePermission.valueOf(action.name()));
        }

        return new SharedUserAccess(fromUser(userShared.getUser()), permissions, userShared.isPending());
    }

    public static CollectionDataView fromCollection(CollectionData collection) {
        return new CollectionDataView(
                new CollectionId(collection.getId()),
                collection.getPersonal() ? FrontierType.PERSONAL : FrontierType.GLOBAL,
                fromUser(collection.getOwner()),
                collection.getName(),
                collection.getColor(),
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
                fromVisibility(frontier.getVisibilityData()),
                fromBanner(frontier.getbannerData()),
                frontier.getMode() == FrontierData.Mode.Path ? Optional.of(fromPathStyle(frontier.getPathStyle())) : Optional.empty(),
                Optional.ofNullable(frontier.getCollectionId()).map(CollectionId::new),
                Optional.ofNullable(frontier.getSourcePluginId()),
                owner,
                sharedUsers
        );
    }

    public static FrontierLifetime fromLifetime(FrontierData.FrontierLifetime lifetime) {
        return switch (lifetime) {
            case PERSISTENT -> FrontierLifetime.PERSISTENT;
            case SESSION_ONLY -> FrontierLifetime.SESSION_ONLY;
        };
    }

    public static FrontierData.FrontierLifetime toLifetime(FrontierLifetime lifetime) {
        FrontierLifetime checkedLifetime = lifetime == null ? FrontierLifetime.PERSISTENT : lifetime;
        return switch (checkedLifetime) {
            case PERSISTENT -> FrontierData.FrontierLifetime.PERSISTENT;
            case SESSION_ONLY -> FrontierData.FrontierLifetime.SESSION_ONLY;
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

    public static void applyMutation(FrontierData frontier, FrontierMutation mutation) {
        FrontierMutationApplier.applyMutation(frontier, mutation);
    }

    public static void applyCollectionMutation(CollectionData collection, CollectionMutation mutation) {
        mutation.name().ifPresent(collection::setName);
        mutation.color().ifPresent(collection::setColor);
    }
}
