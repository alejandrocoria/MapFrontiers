package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public class BannerData {
    public DyeColor baseColor;
    public @Nullable ListTag patterns;
    public int rotation;

    public static @Nullable ListTag normalizePatterns(@Nullable ListTag patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return null;
        }

        return patterns;
    }

    public BannerData() {
        baseColor = DyeColor.WHITE;
        rotation = 0;
    }

    public BannerData(BannerData other) {
        baseColor = other.baseColor;
        patterns = normalizePatterns(other.patterns == null ? null : other.patterns.copy());
        rotation = other.rotation;
    }

    public BannerData(DyeColor baseColor, @Nullable ListTag patterns, int rotation) {
        this.baseColor = baseColor;
        this.patterns = normalizePatterns(patterns == null ? null : patterns.copy());
        this.rotation = rotation;
    }

    public void readFromNBT(CompoundTag nbt) {
        baseColor = DyeColor.byId(NbtReadHelper.requireInt(nbt, "Base"));
        patterns = normalizePatterns(nbt.getListOrEmpty("Patterns"));
        rotation = nbt.getIntOr("Rotation", 0);
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putInt("Base", baseColor.getId());

        if (patterns != null) {
            nbt.put("Patterns", patterns);
        }

        nbt.putInt("Rotation", rotation);
    }

    public void fromBytes(FriendlyByteBuf buf) {
        baseColor = DyeColor.byId(buf.readInt());

        CompoundTag nbt = buf.readNbt();
        if (nbt != null) {
            patterns = normalizePatterns(nbt.getListOrEmpty("Patterns"));
        } else {
            patterns = null;
        }

        rotation = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(baseColor.getId());

        if (patterns == null) {
            buf.writeNbt(null);
        } else {
            CompoundTag nbt = new CompoundTag();
            nbt.put("Patterns", patterns);
            buf.writeNbt(nbt);
        }

        buf.writeInt(rotation);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BannerData that)) {
            return false;
        }
        return rotation == that.rotation && baseColor == that.baseColor && Objects.equals(patterns, that.patterns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseColor, patterns, rotation);
    }
}
