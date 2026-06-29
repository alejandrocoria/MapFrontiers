package games.alejandrocoria.mapfrontiers.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
public final class NbtCompat {
    private NbtCompat() {
    }

    public static boolean getBooleanOr(CompoundTag nbt, String key, boolean defaultValue) {
        return nbt.contains(key) ? nbt.getBoolean(key) : defaultValue;
    }

    public static int getIntOr(CompoundTag nbt, String key, int defaultValue) {
        return nbt.contains(key) ? nbt.getInt(key) : defaultValue;
    }

    public static String getStringOr(CompoundTag nbt, String key, String defaultValue) {
        return nbt.contains(key) ? nbt.getString(key) : defaultValue;
    }

    public static CompoundTag getCompoundOrEmpty(CompoundTag nbt, String key) {
        Tag tag = nbt.get(key);
        return tag instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
    }

    public static ListTag getListOrEmpty(CompoundTag nbt, String key) {
        Tag tag = nbt.get(key);
        return tag instanceof ListTag listTag ? listTag : new ListTag();
    }

    public static CompoundTag getCompoundOrEmpty(ListTag list, int index) {
        if (index < 0 || index >= list.size()) {
            return new CompoundTag();
        }

        Tag tag = list.get(index);
        return tag instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
    }

    public static Set<String> getAllKeys(CompoundTag nbt) {
        return nbt.getAllKeys();
    }

    public static String getStringValue(StringTag tag) {
        return tag.getAsString();
    }
}
