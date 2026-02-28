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
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.resources.ResourceKey;
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

        boolean changed = frontiersManager.changePersonalFrontierToGlobal(frontier.getOwner(), frontier.getDimension(), frontier.getId());
        if (!changed) {
            return Optional.empty();
        }

        FrontierData updated = frontiersManager.getFrontierFromID(frontierId.value());
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

        frontier.addUserShared(ApiConverters.toSharedUser(sharedUserAccess));
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

        currentShared.setActions(ApiConverters.toSharedUser(sharedUserAccess).getActions());
        currentShared.setPending(sharedUserAccess.pending());
        frontier.addChange(FrontierData.Change.Shared);
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
        }

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
}
