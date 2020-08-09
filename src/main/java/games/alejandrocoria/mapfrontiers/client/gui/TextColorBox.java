package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class TextColorBox extends GuiTextField {
    private static final String numericRegex = "[^\\d]";
    private TextColorBoxResponder responder;

    public TextColorBox(int componentId, int value, FontRenderer fontRenderer, int x, int y) {
        super(componentId, fontRenderer, x, y, 26, 12);
        this.setText(String.valueOf(value));
    }

    public void setResponder(TextColorBoxResponder responderIn) {
        responder = responderIn;
    }

    @Override
    public void writeText(String textToWrite) {
        super.writeText(textToWrite);
        String fixed = getText().replaceAll(numericRegex, "");
        this.setText(fixed);
    }

    public void setText(Object object) {
        this.setText(object.toString());

        if (responder != null) {
            responder.updatedValue(getId(), Integer.valueOf(getText()));
        }
    }

    @Override
    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        boolean res = super.textboxKeyTyped(typedChar, keyCode);

        Integer integer = clamped();
        setText(integer);

        setCursorPositionZero();
        setCursorPositionEnd();

        if (responder != null) {
            responder.updatedValue(getId(), integer);
        }

        return res;
    }

    public Integer clamped() {
        String text = getText();
        if (text == null || text.length() == 0) {
            return Integer.valueOf(0);
        }

        Integer integer = Integer.valueOf(0);

        try {
            integer = Integer.parseInt(text);
        } catch (Exception e) {
            return Integer.valueOf(0);
        }

        integer = Math.max(0, integer);
        integer = Math.min(255, integer);

        return integer;
    }

    @SideOnly(Side.CLIENT)
    public interface TextColorBoxResponder {
        public void updatedValue(int id, int value);
    }
}