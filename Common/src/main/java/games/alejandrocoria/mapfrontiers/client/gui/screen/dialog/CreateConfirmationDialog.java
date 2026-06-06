package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class CreateConfirmationDialog extends ConfirmationDialog {
    private static final String CONFIRM_KEY = "mapfrontiers.create";
    private static final String CANCEL_KEY = "gui.cancel";
    private static final String CONFIRM_DONT_ASK_KEY = "mapfrontiers.create_and_dont_ask_again";

    public CreateConfirmationDialog(String titleKey, Consumer<Response> callback) {
        this(titleKey, null, callback);
    }

    public CreateConfirmationDialog(String titleKey, @Nullable String descKey, Consumer<Response> callback) {
        this(titleKey, descKey, CONFIRM_DONT_ASK_KEY, callback);
    }

    protected CreateConfirmationDialog(String titleKey, @Nullable String descKey, @Nullable String confirmDontAskKey, Consumer<Response> callback) {
        super(titleKey, descKey, CONFIRM_KEY, CANCEL_KEY, confirmDontAskKey, callback);
    }

    @Override
    protected void initScreen() {
        super.initScreen();
        confirmButton.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_CONFIRM_NORMAL, ColorConstants.SIMPLE_BUTTON_TEXT_CONFIRM_HIGHLIGHT);
        if (confirmAlternativeButton != null) {
            confirmAlternativeButton.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_CONFIRM_NORMAL, ColorConstants.SIMPLE_BUTTON_TEXT_CONFIRM_HIGHLIGHT);
        }
    }
}
