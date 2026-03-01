package games.alejandrocoria.mapfrontiers.common.api;

import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierBanner;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShapeType;
import games.alejandrocoria.mapfrontiers.api.model.FrontierSharePermission;
import games.alejandrocoria.mapfrontiers.api.model.FrontierType;
import games.alejandrocoria.mapfrontiers.api.model.FrontierVisibilityFlag;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.api.model.SharedUserAccess;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
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
        List<ChunkCoord> chunks = frontier.getChunks().stream().map(chunk -> new ChunkCoord(chunk.x, chunk.z)).toList();

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            return FrontierShape.vertex(vertices);
        }

        return FrontierShape.chunk(chunks);
    }

    public static void applyShape(FrontierData frontier, FrontierShape shape) {
        frontier.clearVertices();
        frontier.clearChunks();

        if (shape.type() == FrontierShapeType.VERTEX) {
            frontier.setMode(FrontierData.Mode.Vertex);
            for (Point2i vertex : shape.vertices()) {
                frontier.addVertex(new BlockPos(vertex.x(), 0, vertex.z()));
            }
        } else {
            frontier.setMode(FrontierData.Mode.Chunk);
            for (ChunkCoord chunk : shape.chunks()) {
                frontier.addChunk(new ChunkPos(chunk.x(), chunk.z()));
            }
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

    public static FrontierBanner fromBanner(FrontierData.BannerData bannerData) {
        if (bannerData == null) {
            return null;
        }

        return new FrontierBanner(
                bannerData.baseColor.getId(),
                bannerData.patterns == null ? null : bannerData.patterns.toString(),
                bannerData.rotation
        );
    }

    public static FrontierData.BannerData toBanner(FrontierBanner banner) {
        if (banner == null) {
            return null;
        }

        FrontierData.BannerData data = new FrontierData.BannerData();
        data.baseColor = DyeColor.byId(banner.baseColorId());
        if (banner.patternsNbt() != null && !banner.patternsNbt().isBlank()) {
            try {
                Object parsed = TagParser.create(NbtOps.INSTANCE).parseFully(banner.patternsNbt());
                if (parsed instanceof ListTag listTag) {
                    data.patterns = listTag;
                }
            } catch (Exception ignored) {
            }
        }
        data.rotation = banner.rotation();
        return data;
    }

    public static SharedUserAccess fromSharedUser(SettingsUserShared userShared) {
        EnumSet<FrontierSharePermission> permissions = EnumSet.noneOf(FrontierSharePermission.class);
        for (SettingsUserShared.Action action : userShared.getActions()) {
            permissions.add(FrontierSharePermission.valueOf(action.name()));
        }

        return new SharedUserAccess(fromUser(userShared.getUser()), permissions, userShared.isPending());
    }

    public static SettingsUserShared toSharedUser(SharedUserAccess sharedUserAccess) {
        SettingsUser user = toUser(sharedUserAccess.user());
        SettingsUserShared userShared = new SettingsUserShared(user, sharedUserAccess.pending());
        EnumSet<SettingsUserShared.Action> actions = EnumSet.noneOf(SettingsUserShared.Action.class);
        for (FrontierSharePermission permission : sharedUserAccess.permissions()) {
            actions.add(SettingsUserShared.Action.valueOf(permission.name()));
        }
        userShared.setActions(actions);
        return userShared;
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
        mutation.name1().ifPresent(frontier::setName1);
        mutation.name2().ifPresent(frontier::setName2);
        mutation.color().ifPresent(frontier::setColor);
        Optional<FrontierShape> shape = mutation.shape();
        shape.ifPresent(value -> applyShape(frontier, value));
        mutation.visibility().ifPresent(value -> frontier.setVisibilityData(toVisibility(value)));
        if (mutation.clearBanner()) {
            frontier.setBannerData(null);
        } else {
            mutation.banner().ifPresent(value -> frontier.setBannerData(toBanner(value)));
        }
    }
}
