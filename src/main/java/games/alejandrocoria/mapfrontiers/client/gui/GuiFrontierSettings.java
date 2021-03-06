package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.matrix.MatrixStack;

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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.gui.GuiUtils;
import org.lwjgl.glfw.GLFW;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierSettings extends Screen implements GuiScrollBox.ScrollBoxResponder,
        GuiGroupActionElement.GroupActionResponder, GuiTabbedBox.TabbedBoxResponder, TextBox.TextBoxResponder {
    public enum Tab {
        Credits, General, Groups, Actions
    }

    private static final int guiTextureSize = 512;

    private final ResourceLocation guiTexture;
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
    private final List<GuiSimpleLabel> labels;
    private final Map<GuiSimpleLabel, List<ITextComponent>> labelTooltips;
    private final boolean canEditGroups;
    private Tab tabSelected = Tab.Credits;
    private int ticksSinceLastUpdate = 0;

    public GuiFrontierSettings(SettingsProfile profile) {
        super(StringTextComponent.EMPTY);
        guiTexture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/gui.png");
        labels = new ArrayList<>();
        labelTooltips = new HashMap<>();
        canEditGroups = profile.updateSettings == SettingsProfile.State.Enabled;
    }

    @Override
    public void init() {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings());

        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        tabSelected = ClientProxy.getLastSettingsTab();

        tabbedBox = new GuiTabbedBox(font, 40, 24, width - 80, height - 64, this);
        tabbedBox.addTab(new TranslationTextComponent("mapfrontiers.credits"));
        tabbedBox.addTab(new TranslationTextComponent("mapfrontiers.general"));
        if (canEditGroups) {
            tabbedBox.addTab(new TranslationTextComponent("mapfrontiers.groups"));
            tabbedBox.addTab(new TranslationTextComponent("mapfrontiers.actions"));
        } else {
            if (tabSelected == Tab.Groups || tabSelected == Tab.Actions) {
                tabSelected = Tab.Credits;
            }
        }
        tabbedBox.setTabSelected(tabSelected.ordinal());

        buttonWeb = new GuiLinkButton(font, width / 2, height / 2 - 98, new StringTextComponent("alejandrocoria.games"),
                "https://alejandrocoria.games", (open) -> linkClicked(open, buttonWeb));

        buttonProject = new GuiLinkButton(font, width / 2, height / 2 - 20,
                new StringTextComponent("curseforge.com/minecraft/mc-mods/mapfrontiers"),
                "https://www.curseforge.com/minecraft/mc-mods/mapfrontiers", (open) -> linkClicked(open, buttonProject));

        buttonPatreon = new GuiPatreonButton(width / 2, height / 2 + 36, 212, 50, 0, 462, guiTexture, guiTextureSize,
                "https://www.patreon.com/alejandrocoria", (open) -> linkClicked(open, buttonPatreon));

        buttonAddVertexToNewFrontier = new GuiOptionButton(font, width / 2 + 50, 70, 100, this::buttonPressed);
        buttonAddVertexToNewFrontier.addOption("true");
        buttonAddVertexToNewFrontier.addOption("false");
        buttonAddVertexToNewFrontier.setSelected(ConfigData.addVertexToNewFrontier ? 0 : 1);

        buttonAlwaysShowUnfinishedFrontiers = new GuiOptionButton(font, width / 2 + 50, 86, 100, this::buttonPressed);
        buttonAlwaysShowUnfinishedFrontiers.addOption("true");
        buttonAlwaysShowUnfinishedFrontiers.addOption("false");
        buttonAlwaysShowUnfinishedFrontiers.setSelected(ConfigData.alwaysShowUnfinishedFrontiers ? 0 : 1);

        buttonNameVisibility = new GuiOptionButton(font, width / 2 + 50, 102, 100, this::buttonPressed);
        buttonNameVisibility.addOption(ConfigData.NameVisibility.Manual.name());
        buttonNameVisibility.addOption(ConfigData.NameVisibility.Show.name());
        buttonNameVisibility.addOption(ConfigData.NameVisibility.Hide.name());
        buttonNameVisibility.setSelected(ConfigData.nameVisibility.ordinal());

        textPolygonsOpacity = new TextBox(font, width / 2 + 50, 118, 100, "");
        textPolygonsOpacity.setValue(String.valueOf(ConfigData.polygonsOpacity));
        textPolygonsOpacity.setMaxLength(10);
        textPolygonsOpacity.setResponder(this);
        textPolygonsOpacity.setCentered(false);
        textPolygonsOpacity.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textPolygonsOpacity.setFrame(true);

        textSnapDistance = new TextBox(font, width / 2 + 50, 134, 100, "");
        textSnapDistance.setValue(String.valueOf(ConfigData.snapDistance));
        textSnapDistance.setMaxLength(2);
        textSnapDistance.setResponder(this);
        textSnapDistance.setCentered(false);
        textSnapDistance.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textSnapDistance.setFrame(true);

        buttonHUDEnabled = new GuiOptionButton(font, width / 2 + 50, 188, 100, this::buttonPressed);
        buttonHUDEnabled.addOption("true");
        buttonHUDEnabled.addOption("false");
        buttonHUDEnabled.setSelected(ConfigData.hudEnabled ? 0 : 1);

        buttonEditHUD = new GuiSettingsButton(font, width / 2 - 50, 208, 100,
                new TranslationTextComponent("mapfrontiers.edit_hud"), this::buttonPressed);

        groups = new GuiScrollBox(50, 50, 160, height - 120, 16, this);
        users = new GuiScrollBox(250, 82, 258, height - 150, 16, this);
        groupsActions = new GuiScrollBox(width / 2 - 215, 82, 430, height - 128, 16, this);

        textNewGroupName = new TextBox(font, 50, height - 61, 140, I18n.get("mapfrontiers.new_group_name"));
        textNewGroupName.setMaxLength(22);
        textNewGroupName.setResponder(this);
        textNewGroupName.setCentered(false);
        textNewGroupName.setColor(GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textNewGroupName.setFrame(true);

        buttonNewGroup = new GuiButtonIcon(192, height - 61, 13, 13, 494, 119, -23, guiTexture, guiTextureSize,
                this::buttonPressed);

        textNewUser = new TextUserBox(minecraft, font, 250, height - 61, 238, I18n.get("mapfrontiers.new_user"));
        textNewUser.setMaxLength(38);
        textNewUser.setResponder(this);
        textNewUser.setCentered(false);
        textNewUser.setFrame(true);

        buttonNewUser = new GuiButtonIcon(490, height - 61, 13, 13, 494, 119, -23, guiTexture, guiTextureSize,
                this::buttonPressed);

        textGroupName = new TextBox(font, 250, 50, 140, I18n.get("mapfrontiers.edit_group_name"));
        textGroupName.setMaxLength(22);
        textGroupName.setResponder(this);
        textGroupName.active = false;
        textGroupName.setCentered(false);
        textGroupName.setColor(GuiColors.SETTINGS_TEXT_HIGHLIGHT);

        addButton(tabbedBox);
        addButton(buttonWeb);
        addButton(buttonProject);
        addButton(buttonPatreon);
        addButton(buttonAddVertexToNewFrontier);
        addButton(buttonAlwaysShowUnfinishedFrontiers);
        addButton(buttonNameVisibility);
        addButton(textPolygonsOpacity);
        addButton(textSnapDistance);
        addButton(buttonHUDEnabled);
        addButton(buttonEditHUD);
        addButton(groups);
        addButton(users);
        addButton(groupsActions);
        addButton(textNewGroupName);
        addButton(buttonNewGroup);
        addButton(textNewUser);
        addButton(buttonNewUser);
        addButton(textGroupName);

        resetLabels();
        updateButtonsVisibility();
    }

    @Override
    public void tick() {
        if (!canEditGroups || settings == null) {
            return;
        }

        ++ticksSinceLastUpdate;

        if (ticksSinceLastUpdate >= 100) {
            ticksSinceLastUpdate = 0;
            PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings(settings.getChangeCounter()));

            ClientPlayNetHandler handler = minecraft.getConnection();
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
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);

        for (GuiSimpleLabel label : labels) {
            label.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        for (GuiSimpleLabel label : labels) {
            if (label.isHovered()) {
                List<ITextComponent> tooltip = labelTooltips.get(label);
                if (tooltip == null) {
                    continue;
                }

                GuiUtils.drawHoveringText(matrixStack, tooltip, mouseX, mouseY, width, height, 300, font);
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_E && !(getFocused() instanceof TextFieldWidget)) {
            onClose();
            return true;
        } else {
            return super.keyPressed(key, value, modifier);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Widget w : buttons) {
            if (w instanceof GuiScrollBox) {
                ((GuiScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonAddVertexToNewFrontier) {
            ConfigData.addVertexToNewFrontier = buttonAddVertexToNewFrontier.getSelected() == 0;
            ClientProxy.configUpdated();
        } else if (button == buttonAlwaysShowUnfinishedFrontiers) {
            ConfigData.alwaysShowUnfinishedFrontiers = buttonAlwaysShowUnfinishedFrontiers.getSelected() == 0;
            ClientProxy.configUpdated();
        } else if (button == buttonNameVisibility) {
            ConfigData.nameVisibility = ConfigData.NameVisibility.values()[buttonNameVisibility.getSelected()];
            ClientProxy.configUpdated();
        } else if (button == buttonHUDEnabled) {
            ConfigData.hudEnabled = buttonHUDEnabled.getSelected() == 0;
            buttonEditHUD.visible = ConfigData.hudEnabled;
            ClientProxy.configUpdated();
        } else if (button == buttonEditHUD) {
            minecraft.setScreen(new GuiHUDSettings(this));
        } else if (button == buttonNewGroup) {
            if (settings != null) {
                SettingsGroup group = settings.createCustomGroup(textNewGroupName.getValue());
                GuiGroupElement element = new GuiGroupElement(font, buttons, group, guiTexture, guiTextureSize);
                groups.addElement(element);
                groupClicked(element);
                groups.scrollBottom();
                groupsActions.scrollBottom();

                textNewGroupName.setValue("");

                sendChangesToServer();
            }
        } else if (button == buttonNewUser) {
            SettingsGroup group = ((GuiGroupElement) groups.getSelectedElement()).getGroup();
            SettingsUser user = new SettingsUser();

            String usernameOrUUID = textNewUser.getValue();
            if (StringUtils.isBlank(usernameOrUUID)) {
                return;
            } else if (usernameOrUUID.length() < 28) {
                user.username = usernameOrUUID;
                user.fillMissingInfo(false);
            } else {
                usernameOrUUID = usernameOrUUID.replaceAll("[^0-9a-fA-F]", "");
                if (usernameOrUUID.length() != 32) {
                    textNewUser.setError(new TranslationTextComponent("mapfrontiers.new_user_error_uuid_size"));
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
                    textNewUser.setError(new TranslationTextComponent("mapfrontiers.new_user_error_uuid_format"));
                    return;
                }
            }

            if (group.hasUser(user)) {
                textNewUser.setError(new TranslationTextComponent("mapfrontiers.new_user_error_user_repeated"));
                return;
            }

            group.addUser(user);
            GuiUserElement element = new GuiUserElement(font, buttons, user, guiTexture, guiTextureSize);
            users.addElement(element);
            users.scrollBottom();

            textNewUser.setValue("");

            sendChangesToServer();
        }
    }

    @Override
    public void onClose() {
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
        ClientProxy.setLastSettingsTab(tabSelected);
        super.onClose();
    }

    public void linkClicked(boolean open, Widget widget) {
        if (open) {
            if (widget == buttonWeb) {
                buttonWeb.openLink();
            } else if (widget == buttonProject) {
                buttonProject.openLink();
            } else if (widget == buttonPatreon) {
                buttonPatreon.openLink();
            } else {
                return;
            }
        }

        minecraft.setScreen(this);
    }

    public void setFrontierSettings(FrontierSettings settings) {
        this.settings = settings;

        groups.removeAll();
        groups.addElement(new GuiGroupElement(font, buttons, settings.getOPsGroup(), guiTexture, guiTextureSize));
        groups.addElement(new GuiGroupElement(font, buttons, settings.getOwnersGroup(), guiTexture, guiTextureSize));
        groups.addElement(new GuiGroupElement(font, buttons, settings.getEveryoneGroup(), guiTexture, guiTextureSize));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groups.addElement(new GuiGroupElement(font, buttons, group, guiTexture, guiTextureSize));
        }

        updateGroupsActions();

        resetLabels();
        updateButtonsVisibility();
    }

    public void updateSettingsProfile(SettingsProfile profile) {
        if ((profile.updateSettings == SettingsProfile.State.Enabled) == canEditGroups) {
            return;
        }

        minecraft.setScreen(new GuiFrontierSettings(profile));
    }

    private void resetLabels() {
        labels.clear();
        labelTooltips.clear();

        if (tabSelected == Tab.Credits) {
            labels.add(new GuiSimpleLabel(font, width / 2, height / 2 - 106, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.credits_created_by"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, width / 2, height / 2 - 58, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.credits_many_thanks"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, width / 2, height / 2 - 28, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.credits_project"), GuiColors.SETTINGS_TEXT_MEDIUM));
            labels.add(new GuiSimpleLabel(font, width / 2, height / 2 + 22, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.credits_patreon"), GuiColors.SETTINGS_TEXT_MEDIUM));
            labels.add(new GuiSimpleLabel(font, 50, height - 54, GuiSimpleLabel.Align.Left,
                    new TranslationTextComponent("mapfrontiers.credits_translation"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, width - 48, height - 54, GuiSimpleLabel.Align.Right,
                    new TranslationTextComponent(MapFrontiers.VERSION), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
        } else if (tabSelected == Tab.General) {
            labels.add(new GuiSimpleLabel(font, width / 2, 54, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.frontiers"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 72, GuiSimpleLabel.Align.Left,
                            new TranslationTextComponent("addVertexToNewFrontier"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("addVertexToNewFrontier"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 88, GuiSimpleLabel.Align.Left,
                            new TranslationTextComponent("alwaysShowUnfinishedFrontiers"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("alwaysShowUnfinishedFrontiers"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 104, GuiSimpleLabel.Align.Left,
                            new TranslationTextComponent("nameVisibility"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("nameVisibility"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 120, GuiSimpleLabel.Align.Left,
                            new TranslationTextComponent("polygonsOpacity"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("polygonsOpacity"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 136, GuiSimpleLabel.Align.Left,
                            new TranslationTextComponent("snapDistance"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("snapDistance"));
            labels.add(new GuiSimpleLabel(font, width / 2, 170, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.hud"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 190, GuiSimpleLabel.Align.Left,
                            new TranslationTextComponent("enabled"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("hud.enabled"));
        } else if (tabSelected == Tab.Groups) {
            if (settings != null) {
                GuiGroupElement element = (GuiGroupElement) groups.getSelectedElement();
                if (element != null && element.getGroup().isSpecial()) {
                    SettingsGroup group = element.getGroup();
                    if (group == settings.getOPsGroup()) {
                        labels.add(new GuiSimpleLabel(font, 250, 82, GuiSimpleLabel.Align.Left,
                                new TranslationTextComponent("mapfrontiers.group_ops_desc"), GuiColors.SETTINGS_TEXT));
                    } else if (group == settings.getOwnersGroup()) {
                        labels.add(new GuiSimpleLabel(font, 250, 82, GuiSimpleLabel.Align.Left,
                                new TranslationTextComponent("mapfrontiers.group_owners_desc"), GuiColors.SETTINGS_TEXT));
                    } else if (group == settings.getEveryoneGroup()) {
                        labels.add(new GuiSimpleLabel(font, 250, 82, GuiSimpleLabel.Align.Left,
                                new TranslationTextComponent("mapfrontiers.group_everyone_desc"), GuiColors.SETTINGS_TEXT));
                    }
                }
            }
        } else if (tabSelected == Tab.Actions) {
            int x = width / 2 - 55;
            labels.add(new GuiSimpleLabel(font, x, 54, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.create_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 60, 54, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.delete_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 120, 54, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.update_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 180, 54, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.update_settings"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 240, 54, GuiSimpleLabel.Align.Center,
                    new TranslationTextComponent("mapfrontiers.personal_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
        }
    }

    private void addLabelWithTooltip(GuiSimpleLabel label, @Nullable List<ITextComponent> tooltip) {
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
        textPolygonsOpacity.visible = tabSelected == Tab.General;
        textSnapDistance.visible = tabSelected == Tab.General;
        buttonHUDEnabled.visible = tabSelected == Tab.General;
        buttonEditHUD.visible = tabSelected == Tab.General && ConfigData.hudEnabled;
        groups.visible = tabSelected == Tab.Groups;
        users.visible = tabSelected == Tab.Groups;
        groupsActions.visible = tabSelected == Tab.Actions;
        textNewGroupName.visible = tabSelected == Tab.Groups;
        buttonNewGroup.visible = tabSelected == Tab.Groups;
        textNewUser.visible = canAddNewUser();
        buttonNewUser.visible = canAddNewUser();
        textGroupName.visible = tabSelected == Tab.Groups && groups.getSelectedElement() != null;
    }

    public void groupClicked(GuiGroupElement element) {
        groups.selectElement(element);
        textGroupName.setValue(element.getGroup().getName());
        textGroupName.active = !element.getGroup().isSpecial();

        resetLabels();
        updateUsers();
    }

    @Override
    public void elementClicked(GuiScrollBox scrollBox, ScrollElement element) {
        if (scrollBox == groups) {
            GuiGroupElement group = (GuiGroupElement) element;
            textGroupName.setValue(group.getGroup().getName());
            textGroupName.active = !group.getGroup().isSpecial();
            resetLabels();
            updateUsers();
            updateButtonsVisibility();
        }
    }

    @Override
    public void elementDelete(GuiScrollBox scrollBox, ScrollElement element) {
        if (settings != null) {
            if (scrollBox == groups) {
                if (groups.getSelectedElement() != null) {
                    groupClicked((GuiGroupElement) groups.getSelectedElement());
                }
                settings.removeCustomGroup(((GuiGroupElement) element).getGroup());
                sendChangesToServer();
            } else if (scrollBox == users) {
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
    public void updatedValue(TextBox textBox, String value) {
    }

    @Override
    public void lostFocus(TextBox textBox, String value) {
        if (textPolygonsOpacity == textBox && tabSelected == Tab.General) {
            if (StringUtils.isBlank(value)) {
                textPolygonsOpacity.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                textPolygonsOpacity.setValue(ConfigData.getDefault("polygonsOpacity"));
                ConfigData.polygonsOpacity = Double.parseDouble(textPolygonsOpacity.getValue());
                ClientProxy.configUpdated();
            } else {
                try {
                    textPolygonsOpacity.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
                    Double opacity = Double.valueOf(value);
                    if (ConfigData.isInRange("polygonsOpacity", opacity)) {
                        ConfigData.polygonsOpacity = opacity;
                        ClientProxy.configUpdated();
                        textPolygonsOpacity.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                    }
                } catch (Exception ignored) {
                }
            }
        } else if (textSnapDistance == textBox && tabSelected == Tab.General) {
            if (StringUtils.isBlank(value)) {
                textSnapDistance.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                textSnapDistance.setValue(ConfigData.getDefault("snapDistance"));
                ConfigData.snapDistance = Integer.parseInt(textSnapDistance.getValue());
                ClientProxy.configUpdated();
            } else {
                try {
                    textSnapDistance.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
                    Integer distance = Integer.valueOf(value);
                    if (ConfigData.isInRange("snapDistance", distance)) {
                        ConfigData.snapDistance = distance;
                        ClientProxy.configUpdated();
                        textSnapDistance.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                    }
                } catch (Exception ignored) {
                }
            }
        } else if (textGroupName == textBox && tabSelected == Tab.Groups) {
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
                users.addElement(new GuiUserElement(font, buttons, user, guiTexture, guiTextureSize));
            }
        }

        buttonNewUser.visible = canAddNewUser();
        textNewUser.visible = canAddNewUser();
    }

    private void updateGroupsActions() {
        if (settings != null) {
            groupsActions.removeAll();
            groupsActions.addElement(new GuiGroupActionElement(font, settings.getOPsGroup(), this));
            groupsActions.addElement(new GuiGroupActionElement(font, settings.getOwnersGroup(), true, this));
            groupsActions.addElement(new GuiGroupActionElement(font, settings.getEveryoneGroup(), this));

            for (SettingsGroup group : settings.getCustomGroups()) {
                groupsActions.addElement(new GuiGroupActionElement(font, group, this));
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
