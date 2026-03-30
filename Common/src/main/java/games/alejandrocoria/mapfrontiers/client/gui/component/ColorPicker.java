package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class ColorPicker extends AbstractWidgetNoNarration {
    private static final Identifier texture = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/color_picker.png");
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

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        hsGrabbed = false;
        vGrabbed = false;

        updateMouse(event.x(), event.y(), false);

        return true;
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        updateMouse(event.x(), event.y(), false);
        hsGrabbed = false;
        vGrabbed = false;
    }

    @Override
    public void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        updateMouse(event.x(), event.y(), true);
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    public void finishSelection() {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        callbackColorUpdated.accept(color, false);
        hsGrabbed = false;
        vGrabbed = false;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int texX = active ? 0 : 137;

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), texX, 0, 128, 128, textureSizeX, textureSizeY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX() + 132, getY(), texX + 129, 0, 8, 128, textureSizeX, textureSizeY, active ? colorFullBrightness : 0xFFFFFFFF);
        graphics.fill(getX() + (int) hsX + 64, getY() + (int) hsY + 64, getX() + (int) hsX + 65, getY() + (int) hsY + 65, active ? 0xFFFFFFFF : colorFullBrightness);
        graphics.fill(getX() + 131, getY() + (int) v, getX() + 139, getY() + (int) v + 1, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX() + (int) hsX + 64 - 2, getY() + (int) hsY + 64 - 2, texX, 129, 5, 5, textureSizeX, textureSizeY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX() + 131, getY() + (int) v - 2, texX + 6, 129, 10, 5, textureSizeX, textureSizeY);
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
