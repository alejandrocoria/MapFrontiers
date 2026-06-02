package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.territory.BannerDataHelper;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;

import javax.annotation.Nullable;
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
        BannerPatternLayers patterns = createPreviewPatterns();
        if (patterns != null) {
            frontierData.setBannerData(BannerDataHelper.fromBanner(DyeColor.BLACK, patterns));
        }
    }

    public static void setPreviewBanner(CollectionData collectionData) {
        BannerPatternLayers patterns = createPreviewPatterns();
        if (patterns != null) {
            collectionData.setBannerData(BannerDataHelper.fromBanner(DyeColor.BLACK, patterns));
        }
    }

    private static @Nullable BannerPatternLayers createPreviewPatterns() {
        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return null;
            }

            HolderLookup<BannerPattern> patternRegistry = level.registryAccess().lookup(Registries.BANNER_PATTERN).orElseThrow();
            return (new BannerPatternLayers.Builder())
                    .add(patternRegistry.get(BannerPatterns.FLOWER).orElseThrow(), DyeColor.GREEN)
                    .add(patternRegistry.get(BannerPatterns.BRICKS).orElseThrow(), DyeColor.LIGHT_GRAY)
                    .add(patternRegistry.get(BannerPatterns.BORDER).orElseThrow(), DyeColor.LIGHT_BLUE)
                    .add(patternRegistry.get(BannerPatterns.TRIANGLE_TOP).orElseThrow(), DyeColor.LIGHT_BLUE)
                    .add(patternRegistry.get(BannerPatterns.TRIANGLE_BOTTOM).orElseThrow(), DyeColor.BLACK)
                    .add(patternRegistry.get(BannerPatterns.STRIPE_BOTTOM).orElseThrow(), DyeColor.GREEN)
                    .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private PreviewFrontierHelper() {
    }
}
