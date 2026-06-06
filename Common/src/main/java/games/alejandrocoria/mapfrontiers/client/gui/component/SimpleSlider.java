package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.mixin.client.AbstractSliderButtonAccessor;
import net.minecraft.client.Minecraft;
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
    private static final int DEFAULT_HEIGHT = 13;
    private static final int HANDLE_WIDTH = 4;
    private static final int TRACK_INSET = 1;
    private static final int HANDLE_VERTICAL_INSET = 1;
    private static final int LABEL_Y_OFFSET = -5;

    private final int minValue;
    private final int maxValue;
    private final ValueChanged callback;
    private final ValueTextFormatter valueTextFormatter;
    private final List<Integer> discreteValues;
    private final StringWidget label;
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
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.callback = callback;
        this.valueTextFormatter = valueTextFormatter;
        this.discreteValues = List.of();
        this.label = new StringWidget(Component.empty(), font, StringWidget.Align.Center);

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
                normalizeDiscreteIndex(resolveDiscreteIndex(discreteValues, initialValue), discreteValues.size()));
        this.minValue = 0;
        this.maxValue = discreteValues.size() - 1;
        this.callback = callback;
        this.valueTextFormatter = valueTextFormatter;
        this.discreteValues = List.copyOf(discreteValues);
        this.label = new StringWidget(Component.empty(), font, StringWidget.Align.Center);

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

    private static double normalizeDiscreteIndex(int index, int stepCount) {
        if (stepCount <= 1) {
            return 0.5;
        }

        return (index + 0.5) / (double) stepCount;
    }

    private int resolveDiscreteStep(double value) {
        int stepCount = discreteValues.size();
        if (stepCount <= 1) {
            return 0;
        }

        return Math.clamp((int) Math.floor(value * stepCount), 0, stepCount - 1);
    }

    private double snapNormalizedValue(double value) {
        if (!usesDiscreteValues()) {
            return value;
        }

        return normalizeDiscreteIndex(resolveDiscreteStep(value), discreteValues.size());
    }

    private void snapHandleToDiscreteStep() {
        value = snapNormalizedValue(value);
    }

    private int getTrackStartX() {
        return getX() + TRACK_INSET;
    }

    private int getTrackWidth() {
        return Math.max(0, width - TRACK_INSET * 2);
    }

    private int getContinuousHandleX(double value) {
        return getTrackStartX() + (int) (value * Math.max(0, getTrackWidth() - HANDLE_WIDTH));
    }

    private int getDiscreteBoundaryX(int lowerStepIndex, int lastStepIndex) {
        int stepCount = lastStepIndex + 1;
        return getTrackStartX() + (int) Math.round((lowerStepIndex + 1) * (getTrackWidth() / (double) stepCount));
    }

    private int denormalizeInternal(double value) {
        return (int) Math.round(minValue + value * (maxValue - minValue));
    }

    private int getResolvedValue() {
        int internalValue = usesDiscreteValues() ? resolveDiscreteStep(value) : denormalizeInternal(value);
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
    public void setX(int x) {
        super.setX(x);
        if (label != null) {
            label.setX(x + width / 2);
        }
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        if (label != null) {
            label.setY(y + height / 2 + LABEL_Y_OFFSET);
        }
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        if (label != null) {
            label.setMessage(message);
            label.setX(getX() + width / 2);
        }
    }

    @Override
    protected void applyValue() {
        snapHandleToDiscreteStep();
        callback.onChanged(getResolvedValue(), dragging);
    }

    public void setValue(int value) {
        int internalValue = usesDiscreteValues() ? resolveDiscreteIndex(discreteValues, value) : value;
        this.value = usesDiscreteValues()
                ? normalizeDiscreteIndex(internalValue, discreteValues.size())
                : normalize(internalValue, minValue, maxValue);
        updateMessage();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hDelta, double vDelta) {
        if (visible && active && isHovered) {
            int val = usesDiscreteValues() ? resolveDiscreteStep(value) : denormalizeInternal(value);
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
        if (!visible || !active) {
            return false;
        }

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

        int internalValue = resolveDiscreteStep(value);
        internalValue = left ? Math.max(internalValue - 1, minValue) : Math.min(internalValue + 1, maxValue);
        setValue(discreteValues.get(internalValue));
        return true;
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        dragging = true;
        super.onDrag(event, dragX, dragY);
        snapHandleToDiscreteStep();
    }

    // Custom mouseReleased to be called from the Screen.
    public void mouseReleased() {
        if (dragging) {
            dragging = false;
            applyValue();
        }
    }

    private boolean isKeyboardFocused() {
        return isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        AbstractSliderButtonAccessor accessor = (AbstractSliderButtonAccessor) this;
        boolean canChangeValue = accessor.getCanChangeValue();
        boolean keyboardFocused = isKeyboardFocused();

        int lineColor = !active ? ColorConstants.SLIDER_BORDER_DISABLED
                : keyboardFocused ? ColorConstants.SLIDER_BORDER_FOCUSED : ColorConstants.SLIDER_BORDER_NORMAL;
        graphics.outline(getX(), getY(), width, height, lineColor);

        int handleColor;
        if (!active) {
            handleColor = ColorConstants.SLIDER_HANDLER_DISABLED;
        } else if (isHovered || (keyboardFocused && canChangeValue)) {
            handleColor = ColorConstants.SLIDER_HANDLER_FOCUSED;
        } else {
            handleColor = ColorConstants.SLIDER_HANDLER_NORMAL;
        }

        if (usesDiscreteValues()) {
            int stepIndex = resolveDiscreteStep(snapNormalizedValue(value));
            int lastStepIndex = discreteValues.size() - 1;
            int handleX;
            int handleRight;

            if (lastStepIndex <= 0) {
                handleX = getTrackStartX();
                handleRight = getTrackStartX() + getTrackWidth();
            } else {
                handleX = stepIndex == 0 ? getTrackStartX() : getDiscreteBoundaryX(stepIndex - 1, lastStepIndex);
                handleRight = stepIndex == lastStepIndex
                        ? getTrackStartX() + getTrackWidth()
                        : getDiscreteBoundaryX(stepIndex, lastStepIndex);
            }

            graphics.fill(handleX, getY() + HANDLE_VERTICAL_INSET, handleRight,
                    getY() + height - HANDLE_VERTICAL_INSET, handleColor);
        } else {
            int handleX = getContinuousHandleX(value);
            graphics.fill(handleX, getY() + HANDLE_VERTICAL_INSET, handleX + HANDLE_WIDTH,
                    getY() + height - HANDLE_VERTICAL_INSET, handleColor);
        }

        label.setColor(!active ? ColorConstants.SLIDER_TEXT_DISABLED
                : isHovered || keyboardFocused ? ColorConstants.SLIDER_TEXT_HIGHLIGHT : ColorConstants.SLIDER_TEXT_NORMAL);
        label.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
