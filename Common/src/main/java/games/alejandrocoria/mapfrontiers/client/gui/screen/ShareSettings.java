package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
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
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ShareSettings extends Screen {
    private final FrontiersOverlayManager frontiersOverlayManager;
    private FrontierOverlay frontier;
    private ScrollBox users;
    private TextBoxUser textNewUser;
    private IconButton buttonNewUser;
    private SimpleButton buttonDone;

    private final List<SimpleLabel> labels;
    private boolean canUpdate;
    private int ticksSinceLastUpdate = 0;

    public ShareSettings(FrontiersOverlayManager frontiersOverlayManager, FrontierOverlay frontier) {
        super(Component.translatable("mapfrontiers.title_share_settings"));
        this.frontiersOverlayManager = frontiersOverlayManager;
        this.frontier = frontier;

        labels = new ArrayList<>();

        ClientEventHandler.subscribeDeletedFrontierEvent(this, frontierID -> {
            if (frontierID.equals(this.frontier.getId())) {
                Services.PLATFORM.popGuiLayer();
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
    public void init() {
        if (!MapFrontiersClient.isModOnServer()) {
            Services.PLATFORM.popGuiLayer();
        }

        users = new ScrollBox(width / 2 - 215, 82, 430, height - 128, 16);
        users.setElementDeletedCallback(element -> {
            SettingsUser user = ((UserSharedElement) element).getUser();
            frontier.removeUserShared(user);
            PacketHandler.sendToServer(new PacketRemoveSharedUserPersonalFrontier(frontier.getId(), user));
            resetLabels();
        });

        textNewUser = new TextBoxUser(minecraft, font, width / 2 - 125, height - 61, 238);
        textNewUser.setMaxLength(38);

        buttonNewUser = new IconButton(width / 2 + 114, height - 61, IconButton.Type.Add, (button) -> buttonNewUserPressed());
        buttonNewUser.visible = false;

        buttonDone = new SimpleButton(font, width / 2 - 70, height - 28, 140,
                Component.translatable("gui.done"), this::buttonPressed);

        addRenderableWidget(buttonNewUser);
        addRenderableWidget(users);
        addRenderableWidget(textNewUser);
        addRenderableWidget(buttonDone);

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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, this.width / 2, 8, ColorConstants.WHITE);
        super.render(graphics, mouseX, mouseY, partialTicks);

        for (SimpleLabel label : labels) {
            if (label.visible) {
                label.render(graphics, mouseX, mouseY, partialTicks);
            }
        }
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

    protected void buttonPressed(Button button) {
        if (button == buttonDone) {
            Services.PLATFORM.popGuiLayer();
        }
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
                textNewUser.setError(Component.translatable("mapfrontiers.new_user_error_uuid_size"));
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
                textNewUser.setError(Component.translatable("mapfrontiers.new_user_error_uuid_format"));
                return;
            }
        }

        if (user.uuid == null) {
            textNewUser.setError(Component.translatable("mapfrontiers.new_user_shared_error_user_not_found"));
            return;
        }

        ClientPacketListener handler = minecraft.getConnection();
        if (handler != null) {
            if (handler.getPlayerInfo(user.uuid) == null) {
                textNewUser.setError(Component.translatable("mapfrontiers.new_user_shared_error_user_not_found"));
                return;
            }
        }

        if (user.username.equals(minecraft.player.getGameProfile().getName())) {
            textNewUser.setError(Component.translatable("mapfrontiers.new_user_shared_error_self"));
            return;
        }

        if (frontier.getOwner().equals(user)) {
            textNewUser.setError(Component.translatable("mapfrontiers.new_user_shared_error_owner"));
            return;
        }

        if (frontier.hasUserShared(user)) {
            textNewUser.setError(Component.translatable("mapfrontiers.new_user_shared_error_user_repeated"));
            return;
        }

        SettingsUserShared userShared = new SettingsUserShared(user, true);

        frontier.addUserShared(userShared);
        frontiersOverlayManager.clientShareFrontier(frontier.getId(), user);

        UserSharedElement element = new UserSharedElement(font, this, userShared, canUpdate, true, this::actionChanged);
        users.addElement(element);
        users.scrollBottom();

        textNewUser.setValue("");
        resetLabels();
    }

    @Override
    public void removed() {
        ClientEventHandler.unsuscribeAllEvents(this);
    }

    private void resetLabels() {
        labels.clear();

        if (!users.getElements().isEmpty()) {
            int x = width / 2 + 35;
            labels.add(new SimpleLabel(font, x, 54, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_frontier"), ColorConstants.TEXT_HIGHLIGHT));
            labels.add(new SimpleLabel(font, x + 60, 54, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_settings"), ColorConstants.TEXT_HIGHLIGHT));
        }
    }

    private void updateButtonsVisibility() {
        users.visible = true;
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
                users.addElement(new UserSharedElement(font, this, user, canUpdate, !user.getUser().equals(player), this::actionChanged));
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
