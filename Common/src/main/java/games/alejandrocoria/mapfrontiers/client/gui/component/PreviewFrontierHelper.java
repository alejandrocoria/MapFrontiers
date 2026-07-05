package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.territory.BannerDataHelper;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class PreviewFrontierHelper {
    public static SettingsUser createPreviewOwner() {
        SettingsUser owner = new SettingsUser();
        owner.username = translate("mapfrontiers.preview_owner");
        return owner;
    }

    public static String translate(String key) {
        return Component.translatable(key).getString();
    }

    public static void setPreviewBanner(FrontierData frontierData) {
        frontierData.setBannerData(BannerDataHelper.fromBanner(DyeColor.BLACK, createPreviewPatterns()));
    }

    public static void setPreviewBanner(CollectionData collectionData) {
        collectionData.setBannerData(BannerDataHelper.fromBanner(DyeColor.BLACK, createPreviewPatterns()));
    }

    private static ListTag createPreviewPatterns() {
        ListTag patterns = new ListTag();
        addPattern(patterns, "flo", DyeColor.GREEN);
        addPattern(patterns, "bri", DyeColor.LIGHT_GRAY);
        addPattern(patterns, "bo", DyeColor.LIGHT_BLUE);
        addPattern(patterns, "tt", DyeColor.LIGHT_BLUE);
        addPattern(patterns, "bt", DyeColor.BLACK);
        addPattern(patterns, "bs", DyeColor.GREEN);
        return patterns;
    }

    private static void addPattern(ListTag patterns, String pattern, DyeColor color) {
        CompoundTag patternTag = new CompoundTag();
        patternTag.putString("Pattern", pattern);
        patternTag.putInt("Color", color.getId());
        patterns.add(patternTag);
    }

    private PreviewFrontierHelper() {
    }
}
