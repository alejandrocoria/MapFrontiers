package games.alejandrocoria.mapfrontiers.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.base.Splitter;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.ReflectionHelper;
import journeymap.client.io.ThemeLoader;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.minimap.Shape;
import journeymap.client.ui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber(modid = MapFrontiers.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ConfigData {
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;
    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public enum AfterCreatingFrontier {
        Info, Edit, None
    }

    public enum NameVisibility {
        Manual, Show, Hide
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

    public static boolean addVertexToNewFrontier;
    public static AfterCreatingFrontier afterCreatingFrontier;
    public static boolean alwaysShowUnfinishedFrontiers;
    public static NameVisibility nameVisibility;
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
        addVertexToNewFrontier = CLIENT.addVertexToNewFrontier.get();
        afterCreatingFrontier = CLIENT.afterCreatingFrontier.get();
        alwaysShowUnfinishedFrontiers = CLIENT.alwaysShowUnfinishedFrontiers.get();
        nameVisibility = CLIENT.nameVisibility.get();
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

    @SubscribeEvent
    public static void onModConfigEvent(ModConfig.ModConfigEvent configEvent) {
        if (configEvent.getConfig().getType()== ModConfig.Type.CLIENT) {
            bakeConfig();
        }
    }

    public static class ClientConfig {
        public final BooleanValue addVertexToNewFrontier;
        public final EnumValue<AfterCreatingFrontier> afterCreatingFrontier;
        public final BooleanValue alwaysShowUnfinishedFrontiers;
        public final EnumValue<NameVisibility> nameVisibility;
        public final DoubleValue polygonsOpacity;
        public final IntValue snapDistance;
        public final EnumValue<FilterFrontierType> filterFrontierType;
        public final EnumValue<FilterFrontierOwner> filterFrontierOwner;
        public final ForgeConfigSpec.ConfigValue<String> filterFrontierDimension;
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

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            addVertexToNewFrontier = builder.comment(
                    "If true, when a new frontier is created, the first vertex will automatically be added where the player is.")
                    .translation(MapFrontiers.MODID + ".config." + "addVertexToNewFrontier")
                    .define("addVertexToNewFrontier", true);
            afterCreatingFrontier = builder.comment(
                    "What to do after creating a new frontier.")
                    .translation(MapFrontiers.MODID + ".config." + "afterCreatingFrontier")
                    .defineEnum("afterCreatingFrontier", AfterCreatingFrontier.Info);
            alwaysShowUnfinishedFrontiers = builder.comment(
                    "With true, it always shows unfinished frontiers. With false, they will only be seen with the book in hand.")
                    .translation(MapFrontiers.MODID + ".config." + "alwaysShowUnfinishedFrontiers")
                    .define("alwaysShowUnfinishedFrontiers", true);
            nameVisibility = builder.comment(
                    "Force all frontier names to be shown on the map or hidden. In Manual you can decide for each frontier.")
                    .translation(MapFrontiers.MODID + ".config." + "nameVisibility")
                    .defineEnum("nameVisibility", NameVisibility.Manual);
            polygonsOpacity = builder
                    .comment("Transparency of the frontier polygons. 0.0 is fully transparent and 1.0 is no transparency.")
                    .translation(MapFrontiers.MODID + ".config." + "polygonsOpacity")
                    .defineInRange("polygonsOpacity", 0.4, 0.0, 1.0);
            snapDistance = builder.comment("Distance at which vertices are attached to nearby vertices.")
                    .translation(MapFrontiers.MODID + ".config." + "snapDistance").defineInRange("snapDistance", 8, 0, 16);
            filterFrontierType = builder.comment(
                            "Filter the list of frontier by type.")
                    .translation(MapFrontiers.MODID + ".config.filter." + "frontierType")
                    .defineEnum("filterFrontierType", FilterFrontierType.All);
            filterFrontierOwner = builder.comment(
                            "Filter the list of frontier by owner.")
                    .translation(MapFrontiers.MODID + ".config.filter." + "frontierOwner")
                    .defineEnum("filterFrontierOwner", FilterFrontierOwner.All);
            filterFrontierDimension = builder.comment(
                            "Filter the list of frontier by dimension.\nAllowed values are \"all\", \"current\" or the name of a dimension (eg: \"minecraft:the_nether\")")
                    .translation(MapFrontiers.MODID + ".config.filter." + "frontierDimension")
                    .define("filterFrontierDimension", "all");

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
            hudXPosition = builder.comment("Size of the HUD banner.")
                    .translation(MapFrontiers.MODID + ".config.hud." + "xPosition")
                    .defineInRange("xPosition", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            hudYPosition = builder.comment("Size of the HUD banner.")
                    .translation(MapFrontiers.MODID + ".config.hud." + "yPosition")
                    .defineInRange("yPosition", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            builder.pop();
        }
    }

    public static void save() {
        CLIENT.addVertexToNewFrontier.set(addVertexToNewFrontier);
        CLIENT.afterCreatingFrontier.set(afterCreatingFrontier);
        CLIENT.alwaysShowUnfinishedFrontiers.set(alwaysShowUnfinishedFrontiers);
        CLIENT.nameVisibility.set(nameVisibility);
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

    public static List<ITextComponent> getTooltip(String name) {
        List<ITextComponent> tooltip = new ArrayList<>();

        ValueSpec valueSpec = getValueSpec(name);
        if (valueSpec != null) {
            for (String string : Splitter.on("\n").split(valueSpec.getComment())) {
                tooltip.add(new StringTextComponent(string));
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

        if (value instanceof Config) {
            return getValueSpec(path.subList(1, path.size()), (Config) value);
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
        case ScreenTop:
            p.x = displayWidth / 2;
            break;
        case ScreenTopRight:
            p.x = displayWidth;
            break;
        case ScreenRight:
            p.x = displayWidth;
            p.y = displayHeight / 2;
            break;
        case ScreenBottomRight:
            p.x = displayWidth;
            p.y = displayHeight;
            break;
        case ScreenBottom:
            p.x = displayWidth / 2;
            p.y = displayHeight;
            break;
        case ScreenBottomLeft:
            p.y = displayHeight;
            break;
        case ScreenLeft:
            p.y = displayHeight / 2;
            break;
        case ScreenTopLeft:
            break;
        case Minimap:
            p = getMinimapCorner();
            break;
        case MinimapHorizontal:
            p = getMinimapCorner();
            if (p.y < displayHeight / 2) {
                p.y = 0;
            } else if (p.y > displayHeight / 2) {
                p.y = displayHeight;
            }
            break;
        case MinimapVertical:
            p = getMinimapCorner();
            if (p.x < displayWidth / 2) {
                p.x = 0;
            } else if (p.x > displayWidth / 2) {
                p.x = displayWidth;
            }
            break;
        }

        return p;
    }

    public static Point getHUDOrigin(HUDAnchor anchor, int hudWidth, int hudHeight) {
        Point p = new Point();

        switch (anchor) {
        case ScreenTop:
            p.x = hudWidth / 2;
            break;
        case ScreenTopRight:
            p.x = hudWidth;
            break;
        case ScreenRight:
            p.x = hudWidth;
            p.y = hudHeight / 2;
            break;
        case ScreenBottomRight:
            p.x = hudWidth;
            p.y = hudHeight;
            break;
        case ScreenBottom:
            p.x = hudWidth / 2;
            p.y = hudHeight;
            break;
        case ScreenBottomLeft:
            p.y = hudHeight;
            break;
        case ScreenLeft:
            p.y = hudHeight / 2;
            break;
        case ScreenTopLeft:
            break;
        case Minimap:
        case MinimapHorizontal:
        case MinimapVertical:
            p = getHUDOriginFromMinimap(hudWidth, hudHeight);
            break;
        }

        return p;
    }

    @OnlyIn(Dist.CLIENT)
    public static Point getMinimapCorner() {
        Minecraft mc = Minecraft.getInstance();

        Point corner = new Point();
        MiniMap minimap = UIManager.INSTANCE.getMiniMap();
        int displayWidth = mc.getWindow().getWidth();
        int displayHeight = mc.getWindow().getHeight();

        switch (minimap.getCurrentMinimapProperties().position.get()) {
        case TopRight:
            corner.x = displayWidth;
            break;
        case BottomRight:
            corner.x = displayWidth;
            corner.y = displayHeight;
            break;
        case BottomLeft:
            corner.y = displayHeight;
            break;
        case TopLeft:
            break;
        case TopCenter:
            corner.x = displayWidth / 2;
            break;
        case Center:
            corner.x = displayWidth / 2;
            corner.y = displayHeight / 2;
            break;
        }

        if (UIManager.INSTANCE.isMiniMapEnabled()) {
            try {
                DisplayVars dv = ReflectionHelper.getPrivateField(minimap, "dv");

                int minimapWidth = ReflectionHelper.getPrivateField(dv, "minimapWidth");
                int minimapHeight = ReflectionHelper.getPrivateField(dv, "minimapHeight");
                int translateX = ReflectionHelper.getPrivateField(dv, "translateX");
                int translateY = ReflectionHelper.getPrivateField(dv, "translateY");

                Theme.Minimap.MinimapSpec minimapSpec;
                if (minimap.getCurrentMinimapProperties().shape.get() == Shape.Circle) {
                    minimapSpec = ThemeLoader.getCurrentTheme().minimap.circle;
                } else {
                    minimapSpec = ThemeLoader.getCurrentTheme().minimap.square;
                }

                minimapWidth += minimapSpec.margin * 2;
                minimapHeight += minimapSpec.margin * 2;
                translateX += displayWidth / 2;
                translateY += displayHeight / 2;

                switch (minimap.getCurrentMinimapProperties().position.get()) {
                case TopRight:
                    corner.x = translateX - minimapWidth / 2;
                    corner.y = translateY + minimapHeight / 2;
                    break;
                case BottomRight:
                    corner.x = translateX - minimapWidth / 2;
                    corner.y = translateY - minimapHeight / 2;
                    break;
                case BottomLeft:
                    corner.x = translateX + minimapWidth / 2;
                    corner.y = translateY - minimapHeight / 2;
                    break;
                case TopLeft:
                    corner.x = translateX + minimapWidth / 2;
                    corner.y = translateY + minimapHeight / 2;
                    break;
                case TopCenter:
                    corner.x = translateX;
                    corner.y = translateY + minimapHeight / 2;
                    break;
                case Center:
                    corner.x = translateX;
                    corner.y = translateY;
                    break;
                }
            } catch (Exception e) {
                MapFrontiers.LOGGER.warn(e.getMessage(), e);
            }
        }

        return corner;
    }

    public static Point getHUDOriginFromMinimap(int hudWidth, int hudHeight) {
        Point origin = new Point();
        MiniMap minimap = UIManager.INSTANCE.getMiniMap();

        switch (minimap.getCurrentMinimapProperties().position.get()) {
        case TopRight:
            origin.x = hudWidth;
            break;
        case BottomRight:
            origin.x = hudWidth;
            origin.y = hudHeight;
            break;
        case BottomLeft:
            origin.y = hudHeight;
            break;
        case TopLeft:
            break;
        case TopCenter:
            origin.x = hudWidth / 2;
            break;
        case Center:
            origin.x = hudWidth / 2;
            origin.y = hudHeight / 2;
            break;
        }

        return origin;
    }

    public static class Point {
        public int x = 0;
        public int y = 0;
    }
}
