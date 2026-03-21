package games.alejandrocoria.mapfrontiers.client.gui.dialog;

import games.alejandrocoria.mapfrontiers.client.ChatFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.FrontierData;

import javax.annotation.Nullable;

public class AcceptFrontierCopyDialog extends ConfirmationDialog {
    private static final String titleKey = "mapfrontiers.receive_frontier_copy";
    private static final String descKey = "mapfrontiers.accept_frontier_desc";
    private static final String descReplaceKey = "mapfrontiers.accept_frontier_desc_replace";
    private static final String confirmKey = "mapfrontiers.accept_frontier";
    private static final String confirmReplaceKey = "mapfrontiers.replace";
    private static final String confirmNoReplaceKey = "mapfrontiers.accept_frontier_as_new";
    private static final String cancelKey = "gui.cancel";

    public AcceptFrontierCopyDialog(int id, FrontierData receivedFrontier, @Nullable FrontierOverlay currentFrontier) {
        super(titleKey,
                currentFrontier == null ? descKey : descReplaceKey,
                currentFrontier == null ? confirmKey : confirmReplaceKey,
                cancelKey,
                currentFrontier == null ? null : confirmNoReplaceKey,
                response -> {
                    if (response == Response.Confirm && currentFrontier == null) {
                        acceptFrontier(id, receivedFrontier, null);
                    } else if (response == Response.Confirm) {
                        acceptFrontierAndReplace(id, receivedFrontier, currentFrontier);
                    } else {
                        acceptFrontier(id, receivedFrontier, currentFrontier);
                    }
                });
    }

    @Override
    protected void initScreen() {
        super.initScreen();
        if (confirmAlternativeButton != null) {
            confirmButton.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        }
    }

    private static void acceptFrontier(int id, FrontierData receivedFrontier, @Nullable FrontierOverlay currentFrontier) {
        MapFrontiersClient.getCommandService().acceptCopiedFrontier(receivedFrontier, currentFrontier);
        ChatFrontiers.removeReceivedId(id);
    }

    private static void acceptFrontierAndReplace(int id, FrontierData receivedFrontier, FrontierOverlay currentFrontier) {
        MapFrontiersClient.getCommandService().acceptCopiedFrontierAndReplace(receivedFrontier, currentFrontier);
        ChatFrontiers.removeReceivedId(id);
    }
}
