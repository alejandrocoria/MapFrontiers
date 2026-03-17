package games.alejandrocoria.mapfrontiers.common.api.server;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.internal.PluginScopedServerFrontierService;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerFrontierServiceImpl implements PluginScopedServerFrontierService {
    private static final int SYSTEM_ACTOR_ID = -1;

    private final FrontiersManager frontiersManager;
    private final SimpleEventBus eventBus;

    public ServerFrontierServiceImpl(FrontiersManager frontiersManager, SimpleEventBus eventBus) {
        this.frontiersManager = frontiersManager;
        this.eventBus = eventBus;
    }

    @Override
    public FrontierDataView createGlobalFrontier(String pluginModId, UserRef owner, DimensionId dimension, FrontierShape shape) {
        ResourceKey<Level> level = ApiConverters.toDimension(dimension);
        SettingsUser frontierOwner = ApiConverters.toUser(owner);

        FrontierData frontier = new FrontierData();
        frontier.setId(UUID.randomUUID());
        frontier.setOwner(frontierOwner);
        frontier.setDimension(level);
        frontier.setPersonal(false);
        frontier.setColor(ColorHelper.getRandomColor());
        frontier.setCreated(new Date());
        frontier.setSourcePluginId(pluginModId);
        ApiConverters.applyShape(frontier, shape);

        frontiersManager.addGlobalFrontier(frontier);
        MapFrontiers.LOGGER.info("Created global frontier via server API. pluginModId={}, frontierId={}, owner={}, dimension={}",
                pluginModId, frontier.getId(), frontierOwner.username, level.identifier());
        notifyGlobalCreated(frontier);

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        eventBus.post(new FrontierCreatedEvent(view));
        return view;
    }

    @Override
    public Optional<FrontierDataView> updateGlobalFrontier(String pluginModId, FrontierId frontierId, FrontierMutation mutation) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return Optional.empty();
        }

        ApiConverters.applyMutation(frontier, mutation);
        boolean updated = frontiersManager.updateGlobalFrontier(frontier);
        if (!updated) {
            return Optional.empty();
        }
        notifyGlobalUpdated(frontier);

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        eventBus.post(new FrontierUpdatedEvent(view));
        return Optional.of(view);
    }

    @Override
    public boolean deleteGlobalFrontier(String pluginModId, FrontierId frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return false;
        }

        boolean deleted = frontiersManager.deleteGlobalFrontier(frontier.getDimension(), frontier.getId());
        if (deleted) {
            notifyGlobalDeleted(frontier);
            eventBus.post(new FrontierDeletedEvent(frontierId));
        }

        return deleted;
    }

    @Override
    public Optional<FrontierDataView> getFrontier(String pluginModId, FrontierId frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return Optional.empty();
        }
        return Optional.of(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public List<FrontierDataView> listGlobalFrontiers(String pluginModId, DimensionId dimension) {
        ResourceKey<Level> level = ApiConverters.toDimension(dimension);
        return frontiersManager.getAllGlobalFrontiers(level).stream().map(ApiConverters::fromFrontier).toList();
    }

    private void notifyGlobalCreated(FrontierData frontier) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server != null) {
            PacketHandler.sendToAll(new PacketFrontierCreated(frontier, SYSTEM_ACTOR_ID), server);
        }
    }

    private void notifyGlobalUpdated(FrontierData frontier) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server != null) {
            PacketHandler.sendToAll(new PacketFrontierUpdated(frontier, SYSTEM_ACTOR_ID), server);
        }
    }

    private void notifyGlobalDeleted(FrontierData frontier) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server != null) {
            PacketHandler.sendToAll(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(), false, SYSTEM_ACTOR_ID), server);
        }
    }

}

