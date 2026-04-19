package games.alejandrocoria.mapfrontiers.client.gui.component.textbox;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
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
    private Consumer<String> submitCallback;

    public TextBox(Font font, int width) {
        this(font, width, "");
    }

    public TextBox(Font font, int width, String defaultText) {
        super(font, 0, 0, width, 13, Component.empty());
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

    public void setSubmitCallback(Consumer<String> callback) {
        submitCallback = callback;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        boolean res = false;
        if (active && isHoveredOrFocused()) {
            res = super.charTyped(event);
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
    public boolean keyPressed(KeyEvent event) {
        boolean res = false;
        if (active && isHoveredOrFocused()) {
            res = super.keyPressed(event);

            if (event.input() == GLFW.GLFW_KEY_ENTER || event.input() == GLFW.GLFW_KEY_KP_ENTER) {
                setFocused(false);
                if (submitCallback != null) {
                    submitCallback.accept(getValue());
                }
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
