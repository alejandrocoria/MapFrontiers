package games.alejandrocoria.mapfrontiers.common.api.server;

import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierDeletedEvent;
import games.alejandrocoria.mapfrontiers.api.event.FrontierUpdatedEvent;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.SharedUserAccess;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.api.server.ServerFrontierService;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierCreated;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierDeleted;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ServerFrontierServiceImpl implements ServerFrontierService {
    private final FrontiersManager frontiersManager;
    private final SimpleEventBus eventBus;

    public ServerFrontierServiceImpl(FrontiersManager frontiersManager, SimpleEventBus eventBus) {
        this.frontiersManager = frontiersManager;
        this.eventBus = eventBus;
    }

    @Override
    public FrontierDataView createGlobalFrontier(DimensionId dimension, FrontierShape shape, UserRef user) {
        ResourceKey<Level> level = ApiConverters.toDimension(dimension);
        SettingsUser owner = ApiConverters.toUser(user);
        if (!frontiersManager.getSettings().checkAction(FrontierSettings.Action.CreateGlobalFrontier, owner, false, null)) {
            throw new IllegalStateException("User is not allowed to create global frontiers");
        }

        FrontierData frontier = new FrontierData();
        frontier.setId(java.util.UUID.randomUUID());
        frontier.setOwner(owner);
        frontier.setDimension(level);
        frontier.setPersonal(false);
        frontier.setCreated(new Date());
        ApiConverters.applyShape(frontier, shape);

        frontiersManager.addGlobalFrontier(frontier);
        notifyGlobalCreated(frontier, user);

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        eventBus.post(new FrontierCreatedEvent(view));
        return view;
    }

    @Override
    public Optional<FrontierDataView> updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation, UserRef user) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return Optional.empty();
        }

        SettingsUser actorUser = ApiConverters.toUser(user);
        boolean allowed = frontiersManager.getSettings().checkAction(FrontierSettings.Action.UpdateGlobalFrontier, actorUser, false, frontier.getOwner());
        if (!allowed) {
            return Optional.empty();
        }

        ApiConverters.applyMutation(frontier, mutation);
        boolean updated = frontiersManager.updateGlobalFrontier(frontier);
        if (!updated) {
            return Optional.empty();
        }
        notifyGlobalUpdated(frontier, user);

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        eventBus.post(new FrontierUpdatedEvent(view));
        return Optional.of(view);
    }

    @Override
    public boolean deleteGlobalFrontier(FrontierId frontierId, UserRef user) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return false;
        }

        SettingsUser actorUser = ApiConverters.toUser(user);
        boolean allowed = frontiersManager.getSettings().checkAction(FrontierSettings.Action.DeleteGlobalFrontier, actorUser, false, frontier.getOwner());
        if (!allowed) {
            return false;
        }

        boolean deleted = frontiersManager.deleteGlobalFrontier(frontier.getDimension(), frontier.getId());
        if (deleted) {
            notifyGlobalDeleted(frontier, user);
            eventBus.post(new FrontierDeletedEvent(frontierId));
        }

        return deleted;
    }

    @Override
    public Optional<FrontierDataView> changeGlobalToPersonal(FrontierId frontierId, UserRef newOwner, UserRef user) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return Optional.empty();
        }

        SettingsUser actorUser = ApiConverters.toUser(user);
        boolean allowed = frontiersManager.getSettings().checkAction(FrontierSettings.Action.DeleteGlobalFrontier, actorUser, false, frontier.getOwner());
        if (!allowed) {
            return Optional.empty();
        }

        SettingsUser owner = ApiConverters.toUser(newOwner);
        boolean changed = frontiersManager.changeGlobalFrontierToPersonal(owner, frontier.getDimension(), frontier.getId());
        if (!changed) {
            return Optional.empty();
        }

        FrontierData updated = frontiersManager.getFrontierFromID(frontierId.value());
        notifyGlobalDeleted(frontier, user);
        notifyPersonalCreated(updated, user);
        FrontierDataView view = ApiConverters.fromFrontier(updated);
        eventBus.post(new FrontierUpdatedEvent(view));
        return Optional.of(view);
    }

    @Override
    public Optional<FrontierDataView> changePersonalToGlobal(FrontierId frontierId, UserRef user) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return Optional.empty();
        }

        SettingsUser actorUser = ApiConverters.toUser(user);
        if (!frontier.getOwner().equals(actorUser)) {
            return Optional.empty();
        }

        List<SettingsUserShared> previousSharedUsers = frontier.getUsersShared() == null ? List.of() : List.copyOf(frontier.getUsersShared());
        boolean changed = frontiersManager.changePersonalFrontierToGlobal(frontier.getOwner(), frontier.getDimension(), frontier.getId());
        if (!changed) {
            return Optional.empty();
        }

        FrontierData updated = frontiersManager.getFrontierFromID(frontierId.value());
        notifyPersonalDeleted(frontier, previousSharedUsers, user);
        notifyGlobalCreated(updated, user);
        FrontierDataView view = ApiConverters.fromFrontier(updated);
        eventBus.post(new FrontierUpdatedEvent(view));
        return Optional.of(view);
    }

    @Override
    public Optional<FrontierDataView> sharePersonalFrontier(FrontierId frontierId, SharedUserAccess sharedUserAccess, UserRef user) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return Optional.empty();
        }

        SettingsUser actorUser = ApiConverters.toUser(user);
        if (!canManagePersonalSharing(frontier, actorUser)) {
            return Optional.empty();
        }

        SettingsUser targetUser = ApiConverters.toUser(sharedUserAccess.user());
        if (frontier.getOwner().equals(targetUser) || frontier.hasUserShared(targetUser)) {
            return Optional.empty();
        }

        SettingsUserShared targetAccess = ApiConverters.toSharedUser(sharedUserAccess);
        targetAccess.setPending(false);
        frontier.addUserShared(targetAccess);
        if (!frontiersManager.hasPersonalFrontier(targetUser, frontier.getId())) {
            frontiersManager.addPersonalFrontier(targetUser, frontier);
        } else {
            frontiersManager.updatePersonalFrontier(frontier.getOwner(), frontier);
        }
        notifyPersonalShared(frontier, targetUser, user);

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        eventBus.post(new FrontierUpdatedEvent(view));
        return Optional.of(view);
    }

    @Override
    public Optional<FrontierDataView> updateSharedUserAccess(FrontierId frontierId, SharedUserAccess sharedUserAccess, UserRef user) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return Optional.empty();
        }

        SettingsUser actorUser = ApiConverters.toUser(user);
        if (!canManagePersonalSharing(frontier, actorUser)) {
            return Optional.empty();
        }

        SettingsUser targetUser = ApiConverters.toUser(sharedUserAccess.user());
        SettingsUserShared currentShared = frontier.getUserShared(targetUser);
        if (currentShared == null) {
            return Optional.empty();
        }

        boolean wasPending = currentShared.isPending();
        currentShared.setActions(ApiConverters.toSharedUser(sharedUserAccess).getActions());
        currentShared.setPending(sharedUserAccess.pending());

        if (wasPending && !currentShared.isPending()) {
            if (!frontiersManager.hasPersonalFrontier(targetUser, frontier.getId())) {
                frontiersManager.addPersonalFrontier(targetUser, frontier);
            }
            notifyPersonalCreatedForUser(frontier, targetUser, user);
        } else if (!wasPending && currentShared.isPending()) {
            frontiersManager.deletePersonalFrontier(targetUser, frontier.getDimension(), frontier.getId());
            notifyPersonalDeletedForUser(frontier, targetUser, user);
            frontiersManager.updatePersonalFrontier(frontier.getOwner(), frontier);
        } else {
            frontiersManager.updatePersonalFrontier(frontier.getOwner(), frontier);
        }

        frontier.addChange(FrontierData.Change.Shared);
        notifyPersonalUpdated(frontier, user);

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        eventBus.post(new FrontierUpdatedEvent(view));
        return Optional.of(view);
    }

    @Override
    public Optional<FrontierDataView> removeSharedUser(FrontierId frontierId, UserRef targetUser, UserRef user) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return Optional.empty();
        }

        SettingsUser actorUser = ApiConverters.toUser(user);
        if (!canManagePersonalSharing(frontier, actorUser)) {
            return Optional.empty();
        }

        SettingsUser userToRemove = ApiConverters.toUser(targetUser);
        SettingsUserShared shared = frontier.getUserShared(userToRemove);
        if (shared == null) {
            return Optional.empty();
        }

        frontier.removeUserShared(userToRemove);
        if (!shared.isPending()) {
            frontiersManager.deletePersonalFrontier(userToRemove, frontier.getDimension(), frontier.getId());
            notifyPersonalDeletedForUser(frontier, userToRemove, user);
        }
        frontiersManager.updatePersonalFrontier(frontier.getOwner(), frontier);
        notifyPersonalUpdated(frontier, user);

        FrontierDataView view = ApiConverters.fromFrontier(frontier);
        eventBus.post(new FrontierUpdatedEvent(view));
        return Optional.of(view);
    }

    @Override
    public Optional<FrontierDataView> getFrontier(FrontierId frontierId) {
        FrontierData frontier = frontiersManager.getFrontierFromID(frontierId.value());
        return frontier == null ? Optional.empty() : Optional.of(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public List<FrontierDataView> listGlobalFrontiers(DimensionId dimension) {
        ResourceKey<Level> level = ApiConverters.toDimension(dimension);
        return frontiersManager.getAllGlobalFrontiers(level).stream().map(ApiConverters::fromFrontier).toList();
    }

    private boolean canManagePersonalSharing(FrontierData frontier, SettingsUser actorUser) {
        boolean baseAllowed = frontiersManager.getSettings().checkAction(FrontierSettings.Action.SharePersonalFrontier,
                actorUser, false, frontier.getOwner());
        if (!baseAllowed) {
            return false;
        }

        return frontier.checkActionUserShared(actorUser, SettingsUserShared.Action.UpdateSettings);
    }

    private int resolveActorId(UserRef user) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server == null || user.id() == null) {
            return -1;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(user.id());
        return player == null ? -1 : player.getId();
    }

    private void notifyGlobalCreated(FrontierData frontier, UserRef actor) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server != null) {
            PacketHandler.sendToAll(new PacketFrontierCreated(frontier, resolveActorId(actor)), server);
        }
    }

    private void notifyGlobalUpdated(FrontierData frontier, UserRef actor) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server != null) {
            PacketHandler.sendToAll(new PacketFrontierUpdated(frontier, resolveActorId(actor)), server);
        }
    }

    private void notifyGlobalDeleted(FrontierData frontier, UserRef actor) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server != null) {
            PacketHandler.sendToAll(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(), false, resolveActorId(actor)), server);
        }
    }

    private void notifyPersonalCreated(FrontierData frontier, UserRef actor) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server != null) {
            PacketHandler.sendToUsersWithAccess(new PacketFrontierCreated(frontier, resolveActorId(actor)), frontier, server);
        }
    }

    private void notifyPersonalUpdated(FrontierData frontier, UserRef actor) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server != null) {
            PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier, resolveActorId(actor)), frontier, server);
        }
    }

    private void notifyPersonalDeleted(FrontierData frontier, List<SettingsUserShared> previousSharedUsers, UserRef actor) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server == null) {
            return;
        }

        int actorId = resolveActorId(actor);
        ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(frontier.getOwner().uuid);
        if (ownerPlayer != null) {
            PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(), true, actorId), ownerPlayer);
        }

        for (SettingsUserShared shared : previousSharedUsers) {
            if (shared.isPending()) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(shared.getUser().uuid);
            if (player != null) {
                PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(), true, actorId), player);
            }
        }
    }

    private void notifyPersonalCreatedForUser(FrontierData frontier, SettingsUser user, UserRef actor) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server == null || user.uuid == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(user.uuid);
        if (player != null) {
            PacketHandler.sendTo(new PacketFrontierCreated(frontier, resolveActorId(actor)), player);
        }
    }

    private void notifyPersonalDeletedForUser(FrontierData frontier, SettingsUser user, UserRef actor) {
        MinecraftServer server = MapFrontiers.getCurrentServer();
        if (server == null || user.uuid == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(user.uuid);
        if (player != null) {
            PacketHandler.sendTo(new PacketFrontierDeleted(frontier.getDimension(), frontier.getId(), true, resolveActorId(actor)), player);
        }
    }

    private void notifyPersonalShared(FrontierData frontier, SettingsUser targetUser, UserRef actor) {
        notifyPersonalCreatedForUser(frontier, targetUser, actor);
        notifyPersonalUpdated(frontier, actor);
    }
}
