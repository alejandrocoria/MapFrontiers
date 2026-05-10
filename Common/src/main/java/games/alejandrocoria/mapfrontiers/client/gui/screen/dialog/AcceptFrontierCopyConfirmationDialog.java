package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.ChatFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;

import javax.annotation.Nullable;

public class AcceptFrontierCopyConfirmationDialog extends ConfirmationDialog {
    private static final String TITLE_KEY = "mapfrontiers.receive_frontier_copy";
    private static final String DESC_KEY = "mapfrontiers.accept_frontier_desc";
    private static final String DESC_REPLACE_KEY = "mapfrontiers.accept_frontier_desc_replace";
    private static final String CONFIRM_KEY = "mapfrontiers.accept_frontier";
    private static final String CONFIRM_REPLACE_KEY = "mapfrontiers.replace";
    private static final String CONFIRM_NO_REPLACE_KEY = "mapfrontiers.accept_frontier_as_new";
    private static final String CANCEL_KEY = "gui.cancel";

    public AcceptFrontierCopyConfirmationDialog(int id, ChatFrontiers.ReceivedFrontierCopy receivedCopy, @Nullable FrontierOverlay currentFrontier) {
        super(TITLE_KEY,
                currentFrontier == null ? DESC_KEY : DESC_REPLACE_KEY,
                currentFrontier == null ? CONFIRM_KEY : CONFIRM_REPLACE_KEY,
                CANCEL_KEY,
                currentFrontier == null ? null : CONFIRM_NO_REPLACE_KEY,
                response -> {
                    if (response == Response.Confirm && currentFrontier == null) {
                        acceptFrontier(id, receivedCopy, null);
                    } else if (response == Response.Confirm) {
                        acceptFrontierAndReplace(id, receivedCopy, currentFrontier);
                    } else {
                        acceptFrontier(id, receivedCopy, currentFrontier);
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

    private static void acceptFrontier(int id, ChatFrontiers.ReceivedFrontierCopy receivedCopy, @Nullable FrontierOverlay currentFrontier) {
        MapFrontiersClient.getOperationService().acceptCopiedFrontier(receivedCopy.frontier(), receivedCopy.collection(), currentFrontier);
        ChatFrontiers.removeReceivedId(id);
    }

    private static void acceptFrontierAndReplace(int id, ChatFrontiers.ReceivedFrontierCopy receivedCopy, FrontierOverlay currentFrontier) {
        MapFrontiersClient.getOperationService().acceptCopiedFrontierAndReplace(receivedCopy.frontier(), receivedCopy.collection(), currentFrontier);
        ChatFrontiers.removeReceivedId(id);
    }
}
