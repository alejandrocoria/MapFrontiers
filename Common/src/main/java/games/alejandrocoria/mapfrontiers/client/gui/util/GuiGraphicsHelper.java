package games.alejandrocoria.mapfrontiers.client.gui.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class GuiGraphicsHelper {
    private GuiGraphicsHelper() {
    }

    public static void blitTinted(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v,
                                  int width, int height, int textureWidth, int textureHeight, int color) {
        setColor(graphics, color);
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        resetColor(graphics);
    }

    public static void blitTinted(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v,
                                  int width, int height, int sourceWidth, int sourceHeight,
                                  int textureWidth, int textureHeight, int color) {
        setColor(graphics, color);
        graphics.blit(texture, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
        resetColor(graphics);
    }

    public static void blitTranslucent(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v,
                                       int width, int height, int textureWidth, int textureHeight) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        graphics.flush();
        RenderSystem.disableBlend();
    }

    public static void blitTranslucent(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                                       int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(texture, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
        graphics.flush();
        RenderSystem.disableBlend();
    }

    /**
     * Enables a scissor rectangle in the current pose coordinate space.
     *
     * <p>GuiGraphics scissor coordinates do not account for its pose stack in this Minecraft version, while normal
     * GUI rendering does. Transforming the rectangle here keeps clipping aligned with widgets rendered under a
     * scaled or translated pose. GuiGraphics still applies the window GUI scale when converting to framebuffer
     * coordinates.</p>
     */
    public static void enableTransformedScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        Matrix4f pose = graphics.pose().last().pose();

        float x1 = transformX(pose, left, top);
        float y1 = transformY(pose, left, top);
        float x2 = transformX(pose, right, top);
        float y2 = transformY(pose, right, top);
        float x3 = transformX(pose, left, bottom);
        float y3 = transformY(pose, left, bottom);
        float x4 = transformX(pose, right, bottom);
        float y4 = transformY(pose, right, bottom);

        graphics.enableScissor((int) Math.floor(Math.min(Math.min(x1, x2), Math.min(x3, x4))),
                (int) Math.floor(Math.min(Math.min(y1, y2), Math.min(y3, y4))),
                (int) Math.ceil(Math.max(Math.max(x1, x2), Math.max(x3, x4))),
                (int) Math.ceil(Math.max(Math.max(y1, y2), Math.max(y3, y4))));
    }

    private static float transformX(Matrix4f matrix, float x, float y) {
        return matrix.m00() * x + matrix.m10() * y + matrix.m30();
    }

    private static float transformY(Matrix4f matrix, float x, float y) {
        return matrix.m01() * x + matrix.m11() * y + matrix.m31();
    }

    private static void setColor(GuiGraphics graphics, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        graphics.setColor(red, green, blue, alpha);
    }

    private static void resetColor(GuiGraphics graphics) {
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
