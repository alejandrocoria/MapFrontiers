package games.alejandrocoria.mapfrontiers.server.api;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierCreateRequest;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierCreationFactory;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierEvents;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationResult;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationService;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
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
        if (hasUnsupportedCreateFields(request)) {
            throw new UnsupportedOperationException("Enriched frontier create fields are not implemented yet.");
        }

        ResourceKey<Level> level = ApiConverters.toDimension(request.dimension());
        SettingsUser frontierOwner = ApiConverters.toUser(owner);

        FrontierData frontier = FrontierCreationFactory.createFrontier(UUID.randomUUID(), frontierOwner, level, false,
                FrontierData.FrontierLifetime.PERSISTENT, pluginModId, null, null);
        ApiConverters.applyShape(frontier, request.shape());

        ServerFrontierOperationResult result = operationService.createGlobalFrontier(frontier);
        result.dispatchNetworkActions();
        MapFrontiers.LOGGER.info("Created global frontier via server API. pluginModId={}, frontierId={}, owner={}, dimension={}",
                pluginModId, frontier.getId(), frontierOwner.username, level.identifier());

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

    private static boolean hasUnsupportedCreateFields(FrontierCreateRequest request) {
        return request.collectionId().isPresent()
                || request.name1().isPresent()
                || request.name2().isPresent()
                || request.color().isPresent()
                || request.visibility().isPresent()
                || request.banner().isPresent()
                || request.pathStyle().isPresent();
    }
}
