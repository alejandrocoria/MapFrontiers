package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
sealed interface GeometryChange permits GeometryChange.InsertPathPointAt, GeometryChange.InsertPathPointBeforeFirst,
        GeometryChange.InsertPathPointAfterLast, GeometryChange.InsertPathPointAutomatically, GeometryChange.SetPathPointAt,
        GeometryChange.RemovePathPointAt, GeometryChange.ReversePath, GeometryChange.InsertVertexAt,
        GeometryChange.InsertVertexAutomatically, GeometryChange.SetVertexAt, GeometryChange.RemoveVertexAt,
        GeometryChange.AddChunks, GeometryChange.RemoveChunks {
    int INSERT_PATH_POINT_AT = 0;
    int INSERT_PATH_POINT_BEFORE_FIRST = 1;
    int INSERT_PATH_POINT_AFTER_LAST = 2;
    int INSERT_PATH_POINT_AUTOMATICALLY = 3;
    int SET_PATH_POINT_AT = 4;
    int REMOVE_PATH_POINT_AT = 5;
    int REVERSE_PATH = 6;
    int INSERT_VERTEX_AT = 7;
    int INSERT_VERTEX_AUTOMATICALLY = 8;
    int SET_VERTEX_AT = 9;
    int REMOVE_VERTEX_AT = 10;
    int ADD_CHUNKS = 11;
    int REMOVE_CHUNKS = 12;

    static List<GeometryChange> readList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        validateCountAgainstRemainingBytes("geometry changes", count, buf.readableBytes(), 1);

        List<GeometryChange> changes = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            changes.add(read(buf));
        }
        return List.copyOf(changes);
    }

    static void writeList(FriendlyByteBuf buf, List<GeometryChange> changes) {
        buf.writeVarInt(changes.size());
        for (GeometryChange change : changes) {
            write(buf, change);
        }
    }

    private static GeometryChange read(FriendlyByteBuf buf) {
        int type = buf.readUnsignedByte();
        return switch (type) {
            case INSERT_PATH_POINT_AT -> new InsertPathPointAt(buf.readVarInt(), BlockPos.of(buf.readLong()));
            case INSERT_PATH_POINT_BEFORE_FIRST -> new InsertPathPointBeforeFirst(BlockPos.of(buf.readLong()));
            case INSERT_PATH_POINT_AFTER_LAST -> new InsertPathPointAfterLast(BlockPos.of(buf.readLong()));
            case INSERT_PATH_POINT_AUTOMATICALLY -> new InsertPathPointAutomatically(BlockPos.of(buf.readLong()));
            case SET_PATH_POINT_AT -> new SetPathPointAt(buf.readVarInt(), BlockPos.of(buf.readLong()));
            case REMOVE_PATH_POINT_AT -> new RemovePathPointAt(buf.readVarInt());
            case REVERSE_PATH -> new ReversePath();
            case INSERT_VERTEX_AT -> new InsertVertexAt(buf.readVarInt(), BlockPos.of(buf.readLong()));
            case INSERT_VERTEX_AUTOMATICALLY -> new InsertVertexAutomatically(BlockPos.of(buf.readLong()));
            case SET_VERTEX_AT -> new SetVertexAt(buf.readVarInt(), BlockPos.of(buf.readLong()));
            case REMOVE_VERTEX_AT -> new RemoveVertexAt(buf.readVarInt());
            case ADD_CHUNKS -> new AddChunks(readChunks(buf));
            case REMOVE_CHUNKS -> new RemoveChunks(readChunks(buf));
            default -> throw new IllegalArgumentException("Unknown geometry change discriminator: " + type);
        };
    }

    private static void write(FriendlyByteBuf buf, GeometryChange change) {
        switch (change) {
            case InsertPathPointAt value -> {
                buf.writeByte(INSERT_PATH_POINT_AT);
                buf.writeVarInt(value.index());
                buf.writeLong(value.point().asLong());
            }
            case InsertPathPointBeforeFirst value -> {
                buf.writeByte(INSERT_PATH_POINT_BEFORE_FIRST);
                buf.writeLong(value.point().asLong());
            }
            case InsertPathPointAfterLast value -> {
                buf.writeByte(INSERT_PATH_POINT_AFTER_LAST);
                buf.writeLong(value.point().asLong());
            }
            case InsertPathPointAutomatically value -> {
                buf.writeByte(INSERT_PATH_POINT_AUTOMATICALLY);
                buf.writeLong(value.point().asLong());
            }
            case SetPathPointAt value -> {
                buf.writeByte(SET_PATH_POINT_AT);
                buf.writeVarInt(value.index());
                buf.writeLong(value.point().asLong());
            }
            case RemovePathPointAt value -> {
                buf.writeByte(REMOVE_PATH_POINT_AT);
                buf.writeVarInt(value.index());
            }
            case ReversePath ignored -> buf.writeByte(REVERSE_PATH);
            case InsertVertexAt value -> {
                buf.writeByte(INSERT_VERTEX_AT);
                buf.writeVarInt(value.index());
                buf.writeLong(value.vertex().asLong());
            }
            case InsertVertexAutomatically value -> {
                buf.writeByte(INSERT_VERTEX_AUTOMATICALLY);
                buf.writeLong(value.vertex().asLong());
            }
            case SetVertexAt value -> {
                buf.writeByte(SET_VERTEX_AT);
                buf.writeVarInt(value.index());
                buf.writeLong(value.vertex().asLong());
            }
            case RemoveVertexAt value -> {
                buf.writeByte(REMOVE_VERTEX_AT);
                buf.writeVarInt(value.index());
            }
            case AddChunks value -> {
                buf.writeByte(ADD_CHUNKS);
                writeChunks(buf, value.chunks());
            }
            case RemoveChunks value -> {
                buf.writeByte(REMOVE_CHUNKS);
                writeChunks(buf, value.chunks());
            }
        }
    }

    private static Set<ChunkPos> readChunks(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        validateCountAgainstRemainingBytes("chunk batch", count, buf.readableBytes(), Long.BYTES);

        Set<ChunkPos> chunks = new LinkedHashSet<>();
        for (int i = 0; i < count; ++i) {
            chunks.add(ChunkPos.unpack(buf.readLong()));
        }
        return chunks;
    }

    private static void writeChunks(FriendlyByteBuf buf, Set<ChunkPos> chunks) {
        buf.writeVarInt(chunks.size());
        for (ChunkPos chunk : chunks) {
            buf.writeLong(chunk.pack());
        }
    }

    private static void validateCountAgainstRemainingBytes(String label, int count, int readableBytes, int minimumBytesPerItem) {
        if (count < 0 || count > readableBytes / minimumBytesPerItem) {
            throw new IllegalArgumentException("Invalid " + label + " count " + count + " for " + readableBytes + " remaining bytes");
        }
    }

    private static Set<ChunkPos> immutableChunks(Set<ChunkPos> chunks) {
        Objects.requireNonNull(chunks, "chunks");
        LinkedHashSet<ChunkPos> copy = new LinkedHashSet<>();
        for (ChunkPos chunk : chunks) {
            copy.add(Objects.requireNonNull(chunk, "chunk"));
        }
        return Collections.unmodifiableSet(copy);
    }

    record InsertPathPointAt(int index, BlockPos point) implements GeometryChange {
        public InsertPathPointAt {
            Objects.requireNonNull(point, "point");
        }
    }

    record InsertPathPointBeforeFirst(BlockPos point) implements GeometryChange {
        public InsertPathPointBeforeFirst {
            Objects.requireNonNull(point, "point");
        }
    }

    record InsertPathPointAfterLast(BlockPos point) implements GeometryChange {
        public InsertPathPointAfterLast {
            Objects.requireNonNull(point, "point");
        }
    }

    record InsertPathPointAutomatically(BlockPos point) implements GeometryChange {
        public InsertPathPointAutomatically {
            Objects.requireNonNull(point, "point");
        }
    }

    record SetPathPointAt(int index, BlockPos point) implements GeometryChange {
        public SetPathPointAt {
            Objects.requireNonNull(point, "point");
        }
    }

    record RemovePathPointAt(int index) implements GeometryChange {
    }

    record ReversePath() implements GeometryChange {
    }

    record InsertVertexAt(int index, BlockPos vertex) implements GeometryChange {
        public InsertVertexAt {
            Objects.requireNonNull(vertex, "vertex");
        }
    }

    record InsertVertexAutomatically(BlockPos vertex) implements GeometryChange {
        public InsertVertexAutomatically {
            Objects.requireNonNull(vertex, "vertex");
        }
    }

    record SetVertexAt(int index, BlockPos vertex) implements GeometryChange {
        public SetVertexAt {
            Objects.requireNonNull(vertex, "vertex");
        }
    }

    record RemoveVertexAt(int index) implements GeometryChange {
    }

    record AddChunks(Set<ChunkPos> chunks) implements GeometryChange {
        public AddChunks {
            chunks = immutableChunks(chunks);
        }
    }

    record RemoveChunks(Set<ChunkPos> chunks) implements GeometryChange {
        public RemoveChunks {
            chunks = immutableChunks(chunks);
        }
    }
}
