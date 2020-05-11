package games.alejandrocoria.mapfrontiers.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.ReflectionHelper;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import journeymap.client.ui.minimap.MiniMap;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Ignore;
import net.minecraftforge.common.config.Config.RangeDouble;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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

        @Comment({ "Automatically snap to closest border when HUD position is edited (on settings screen)." })
        public boolean snapToBorder = true;

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
        ensureProperties();

        IConfigElement configElement = properties.get(fieldName);
        if (configElement == null) {
            return false;
        }

        int min = Integer.parseInt((String) configElement.getMinValue());
        int max = Integer.parseInt((String) configElement.getMaxValue());

        return value >= min && value <= max;
    }

    public static boolean isInRange(String fieldName, double value) {
        ensureProperties();

        IConfigElement configElement = properties.get(fieldName);
        if (configElement == null) {
            return false;
        }

        double min = Double.parseDouble((String) configElement.getMinValue());
        double max = Double.parseDouble((String) configElement.getMaxValue());

        return value >= min && value <= max;
    }

    public static Object getDefault(String fieldName) {
        ensureProperties();

        IConfigElement configElement = properties.get(fieldName);
        if (configElement == null) {
            return null;
        }

        return configElement.getDefault();
    }

    public static Point getHUDAnchor(HUDAnchor anchor) {
        Minecraft mc = Minecraft.getMinecraft();
        Point p = new Point();

        switch (anchor) {
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
            p = getMinimapCorner();
            break;
        case MinimapHorizontal:
            p = getMinimapCorner();
            if (p.y < mc.displayHeight / 2) {
                p.y = 0;
            } else if (p.y > mc.displayHeight / 2) {
                p.y = mc.displayHeight;
            }
            break;
        case MinimapVertical:
            p = getMinimapCorner();
            if (p.x < mc.displayWidth / 2) {
                p.x = 0;
            } else if (p.x > mc.displayWidth / 2) {
                p.x = mc.displayWidth;
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

    @SideOnly(Side.CLIENT)
    public static Point getMinimapCorner() {
        Minecraft mc = Minecraft.getMinecraft();

        Point corner = new Point();
        MiniMap minimap = UIManager.INSTANCE.getMiniMap();

        switch (minimap.getCurrentMinimapProperties().position.get()) {
        case TopRight:
            corner.x = mc.displayWidth;
            break;
        case BottomRight:
            corner.x = mc.displayWidth;
            corner.y = mc.displayHeight;
            break;
        case BottomLeft:
            corner.y = mc.displayHeight;
            break;
        case TopLeft:
            break;
        case TopCenter:
            corner.x = mc.displayWidth / 2;
            break;
        case Center:
            corner.x = mc.displayWidth / 2;
            corner.y = mc.displayHeight / 2;
            break;
        }

        if (UIManager.INSTANCE.isMiniMapEnabled()) {
            try {
                DisplayVars dv = ReflectionHelper.getPrivateField(minimap, "dv", DisplayVars.class);

                int minimapWidth = ReflectionHelper.getPrivateField(dv, "minimapWidth", int.class);
                int minimapHeight = ReflectionHelper.getPrivateField(dv, "minimapHeight", int.class);
                int translateX = ReflectionHelper.getPrivateField(dv, "translateX", int.class);
                int translateY = ReflectionHelper.getPrivateField(dv, "translateY", int.class);

                translateX += mc.displayWidth / 2;
                translateY += mc.displayHeight / 2;

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

    private static void ensureProperties() {
        if (properties != null) {
            return;
        }

        properties = new HashMap<String, IConfigElement>();
        addProperties(ConfigElement.from(ConfigData.class).getChildElements(), "");
    }

    private static void addProperties(List<IConfigElement> elements, String prefix) {
        if (elements == null) {
            return;
        }

        for (IConfigElement configElement : elements) {
            if (configElement.isProperty()) {
                properties.put(prefix + configElement.getName(), configElement);
            } else {
                addProperties(configElement.getChildElements(), prefix + configElement.getName() + ".");
            }
        }
    }


    public static class Point {
        public int x = 0;
        public int y = 0;
    }
}