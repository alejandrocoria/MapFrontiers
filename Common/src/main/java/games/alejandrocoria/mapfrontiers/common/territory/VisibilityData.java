package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumSet;

public class VisibilityData {
    private final EnumSet<FrontierVisibility> values;

    public VisibilityData() {
        values = EnumSet.of(
                FrontierVisibility.Frontier,
                FrontierVisibility.MentionCollection,
                FrontierVisibility.Fullscreen,
                FrontierVisibility.FullscreenName,
                FrontierVisibility.FullscreenDay,
                FrontierVisibility.FullscreenNight,
                FrontierVisibility.FullscreenUnderground,
                FrontierVisibility.FullscreenTopo,
                FrontierVisibility.FullscreenBiome,
                FrontierVisibility.Minimap,
                FrontierVisibility.MinimapName,
                FrontierVisibility.MinimapDay,
                FrontierVisibility.MinimapNight,
                FrontierVisibility.MinimapUnderground,
                FrontierVisibility.MinimapTopo,
                FrontierVisibility.MinimapBiome,
                FrontierVisibility.Webmap,
                FrontierVisibility.WebmapName,
                FrontierVisibility.WebmapDay,
                FrontierVisibility.WebmapNight,
                FrontierVisibility.WebmapUnderground,
                FrontierVisibility.WebmapTopo,
                FrontierVisibility.WebmapBiome
        );
    }

    public VisibilityData(boolean setAll) {
        if (setAll) {
            values = EnumSet.allOf(FrontierVisibility.class);
        }
        else {
            values = EnumSet.noneOf(FrontierVisibility.class);
        }
    }

    public VisibilityData(VisibilityData other) {
        values = other.values.clone();
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

        if (other instanceof VisibilityData otherVisibility) {
            return values.equals(otherVisibility.values);
        }

        return false;
    }

    public void setValue(FrontierVisibility value, boolean set) {
        if (set) {
            values.add(value);
        }
        else {
            values.remove(value);
        }
    }

    public boolean getValue(FrontierVisibility value) {
        return values.contains(value);
    }

    public boolean hasSome() {
        return !values.isEmpty();
    }

    public void readFromLegacyNBT(CompoundTag nbt) {
        setValue(FrontierVisibility.Frontier, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.Fullscreen, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.FullscreenName, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.FullscreenCollection, false);
        setValue(FrontierVisibility.FullscreenOwner, NbtReadHelper.requireBoolean(nbt, "nameVisible"));
        setValue(FrontierVisibility.FullscreenBanner, false);
        setValue(FrontierVisibility.FullscreenDay, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.FullscreenNight, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.FullscreenUnderground, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.FullscreenTopo, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.FullscreenBiome, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.Minimap, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.MinimapName, NbtReadHelper.requireBoolean(nbt, "nameVisible"));
        setValue(FrontierVisibility.MinimapCollection, false);
        setValue(FrontierVisibility.MinimapOwner, NbtReadHelper.requireBoolean(nbt, "ownerVisible"));
        setValue(FrontierVisibility.MinimapBanner, false);
        setValue(FrontierVisibility.MinimapDay, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.MinimapNight, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.MinimapUnderground, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.MinimapTopo, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.MinimapBiome, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.Webmap, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.WebmapName, NbtReadHelper.requireBoolean(nbt, "nameVisible"));
        setValue(FrontierVisibility.WebmapCollection, false);
        setValue(FrontierVisibility.WebmapOwner, NbtReadHelper.requireBoolean(nbt, "ownerVisible"));
        setValue(FrontierVisibility.WebmapBanner, false);
        setValue(FrontierVisibility.WebmapDay, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.WebmapNight, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.WebmapUnderground, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.WebmapTopo, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.WebmapBiome, NbtReadHelper.requireBoolean(nbt, "visible"));

        setValue(FrontierVisibility.AnnounceInChat, NbtReadHelper.getBooleanOrDefault(nbt, "announceInChat", false));
        setValue(FrontierVisibility.AnnounceInTitle, NbtReadHelper.getBooleanOrDefault(nbt, "announceInTitle", false));
        setValue(FrontierVisibility.MentionCollection, true);
    }

