package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.UserSharedElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxUser;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRemoveSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Date;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ShareSettings extends AutoScaledScreen {
    private static final Component titleLabel = Component.translatable("mapfrontiers.title_share_settings");
    private static final Component updateFrontierLabel = Component.translatable("mapfrontiers.update_frontier");
    private static final Component updateSettingsLabel = Component.translatable("mapfrontiers.update_settings");
    private static final Component errorUUIDSizeLabel = Component.translatable("mapfrontiers.new_user_error_uuid_size");
    private static final Component errorUUIDFormatLabel = Component.translatable("mapfrontiers.new_user_error_uuid_format");
    private static final Component errorUserNotFoundLabel = Component.translatable("mapfrontiers.new_user_shared_error_user_not_found");
    private static final Component errorSelfLabel = Component.translatable("mapfrontiers.new_user_shared_error_self");
    private static final Component errorOwnerLabel = Component.translatable("mapfrontiers.new_user_shared_error_owner");
    private static final Component errorRepeatedLabel = Component.translatable("mapfrontiers.new_user_shared_error_user_repeated");
    private static final Component doneLabel = Component.translatable("gui.done");

    private final FrontiersOverlayManager frontiersOverlayManager;
    private FrontierOverlay frontier;
    private MultiLineTextWidget updateFrontier;
    private MultiLineTextWidget updateSettings;
    private ScrollBox users;
    private TextBoxUser textNewUser;
    private IconButton buttonNewUser;

    private boolean canUpdate;
    private int ticksSinceLastUpdate = 0;

    public ShareSettings(FrontiersOverlayManager frontiersOverlayManager, FrontierOverlay frontier) {
        super(titleLabel, 470, 120);
        this.frontiersOverlayManager = frontiersOverlayManager;
        this.frontier = frontier;

        ClientEventHandler.subscribeDeletedFrontierEvent(this, frontierID -> {
            if (frontierID.equals(this.frontier.getId())) {
                onClose();
            }
        });

        ClientEventHandler.subscribeUpdatedFrontierEvent(this, (frontierOverlay, playerID) -> {
            if (frontierOverlay.getId().equals(this.frontier.getId())) {
                this.frontier = frontierOverlay;
                updateCanUpdate();
                updateUsers();
                updateButtonsVisibility();
            }
        });
    }

    @Override
    public void initScreen() {
        if (!MapFrontiersClient.isModOnServer()) {
            onClose();
        }

        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout header = LinearLayout.horizontal();
        mainLayout.addChild(header);

        updateFrontier = header.addChild(new MultiLineTextWidget(updateFrontierLabel, font));
        updateFrontier.setColor(ColorConstants.TEXT_HIGHLIGHT);
        updateFrontier.setCentered(true);
        updateSettings = header.addChild(new MultiLineTextWidget(updateSettingsLabel, font));
        updateSettings.setColor(ColorConstants.TEXT_HIGHLIGHT);
        updateSettings.setCentered(true);

        users = new ScrollBox(actualHeight - 128, 430, 16);
        users.setElementDeletedCallback(element -> {
            SettingsUser user = ((UserSharedElement) element).getUser();
            frontier.removeUserShared(user);
            PacketHandler.sendToServer(new PacketRemoveSharedUserPersonalFrontier(frontier.getId(), user));
            resetLabels();
        });
        mainLayout.addChild(users);

        LinearLayout newUserLayout = LinearLayout.horizontal().spacing(4);
        mainLayout.addChild(newUserLayout);

        textNewUser = new TextBoxUser(minecraft, font, 238);
        textNewUser.setMaxLength(38);
        newUserLayout.addChild(textNewUser);

        buttonNewUser = new IconButton(IconButton.Type.Add, (b) -> buttonNewUserPressed());
        buttonNewUser.visible = false;
        newUserLayout.addChild(buttonNewUser);

        bottomButtons.addChild(new SimpleButton(font, 140, doneLabel, (b) -> onClose()));

        updateCanUpdate();
        updateButtonsVisibility();
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
                SettingsUser user = userElement.getUser();
                PlayerInfo networkplayerinfo = null;

                if (user.uuid != null) {
                    networkplayerinfo = handler.getPlayerInfo(user.uuid);
                } else if (!StringUtils.isBlank(user.username)) {
                    networkplayerinfo = handler.getPlayerInfo(user.username);
                }

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
    public void repositionElements() {
        users.setSize(430, actualHeight - 128);
        super.repositionElements();
        updateFrontier.setX(users.getX() + 250 - updateFrontier.getWidth() / 2);
        updateSettings.setX(users.getX() + 310 - updateSettings.getWidth() / 2);
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (GuiEventListener w : children()) {
            if (w instanceof ScrollBox) {
                ((ScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void buttonNewUserPressed() {
        if (minecraft.player == null) {
            return;
        }

        SettingsUser user = new SettingsUser();

        String usernameOrUUID = textNewUser.getValue();
        textNewUser.setFocused(false);
        if (StringUtils.isBlank(usernameOrUUID)) {
            return;
        } else if (usernameOrUUID.length() < 28) {
            user.username = usernameOrUUID;
            user.fillMissingInfo(false, null);
        } else {
            usernameOrUUID = usernameOrUUID.replaceAll("[^0-9a-fA-F]", "");
            if (usernameOrUUID.length() != 32) {
                textNewUser.setError(errorUUIDSizeLabel);
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
                textNewUser.setError(errorUUIDFormatLabel);
                return;
            }
        }

        if (user.uuid == null) {
            textNewUser.setError(errorUserNotFoundLabel);
            return;
        }

        ClientPacketListener handler = minecraft.getConnection();
        if (handler != null) {
            if (handler.getPlayerInfo(user.uuid) == null) {
                textNewUser.setError(errorUserNotFoundLabel);
                return;
            }
        }

        if (user.username.equals(minecraft.player.getGameProfile().getName())) {
            textNewUser.setError(errorSelfLabel);
            return;
        }

        if (frontier.getOwner().equals(user)) {
            textNewUser.setError(errorOwnerLabel);
            return;
        }

        if (frontier.hasUserShared(user)) {
            textNewUser.setError(errorRepeatedLabel);
            return;
        }

        SettingsUserShared userShared = new SettingsUserShared(user, true);

        frontier.addUserShared(userShared);
        frontiersOverlayManager.clientShareFrontier(frontier.getId(), user);

        UserSharedElement element = new UserSharedElement(font, userShared, canUpdate, true, this::actionChanged);
        users.addElement(element);
        users.scrollBottom();

        textNewUser.setValue("");
        resetLabels();
    }

    @Override
    public void onClose() {
        ClientEventHandler.unsuscribeAllEvents(this);
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

    private void updateButtonsVisibility() {
        buttonNewUser.visible = canUpdate;
        textNewUser.visible = canUpdate;
    }

    private void actionChanged(SettingsUserShared user, SettingsUserShared.Action action, boolean checked) {
        if (minecraft.player == null) {
            return;
        }

        if (checked) {
            user.addAction(action);
        } else {
            user.removeAction(action);
        }

        if (user.getUser().equals(new SettingsUser(minecraft.player))) {
            if (action == SettingsUserShared.Action.UpdateSettings) {
                updateCanUpdate();
                updateUsers();
                updateButtonsVisibility();
            }

            frontier.setModified(new Date());
            ClientEventHandler.postUpdatedFrontierEvent(frontier, -1);
        }

        PacketHandler.sendToServer(new PacketUpdateSharedUserPersonalFrontier(frontier.getId(), user));
    }

    private void updateUsers() {
        users.removeAll();
        if (minecraft.player == null) {
            return;
        }

        SettingsUser player = new SettingsUser(minecraft.player);
        if (frontier.getUsersShared() != null) {
            for (SettingsUserShared user : frontier.getUsersShared()) {
                users.addElement(new UserSharedElement(font, user, canUpdate, !user.getUser().equals(player), this::actionChanged));
            }
        }

        resetLabels();
    }

    private void updateCanUpdate() {
        if (minecraft.player == null) {
            return;
        }
        canUpdate = frontier.checkActionUserShared(new SettingsUser(minecraft.player), SettingsUserShared.Action.UpdateSettings);
    }
}
