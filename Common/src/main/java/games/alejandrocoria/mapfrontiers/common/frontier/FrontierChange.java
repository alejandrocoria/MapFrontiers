package games.alejandrocoria.mapfrontiers.common.frontier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public class FrontierChange {
    private static final int MAX_NAME_CHARACTERS = 17;
    private static final int MAX_NAME_BYTES = MAX_NAME_CHARACTERS * 4;

    private @Nullable NameChange name;
    private @Nullable VisibilityChange visibility;
    private @Nullable ColorChange color;
    private @Nullable BannerChange banner;
    private @Nullable ShapeChange shape;
    private @Nullable Long modifiedTime;

    public FrontierChange() {
    }

    public FrontierChange(FrontierChange other) {
        if (other.name != null) {
            name = new NameChange(other.name.name1, other.name.name2);
        }
        if (other.visibility != null) {
            visibility = new VisibilityChange(new FrontierData.VisibilityData(other.visibility.visibilityData));
        }
        if (other.color != null) {
            color = new ColorChange(other.color.color);
        }
        if (other.banner != null) {
            banner = new BannerChange(other.banner.banner == null ? null : new FrontierData.BannerData(other.banner.banner));
        }
        if (other.shape != null) {
            shape = new ShapeChange(other.shape.vertices, other.shape.chunks, other.shape.mode);
        }
        modifiedTime = other.modifiedTime;
    }

    public FrontierChange(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            name = new NameChange(buf.readUtf(MAX_NAME_BYTES), buf.readUtf(MAX_NAME_BYTES));
        }

        if (buf.readBoolean()) {
            FrontierData.VisibilityData visibilityData = new FrontierData.VisibilityData();
            visibilityData.fromBytes(buf);
            visibility = new VisibilityChange(visibilityData);
        }

        if (buf.readBoolean()) {
            color = new ColorChange(buf.readInt());
        }

        if (buf.readBoolean()) {
            FrontierData.BannerData bannerData = null;
            if (buf.readBoolean()) {
                bannerData = new FrontierData.BannerData();
                bannerData.fromBytes(buf);
            }
            banner = new BannerChange(bannerData);
        }

        if (buf.readBoolean()) {
            int verticesCount = buf.readInt();
            List<BlockPos> vertices = new ArrayList<>(verticesCount);
            for (int i = 0; i < verticesCount; ++i) {
                vertices.add(BlockPos.of(buf.readLong()));
            }

            int chunksCount = buf.readInt();
            Set<ChunkPos> chunks = new HashSet<>(chunksCount);
            for (int i = 0; i < chunksCount; ++i) {
                chunks.add(new ChunkPos(buf.readLong()));
            }

            FrontierData.Mode mode = FrontierData.Mode.values()[buf.readInt()];
            shape = new ShapeChange(vertices, chunks, mode);
        }

        if (buf.readBoolean()) {
            modifiedTime = buf.readLong();
        }
    }

    public static FrontierChange fromFrontierData(FrontierData frontier) {
        return fromFrontierData(frontier, false);
    }

    public static FrontierChange fromFrontierData(FrontierData frontier, boolean includeModifiedTime) {
        FrontierChange change = new FrontierChange();
        change.setName(frontier.getName1(), frontier.getName2());
        change.setVisibility(frontier.getVisibilityData());
        change.setColor(frontier.getColor());
        change.setBanner(frontier.getbannerData());
        change.setShape(frontier.getVertices(), frontier.getChunks(), frontier.getMode());

        if (includeModifiedTime && frontier.getModified() != null) {
            change.setModifiedTime(frontier.getModified().getTime());
        }

        return change;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(name != null);
        if (name != null) {
            buf.writeUtf(name.name1, MAX_NAME_BYTES);
            buf.writeUtf(name.name2, MAX_NAME_BYTES);
        }

        buf.writeBoolean(visibility != null);
        if (visibility != null) {
            visibility.visibilityData.toBytes(buf);
        }

        buf.writeBoolean(color != null);
        if (color != null) {
            buf.writeInt(color.color);
        }

        buf.writeBoolean(banner != null);
        if (banner != null) {
            if (banner.banner == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                banner.banner.toBytes(buf);
            }
        }

        buf.writeBoolean(shape != null);
        if (shape != null) {
            buf.writeInt(shape.vertices.size());
            for (BlockPos vertex : shape.vertices) {
                buf.writeLong(vertex.asLong());
            }

            buf.writeInt(shape.chunks.size());
            for (ChunkPos chunk : shape.chunks) {
                buf.writeLong(chunk.toLong());
            }

            buf.writeInt(shape.mode.ordinal());
        }

        buf.writeBoolean(modifiedTime != null);
        if (modifiedTime != null) {
            buf.writeLong(modifiedTime);
        }
    }

    public boolean isEmpty() {
        return name == null && visibility == null && color == null && banner == null && shape == null && modifiedTime == null;
    }

    public @Nullable NameChange getName() {
        return name;
    }

    public @Nullable VisibilityChange getVisibility() {
        return visibility;
    }

    public @Nullable ColorChange getColor() {
        return color;
    }

    public @Nullable BannerChange getBanner() {
        return banner;
    }

    public @Nullable ShapeChange getShape() {
        return shape;
    }

    public @Nullable Long getModifiedTime() {
        return modifiedTime;
    }

    public boolean hasNameChange() {
        return name != null;
    }

    public boolean hasVisibilityChange() {
        return visibility != null;
    }

    public boolean hasColorChange() {
        return color != null;
    }

    public boolean hasBannerChange() {
        return banner != null;
    }

    public boolean hasShapeChange() {
        return shape != null;
    }

    public boolean hasModifiedTime() {
        return modifiedTime != null;
    }

    public void setName(String name1, String name2) {
        name = new NameChange(name1, name2);
    }

    public void setVisibility(FrontierData.VisibilityData visibilityData) {
        visibility = new VisibilityChange(new FrontierData.VisibilityData(visibilityData));
    }

    public void setColor(int color) {
        this.color = new ColorChange(color);
    }

    public void setBanner(@Nullable FrontierData.BannerData banner) {
        this.banner = new BannerChange(banner == null ? null : new FrontierData.BannerData(banner));
    }

    public void setShape(List<BlockPos> vertices, Set<ChunkPos> chunks, FrontierData.Mode mode) {
        shape = new ShapeChange(vertices, chunks, mode);
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public static class NameChange {
        private final String name1;
        private final String name2;

        private NameChange(String name1, String name2) {
            this.name1 = name1;
            this.name2 = name2;
        }

        public String getName1() {
            return name1;
        }

        public String getName2() {
            return name2;
        }
    }

    public static class VisibilityChange {
        private final FrontierData.VisibilityData visibilityData;

        private VisibilityChange(FrontierData.VisibilityData visibilityData) {
            this.visibilityData = visibilityData;
        }

        public FrontierData.VisibilityData getVisibilityData() {
            return new FrontierData.VisibilityData(visibilityData);
        }
    }

    public static class ColorChange {
        private final int color;

        private ColorChange(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    public static class BannerChange {
        private final @Nullable FrontierData.BannerData banner;

        private BannerChange(@Nullable FrontierData.BannerData banner) {
            this.banner = banner;
        }

        public @Nullable FrontierData.BannerData getBanner() {
            return banner == null ? null : new FrontierData.BannerData(banner);
        }
    }

    public static class ShapeChange {
        private final List<BlockPos> vertices;
        private final Set<ChunkPos> chunks;
        private final FrontierData.Mode mode;

        private ShapeChange(List<BlockPos> vertices, Set<ChunkPos> chunks, FrontierData.Mode mode) {
            this.vertices = new ArrayList<>(vertices);
            this.chunks = new HashSet<>(chunks);
            this.mode = mode;
        }

        public List<BlockPos> getVertices() {
            return new ArrayList<>(vertices);
        }

        public Set<ChunkPos> getChunks() {
            return new HashSet<>(chunks);
        }

        public FrontierData.Mode getMode() {
            return mode;
        }
    }
}
