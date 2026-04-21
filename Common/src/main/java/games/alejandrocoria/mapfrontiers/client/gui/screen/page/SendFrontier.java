package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.ChatFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class SendFrontier extends PageScreen {
    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_send");
    private static final Component DESCRIPTION_LABEL = Component.translatable("mapfrontiers.send_description");
    private static final Component ERROR_UUID_SIZE_LABEL = Component.translatable("mapfrontiers.new_user_error_uuid_size");
    private static final Component ERROR_UUID_FORMAT_LABEL = Component.translatable("mapfrontiers.new_user_error_uuid_format");
    private static final Component ERROR_USER_NOT_FOUND_LABEL = Component.translatable("mapfrontiers.new_user_shared_error_user_not_found");
    private static final Component ERROR_SELF_LABEL = Component.translatable("mapfrontiers.new_user_shared_error_self");
    private static final Component DONE_LABEL = Component.translatable("gui.done");

    private FrontierOverlay frontier;
    private MultiLineTextWidget description;
    private TextBoxUser textNewUser;
    private IconButton buttonNewUser;

    public SendFrontier(FrontierOverlay frontier) {
        super(TITLE_LABEL, 470, 120);
        this.frontier = frontier;

        MapFrontiersClient.getFrontierEvents().subscribeDeleted(this, frontierID -> {
            if (frontierID.equals(this.frontier.getId())) {
                onClose();
            }
        });

        MapFrontiersClient.getFrontierEvents().subscribeUpdated(this, (frontierOverlay, playerID) -> {
            if (frontierOverlay.getId().equals(this.frontier.getId())) {
                this.frontier = frontierOverlay;
            }
        });
    }

    @Override
    protected void initScreen() {
        if (MapFrontiersClient.isModOnServer()) {
            onClose();
            return;
        }

        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout header = LinearLayout.horizontal();
        mainLayout.addChild(header);

        description = header.addChild(new MultiLineTextWidget(DESCRIPTION_LABEL.copy().withColor(ColorConstants.TEXT_HIGHLIGHT), font));
        description.setCentered(true);

        LinearLayout newUserLayout = LinearLayout.horizontal().spacing(4);
        mainLayout.addChild(newUserLayout);

        textNewUser = new TextBoxUser(minecraft, font, 238);
        textNewUser.setMaxLength(38);
        textNewUser.setSubmitCallback((value) -> buttonNewUserPressed());
        newUserLayout.addChild(textNewUser);

        buttonNewUser = new IconButton(IconButton.Type.Send, (b) -> buttonNewUserPressed());
        newUserLayout.addChild(buttonNewUser);

        addBottomButton(new SimpleButton(font, 140, DONE_LABEL, (b) -> onClose()));
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

        if (StringUtil.isBlank(user.username)) {
            textNewUser.setError(ERROR_USER_NOT_FOUND_LABEL);
            return;
        }

        if (user.username.equals(minecraft.player.getGameProfile().name())) {
            textNewUser.setError(ERROR_SELF_LABEL);
            return;
        }

        ChatFrontiers.sendFrontier(frontier, user);

        textNewUser.setValue("");
    }

    @Override
    public void onClose() {
        MapFrontiersClient.getFrontierEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
        super.onClose();
    }
}

