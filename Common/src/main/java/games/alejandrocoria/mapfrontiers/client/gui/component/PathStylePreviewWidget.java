package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PathStylePreviewWidget extends AbstractWidgetNoNarration {
    private static final Identifier backgroundTexture = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/frontier_preview_bg.png");
    private static final int WIDTH = 220;
    private static final int HEIGHT = 150;
    private static final Vec2 START = new Vec2(34.f, 106.f);
    private static final Vec2 MIDDLE = new Vec2(108.f, 54.f);
    private static final Vec2 END = new Vec2(184.f, 98.f);

    private FrontierData.PathStyle style = new FrontierData.PathStyle();

    public PathStylePreviewWidget() {
        super(0, 0, WIDTH, HEIGHT, Component.empty());
    }

    public void setPathStyle(FrontierData.PathStyle style) {
        this.style = new FrontierData.PathStyle(style);
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, getX(), getY(), 0, 0, getWidth(), getHeight(), 420, 420, 420, 420);
        graphics.horizontalLine(getX(), getX() + getWidth() - 1, getY(), ColorConstants.OPTION_BORDER);
        graphics.horizontalLine(getX(), getX() + getWidth() - 1, getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);
        graphics.verticalLine(getX(), getY(), getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);
        graphics.verticalLine(getX() + getWidth() - 1, getY(), getY() + getHeight() - 1, ColorConstants.OPTION_BORDER);

        drawRepeatedMarkers(graphics, START, MIDDLE, style.segmentMarker);
        drawRepeatedMarkers(graphics, MIDDLE, END, style.segmentMarker);

        drawMarker(graphics, START, style.startMarker, getRotation(START, MIDDLE));
        drawMarker(graphics, MIDDLE, style.middleMarker, getRotation(MIDDLE, END));
        drawMarker(graphics, END, style.endMarker, getRotation(MIDDLE, END));

        if (style.labelAtStart) {
            drawLabelBlock(graphics, add(START, scale(normalize(subtract(START, MIDDLE)), 34.f)));
        }

        if (style.labelAtMiddle) {
            drawLabelBlock(graphics, getPolylineMidpoint());
        }

        if (style.labelAtEnd) {
            drawLabelBlock(graphics, add(END, scale(normalize(subtract(END, MIDDLE)), 34.f)));
        }
    }

    private void drawRepeatedMarkers(GuiGraphicsExtractor graphics, Vec2 from, Vec2 to, Identifier markerId) {
        PathMarkerCatalog.Entry entry = PathMarkerCatalog.get(markerId);
        if (entry == null || entry.texture() == null) {
            return;
        }

        float length = distance(from, to);
        if (length < 1.f) {
            return;
        }

        float rotation = getRotation(from, to);
        float spacing = 18.f;
        for (float distance = spacing; distance < length; distance += spacing) {
            drawMarker(graphics, lerp(from, to, distance / length), markerId, rotation);
        }
    }

    private void drawMarker(GuiGraphicsExtractor graphics, Vec2 center, Identifier markerId, float rotation) {
        PathMarkerCatalog.Entry entry = PathMarkerCatalog.get(markerId);
        if (entry == null || entry.texture() == null) {
            return;
        }

        float centerX = getX() + center.x;
        float centerY = getY() + center.y;
        int width = entry.width();
        int height = entry.height();

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        if (entry.directional()) {
            graphics.pose().rotate((float) Math.toRadians(rotation));
        }
        graphics.pose().translate(-centerX, -centerY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, entry.texture(), Math.round(centerX - width / 2.f), Math.round(centerY - height / 2.f), 0, 0,
                width, height, width, height, width, height);
        graphics.pose().popMatrix();
    }

    private void drawLabelBlock(GuiGraphicsExtractor graphics, Vec2 anchor) {
        int width = 64;
        int height = 24;
        int x = Math.round(getX() + anchor.x - width / 2.f);
        int y = Math.round(getY() + anchor.y - height / 2.f);

        graphics.fill(x, y, x + width, y + height, ColorConstants.TEXTBOX_EXTRA_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xCC101010);
        graphics.fill(x + 4, y + 14, x + 14, y + 22, 0xFF8B1E1E);
        graphics.text(Minecraft.getInstance().font, Component.literal("Preview"), x + 18, y + 4, ColorConstants.TEXT_HIGHLIGHT);
        graphics.text(Minecraft.getInstance().font, Component.literal("Owner"), x + 18, y + 14, ColorConstants.TEXT_MEDIUM);
    }

    private static float getRotation(Vec2 from, Vec2 to) {
        return (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
    }

    private static Vec2 getPolylineMidpoint() {
        float firstLength = distance(START, MIDDLE);
        float secondLength = distance(MIDDLE, END);
        float half = (firstLength + secondLength) / 2.f;
        if (half <= firstLength) {
            return lerp(START, MIDDLE, half / firstLength);
        }

        return lerp(MIDDLE, END, (half - firstLength) / secondLength);
    }

    private static Vec2 lerp(Vec2 from, Vec2 to, float t) {
        return new Vec2(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t);
    }

    private static Vec2 subtract(Vec2 a, Vec2 b) {
        return new Vec2(a.x - b.x, a.y - b.y);
    }

    private static Vec2 add(Vec2 a, Vec2 b) {
        return new Vec2(a.x + b.x, a.y + b.y);
    }

    private static Vec2 scale(Vec2 vector, float scale) {
        return new Vec2(vector.x * scale, vector.y * scale);
    }

    private static Vec2 normalize(Vec2 vector) {
        float length = (float) Math.sqrt(vector.x * vector.x + vector.y * vector.y);
        if (length < 0.0001f) {
            return new Vec2(0.f, 1.f);
        }

        return new Vec2(vector.x / length, vector.y / length);
    }

    private static float distance(Vec2 a, Vec2 b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
