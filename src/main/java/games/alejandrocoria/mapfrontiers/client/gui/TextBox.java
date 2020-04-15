package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class TextBox extends GuiTextField {
    private final FontRenderer fontRenderer;
    private TextBoxResponder responder;
    private String defaultText;

    public TextBox(int componentId, FontRenderer fontRenderer, int x, int y, int width, String defaultText) {
        super(componentId, fontRenderer, x, y, width, 12);
        this.fontRenderer = fontRenderer;
        this.defaultText = defaultText;
    }

    public void setResponder(TextBoxResponder responderIn) {
        responder = responderIn;
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

        if (responder != null) {
            responder.updatedValue(getId(), getText());
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
            fontRenderer.drawString(text, x - widthOfString / 2 + width / 2, y + 2, empty ? 0xbbbbbb : 0);
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