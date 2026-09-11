package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryChangeTest {
    @Test
    void appliesOrderedPathChangesAgainstProgressiveState() {
        FrontierData frontier = pathFrontier(point(0, 0), point(10, 0), point(20, 0));
        FrontierChange change = geometryChange(
                new GeometryChange.RemovePathPointAt(0),
                new GeometryChange.SetPathPointAt(1, point(25, 0)),
                new GeometryChange.InsertPathPointAt(1, point(15, 0)),
                new GeometryChange.ReversePath()
        );

        FrontierChangeApplicationResult result = frontier.applyChange(change);

        assertTrue(result.isApplied());
        assertEquals(List.of(point(25, 0), point(15, 0), point(10, 0)), frontier.getPoints());
        assertTrue(result.effectiveChange().affectsGeometry());
    }

    @Test
    void rejectsInvalidOperationWithoutApplyingEarlierFieldsOrGeometry() {
        FrontierData frontier = pathFrontier(point(0, 0), point(10, 0));
        frontier.setName1("Before");
        long initialHash = frontier.computeSyncHash();
        FrontierChange change = geometryChange(
                new GeometryChange.RemovePathPointAt(0),
                new GeometryChange.SetPathPointAt(5, point(20, 0))
        );
        change.setName("After", frontier.getName2());

        FrontierChangeApplicationResult result = frontier.applyChange(change);

        assertTrue(result.isRejected());
        assertEquals("Before", frontier.getName1());
        assertEquals(List.of(point(0, 0), point(10, 0)), frontier.getPoints());
        assertEquals(initialHash, frontier.computeSyncHash());
    }

    @Test
    void rejectsGeometryForIncompatibleShape() {
        FrontierData frontier = vertexFrontier(point(0, 0), point(10, 0));

        FrontierChangeApplicationResult result = frontier.applyChange(
                geometryChange(new GeometryChange.InsertPathPointAfterLast(point(20, 0)))
        );

        assertTrue(result.isRejected());
        assertEquals(List.of(point(0, 0), point(10, 0)), frontier.getVertices());
    }

    @Test
    void treatsIdempotentChunkBatchAsNoChange() {
        FrontierData frontier = new FrontierData(new PlayerId(UUID.randomUUID()));
        frontier.setShape(FrontierShape.Chunk);
        frontier.addChunk(new ChunkPos(2, 3));
        frontier.setModified(new Date(123L));
        FrontierChange change = geometryChange(new GeometryChange.AddChunks(Set.of(new ChunkPos(2, 3))));
        change.setModifiedTime(456L);

        FrontierChangeApplicationResult result = frontier.applyChange(change);

        assertTrue(result.isNoChange());
        assertEquals(Set.of(new ChunkPos(2, 3)), frontier.getChunks());
        assertEquals(123L, frontier.getModified().getTime());
    }

    @Test
    void chunkGeometryCommitInvalidatesCachedChunkHash() {
        FrontierData frontier = new FrontierData(new PlayerId(UUID.randomUUID()));
        frontier.setShape(FrontierShape.Chunk);
        frontier.addChunk(new ChunkPos(1, 1));
        long initialHash = frontier.computeSyncHash();

        FrontierChangeApplicationResult result = frontier.applyChange(
                geometryChange(new GeometryChange.AddChunks(Set.of(new ChunkPos(2, 2))))
        );

        assertTrue(result.isApplied());
        assertNotEquals(initialHash, frontier.computeSyncHash());
        assertEquals(new FrontierData(frontier).computeSyncHash(), frontier.computeSyncHash());
    }

    @Test
    void omitsNeutralGeometryWhenAnotherFieldChanges() {
        FrontierData frontier = new FrontierData(new PlayerId(UUID.randomUUID()));
        frontier.setShape(FrontierShape.Chunk);
        frontier.addChunk(new ChunkPos(2, 3));
        FrontierChange change = geometryChange(new GeometryChange.AddChunks(Set.of(new ChunkPos(2, 3))));
        change.setName("Changed", frontier.getName2());

        FrontierChangeApplicationResult result = frontier.applyChange(change);

        assertTrue(result.isApplied());
        assertEquals("Changed", frontier.getName1());
        assertFalse(result.effectiveChange().hasGeometryChanges());
        assertFalse(result.effectiveChange().affectsGeometry());
    }

    @Test
    void automaticPathInsertionUsesFirstSegmentAndFinalEndpointTieRules() {
        FrontierData segmented = pathFrontier(point(0, 0), point(10, 0), point(20, 0));
        FrontierChangeApplicationResult segmentResult = segmented.applyChange(
                geometryChange(new GeometryChange.InsertPathPointAutomatically(point(10, 5)))
        );

        assertTrue(segmentResult.isApplied());
        assertEquals(point(10, 5), segmented.getPoints().get(1));

        FrontierData degenerate = pathFrontier(point(0, 0), point(0, 0));
        degenerate.applyChange(geometryChange(new GeometryChange.InsertPathPointAutomatically(point(10, 0))));
        assertEquals(point(10, 0), degenerate.getPoints().getLast());
    }

    @Test
    void automaticVertexInsertionIncludesClosingEdge() {
        FrontierData frontier = vertexFrontier(point(0, 0), point(10, 0), point(10, 10));

        FrontierChangeApplicationResult result = frontier.applyChange(
                geometryChange(new GeometryChange.InsertVertexAutomatically(point(0, 5)))
        );

        assertTrue(result.isApplied());
        assertEquals(point(0, 5), frontier.getVertices().getLast());
    }

    @Test
    void geometryCodecPreservesOrderAndRejectsInvalidCountsAndTags() {
        List<GeometryChange> expected = List.of(
                new GeometryChange.InsertPathPointAt(1, point(4, 5)),
                new GeometryChange.ReversePath(),
                new GeometryChange.AddChunks(Set.of(new ChunkPos(7, 8)))
        );
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        GeometryChange.writeList(encoded, expected);

        assertEquals(expected, GeometryChange.readList(encoded));

        FriendlyByteBuf invalidCount = new FriendlyByteBuf(Unpooled.buffer());
        invalidCount.writeVarInt(2);
        invalidCount.writeByte(GeometryChange.REVERSE_PATH);
        assertThrows(IllegalArgumentException.class, () -> GeometryChange.readList(invalidCount));

        FriendlyByteBuf invalidTag = new FriendlyByteBuf(Unpooled.buffer());
        invalidTag.writeVarInt(1);
        invalidTag.writeByte(255);
        assertThrows(IllegalArgumentException.class, () -> GeometryChange.readList(invalidTag));
    }

    @Test
    void frontierChangeCopyAndCodecPreserveIncrementalGeometry() {
        FrontierChange original = geometryChange(
                new GeometryChange.InsertVertexAt(0, point(1, 2)),
                new GeometryChange.RemoveVertexAt(1)
        );
        original.setColor(0x123456);

        FrontierChange copy = new FrontierChange(original);
        assertEquals(original.getGeometryChanges(), copy.getGeometryChanges());
        assertEquals(0x123456, copy.getColor().getColor());

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        original.toBytes(encoded);
        FrontierChange decoded = new FrontierChange(encoded);
        assertEquals(original.getGeometryChanges(), decoded.getGeometryChanges());
        assertEquals(0x123456, decoded.getColor().getColor());
        assertFalse(decoded.hasShapeChange());
    }

    @Test
    void fullShapeReplacementStillRoundTripsWithoutIncrementalGeometry() {
        FrontierChange original = new FrontierChange();
        original.setShape(List.of(point(0, 0), point(10, 0)), Set.of(), List.of(), FrontierShape.Vertex);
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());

        original.toBytes(encoded);
        FrontierChange decoded = new FrontierChange(encoded);

        assertTrue(decoded.hasShapeChange());
        assertFalse(decoded.hasGeometryChanges());
        assertEquals(List.of(point(0, 0), point(10, 0)), decoded.getShape().getVertices());
    }

    @Test
    void automaticInsertionHandlesEmptyAndMinimalShapes() {
        FrontierData emptyPath = pathFrontier();
        emptyPath.applyChange(geometryChange(new GeometryChange.InsertPathPointAutomatically(point(1, 2))));
        assertEquals(List.of(point(1, 2)), emptyPath.getPoints());

        FrontierData onePointPath = pathFrontier(point(0, 0));
        onePointPath.applyChange(geometryChange(new GeometryChange.InsertPathPointAutomatically(point(1, 2))));
        assertEquals(List.of(point(0, 0), point(1, 2)), onePointPath.getPoints());

        FrontierData emptyVertex = vertexFrontier();
        emptyVertex.applyChange(geometryChange(new GeometryChange.InsertVertexAutomatically(point(1, 2))));
        assertEquals(List.of(point(1, 2)), emptyVertex.getVertices());

        FrontierData oneVertex = vertexFrontier(point(0, 0));
        oneVertex.applyChange(geometryChange(new GeometryChange.InsertVertexAutomatically(point(1, 2))));
        assertEquals(List.of(point(0, 0), point(1, 2)), oneVertex.getVertices());
    }

    @Test
    void recordsRepresentativeLargePayloadSizes() {
        List<GeometryChange> pathChanges = new ArrayList<>();
        for (int i = 0; i < 10_000; ++i) {
            pathChanges.add(new GeometryChange.InsertPathPointAutomatically(point(i, -i)));
        }
        FrontierChange pathChange = new FrontierChange();
        pathChange.setGeometryChanges(pathChanges);
        FriendlyByteBuf encodedPath = new FriendlyByteBuf(Unpooled.buffer());
        pathChange.toBytes(encodedPath);

        Set<ChunkPos> chunks = new LinkedHashSet<>();
        for (int i = 0; i < 10_000; ++i) {
            chunks.add(new ChunkPos(i, -i));
        }
        FrontierChange chunkChange = geometryChange(new GeometryChange.AddChunks(chunks));
        FriendlyByteBuf encodedChunks = new FriendlyByteBuf(Unpooled.buffer());
        chunkChange.toBytes(encodedChunks);

        assertEquals(90_011, encodedPath.readableBytes());
        assertEquals(80_013, encodedChunks.readableBytes());
    }

    @Test
    void shapeReplacementAndIncrementalGeometryCannotCoexist() {
        FrontierChange change = geometryChange(new GeometryChange.ReversePath());

        assertThrows(IllegalStateException.class,
                () -> change.setShape(List.of(), Set.of(), List.of(point(0, 0)), FrontierShape.Path));
    }

    @Test
    void mapsPublicPathEditsAndAppliesThemInOrder() {
        FrontierData frontier = pathFrontier(point(0, 0), point(10, 0));
        FrontierMutation mutation = FrontierMutation.builder()
                .insertPathPointBeforeFirst(new Point2i(-10, 0))
                .insertPathPointAfterLast(new Point2i(20, 0))
                .setPathPointAt(1, new Point2i(1, 1))
                .removePathPointAt(2)
                .reversePath()
                .build();

        FrontierChange change = FrontierChange.fromMutation(frontier, mutation);
        FrontierChangeApplicationResult result = frontier.applyChange(change);

        assertTrue(result.isApplied());
        assertEquals(List.of(point(20, 0), point(1, 1), point(-10, 0)), frontier.getPoints());
        assertEquals(List.of(
                GeometryChange.InsertPathPointBeforeFirst.class,
                GeometryChange.InsertPathPointAfterLast.class,
                GeometryChange.SetPathPointAt.class,
                GeometryChange.RemovePathPointAt.class,
                GeometryChange.ReversePath.class
        ), change.getGeometryChanges().stream().map(Object::getClass).toList());
    }

    @Test
    void mapsEveryRemainingPublicGeometryEditVariant() {
        FrontierData path = pathFrontier(point(0, 0), point(10, 0));
        FrontierChange pathChange = FrontierChange.fromMutation(path, FrontierMutation.builder()
                .insertPathPointAt(1, new Point2i(3, 4))
                .insertPathPointAutomatically(new Point2i(5, 2))
                .build());
        assertEquals(List.of(GeometryChange.InsertPathPointAt.class, GeometryChange.InsertPathPointAutomatically.class),
                pathChange.getGeometryChanges().stream().map(Object::getClass).toList());

        FrontierData vertex = vertexFrontier(point(0, 0), point(10, 0), point(10, 10));
        FrontierChange vertexChange = FrontierChange.fromMutation(vertex, FrontierMutation.builder()
                .insertVertexAt(1, new Point2i(3, 4))
                .insertVertexAutomatically(new Point2i(5, 2))
                .setVertexAt(0, new Point2i(1, 1))
                .removeVertexAt(2)
                .build());
        assertEquals(List.of(
                GeometryChange.InsertVertexAt.class,
                GeometryChange.InsertVertexAutomatically.class,
                GeometryChange.SetVertexAt.class,
                GeometryChange.RemoveVertexAt.class
        ), vertexChange.getGeometryChanges().stream().map(Object::getClass).toList());

        FrontierData chunks = new FrontierData(new PlayerId(UUID.randomUUID()));
        chunks.setShape(FrontierShape.Chunk);
        FrontierChange chunkChange = FrontierChange.fromMutation(chunks, FrontierMutation.builder()
                .addChunk(new ChunkCoord(1, 2))
                .addChunks(Set.of(new ChunkCoord(3, 4)))
                .removeChunk(new ChunkCoord(5, 6))
                .removeChunks(Set.of(new ChunkCoord(7, 8)))
                .build());
        assertEquals(List.of(
                GeometryChange.AddChunks.class,
                GeometryChange.AddChunks.class,
                GeometryChange.RemoveChunks.class,
                GeometryChange.RemoveChunks.class
        ), chunkChange.getGeometryChanges().stream().map(Object::getClass).toList());
    }

    @Test
    void publicMutationWithInvalidProgressiveIndexIsRejectedAtomically() {
        FrontierData frontier = pathFrontier(point(0, 0));
        frontier.setColor(0x112233);
        FrontierChange change = FrontierChange.fromMutation(frontier, FrontierMutation.builder()
                .color(0x445566)
                .removePathPointAt(0)
                .setPathPointAt(0, new Point2i(5, 5))
                .build());

        FrontierChangeApplicationResult result = frontier.applyChange(change);

        assertTrue(result.isRejected());
        assertEquals(0x112233, frontier.getColor());
        assertEquals(List.of(point(0, 0)), frontier.getPoints());
    }

    private static FrontierData pathFrontier(BlockPos... points) {
        FrontierData frontier = new FrontierData(new PlayerId(UUID.randomUUID()));
        frontier.setShape(FrontierShape.Path);
        for (BlockPos point : points) {
            frontier.addPoint(point);
        }
        return frontier;
    }

    private static FrontierData vertexFrontier(BlockPos... vertices) {
        FrontierData frontier = new FrontierData(new PlayerId(UUID.randomUUID()));
        frontier.setShape(FrontierShape.Vertex);
        for (BlockPos vertex : vertices) {
            frontier.addVertex(vertex);
        }
        return frontier;
    }

    private static FrontierChange geometryChange(GeometryChange... changes) {
        FrontierChange change = new FrontierChange();
        change.setGeometryChanges(List.of(changes));
        return change;
    }

    private static BlockPos point(int x, int z) {
        return new BlockPos(x, 70, z);
    }
}
