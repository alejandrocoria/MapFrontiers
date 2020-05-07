package games.alejandrocoria.mapfrontiers.common;

import java.util.HashMap;
import java.util.Map;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Ignore;
import net.minecraftforge.common.config.Config.RangeDouble;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.IConfigElement;

@Config(modid = MapFrontiers.MODID)
public class ConfigData {
    public enum NameVisibility {
        Manual, Show, Hide
    }

    public enum HUDAnchor {
        ScreenTop, ScreenTopRight, ScreenRight, ScreenBottomRight, ScreenBottom, ScreenBottomLeft, ScreenLeft, ScreenTopLeft,
        Minimap, MinimapHorizontal, MinimapVertical
    }

    public enum HUDSlot {
        None, Name, Owner, Banner
    }

    @Ignore
    private static Map<String, IConfigElement> properties;

    @Comment({ "If true, when a new frontier is created, the first vertex will automatically be added where the player is." })
    public static boolean addVertexToNewFrontier = true;

    @Comment({ "With true, it always shows unfinished frontiers. With false, they will only be seen with the book in hand." })
    public static boolean alwaysShowUnfinishedFrontiers = false;

    @Comment({ "Force all frontier names to be shown on the map or hidden. In Manual you can decide for each frontier." })
    public static NameVisibility nameVisibility = NameVisibility.Manual;

    @Comment({ "Transparency of the frontier polygons. 0.0 is fully transparent and 1.0 is no transparency." })
    @RangeDouble(min = 0.0, max = 1.0)
    public static double polygonsOpacity = 0.4;

    @Comment({ "Distance at which vertices are attached to nearby vertices." })
    @RangeInt(min = 0, max = 16)
    public static int snapDistance = 8;

    public static HUDConfig hud = new HUDConfig();


    public static class HUDConfig {
        @Comment({ "Show the HUD on screen." })
        public boolean enabled = true;

        @Comment({ "Automatically switch to nearest anchor when HUD position is edited (on settings screen)." })
        public boolean autoAdjustAnchor = true;

        @Comment({ "Size of the HUD banner." })
        @RangeInt(min = 1, max = 8)
        public int bannerSize = 3;

        @Comment({ "HUD element on slot 1." })
        public HUDSlot slot1 = HUDSlot.Name;

        @Comment({ "HUD element on slot 2." })
        public HUDSlot slot2 = HUDSlot.Owner;

        @Comment({ "HUD element on slot 3." })
        public HUDSlot slot3 = HUDSlot.Banner;

        @Comment({
                "Anchor point of the HUD. In the case of choosing the minimap as an anchor, its default position will be used as a reference in the coordinates." })
        public HUDAnchor anchor = HUDAnchor.MinimapHorizontal;

        @Comment({ "Position of the HUD relative to anchor." })
        public Point position = new Point();
    }


    public static boolean isInRange(String fieldName, int value) {
        try {
            RangeInt range = ConfigData.class.getField(fieldName).getAnnotation(RangeInt.class);
            return value >= range.min() && value <= range.max();
        } catch (Exception e) {
            MapFrontiers.LOGGER.warn(e.getMessage(), e);
        }

        return false;
    }

    public static boolean isInRange(String fieldName, double value) {
        try {
            RangeDouble range = ConfigData.class.getField(fieldName).getAnnotation(RangeDouble.class);
            return value >= range.min() && value <= range.max();
        } catch (Exception e) {
            MapFrontiers.LOGGER.warn(e.getMessage(), e);
        }

        return false;
    }

    public static Object getDefault(String fieldName) {
        ensureProperties();

        IConfigElement configElement = properties.get(fieldName);
        if (configElement == null) {
            return null;
        }

        return configElement.getDefault();
    }

    public static Point getHUDAnchor() {
        Minecraft mc = Minecraft.getMinecraft();
        Point p = new Point();

        switch (ConfigData.hud.anchor) {
        case ScreenTop:
            p.x = mc.displayWidth / 2;
            break;
        case ScreenTopRight:
            p.x = mc.displayWidth;
            break;
        case ScreenRight:
            p.x = mc.displayWidth;
            p.y = mc.displayHeight / 2;
            break;
        case ScreenBottomRight:
            p.x = mc.displayWidth;
            p.y = mc.displayHeight;
            break;
        case ScreenBottom:
            p.x = mc.displayWidth / 2;
            p.y = mc.displayHeight;
            break;
        case ScreenBottomLeft:
            p.y = mc.displayHeight;
            break;
        case ScreenLeft:
            p.y = mc.displayHeight / 2;
            break;
        case ScreenTopLeft:
            break;
        case Minimap:
            // @Incomplete
            p.x = mc.displayWidth;
            break;
        case MinimapHorizontal:
            p.x = mc.displayWidth;
            break;
        case MinimapVertical:
            p.x = mc.displayWidth;
            break;
        }

        return p;
    }

    public static Point getHUDOrigin(int hudWidth, int hudHeight) {
        Point p = new Point();

        switch (ConfigData.hud.anchor) {
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
            // @Incomplete
            p.x = hudWidth;
            break;
        case MinimapHorizontal:
            p.x = hudWidth;
            break;
        case MinimapVertical:
            p.x = hudWidth;
            break;
        }

        return p;
    }

    private static void ensureProperties() {
        if (properties != null) {
            return;
        }

        properties = new HashMap<String, IConfigElement>();

        for (IConfigElement configElement : ConfigElement.from(ConfigData.class).getChildElements()) {
            properties.put(configElement.getName(), configElement);
        }
    }


    public static class Point {
        public int x = 0;
        public int y = 0;
    }
}