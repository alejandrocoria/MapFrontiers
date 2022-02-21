package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class TextBox extends EditBox {

    private TextBoxResponder responder;

    public TextBox(Font font, int x, int y, int width) {
        super(font, x, y, width, 12, TextComponent.EMPTY);
    }

    public void setResponder(TextBoxResponder responderIn) {
        responder = responderIn;
        this.setResponder((string) -> responder.updatedValue(this, string));
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
}
