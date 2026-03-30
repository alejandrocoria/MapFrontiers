package games.alejandrocoria.mapfrontiers.common.api;

import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierBanner;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.FrontierType;
import games.alejandrocoria.mapfrontiers.api.model.FrontierVisibilityFlag;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.api.model.SharedUserAccess;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierMutationApplier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

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

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            return FrontierShape.vertex(vertices);
        }

        return FrontierShape.chunk(chunks);
    }

    public static void applyShape(FrontierData frontier, FrontierShape shape) {
        FrontierMutationApplier.applyShape(frontier, shape);
    }

    public static Set<FrontierVisibilityFlag> fromVisibility(FrontierData.VisibilityData visibilityData) {
        return FrontierMutationApplier.fromVisibility(visibilityData);
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

    public static SharedUserAccess fromSharedUser(SettingsUserShared userShared) {
        EnumSet<FrontierSharePermission> permissions = EnumSet.noneOf(FrontierSharePermission.class);
        for (SettingsUserShared.Action action : userShared.getActions()) {
            permissions.add(FrontierSharePermission.valueOf(action.name()));
        }

        return new SharedUserAccess(fromUser(userShared.getUser()), permissions, userShared.isPending());
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
                fromDimension(frontier.getDimension()),
                frontier.getColor(),
                frontier.getName1(),
                frontier.getName2(),
                toShape(frontier),
                fromVisibility(frontier.getVisibilityData()),
                fromBanner(frontier.getbannerData()),
                Optional.ofNullable(frontier.getSourcePluginId()),
                owner,
                sharedUsers
        );
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
}
