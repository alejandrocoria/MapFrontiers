package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import games.alejandrocoria.mapfrontiers.testutil.PeriodicGeometry;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkEditTest {
    @Test
    void additionsStayNextToTheFrontierAcrossEachWrappedAxis() {
        assertPlacement(new PeriodicGeometry(1024, 0), new ChunkPos(31, 5), new ChunkPos(-32, 5), new ChunkPos(32, 5));
        assertPlacement(new PeriodicGeometry(0, 1024), new ChunkPos(5, 31), new ChunkPos(5, -32), new ChunkPos(5, 32));
        assertPlacement(new PeriodicGeometry(1024, 1024), new ChunkPos(31, 31), new ChunkPos(-32, -32), new ChunkPos(32, 32));
    }

    @Test
    void togglingFindsAllEquivalentStoredChunksAfterRelocation() {
        WorldGeometry world = new PeriodicGeometry(1024, 1024);
        Set<ChunkPos> chunks = Set.of(new ChunkPos(32, 32), new ChunkPos(96, 96), new ChunkPos(32, 33));
        ChunkEdit edit = ChunkEdit.resolve(world, chunks, new ChunkPos(-32, -32), true);
        assertEquals(Set.of(new ChunkPos(32, 32), new ChunkPos(96, 96)), Set.copyOf(edit.existingCopies()));
        assertTrue(edit.existingCopies().contains(edit.position()));
        FrontierData frontier = chunkFrontier(chunks);
        edit.existingCopies().forEach(frontier::removeChunk);
        assertEquals(Set.of(new ChunkPos(32, 33)), frontier.getChunks());
    }

    @Test
    void paintingKeepsTheCursorCopyAndDoesNotRealignEachStep() {
        WorldGeometry world = new PeriodicGeometry(1024, 0);
        Set<ChunkPos> chunks = Set.of(new ChunkPos(31, 0), new ChunkPos(-31, 5));
        ChunkPos nextCursor = world.nearestChunkCopy(new ChunkPos(32, 0), new ChunkPos(-31, 0));
        ChunkEdit edit = ChunkEdit.resolve(world, chunks, nextCursor, false);
        assertEquals(new ChunkPos(33, 0), edit.position());
        assertTrue(edit.existingCopies().isEmpty());
        ChunkEdit overlapping = ChunkEdit.resolve(world, Set.of(new ChunkPos(33, 0)), nextCursor, false);
        assertEquals(List.of(new ChunkPos(33, 0)), overlapping.existingCopies());
    }

    @Test
    void emptyFrontierKeepsInputAndUnwrappedAxisDoesNotIdentifyDifferentChunks() {
        WorldGeometry world = new PeriodicGeometry(1024, 0);
        assertEquals(new ChunkEdit(new ChunkPos(90, 20), List.of()),
                ChunkEdit.resolve(world, Set.of(), new ChunkPos(90, 20), true));
        ChunkEdit edit = ChunkEdit.resolve(world, Set.of(new ChunkPos(1, 65)), new ChunkPos(1, 1), true);
        assertTrue(edit.existingCopies().isEmpty());
    }

    @Test
    void exactChunkMutationsDoNotFoldStoredCoordinates() {
        FrontierData frontier = chunkFrontier(Set.of(new ChunkPos(0, 0)));
        assertTrue(frontier.addChunk(new ChunkPos(64, 0)));
        assertTrue(frontier.toggleChunk(new ChunkPos(128, 0)));
        frontier.removeChunk(new ChunkPos(64, 0));
        assertEquals(Set.of(new ChunkPos(0, 0), new ChunkPos(128, 0)), frontier.getChunks());
    }

    private static void assertPlacement(WorldGeometry world, ChunkPos stored, ChunkPos input, ChunkPos expected) {
        ChunkEdit edit = ChunkEdit.resolve(world, Set.of(stored), input, true);
        assertEquals(expected, edit.position());
        assertTrue(edit.existingCopies().isEmpty());
    }

    private static FrontierData chunkFrontier(Set<ChunkPos> chunks) {
        FrontierData frontier = new FrontierData(new PlayerId(UUID.randomUUID()));
        frontier.setShape(FrontierShape.Chunk);
        chunks.forEach(frontier::addChunk);
        return frontier;
    }
}
