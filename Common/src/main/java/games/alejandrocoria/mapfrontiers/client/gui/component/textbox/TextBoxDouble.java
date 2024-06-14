package games.alejandrocoria.mapfrontiers.client.gui.component.textbox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.DoubleConsumer;

@ParametersAreNonnullByDefault
public class TextBoxDouble extends EditBox {
    private final double defaultValue;
    private final double min;
    private final double max;
    private DoubleConsumer valueChangedCallback;

    public TextBoxDouble(double defaultValue, double min, double max, Font font, int x, int y, int width) {
        super(font, x, y, width, 12, Component.empty());
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.setValue(defaultValue);
    }

    public void setValueChangedCallback(DoubleConsumer callback) {
        valueChangedCallback = callback;
    }

    @Override
    public void insertText(String textToWrite) {
        if (textToWrite.equals("-") && getCursorPosition() == 0) {
            super.insertText(textToWrite);
            return;
        }

        if (textToWrite.equals("0")) {
            super.insertText(textToWrite);
            return;
        }

        if (textToWrite.equals(".")) {
            super.insertText(textToWrite);
            return;
        }

        String currentString = getValue();
        super.insertText(textToWrite);

        try {
            double current = Double.parseDouble(getValue());
            this.setValue(current);
        } catch (Exception e) {
            this.setValue(currentString);
        }
    }

    public void setValue(Object object) {
        this.setValue(object.toString());

        if (valueChangedCallback != null) {
            try {
                double current = Double.parseDouble(getValue());
                valueChangedCallback.accept(current);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean charTyped(char c, int key) {
        boolean res = false;
        if (isHoveredOrFocused()) {
            res = super.charTyped(c, key);
            if (res) {
                double current;
                try {
                    current = Double.parseDouble(getValue());
                    if (current > max) {
                        setValue(max);
                        current = max;
                    }
                } catch (Exception e) {
                    return true;
                } finally {
                    moveCursorToStart(false);
                    moveCursorToEnd(false);
                }

                if (valueChangedCallback != null) {
                    valueChangedCallback.accept(Math.max(current, min));
                }
            }
        }

        return res;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean res = false;
        if (isHoveredOrFocused()) {
            res = super.keyPressed(keyCode, scanCode, modifiers);

            if (valueChangedCallback != null && (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE)) {
                valueChangedCallback.accept(clamped());
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                setFocused(false);
            }
        }

        return res;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hDelta, double vDelta) {
        if (visible && isHovered) {
            double current;
            try {
                current = Double.parseDouble(getValue());
            } catch (Exception e) {
                current = defaultValue;
            }

            if (vDelta > 0) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                setValue(Math.min(current + 0.1, max));
            } else if (vDelta < 0) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                setValue(Math.max(current - 0.1, min));
            }

            return true;
        }

        return false;
    }

    public double clamped() {
        String text = getValue();
        if (text.isEmpty()) {
            return defaultValue;
        }

        double current;

        try {
            current = Double.parseDouble(text);
        } catch (Exception e) {
            return defaultValue;
        }

        current = Math.max(min, current);
        current = Math.min(max, current);

        return current;
    }

    @Override
    public void setFocused(boolean isFocusedIn) {
        if (!isFocusedIn) {
            setValue(clamped());
        }

        super.setFocused(isFocusedIn);
    }
}
