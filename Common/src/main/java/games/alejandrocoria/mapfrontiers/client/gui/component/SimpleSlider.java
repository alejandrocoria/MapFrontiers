package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.mixin.AbstractSliderButtonAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SimpleSlider extends AbstractSliderButton
{
    private final Font font;
    private final int minValue;
    private final int maxValue;
    private final ValueChanged callback;
    private boolean dragging = false;

    private final String translationKey;

    public interface ValueChanged {
        void onChanged(int value, boolean dragging);
    }

    public SimpleSlider(Font font, int width, String translationKey, int minValue, int maxValue, int initialValue, ValueChanged callback) {
        super(0, 0, width, 16, Component.literal(String.valueOf(initialValue)), normalize(initialValue, minValue, maxValue));
        this.font = font;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.callback = callback;

        this.translationKey = translationKey;
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
        setMessage(Component.translatable(translationKey, denormalize(value)));
    }

    @Override
    protected void applyValue() {
        int val = denormalize(value);
        callback.onChanged(val, dragging);
    }

    public void setValue(int value) {
        this.value = normalize(value, minValue, maxValue);
        updateMessage();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hDelta, double vDelta) {
        if (visible && isHovered) {
            int val = denormalize(value);
            if (vDelta > 0) {
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
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        dragging = true;
        super.onDrag(event, dragX, dragY);
    }

    // Custom mouseReleased to be called from the Screen.
    public void mouseReleased() {
        if (dragging) {
            dragging = false;
            applyValue();
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int lineColor = ((AbstractSliderButtonAccessor) this).getCanChangeValue() ? ColorConstants.SIMPLE_BUTTON_BORDER_FOCUSED : ColorConstants.SIMPLE_BUTTON_BORDER;
        graphics.horizontalLine(getX(), getX() + width - 1, getY(), lineColor);
        graphics.horizontalLine(getX(), getX() + width - 1, getY() + height - 1, lineColor);
        graphics.verticalLine(getX(), getY(), getY() + height - 1, lineColor);
        graphics.verticalLine(getX() + width - 1, getY(), getY() + height - 1, lineColor);

        int handleX = getX() + (int)(value * (width - 6)) + 1;
        graphics.fill(handleX, getY() + 1, handleX + 4, getY() + height - 1, isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFAAAAAA);

        graphics.centeredText(font, getMessage(), getX() + width / 2, getY() + 5, isHovered ? ColorConstants.SIMPLE_BUTTON_TEXT_HIGHLIGHT : ColorConstants.SIMPLE_BUTTON_TEXT);
    }
}
