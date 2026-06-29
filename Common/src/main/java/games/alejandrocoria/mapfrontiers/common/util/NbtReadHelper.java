package games.alejandrocoria.mapfrontiers.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class NbtReadHelper {
    public static String requireString(CompoundTag nbt, String key) {
        if (!nbt.contains(key)) {
            throw new InvalidNbtFormatException("Missing string field '" + key + "'.");
        }

        return nbt.getString(key);
    }

    public static int requireInt(CompoundTag nbt, String key) {
        if (!nbt.contains(key)) {
            throw new InvalidNbtFormatException("Missing int field '" + key + "'.");
        }

        return nbt.getInt(key);
    }

    public static long requireLong(CompoundTag nbt, String key) {
        if (!nbt.contains(key)) {
            throw new InvalidNbtFormatException("Missing long field '" + key + "'.");
        }

        return nbt.getLong(key);
    }

    public static boolean requireBoolean(CompoundTag nbt, String key) {
        if (!nbt.contains(key)) {
            throw new InvalidNbtFormatException("Missing boolean field '" + key + "'.");
        }

        return nbt.getBoolean(key);
    }

    public static boolean getBooleanOrDefault(CompoundTag nbt, String key, boolean defaultValue) {
        if (!nbt.contains(key)) {
            return defaultValue;
        }

        return requireBoolean(nbt, key);
    }

    public static CompoundTag requireCompound(CompoundTag nbt, String key) {
        Tag tag = nbt.get(key);
        if (tag instanceof CompoundTag compoundTag) {
            return compoundTag;
        }

        throw new InvalidNbtFormatException("Missing compound field '" + key + "'.");
    }

    public static CompoundTag requireCompound(ListTag list, int index, String listName) {
        if (index < 0 || index >= list.size()) {
            throw new InvalidNbtFormatException("Missing compound entry at " + listName + "[" + index + "].");
        }

        Tag tag = list.get(index);
        if (tag instanceof CompoundTag compoundTag) {
            return compoundTag;
        }

        throw new InvalidNbtFormatException("Missing compound entry at " + listName + "[" + index + "].");
    }

    public static String requireString(ListTag list, int index, String listName) {
        if (index < 0 || index >= list.size()) {
            throw new InvalidNbtFormatException("Missing string entry at " + listName + "[" + index + "].");
        }

        Tag tag = list.get(index);
        if (tag instanceof StringTag stringTag) {
            return stringTag.getAsString();
        }

        throw new InvalidNbtFormatException("Missing string entry at " + listName + "[" + index + "].");
    }

    private NbtReadHelper() {
    }
}
