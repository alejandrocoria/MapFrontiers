package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.UserSharedElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxUser;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class SharedAccessPage extends PageScreen {
    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_shared_access");
    private static final Component UPDATE_FRONTIER_LABEL = Component.translatable("mapfrontiers.update_frontier");
    private static final Component UPDATE_SETTINGS_LABEL = Component.translatable("mapfrontiers.update_settings");
    private static final Component ERROR_UUID_SIZE_LABEL = Component.translatable("mapfrontiers.new_user_error_uuid_size");
    private static final Component ERROR_UUID_FORMAT_LABEL = Component.translatable("mapfrontiers.new_user_error_uuid_format");
    private static final Component ERROR_USER_NOT_FOUND_LABEL = Component.translatable("mapfrontiers.new_user_shared_error_user_not_found");
    private static final Component ERROR_SELF_LABEL = Component.translatable("mapfrontiers.new_user_shared_error_self");
    private static final Component ERROR_OWNER_LABEL = Component.translatable("mapfrontiers.new_user_shared_error_owner");
    private static final Component ERROR_REPEATED_LABEL = Component.translatable("mapfrontiers.new_user_shared_error_user_repeated");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final Tooltip ADD_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.add.tooltip"));
    private static final int USERS_WIDTH = 430;
    private static final int USERS_ELEMENT_HEIGHT = 15;
    private static final int USERS_MIN_ROWS = 4;
    private static final int USERS_VERTICAL_MARGIN = 128;

    private FrontierOverlay frontier;
    private MultiLineTextWidget updateFrontier;
    private MultiLineTextWidget updateSettings;
    private ScrollBox users;
    private TextBoxUser textNewUser;
    private IconButton buttonNewUser;

    private boolean canUpdate;
    private int ticksSinceLastUpdate = 0;

    public SharedAccessPage(FrontierOverlay frontier) {
        super(TITLE_LABEL);
        this.frontier = frontier;

        MapFrontiersClient.getFrontierEvents().subscribeDeleted(this, frontierID -> {
            if (frontierID.equals(this.frontier.getId())) {
                onClose();
            }
        });

        MapFrontiersClient.getFrontierEvents().subscribeUpdated(this, (frontierOverlay, playerID) -> {
            if (frontierOverlay.getId().equals(this.frontier.getId())) {
                this.frontier = frontierOverlay;
                updateCanUpdate();
                updateUsers();
                refreshControlState();
            }
        });
    }

    @Override
    protected void initScreen() {
        if (!MapFrontiersClient.isModOnServer()) {
            onClose();
            return;
        }

        LinearLayout mainLayout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout header = LinearLayout.horizontal();
        mainLayout.addChild(header);

        updateFrontier = header.addChild(new MultiLineTextWidget(UPDATE_FRONTIER_LABEL.copy().withColor(ColorConstants.TEXT_HIGHLIGHT), font));
        updateFrontier.setCentered(true);
        updateSettings = header.addChild(new MultiLineTextWidget(UPDATE_SETTINGS_LABEL.copy().withColor(ColorConstants.TEXT_HIGHLIGHT), font));
        updateSettings.setCentered(true);

        users = new ScrollBox(ScrollBox.rowsToHeight(USERS_MIN_ROWS, USERS_ELEMENT_HEIGHT), USERS_WIDTH, USERS_ELEMENT_HEIGHT);
        users.setHorizontalEdgeNavigation(ScrollBox.HorizontalEdgeNavigation.KEEP_FOCUS);
        users.setElementDeletePressedCallback(element -> {
            new DeleteConfirmationDialog(
                    "mapfrontiers.delete_user_dialog",
                    ClientConfig.ASK_CONFIRMATION_USER_DELETE,
                    response -> deleteUserPressed(element)
            ).display();
        });
        mainLayout.addChild(users);

        LinearLayout newUserLayout = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        newUserLayout.defaultCellSetting().alignVerticallyMiddle();
        mainLayout.addChild(newUserLayout);

        textNewUser = new TextBoxUser(minecraft, font, LayoutConstants.USER_TEXTBOX_WIDTH);
        textNewUser.setMaxLength(38);
        textNewUser.setSubmitCallback((value) -> buttonNewUserPressed());
        newUserLayout.addChild(textNewUser);

        buttonNewUser = new IconButton(IconButton.Type.Add, (b) -> buttonNewUserPressed());
        buttonNewUser.setTooltip(ADD_TOOLTIP);
        buttonNewUser.active = false;
        newUserLayout.addChild(buttonNewUser);

        addBottomButton(new SimpleButton(font, LayoutConstants.PAGE_BUTTON_WIDTH, DONE_LABEL, (b) -> onClose()));

        updateCanUpdate();
        refreshControlState();
        updateUsers();
    }

    @Override
    public void tick() {
        ++ticksSinceLastUpdate;

        if (ticksSinceLastUpdate >= 100) {
            ticksSinceLastUpdate = 0;

            ClientPacketListener handler = minecraft.getConnection();
            if (handler == null) {
                return;
            }

            for (ScrollElement element : users.getElements()) {
                UserSharedElement userElement = (UserSharedElement) element;
                PlayerId user = userElement.getUser();
                PlayerInfo networkplayerinfo = null;

                networkplayerinfo = handler.getPlayerInfo(user.uuid());

                if (networkplayerinfo == null) {
                    userElement.setPingBar(0);
                    continue;
                }

                if (networkplayerinfo.getLatency() < 0) {
                    userElement.setPingBar(0);
                } else if (networkplayerinfo.getLatency() < 150) {
                    userElement.setPingBar(5);
                } else if (networkplayerinfo.getLatency() < 300) {
                    userElement.setPingBar(4);
                } else if (networkplayerinfo.getLatency() < 600) {
                    userElement.setPingBar(3);
                } else if (networkplayerinfo.getLatency() < 1000) {
                    userElement.setPingBar(2);
                } else {
                    userElement.setPingBar(1);
                }
            }
        }
    }

    @Override
    protected void resetContentToMinimumSize() {
        users.setViewportHeight(ScrollBox.rowsToHeight(USERS_MIN_ROWS, USERS_ELEMENT_HEIGHT));
    }

    @Override
    protected void resizeContentToAvailableSpace() {
        users.setViewportSize(USERS_WIDTH, Math.max(ScrollBox.rowsToHeight(USERS_MIN_ROWS, USERS_ELEMENT_HEIGHT),
                availableHeight(USERS_VERTICAL_MARGIN)));
    }

    @Override
    public void repositionElements() {
        super.repositionElements();
        updateFrontier.setX(users.getX() + 250 - updateFrontier.getWidth() / 2);
        updateSettings.setX(users.getX() + 310 - updateSettings.getWidth() / 2);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (GuiEventListener w : children()) {
            if (w instanceof ScrollBox) {
                ((ScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(event);
    }

    private void deleteUserPressed(ScrollElement element) {
        PlayerId user = ((UserSharedElement) element).getUser();
        MapFrontiersClient.getOperationService().submitOptimisticRemoveSharedUser(frontier.getId(), new SettingsUser(user));
    }

    private void buttonNewUserPressed() {
        if (minecraft.player == null) {
            return;
        }

        SettingsUser user = new SettingsUser();

        String usernameOrUUID = textNewUser.getValue();
        clearTextBoxFocus(textNewUser);
        if (StringUtils.isBlank(usernameOrUUID)) {
            return;
        } else if (usernameOrUUID.length() < 28) {
            user.username = usernameOrUUID;
            user.fillMissingInfo(false, null);
        } else {
            usernameOrUUID = usernameOrUUID.replaceAll("[^0-9a-fA-F]", "");
            if (usernameOrUUID.length() != 32) {
                textNewUser.setError(ERROR_UUID_SIZE_LABEL);
                return;
            }
            usernameOrUUID = usernameOrUUID.toLowerCase();
            String uuid = usernameOrUUID.substring(0, 8) + "-" + usernameOrUUID.substring(8, 12) + "-"
                    + usernameOrUUID.substring(12, 16) + "-" + usernameOrUUID.substring(16, 20) + "-"
                    + usernameOrUUID.substring(20, 32);

            try {
                user.uuid = UUID.fromString(uuid);
                user.fillMissingInfo(true, null);
            } catch (Exception e) {
                textNewUser.setError(ERROR_UUID_FORMAT_LABEL);
                return;
            }
        }

        if (user.uuid == null) {
            textNewUser.setError(ERROR_USER_NOT_FOUND_LABEL);
            return;
        }

        ClientPacketListener handler = minecraft.getConnection();
        if (handler != null) {
            if (handler.getPlayerInfo(user.uuid) == null) {
                textNewUser.setError(ERROR_USER_NOT_FOUND_LABEL);
                return;
            }
        }

        if (user.username.equals(minecraft.player.getGameProfile().name())) {
            textNewUser.setError(ERROR_SELF_LABEL);
            return;
        }

        if (frontier.getOwner().equals(user.toPlayerId())) {
            textNewUser.setError(ERROR_OWNER_LABEL);
            return;
        }

        if (frontier.hasUserAccess(user.toPlayerId())) {
            textNewUser.setError(ERROR_REPEATED_LABEL);
            return;
        }

        if (MapFrontiersClient.getOperationService().submitOptimisticShareFrontier(frontier.getId(), user)) {
            users.scrollBottom();
            textNewUser.setValue("");
        }
    }

    private void clearTextBoxFocus(TextBox textBox) {
        if (getFocused() == textBox) {
            setFocused(null);
        }
    }

    @Override
    public void onClose() {
        MapFrontiersClient.getFrontierEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
        super.onClose();
    }

    private void resetLabels() {
        if (users.getElements().isEmpty()) {
            updateFrontier.visible = false;
            updateSettings.visible = false;
        } else {
            updateFrontier.visible = true;
            updateSettings.visible = true;
        }
    }

    private void refreshControlState() {
        buttonNewUser.active = canUpdate;
        textNewUser.setEditable(canUpdate);
    }

    private void actionChanged(FrontierUserAccess user, FrontierUserAccess.Action action, boolean checked) {
        if (minecraft.player == null) {
            return;
        }

        FrontierUserAccess desiredUser = new FrontierUserAccess(user);
        if (checked) {
            desiredUser.addAction(action);
        } else {
            desiredUser.removeAction(action);
        }

        if (!MapFrontiersClient.getOperationService().submitOptimisticUpdateSharedUser(frontier.getId(), desiredUser)) {
            updateUsers();
        }
    }

    private void updateUsers() {
        ScrollBox.FocusSnapshot focusSnapshot = users.captureFocusSnapshot();
        users.removeAll();
        if (minecraft.player == null) {
            return;
        }

        PlayerId player = new PlayerId(minecraft.player.getUUID());
        if (frontier.getUserAccesses() != null) {
            for (FrontierUserAccess user : frontier.getUserAccesses()) {
                users.addElement(new UserSharedElement(font, user, canUpdate, !user.getPlayerId().equals(player), this::actionChanged));
            }
        }

        resetLabels();
        ComponentPath path = users.restoreFocusSnapshot(focusSnapshot);
        if (path != null) {
            setFocused(users);
            path.applyFocus(true);
        }
    }

    private void updateCanUpdate() {
        if (minecraft.player == null) {
            return;
        }
        canUpdate = frontier.checkUserAccess(new PlayerId(minecraft.player.getUUID()), FrontierUserAccess.Action.UpdateSettings);
    }

}
