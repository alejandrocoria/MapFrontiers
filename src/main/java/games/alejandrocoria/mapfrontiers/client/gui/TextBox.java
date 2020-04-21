package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class TextBox extends GuiTextField {
    protected final FontRenderer fontRenderer;
    private TextBoxResponder responder;
    private String defaultText;
    private boolean centered = true;
    private int color = 0;
    private boolean frame = false;

    public TextBox(int componentId, FontRenderer fontRenderer, int x, int y, int width, String defaultText) {
        super(componentId, fontRenderer, x, y, width, 12);
        this.fontRenderer = fontRenderer;
        this.defaultText = defaultText;
    }

    public void setResponder(TextBoxResponder responderIn) {
        responder = responderIn;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setFrame(boolean frame) {
        this.frame = frame;
    }

    @Override
    public void writeText(String textToWrite) {
        super.writeText(textToWrite);
    }

    public void setText(Object object) {
        this.setText(object.toString());

        if (responder != null) {
            responder.updatedValue(getId(), getText());
        }
    }

    @Override
    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        boolean res = super.textboxKeyTyped(typedChar, keyCode);

        if (isFocused() && responder != null) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                setFocused(false);
            } else {
                responder.updatedValue(getId(), getText());
            }
        }

        return res;
    }

    @Override
    public void drawTextBox() {
        if (isFocused()) {
            super.drawTextBox();
        } else {
            String text = getText();
            boolean empty = false;
            if (text.isEmpty()) {
                text = defaultText;
                empty = true;
            }

            int widthOfString = fontRenderer.getStringWidth(text);
            int posX = x + 4;
            if (centered) {
                posX = x - widthOfString / 2 + width / 2;
            }

            if (frame) {
                Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0xff444444);
                Gui.drawRect(x, y, x + width, y + height, 0xff000000);
            }

            fontRenderer.drawString(text, posX, y + 2, empty ? 0xbbbbbb : color);
        }
    }

    @Override
    public void setFocused(boolean isFocusedIn) {
        boolean lostFocus = false;
        if (!isFocusedIn && isFocused()) {
            lostFocus = true;
        }

        super.setFocused(isFocusedIn);

        if (lostFocus && responder != null) {
            responder.lostFocus(getId(), getText());
        }
    }

    @SideOnly(Side.CLIENT)
    public interface TextBoxResponder {
        public void updatedValue(int id, String value);

        public void lostFocus(int id, String value);
    }
}