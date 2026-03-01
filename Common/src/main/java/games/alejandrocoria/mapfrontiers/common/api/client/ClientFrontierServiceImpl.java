package games.alejandrocoria.mapfrontiers.common.api.client;

import games.alejandrocoria.mapfrontiers.api.client.ClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.model.DimensionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.SharedUserAccess;
import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.api.ApiConverters;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToGlobal;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToPersonal;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRemoveSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketSharePersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateSharedUserPersonalFrontier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class ClientFrontierServiceImpl implements ClientFrontierService {
    public ClientFrontierServiceImpl() {
    }

    @Override
    public FrontierActionResult createPersonalFrontier(DimensionId dimension, FrontierShape shape) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        FrontierOverlay frontier = manager.clientCreateNewFrontierAndReturn(resourceKey, shape);
        if (frontier == null) {
            if (MapFrontiersClient.isModOnServer()) {
                return FrontierActionResult.acceptedAsync();
            }
            return FrontierActionResult.rejected();
        }

        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public FrontierActionResult createGlobalFrontier(DimensionId dimension, FrontierShape shape) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(false);
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        FrontierOverlay frontier = manager.clientCreateNewFrontierAndReturn(resourceKey, shape);
        if (frontier == null) {
            if (MapFrontiersClient.isModOnServer()) {
                return FrontierActionResult.acceptedAsync();
            }
            return FrontierActionResult.rejected();
        }

        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public Optional<FrontierDataView> getFrontier(FrontierId frontierId) {
        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null) {
            frontier = MapFrontiersClient.getFrontiersOverlayManager(false).getFrontier(frontierId.value());
        }

        return frontier == null ? Optional.empty() : Optional.of(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public FrontierActionResult updateGlobalFrontier(FrontierId frontierId, FrontierMutation mutation) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(false);
        FrontierOverlay frontier = manager.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }
        return updateFrontierInManager(frontierId, mutation, manager, frontier);
    }

    @Override
    public FrontierActionResult deleteGlobalFrontier(FrontierId frontierId) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(false);
        FrontierOverlay frontier = manager.getFrontier(frontierId.value());
        if (frontier == null || frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }
        return deleteFrontierInManager(frontierId, manager, frontier);
    }

    @Override
    public FrontierActionResult updatePersonalFrontier(FrontierId frontierId, FrontierMutation mutation) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = manager.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }
        return updateFrontierInManager(frontierId, mutation, manager, frontier);
    }

    @Override
    public FrontierActionResult deletePersonalFrontier(FrontierId frontierId) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = manager.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }
        return deleteFrontierInManager(frontierId, manager, frontier);
    }

    @Override
    public List<FrontierDataView> listPersonalFrontiers(DimensionId dimension) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        return manager.getAllFrontiers(resourceKey).stream().map(ApiConverters::fromFrontier).toList();
    }

    @Override
    public List<FrontierDataView> listGlobalFrontiers(DimensionId dimension) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(false);
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);
        return manager.getAllFrontiers(resourceKey).stream().map(ApiConverters::fromFrontier).toList();
    }

    @Override
    public FrontierActionResult changeToGlobal(FrontierId frontierId) {
        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null) {
            return FrontierActionResult.notFound(frontierId);
        }
        if (!MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.rejected();
        }

        PacketHandler.sendToServer(new PacketChangeFrontierToGlobal(frontierId.value(), null));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    @Override
    public FrontierActionResult changeToPersonal(FrontierId frontierId) {
        FrontiersOverlayManager global = MapFrontiersClient.getFrontiersOverlayManager(false);
        FrontierOverlay frontier = global.getFrontier(frontierId.value());
        if (frontier == null) {
            return FrontierActionResult.notFound(frontierId);
        }
        if (!MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.rejected();
        }

        PacketHandler.sendToServer(new PacketChangeFrontierToPersonal(frontierId.value(), null));
        return FrontierActionResult.acceptedAsync(frontierId);
    }

    @Override
    public FrontierActionResult sharePersonalFrontier(FrontierId frontierId, SharedUserAccess sharedUserAccess) {
        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }

        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketSharePersonalFrontier(frontierId.value(), ApiConverters.toUser(sharedUserAccess.user())));
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        frontier.addUserShared(ApiConverters.toSharedUser(sharedUserAccess));
        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public FrontierActionResult updateSharedUserAccess(FrontierId frontierId, SharedUserAccess sharedUserAccess) {
        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }

        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketUpdateSharedUserPersonalFrontier(frontierId.value(), ApiConverters.toSharedUser(sharedUserAccess)));
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        var currentShared = frontier.getUserShared(ApiConverters.toUser(sharedUserAccess.user()));
        if (currentShared == null) {
            return FrontierActionResult.rejected();
        }

        currentShared.setActions(ApiConverters.toSharedUser(sharedUserAccess).getActions());
        currentShared.setPending(sharedUserAccess.pending());
        frontier.addChange(FrontierData.Change.Shared);
        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public FrontierActionResult removeSharedUser(FrontierId frontierId, UserRef user) {
        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }

        var targetUser = ApiConverters.toUser(user);
        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketRemoveSharedUserPersonalFrontier(frontierId.value(), targetUser));
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        if (frontier.getUserShared(targetUser) == null) {
            return FrontierActionResult.rejected();
        }

        frontier.removeUserShared(targetUser);
        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    private FrontierActionResult updateFrontierInManager(FrontierId frontierId,
                                                         FrontierMutation mutation,
                                                         FrontiersOverlayManager manager,
                                                         FrontierOverlay frontier) {
        if (MapFrontiersClient.isModOnServer()) {
            FrontierData payload = new FrontierData(frontier);
            ApiConverters.applyMutation(payload, mutation);
            PacketHandler.sendToServer(new PacketUpdateFrontier(payload));
            return FrontierActionResult.acceptedAsync(frontierId);
        }

        ApiConverters.applyMutation(frontier, mutation);
        manager.clientUpdateFrontier(frontier);
        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    private FrontierActionResult deleteFrontierInManager(FrontierId frontierId,
                                                         FrontiersOverlayManager manager,
                                                         FrontierOverlay frontier) {
        manager.clientDeleteFrontier(frontier);
        if (MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.acceptedAsync(frontierId);
        }
        FrontierDataView deleted = ApiConverters.fromFrontier(frontier);
        return FrontierActionResult.applied(deleted);
    }
}
