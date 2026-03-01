package games.alejandrocoria.mapfrontiers.common.api.server;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.api.server.ServerFrontierService;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerFrontierServiceImpl implements ServerFrontierService {
    private static final int SYSTEM_ACTOR_ID = -1;
    private static final SettingsUser SYSTEM_USER = createSystemUser();

    private final FrontiersManager frontiersManager;
    private final SimpleEventBus eventBus;

    public ServerFrontierServiceImpl(FrontiersManager frontiersManager, SimpleEventBus eventBus) {
        this.frontiersManager = frontiersManager;
        this.eventBus = eventBus;
    }

    @Override
    public FrontierDataView createGlobalFrontier(DimensionId dimension, FrontierShape shape) {
        ResourceKey<Level> level = ApiConverters.toDimension(dimension);

        FrontierData frontier = new FrontierData();
        frontier.setId(UUID.randomUUID());
        frontier.setOwner(copySystemUser());
        frontier.setDimension(level);
        frontier.setPersonal(false);
        frontier.setCreated(new Date());
        ApiConverters.applyShape(frontier, shape);

        frontiersManager.addGlobalFrontier(frontier);
        notifyGlobalCreated(frontier);

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        eventBus.post(new FrontierCreatedEvent(view));
        return view;
    }

    @Override
    public Optional<FrontierDataView> updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation) {
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
    public boolean deleteGlobalFrontier(FrontierId frontierId) {
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
    public Optional<FrontierDataView> changeGlobalToPersonal(FrontierId frontierId, UserRef newOwner) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return Optional.empty();
        }

        SettingsUser owner = ApiConverters.toUser(newOwner);
        boolean changed = frontiersManager.changeGlobalFrontierToPersonal(owner, frontier.getDimension(), frontier.getId());
        if (!changed) {
            return Optional.empty();
        }

        FrontierData updated = frontiersManager.getFrontierFromID(frontierId.value());
        notifyGlobalDeleted(frontier);
        notifyPersonalCreated(updated);
        FrontierDataView view = ApiConverters.fromFrontier(updated);
        eventBus.post(new FrontierUpdatedEvent(view));
        return Optional.of(view);
    }

    @Override
    public Optional<FrontierDataView> changePersonalToGlobal(FrontierId frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return Optional.empty();
        }

        List<SettingsUserShared> previousSharedUsers = frontier.getUsersShared() == null ? List.of() : List.copyOf(frontier.getUsersShared());
        boolean changed = frontiersManager.changePersonalFrontierToGlobal(frontier.getOwner(), frontier.getDimension(), frontier.getId());
        if (!changed) {
            return Optional.empty();
        }

        FrontierData updated = frontiersManager.getFrontierFromID(frontierId.value());
        notifyPersonalDeleted(frontier, previousSharedUsers);
        notifyGlobalCreated(updated);
        FrontierDataView view = ApiConverters.fromFrontier(updated);
        eventBus.post(new FrontierUpdatedEvent(view));
        return Optional.of(view);
    }

    @Override
    public Optional<FrontierDataView> getFrontier(FrontierId frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return Optional.empty();
        }
        return Optional.of(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public List<FrontierDataView> listGlobalFrontiers(DimensionId dimension) {
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

    private void notifyPersonalCreated(FrontierData frontier) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server != null) {
            PacketHandler.sendToUsersWithAccess(new PacketFrontierCreated(frontier, SYSTEM_ACTOR_ID), frontier, server);
        }
    }

    private void notifyPersonalDeleted(FrontierData frontier, List<SettingsUserShared> previousSharedUsers) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server == null) {
            return;
        }

        ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(frontier.getOwner().uuid);
        if (ownerPlayer != null) {
            PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(), true, SYSTEM_ACTOR_ID), ownerPlayer);
        }

        for (SettingsUserShared shared : previousSharedUsers) {
            if (shared.isPending()) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(shared.getUser().uuid);
            if (player != null) {
                PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(), true, SYSTEM_ACTOR_ID), player);
            }
        }
    }

    private static SettingsUser createSystemUser() {
        SettingsUser system = new SettingsUser();
        system.username = "MapFrontiersSystem";
        system.uuid = UUID.nameUUIDFromBytes("mapfrontiers:system".getBytes(StandardCharsets.UTF_8));
        return system;
    }

    private static SettingsUser copySystemUser() {
        SettingsUser copy = new SettingsUser();
        copy.username = SYSTEM_USER.username;
        copy.uuid = SYSTEM_USER.uuid;
        return copy;
    }
}
