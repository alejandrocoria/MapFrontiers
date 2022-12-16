package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiColorPicker extends AbstractWidget {
    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/color_picker.png");
    private static final int textureSizeX = 274;
    private static final int textureSizeY = 134;
    private static final int[] palette = {
            0xffff0000, 0xffff8000, 0xffffff00, 0xff80ff00, 0xff00ff00, 0xff00ff80,
            0xff00ffff, 0xff0080ff, 0xff0000ff, 0xff8000ff, 0xffff00ff, 0xffff0080,
            0xff572f07, 0xff000000, 0xff404040, 0xff808080, 0xffbfbfbf, 0xffffffff};

    private static final int[] paletteInactive = {
            0xff343434, 0xff595959, 0xff7e7e7e, 0xff6b6b6b, 0xff585858, 0xff5f5f5f,
            0xff666666, 0xff424242, 0xff1c1c1c, 0xff2f2f2f, 0xff424242, 0xff3b3b3b,
            0xff292929, 0xff0e0e0e, 0xff2e2e2e, 0xff4d4d4d, 0xff6d6d6d, 0xff8d8d8d};

    private double hsX;
    private double hsY;
    private double v;
    private int color;
    private int colorFullBrightness;
    private boolean hsGrabbed = false;
    private boolean vGrabbed = false;
    private final BiConsumer<GuiColorPicker, Boolean> callbackColorUpdated;

    public GuiColorPicker(int x, int y, int color, BiConsumer<GuiColorPicker, Boolean> callbackColorUpdated) {
        super(x, y, 304, 127, Component.empty());
        this.callbackColorUpdated = callbackColorUpdated;
        setColor(color);
    }

    public void setColor(int newColor) {
        color = newColor;
        float[] hsv = Color.RGBtoHSB((newColor & 0xff0000) >> 16, (newColor & 0x00ff00) >> 8, newColor & 0x0000ff, null);
        double angle = hsv[0] * Math.PI * 2.0;
        double dist = hsv[1] * 64.0;
        hsX = dist * Math.cos(angle);
        hsY = dist * Math.sin(angle);
        v = 127.5 - hsv[2] * 127.5;
        colorFullBrightness = Color.HSBtoRGB(hsv[0], hsv[1], 1.f);
    }

    public int getColor() {
        return color;
    }

    @Override
    public boolean clicked(double mouseX, double mouseY) {
        if (!active || !visible) {
            return false;
        }

        return mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return clicked(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        hsGrabbed = false;
        vGrabbed = false;

        updateMouse(mouseX, mouseY, false);

        if (!hsGrabbed && !vGrabbed) {
            double paletteX = (mouseX - (getX() + 165)) / 23.0;
            double paletteY = (mouseY - (getY() + 57)) / 23.0;
            if (paletteX >= 0.0 && paletteX < 6.0 && paletteY >= 0.0 && paletteY < 3.0) {
                setColor(palette[(int) paletteX + (int) paletteY * 6]);
                callbackColorUpdated.accept(this, false);
            }
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        updateMouse(mouseX, mouseY, false);
        hsGrabbed = false;
        vGrabbed = false;
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        updateMouse(mouseX, mouseY, true);
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        int texX = active ? 0 : 137;

        blit(matrixStack, getX(), getY(), texX, 0, 128, 128, textureSizeX, textureSizeY);
        if (active) {
            RenderSystem.setShaderColor(((colorFullBrightness & 0xff0000) >> 16) / 255.f, ((colorFullBrightness & 0x00ff00) >> 8) / 255.f, (colorFullBrightness & 0x0000ff) / 255.f, 1.f);
        }
        blit(matrixStack, getX() + 132, getY(), texX + 129, 0, 8, 128, textureSizeX, textureSizeY);
        if (!active) {
            RenderSystem.setShaderColor(((colorFullBrightness & 0xff0000) >> 16) / 255.f, ((colorFullBrightness & 0x00ff00) >> 8) / 255.f, (colorFullBrightness & 0x0000ff) / 255.f, 1.f);
        }
        fill(matrixStack, getX() + (int) hsX + 64, getY() + (int) hsY + 64, getX() + (int) hsX + 65, getY() + (int) hsY + 65, colorFullBrightness);
        fill(matrixStack, getX() + 131, getY() + (int) v, getX() + 139, getY() + (int) v + 1, color);
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        blit(matrixStack, getX() + (int) hsX + 64 - 2, getY() + (int) hsY + 64 - 2, texX, 129, 5, 5, textureSizeX, textureSizeY);
        blit(matrixStack, getX() + 131, getY() + (int) v - 2, texX + 6, 129, 10, 5, textureSizeX, textureSizeY);

        fill(matrixStack, getX() + 165, getY() + 57, getX() + 304, getY() + 127, 0xff000000);
        int col = 0;
        int row = 0;
        for (int c : (active ? palette : paletteInactive)) {
            if (active && c == color) {
                fill(matrixStack, getX() + 165 + col * 23, getY() + 57 + row * 23, getX() + 189 + col * 23, getY() + 81 + row * 23, 0xffffffff);
            }
            fill(matrixStack, getX() + 166 + col * 23, getY() + 58 + row * 23, getX() + 188 + col * 23, getY() + 80 + row * 23, c);
            ++col;
            if (col == 6) {
                col = 0;
                ++row;
            }
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    private void updateMouse(double mouseX, double mouseY, boolean dragging) {
        if (!vGrabbed) {
            double localX = mouseX - (getX() + 64);
            double localY = mouseY - (getY() + 64);
            double dist = Math.sqrt(localX * localX + localY * localY);

            if (dist < 66.0) {
                hsGrabbed = true;
            }

            if (hsGrabbed) {
                if (dist >= 64.0) {
                    localX = localX / dist * 64.0;
                    localY = localY / dist * 64.0;
                }
                hsX = localX;
                hsY = localY;
                updateColor(dragging);
                return;
            }
        }

        if (!hsGrabbed) {
            double localX = mouseX - (getX() + 132);
            double localY = mouseY - getY();

            if (localX >= 0 && localX < 8 && localY >= 0 && localY < 128.0) {
                vGrabbed = true;
            }

            if (vGrabbed) {
                v = Math.max(0.0, Math.min(localY, 127.99));
                updateColor(dragging);
            }
        }

    }

    private void updateColor(boolean dragging) {
        double dist = Math.sqrt(hsX * hsX + hsY * hsY);
        double hue = Math.atan2(hsY, hsX) / (Math.PI * 2.0);
        double sat = dist / 64.0;
        double lum = 1.0 - v / 128.0;

        color = Color.HSBtoRGB((float) hue, (float) sat, (float) lum);
        colorFullBrightness = Color.HSBtoRGB((float) hue, (float) sat, 1.f);
        callbackColorUpdated.accept(this, dragging);
    }
}