    public void readFromNBT(CompoundTag nbt) {
        setValue(FrontierVisibility.Frontier, NbtReadHelper.requireBoolean(nbt, "visible"));
        setValue(FrontierVisibility.Fullscreen, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenVisible", true));
        setValue(FrontierVisibility.FullscreenName, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenNameVisible", true));
        setValue(FrontierVisibility.FullscreenCollection, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenCollectionVisible", false));
        setValue(FrontierVisibility.FullscreenOwner, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenOwnerVisible", false));
        setValue(FrontierVisibility.FullscreenBanner, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenBannerVisible", false));
        setValue(FrontierVisibility.FullscreenDay, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenDay", true));
        setValue(FrontierVisibility.FullscreenNight, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenNight", true));
        setValue(FrontierVisibility.FullscreenUnderground, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenUnderground", true));
        setValue(FrontierVisibility.FullscreenTopo, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenTopo", true));
        setValue(FrontierVisibility.FullscreenBiome, NbtReadHelper.getBooleanOrDefault(nbt, "fullscreenBiome", true));
        setValue(FrontierVisibility.Minimap, NbtReadHelper.getBooleanOrDefault(nbt, "minimapVisible", true));
        setValue(FrontierVisibility.MinimapName, NbtReadHelper.getBooleanOrDefault(nbt, "minimapNameVisible", true));
        setValue(FrontierVisibility.MinimapCollection, NbtReadHelper.getBooleanOrDefault(nbt, "minimapCollectionVisible", false));
        setValue(FrontierVisibility.MinimapOwner, NbtReadHelper.getBooleanOrDefault(nbt, "minimapOwnerVisible", false));
        setValue(FrontierVisibility.MinimapBanner, NbtReadHelper.getBooleanOrDefault(nbt, "minimapBannerVisible", false));
        setValue(FrontierVisibility.MinimapDay, NbtReadHelper.getBooleanOrDefault(nbt, "minimapDay", true));
        setValue(FrontierVisibility.MinimapNight, NbtReadHelper.getBooleanOrDefault(nbt, "minimapNight", true));
        setValue(FrontierVisibility.MinimapUnderground, NbtReadHelper.getBooleanOrDefault(nbt, "minimapUnderground", true));
        setValue(FrontierVisibility.MinimapTopo, NbtReadHelper.getBooleanOrDefault(nbt, "minimapTopo", true));
        setValue(FrontierVisibility.MinimapBiome, NbtReadHelper.getBooleanOrDefault(nbt, "minimapBiome", true));
        setValue(FrontierVisibility.Webmap, NbtReadHelper.getBooleanOrDefault(nbt, "webmapVisible", getValue(FrontierVisibility.Minimap)));
        setValue(FrontierVisibility.WebmapName, NbtReadHelper.getBooleanOrDefault(nbt, "webmapNameVisible", getValue(FrontierVisibility.MinimapName)));
        setValue(FrontierVisibility.WebmapCollection, NbtReadHelper.getBooleanOrDefault(nbt, "webmapCollectionVisible", getValue(FrontierVisibility.MinimapCollection)));
        setValue(FrontierVisibility.WebmapOwner, NbtReadHelper.getBooleanOrDefault(nbt, "webmapOwnerVisible", getValue(FrontierVisibility.MinimapOwner)));
        setValue(FrontierVisibility.WebmapBanner, NbtReadHelper.getBooleanOrDefault(nbt, "webmapBannerVisible", getValue(FrontierVisibility.MinimapBanner)));
        setValue(FrontierVisibility.WebmapDay, NbtReadHelper.getBooleanOrDefault(nbt, "webmapDay", getValue(FrontierVisibility.MinimapDay)));
        setValue(FrontierVisibility.WebmapNight, NbtReadHelper.getBooleanOrDefault(nbt, "webmapNight", getValue(FrontierVisibility.MinimapNight)));
        setValue(FrontierVisibility.WebmapUnderground, NbtReadHelper.getBooleanOrDefault(nbt, "webmapUnderground", getValue(FrontierVisibility.MinimapUnderground)));
        setValue(FrontierVisibility.WebmapTopo, NbtReadHelper.getBooleanOrDefault(nbt, "webmapTopo", getValue(FrontierVisibility.MinimapTopo)));
        setValue(FrontierVisibility.WebmapBiome, NbtReadHelper.getBooleanOrDefault(nbt, "webmapBiome", getValue(FrontierVisibility.MinimapBiome)));
        setValue(FrontierVisibility.AnnounceInChat, NbtReadHelper.getBooleanOrDefault(nbt, "announceInChat", false));
        setValue(FrontierVisibility.AnnounceInTitle, NbtReadHelper.getBooleanOrDefault(nbt, "announceInTitle", false));
        setValue(FrontierVisibility.MentionCollection, NbtReadHelper.getBooleanOrDefault(nbt, "mentionCollection", true));
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("visible", getValue(FrontierVisibility.Frontier));
        nbt.putBoolean("announceInChat", getValue(FrontierVisibility.AnnounceInChat));
        nbt.putBoolean("announceInTitle", getValue(FrontierVisibility.AnnounceInTitle));
        nbt.putBoolean("mentionCollection", getValue(FrontierVisibility.MentionCollection));
        nbt.putBoolean("fullscreenVisible", getValue(FrontierVisibility.Fullscreen));
        nbt.putBoolean("fullscreenNameVisible", getValue(FrontierVisibility.FullscreenName));
        nbt.putBoolean("fullscreenCollectionVisible", getValue(FrontierVisibility.FullscreenCollection));
        nbt.putBoolean("fullscreenOwnerVisible", getValue(FrontierVisibility.FullscreenOwner));
        nbt.putBoolean("fullscreenBannerVisible", getValue(FrontierVisibility.FullscreenBanner));
        nbt.putBoolean("fullscreenDay", getValue(FrontierVisibility.FullscreenDay));
        nbt.putBoolean("fullscreenNight", getValue(FrontierVisibility.FullscreenNight));
        nbt.putBoolean("fullscreenUnderground", getValue(FrontierVisibility.FullscreenUnderground));
        nbt.putBoolean("fullscreenTopo", getValue(FrontierVisibility.FullscreenTopo));
        nbt.putBoolean("fullscreenBiome", getValue(FrontierVisibility.FullscreenBiome));
        nbt.putBoolean("minimapVisible", getValue(FrontierVisibility.Minimap));
        nbt.putBoolean("minimapNameVisible", getValue(FrontierVisibility.MinimapName));
        nbt.putBoolean("minimapCollectionVisible", getValue(FrontierVisibility.MinimapCollection));
        nbt.putBoolean("minimapOwnerVisible", getValue(FrontierVisibility.MinimapOwner));
        nbt.putBoolean("minimapBannerVisible", getValue(FrontierVisibility.MinimapBanner));
        nbt.putBoolean("minimapDay", getValue(FrontierVisibility.MinimapDay));
        nbt.putBoolean("minimapNight", getValue(FrontierVisibility.MinimapNight));
        nbt.putBoolean("minimapUnderground", getValue(FrontierVisibility.MinimapUnderground));
        nbt.putBoolean("minimapTopo", getValue(FrontierVisibility.MinimapTopo));
        nbt.putBoolean("minimapBiome", getValue(FrontierVisibility.MinimapBiome));
        nbt.putBoolean("webmapVisible", getValue(FrontierVisibility.Webmap));
        nbt.putBoolean("webmapNameVisible", getValue(FrontierVisibility.WebmapName));
        nbt.putBoolean("webmapCollectionVisible", getValue(FrontierVisibility.WebmapCollection));
        nbt.putBoolean("webmapOwnerVisible", getValue(FrontierVisibility.WebmapOwner));
        nbt.putBoolean("webmapBannerVisible", getValue(FrontierVisibility.WebmapBanner));
        nbt.putBoolean("webmapDay", getValue(FrontierVisibility.WebmapDay));
        nbt.putBoolean("webmapNight", getValue(FrontierVisibility.WebmapNight));
        nbt.putBoolean("webmapUnderground", getValue(FrontierVisibility.WebmapUnderground));
        nbt.putBoolean("webmapTopo", getValue(FrontierVisibility.WebmapTopo));
        nbt.putBoolean("webmapBiome", getValue(FrontierVisibility.WebmapBiome));
    }

    public void fromBytes(FriendlyByteBuf buf) {
        setValue(FrontierVisibility.Frontier, buf.readBoolean());
        setValue(FrontierVisibility.AnnounceInChat, buf.readBoolean());
        setValue(FrontierVisibility.AnnounceInTitle, buf.readBoolean());
        setValue(FrontierVisibility.MentionCollection, buf.readBoolean());
        setValue(FrontierVisibility.Fullscreen, buf.readBoolean());
        setValue(FrontierVisibility.FullscreenName, buf.readBoolean());
        setValue(FrontierVisibility.FullscreenCollection, buf.readBoolean());
        setValue(FrontierVisibility.FullscreenOwner, buf.readBoolean());
        setValue(FrontierVisibility.FullscreenBanner, buf.readBoolean());
        setValue(FrontierVisibility.FullscreenDay, buf.readBoolean());
        setValue(FrontierVisibility.FullscreenNight, buf.readBoolean());
        setValue(FrontierVisibility.FullscreenUnderground, buf.readBoolean());
        setValue(FrontierVisibility.FullscreenTopo, buf.readBoolean());
        setValue(FrontierVisibility.FullscreenBiome, buf.readBoolean());
        setValue(FrontierVisibility.Minimap, buf.readBoolean());
        setValue(FrontierVisibility.MinimapName, buf.readBoolean());
        setValue(FrontierVisibility.MinimapCollection, buf.readBoolean());
        setValue(FrontierVisibility.MinimapOwner, buf.readBoolean());
        setValue(FrontierVisibility.MinimapBanner, buf.readBoolean());
        setValue(FrontierVisibility.MinimapDay, buf.readBoolean());
        setValue(FrontierVisibility.MinimapNight, buf.readBoolean());
        setValue(FrontierVisibility.MinimapUnderground, buf.readBoolean());
        setValue(FrontierVisibility.MinimapTopo, buf.readBoolean());
        setValue(FrontierVisibility.MinimapBiome, buf.readBoolean());
        setValue(FrontierVisibility.Webmap, buf.readBoolean());
        setValue(FrontierVisibility.WebmapName, buf.readBoolean());
        setValue(FrontierVisibility.WebmapCollection, buf.readBoolean());
        setValue(FrontierVisibility.WebmapOwner, buf.readBoolean());
        setValue(FrontierVisibility.WebmapBanner, buf.readBoolean());
        setValue(FrontierVisibility.WebmapDay, buf.readBoolean());
        setValue(FrontierVisibility.WebmapNight, buf.readBoolean());
        setValue(FrontierVisibility.WebmapUnderground, buf.readBoolean());
        setValue(FrontierVisibility.WebmapTopo, buf.readBoolean());
        setValue(FrontierVisibility.WebmapBiome, buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(getValue(FrontierVisibility.Frontier));
        buf.writeBoolean(getValue(FrontierVisibility.AnnounceInChat));
        buf.writeBoolean(getValue(FrontierVisibility.AnnounceInTitle));
        buf.writeBoolean(getValue(FrontierVisibility.MentionCollection));
        buf.writeBoolean(getValue(FrontierVisibility.Fullscreen));
        buf.writeBoolean(getValue(FrontierVisibility.FullscreenName));
        buf.writeBoolean(getValue(FrontierVisibility.FullscreenCollection));
        buf.writeBoolean(getValue(FrontierVisibility.FullscreenOwner));
        buf.writeBoolean(getValue(FrontierVisibility.FullscreenBanner));
        buf.writeBoolean(getValue(FrontierVisibility.FullscreenDay));
        buf.writeBoolean(getValue(FrontierVisibility.FullscreenNight));
        buf.writeBoolean(getValue(FrontierVisibility.FullscreenUnderground));
        buf.writeBoolean(getValue(FrontierVisibility.FullscreenTopo));
        buf.writeBoolean(getValue(FrontierVisibility.FullscreenBiome));
        buf.writeBoolean(getValue(FrontierVisibility.Minimap));
        buf.writeBoolean(getValue(FrontierVisibility.MinimapName));
        buf.writeBoolean(getValue(FrontierVisibility.MinimapCollection));
        buf.writeBoolean(getValue(FrontierVisibility.MinimapOwner));
        buf.writeBoolean(getValue(FrontierVisibility.MinimapBanner));
        buf.writeBoolean(getValue(FrontierVisibility.MinimapDay));
        buf.writeBoolean(getValue(FrontierVisibility.MinimapNight));
        buf.writeBoolean(getValue(FrontierVisibility.MinimapUnderground));
        buf.writeBoolean(getValue(FrontierVisibility.MinimapTopo));
        buf.writeBoolean(getValue(FrontierVisibility.MinimapBiome));
        buf.writeBoolean(getValue(FrontierVisibility.Webmap));
        buf.writeBoolean(getValue(FrontierVisibility.WebmapName));
        buf.writeBoolean(getValue(FrontierVisibility.WebmapCollection));
        buf.writeBoolean(getValue(FrontierVisibility.WebmapOwner));
        buf.writeBoolean(getValue(FrontierVisibility.WebmapBanner));
        buf.writeBoolean(getValue(FrontierVisibility.WebmapDay));
        buf.writeBoolean(getValue(FrontierVisibility.WebmapNight));
        buf.writeBoolean(getValue(FrontierVisibility.WebmapUnderground));
        buf.writeBoolean(getValue(FrontierVisibility.WebmapTopo));
        buf.writeBoolean(getValue(FrontierVisibility.WebmapBiome));
    }
}
