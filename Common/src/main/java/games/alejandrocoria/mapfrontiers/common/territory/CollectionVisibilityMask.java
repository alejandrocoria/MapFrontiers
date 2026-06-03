package games.alejandrocoria.mapfrontiers.common.territory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumSet;

public class CollectionVisibilityMask {
    private final EnumSet<CollectionVisibilityField> values;

    public CollectionVisibilityMask() {
        values = EnumSet.noneOf(CollectionVisibilityField.class);
    }

    public CollectionVisibilityMask(CollectionVisibilityMask other) {
        values = other.values.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CollectionVisibilityMask otherMask)) {
            return false;
        }

        return values.equals(otherMask.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    public boolean has(CollectionVisibilityField field) {
        return values.contains(field);
    }

    public void set(CollectionVisibilityField field, boolean enabled) {
        if (enabled) {
            values.add(field);
        } else {
            values.remove(field);
        }
    }

    public void clear() {
        values.clear();
    }

    public boolean hasAny() {
        return !values.isEmpty();
    }

    public void readFromNBT(CompoundTag nbt) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            set(field, nbt.getBooleanOr(field.getNbtKey(), false));
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            nbt.putBoolean(field.getNbtKey(), has(field));
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            set(field, buf.readBoolean());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            buf.writeBoolean(has(field));
        }
    }
}
