package games.alejandrocoria.mapfrontiers.common.api.server;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierCreationFactory;
import games.alejandrocoria.mapfrontiers.common.frontier.server.ServerFrontierCommandResult;
import games.alejandrocoria.mapfrontiers.common.frontier.server.ServerFrontierCommandService;
import games.alejandrocoria.mapfrontiers.common.frontier.server.ServerFrontierEvents;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerFrontierServiceImpl implements PluginScopedServerFrontierService {
    private final ServerFrontierCommandService commandService;
    private final ServerFrontierEvents frontierEvents;

    public ServerFrontierServiceImpl(ServerFrontierCommandService commandService, ServerFrontierEvents frontierEvents) {
        this.commandService = commandService;
        this.frontierEvents = frontierEvents;
    }

    @Override
    public FrontierDataView createGlobalFrontier(String pluginModId, UserRef owner, DimensionId dimension, FrontierShape shape) {
        ResourceKey<Level> level = ApiConverters.toDimension(dimension);
        SettingsUser frontierOwner = ApiConverters.toUser(owner);

        FrontierData frontier = FrontierCreationFactory.createFrontier(UUID.randomUUID(), frontierOwner, level, false, pluginModId, null, null);
        ApiConverters.applyShape(frontier, shape);

        ServerFrontierCommandResult result = commandService.createGlobalFrontier(frontier);
        result.dispatchNetworkActions();
        MapFrontiers.LOGGER.info("Created global frontier via server API. pluginModId={}, frontierId={}, owner={}, dimension={}",
                pluginModId, frontier.getId(), frontierOwner.username, level.identifier());

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        frontierEvents.postCreated(frontier);
        return view;
    }

    @Override
    public Optional<FrontierDataView> updateGlobalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        FrontierData frontier = commandService.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return Optional.empty();
        }

        ApiConverters.applyMutation(frontier, mutation);
        ServerFrontierCommandResult result = commandService.updateGlobalFrontier(frontier);
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
        ServerFrontierCommandResult result = commandService.deleteGlobalFrontier(frontierId.value());
        if (result.isSuccess()) {
            result.dispatchNetworkActions();
            frontierEvents.postDeleted(frontierId.value());
        }

        return result.isSuccess();
    }

    @Override
    public Optional<FrontierDataView> getFrontier(String pluginModId, FrontierId frontierId) {
        FrontierData frontier = commandService.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return Optional.empty();
        }
        return Optional.of(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public List<FrontierDataView> listGlobalFrontiers(String pluginModId, DimensionId dimension) {
        ResourceKey<Level> level = ApiConverters.toDimension(dimension);
        return commandService.getAllGlobalFrontiers(level).stream().map(ApiConverters::fromFrontier).toList();
    }

}
