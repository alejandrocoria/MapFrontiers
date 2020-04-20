package games.alejandrocoria.mapfrontiers.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import org.lwjgl.input.Mouse;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRequestFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings.Action;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
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
public class GuiFrontierSettings extends GuiScreen implements GuiScrollBox.ScrollBoxResponder,
        GuiGroupActionElement.GroupActionResponder, GuiTabbedBox.TabbedBoxResponder, TextBox.TextBoxResponder {
    private static final int guiTextureSize = 512;

    private ResourceLocation guiTexture;
    private FrontierSettings settings;
    private GuiTabbedBox tabbedBox;
    private GuiScrollBox groups;
    private GuiScrollBox users;
    private GuiScrollBox groupsActions;
    private TextBox textNewGroupName;
    private GuiButtonIcon buttonNewGroup;
    private TextBox textNewUser;
    private GuiButtonIcon buttonNewUser;
    private TextBox textGroupName;
    private List<GuiSimpleLabel> labels;
    private int tabSelected = 0;
    private int ticksSinceLastUpdate = 0;
    private int id = 0;

    public GuiFrontierSettings() {
        guiTexture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/gui.png");
        labels = new ArrayList<GuiSimpleLabel>();
    }

    @Override
    public void initGui() {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings());
        tabbedBox = new GuiTabbedBox(fontRenderer, 40, 24, width - 80, height - 64, this);
        tabbedBox.addTab(I18n.format("mapfrontiers.groups"));
        tabbedBox.addTab(I18n.format("mapfrontiers.actions"));
        tabbedBox.setTabSelected(tabSelected);
        groups = new GuiScrollBox(++id, 50, 50, 160, height - 120, 16, this);
        users = new GuiScrollBox(++id, 250, 82, 258, height - 150, 16, this);
        groupsActions = new GuiScrollBox(++id, width / 2 - 185, 82, 370, height - 128, 16, this);

        textNewGroupName = new TextBox(++id, fontRenderer, 50, height - 61, 140, I18n.format("mapfrontiers.new_group_name"));
        textNewGroupName.setMaxStringLength(22);
        textNewGroupName.setResponder(this);
        textNewGroupName.setCentered(false);
        textNewGroupName.setColor(0xffffffff);
        textNewGroupName.setFrame(true);

        buttonNewGroup = new GuiButtonIcon(++id, 192, height - 61, 13, 13, 494, 119, -23, guiTexture, guiTextureSize);

        textNewUser = new TextBox(++id, fontRenderer, 250, height - 61, 238, I18n.format("mapfrontiers.new_user"));
        textNewUser.setMaxStringLength(38);
        textNewUser.setResponder(this);
        textNewUser.setCentered(false);
        textNewUser.setColor(0xffffffff);
        textNewUser.setFrame(true);

        buttonNewUser = new GuiButtonIcon(++id, 490, height - 61, 13, 13, 494, 119, -23, guiTexture, guiTextureSize);
        buttonNewUser.visible = false;

        textGroupName = new TextBox(++id, fontRenderer, 250, 50, 140, I18n.format("mapfrontiers.edit_group_name"));
        textGroupName.setMaxStringLength(22);
        textGroupName.setResponder(this);
        textGroupName.setEnabled(false);
        textGroupName.setCentered(false);
        textGroupName.setColor(0xffffffff);

        buttonList.add(buttonNewGroup);
        buttonList.add(buttonNewUser);
    }

    @Override
    public void updateScreen() {
        ++ticksSinceLastUpdate;

        if (ticksSinceLastUpdate >= 100) {
            ticksSinceLastUpdate = 0;
            PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings(settings.getChangeCounter()));

            NetHandlerPlayClient handler = mc.getConnection();
            if (handler == null) {
                return;
            }

            for (ScrollElement element : users.getElements()) {
                GuiUserElement userElement = (GuiUserElement) element;
                SettingsUser user = userElement.getUser();
                NetworkPlayerInfo networkplayerinfo = null;

                if (user.uuid != null) {
                    networkplayerinfo = handler.getPlayerInfo(user.uuid);
                } else if (!user.username.isEmpty()) {
                    networkplayerinfo = handler.getPlayerInfo(user.username);
                }

                if (networkplayerinfo == null) {
                    userElement.setPingBar(0);
                    continue;
                }

                if (networkplayerinfo.getResponseTime() <= 0) {
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

        tabbedBox.drawBox(mc, mouseX, mouseY);
        groups.drawBox(mc, mouseX, mouseY);
        users.drawBox(mc, mouseX, mouseY);
        groupsActions.drawBox(mc, mouseX, mouseY);

        if (tabSelected == 0) {
            textNewGroupName.drawTextBox();
            if (groups.getSelectedElement() != null) {
                textGroupName.drawTextBox();
            }
        }

        if (canAddNewUser()) {
            textNewUser.drawTextBox();
        }

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
            groups.scroll(i);
            users.scroll(i);
            groupsActions.scroll(i);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int btn) throws IOException {
        if (btn == 0) {
            tabbedBox.mousePressed(mc, x, y);
            groups.mousePressed(mc, x, y);
            users.mousePressed(mc, x, y);
            textNewGroupName.mouseClicked(x, y, btn);
            textNewUser.mouseClicked(x, y, btn);
            groupsActions.mousePressed(mc, x, y);
            textGroupName.mouseClicked(x, y, btn);
        }

        super.mouseClicked(x, y, btn);
    }

    @Override
    protected void mouseReleased(int x, int y, int state) {
        if (state == 0) {
            groups.mouseReleased(mc, x, y);
            users.mouseReleased(mc, x, y);
            groupsActions.mouseReleased(mc, x, y);
        }

        super.mouseReleased(x, y, state);
    }

    @Override
    protected void mouseClickMove(int x, int y, int btn, long timeSinceLastClick) {
        if (btn == 0) {
            groups.mouseClickMove(mc, x, y);
            users.mouseClickMove(mc, x, y);
            groupsActions.mouseClickMove(mc, x, y);
        }

        super.mouseClickMove(x, y, btn, timeSinceLastClick);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        textNewGroupName.textboxKeyTyped(typedChar, keyCode);
        textNewUser.textboxKeyTyped(typedChar, keyCode);
        textGroupName.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == buttonNewGroup) {
            SettingsGroup group = settings.createCustomGroup(textNewGroupName.getText());
            GuiGroupElement element = new GuiGroupElement(fontRenderer, buttonList, id, group, guiTexture, guiTextureSize);
            groups.addElement(element);
            groupClicked(element);
            groups.scrollBottom();
            groupsActions.scrollBottom();

            textNewGroupName.setText("");

            sendChangesToServer();
        } else if (button == buttonNewUser) {
            SettingsGroup group = ((GuiGroupElement) groups.getSelectedElement()).getGroup();
            SettingsUser user = new SettingsUser();

            String usernameOrUUID = textNewUser.getText();
            if (usernameOrUUID.isEmpty()) {
                return;
            } else if (usernameOrUUID.length() < 28) {
                user.username = usernameOrUUID;
                user.fillMissingInfo(false);
            } else {
                usernameOrUUID = usernameOrUUID.replaceAll("[^0-9a-fA-F]", "");
                if (usernameOrUUID.length() != 32) {
                    // @Incomplete: announce error
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
                    // @Incomplete: announce error
                    return;
                }
            }

            if (group.hasUser(user)) {
                // @Incomplete: announce error
                return;
            }

            group.addUser(user);
            GuiUserElement element = new GuiUserElement(fontRenderer, buttonList, id, user, guiTexture, guiTextureSize);
            users.addElement(element);
            users.scrollBottom();

            textNewUser.setText("");

            sendChangesToServer();
        }
    }

    @Override
    public void onGuiClosed() {

    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    public void setFrontierSettings(FrontierSettings settings) {
        this.settings = settings;

        groups.removeAll();
        groups.addElement(new GuiGroupElement(fontRenderer, buttonList, id, settings.getOPsGroup(), guiTexture, guiTextureSize));
        groups.addElement(
                new GuiGroupElement(fontRenderer, buttonList, id, settings.getOwnersGroup(), guiTexture, guiTextureSize));
        groups.addElement(
                new GuiGroupElement(fontRenderer, buttonList, id, settings.getEveryoneGroup(), guiTexture, guiTextureSize));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groups.addElement(new GuiGroupElement(fontRenderer, buttonList, id, group, guiTexture, guiTextureSize));
        }

        updateGroupsActions();

        resetLabels();
        updateButtonsVisibility();
    }

    private void resetLabels() {
        labels.clear();

        if (tabSelected == 0) {
            GuiGroupElement element = (GuiGroupElement) groups.getSelectedElement();
            if (element != null && element.getGroup().isSpecial()) {
                SettingsGroup group = element.getGroup();
                if (group == settings.getOPsGroup()) {
                    labels.add(new GuiSimpleLabel(fontRenderer, 250, 82, GuiSimpleLabel.Align.Left,
                            I18n.format("mapfrontiers.group_ops_desc"), 0xffdddddd));
                } else if (group == settings.getOwnersGroup()) {
                    labels.add(new GuiSimpleLabel(fontRenderer, 250, 82, GuiSimpleLabel.Align.Left,
                            I18n.format("mapfrontiers.group_owners_desc"), 0xffdddddd));
                } else if (group == settings.getEveryoneGroup()) {
                    labels.add(new GuiSimpleLabel(fontRenderer, 250, 82, GuiSimpleLabel.Align.Left,
                            I18n.format("mapfrontiers.group_everyone_desc"), 0xffdddddd));
                }
            }
        } else if (tabSelected == 1) {
            int x = width / 2 - 25;
            labels.add(new GuiSimpleLabel(fontRenderer, x, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.create_frontier"), 0xffffffff));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 60, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.delete_frontier"), 0xffffffff));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 120, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.update_frontier"), 0xffffffff));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 180, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.update_settings"), 0xffffffff));
        }
    }

    private void updateButtonsVisibility() {
        groups.visible = tabSelected == 0;
        users.visible = tabSelected == 0;
        buttonNewGroup.visible = tabSelected == 0;
        buttonNewUser.visible = canAddNewUser();
        groupsActions.visible = tabSelected == 1;
    }

    public void groupClicked(GuiGroupElement element) {
        groups.selectElement(element);
        textGroupName.setText(element.getGroup().getName());
        textGroupName.setEnabled(!element.getGroup().isSpecial());

        resetLabels();
        updateUsers();
    }

    @Override
    public void elementClicked(int id, ScrollElement element) {
        if (id == groups.getId()) {
            GuiGroupElement group = (GuiGroupElement) element;
            textGroupName.setText(group.getGroup().getName());
            textGroupName.setEnabled(!group.getGroup().isSpecial());
            resetLabels();
            updateUsers();
        }
    }

    @Override
    public void elementDelete(int id, ScrollElement element) {
        if (id == groups.getId()) {
            if (groups.getSelectedElement() != null) {
                groupClicked((GuiGroupElement) groups.getSelectedElement());
            }
            settings.removeCustomGroup(((GuiGroupElement) element).getGroup());
            sendChangesToServer();
        } else if (id == users.getId()) {
            SettingsGroup group = ((GuiGroupElement) groups.getSelectedElement()).getGroup();
            group.removeUser(((GuiUserElement) element).getUser());
            sendChangesToServer();
        }
    }

    @Override
    public void actionChanged(SettingsGroup group, Action action, boolean checked) {
        if (checked) {
            group.addAction(action);
        } else {
            group.removeAction(action);
        }

        sendChangesToServer();
    }

    @Override
    public void tabChanged(int tab) {
        tabSelected = tab;

        if (tabSelected == 1) {
            updateGroupsActions();
        }

        resetLabels();
        updateButtonsVisibility();
    }

    @Override
    public void updatedValue(int id, String value) {
    }

    @Override
    public void lostFocus(int id, String value) {
        if (textGroupName.getId() == id && tabSelected == 0) {
            GuiGroupElement groupElement = (GuiGroupElement) groups.getSelectedElement();
            if (groupElement != null) {
                groupElement.getGroup().setName(value);
                sendChangesToServer();
            }
        }
    }

    private void sendChangesToServer() {
        settings.advanceChangeCounter();
        PacketHandler.INSTANCE.sendToServer(new PacketFrontierSettings(settings));
    }

    private void updateUsers() {
        users.removeAll();
        GuiGroupElement element = (GuiGroupElement) groups.getSelectedElement();
        if (element != null && !element.getGroup().isSpecial()) {
            for (SettingsUser user : element.getGroup().getUsers()) {
                users.addElement(new GuiUserElement(fontRenderer, buttonList, id, user, guiTexture, guiTextureSize));
            }
        }

        buttonNewUser.visible = canAddNewUser();
    }

    private void updateGroupsActions() {
        groupsActions.removeAll();
        groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getOPsGroup(), this));
        groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getOwnersGroup(), true, this));
        groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getEveryoneGroup(), this));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groupsActions.addElement(new GuiGroupActionElement(fontRenderer, group, this));
        }
    }

    private boolean canAddNewUser() {
        if (tabSelected == 0 && groups.getSelectedElement() != null) {
            SettingsGroup group = ((GuiGroupElement) groups.getSelectedElement()).getGroup();
            if (!group.isSpecial()) {
                return true;
            }
        }

        return false;
    }
}
