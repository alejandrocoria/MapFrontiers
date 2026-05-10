package games.alejandrocoria.mapfrontiers.common.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Date;

@ParametersAreNonnullByDefault
public final class FrontierCreationFactory {
    public static FrontierData createFrontier(FrontierCreateSpec spec) {
        FrontierData frontier = new FrontierData();
        frontier.setId(spec.getFrontierId());
        frontier.setOwner(spec.getOwner());
        frontier.setDimension(spec.getDimension());
        frontier.setPersonal(spec.isPersonal());
        frontier.setLifetime(spec.getLifetime());
        frontier.setCreated(new Date());
        applySpecShape(frontier, spec);
        FrontierCreateMetadataApplier.applyInitialMetadata(frontier, spec);
        return frontier;
    }

    private static void applySpecShape(FrontierData frontier, FrontierCreateSpec spec) {
        frontier.clearVertices();
        frontier.clearChunks();
        frontier.clearPoints();

        switch (spec.getMode()) {
            case Vertex -> {
                frontier.setShape(FrontierShape.Vertex);
                for (BlockPos vertex : spec.getVertices()) {
                    frontier.addVertex(vertex);
                }
            }
            case Chunk -> {
                frontier.setShape(FrontierShape.Chunk);
                for (ChunkPos chunk : spec.getChunks()) {
                    frontier.addChunk(chunk);
                }
            }
            case Path -> {
                frontier.setShape(FrontierShape.Path);
                for (BlockPos point : spec.getPoints()) {
                    frontier.addPoint(point);
                }
            }
        }
    }

    private FrontierCreationFactory() {
    }
}
