package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import journeymap.client.ui.minimap.MiniMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiColorPicker extends Widget {
    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/color_picker.png");
    private static final int textureSizeX = 137;
    private static final int textureSizeY = 134;
    private static final int[] palette = {
            0xffff0000, 0xffff8000, 0xffffff00, 0xff80ff00, 0xff00ff00, 0xff00ff80,
            0xff00ffff, 0xff0080ff, 0xff0000ff, 0xff8000ff, 0xffff00ff, 0xffff0080,
            0xff572f07, 0xff000000, 0xff404040, 0xff808080, 0xffbfbfbf, 0xffffffff};

    private double hsX;
    private double hsY;
    private double v;
    private int color;
    private int colorFullBrightness;
    private boolean hsGrabbed = false;
    private boolean vGrabbed = false;
    private final Consumer<GuiColorPicker> callbackColorUpdated;

    public GuiColorPicker(int x, int y, int color, Consumer<GuiColorPicker> callbackColorUpdated) {
        super(x, y, 304, 127, StringTextComponent.EMPTY);
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

        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return clicked(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        hsGrabbed = false;
        vGrabbed = false;

        updateMouse(mouseX, mouseY);

        if (!hsGrabbed && !vGrabbed) {
            double paletteX = (mouseX - (x + 165)) / 23.0;
            double paletteY = (mouseY - (y + 57)) / 23.0;
            if (paletteX >= 0.0 && paletteX < 6.0 && paletteY >= 0.0 && paletteY < 3.0) {
                setColor(palette[(int) paletteX + (int) paletteY * 6]);
                callbackColorUpdated.accept(this);
            }
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        updateMouse(mouseX, mouseY);
        hsGrabbed = false;
        vGrabbed = false;
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        updateMouse(mouseX, mouseY);
    }

    @Override
    public void playDownSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.color4f(1.f, 1.f, 1.f, 1f);
        Minecraft.getInstance().getTextureManager().bind(texture);

        blit(matrixStack, x, y, 0, 0, 128, 128, textureSizeX, textureSizeY);
        RenderSystem.color4f(((colorFullBrightness & 0xff0000) >> 16) / 255.f, ((colorFullBrightness & 0x00ff00) >> 8) / 255.f, (colorFullBrightness & 0x0000ff) / 255.f, 1f);
        blit(matrixStack, x + 132, y, 129, 0, 8, 128, textureSizeX, textureSizeY);
        fill(matrixStack, x + (int) hsX + 64, y + (int) hsY + 64, x + (int) hsX + 65, y + (int) hsY + 65, colorFullBrightness);
        fill(matrixStack, x + 131, y + (int) v, x + 139, y + (int) v + 1, color);
        RenderSystem.color4f(1.f, 1.f, 1.f, 1f);
        blit(matrixStack, x + (int) hsX + 64 - 2, y + (int) hsY + 64 - 2, 0, 129, 5, 5, textureSizeX, textureSizeY);
        blit(matrixStack, x + 131, y + (int) v - 2, 6, 129, 10, 5, textureSizeX, textureSizeY);

        fill(matrixStack, x + 165, y + 57, x + 304, y + 127, 0xff000000);
        int row = 0;
        int col = 0;
        for (int c : palette) {
            if (c == color) {
                fill(matrixStack, x + 165 + row * 23, y + 57 + col * 23, x + 189 + row * 23, y + 81 + col * 23, 0xffffffff);
            }
            fill(matrixStack, x + 166 + row * 23, y + 58 + col * 23, x + 188 + row * 23, y + 80 + col * 23, c);
            ++row;
            if (row == 6) {
                row = 0;
                ++col;
            }
        }
    }

    private boolean updateMouse(double mouseX, double mouseY) {
        if (!vGrabbed) {
            double localX = mouseX - (x + 64);
            double localY = mouseY - (y + 64);
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
                updateColor();
                return true;
            }
        }

        if (!hsGrabbed) {
            double localX = mouseX - (x + 132);
            double localY = mouseY - y;

            if (localX >= 0 && localX < 8 && localY >= 0 && localY < 128.0) {
                vGrabbed = true;
            }

            if (vGrabbed) {
                v = Math.max(0.0, Math.min(localY, 127.99));
                updateColor();
                return true;
            }
        }

        return false;
    }

    private void updateColor() {
        double dist = Math.sqrt(hsX * hsX + hsY * hsY);

        double hue = Math.atan2(hsY, hsX) / (Math.PI * 2.0);
        double sat = dist / 64.0;
        double lum = 1.0 - v / 128.0;
        int newColor = Color.HSBtoRGB((float) hue, (float) sat, (float) lum);

        if (newColor != color) {
            color = newColor;
            colorFullBrightness = Color.HSBtoRGB((float) hue, (float) sat, 1.f);
            callbackColorUpdated.accept(this);
        }
    }
}
