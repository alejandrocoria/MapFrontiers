package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumSet;

public class VisibilityData {
    private final EnumSet<FrontierVisibility> values;

    public VisibilityData() {
        values = EnumSet.noneOf(FrontierVisibility.class);
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (visibility.getDefaultValue()) {
                values.add(visibility);
            }
        }
    }

    public VisibilityData(boolean setAll) {
        if (setAll) {
            values = EnumSet.allOf(FrontierVisibility.class);
        } else {
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
        set(value, set);
    }

    public boolean getValue(FrontierVisibility value) {
        return get(value);
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

    public void setFrontier(boolean enabled) {
        set(FrontierVisibility.Frontier, enabled);
    }

    public boolean getAnnounceInChat() {
        return get(FrontierVisibility.AnnounceInChat);
    }

    public void setAnnounceInChat(boolean enabled) {
        set(FrontierVisibility.AnnounceInChat, enabled);
    }

    public boolean getAnnounceInTitle() {
        return get(FrontierVisibility.AnnounceInTitle);
    }

    public void setAnnounceInTitle(boolean enabled) {
        set(FrontierVisibility.AnnounceInTitle, enabled);
    }

    public boolean getMentionCollection() {
        return get(FrontierVisibility.MentionCollection);
    }

    public void setMentionCollection(boolean enabled) {
        set(FrontierVisibility.MentionCollection, enabled);
    }

    public boolean getFullscreen() {
        return get(FrontierVisibility.Fullscreen);
    }

    public void setFullscreen(boolean enabled) {
        set(FrontierVisibility.Fullscreen, enabled);
    }

    public boolean getFullscreenName() {
        return get(FrontierVisibility.FullscreenName);
    }

    public void setFullscreenName(boolean enabled) {
        set(FrontierVisibility.FullscreenName, enabled);
    }

    public boolean getFullscreenCollection() {
        return get(FrontierVisibility.FullscreenCollection);
    }

    public void setFullscreenCollection(boolean enabled) {
        set(FrontierVisibility.FullscreenCollection, enabled);
    }

    public boolean getFullscreenOwner() {
        return get(FrontierVisibility.FullscreenOwner);
    }

    public void setFullscreenOwner(boolean enabled) {
        set(FrontierVisibility.FullscreenOwner, enabled);
    }

    public boolean getFullscreenBanner() {
        return get(FrontierVisibility.FullscreenBanner);
    }

    public void setFullscreenBanner(boolean enabled) {
        set(FrontierVisibility.FullscreenBanner, enabled);
    }

    public boolean getFullscreenDay() {
        return get(FrontierVisibility.FullscreenDay);
    }

    public void setFullscreenDay(boolean enabled) {
        set(FrontierVisibility.FullscreenDay, enabled);
    }

    public boolean getFullscreenNight() {
        return get(FrontierVisibility.FullscreenNight);
    }

    public void setFullscreenNight(boolean enabled) {
        set(FrontierVisibility.FullscreenNight, enabled);
    }

    public boolean getFullscreenUnderground() {
        return get(FrontierVisibility.FullscreenUnderground);
    }

    public void setFullscreenUnderground(boolean enabled) {
        set(FrontierVisibility.FullscreenUnderground, enabled);
    }

    public boolean getFullscreenTopo() {
        return get(FrontierVisibility.FullscreenTopo);
    }

    public void setFullscreenTopo(boolean enabled) {
        set(FrontierVisibility.FullscreenTopo, enabled);
    }

    public boolean getFullscreenBiome() {
        return get(FrontierVisibility.FullscreenBiome);
    }

    public void setFullscreenBiome(boolean enabled) {
        set(FrontierVisibility.FullscreenBiome, enabled);
    }

    public boolean getMinimap() {
        return get(FrontierVisibility.Minimap);
    }

    public void setMinimap(boolean enabled) {
        set(FrontierVisibility.Minimap, enabled);
    }

    public boolean getMinimapName() {
        return get(FrontierVisibility.MinimapName);
    }

    public void setMinimapName(boolean enabled) {
        set(FrontierVisibility.MinimapName, enabled);
    }

    public boolean getMinimapCollection() {
        return get(FrontierVisibility.MinimapCollection);
    }

    public void setMinimapCollection(boolean enabled) {
        set(FrontierVisibility.MinimapCollection, enabled);
    }

    public boolean getMinimapOwner() {
        return get(FrontierVisibility.MinimapOwner);
    }

    public void setMinimapOwner(boolean enabled) {
        set(FrontierVisibility.MinimapOwner, enabled);
    }

    public boolean getMinimapBanner() {
        return get(FrontierVisibility.MinimapBanner);
    }

    public void setMinimapBanner(boolean enabled) {
        set(FrontierVisibility.MinimapBanner, enabled);
    }

    public boolean getMinimapDay() {
        return get(FrontierVisibility.MinimapDay);
    }

    public void setMinimapDay(boolean enabled) {
        set(FrontierVisibility.MinimapDay, enabled);
    }

    public boolean getMinimapNight() {
        return get(FrontierVisibility.MinimapNight);
    }

    public void setMinimapNight(boolean enabled) {
        set(FrontierVisibility.MinimapNight, enabled);
    }

    public boolean getMinimapUnderground() {
        return get(FrontierVisibility.MinimapUnderground);
    }

    public void setMinimapUnderground(boolean enabled) {
        set(FrontierVisibility.MinimapUnderground, enabled);
    }

    public boolean getMinimapTopo() {
        return get(FrontierVisibility.MinimapTopo);
    }

    public void setMinimapTopo(boolean enabled) {
        set(FrontierVisibility.MinimapTopo, enabled);
    }

    public boolean getMinimapBiome() {
        return get(FrontierVisibility.MinimapBiome);
    }

    public void setMinimapBiome(boolean enabled) {
        set(FrontierVisibility.MinimapBiome, enabled);
    }

    public boolean getWebmap() {
        return get(FrontierVisibility.Webmap);
    }

    public void setWebmap(boolean enabled) {
        set(FrontierVisibility.Webmap, enabled);
    }

    public boolean getWebmapName() {
        return get(FrontierVisibility.WebmapName);
    }

    public void setWebmapName(boolean enabled) {
        set(FrontierVisibility.WebmapName, enabled);
    }

    public boolean getWebmapCollection() {
        return get(FrontierVisibility.WebmapCollection);
    }

    public void setWebmapCollection(boolean enabled) {
        set(FrontierVisibility.WebmapCollection, enabled);
    }

    public boolean getWebmapOwner() {
        return get(FrontierVisibility.WebmapOwner);
    }

    public void setWebmapOwner(boolean enabled) {
        set(FrontierVisibility.WebmapOwner, enabled);
    }

    public boolean getWebmapBanner() {
        return get(FrontierVisibility.WebmapBanner);
    }

    public void setWebmapBanner(boolean enabled) {
        set(FrontierVisibility.WebmapBanner, enabled);
    }

    public boolean getWebmapDay() {
        return get(FrontierVisibility.WebmapDay);
    }

    public void setWebmapDay(boolean enabled) {
        set(FrontierVisibility.WebmapDay, enabled);
    }

    public boolean getWebmapNight() {
        return get(FrontierVisibility.WebmapNight);
    }

    public void setWebmapNight(boolean enabled) {
        set(FrontierVisibility.WebmapNight, enabled);
    }

    public boolean getWebmapUnderground() {
        return get(FrontierVisibility.WebmapUnderground);
    }

    public void setWebmapUnderground(boolean enabled) {
        set(FrontierVisibility.WebmapUnderground, enabled);
    }

    public boolean getWebmapTopo() {
        return get(FrontierVisibility.WebmapTopo);
    }

    public void setWebmapTopo(boolean enabled) {
        set(FrontierVisibility.WebmapTopo, enabled);
    }

    public boolean getWebmapBiome() {
        return get(FrontierVisibility.WebmapBiome);
    }

    public void setWebmapBiome(boolean enabled) {
        set(FrontierVisibility.WebmapBiome, enabled);
    }

    public void applyOverride(VisibilityData override, FrontierVisibilityMask mask) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (mask.has(visibility)) {
                set(visibility, override.get(visibility));
            }
        }
    }

    public boolean equalsMasked(VisibilityData other, FrontierVisibilityMask mask) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (mask.has(visibility) && get(visibility) != other.get(visibility)) {
                return false;
            }
        }

        return true;
    }

    public void readSparseNbt(CompoundTag nbt, FrontierVisibilityMask mask) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (!nbt.contains(visibility.getNbtKey())) {
                continue;
            }

            set(visibility, nbt.getBooleanOr(visibility.getNbtKey(), false));
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
        setFrontier(NbtReadHelper.requireBoolean(nbt, "visible"));
        setFullscreen(NbtReadHelper.requireBoolean(nbt, "visible"));
        setFullscreenName(NbtReadHelper.requireBoolean(nbt, "visible"));
        setFullscreenCollection(false);
        setFullscreenOwner(NbtReadHelper.requireBoolean(nbt, "nameVisible"));
        setFullscreenBanner(false);
        setFullscreenDay(NbtReadHelper.requireBoolean(nbt, "visible"));
        setFullscreenNight(NbtReadHelper.requireBoolean(nbt, "visible"));
        setFullscreenUnderground(NbtReadHelper.requireBoolean(nbt, "visible"));
        setFullscreenTopo(NbtReadHelper.requireBoolean(nbt, "visible"));
        setFullscreenBiome(NbtReadHelper.requireBoolean(nbt, "visible"));
        setMinimap(NbtReadHelper.requireBoolean(nbt, "visible"));
        setMinimapName(NbtReadHelper.requireBoolean(nbt, "nameVisible"));
        setMinimapCollection(false);
        setMinimapOwner(NbtReadHelper.requireBoolean(nbt, "ownerVisible"));
        setMinimapBanner(false);
        setMinimapDay(NbtReadHelper.requireBoolean(nbt, "visible"));
        setMinimapNight(NbtReadHelper.requireBoolean(nbt, "visible"));
        setMinimapUnderground(NbtReadHelper.requireBoolean(nbt, "visible"));
        setMinimapTopo(NbtReadHelper.requireBoolean(nbt, "visible"));
        setMinimapBiome(NbtReadHelper.requireBoolean(nbt, "visible"));
        setWebmap(NbtReadHelper.requireBoolean(nbt, "visible"));
        setWebmapName(NbtReadHelper.requireBoolean(nbt, "nameVisible"));
        setWebmapCollection(false);
        setWebmapOwner(NbtReadHelper.requireBoolean(nbt, "ownerVisible"));
        setWebmapBanner(false);
        setWebmapDay(NbtReadHelper.requireBoolean(nbt, "visible"));
        setWebmapNight(NbtReadHelper.requireBoolean(nbt, "visible"));
        setWebmapUnderground(NbtReadHelper.requireBoolean(nbt, "visible"));
        setWebmapTopo(NbtReadHelper.requireBoolean(nbt, "visible"));
        setWebmapBiome(NbtReadHelper.requireBoolean(nbt, "visible"));
        setAnnounceInChat(NbtReadHelper.getBooleanOrDefault(nbt, FrontierVisibility.AnnounceInChat.getNbtKey(), false));
        setAnnounceInTitle(NbtReadHelper.getBooleanOrDefault(nbt, FrontierVisibility.AnnounceInTitle.getNbtKey(), false));
        setMentionCollection(true);
    }

    public void readFromNBT(CompoundTag nbt) {
        setFrontier(NbtReadHelper.requireBoolean(nbt, FrontierVisibility.Frontier.getNbtKey()));
        setFullscreen(readNbtValue(nbt, FrontierVisibility.Fullscreen));
        setFullscreenName(readNbtValue(nbt, FrontierVisibility.FullscreenName));
        setFullscreenCollection(readNbtValue(nbt, FrontierVisibility.FullscreenCollection));
        setFullscreenOwner(readNbtValue(nbt, FrontierVisibility.FullscreenOwner));
        setFullscreenBanner(readNbtValue(nbt, FrontierVisibility.FullscreenBanner));
        setFullscreenDay(readNbtValue(nbt, FrontierVisibility.FullscreenDay));
        setFullscreenNight(readNbtValue(nbt, FrontierVisibility.FullscreenNight));
        setFullscreenUnderground(readNbtValue(nbt, FrontierVisibility.FullscreenUnderground));
        setFullscreenTopo(readNbtValue(nbt, FrontierVisibility.FullscreenTopo));
        setFullscreenBiome(readNbtValue(nbt, FrontierVisibility.FullscreenBiome));
        setMinimap(readNbtValue(nbt, FrontierVisibility.Minimap));
        setMinimapName(readNbtValue(nbt, FrontierVisibility.MinimapName));
        setMinimapCollection(readNbtValue(nbt, FrontierVisibility.MinimapCollection));
        setMinimapOwner(readNbtValue(nbt, FrontierVisibility.MinimapOwner));
        setMinimapBanner(readNbtValue(nbt, FrontierVisibility.MinimapBanner));
        setMinimapDay(readNbtValue(nbt, FrontierVisibility.MinimapDay));
        setMinimapNight(readNbtValue(nbt, FrontierVisibility.MinimapNight));
        setMinimapUnderground(readNbtValue(nbt, FrontierVisibility.MinimapUnderground));
        setMinimapTopo(readNbtValue(nbt, FrontierVisibility.MinimapTopo));
        setMinimapBiome(readNbtValue(nbt, FrontierVisibility.MinimapBiome));
        setWebmap(readNbtValue(nbt, FrontierVisibility.Webmap, getMinimap()));
        setWebmapName(readNbtValue(nbt, FrontierVisibility.WebmapName, getMinimapName()));
        setWebmapCollection(readNbtValue(nbt, FrontierVisibility.WebmapCollection, getMinimapCollection()));
        setWebmapOwner(readNbtValue(nbt, FrontierVisibility.WebmapOwner, getMinimapOwner()));
        setWebmapBanner(readNbtValue(nbt, FrontierVisibility.WebmapBanner, getMinimapBanner()));
        setWebmapDay(readNbtValue(nbt, FrontierVisibility.WebmapDay, getMinimapDay()));
        setWebmapNight(readNbtValue(nbt, FrontierVisibility.WebmapNight, getMinimapNight()));
        setWebmapUnderground(readNbtValue(nbt, FrontierVisibility.WebmapUnderground, getMinimapUnderground()));
        setWebmapTopo(readNbtValue(nbt, FrontierVisibility.WebmapTopo, getMinimapTopo()));
        setWebmapBiome(readNbtValue(nbt, FrontierVisibility.WebmapBiome, getMinimapBiome()));
        setAnnounceInChat(readNbtValue(nbt, FrontierVisibility.AnnounceInChat));
        setAnnounceInTitle(readNbtValue(nbt, FrontierVisibility.AnnounceInTitle));
        setMentionCollection(readNbtValue(nbt, FrontierVisibility.MentionCollection));
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
