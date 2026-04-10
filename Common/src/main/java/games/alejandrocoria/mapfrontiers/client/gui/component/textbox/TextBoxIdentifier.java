package games.alejandrocoria.mapfrontiers.client.gui.component.textbox;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class TextBoxIdentifier extends TextBox {
    private final Font textFont;
    private @Nullable Component error;

    public TextBoxIdentifier(Font font, int width) {
        super(font, width);
        textFont = font;
    }

    public void setError(@Nullable Component error) {
        this.error = error;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        setTextColor(error == null ? ColorConstants.TEXTBOX_TEXT : ColorConstants.TEXT_ERROR);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);

        if (error != null && !error.getString().isEmpty()) {
            List<FormattedCharSequence> errorList = textFont.split(error, width - 8);
            int maxErrorWidth = width - 8;
            graphics.fill(getX() - 1, getY() - errorList.size() * 12 - 5, getX() + maxErrorWidth + 9, getY() - 1,
                    ColorConstants.TEXTBOX_EXTRA_BORDER);
            graphics.fill(getX(), getY() - errorList.size() * 12 - 4, getX() + maxErrorWidth + 8, getY() - 1,
                    ColorConstants.TEXTBOX_EXTRA_BG);

            int posX = getX() + 4;
            int posY = getY() - errorList.size() * 12;
            for (FormattedCharSequence sequence : errorList) {
                graphics.text(textFont, sequence, posX, posY, ColorConstants.TEXT_HIGHLIGHT);
                posY += 12;
            }
        }
    }
}
