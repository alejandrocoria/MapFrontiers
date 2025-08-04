package games.alejandrocoria.mapfrontiers.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class SimpleSlider extends AbstractSliderButton
{
    private final Font font;
    private final int minValue;
    private final int maxValue;
    private final ValueChanged callback;

    private String translationKey;

    private SimpleLabel label;

    private int textColor = ColorConstants.SIMPLE_BUTTON_TEXT;

    private int textColorHighlight = ColorConstants.SIMPLE_BUTTON_TEXT_HIGHLIGHT;

    public interface ValueChanged {
        void onChanged(int value);
    }

    public SimpleSlider(Font font, int x, int y, int width, String translationKey, int minValue, int maxValue, int initialValue, ValueChanged callback) {
        super(x, y, width, 16, Component.literal(String.valueOf(initialValue)), normalize(initialValue, minValue, maxValue));
        this.font = font;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.callback = callback;

        this.translationKey = translationKey;
        this.label = new SimpleLabel(font, getX() + width / 2, getY() + 5, SimpleLabel.Align.Center, Component.translatable(translationKey, denormalize(value)), textColor);
        updateMessage();
    }

    private static double normalize(int value, int min, int max) {
        return (value - min) / (double)(max - min);
    }

    private int denormalize(double value) {
        return (int) Math.round(minValue + value * (maxValue - minValue));
    }

    @Override
    protected void updateMessage() {
        int val = denormalize(value);
        label.setText(Component.translatable(translationKey, val));
    }

    @Override
    protected void applyValue() {
        int val = denormalize(value);
        callback.onChanged(val);
    }

    public void setValue(int value) {
        this.value = normalize(value, minValue, maxValue);
        updateMessage();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (visible && isHovered) {
            int val = denormalize(value);
            if (delta > 0) {
                val = Math.min(val + 1, maxValue);
            } else {
                val = Math.max(val - 1, minValue);
            }

            setValue(val);
            applyValue();

            return true;
        }

        return false;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isHovered) {
            label.setColor(textColorHighlight);
        } else {
            label.setColor(textColor);
        }

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

        graphics.hLine(getX(), getX() + width, getY(), ColorConstants.SIMPLE_BUTTON_BORDER);
        graphics.hLine(getX(), getX() + width, getY() + 16, ColorConstants.SIMPLE_BUTTON_BORDER);
        graphics.vLine(getX(), getY(), getY() + 16, ColorConstants.SIMPLE_BUTTON_BORDER);
        graphics.vLine(getX() + width, getY(), getY() + 16, ColorConstants.SIMPLE_BUTTON_BORDER);

        int handleX = getX() + (int)(value * (width - 5)) + 1;
        graphics.fill(handleX, getY() + 1, handleX + 4, getY() + 16, isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFAAAAAA);

        label.render(graphics, mouseX, mouseY, partialTick);
    }
}
