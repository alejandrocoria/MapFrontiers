package games.alejandrocoria.mapfrontiers.common.api.client;

import games.alejandrocoria.mapfrontiers.api.client.ClientFrontierService;
import games.alejandrocoria.mapfrontiers.api.client.FrontierActionResult;
import games.alejandrocoria.mapfrontiers.api.event.FrontierCreatedEvent;
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
import games.alejandrocoria.mapfrontiers.common.api.SimpleEventBus;
import games.alejandrocoria.mapfrontiers.common.network.PacketPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToGlobal;
import games.alejandrocoria.mapfrontiers.common.network.PacketChangeFrontierToPersonal;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRemoveSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketSharePersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClientFrontierServiceImpl implements ClientFrontierService {
    private final SimpleEventBus eventBus;

    public ClientFrontierServiceImpl(SimpleEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public FrontierActionResult createPersonalFrontier(DimensionId dimension, FrontierShape shape) {
        FrontiersOverlayManager manager = MapFrontiersClient.getFrontiersOverlayManager(true);
        ResourceKey<Level> resourceKey = ApiConverters.toDimension(dimension);

        // On remote servers, creation is asynchronous and manager returns null.
        // Build the local frontier immediately and sync it with PacketPersonalFrontier.
        if (MapFrontiersClient.isModOnServer()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return FrontierActionResult.rejected();
            }

            FrontierData frontier = new FrontierData();
            frontier.setId(UUID.randomUUID());
            frontier.setOwner(new SettingsUser(minecraft.player));
            frontier.setDimension(resourceKey);
            frontier.setPersonal(true);
            frontier.setColor(ColorHelper.getRandomColor());
            frontier.setCreated(new Date());

            if (shape.type() == games.alejandrocoria.mapfrontiers.api.model.FrontierShapeType.VERTEX) {
                frontier.setMode(FrontierData.Mode.Vertex);
                for (var vertex : shape.vertices()) {
                    frontier.addVertex(new BlockPos(vertex.x(), 0, vertex.z()));
                }
            } else {
                frontier.setMode(FrontierData.Mode.Chunk);
                for (var chunk : shape.chunks()) {
                    frontier.toggleChunk(new ChunkPos(chunk.x(), chunk.z()));
                }
            }

            FrontierOverlay overlay = manager.addFrontier(frontier);
            PacketHandler.sendToServer(new PacketPersonalFrontier(frontier));
            eventBus.post(new FrontierCreatedEvent(ApiConverters.fromFrontier(overlay)));
            return FrontierActionResult.applied(ApiConverters.fromFrontier(overlay));
        }

        FrontierOverlay frontier = manager.clientCreateNewFrontierAndReturn(resourceKey, shape);
        if (frontier == null) {
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

        PacketHandler.sendToServer(new PacketSharePersonalFrontier(frontierId.value(), ApiConverters.toUser(sharedUserAccess.user())));
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

        var currentShared = frontier.getUserShared(ApiConverters.toUser(sharedUserAccess.user()));
        if (currentShared == null) {
            return FrontierActionResult.notFound(frontierId);
        }

        currentShared.setActions(ApiConverters.toSharedUser(sharedUserAccess).getActions());
        currentShared.setPending(sharedUserAccess.pending());
        frontier.addChange(FrontierData.Change.Shared);

        PacketHandler.sendToServer(new PacketUpdateSharedUserPersonalFrontier(frontierId.value(), ApiConverters.toSharedUser(sharedUserAccess)));
        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    @Override
    public FrontierActionResult removeSharedUser(FrontierId frontierId, UserRef user) {
        FrontiersOverlayManager personal = MapFrontiersClient.getFrontiersOverlayManager(true);
        FrontierOverlay frontier = personal.getFrontier(frontierId.value());
        if (frontier == null || !frontier.getPersonal()) {
            return FrontierActionResult.notFound(frontierId);
        }

        PacketHandler.sendToServer(new PacketRemoveSharedUserPersonalFrontier(frontierId.value(), ApiConverters.toUser(user)));
        frontier.removeUserShared(ApiConverters.toUser(user));
        return FrontierActionResult.applied(ApiConverters.fromFrontier(frontier));
    }

    private FrontierActionResult updateFrontierInManager(FrontierId frontierId,
                                                         FrontierMutation mutation,
                                                         FrontiersOverlayManager manager,
                                                         FrontierOverlay frontier) {
        ApiConverters.applyMutation(frontier, mutation);
        manager.clientUpdateFrontier(frontier);
        if (MapFrontiersClient.isModOnServer()) {
            return FrontierActionResult.acceptedAsync(frontierId);
        }
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
