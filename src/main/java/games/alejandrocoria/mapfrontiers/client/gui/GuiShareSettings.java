package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.gui.GuiScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.common.event.DeletedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRemoveSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateSharedUserPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiShareSettings extends Screen
        implements GuiScrollBox.ScrollBoxResponder, GuiUserSharedElement.UserSharedResponder, TextBox.TextBoxResponder {
    private final FrontiersOverlayManager frontiersOverlayManager;
    private FrontierOverlay frontier;
    private GuiScrollBox users;
    private TextUserBox textNewUser;
    private GuiButtonIcon buttonNewUser;
    private GuiSettingsButton buttonDone;

    private final List<GuiSimpleLabel> labels;
    private boolean canUpdate;
    private int ticksSinceLastUpdate = 0;

    public GuiShareSettings( FrontiersOverlayManager frontiersOverlayManager, FrontierOverlay frontier) {
        super(CommonComponents.EMPTY);
        this.frontiersOverlayManager = frontiersOverlayManager;
        this.frontier = frontier;

        labels = new ArrayList<>();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        Component title = Component.translatable("mapfrontiers.title_share_settings");
        addRenderableOnly(new GuiSimpleLabel(font, width / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        users = new GuiScrollBox(width / 2 - 215, 82, 430, height - 128, 16, this);

        textNewUser = new TextUserBox(minecraft, font, width / 2 - 125, height - 61, 238);
        textNewUser.setMaxLength(38);
        textNewUser.setResponder(this);

        buttonNewUser = new GuiButtonIcon(width / 2 + 114, height - 61, GuiButtonIcon.Type.Add, (button) -> buttonNewUserPressed());
        buttonNewUser.visible = false;

        buttonDone = new GuiSettingsButton(font, width / 2 - 70, height - 28, 140,
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
                GuiUserSharedElement userElement = (GuiUserSharedElement) element;
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
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Widget w : renderables) {
            if (w instanceof GuiScrollBox) {
                ((GuiScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonDone) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }

    private void buttonNewUserPressed() {
        SettingsUser user = new SettingsUser();

        String usernameOrUUID = textNewUser.getValue();
        textNewUser.setFocus(false);
        if (StringUtils.isBlank(usernameOrUUID)) {
            return;
        } else if (usernameOrUUID.length() < 28) {
            user.username = usernameOrUUID;
            user.fillMissingInfo(false);
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
                user.fillMissingInfo(true);
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

        GuiUserSharedElement element = new GuiUserSharedElement(font, renderables, userShared, canUpdate, true, this);
        users.addElement(element);
        users.scrollBottom();

        textNewUser.setValue("");
        resetLabels();
    }

    @Override
    public void removed() {
        MinecraftForge.EVENT_BUS.unregister(this);
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedFrontierEvent(UpdatedFrontierEvent event) {
        if (event.frontierOverlay.getId().equals(frontier.getId())) {
            frontier = event.frontierOverlay;
            updateCanUpdate();
            updateUsers();
            updateButtonsVisibility();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeletedFrontierEvent(DeletedFrontierEvent event) {
        if (event.frontierID.equals(frontier.getId())) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }

    private void resetLabels() {
        for (GuiSimpleLabel label : labels) {
            removeWidget(label);
        }

        labels.clear();

        if (!users.getElements().isEmpty()) {
            int x = width / 2 + 35;
            labels.add(new GuiSimpleLabel(font, x, 54, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 60, 54, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_settings"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
        }

        for (GuiSimpleLabel label : labels) {
            addRenderableOnly(label);
        }
    }

    private void updateButtonsVisibility() {
        users.visible = true;
        buttonNewUser.visible = canUpdate;
        textNewUser.visible = canUpdate;
    }

    @Override
    public void elementClicked(GuiScrollBox scrollBox, ScrollElement element) {

    }

    @Override
    public void elementDelete(GuiScrollBox scrollBox, ScrollElement element) {
        if (scrollBox == users) {
            SettingsUser user = ((GuiUserSharedElement) element).getUser();
            frontier.removeUserShared(user);
            PacketHandler.INSTANCE.sendToServer(new PacketRemoveSharedUserPersonalFrontier(frontier.getId(), user));
            resetLabels();
        }
    }

    @Override
    public void actionChanged(SettingsUserShared user, SettingsUserShared.Action action, boolean checked) {
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
        }

        PacketHandler.INSTANCE.sendToServer(new PacketUpdateSharedUserPersonalFrontier(frontier.getId(), user));
    }

    @Override
    public void updatedValue(TextBox textBox, String value) {

    }

    @Override
    public void lostFocus(TextBox textBox, String value) {

    }

    private void updateUsers() {
        users.removeAll();

        SettingsUser player = new SettingsUser(minecraft.player);
        if (frontier.getUsersShared() != null) {
            for (SettingsUserShared user : frontier.getUsersShared()) {
                users.addElement(new GuiUserSharedElement(font, renderables, user, canUpdate, !user.getUser().equals(player), this));
            }
        }

        resetLabels();
    }

    private void updateCanUpdate() {
        canUpdate = frontier.checkActionUserShared(new SettingsUser(minecraft.player), SettingsUserShared.Action.UpdateSettings);
    }
}
