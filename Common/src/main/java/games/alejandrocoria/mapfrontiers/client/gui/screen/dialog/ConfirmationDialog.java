package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ConfirmationDialog extends AutoScaledScreen {
    public enum Response {
        Confirm,
        ConfirmAlternative
    }

    private final String titleKey;
    @Nullable
    private final String textKey;
    private final String confirmKey;
    private final String cancelKey;
    @Nullable
    private final String confirmAlternativeKey;
    private final Consumer<Response> callback;

    protected SimpleButton confirmButton;
    protected SimpleButton cancelButton;
    @Nullable
    protected SimpleButton confirmAlternativeButton;

    public ConfirmationDialog(String titleKey, @Nullable String textKey, String confirmKey, String cancelKey, @Nullable String confirmAlternativeKey, Consumer<Response> callback) {
        super(Component.empty());
        this.titleKey = titleKey;
        this.textKey = textKey;
        this.confirmKey = confirmKey;
        this.cancelKey = cancelKey;
        this.confirmAlternativeKey = confirmAlternativeKey;
        this.callback = callback;
    }

    @Override
    protected void initScreen() {
        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        mainLayout.addChild(new StringWidget(Component.translatable(titleKey).withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.WHITE));

        if (textKey != null) {
            mainLayout.addChild(new MultiLineTextWidget(Component.translatable(textKey), font).setCentered(true));
        }

        GridLayout buttons = new GridLayout().spacing(4);
        mainLayout.addChild(buttons);

        confirmButton = buttons.addChild(new SimpleButton(font, 150, Component.translatable(confirmKey), (b) -> {
            onClose();
            callback.accept(Response.Confirm);
        }), 0, 0);
        cancelButton = buttons.addChild(new SimpleButton(font, 150, Component.translatable(cancelKey), (b) -> onClose()), 0, 1);

        if (confirmAlternativeKey != null) {
            confirmAlternativeButton = buttons.addChild(new SimpleButton(font, 304, Component.translatable(confirmAlternativeKey), (b) -> {
                onClose();
                callback.accept(Response.ConfirmAlternative);
            }), 1, 0, 1, 2);
        }
    }

    @Override
    protected void setInitialFocus() {
        if (minecraft.getLastInputType().isKeyboard()) {
            setInitialFocus(cancelButton);
        }
    }

    @Override
    protected void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
    }
}
