package games.alejandrocoria.mapfrontiers.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class TextIntBox extends EditBox {
    private static final String numericRegex = "[^\\d]";

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

    @Override
    public void insertText(String textToWrite) {
        super.insertText(textToWrite);
        String fixed = getValue().replaceAll(numericRegex, "");
        this.setValue(fixed);
    }

    public void setValue(Object object) {
        this.setValue(object.toString());

        if (responder != null) {
            responder.updatedValue(this, Integer.parseInt(getValue()));
        }
    }

    @Override
    public boolean charTyped(char c, int key) {
        boolean res = false;
        if (isHoveredOrFocused()) {
            res = super.charTyped(c, key);
            if (res) {
                Integer integer = clamped();
                setValue(integer);

                moveCursorToStart();
                moveCursorToEnd();

                if (responder != null) {
                    responder.updatedValue(this, integer);
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

    public Integer clamped() {
        String text = getValue();
        if (text.length() == 0) {
            return defaultValue;
        }

        int integer;

        try {
            integer = Integer.parseInt(text);
        } catch (Exception e) {
            return defaultValue;
        }

        integer = Math.max(min, integer);
        integer = Math.min(max, integer);

        return integer;
    }

    @OnlyIn(Dist.CLIENT)
    public interface TextIntBoxResponder {
        void updatedValue(TextIntBox textIntBox, int value);
    }
}
