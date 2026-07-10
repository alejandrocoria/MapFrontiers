package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import games.alejandrocoria.mapfrontiers.common.util.NbtCompat;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumSet;

public class FrontierVisibilityData {
    private final EnumSet<FrontierVisibility> values;

    public FrontierVisibilityData() {
        values = EnumSet.noneOf(FrontierVisibility.class);
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (visibility.getDefaultValue()) {
                values.add(visibility);
            }
        }
    }

    public FrontierVisibilityData(boolean setAll) {
        if (setAll) {
            values = EnumSet.allOf(FrontierVisibility.class);
        } else {
            values = EnumSet.noneOf(FrontierVisibility.class);
        }
    }

    public FrontierVisibilityData(FrontierVisibilityData other) {
        values = other.values.clone();
    }

    public FrontierVisibilityData normalized() {
        return new FrontierVisibilityData(this);
    }

    public FrontierVisibilityData normalized(FrontierVisibilityMask mask) {
        FrontierVisibilityData normalized = normalized();
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (!mask.has(visibility)) {
                normalized.set(visibility, visibility.getDefaultValue());
            }
        }
        return normalized;
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof FrontierVisibilityData otherVisibility) {
            return values.equals(otherVisibility.values);
        }

        return false;
    }

    public void set(FrontierVisibility visibility, boolean enabled) {
        if (enabled) {
            values.add(visibility);
        } else {
            values.remove(visibility);
        }
    }

    public boolean get(FrontierVisibility visibility) {
        return values.contains(visibility);
    }

    public boolean getFrontier() {
        return get(FrontierVisibility.Frontier);
    }

    public void applyOverride(FrontierVisibilityData override, FrontierVisibilityMask mask) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (mask.has(visibility)) {
                set(visibility, override.get(visibility));
            }
        }
    }

    public void readSparseNbt(CompoundTag nbt, FrontierVisibilityMask mask) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (!nbt.contains(visibility.getNbtKey())) {
                continue;
            }

            set(visibility, NbtCompat.getBooleanOr(nbt, visibility.getNbtKey(), false));
            mask.set(visibility, true);
        }
    }

    public void writeSparseNbt(CompoundTag nbt, FrontierVisibilityMask mask) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (!mask.has(visibility)) {
                continue;
            }

            nbt.putBoolean(visibility.getNbtKey(), get(visibility));
        }
    }

    public void readFromLegacyNBT(CompoundTag nbt) {
        set(FrontierVisibility.Frontier, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.Fullscreen, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.FullscreenName, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.FullscreenCollection, false);
        set(FrontierVisibility.FullscreenOwner, NbtReadHelper.requireBoolean(nbt, "nameVisible"));
        set(FrontierVisibility.FullscreenBanner, false);
        set(FrontierVisibility.FullscreenDay, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.FullscreenNight, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.FullscreenUnderground, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.FullscreenTopo, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.FullscreenBiome, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.Minimap, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.MinimapName, NbtReadHelper.requireBoolean(nbt, "nameVisible"));
        set(FrontierVisibility.MinimapCollection, false);
        set(FrontierVisibility.MinimapOwner, NbtReadHelper.requireBoolean(nbt, "ownerVisible"));
        set(FrontierVisibility.MinimapBanner, false);
        set(FrontierVisibility.MinimapDay, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.MinimapNight, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.MinimapUnderground, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.MinimapTopo, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.MinimapBiome, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.Webmap, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.WebmapName, NbtReadHelper.requireBoolean(nbt, "nameVisible"));
        set(FrontierVisibility.WebmapCollection, false);
        set(FrontierVisibility.WebmapOwner, NbtReadHelper.requireBoolean(nbt, "ownerVisible"));
        set(FrontierVisibility.WebmapBanner, false);
        set(FrontierVisibility.WebmapDay, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.WebmapNight, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.WebmapUnderground, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.WebmapTopo, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.WebmapBiome, NbtReadHelper.requireBoolean(nbt, "visible"));
        set(FrontierVisibility.AnnounceInChat, NbtReadHelper.getBooleanOrDefault(nbt, FrontierVisibility.AnnounceInChat.getNbtKey(), false));
        set(FrontierVisibility.AnnounceInTitle, NbtReadHelper.getBooleanOrDefault(nbt, FrontierVisibility.AnnounceInTitle.getNbtKey(), false));
        set(FrontierVisibility.MentionCollection, true);
    }

    public void readFromNBT(CompoundTag nbt) {
        set(FrontierVisibility.Frontier, NbtReadHelper.requireBoolean(nbt, FrontierVisibility.Frontier.getNbtKey()));
        set(FrontierVisibility.Fullscreen, readNbtValue(nbt, FrontierVisibility.Fullscreen));
        set(FrontierVisibility.FullscreenName, readNbtValue(nbt, FrontierVisibility.FullscreenName));
        set(FrontierVisibility.FullscreenCollection, readNbtValue(nbt, FrontierVisibility.FullscreenCollection));
        set(FrontierVisibility.FullscreenOwner, readNbtValue(nbt, FrontierVisibility.FullscreenOwner));
        set(FrontierVisibility.FullscreenBanner, readNbtValue(nbt, FrontierVisibility.FullscreenBanner));
        set(FrontierVisibility.FullscreenDay, readNbtValue(nbt, FrontierVisibility.FullscreenDay));
        set(FrontierVisibility.FullscreenNight, readNbtValue(nbt, FrontierVisibility.FullscreenNight));
        set(FrontierVisibility.FullscreenUnderground, readNbtValue(nbt, FrontierVisibility.FullscreenUnderground));
        set(FrontierVisibility.FullscreenTopo, readNbtValue(nbt, FrontierVisibility.FullscreenTopo));
        set(FrontierVisibility.FullscreenBiome, readNbtValue(nbt, FrontierVisibility.FullscreenBiome));
        set(FrontierVisibility.Minimap, readNbtValue(nbt, FrontierVisibility.Minimap));
        set(FrontierVisibility.MinimapName, readNbtValue(nbt, FrontierVisibility.MinimapName));
        set(FrontierVisibility.MinimapCollection, readNbtValue(nbt, FrontierVisibility.MinimapCollection));
        set(FrontierVisibility.MinimapOwner, readNbtValue(nbt, FrontierVisibility.MinimapOwner));
        set(FrontierVisibility.MinimapBanner, readNbtValue(nbt, FrontierVisibility.MinimapBanner));
        set(FrontierVisibility.MinimapDay, readNbtValue(nbt, FrontierVisibility.MinimapDay));
        set(FrontierVisibility.MinimapNight, readNbtValue(nbt, FrontierVisibility.MinimapNight));
        set(FrontierVisibility.MinimapUnderground, readNbtValue(nbt, FrontierVisibility.MinimapUnderground));
        set(FrontierVisibility.MinimapTopo, readNbtValue(nbt, FrontierVisibility.MinimapTopo));
        set(FrontierVisibility.MinimapBiome, readNbtValue(nbt, FrontierVisibility.MinimapBiome));
        set(FrontierVisibility.Webmap, readNbtValue(nbt, FrontierVisibility.Webmap, get(FrontierVisibility.Minimap)));
        set(FrontierVisibility.WebmapName, readNbtValue(nbt, FrontierVisibility.WebmapName, get(FrontierVisibility.MinimapName)));
        set(FrontierVisibility.WebmapCollection, readNbtValue(nbt, FrontierVisibility.WebmapCollection, get(FrontierVisibility.MinimapCollection)));
        set(FrontierVisibility.WebmapOwner, readNbtValue(nbt, FrontierVisibility.WebmapOwner, get(FrontierVisibility.MinimapOwner)));
        set(FrontierVisibility.WebmapBanner, readNbtValue(nbt, FrontierVisibility.WebmapBanner, get(FrontierVisibility.MinimapBanner)));
        set(FrontierVisibility.WebmapDay, readNbtValue(nbt, FrontierVisibility.WebmapDay, get(FrontierVisibility.MinimapDay)));
        set(FrontierVisibility.WebmapNight, readNbtValue(nbt, FrontierVisibility.WebmapNight, get(FrontierVisibility.MinimapNight)));
        set(FrontierVisibility.WebmapUnderground, readNbtValue(nbt, FrontierVisibility.WebmapUnderground, get(FrontierVisibility.MinimapUnderground)));
        set(FrontierVisibility.WebmapTopo, readNbtValue(nbt, FrontierVisibility.WebmapTopo, get(FrontierVisibility.MinimapTopo)));
        set(FrontierVisibility.WebmapBiome, readNbtValue(nbt, FrontierVisibility.WebmapBiome, get(FrontierVisibility.MinimapBiome)));
        set(FrontierVisibility.AnnounceInChat, readNbtValue(nbt, FrontierVisibility.AnnounceInChat));
        set(FrontierVisibility.AnnounceInTitle, readNbtValue(nbt, FrontierVisibility.AnnounceInTitle));
        set(FrontierVisibility.MentionCollection, readNbtValue(nbt, FrontierVisibility.MentionCollection));
    }

    public void writeToNBT(CompoundTag nbt) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            nbt.putBoolean(visibility.getNbtKey(), get(visibility));
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            set(visibility, buf.readBoolean());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            buf.writeBoolean(get(visibility));
        }
    }

    private boolean readNbtValue(CompoundTag nbt, FrontierVisibility visibility) {
        return readNbtValue(nbt, visibility, visibility.getDefaultValue());
    }

    private boolean readNbtValue(CompoundTag nbt, FrontierVisibility visibility, boolean fallback) {
        return NbtReadHelper.getBooleanOrDefault(nbt, visibility.getNbtKey(), fallback);
    }
}
