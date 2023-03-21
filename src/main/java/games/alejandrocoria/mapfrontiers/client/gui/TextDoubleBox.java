package games.alejandrocoria.mapfrontiers.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class TextDoubleBox extends EditBox {
    private double defaultValue;
    private double min;
    private double max;
    private TextDoubleBoxResponder responder;

    public TextDoubleBox(double defaultValue, double min, double max, Font font, int x, int y, int width) {
        super(font, x, y, width, 12, CommonComponents.EMPTY);
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.setValue(defaultValue);
    }

    public void setResponder(TextDoubleBoxResponder responderIn) {
        responder = responderIn;
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

        if (responder != null) {
            try {
                double current = Double.parseDouble(getValue());
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
                setFocused(false);
            }
        }

        return res;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (visible && isHovered) {
            double current;
            try {
                current = Double.parseDouble(getValue());
            } catch (Exception e) {
                current = defaultValue;
            }

            if (delta > 0) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                setValue(Math.min(current + 0.1, max));
            } else if (delta < 0) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                setValue(Math.max(current - 0.1, min));
            }

            return true;
        }

        return false;
    }

    public double clamped() {
        String text = getValue();
        if (text.length() == 0) {
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

    @OnlyIn(Dist.CLIENT)
    public interface TextDoubleBoxResponder {
        void updatedValue(TextDoubleBox textDoubleBox, double value);
    }
}
