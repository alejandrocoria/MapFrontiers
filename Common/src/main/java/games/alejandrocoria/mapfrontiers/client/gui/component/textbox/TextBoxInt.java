package games.alejandrocoria.mapfrontiers.client.gui.component.textbox;

import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntConsumer;

@ParametersAreNonnullByDefault
public class TextBoxInt extends EditBox {
    private static final int DEFAULT_HEIGHT = 13;

    private int defaultValue;
    private int min;
    private int max;
    private IntConsumer valueChangedCallback;

    public TextBoxInt(int defaultValue, int min, int max, Font font, int width) {
        super(font, 0, 0, width, DEFAULT_HEIGHT, Component.empty());
        setRange(defaultValue, min, max);
        this.setValue(defaultValue);
    }

    public TextBoxInt(IntConfigEntry entry, Font font, int width) {
        this(entry.defaultValue(), entry.minValue(), entry.maxValue(), font, width);
    }

    public void setRange(IntConfigEntry entry) {
        setRange(entry.defaultValue(), entry.minValue(), entry.maxValue());
    }

    public void setRange(int defaultValue, int min, int max) {
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    public void setValueChangedCallback(IntConsumer callback) {
        valueChangedCallback = callback;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        active = editable;
        if (!editable) {
            setFocused(false);
        }
    }

    @Override
    public void insertText(String textToWrite) {
        if (textToWrite.equals("-") && getCursorPosition() == 0) {
            super.insertText(textToWrite);
            return;
        }

        String currentString = getValue();
        super.insertText(textToWrite);

        try {
            int current = Integer.parseInt(getValue());
            current = Mth.clamp(current, min, max);
            this.setValue(current);
        } catch (Exception e) {
            this.setValue(currentString);
        }
    }

    public void setValue(Object object) {
        this.setValue(object.toString());

        if (valueChangedCallback != null) {
            try {
                int current = Integer.parseInt(getValue());
                valueChangedCallback.accept(current);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean res = false;
        if (active && isHoveredOrFocused()) {
            res = super.charTyped(codePoint, modifiers);
            if (res) {
                int current;
                try {
                    current = Integer.parseInt(getValue());
                    if (current > max) {
                        setValue(max);
                        current = max;
                    }
                } catch (Exception e) {
                    return true;
                } finally {
                    moveCursorToStart();
                    moveCursorToEnd();
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
        if (active && isHoveredOrFocused()) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double vDelta) {
        if (visible && active && isHovered) {
            int current;
            try {
                current = Integer.parseInt(getValue());
            } catch (Exception e) {
                current = defaultValue;
            }

            if (vDelta > 0 && current < max) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                setValue(++current);
            } else if (vDelta < 0 && current > min) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                setValue(--current);
            }

            return true;
        }

        return false;
    }

    public int clamped() {
        String text = getValue();
        if (text.isEmpty()) {
            return defaultValue;
        }

        int current;

        try {
            current = Integer.parseInt(text);
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
