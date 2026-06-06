package games.alejandrocoria.mapfrontiers.client.gui.component.textbox;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class TextBoxIdentifier extends TextBox {
    private @Nullable Identifier parsedValue;
    private boolean invalid = false;
    private boolean settingIdentifier = false;
    private @Nullable Consumer<String> externalValueChangedCallback;

    public TextBoxIdentifier(Font font, int width) {
        super(font, width);
        super.setValueChangedCallback(this::onValueChanged);
    }

    public void setIdentifier(Identifier identifier) {
        parsedValue = identifier;
        invalid = false;
        settingIdentifier = true;
        try {
            setValue(identifier.toString());
        } finally {
            settingIdentifier = false;
        }
    }

    public @Nullable Identifier getParsedValue() {
        return parsedValue;
    }

    public boolean isInvalid() {
        return invalid;
    }

    @Override
    public void setValueChangedCallback(Consumer<String> callback) {
        externalValueChangedCallback = callback;
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
    }

    private void onValueChanged(String value) {
        validateIdentifier(value);
        if (!settingIdentifier && externalValueChangedCallback != null) {
            externalValueChangedCallback.accept(value);
        }
    }

    private void validateIdentifier(String value) {
        try {
            parsedValue = Identifier.parse(value);
            invalid = false;
        } catch (Exception ignored) {
            parsedValue = null;
            invalid = true;
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        setTextColor(invalid ? ColorConstants.TEXT_ERROR_NORMAL : ColorConstants.TEXTBOX_TEXT);
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
    }
}
