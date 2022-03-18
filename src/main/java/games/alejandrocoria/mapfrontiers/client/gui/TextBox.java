package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class TextBox extends EditBox {
    private final String defaultText;
    private TextBoxResponder responder;

    public TextBox(Font font, int x, int y, int width) {
        this(font, x, y, width, "");
    }

    public TextBox(Font font, int x, int y, int width, String defaultText) {
        super(font, x, y, width, 12, TextComponent.EMPTY);
        this.defaultText = defaultText;
        if (!StringUtils.isBlank(defaultText)) {
            setResponder((value) -> updateDefaultText());
        }
        updateDefaultText();
    }

    public void setResponder(TextBoxResponder responderIn) {
        responder = responderIn;
        if (StringUtils.isBlank(defaultText)) {
            this.setResponder((value) -> responder.updatedValue(this, value));
        } else {
            this.setResponder((value) -> {
                updateDefaultText();
                responder.updatedValue(this, value);
            });
        }
    }

    @Override
    public boolean charTyped(char c, int key) {
        boolean res = false;
        if (active && isHoveredOrFocused()) {
            res = super.charTyped(c, key);
            if (res && responder != null) {
                responder.updatedValue(this, getValue());
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
                changeFocus(false);
            }
        }

        return res;
    }

    @Override
    public void setFocused(boolean isFocusedIn) {
        if (isFocused() && !isFocusedIn && responder != null) {
            responder.lostFocus(this, getValue());
        }

        super.setFocused(isFocusedIn);
    }

    @Override
    protected void onFocusedChanged(boolean focused) {
        if (!focused && responder != null) {
            responder.lostFocus(this, getValue());
        }

        super.onFocusedChanged(focused);
    }

    @OnlyIn(Dist.CLIENT)
    public interface TextBoxResponder {
        void updatedValue(TextBox textBox, String value);

        void lostFocus(TextBox textBox, String value);
    }

    private void updateDefaultText() {
        if (getValue().isEmpty()) {
            setSuggestion(defaultText);
        } else {
            setSuggestion("");
        }
    }
}
