package games.alejandrocoria.mapfrontiers.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.gui.GuiScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRequestFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings.Action;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiFrontierSettings extends GuiScreen implements GuiScrollBox.ScrollBoxResponder,
        GuiGroupActionElement.GroupActionResponder, GuiTabbedBox.TabbedBoxResponder, TextBox.TextBoxResponder {
    public enum Tab {
        Credits, General, Groups, Actions
    }

    private static final int guiTextureSize = 512;

    private ResourceLocation guiTexture;
    private FrontierSettings settings;
    private GuiTabbedBox tabbedBox;
    private GuiLinkButton buttonWeb;
    private GuiLinkButton buttonProject;
    private GuiPatreonButton buttonPatreon;
    private GuiOptionButton buttonAddVertexToNewFrontier;
    private GuiOptionButton buttonAlwaysShowUnfinishedFrontiers;
    private GuiOptionButton buttonNameVisibility;
    private TextBox textPolygonsOpacity;
    private TextBox textSnapDistance;
    private GuiOptionButton buttonHUDEnabled;
    private GuiSettingsButton buttonEditHUD;
    private GuiScrollBox groups;
    private GuiScrollBox users;
    private GuiScrollBox groupsActions;
    private TextBox textNewGroupName;
    private GuiButtonIcon buttonNewGroup;
    private TextUserBox textNewUser;
    private GuiButtonIcon buttonNewUser;
    private TextBox textGroupName;
    private List<GuiSimpleLabel> labels;
    private Map<GuiSimpleLabel, List<String>> labelTooltips;
    private boolean canEditGroups;
    private Tab tabSelected = Tab.Credits;
    private int ticksSinceLastUpdate = 0;
    private int id = 0;

    public GuiFrontierSettings(SettingsProfile profile) {
        guiTexture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/gui.png");
        labels = new ArrayList<GuiSimpleLabel>();
        labelTooltips = new HashMap<GuiSimpleLabel, List<String>>();
        canEditGroups = profile.updateSettings == SettingsProfile.State.Enabled;
    }

    @Override
    public void initGui() {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings());

        Keyboard.enableRepeatEvents(true);

        tabSelected = ((ClientProxy) MapFrontiers.proxy).getLastSettingsTab();

        tabbedBox = new GuiTabbedBox(fontRenderer, 40, 24, width - 80, height - 64, this);
        tabbedBox.addTab(I18n.format("mapfrontiers.credits"));
        tabbedBox.addTab(I18n.format("mapfrontiers.general"));
        if (canEditGroups) {
            tabbedBox.addTab(I18n.format("mapfrontiers.groups"));
            tabbedBox.addTab(I18n.format("mapfrontiers.actions"));
        } else {
            if (tabSelected == Tab.Groups || tabSelected == Tab.Actions) {
                tabSelected = Tab.Credits;
            }
        }
        tabbedBox.setTabSelected(tabSelected.ordinal());

        buttonWeb = new GuiLinkButton(this, ++id, mc.fontRenderer, width / 2, height / 2 - 98, "alejandrocoria.games",
                "https://alejandrocoria.games");

        buttonProject = new GuiLinkButton(this, ++id, mc.fontRenderer, width / 2, height / 2 - 20,
                "curseforge.com/minecraft/mc-mods/mapfrontiers", "https://www.curseforge.com/minecraft/mc-mods/mapfrontiers");

        buttonPatreon = new GuiPatreonButton(this, ++id, width / 2, height / 2 + 36, 212, 50, 0, 462, guiTexture, guiTextureSize,
                "https://www.patreon.com/alejandrocoria");

        buttonAddVertexToNewFrontier = new GuiOptionButton(++id, mc.fontRenderer, width / 2 + 50, 70, 100);
        buttonAddVertexToNewFrontier.addOption("true");
        buttonAddVertexToNewFrontier.addOption("false");
        buttonAddVertexToNewFrontier.setSelected(ConfigData.addVertexToNewFrontier ? 0 : 1);

        buttonAlwaysShowUnfinishedFrontiers = new GuiOptionButton(++id, mc.fontRenderer, width / 2 + 50, 86, 100);
        buttonAlwaysShowUnfinishedFrontiers.addOption("true");
        buttonAlwaysShowUnfinishedFrontiers.addOption("false");
        buttonAlwaysShowUnfinishedFrontiers.setSelected(ConfigData.alwaysShowUnfinishedFrontiers ? 0 : 1);

        buttonNameVisibility = new GuiOptionButton(++id, mc.fontRenderer, width / 2 + 50, 102, 100);
        buttonNameVisibility.addOption(ConfigData.NameVisibility.Manual.name());
        buttonNameVisibility.addOption(ConfigData.NameVisibility.Show.name());
        buttonNameVisibility.addOption(ConfigData.NameVisibility.Hide.name());
        buttonNameVisibility.setSelected(ConfigData.nameVisibility.ordinal());

        textPolygonsOpacity = new TextBox(++id, mc.fontRenderer, width / 2 + 50, 118, 100, "");
        textPolygonsOpacity.setText(String.valueOf(ConfigData.polygonsOpacity));
        textPolygonsOpacity.setMaxStringLength(10);
        textPolygonsOpacity.setResponder(this);
        textPolygonsOpacity.setCentered(false);
        textPolygonsOpacity.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textPolygonsOpacity.setFrame(true);

        textSnapDistance = new TextBox(++id, mc.fontRenderer, width / 2 + 50, 134, 100, "");
        textSnapDistance.setText(String.valueOf(ConfigData.snapDistance));
        textSnapDistance.setMaxStringLength(2);
        textSnapDistance.setResponder(this);
        textSnapDistance.setCentered(false);
        textSnapDistance.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textSnapDistance.setFrame(true);

        buttonHUDEnabled = new GuiOptionButton(++id, mc.fontRenderer, width / 2 + 50, 188, 100);
        buttonHUDEnabled.addOption("true");
        buttonHUDEnabled.addOption("false");
        buttonHUDEnabled.setSelected(ConfigData.hud.enabled ? 0 : 1);

        buttonEditHUD = new GuiSettingsButton(++id, mc.fontRenderer, width / 2 - 50, 208, 100,
                I18n.format("mapfrontiers.edit_hub"));

        groups = new GuiScrollBox(++id, 50, 50, 160, height - 120, 16, this);
        users = new GuiScrollBox(++id, 250, 82, 258, height - 150, 16, this);
        groupsActions = new GuiScrollBox(++id, width / 2 - 215, 82, 430, height - 128, 16, this);

        textNewGroupName = new TextBox(++id, fontRenderer, 50, height - 61, 140, I18n.format("mapfrontiers.new_group_name"));
        textNewGroupName.setMaxStringLength(22);
        textNewGroupName.setResponder(this);
        textNewGroupName.setCentered(false);
        textNewGroupName.setColor(GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textNewGroupName.setFrame(true);

        buttonNewGroup = new GuiButtonIcon(++id, 192, height - 61, 13, 13, 494, 119, -23, guiTexture, guiTextureSize);

        textNewUser = new TextUserBox(++id, mc, fontRenderer, 250, height - 61, 238, I18n.format("mapfrontiers.new_user"));
        textNewUser.setMaxStringLength(38);
        textNewUser.setResponder(this);
        textNewUser.setCentered(false);
        textNewUser.setFrame(true);

        buttonNewUser = new GuiButtonIcon(++id, 490, height - 61, 13, 13, 494, 119, -23, guiTexture, guiTextureSize);

        textGroupName = new TextBox(++id, fontRenderer, 250, 50, 140, I18n.format("mapfrontiers.edit_group_name"));
        textGroupName.setMaxStringLength(22);
        textGroupName.setResponder(this);
        textGroupName.setEnabled(false);
        textGroupName.setCentered(false);
        textGroupName.setColor(GuiColors.SETTINGS_TEXT_HIGHLIGHT);

        buttonList.add(buttonWeb);
        buttonList.add(buttonProject);
        buttonList.add(buttonPatreon);
        buttonList.add(buttonAddVertexToNewFrontier);
        buttonList.add(buttonAlwaysShowUnfinishedFrontiers);
        buttonList.add(buttonNameVisibility);
        buttonList.add(buttonHUDEnabled);
        buttonList.add(buttonEditHUD);
        buttonList.add(buttonNewGroup);
        buttonList.add(buttonNewUser);

        resetLabels();
        updateButtonsVisibility();
    }

    @Override
    public void updateScreen() {
        if (!canEditGroups || settings == null) {
            return;
        }

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

        tabbedBox.drawBox(mc, mouseX, mouseY);
        groups.drawBox(mc, mouseX, mouseY);
        users.drawBox(mc, mouseX, mouseY);
        groupsActions.drawBox(mc, mouseX, mouseY);

        if (tabSelected == Tab.General) {
            textPolygonsOpacity.drawTextBox(mouseX, mouseY);
            textSnapDistance.drawTextBox(mouseX, mouseY);
        } else if (tabSelected == Tab.Groups) {
            textNewGroupName.drawTextBox(mouseX, mouseY);
            if (groups.getSelectedElement() != null) {
                textGroupName.drawTextBox(mouseX, mouseY);
            }
        }

        if (canAddNewUser()) {
            textNewUser.drawTextBox(mouseX, mouseY);
        }

        for (GuiSimpleLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (GuiSimpleLabel label : labels) {
            if (label.isMouseOver()) {
                List<String> tooltip = labelTooltips.get(label);
                if (tooltip == null) {
                    continue;
                }

                GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, width, height, 300, fontRenderer);
            }
        }
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
            textPolygonsOpacity.mouseClicked(x, y, btn);
            textSnapDistance.mouseClicked(x, y, btn);
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
        textPolygonsOpacity.textboxKeyTyped(typedChar, keyCode);
        textSnapDistance.textboxKeyTyped(typedChar, keyCode);
        textNewGroupName.textboxKeyTyped(typedChar, keyCode);
        textNewUser.textboxKeyTyped(typedChar, keyCode);
        textGroupName.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == buttonAddVertexToNewFrontier) {
            ConfigData.addVertexToNewFrontier = buttonAddVertexToNewFrontier.getSelected() == 0;
            MapFrontiers.proxy.configUpdated();
        } else if (button == buttonAlwaysShowUnfinishedFrontiers) {
            ConfigData.alwaysShowUnfinishedFrontiers = buttonAlwaysShowUnfinishedFrontiers.getSelected() == 0;
            MapFrontiers.proxy.configUpdated();
        } else if (button == buttonNameVisibility) {
            ConfigData.nameVisibility = ConfigData.NameVisibility.values()[buttonNameVisibility.getSelected()];
            MapFrontiers.proxy.configUpdated();
        } else if (button == buttonHUDEnabled) {
            ConfigData.hud.enabled = buttonHUDEnabled.getSelected() == 0;
            buttonEditHUD.visible = ConfigData.hud.enabled;
            MapFrontiers.proxy.configUpdated();
        } else if (button == buttonEditHUD) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiHUDSettings(this));
        } else if (button == buttonNewGroup) {
            if (settings != null) {
                SettingsGroup group = settings.createCustomGroup(textNewGroupName.getText());
                GuiGroupElement element = new GuiGroupElement(fontRenderer, buttonList, id, group, guiTexture, guiTextureSize);
                groups.addElement(element);
                groupClicked(element);
                groups.scrollBottom();
                groupsActions.scrollBottom();

                textNewGroupName.setText("");

                sendChangesToServer();
            }
        } else if (button == buttonNewUser) {
            SettingsGroup group = ((GuiGroupElement) groups.getSelectedElement()).getGroup();
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

            if (group.hasUser(user)) {
                textNewUser.setError(I18n.format("mapfrontiers.new_user_error_user_repeated"));
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
        Keyboard.enableRepeatEvents(false);
        ((ClientProxy) MapFrontiers.proxy).setLastSettingsTab(tabSelected);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result) {
            if (id == buttonWeb.id) {
                buttonWeb.openLink();
            } else if (id == buttonProject.id) {
                buttonProject.openLink();
            } else if (id == buttonPatreon.id) {
                buttonPatreon.openLink();
            } else {
                super.confirmClicked(result, id);
                return;
            }
        }

        mc.displayGuiScreen(this);
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

    public void updateSettingsProfile(SettingsProfile profile) {
        if ((profile.updateSettings == SettingsProfile.State.Enabled) == canEditGroups) {
            return;
        }

        mc.displayGuiScreen(new GuiFrontierSettings(profile));
    }

    private void resetLabels() {
        labels.clear();
        labelTooltips.clear();

        if (tabSelected == Tab.Credits) {
            labels.add(new GuiSimpleLabel(fontRenderer, width / 2, height / 2 - 106, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.credits_created_by"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(fontRenderer, width / 2, height / 2 - 58, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.credits_many_thanks"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(fontRenderer, width / 2, height / 2 - 28, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.credits_project"), GuiColors.SETTINGS_TEXT_MEDIUM));
            labels.add(new GuiSimpleLabel(fontRenderer, width / 2, height / 2 + 22, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.credits_patreon"), GuiColors.SETTINGS_TEXT_MEDIUM));
            labels.add(new GuiSimpleLabel(fontRenderer, 50, height - 54, GuiSimpleLabel.Align.Left,
                    I18n.format("mapfrontiers.credits_translation"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(fontRenderer, width - 48, height - 54, GuiSimpleLabel.Align.Right,
                    I18n.format("@VERSION@"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
        } else if (tabSelected == Tab.General) {
            labels.add(new GuiSimpleLabel(fontRenderer, width / 2, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.frontiers"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            addLabelWithTooltip(new GuiSimpleLabel(fontRenderer, width / 2 - 120, 72, GuiSimpleLabel.Align.Left,
                    "addVertexToNewFrontier", GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("addVertexToNewFrontier"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(fontRenderer, width / 2 - 120, 88, GuiSimpleLabel.Align.Left,
                            "alwaysShowUnfinishedFrontiers", GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("alwaysShowUnfinishedFrontiers"));
            addLabelWithTooltip(new GuiSimpleLabel(fontRenderer, width / 2 - 120, 104, GuiSimpleLabel.Align.Left,
                    "nameVisibility", GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("nameVisibility"));
            addLabelWithTooltip(new GuiSimpleLabel(fontRenderer, width / 2 - 120, 120, GuiSimpleLabel.Align.Left,
                    "polygonsOpacity", GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("polygonsOpacity"));
            addLabelWithTooltip(new GuiSimpleLabel(fontRenderer, width / 2 - 120, 136, GuiSimpleLabel.Align.Left, "snapDistance",
                    GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("snapDistance"));
            labels.add(new GuiSimpleLabel(fontRenderer, width / 2, 170, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.hud"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            addLabelWithTooltip(new GuiSimpleLabel(fontRenderer, width / 2 - 120, 190, GuiSimpleLabel.Align.Left, "enabled",
                    GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.enabled"));
        } else if (tabSelected == Tab.Groups) {
            if (settings != null) {
                GuiGroupElement element = (GuiGroupElement) groups.getSelectedElement();
                if (element != null && element.getGroup().isSpecial()) {
                    SettingsGroup group = element.getGroup();
                    if (group == settings.getOPsGroup()) {
                        labels.add(new GuiSimpleLabel(fontRenderer, 250, 82, GuiSimpleLabel.Align.Left,
                                I18n.format("mapfrontiers.group_ops_desc"), GuiColors.SETTINGS_TEXT));
                    } else if (group == settings.getOwnersGroup()) {
                        labels.add(new GuiSimpleLabel(fontRenderer, 250, 82, GuiSimpleLabel.Align.Left,
                                I18n.format("mapfrontiers.group_owners_desc"), GuiColors.SETTINGS_TEXT));
                    } else if (group == settings.getEveryoneGroup()) {
                        labels.add(new GuiSimpleLabel(fontRenderer, 250, 82, GuiSimpleLabel.Align.Left,
                                I18n.format("mapfrontiers.group_everyone_desc"), GuiColors.SETTINGS_TEXT));
                    }
                }
            }
        } else if (tabSelected == Tab.Actions) {
            int x = width / 2 - 55;
            labels.add(new GuiSimpleLabel(fontRenderer, x, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.create_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 60, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.delete_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 120, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.update_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 180, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.update_settings"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 240, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.personal_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
        }
    }

    private void addLabelWithTooltip(GuiSimpleLabel label, @Nullable List<String> tooltip) {
        labels.add(label);
        if (tooltip != null) {
            labelTooltips.put(label, tooltip);
        }
    }

    private void updateButtonsVisibility() {
        buttonWeb.visible = tabSelected == Tab.Credits;
        buttonProject.visible = tabSelected == Tab.Credits;
        buttonPatreon.visible = tabSelected == Tab.Credits;
        buttonAddVertexToNewFrontier.visible = tabSelected == Tab.General;
        buttonAlwaysShowUnfinishedFrontiers.visible = tabSelected == Tab.General;
        buttonNameVisibility.visible = tabSelected == Tab.General;
        buttonHUDEnabled.visible = tabSelected == Tab.General;
        buttonEditHUD.visible = tabSelected == Tab.General && ConfigData.hud.enabled;
        groups.visible = tabSelected == Tab.Groups;
        users.visible = tabSelected == Tab.Groups;
        buttonNewGroup.visible = tabSelected == Tab.Groups;
        buttonNewUser.visible = canAddNewUser();
        groupsActions.visible = tabSelected == Tab.Actions;
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
        if (settings != null) {
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
        tabSelected = Tab.values()[tab];

        if (tabSelected == Tab.Actions) {
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
        if (textPolygonsOpacity.getId() == id && tabSelected == Tab.General) {
            if (StringUtils.isBlank(value)) {
                textPolygonsOpacity.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                textPolygonsOpacity.setText(ConfigData.getDefault("polygonsOpacity"));
                ConfigData.polygonsOpacity = Double.valueOf(textPolygonsOpacity.getText());
                MapFrontiers.proxy.configUpdated();
            } else {
                try {
                    textPolygonsOpacity.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
                    Double opacity = Double.valueOf(value);
                    if (ConfigData.isInRange("polygonsOpacity", opacity)) {
                        ConfigData.polygonsOpacity = opacity;
                        MapFrontiers.proxy.configUpdated();
                        textPolygonsOpacity.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                    }
                } catch (Exception e) {
                }
            }
        } else if (textSnapDistance.getId() == id && tabSelected == Tab.General) {
            if (StringUtils.isBlank(value)) {
                textSnapDistance.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                textSnapDistance.setText(ConfigData.getDefault("snapDistance"));
                ConfigData.snapDistance = Integer.valueOf(textSnapDistance.getText());
                MapFrontiers.proxy.configUpdated();
            } else {
                try {
                    textSnapDistance.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
                    Integer distance = Integer.valueOf(value);
                    if (ConfigData.isInRange("snapDistance", distance)) {
                        ConfigData.snapDistance = distance;
                        MapFrontiers.proxy.configUpdated();
                        textSnapDistance.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                    }
                } catch (Exception e) {
                }
            }
        } else if (textGroupName.getId() == id && tabSelected == Tab.Groups) {
            GuiGroupElement groupElement = (GuiGroupElement) groups.getSelectedElement();
            if (groupElement != null) {
                groupElement.getGroup().setName(value);
                sendChangesToServer();
            }
        }
    }

    private void sendChangesToServer() {
        if (settings != null) {
            settings.advanceChangeCounter();
            PacketHandler.INSTANCE.sendToServer(new PacketFrontierSettings(settings));
        }
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
        if (settings != null) {
            groupsActions.removeAll();
            groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getOPsGroup(), this));
            groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getOwnersGroup(), true, this));
            groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getEveryoneGroup(), this));

            for (SettingsGroup group : settings.getCustomGroups()) {
                groupsActions.addElement(new GuiGroupActionElement(fontRenderer, group, this));
            }
        }
    }

    private boolean canAddNewUser() {
        if (tabSelected == Tab.Groups && groups.getSelectedElement() != null) {
            SettingsGroup group = ((GuiGroupElement) groups.getSelectedElement()).getGroup();
            if (!group.isSpecial()) {
                return true;
            }
        }

        return false;
    }
}
