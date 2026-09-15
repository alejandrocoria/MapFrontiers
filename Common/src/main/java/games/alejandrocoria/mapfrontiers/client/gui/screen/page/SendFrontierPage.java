package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.ChatFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxUser;
import games.alejandrocoria.mapfrontiers.client.gui.util.PlayerInputResolver;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SendFrontierPage extends PageScreen {
    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_send");
    private static final Component DESCRIPTION_LABEL = Component.translatable("mapfrontiers.send_description");
    private static final Component ERROR_SELF_LABEL = Component.translatable("mapfrontiers.new_user_shared_error_self");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final Tooltip SEND_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.send.tooltip"));

    private FrontierOverlay frontier;
    private MultiLineTextWidget description;
    private TextBoxUser textNewUser;
    private IconButton buttonNewUser;

    public SendFrontierPage(FrontierOverlay frontier) {
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
            }
        });
    }

    @Override
    protected void initScreen() {
        if (MapFrontiersClient.isModOnServer()) {
            onClose();
            return;
        }

        LinearLayout mainLayout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout header = LinearLayout.horizontal();
        mainLayout.addChild(header);

        description = header.addChild(new MultiLineTextWidget(DESCRIPTION_LABEL.copy().withColor(ColorConstants.TEXT_HIGHLIGHT), font));
        description.setCentered(true);

        LinearLayout newUserLayout = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        newUserLayout.defaultCellSetting().alignVerticallyMiddle();
        mainLayout.addChild(newUserLayout);

        textNewUser = new TextBoxUser(minecraft, font, LayoutConstants.USER_TEXTBOX_WIDTH);
        textNewUser.setMaxLength(38);
        textNewUser.setSubmitCallback((value) -> buttonNewUserPressed());
        newUserLayout.addChild(textNewUser);

        buttonNewUser = new IconButton(IconButton.Type.Send, (b) -> buttonNewUserPressed());
        buttonNewUser.setTooltip(SEND_TOOLTIP);
        newUserLayout.addChild(buttonNewUser);

        addBottomButton(new SimpleButton(font, LayoutConstants.PAGE_BUTTON_WIDTH, DONE_LABEL, (b) -> onClose()));
    }

    private void buttonNewUserPressed() {
        if (minecraft.player == null) {
            return;
        }

        String usernameOrUUID = textNewUser.getValue();
        clearTextBoxFocus(textNewUser);
        if (StringUtils.isBlank(usernameOrUUID)) {
            return;
        }

        PlayerInputResolver.Result result = PlayerInputResolver.resolveOnline(usernameOrUUID, minecraft.getConnection(),
                MapFrontiersClient.getPlayerNameRepository());
        if (!result.isSuccess()) {
            textNewUser.setError(result.failure().getMessage());
            return;
        }

        PlayerId targetPlayer = result.requirePlayerId();
        String targetUsername = result.requireConnectedUsername();

        if (targetPlayer.equals(new PlayerId(minecraft.player.getUUID()))) {
            textNewUser.setError(ERROR_SELF_LABEL);
            return;
        }

        ChatFrontiers.sendFrontier(frontier, targetUsername);

        textNewUser.setValue("");
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
}
