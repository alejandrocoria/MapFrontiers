package games.alejandrocoria.mapfrontiers.client.gui.hud;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.config.HUDAnchor;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.Minecraft;

public final class HUDPlacementHelper {
    public static class Point {
        public int x = 0;
        public int y = 0;
    }

    public static Point getHUDAnchor(HUDAnchor anchor) {
        Minecraft mc = Minecraft.getInstance();
        Point point = new Point();
        int displayWidth = mc.getWindow().getWidth();
        int displayHeight = mc.getWindow().getHeight();

        switch (anchor) {
            case ScreenTop -> point.x = displayWidth / 2;
            case ScreenTopRight -> point.x = displayWidth;
            case ScreenRight -> {
                point.x = displayWidth;
                point.y = displayHeight / 2;
            }
            case ScreenBottomRight -> {
                point.x = displayWidth;
                point.y = displayHeight;
            }
            case ScreenBottom -> {
                point.x = displayWidth / 2;
                point.y = displayHeight;
            }
            case ScreenBottomLeft -> point.y = displayHeight;
            case ScreenLeft -> point.y = displayHeight / 2;
            case ScreenTopLeft -> {}
            case Minimap -> point = getMinimapCorner();
            case MinimapHorizontal -> {
                point = getMinimapCorner();
                if (point.y < displayHeight / 2) {
                    point.y = 0;
                } else if (point.y > displayHeight / 2) {
                    point.y = displayHeight;
                }
            }
            case MinimapVertical -> {
                point = getMinimapCorner();
                if (point.x < displayWidth / 2) {
                    point.x = 0;
                } else if (point.x > displayWidth / 2) {
                    point.x = displayWidth;
                }
            }
        }

        return point;
    }

    public static Point getHUDOrigin(HUDAnchor anchor, int hudWidth, int hudHeight) {
        Point point = new Point();

        switch (anchor) {
            case ScreenTop -> point.x = hudWidth / 2;
            case ScreenTopRight -> point.x = hudWidth;
            case ScreenRight -> {
                point.x = hudWidth;
                point.y = hudHeight / 2;
            }
            case ScreenBottomRight -> {
                point.x = hudWidth;
                point.y = hudHeight;
            }
            case ScreenBottom -> {
                point.x = hudWidth / 2;
                point.y = hudHeight;
            }
            case ScreenBottomLeft -> point.y = hudHeight;
            case ScreenLeft -> point.y = hudHeight / 2;
            case ScreenTopLeft -> {}
            case Minimap, MinimapHorizontal, MinimapVertical -> point = getHUDOriginFromMinimap(hudWidth, hudHeight);
        }

        return point;
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

    private HUDPlacementHelper() {
    }
}
