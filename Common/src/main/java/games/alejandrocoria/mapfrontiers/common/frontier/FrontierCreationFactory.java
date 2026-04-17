package games.alejandrocoria.mapfrontiers.common.frontier;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
public final class FrontierCreationFactory {
    private FrontierCreationFactory() {
    }

    public static FrontierData createFrontier(UUID frontierId,
                                              SettingsUser owner,
                                              ResourceKey<Level> dimension,
                                              boolean personal,
                                              FrontierData.FrontierLifetime lifetime,
                                              @Nullable String sourcePluginId,
                                              @Nullable List<BlockPos> vertices,
                                              @Nullable List<ChunkPos> chunks) {
        return createFrontier(frontierId, owner, dimension, personal, lifetime, sourcePluginId, vertices, chunks, null, null);
    }

    public static FrontierData createFrontier(UUID frontierId,
                                              SettingsUser owner,
                                              ResourceKey<Level> dimension,
                                              boolean personal,
                                              FrontierData.FrontierLifetime lifetime,
                                              @Nullable String sourcePluginId,
                                              @Nullable List<BlockPos> vertices,
                                              @Nullable List<ChunkPos> chunks,
                                              @Nullable List<BlockPos> points,
                                              @Nullable FrontierData.PathStyle pathStyle) {
        FrontierData frontier = new FrontierData();
        frontier.setId(frontierId);
        frontier.setOwner(owner);
        frontier.setDimension(dimension);
        frontier.setPersonal(personal);
        frontier.setLifetime(lifetime);
        frontier.setSourcePluginId(sourcePluginId);
        frontier.setColor(ColorHelper.getRandomColor());
        frontier.setCreated(new Date());
        FrontierMutationApplier.applyCreationShape(frontier, vertices, chunks, points);
        if (pathStyle != null) {
            frontier.setPathStyle(pathStyle);
        }
        return frontier;
    }
}
