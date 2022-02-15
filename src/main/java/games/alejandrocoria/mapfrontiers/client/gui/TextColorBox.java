package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class TextColorBox extends EditBox {
    private static final String numericRegex = "[^\\d]";
    private TextColorBoxResponder responder;

    public TextColorBox(int value, Font font, int x, int y) {
        super(font, x, y, 29, 12, TextComponent.EMPTY);
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
