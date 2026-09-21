package games.alejandrocoria.mapfrontiers.server.api;

import games.alejandrocoria.mapfrontiers.api.model.CollectionId;
import games.alejandrocoria.mapfrontiers.api.model.FrontierDataView;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationService;
import games.alejandrocoria.mapfrontiers.server.territory.TerritoriesManager;
import games.alejandrocoria.mapfrontiers.test.MinecraftTestBootstrap;
import games.alejandrocoria.mapfrontiers.test.TestResourceKeys;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerFrontierServiceImplTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void listGlobalFrontiersInCollectionUsesMembershipIndexAcrossDimensionsAndFiltersScope() {
        TerritoriesManager manager = new TerritoriesManager(new PlayerNameRepository(), username -> null);
        UUID collectionId = UUID.randomUUID();
        FrontierData firstGlobal = manager.createNewGlobalFrontier(frontierSpec(false, overworld(), collectionId));
        FrontierData secondGlobal = manager.createNewGlobalFrontier(frontierSpec(false, nether(), collectionId));
        manager.createNewPersonalFrontier(frontierSpec(true, overworld(), collectionId));
        ServerFrontierServiceImpl service = createService(manager);

        List<FrontierDataView> result = service.listGlobalFrontiersInCollection(
                "different-plugin", new CollectionId(collectionId));

        assertEquals(Set.of(firstGlobal.getId(), secondGlobal.getId()), Set.copyOf(result.stream()
                .map(view -> view.id().value())
                .toList()));
        assertThrows(UnsupportedOperationException.class, result::clear);
    }

    @Test
    void listGlobalFrontiersInCollectionReturnsEmptyForUnknownOrPersonalOnlyCollection() {
        TerritoriesManager manager = new TerritoriesManager(new PlayerNameRepository(), username -> null);
        UUID personalCollectionId = UUID.randomUUID();
        manager.createNewPersonalFrontier(frontierSpec(true, overworld(), personalCollectionId));
        ServerFrontierServiceImpl service = createService(manager);

        assertTrue(service.listGlobalFrontiersInCollection(
                "plugin", new CollectionId(UUID.randomUUID())).isEmpty());
        assertTrue(service.listGlobalFrontiersInCollection(
                "plugin", new CollectionId(personalCollectionId)).isEmpty());
    }

    private static ServerFrontierServiceImpl createService(TerritoriesManager manager) {
        ServerTerritoryOperationService operationService = new ServerTerritoryOperationService(
                null, manager, null, null, null, playerId -> null);
        return new ServerFrontierServiceImpl(operationService, new PlayerNameRepository());
    }

    private static FrontierCreateSpec frontierSpec(boolean personal, ResourceKey<Level> dimension, UUID collectionId) {
        return FrontierCreateSpec.vertex(
                UUID.randomUUID(),
                new PlayerId(UUID.randomUUID()),
                personal,
                dimension,
                TerritoryLifetime.PERSISTENT,
                collectionId,
                "source-plugin",
                "New",
                "Frontier",
                0xFFFFFF,
                new FrontierVisibilityData(),
                null,
                List.of(),
                new FrontierData.PathStyle()
        );
    }

    private static ResourceKey<Level> dimension(String path) {
        return TestResourceKeys.dimension(path);
    }

    private static ResourceKey<Level> overworld() {
        return dimension("overworld");
    }

    private static ResourceKey<Level> nether() {
        return dimension("the_nether");
    }
}
