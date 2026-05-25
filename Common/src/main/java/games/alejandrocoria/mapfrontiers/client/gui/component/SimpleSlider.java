package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.mixin.client.AbstractSliderButtonAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class SimpleSlider extends AbstractSliderButton {
    private static final int DEFAULT_HEIGHT = 15;
    private static final int HANDLE_WIDTH = 4;
    private static final int HANDLE_RANGE_PADDING = 6;
    private static final int HANDLE_X_OFFSET = 1;
    private static final int HANDLE_VERTICAL_INSET = 1;
    private static final int LABEL_Y_OFFSET = 4;

    private final Font font;
    private final int minValue;
    private final int maxValue;
    private final ValueChanged callback;
    private final ValueTextFormatter valueTextFormatter;
    private final List<Integer> discreteValues;
    private boolean dragging = false;

    private final String translationKey;

    public interface ValueChanged {
        void onChanged(int value, boolean dragging);
    }

    public interface ValueTextFormatter {
        Component format(int value);
    }

    public SimpleSlider(Font font, int width, String translationKey, int minValue, int maxValue, int initialValue, ValueChanged callback) {
        this(font, width, translationKey, minValue, maxValue, initialValue, callback, value -> Component.literal(String.valueOf(value)));
    }

    public SimpleSlider(Font font,
                        int width,
                        String translationKey,
                        int minValue,
                        int maxValue,
                        int initialValue,
                        ValueChanged callback,
                        ValueTextFormatter valueTextFormatter) {
        super(0, 0, width, DEFAULT_HEIGHT, Component.literal(String.valueOf(initialValue)), normalize(initialValue, minValue, maxValue));
        this.font = font;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.callback = callback;
        this.valueTextFormatter = valueTextFormatter;
        this.discreteValues = List.of();

        this.translationKey = translationKey;
        updateMessage();
    }

    public SimpleSlider(Font font,
                        int width,
                        String translationKey,
                        List<Integer> discreteValues,
                        int initialValue,
                        ValueChanged callback,
                        ValueTextFormatter valueTextFormatter) {
        super(0, 0, width, DEFAULT_HEIGHT, Component.literal(String.valueOf(initialValue)),
                normalize(resolveDiscreteIndex(discreteValues, initialValue), 0, discreteValues.size() - 1));
        this.font = font;
        this.minValue = 0;
        this.maxValue = discreteValues.size() - 1;
        this.callback = callback;
        this.valueTextFormatter = valueTextFormatter;
        this.discreteValues = List.copyOf(discreteValues);

        this.translationKey = translationKey;
        updateMessage();
    }

    private static double normalize(int value, int min, int max) {
        if (max <= min) {
            return 0.0;
        }
        return (value - min) / (double)(max - min);
    }

    private static int resolveDiscreteIndex(List<Integer> discreteValues, int value) {
        int index = discreteValues.indexOf(value);
        return index >= 0 ? index : 0;
    }

    private boolean usesDiscreteValues() {
        return !discreteValues.isEmpty();
    }

    private int denormalizeInternal(double value) {
        return (int) Math.round(minValue + value * (maxValue - minValue));
    }

    private int getResolvedValue() {
        int internalValue = denormalizeInternal(value);
        if (usesDiscreteValues()) {
            return discreteValues.get(internalValue);
        }
        return internalValue;
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable(translationKey, valueTextFormatter.format(getResolvedValue())));
    }

    @Override
    protected void applyValue() {
        callback.onChanged(getResolvedValue(), dragging);
    }

    public void setValue(int value) {
        int internalValue = usesDiscreteValues() ? resolveDiscreteIndex(discreteValues, value) : value;
        this.value = normalize(internalValue, minValue, maxValue);
        updateMessage();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hDelta, double vDelta) {
        if (visible && isHovered) {
            int val = denormalizeInternal(value);
            if (vDelta > 0) {
                val = Math.min(val + 1, maxValue);
            } else {
                val = Math.max(val - 1, minValue);
            }

            if (usesDiscreteValues()) {
                setValue(discreteValues.get(val));
            } else {
                setValue(val);
            }
            applyValue();

            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!usesDiscreteValues()) {
            return super.keyPressed(event);
        }

        AbstractSliderButtonAccessor accessor = (AbstractSliderButtonAccessor) this;
        if (event.isSelection()) {
            accessor.setCanChangeValue(!accessor.getCanChangeValue());
            return true;
        }

        if (!accessor.getCanChangeValue()) {
            return false;
        }

        boolean left = event.isLeft();
        boolean right = event.isRight();
        if (!left && !right) {
            return false;
        }

        int internalValue = denormalizeInternal(value);
        internalValue = left ? Math.max(internalValue - 1, minValue) : Math.min(internalValue + 1, maxValue);
        setValue(discreteValues.get(internalValue));
        return true;
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
        graphics.outline(getX(), getY(), width, height, lineColor);

        int handleX = getX() + (int)(value * (width - HANDLE_RANGE_PADDING)) + HANDLE_X_OFFSET;
        graphics.fill(handleX, getY() + HANDLE_VERTICAL_INSET, handleX + HANDLE_WIDTH,
                getY() + height - HANDLE_VERTICAL_INSET,
                isHoveredOrFocused() ? ColorConstants.SLIDER_HANDLER_FOCUSED : ColorConstants.SLIDER_HANDLER);

        graphics.centeredText(font, getMessage(), getX() + width / 2, getY() + LABEL_Y_OFFSET,
                isHovered ? ColorConstants.SIMPLE_BUTTON_TEXT_HIGHLIGHT : ColorConstants.SIMPLE_BUTTON_TEXT);
    }
}
