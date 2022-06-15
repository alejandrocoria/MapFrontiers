package games.alejandrocoria.mapfrontiers.common;

import com.google.common.base.Splitter;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.ReflectionHelper;
import journeymap.client.io.ThemeLoader;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.minimap.Shape;
import journeymap.client.ui.theme.Theme;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.ArrayList;
import java.util.List;

@Config(name = MapFrontiers.MODID)
public class ConfigData implements me.shedaniel.autoconfig.ConfigData {
    public enum AfterCreatingFrontier {
        Info, Edit, Nothing
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

    @ConfigEntry.BoundedDiscrete(min = 0, max = 11)
    public static int newFrontierShape;
    @ConfigEntry.BoundedDiscrete(min = 0, max = 999)
    public static int newFrontierShapeWidth;
    @ConfigEntry.BoundedDiscrete(min = 0, max = 999)
    public static int newFrontierShapeRadius;
    @ConfigEntry.BoundedDiscrete(min = 0, max = 7)
    public static int newFrontierChunkShape;
    @ConfigEntry.BoundedDiscrete(min = 0, max = 32)
    public static int newFrontierChunkShapeWidth;
    @ConfigEntry.BoundedDiscrete(min = 0, max = 32)
    public static int newFrontierChunkShapeLength;
    public static FrontierData.Mode newFrontierMode;
    public static AfterCreatingFrontier afterCreatingFrontier;
    public static NameVisibility nameVisibility;
    public static boolean hideNamesThatDontFit;
    public static double polygonsOpacity;
    @ConfigEntry.BoundedDiscrete(min = 0, max = 16)
    public static int snapDistance;
    public static FilterFrontierType filterFrontierType;
    public static FilterFrontierOwner filterFrontierOwner;
    public static String filterFrontierDimension;
    public static boolean hudEnabled;
    public static boolean hudAutoAdjustAnchor;
    public static boolean hudSnapToBorder;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
    public static int hudBannerSize;
    public static HUDSlot hudSlot1;
    public static HUDSlot hudSlot2;
    public static HUDSlot hudSlot3;
    public static HUDAnchor hudAnchor;
    @ConfigEntry.BoundedDiscrete(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    public static int hudXPosition;
    @ConfigEntry.BoundedDiscrete(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    public static int hudYPosition;

    public ConfigData() {
        newFrontierShape = 0;
        newFrontierShapeWidth = 10;
        newFrontierShapeRadius = 20;
        newFrontierChunkShape = 0;
        newFrontierChunkShapeWidth = 5;
        newFrontierChunkShapeLength = 5;
        newFrontierMode = FrontierData.Mode.Vertex;
        afterCreatingFrontier = AfterCreatingFrontier.Info;
        nameVisibility = NameVisibility.Manual;
        hideNamesThatDontFit = true;
        polygonsOpacity = 0.4;
        snapDistance = 8;
        filterFrontierType = FilterFrontierType.All;
        filterFrontierOwner = FilterFrontierOwner.All;
        filterFrontierDimension = "all";
        hudEnabled = true;
        hudAutoAdjustAnchor = true;
        hudSnapToBorder = true;
        hudBannerSize = 3;
        hudSlot1 = HUDSlot.Name;
        hudSlot2 = HUDSlot.Owner;
        hudSlot3 = HUDSlot.Banner;
        hudAnchor = HUDAnchor.MinimapHorizontal;
        hudXPosition = 0;
        hudYPosition = 0;
    }

    @Override
    public void validatePostLoad() {
        polygonsOpacity = Math.min(Math.max(polygonsOpacity, 0.0), 1.0);
    }

    public static Component getTranslatedName(String name) {
        return new TranslatableComponent(MapFrontiers.MODID + ".config." + name);
    }

    public static <E extends Enum<E>> Component getTranslatedEnum(E value) {
        return new TranslatableComponent("mapfrontiers.config." + value.name());
    }

    public static List<Component> getTooltip(String name) {
        List<Component> tooltip = new ArrayList<>();

        String lines = new TranslatableComponent(MapFrontiers.MODID + ".config." + name + ".tooltip").getString();
        for (String string : Splitter.on("\n").split(lines)) {
            tooltip.add(new TextComponent(string));
        }

        return tooltip;
    }

    public static boolean isInRange(String name, Object value) {
        try {
            ConfigEntry.BoundedDiscrete annotation = ConfigData.class.getDeclaredField(name).getAnnotation(ConfigEntry.BoundedDiscrete.class);
            if (annotation != null && value instanceof Integer) {
                int val = (Integer) value;
                return val >= annotation.min() && val <= annotation.max();
            }
        } catch (Exception ignored) {
            return true;
        }

        return true;
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

    @Environment(EnvType.CLIENT)
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
                DisplayVars dv = minimap.getDisplayVars();

                int minimapWidth = dv.minimapWidth;
                int minimapHeight = dv.minimapHeight;
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
