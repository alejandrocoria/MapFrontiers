package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.ChatFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
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
public class SendFrontier extends AutoScaledScreen {
    private static final Component titleLabel = Component.translatable("mapfrontiers.title_send");
    private static final Component descriptionLabel = Component.translatable("mapfrontiers.send_description");
    private static final Component errorUUIDSizeLabel = Component.translatable("mapfrontiers.new_user_error_uuid_size");
    private static final Component errorUUIDFormatLabel = Component.translatable("mapfrontiers.new_user_error_uuid_format");
    private static final Component errorUserNotFoundLabel = Component.translatable("mapfrontiers.new_user_shared_error_user_not_found");
    private static final Component errorSelfLabel = Component.translatable("mapfrontiers.new_user_shared_error_self");
    private static final Component doneLabel = Component.translatable("gui.done");

    private FrontierOverlay frontier;
    private MultiLineTextWidget description;
    private TextBoxUser textNewUser;
    private IconButton buttonNewUser;

    public SendFrontier(FrontierOverlay frontier) {
        super(titleLabel, 470, 120);
        this.frontier = frontier;

        ClientEventHandler.subscribeDeletedFrontierEvent(this, frontierID -> {
            if (frontierID.equals(this.frontier.getId())) {
                onClose();
            }
        });

        ClientEventHandler.subscribeUpdatedFrontierEvent(this, (frontierOverlay, playerID) -> {
            if (frontierOverlay.getId().equals(this.frontier.getId())) {
                this.frontier = frontierOverlay;
            }
        });
    }

    @Override
    public void initScreen() {
        if (MapFrontiersClient.isModOnServer()) {
            onClose();
        }

        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout header = LinearLayout.horizontal();
        mainLayout.addChild(header);

        description = header.addChild(new MultiLineTextWidget(descriptionLabel, font));
        description.setColor(ColorConstants.TEXT_HIGHLIGHT);
        description.setCentered(true);

        LinearLayout newUserLayout = LinearLayout.horizontal().spacing(4);
        mainLayout.addChild(newUserLayout);

        textNewUser = new TextBoxUser(minecraft, font, 238);
        textNewUser.setMaxLength(38);
        textNewUser.setSubmitCallback((value) -> buttonNewUserPressed());
        newUserLayout.addChild(textNewUser);

        buttonNewUser = new IconButton(IconButton.Type.Send, (b) -> buttonNewUserPressed());
        newUserLayout.addChild(buttonNewUser);

        bottomButtons.addChild(new SimpleButton(font, 140, doneLabel, (b) -> onClose()));
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
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

        if (StringUtil.isBlank(user.username)) {
            textNewUser.setError(errorUserNotFoundLabel);
            return;
        }

        if (user.username.equals(minecraft.player.getGameProfile().getName())) {
            textNewUser.setError(errorSelfLabel);
            return;
        }

        ChatFrontiers.sendFrontier(frontier, user);

        textNewUser.setValue("");
    }

    @Override
    public void onClose() {
        ClientEventHandler.unsubscribeAllEvents(this);
        super.onClose();
    }
}
