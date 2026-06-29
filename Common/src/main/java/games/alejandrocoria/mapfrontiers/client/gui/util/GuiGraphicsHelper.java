package games.alejandrocoria.mapfrontiers.client.gui.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

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
