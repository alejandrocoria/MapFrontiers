package games.alejandrocoria.mapfrontiers.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.gui.GuiScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiShareSettings extends GuiScreen
        implements GuiScrollBox.ScrollBoxResponder, GuiUserSharedElement.UserSharedResponder, TextBox.TextBoxResponder {
    private static final int guiTextureSize = 512;

    private ResourceLocation guiTexture;
    private FrontiersOverlayManager frontiersOverlayManager;
    private FrontierOverlay frontier;
    private GuiScrollBox users;
    private TextUserBox textNewUser;
    private GuiButtonIcon buttonNewUser;
    private List<GuiSimpleLabel> labels;
    private int ticksSinceLastUpdate = 0;
    private int id = 0;

    public GuiShareSettings(FrontiersOverlayManager frontiersOverlayManager, FrontierOverlay frontier) {
        this.frontiersOverlayManager = frontiersOverlayManager;
        this.frontier = frontier;

        guiTexture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/gui.png");
        labels = new ArrayList<GuiSimpleLabel>();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        users = new GuiScrollBox(++id, width / 2 - 215, 82, 430, height - 128, 16, this);

        textNewUser = new TextUserBox(++id, mc, fontRenderer, width / 2 - 125, height - 61, 238,
                I18n.format("mapfrontiers.new_user"));
        textNewUser.setMaxStringLength(38);
        textNewUser.setResponder(this);
        textNewUser.setCentered(false);
        textNewUser.setFrame(true);

        buttonNewUser = new GuiButtonIcon(++id, width / 2 + 114, height - 61, 13, 13, 494, 119, -23, guiTexture, guiTextureSize);
        buttonNewUser.visible = false;

        buttonList.add(buttonNewUser);

        resetLabels();
        updateButtonsVisibility();
        updateUsers();
    }

    @Override
    public void updateScreen() {
        ++ticksSinceLastUpdate;

        if (ticksSinceLastUpdate >= 100) {
            ticksSinceLastUpdate = 0;

            NetHandlerPlayClient handler = mc.getConnection();
            if (handler == null) {
                return;
            }

            for (ScrollElement element : users.getElements()) {
                GuiUserSharedElement userElement = (GuiUserSharedElement) element;
                SettingsUser user = userElement.getUser();
                NetworkPlayerInfo networkplayerinfo = null;

                if (user.uuid != null) {
                    networkplayerinfo = handler.getPlayerInfo(user.uuid);
                } else if (!StringUtils.isBlank(user.username)) {
                    networkplayerinfo = handler.getPlayerInfo(user.username);
                }

                if (networkplayerinfo == null) {
                    userElement.setPingBar(0);
                    continue;
                }

                if (networkplayerinfo.getResponseTime() < 0) {
                    userElement.setPingBar(0);
                } else if (networkplayerinfo.getResponseTime() < 150) {
                    userElement.setPingBar(5);
                } else if (networkplayerinfo.getResponseTime() < 300) {
                    userElement.setPingBar(4);
                } else if (networkplayerinfo.getResponseTime() < 600) {
                    userElement.setPingBar(3);
                } else if (networkplayerinfo.getResponseTime() < 1000) {
                    userElement.setPingBar(2);
                } else {
                    userElement.setPingBar(1);
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        users.drawBox(mc, mouseX, mouseY);
        textNewUser.drawTextBox(mouseX, mouseY);

        for (GuiSimpleLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int i = -Mouse.getEventDWheel();

        if (i != 0) {
            i = Integer.signum(i);
            users.scroll(i);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int btn) throws IOException {
        if (btn == 0) {
            users.mousePressed(mc, x, y);
            textNewUser.mouseClicked(x, y, btn);
        }

        super.mouseClicked(x, y, btn);
    }

    @Override
    protected void mouseReleased(int x, int y, int state) {
        if (state == 0) {
            users.mouseReleased(mc, x, y);
        }

        super.mouseReleased(x, y, state);
    }

    @Override
    protected void mouseClickMove(int x, int y, int btn, long timeSinceLastClick) {
        if (btn == 0) {
            users.mouseClickMove(mc, x, y);
        }

        super.mouseClickMove(x, y, btn, timeSinceLastClick);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        textNewUser.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == buttonNewUser) {
            SettingsUser user = new SettingsUser();

            String usernameOrUUID = textNewUser.getText();
            if (StringUtils.isBlank(usernameOrUUID)) {
                return;
            } else if (usernameOrUUID.length() < 28) {
                user.username = usernameOrUUID;
                user.fillMissingInfo(false);
            } else {
                usernameOrUUID = usernameOrUUID.replaceAll("[^0-9a-fA-F]", "");
                if (usernameOrUUID.length() != 32) {
                    textNewUser.setError(I18n.format("mapfrontiers.new_user_error_uuid_size"));
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
                    textNewUser.setError(I18n.format("mapfrontiers.new_user_error_uuid_format"));
                    return;
                }
            }


            if (user.username.equals(mc.player.getName())) {
                textNewUser.setError(I18n.format("mapfrontiers.new_user_shared_error_self"));
                return;
            }

            if (frontier.getOwner().equals(user)) {
                textNewUser.setError(I18n.format("mapfrontiers.new_user_shared_error_owner"));
                return;
            }


            if (frontier.hasUserShared(user)) {
                textNewUser.setError(I18n.format("mapfrontiers.new_user_shared_error_user_repeated"));
                return;
            }

            SettingsUserShared userShared = new SettingsUserShared(user, true);

            frontier.addUserShared(userShared);
            frontiersOverlayManager.clientShareFrontier(frontier.getId(), user);

            GuiUserSharedElement element = new GuiUserSharedElement(fontRenderer, buttonList, id, userShared, this, guiTexture,
                    guiTextureSize);
            users.addElement(element);
            users.scrollBottom();

            textNewUser.setText("");
            resetLabels();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private void resetLabels() {
        labels.clear();

        if (!users.getElements().isEmpty()) {
            int x = width / 2 + 35;
            labels.add(new GuiSimpleLabel(fontRenderer, x, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.update_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 60, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.update_settings"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
        }

    }

    private void updateButtonsVisibility() {
        users.visible = true;
        buttonNewUser.visible = true;
    }

    @Override
    public void elementClicked(int id, ScrollElement element) {

    }

    @Override
    public void elementDelete(int id, ScrollElement element) {
        if (id == users.getId()) {
            // @Incomplete
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
    }

    @Override
    public void updatedValue(int id, String value) {

    }

    @Override
    public void lostFocus(int id, String value) {

    }

    private void updateUsers() {
        List<SettingsUserShared> usersShared = frontier.getUserShared();
        if (usersShared != null) {
            for (SettingsUserShared user : usersShared) {
                users.addElement(new GuiUserSharedElement(fontRenderer, buttonList, id, user, this, guiTexture, guiTextureSize));
            }
        }

        resetLabels();
    }

    private boolean canAddNewUser() {
        // @Incomplete
        return true;
    }
}
