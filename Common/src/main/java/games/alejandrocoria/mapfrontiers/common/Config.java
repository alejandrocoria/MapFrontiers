package games.alejandrocoria.mapfrontiers.common;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.base.Splitter;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config {
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;
    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
        ClientEventHandler.subscribeUpdatedConfigEvent(Config.class, Config::save);
    }

    public enum AfterCreatingFrontier {
        Info, Edit, Nothing
    }

    public enum Visibility {
        Custom, Always, Never
    }

    public enum FilterFrontierType {
        All, Global, Personal
    }

    public enum FilterFrontierOwner {
        All, You, Others
    }

    public enum HUDAnchor {
        ScreenTop, ScreenTopRight, ScreenRight, ScreenBottomRight, ScreenBottom, ScreenBottomLeft, ScreenLeft, ScreenTopLeft,
        Minimap, MinimapHorizontal, MinimapVertical
    }

    public enum HUDSlot {
        None, Name, Owner, Banner
    }

    public static int newFrontierShape;
    public static int newFrontierShapeWidth;
    public static int newFrontierShapeRadius;
    public static int newFrontierChunkShape;
    public static int newFrontierChunkShapeWidth;
    public static int newFrontierChunkShapeLength;
    public static FrontierData.Mode newFrontierMode;
    public static AfterCreatingFrontier afterCreatingFrontier;
    public static boolean pasteName;
    public static boolean pasteVisibility;
    public static boolean pasteColor;
    public static boolean pasteBanner;
    public static boolean pasteOptionsVisible;
    public static boolean fullscreenButtons;
    public static Visibility fullscreenVisibility;
    public static Visibility fullscreenNameVisibility;
    public static Visibility fullscreenOwnerVisibility;
    public static Visibility minimapVisibility;
    public static Visibility minimapNameVisibility;
    public static Visibility minimapOwnerVisibility;
    public static boolean titleAnnouncementAboveHotbar;
    public static boolean announceUnnamedFrontiers;
    public static boolean hideNamesThatDontFit;
    public static double polygonsOpacity;
    public static int snapDistance;
    public static FilterFrontierType filterFrontierType;
    public static FilterFrontierOwner filterFrontierOwner;
    public static String filterFrontierDimension;
    public static boolean hudEnabled;
    public static boolean hudAutoAdjustAnchor;
    public static boolean hudSnapToBorder;
    public static int hudBannerSize;
    public static HUDSlot hudSlot1;
    public static HUDSlot hudSlot2;
    public static HUDSlot hudSlot3;
    public static HUDAnchor hudAnchor;
    public static int hudXPosition;
    public static int hudYPosition;

    public static void bakeConfig() {
        newFrontierShape = CLIENT.newFrontierShape.get();
        newFrontierShapeWidth = CLIENT.newFrontierShapeWidth.get();
        newFrontierShapeRadius = CLIENT.newFrontierShapeRadius.get();
        newFrontierChunkShape = CLIENT.newFrontierChunkShape.get();
        newFrontierChunkShapeWidth = CLIENT.newFrontierChunkShapeWidth.get();
        newFrontierChunkShapeLength = CLIENT.newFrontierChunkShapeLength.get();
        newFrontierMode = CLIENT.newFrontierMode.get();
        afterCreatingFrontier = CLIENT.afterCreatingFrontier.get();
        pasteName = CLIENT.pasteName.get();
        pasteVisibility = CLIENT.pasteVisibility.get();
        pasteColor = CLIENT.pasteColor.get();
        pasteBanner = CLIENT.pasteBanner.get();
        pasteOptionsVisible = CLIENT.pasteOptionsVisible.get();
        fullscreenButtons = CLIENT.fullscreenButtons.get();
        fullscreenVisibility = CLIENT.fullscreenVisibility.get();
        fullscreenNameVisibility = CLIENT.fullscreenNameVisibility.get();
        fullscreenOwnerVisibility = CLIENT.fullscreenOwnerVisibility.get();
        minimapVisibility = CLIENT.minimapVisibility.get();
        minimapNameVisibility = CLIENT.minimapNameVisibility.get();
        minimapOwnerVisibility = CLIENT.minimapOwnerVisibility.get();
        titleAnnouncementAboveHotbar = CLIENT.titleAnnouncementAboveHotbar.get();
        announceUnnamedFrontiers = CLIENT.announceUnnamedFrontiers.get();
        hideNamesThatDontFit = CLIENT.hideNamesThatDontFit.get();
        polygonsOpacity = CLIENT.polygonsOpacity.get();
        snapDistance = CLIENT.snapDistance.get();
        filterFrontierType = CLIENT.filterFrontierType.get();
        filterFrontierOwner = CLIENT.filterFrontierOwner.get();
        filterFrontierDimension = CLIENT.filterFrontierDimension.get();
        hudEnabled = CLIENT.hudEnabled.get();
        hudAutoAdjustAnchor = CLIENT.hudAutoAdjustAnchor.get();
        hudSnapToBorder = CLIENT.hudSnapToBorder.get();
        hudBannerSize = CLIENT.hudBannerSize.get();
        hudSlot1 = CLIENT.hudSlot1.get();
        hudSlot2 = CLIENT.hudSlot2.get();
        hudSlot3 = CLIENT.hudSlot3.get();
        hudAnchor = CLIENT.hudAnchor.get();
        hudXPosition = CLIENT.hudXPosition.get();
        hudYPosition = CLIENT.hudYPosition.get();
    }

    public static class ClientConfig {
        public final IntValue newFrontierShape;
        public final IntValue newFrontierShapeWidth;
        public final IntValue newFrontierShapeRadius;
        public final IntValue newFrontierChunkShape;
        public final IntValue newFrontierChunkShapeWidth;
        public final IntValue newFrontierChunkShapeLength;
        public final EnumValue<FrontierData.Mode> newFrontierMode;
        public final EnumValue<AfterCreatingFrontier> afterCreatingFrontier;
        public final BooleanValue pasteName;
        public final BooleanValue pasteVisibility;
        public final BooleanValue pasteColor;
        public final BooleanValue pasteBanner;
        public final BooleanValue pasteOptionsVisible;
        public final BooleanValue fullscreenButtons;
        public final EnumValue<Visibility> fullscreenVisibility;
        public final EnumValue<Visibility> fullscreenNameVisibility;
        public final EnumValue<Visibility> fullscreenOwnerVisibility;
        public final EnumValue<Visibility> minimapVisibility;
        public final EnumValue<Visibility> minimapNameVisibility;
        public final EnumValue<Visibility> minimapOwnerVisibility;
        public final BooleanValue titleAnnouncementAboveHotbar;
        public final BooleanValue announceUnnamedFrontiers;
        public final BooleanValue hideNamesThatDontFit;
        public final DoubleValue polygonsOpacity;
        public final IntValue snapDistance;
        public final EnumValue<FilterFrontierType> filterFrontierType;
        public final EnumValue<FilterFrontierOwner> filterFrontierOwner;
        public final ConfigValue<String> filterFrontierDimension;
        public final BooleanValue hudEnabled;
        public final BooleanValue hudAutoAdjustAnchor;
        public final BooleanValue hudSnapToBorder;
        public final IntValue hudBannerSize;
        public final EnumValue<HUDSlot> hudSlot1;
        public final EnumValue<HUDSlot> hudSlot2;
        public final EnumValue<HUDSlot> hudSlot3;
        public final EnumValue<HUDAnchor> hudAnchor;
        public final IntValue hudXPosition;
        public final IntValue hudYPosition;

        public ClientConfig(Builder builder) {
            newFrontierShape = builder.defineInRange("newFrontierShape", 0, 0, 11);
            newFrontierShapeWidth = builder.defineInRange("newFrontierShapeWidth", 10, 0, 999);
            newFrontierShapeRadius = builder.defineInRange("newFrontierShapeRadius", 20, 0, 999);
            newFrontierChunkShape = builder.defineInRange("newFrontierChunkShape", 0, 0, 7);
            newFrontierChunkShapeWidth = builder.defineInRange("newFrontierChunkShapeWidth", 5, 0, 32);
            newFrontierChunkShapeLength = builder.defineInRange("newFrontierChunkShapeLength", 5, 0, 32);
            newFrontierMode = builder.defineEnum("newFrontierMode", FrontierData.Mode.Vertex);
            afterCreatingFrontier = builder.defineEnum("afterCreatingFrontier", AfterCreatingFrontier.Info);
            pasteName = builder.define("pasteName", false);
            pasteVisibility = builder.define("pasteVisibility", true);
            pasteColor = builder.define("pasteColor", true);
            pasteBanner = builder.define("pasteBanner", true);
            pasteOptionsVisible = builder.define("pasteOptionsVisible", false);
            fullscreenButtons = builder.comment("Show buttons on fullscreen map.")
                    .translation(MapFrontiers.MODID + ".config." + "fullscreenButtons")
                    .define("fullscreenButtons", true);
            fullscreenVisibility = builder.comment(
                    "Force all frontier to be shown or hidden on the fullscreen map. In Manual you can decide for each frontier.")
                    .translation(MapFrontiers.MODID + ".config." + "fullscreenVisibility")
                    .defineEnum("fullscreenVisibility", Visibility.Custom);
            fullscreenNameVisibility = builder.comment(
                    "Force all frontier names to be shown or hidden on the fullscreen map. In Manual you can decide for each frontier.")
                    .translation(MapFrontiers.MODID + ".config." + "fullscreenNameVisibility")
                    .defineEnum("fullscreenNameVisibility", Visibility.Custom);
            fullscreenOwnerVisibility = builder.comment(
                    "Force all frontier owners to be shown or hidden on the fullscreen map. In Manual you can decide for each frontier.")
                    .translation(MapFrontiers.MODID + ".config." + "fullscreenOwnerVisibility")
                    .defineEnum("fullscreenOwnerVisibility", Visibility.Custom);
            minimapVisibility = builder.comment(
                    "Force all frontier to be shown or hidden on the minimap. In Manual you can decide for each frontier.")
                    .translation(MapFrontiers.MODID + ".config." + "minimapVisibility")
                    .defineEnum("minimapVisibility", Visibility.Custom);
            minimapNameVisibility = builder.comment(
                    "Force all frontier names to be shown or hidden on the minimap. In Manual you can decide for each frontier.")
                    .translation(MapFrontiers.MODID + ".config." + "minimapNameVisibility")
                    .defineEnum("minimapNameVisibility", Visibility.Custom);
            minimapOwnerVisibility = builder.comment(
                    "Force all frontier owners to be shown or hidden on the minimap. In Manual you can decide for each frontier.")
                    .translation(MapFrontiers.MODID + ".config." + "minimapOwnerVisibility")
                    .defineEnum("minimapOwnerVisibility", Visibility.Custom);
            titleAnnouncementAboveHotbar = builder.comment(
                    "Show the frontier announcement above the hotbar instead of showing it as a title.")
                    .translation(MapFrontiers.MODID + ".config." + "titleAnnouncementAboveHotbar")
                    .define("titleAnnouncementAboveHotbar", false);
            announceUnnamedFrontiers = builder.comment(
                    "Announce unnamed frontiers in chat/title.")
                    .translation(MapFrontiers.MODID + ".config." + "announceUnnamedFrontiers")
                    .define("announceUnnamedFrontiers", false);
            hideNamesThatDontFit = builder.comment(
                    "Hides the name if it is wider than the frontier at the zoom level it is being viewed.")
                    .translation(MapFrontiers.MODID + ".config." + "hideNamesThatDontFit")
                    .define("hideNamesThatDontFit", true);
            polygonsOpacity = builder
                    .comment("Transparency of the frontier polygons. 0.0 is fully transparent and 1.0 is opaque.")
                    .translation(MapFrontiers.MODID + ".config." + "polygonsOpacity")
                    .defineInRange("polygonsOpacity", 0.4, 0.0, 1.0);
            snapDistance = builder.comment("Distance at which vertices are attached to nearby vertices.")
                    .translation(MapFrontiers.MODID + ".config." + "snapDistance").defineInRange("snapDistance", 8, 0, 16);
            filterFrontierType = builder.defineEnum("filterFrontierType", FilterFrontierType.All);
            filterFrontierOwner = builder.defineEnum("filterFrontierOwner", FilterFrontierOwner.All);
            filterFrontierDimension = builder.define("filterFrontierDimension", "all");

            builder.push("hud");
            hudEnabled = builder.comment("Show the HUD on screen.").translation(MapFrontiers.MODID + ".config.hud." + "enabled")
                    .define("enabled", true);
            hudAutoAdjustAnchor = builder
                    .comment("Automatically switch to nearest anchor when HUD position is edited (on settings screen).")
                    .translation(MapFrontiers.MODID + ".config.hud." + "autoAdjustAnchor").define("autoAdjustAnchor", true);
            hudSnapToBorder = builder
                    .comment("Automatically snap to closest border when HUD position is edited (on settings screen).")
                    .translation(MapFrontiers.MODID + ".config.hud." + "snapToBorder").define("snapToBorder", true);
            hudBannerSize = builder.comment("Size of the HUD banner.")
                    .translation(MapFrontiers.MODID + ".config.hud." + "bannerSize").defineInRange("bannerSize", 3, 1, 8);
            hudSlot1 = builder.comment("HUD element on slot 1.").translation(MapFrontiers.MODID + ".config.hud." + "slot1")
                    .defineEnum("slot1", HUDSlot.Name);
            hudSlot2 = builder.comment("HUD element on slot 2.").translation(MapFrontiers.MODID + ".config.hud." + "slot2")
                    .defineEnum("slot2", HUDSlot.Owner);
            hudSlot3 = builder.comment("HUD element on slot 3.").translation(MapFrontiers.MODID + ".config.hud." + "slot3")
                    .defineEnum("slot3", HUDSlot.Banner);
            hudAnchor = builder.comment(
                    "Anchor point of the HUD. In the case of choosing the minimap as an anchor, its default position will be used as a reference in the coordinates.")
                    .translation(MapFrontiers.MODID + ".config.hud." + "anchor")
                    .defineEnum("anchor", HUDAnchor.MinimapHorizontal);
            hudXPosition = builder.defineInRange("xPosition", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            hudYPosition = builder.defineInRange("yPosition", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            builder.pop();
        }
    }

    private static void save() {
        CLIENT.newFrontierShape.set(newFrontierShape);
        CLIENT.newFrontierShapeWidth.set(newFrontierShapeWidth);
        CLIENT.newFrontierShapeRadius.set(newFrontierShapeRadius);
        CLIENT.newFrontierChunkShape.set(newFrontierChunkShape);
        CLIENT.newFrontierChunkShapeWidth.set(newFrontierChunkShapeWidth);
        CLIENT.newFrontierChunkShapeLength.set(newFrontierChunkShapeLength);
        CLIENT.newFrontierMode.set(newFrontierMode);
        CLIENT.afterCreatingFrontier.set(afterCreatingFrontier);
        CLIENT.pasteName.set(pasteName);
        CLIENT.pasteVisibility.set(pasteVisibility);
        CLIENT.pasteColor.set(pasteColor);
        CLIENT.pasteBanner.set(pasteBanner);
        CLIENT.pasteOptionsVisible.set(pasteOptionsVisible);
        CLIENT.fullscreenButtons.set(fullscreenButtons);
        CLIENT.fullscreenVisibility.set(fullscreenVisibility);
        CLIENT.fullscreenNameVisibility.set(fullscreenNameVisibility);
        CLIENT.fullscreenOwnerVisibility.set(fullscreenOwnerVisibility);
        CLIENT.minimapVisibility.set(minimapVisibility);
        CLIENT.minimapNameVisibility.set(minimapNameVisibility);
        CLIENT.minimapOwnerVisibility.set(minimapOwnerVisibility);
        CLIENT.titleAnnouncementAboveHotbar.set(titleAnnouncementAboveHotbar);
        CLIENT.announceUnnamedFrontiers.set(announceUnnamedFrontiers);
        CLIENT.hideNamesThatDontFit.set(hideNamesThatDontFit);
        CLIENT.polygonsOpacity.set(polygonsOpacity);
        CLIENT.snapDistance.set(snapDistance);
        CLIENT.filterFrontierType.set(filterFrontierType);
        CLIENT.filterFrontierOwner.set(filterFrontierOwner);
        CLIENT.filterFrontierDimension.set(filterFrontierDimension);
        CLIENT.hudEnabled.set(hudEnabled);
        CLIENT.hudAutoAdjustAnchor.set(hudAutoAdjustAnchor);
        CLIENT.hudSnapToBorder.set(hudSnapToBorder);
        CLIENT.hudBannerSize.set(hudBannerSize);
        CLIENT.hudSlot1.set(hudSlot1);
        CLIENT.hudSlot2.set(hudSlot2);
        CLIENT.hudSlot3.set(hudSlot3);
        CLIENT.hudAnchor.set(hudAnchor);
        CLIENT.hudXPosition.set(hudXPosition);
        CLIENT.hudYPosition.set(hudYPosition);

        CLIENT_SPEC.save();
    }

    public static boolean getVisibilityValue(Visibility visibility, boolean manual) {
        return switch (visibility) {
            case Always -> true;
            case Never -> false;
            default -> manual;
        };
    }

    public static Component getTranslatedName(String name) {
        ValueSpec valueSpec = getValueSpec(name);
        if (valueSpec != null) {
            return Component.translatable(valueSpec.getTranslationKey());
        }

        return CommonComponents.EMPTY;
    }

    public static <E extends Enum<E>> Component getTranslatedEnum(E value) {
        return Component.translatable("mapfrontiers.config." + value.name());
    }

    public static List<Component> getTooltip(String name) {
        List<Component> tooltip = new ArrayList<>();

        ValueSpec valueSpec = getValueSpec(name);
        if (valueSpec != null) {
            String lines = Component.translatable(valueSpec.getTranslationKey() + ".tooltip").getString();
            for (String string : Splitter.on("\n").split(lines)) {
                tooltip.add(Component.literal(string));
            }
        }

        return tooltip;
    }

    public static String getDefault(String name) {
        ValueSpec valueSpec = getValueSpec(name);
        if (valueSpec != null) {
            return valueSpec.getDefault().toString();
        }

        return "";
    }

    public static boolean isInRange(String name, Object value) {
        ValueSpec valueSpec = getValueSpec(name);
        if (valueSpec != null) {
            return valueSpec.test(value);
        }

        return false;
    }

    private static ValueSpec getValueSpec(String name) {
        return getValueSpec(Arrays.asList(name.split("\\.")), CLIENT_SPEC.getSpec());
    }

    private static ValueSpec getValueSpec(List<String> path, UnmodifiableConfig valueMap) {
        if (path.isEmpty()) {
            return null;
        }

        Object value = valueMap.valueMap().get(path.get(0));
        if (value == null) {
            return null;
        }

        if (value instanceof com.electronwill.nightconfig.core.Config) {
            return getValueSpec(path.subList(1, path.size()), (com.electronwill.nightconfig.core.Config) value);
        } else {
            return (ValueSpec) value;
        }
    }

    public static Point getHUDAnchor(HUDAnchor anchor) {
        Minecraft mc = Minecraft.getInstance();
        Point p = new Point();
        int displayWidth = mc.getWindow().getWidth();
        int displayHeight = mc.getWindow().getHeight();

        switch (anchor) {
            case ScreenTop -> p.x = displayWidth / 2;
            case ScreenTopRight -> p.x = displayWidth;
            case ScreenRight -> {
                p.x = displayWidth;
                p.y = displayHeight / 2;
            }
            case ScreenBottomRight -> {
                p.x = displayWidth;
                p.y = displayHeight;
            }
            case ScreenBottom -> {
                p.x = displayWidth / 2;
                p.y = displayHeight;
            }
            case ScreenBottomLeft -> p.y = displayHeight;
            case ScreenLeft -> p.y = displayHeight / 2;
            case ScreenTopLeft -> {}
            case Minimap -> p = getMinimapCorner();
            case MinimapHorizontal -> {
                p = getMinimapCorner();
                if (p.y < displayHeight / 2) {
                    p.y = 0;
                } else if (p.y > displayHeight / 2) {
                    p.y = displayHeight;
                }
            }
            case MinimapVertical -> {
                p = getMinimapCorner();
                if (p.x < displayWidth / 2) {
                    p.x = 0;
                } else if (p.x > displayWidth / 2) {
                    p.x = displayWidth;
                }
            }
        }

        return p;
    }

    public static Point getHUDOrigin(HUDAnchor anchor, int hudWidth, int hudHeight) {
        Point p = new Point();

        switch (anchor) {
            case ScreenTop -> p.x = hudWidth / 2;
            case ScreenTopRight -> p.x = hudWidth;
            case ScreenRight -> {
                p.x = hudWidth;
                p.y = hudHeight / 2;
            }
            case ScreenBottomRight -> {
                p.x = hudWidth;
                p.y = hudHeight;
            }
            case ScreenBottom -> {
                p.x = hudWidth / 2;
                p.y = hudHeight;
            }
            case ScreenBottomLeft -> p.y = hudHeight;
            case ScreenLeft -> p.y = hudHeight / 2;
            case ScreenTopLeft -> {}
            case Minimap, MinimapHorizontal, MinimapVertical -> p = getHUDOriginFromMinimap(hudWidth, hudHeight);
        }

        return p;
    }

    public static Point getMinimapCorner() {
        Minecraft mc = Minecraft.getInstance();

        Point corner = new Point();
        int displayWidth = mc.getWindow().getWidth();
        int displayHeight = mc.getWindow().getHeight();

        switch (Services.JOURNEYMAP.getMinimapPosition()) {
            case TopRight -> corner.x = displayWidth;
            case BottomRight -> {
                corner.x = displayWidth;
                corner.y = displayHeight;
            }
            case BottomLeft -> corner.y = displayHeight;
            case TopLeft -> {}
            case TopCenter -> corner.x = displayWidth / 2;
            case Center -> {
                corner.x = displayWidth / 2;
                corner.y = displayHeight / 2;
            }
        }

        if (Services.JOURNEYMAP.isMinimapEnabled()) {
            try {
                int minimapWidth = Services.JOURNEYMAP.getMinimapWidth();
                int minimapHeight = Services.JOURNEYMAP.getMinimapHeight();
                int translateX = Services.JOURNEYMAP.getMinimapTranslateX();
                int translateY = Services.JOURNEYMAP.getMinimapTranslateY();
                int margin = Services.JOURNEYMAP.getMinimapMargin();
                minimapWidth += margin * 2;
                minimapHeight += margin * 2;
                translateX += displayWidth / 2;
                translateY += displayHeight / 2;

                switch (Services.JOURNEYMAP.getMinimapPosition()) {
                    case TopRight -> {
                        corner.x = translateX - minimapWidth / 2;
                        corner.y = translateY + minimapHeight / 2;
                    }
                    case BottomRight -> {
                        corner.x = translateX - minimapWidth / 2;
                        corner.y = translateY - minimapHeight / 2;
                    }
                    case BottomLeft -> {
                        corner.x = translateX + minimapWidth / 2;
                        corner.y = translateY - minimapHeight / 2;
                    }
                    case TopLeft -> {
                        corner.x = translateX + minimapWidth / 2;
                        corner.y = translateY + minimapHeight / 2;
                    }
                    case TopCenter -> {
                        corner.x = translateX;
                        corner.y = translateY + minimapHeight / 2;
                    }
                    case Center -> {
                        corner.x = translateX;
                        corner.y = translateY;
                    }
                }
            } catch (Exception e) {
                MapFrontiers.LOGGER.warn(e.getMessage(), e);
            }
        }

        return corner;
    }

    public static Point getHUDOriginFromMinimap(int hudWidth, int hudHeight) {
        Point origin = new Point();
        switch (Services.JOURNEYMAP.getMinimapPosition()) {
            case TopRight -> origin.x = hudWidth;
            case BottomRight -> {
                origin.x = hudWidth;
                origin.y = hudHeight;
            }
            case BottomLeft -> origin.y = hudHeight;
            case TopLeft -> {}
            case TopCenter -> origin.x = hudWidth / 2;
            case Center -> {
                origin.x = hudWidth / 2;
                origin.y = hudHeight / 2;
            }
        }

        return origin;
    }

    public static class Point {
        public int x = 0;
        public int y = 0;
    }
}
