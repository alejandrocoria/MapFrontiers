package games.alejandrocoria.mapfrontiers.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class TextIntBox extends EditBox {
    private int defaultValue;
    private int min;
    private int max;
    private TextIntBoxResponder responder;

    public TextIntBox(int defaultValue, int min, int max, Font font, int x, int y, int width) {
        super(font, x, y, width, 12, TextComponent.EMPTY);
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.setValue(defaultValue);
    }

    public void setResponder(TextIntBoxResponder responderIn) {
        responder = responderIn;
    }

    public void setHeight(int height) {
        this.height = height;
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
            this.setValue(current);
        } catch (Exception e) {
            this.setValue(currentString);
        }
    }

    public void setValue(Object object) {
        this.setValue(object.toString());

        if (responder != null) {
            try {
                int current = Integer.parseInt(getValue());
                responder.updatedValue(this, current);
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

                if (responder != null) {
                    responder.updatedValue(this, Math.max(current, min));
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

            if (responder != null && (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE)) {
                responder.updatedValue(this, clamped());
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                changeFocus(false);
            }
        }

        return res;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (visible && isHovered) {
            int current;
            try {
                current = Integer.parseInt(getValue());
            } catch (Exception e) {
                current = defaultValue;
            }

            if (delta > 0 && current < max) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                setValue(++current);
            } else if (delta < 0 && current > min) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                setValue(--current);
            }

            return true;
        }

        return false;
    }

    public int clamped() {
        String text = getValue();
        if (text.length() == 0) {
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
    public void setFocus(boolean isFocusedIn) {
        if (!isFocusedIn) {
            setValue(clamped());
        }

        super.setFocus(isFocusedIn);
    }

    @Environment(EnvType.CLIENT)
    public interface TextIntBoxResponder {
        void updatedValue(TextIntBox textIntBox, int value);
    }
}
