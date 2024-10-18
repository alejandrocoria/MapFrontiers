package games.alejandrocoria.mapfrontiers.client.gui.component.textbox;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class TextBox extends EditBox {
    private final String defaultText;
    private Consumer<String> valueChangedCallback;
    private Consumer<String> lostFocusCallback;

    public TextBox(Font font, int width) {
        this(font, width, "");
    }

    public TextBox(Font font, int width, String defaultText) {
        super(font, 0, 0, width, 12, Component.empty());
        this.defaultText = defaultText;
        if (!StringUtils.isBlank(defaultText)) {
            setResponder((value) -> updateDefaultText());
        }
        updateDefaultText();
    }

    public void setValueChangedCallback(Consumer<String> callback) {
        valueChangedCallback = callback;
        if (StringUtils.isBlank(defaultText)) {
            this.setResponder((value) -> valueChangedCallback.accept(value));
        } else {
            this.setResponder((value) -> {
                updateDefaultText();
                valueChangedCallback.accept(value);
            });
        }
    }

    public void setLostFocusCallback(Consumer<String> callback) {
        lostFocusCallback = callback;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean charTyped(char c, int key) {
        boolean res = false;
        if (active && isHoveredOrFocused()) {
            res = super.charTyped(c, key);
            if (res && valueChangedCallback != null) {
                valueChangedCallback.accept(getValue());
            }
        }

        return res;
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean res = false;
        if (active && isHoveredOrFocused()) {
            res = super.keyPressed(keyCode, scanCode, modifiers);

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                setFocused(false);
            }
        }

        return res;
    }

    @Override
    public void setFocused(boolean isFocusedIn) {
        if (isFocused() && !isFocusedIn && lostFocusCallback != null) {
            lostFocusCallback.accept(getValue());
        }

        super.setFocused(isFocusedIn);
    }

    private void updateDefaultText() {
        if (getValue().isEmpty()) {
            setSuggestion(defaultText);
        } else {
            setSuggestion("");
        }
    }
}
