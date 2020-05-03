package games.alejandrocoria.mapfrontiers.common;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RangeDouble;
import net.minecraftforge.common.config.Config.RangeInt;

@Config(modid = MapFrontiers.MODID)
public class ConfigData {
    public enum NameVisibility {
        Manual, Show, Hide
    }

    public enum HUDAnchor {
        ScreenTop, ScreenTopRight, ScreenRight, ScreenBottomRight, ScreenBottom, ScreenBottomLeft, ScreenLeft, ScreenTopLeft,
        Minimap, MinimapHorizontal, MinimapVertical
    };

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
        @Comment({
                "Anchor point of the HUD. In the case of choosing the minimap as an anchor, its default position will be used as a reference in the coordinates." })
        public HUDAnchor anchor = HUDAnchor.MinimapHorizontal;

        @Comment({ "Position of the HUD relative to anchor." })
        public Point position = new Point();
    }


    public static class Point {
        public int x = 0;
        public int y = 0;
    }
}