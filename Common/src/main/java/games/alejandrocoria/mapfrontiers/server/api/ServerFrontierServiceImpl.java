package games.alejandrocoria.mapfrontiers.server.api;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.DefaultValuesProfile;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationService;
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
    private final ServerTerritoryOperationService operationService;

    public ServerFrontierServiceImpl(ServerTerritoryOperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    public FrontierDataView createGlobalFrontier(String pluginModId, UserRef owner, FrontierCreateRequest request) {
        FrontierCreateSpec createSpec = createGlobalFrontierSpec(pluginModId, owner, request);
        ServerTerritoryOperationResult result = operationService.createGlobalFrontier(createSpec);
        if (!result.isSuccess() || result.getFrontier() == null) {
            throw new IllegalArgumentException("Invalid global frontier create request");
        }
        result.dispatchNetworkActions();
        FrontierData frontier = result.getFrontier();
        MapFrontiers.LOGGER.info("Created global frontier via server API. pluginModId={}, frontierId={}, owner={}, dimension={}",
                pluginModId, frontier.getId(), frontier.getOwner().username, frontier.getDimension().identifier());

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        return view;
    }

    @Override
    public Optional<FrontierDataView> updateGlobalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        FrontierData frontier = operationService.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal() || !frontier.isPersistent()) {
            return Optional.empty();
        }

        FrontierChange change = FrontierChange.fromMutation(frontier, mutation);
        if (change.isEmpty()) {
            return Optional.of(ApiConverters.fromFrontier(frontier));
        }

        ServerTerritoryOperationResult result = operationService.updateGlobalFrontier(frontierId.value(), change);
        if (!result.isSuccess()) {
            return Optional.empty();
        }
        result.dispatchNetworkActions();

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        return Optional.of(view);
    }

    @Override
    public boolean deleteGlobalFrontier(String pluginModId, FrontierId frontierId) {
        FrontierData frontier = operationService.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal() || !frontier.isPersistent()) {
            return false;
        }

        ServerTerritoryOperationResult result = operationService.deleteGlobalFrontier(frontierId.value());
        if (result.isSuccess()) {
            result.dispatchNetworkActions();
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

    @Override
    public List<FrontierDataView> listGlobalFrontiersInCollection(String pluginModId, CollectionId collectionId) {
        return operationService.getFrontiersInCollection(collectionId.value()).stream()
                .filter(frontier -> !frontier.getPersonal())
                .filter(FrontierData::isPersistent)
                .map(ApiConverters::fromFrontier)
                .toList();
    }

    private static FrontierCreateSpec createGlobalFrontierSpec(String pluginModId, UserRef owner, FrontierCreateRequest request) {
        if (request.defaultValuesProfile() == DefaultValuesProfile.CONFIGURED) {
            throw new IllegalArgumentException("CONFIGURED defaults are not supported by the server API");
        }

        FrontierData defaults = new FrontierData();
        UUID frontierId = UUID.randomUUID();
        SettingsUser frontierOwner = ApiConverters.toUser(owner);
        ResourceKey<Level> dimension = ApiConverters.toDimension(request.dimension());
        UUID collectionId = request.collectionId().map(CollectionId::value).orElse(null);
        String name1 = request.name1().orElse(defaults.getName1());
        String name2 = request.name2().orElse(defaults.getName2());
        int color = request.color().orElseGet(ColorHelper::getRandomColor);
        FrontierVisibilityData visibility = request.visibility()
                .map(ApiConverters::toFrontierVisibility)
                .orElseGet(defaults::getVisibilityData);
        BannerData banner = request.banner()
                .map(ApiConverters::toBanner)
                .orElseGet(defaults::getBannerData);
        boolean pathShape = switch (request.shape().type()) {
            case PATH -> true;
            default -> false;
        };
        FrontierData.PathStyle pathStyle = pathShape
                ? request.pathStyle().map(ApiConverters::toPathStyle).orElseGet(FrontierData.PathStyle::new)
                : new FrontierData.PathStyle();

        return switch (request.shape().type()) {
            case VERTEX -> FrontierCreateSpec.vertex(frontierId, frontierOwner, false, dimension,
                    TerritoryLifetime.PERSISTENT, collectionId, pluginModId, name1, name2, color, visibility, banner,
                    request.shape().vertices() == null ? List.of() : request.shape().vertices().stream()
                            .map(vertex -> new BlockPos(vertex.x(), 0, vertex.z()))
                            .toList(),
                    pathStyle);
            case CHUNK -> FrontierCreateSpec.chunk(frontierId, frontierOwner, false, dimension,
                    TerritoryLifetime.PERSISTENT, collectionId, pluginModId, name1, name2, color, visibility, banner,
                    request.shape().chunks() == null ? Set.of() : request.shape().chunks().stream()
                            .map(chunk -> new ChunkPos(chunk.x(), chunk.z()))
                            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
                    pathStyle);
            case PATH -> FrontierCreateSpec.path(frontierId, frontierOwner, false, dimension,
                    TerritoryLifetime.PERSISTENT, collectionId, pluginModId, name1, name2, color, visibility, banner,
                    request.shape().points() == null ? List.of() : request.shape().points().stream()
                            .map(point -> new BlockPos(point.x(), 0, point.z()))
                            .toList(),
                    pathStyle);
        };
    }
}
