package games.alejandrocoria.mapfrontiers.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class NbtReadHelper {
    private NbtReadHelper() {
    }

    public static String requireString(CompoundTag nbt, String key) {
        return nbt.getString(key)
                .orElseThrow(() -> new InvalidNbtFormatException("Missing string field '" + key + "'."));
    }

    public static int requireInt(CompoundTag nbt, String key) {
        return nbt.getInt(key)
                .orElseThrow(() -> new InvalidNbtFormatException("Missing int field '" + key + "'."));
    }

    public static long requireLong(CompoundTag nbt, String key) {
        return nbt.getLong(key)
                .orElseThrow(() -> new InvalidNbtFormatException("Missing long field '" + key + "'."));
    }

    public static boolean requireBoolean(CompoundTag nbt, String key) {
        return nbt.getBoolean(key)
                .orElseThrow(() -> new InvalidNbtFormatException("Missing boolean field '" + key + "'."));
    }

    public static boolean getBooleanOrDefault(CompoundTag nbt, String key, boolean defaultValue) {
        if (!nbt.contains(key)) {
            return defaultValue;
        }

        return requireBoolean(nbt, key);
    }

    public static CompoundTag requireCompound(CompoundTag nbt, String key) {
        return nbt.getCompound(key)
                .orElseThrow(() -> new InvalidNbtFormatException("Missing compound field '" + key + "'."));
    }

    public static CompoundTag requireCompound(ListTag list, int index, String listName) {
        return list.getCompound(index)
                .orElseThrow(() -> new InvalidNbtFormatException("Missing compound entry at " + listName + "[" + index + "]."));
    }

    public static String requireString(ListTag list, int index, String listName) {
        return list.getString(index)
                .orElseThrow(() -> new InvalidNbtFormatException("Missing string entry at " + listName + "[" + index + "]."));
    }
}
