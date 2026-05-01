package games.alejandrocoria.mapfrontiers.server.api;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierEvents;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationResult;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ServerFrontierServiceImpl implements PluginScopedServerFrontierService {
    private final ServerFrontierOperationService operationService;
    private final ServerFrontierEvents frontierEvents;

    public ServerFrontierServiceImpl(ServerFrontierOperationService operationService, ServerFrontierEvents frontierEvents) {
        this.operationService = operationService;
        this.frontierEvents = frontierEvents;
    }

    @Override
    public FrontierDataView createGlobalFrontier(String pluginModId, UserRef owner, FrontierCreateRequest request) {
        FrontierCreateSpec createSpec = createGlobalFrontierSpec(pluginModId, owner, request);
        ServerFrontierOperationResult result = operationService.createGlobalFrontier(createSpec);
        if (!result.isSuccess() || result.getFrontier() == null) {
            throw new IllegalArgumentException("Invalid global frontier create request");
        }
        result.dispatchNetworkActions();
        FrontierData frontier = result.getFrontier();
        MapFrontiers.LOGGER.info("Created global frontier via server API. pluginModId={}, frontierId={}, owner={}, dimension={}",
                pluginModId, frontier.getId(), frontier.getOwner().username, frontier.getDimension().identifier());

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        frontierEvents.postCreated(frontier);
        return view;
    }

    @Override
    public Optional<FrontierDataView> updateGlobalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        FrontierData frontier = operationService.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal() || !frontier.isPersistent()) {
            return Optional.empty();
        }

        FrontierData payload = new FrontierData(frontier);
        ApiConverters.applyMutation(payload, mutation);
        ServerFrontierOperationResult result = operationService.updateGlobalFrontier(frontierId.value(), FrontierChange.fromFrontierData(payload));
        if (!result.isSuccess()) {
            return Optional.empty();
        }
        result.dispatchNetworkActions();

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        frontierEvents.postUpdated(frontier);
        return Optional.of(view);
    }

    @Override
    public boolean deleteGlobalFrontier(String pluginModId, FrontierId frontierId) {
        FrontierData frontier = operationService.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal() || !frontier.isPersistent()) {
            return false;
        }

        ServerFrontierOperationResult result = operationService.deleteGlobalFrontier(frontierId.value());
        if (result.isSuccess()) {
            result.dispatchNetworkActions();
            frontierEvents.postDeleted(frontierId.value());
        }

        return result.isSuccess();
    }

    @Override
    public Optional<FrontierDataView> getFrontier(String pluginModId, FrontierId frontierId) {
        FrontierData frontier = operationService.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal() || !frontier.isPersistent()) {
            return Optional.empty();
        }
        return Optional.of(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public List<FrontierDataView> listGlobalFrontiers(String pluginModId, DimensionId dimension) {
        ResourceKey<Level> level = ApiConverters.toDimension(dimension);
        return operationService.getAllGlobalFrontiers(level).stream()
                .filter(FrontierData::isPersistent)
                .map(ApiConverters::fromFrontier)
                .toList();
    }

    private static FrontierCreateSpec createGlobalFrontierSpec(String pluginModId, UserRef owner, FrontierCreateRequest request) {
        FrontierData defaults = new FrontierData();
        UUID frontierId = UUID.randomUUID();
        SettingsUser frontierOwner = ApiConverters.toUser(owner);
        ResourceKey<Level> dimension = ApiConverters.toDimension(request.dimension());
        UUID collectionId = request.collectionId().map(CollectionId::value).orElse(null);
        String name1 = request.name1().orElse(defaults.getName1());
        String name2 = request.name2().orElse(defaults.getName2());
        int color = request.color().orElseGet(ColorHelper::getRandomColor);
        FrontierData.VisibilityData visibility = request.visibility()
                .map(ApiConverters::toVisibility)
                .orElseGet(defaults::getVisibilityData);
        FrontierData.BannerData banner = request.banner()
                .map(ApiConverters::toBanner)
                .orElseGet(defaults::getbannerData);
        FrontierData.PathStyle pathStyle = request.pathStyle()
                .map(ApiConverters::toPathStyle)
                .orElseGet(FrontierData.PathStyle::new);

        return switch (request.shape().type()) {
            case VERTEX -> FrontierCreateSpec.vertex(frontierId, frontierOwner, false, dimension,
                    FrontierData.FrontierLifetime.PERSISTENT, collectionId, pluginModId, name1, name2, color, visibility, banner,
                    request.shape().vertices() == null ? List.of() : request.shape().vertices().stream()
                            .map(vertex -> new BlockPos(vertex.x(), 0, vertex.z()))
                            .toList(),
                    pathStyle);
            case CHUNK -> FrontierCreateSpec.chunk(frontierId, frontierOwner, false, dimension,
                    FrontierData.FrontierLifetime.PERSISTENT, collectionId, pluginModId, name1, name2, color, visibility, banner,
                    request.shape().chunks() == null ? Set.of() : request.shape().chunks().stream()
                            .map(chunk -> new ChunkPos(chunk.x(), chunk.z()))
                            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
                    pathStyle);
            case PATH -> FrontierCreateSpec.path(frontierId, frontierOwner, false, dimension,
                    FrontierData.FrontierLifetime.PERSISTENT, collectionId, pluginModId, name1, name2, color, visibility, banner,
                    request.shape().points() == null ? List.of() : request.shape().points().stream()
                            .map(point -> new BlockPos(point.x(), 0, point.z()))
                            .toList(),
                    pathStyle);
        };
    }
}
