package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class TextBox extends TextFieldWidget {
    protected final FontRenderer font;
    private TextBoxResponder responder;
    private final String defaultText;
    private boolean centered = true;
    private int color = GuiColors.SETTINGS_TEXT_BOX_TEXT;
    private int highlightedColor = GuiColors.SETTINGS_TEXT_BOX_TEXT_HIGHLIGHT;
    private boolean frame = false;

    public TextBox(FontRenderer font, int x, int y, int width, String defaultText) {
        super(font, x, y, width, 12, StringTextComponent.EMPTY);
        this.font = font;
        this.defaultText = defaultText;
    }

    public void setResponder(TextBoxResponder responderIn) {
        responder = responderIn;
        this.setResponder((string) -> responder.updatedValue(this, string));
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    public void setColor(int color) {
        this.color = color;
        highlightedColor = color;
    }

    public void setColor(int color, int highlightedColor) {
        this.color = color;
        this.highlightedColor = highlightedColor;
    }

    public void setFrame(boolean frame) {
        this.frame = frame;
    }

    public void setValue(Object object) {
        this.setValue(object.toString());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean res = super.keyPressed(keyCode, scanCode, modifiers);

        if (isFocused() && responder != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                changeFocus(false);
            }
        }

        return res;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (isFocused()) {
            super.renderButton(matrixStack, mouseX, mouseY, partialTicks);
        } else {
            boolean hovered = (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height);

            String text = getValue();
            boolean empty = false;
            if (text.isEmpty()) {
                text = defaultText;
                empty = true;
            }

            int widthOfString = font.width(text);
            int posX = x + 4;
            if (centered) {
                posX = x - widthOfString / 2 + width / 2;
            }

            if (frame) {
                fill(matrixStack, x - 1, y - 1, x + width + 1, y + height + 1, GuiColors.SETTINGS_TEXT_BOX_BORDER);
                fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_TEXT_BOX_BG);
            }

            font.draw(matrixStack, text, posX, y + 2,
                    empty ? GuiColors.SETTINGS_TEXT_BOX_TEXT_DEFAULT : (hovered ? highlightedColor : color));
        }
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
