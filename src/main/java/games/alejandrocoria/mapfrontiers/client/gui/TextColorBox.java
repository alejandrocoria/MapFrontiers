package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class TextColorBox extends TextFieldWidget {
    private static final String numericRegex = "[^\\d]";
    private TextColorBoxResponder responder;

    public TextColorBox(int value, FontRenderer font, int x, int y) {
        super(font, x, y, 26, 12, StringTextComponent.EMPTY);
        this.setValue(String.valueOf(value));
    }

    public void setResponder(TextColorBoxResponder responderIn) {
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean res = super.keyPressed(keyCode, scanCode, modifiers);

        Integer integer = clamped();
        setValue(integer);

        moveCursorToStart();
        moveCursorToEnd();

        if (responder != null) {
            responder.updatedValue(this, integer);
        }

        return res;
    }

    public Integer clamped() {
        String text = getValue();
        if (text.length() == 0) {
            return 0;
        }

        int integer;

        try {
            integer = Integer.parseInt(text);
        } catch (Exception e) {
            return 0;
        }

        integer = Math.max(0, integer);
        integer = Math.min(255, integer);

        return integer;
    }

    @OnlyIn(Dist.CLIENT)
    public interface TextColorBoxResponder {
        void updatedValue(TextColorBox textColorBox, int value);
    }
}
