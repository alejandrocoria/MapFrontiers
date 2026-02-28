package games.alejandrocoria.mapfrontiers.api.model;

import java.util.List;

public record FrontierShape(FrontierShapeType type, List<Point2i> vertices, List<ChunkCoord> chunks) {
    public FrontierShape {
        if (type == null) {
            throw new IllegalArgumentException("Shape type cannot be null");
        }

        vertices = vertices == null ? List.of() : List.copyOf(vertices);
        chunks = chunks == null ? List.of() : List.copyOf(chunks);

        if (type == FrontierShapeType.VERTEX) {
            if (vertices.isEmpty() || !chunks.isEmpty()) {
                throw new IllegalArgumentException("Vertex shape requires vertices and forbids chunks");
            }
        } else {
            if (chunks.isEmpty() || !vertices.isEmpty()) {
                throw new IllegalArgumentException("Chunk shape requires chunks and forbids vertices");
            }
        }
    }

    public static FrontierShape vertex(List<Point2i> vertices) {
        return new FrontierShape(FrontierShapeType.VERTEX, vertices, List.of());
    }

    public static FrontierShape chunk(List<ChunkCoord> chunks) {
        return new FrontierShape(FrontierShapeType.CHUNK, List.of(), chunks);
    }
}
