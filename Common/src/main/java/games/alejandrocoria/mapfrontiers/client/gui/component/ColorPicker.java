package games.alejandrocoria.mapfrontiers.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class ColorPicker extends AbstractWidgetNoNarration {
    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/color_picker.png");
    private static final int textureSizeX = 274;
    private static final int textureSizeY = 134;

    private double hsX;
    private double hsY;
    private double v;
    private int color;
    private int colorFullBrightness;
    private boolean hsGrabbed = false;
    private boolean vGrabbed = false;
    private final BiConsumer<Integer, Boolean> callbackColorUpdated;

    public ColorPicker(int color, BiConsumer<Integer, Boolean> callbackColorUpdated) {
        super(0, 0, 141, 128, Component.empty());
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
    public void onClick(double mouseX, double mouseY) {
        hsGrabbed = false;
        vGrabbed = false;

        updateMouse(mouseX, mouseY, false);
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
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

        int texX = active ? 0 : 137;

        graphics.blit(texture, getX(), getY(), texX, 0, 128, 128, textureSizeX, textureSizeY);
        if (active) {
            RenderSystem.setShaderColor(((colorFullBrightness & 0xff0000) >> 16) / 255.f, ((colorFullBrightness & 0x00ff00) >> 8) / 255.f, (colorFullBrightness & 0x0000ff) / 255.f, 1.f);
        }
        graphics.blit(texture, getX() + 132, getY(), texX + 129, 0, 8, 128, textureSizeX, textureSizeY);
        if (!active) {
            RenderSystem.setShaderColor(((colorFullBrightness & 0xff0000) >> 16) / 255.f, ((colorFullBrightness & 0x00ff00) >> 8) / 255.f, (colorFullBrightness & 0x0000ff) / 255.f, 1.f);
        }
        graphics.fill(getX() + (int) hsX + 64, getY() + (int) hsY + 64, getX() + (int) hsX + 65, getY() + (int) hsY + 65, colorFullBrightness);
        graphics.fill(getX() + 131, getY() + (int) v, getX() + 139, getY() + (int) v + 1, color);
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        graphics.blit(texture, getX() + (int) hsX + 64 - 2, getY() + (int) hsY + 64 - 2, texX, 129, 5, 5, textureSizeX, textureSizeY);
        graphics.blit(texture, getX() + 131, getY() + (int) v - 2, texX + 6, 129, 10, 5, textureSizeX, textureSizeY);
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
        callbackColorUpdated.accept(color, dragging);
    }
}
