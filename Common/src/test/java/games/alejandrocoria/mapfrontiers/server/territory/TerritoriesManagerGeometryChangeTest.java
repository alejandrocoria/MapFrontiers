package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChangeApplicationResult;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerritoriesManagerGeometryChangeTest {
    @Test
    void rejectedSequenceDoesNotModifyRegisteredFrontierOrTimestamp() {
        TerritoriesManager manager = new TerritoriesManager(new PlayerNameRepository(), username -> null);
        FrontierData frontier = manager.createNewGlobalFrontier(pathSpec(point(0, 0), point(10, 0)));
        long initialHash = frontier.computeSyncHash();
        long initialModified = frontier.getModified().getTime();
        FrontierChange change = geometryChange(frontier, FrontierMutation.builder()
                .removePathPointAt(0)
                .setPathPointAt(5, new Point2i(20, 0))
                .build());
        change.setName("Changed", frontier.getName2());

        FrontierChangeApplicationResult result = manager.applyGlobalFrontierChange(frontier.getId(), change);

        assertTrue(result.isRejected());
        assertEquals("New", frontier.getName1());
        assertEquals(List.of(point(0, 0), point(10, 0)), frontier.getPoints());
        assertEquals(initialModified, frontier.getModified().getTime());
        assertEquals(initialHash, frontier.computeSyncHash());
    }

    @Test
    void neutralSequenceDoesNotModifyRegisteredFrontierOrTimestamp() {
        TerritoriesManager manager = new TerritoriesManager(new PlayerNameRepository(), username -> null);
        FrontierData frontier = manager.createNewGlobalFrontier(pathSpec(point(0, 0)));
        long initialModified = frontier.getModified().getTime();

        FrontierChangeApplicationResult result = manager.applyGlobalFrontierChange(
                frontier.getId(), geometryChange(frontier, FrontierMutation.builder().reversePath().build())
        );

        assertTrue(result.isNoChange());
        assertEquals(initialModified, frontier.getModified().getTime());
        assertEquals(List.of(point(0, 0)), frontier.getPoints());
    }

    @Test
    void validSequenceCommitsAndAddsAuthoritativeModifiedTimeToEffectiveChange() {
        TerritoriesManager manager = new TerritoriesManager(new PlayerNameRepository(), username -> null);
        FrontierData frontier = manager.createNewGlobalFrontier(pathSpec(point(0, 0)));

        FrontierChangeApplicationResult result = manager.applyGlobalFrontierChange(
                frontier.getId(), geometryChange(frontier, FrontierMutation.builder()
                        .insertPathPointAfterLast(new Point2i(10, 0))
                        .build())
        );

        assertTrue(result.isApplied());
        assertEquals(List.of(point(0, 0), point(10, 0)), frontier.getPoints());
        assertNotNull(result.effectiveChange().getModifiedTime());
        assertEquals(frontier.getModified().getTime(), result.effectiveChange().getModifiedTime());
    }

    private static FrontierCreateSpec pathSpec(BlockPos... points) {
        return FrontierCreateSpec.path(
                UUID.randomUUID(),
                new PlayerId(UUID.randomUUID()),
                false,
                ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld")),
                TerritoryLifetime.PERSISTENT,
                null,
                null,
                "New",
                "Frontier",
                0xFFFFFF,
                new FrontierVisibilityData(),
                null,
                List.of(points),
                new FrontierData.PathStyle()
        );
    }

    private static FrontierChange geometryChange(FrontierData frontier, FrontierMutation mutation) {
        return FrontierChange.fromMutation(frontier, mutation);
    }

    private static BlockPos point(int x, int z) {
        return new BlockPos(x, 70, z);
    }
}
