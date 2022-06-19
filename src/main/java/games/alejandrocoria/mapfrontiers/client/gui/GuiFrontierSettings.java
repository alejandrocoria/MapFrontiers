package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.gui.GuiScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRequestFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings.Action;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierSettings extends Screen implements GuiScrollBox.ScrollBoxResponder,
        GuiGroupActionElement.GroupActionResponder, GuiTabbedBox.TabbedBoxResponder, TextBox.TextBoxResponder,
        TextIntBox.TextIntBoxResponder, TextDoubleBox.TextDoubleBoxResponder {
    public enum Tab {
        Credits, General, Groups, Actions
    }

    private FrontierSettings settings;
    private GuiTabbedBox tabbedBox;
    private GuiLinkButton buttonWeb;
    private GuiLinkButton buttonProject;
    private GuiPatreonButton buttonPatreon;
    private GuiOptionButton buttonNameVisibility;
    private GuiOptionButton buttonHideNamesThatDontFit;
    private TextDoubleBox textPolygonsOpacity;
    private TextIntBox textSnapDistance;
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
    private GuiSettingsButton buttonDone;

    private final List<GuiSimpleLabel> labels;
    private final Map<GuiSimpleLabel, List<Component>> labelTooltips;
    private boolean canEditGroups;
    private Tab tabSelected;
    private int ticksSinceLastUpdate = 0;

    public GuiFrontierSettings() {
        super(CommonComponents.EMPTY);
        labels = new ArrayList<>();
        labelTooltips = new HashMap<>();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings());
        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        Component title = Component.translatable("mapfrontiers.title_settings");
        addRenderableOnly(new GuiSimpleLabel(font, width / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        canEditGroups = ClientProxy.getSettingsProfile().updateSettings == SettingsProfile.State.Enabled;

        if (tabSelected == null) {
            tabSelected = ClientProxy.getLastSettingsTab();
        }

        tabbedBox = new GuiTabbedBox(font, 40, 24, width - 80, height - 64, this);
        tabbedBox.addTab(Component.translatable("mapfrontiers.credits"), true);
        tabbedBox.addTab(Component.translatable("mapfrontiers.general"), true);
        tabbedBox.addTab(Component.translatable("mapfrontiers.groups"), canEditGroups);
        tabbedBox.addTab(Component.translatable("mapfrontiers.actions"), canEditGroups);
        if (!canEditGroups) {
            if (tabSelected == Tab.Groups || tabSelected == Tab.Actions) {
                tabSelected = Tab.Credits;
            }
        }
        tabbedBox.setTabSelected(tabSelected.ordinal());

        buttonWeb = new GuiLinkButton(font, width / 2, height / 2 - 98, Component.literal("alejandrocoria.games"),
                "https://alejandrocoria.games", (open) -> linkClicked(open, buttonWeb));

        buttonProject = new GuiLinkButton(font, width / 2, height / 2 - 20,
                Component.literal("curseforge.com/minecraft/mc-mods/mapfrontiers"),
                "https://www.curseforge.com/minecraft/mc-mods/mapfrontiers", (open) -> linkClicked(open, buttonProject));

        buttonPatreon = new GuiPatreonButton(width / 2, height / 2 + 36, "https://www.patreon.com/alejandrocoria",
                (open) -> linkClicked(open, buttonPatreon));

        buttonNameVisibility = new GuiOptionButton(font, width / 2 + 50, 70, 100, this::buttonPressed);
        buttonNameVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.NameVisibility.Manual));
        buttonNameVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.NameVisibility.Show));
        buttonNameVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.NameVisibility.Hide));
        buttonNameVisibility.setSelected(ConfigData.nameVisibility.ordinal());

        buttonHideNamesThatDontFit = new GuiOptionButton(font, width / 2 + 50, 86, 100, this::buttonPressed);
        buttonHideNamesThatDontFit.addOption(Component.translatable("options.on"));
        buttonHideNamesThatDontFit.addOption(Component.translatable("options.off"));
        buttonHideNamesThatDontFit.setSelected(ConfigData.hideNamesThatDontFit ? 0 : 1);

        textPolygonsOpacity = new TextDoubleBox(0.4, 0.0, 1.0, font, width / 2 + 50, 102, 100);
        textPolygonsOpacity.setValue(String.valueOf(ConfigData.polygonsOpacity));
        textPolygonsOpacity.setMaxLength(10);
        textPolygonsOpacity.setResponder(this);

        textSnapDistance = new TextIntBox(8, 0, 16, font, width / 2 + 50, 118, 100);
        textSnapDistance.setValue(String.valueOf(ConfigData.snapDistance));
        textSnapDistance.setMaxLength(2);
        textSnapDistance.setResponder(this);

        buttonHUDEnabled = new GuiOptionButton(font, width / 2 + 50, 172, 100, this::buttonPressed);
        buttonHUDEnabled.addOption(Component.translatable("options.on"));
        buttonHUDEnabled.addOption(Component.translatable("options.off"));
        buttonHUDEnabled.setSelected(ConfigData.hudEnabled ? 0 : 1);

        buttonEditHUD = new GuiSettingsButton(font, width / 2 - 50, 192, 100,
                Component.translatable("mapfrontiers.edit_hud"), this::buttonPressed);

        groups = new GuiScrollBox(50, 50, 160, height - 120, 16, this);
        users = new GuiScrollBox(250, 82, 258, height - 150, 16, this);
        groupsActions = new GuiScrollBox(width / 2 - 215, 82, 430, height - 128, 16, this);

        textNewGroupName = new TextBox(font, 50, height - 61, 140,
                I18n.get("mapfrontiers.new_group_name"));
        textNewGroupName.setMaxLength(22);
        textNewGroupName.setResponder(this);

        buttonNewGroup = new GuiButtonIcon(192, height - 61, GuiButtonIcon.Type.Add, this::buttonPressed);

        textNewUser = new TextUserBox(minecraft, font, 250, height - 61, 238,
                I18n.get("mapfrontiers.new_user"));
        textNewUser.setMaxLength(38);
        textNewUser.setResponder(this);

        buttonNewUser = new GuiButtonIcon(490, height - 61, GuiButtonIcon.Type.Add, this::buttonPressed);

        textGroupName = new TextBox(font, 250, 50, 140);
        textGroupName.setMaxLength(22);
        textGroupName.setResponder(this);
        textGroupName.setEditable(false);
        textGroupName.setBordered(false);

        buttonDone = new GuiSettingsButton(font, width / 2 - 70, height - 28, 140,
                Component.translatable("gui.done"), this::buttonPressed);

        addRenderableWidget(tabbedBox);
        addRenderableWidget(buttonWeb);
        addRenderableWidget(buttonProject);
        addRenderableWidget(buttonPatreon);
        addRenderableWidget(buttonNameVisibility);
        addRenderableWidget(buttonHideNamesThatDontFit);
        addRenderableWidget(textPolygonsOpacity);
        addRenderableWidget(textSnapDistance);
        addRenderableWidget(buttonHUDEnabled);
        addRenderableWidget(buttonEditHUD);
        addRenderableWidget(groups);
        addRenderableWidget(users);
        addRenderableWidget(groupsActions);
        addRenderableWidget(textNewGroupName);
        addRenderableWidget(buttonNewGroup);
        addRenderableWidget(textNewUser);
        addRenderableWidget(buttonNewUser);
        addRenderableWidget(textGroupName);
        addRenderableWidget(buttonDone);

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

            ClientPacketListener handler = minecraft.getConnection();
            if (handler == null) {
                return;
            }

            for (ScrollElement element : users.getElements()) {
                GuiUserElement userElement = (GuiUserElement) element;
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

        for (GuiSimpleLabel label : labels) {
            if (label.isHoveredOrFocused()) {
                List<Component> tooltip = labelTooltips.get(label);
                if (tooltip != null) {
                    renderTooltip(matrixStack, tooltip, Optional.empty(), mouseX, mouseY);
                }

                break;
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_E && !(getFocused() instanceof EditBox)) {
            ForgeHooksClient.popGuiLayer(minecraft);
            return true;
        } else {
            return super.keyPressed(key, value, modifier);
        }
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
        if (button == buttonNameVisibility) {
            ConfigData.nameVisibility = ConfigData.NameVisibility.values()[buttonNameVisibility.getSelected()];
        } else if (button == buttonHideNamesThatDontFit) {
            ConfigData.hideNamesThatDontFit = buttonHideNamesThatDontFit.getSelected() == 0;
        } else if (button == buttonHUDEnabled) {
            ConfigData.hudEnabled = buttonHUDEnabled.getSelected() == 0;
            buttonEditHUD.visible = ConfigData.hudEnabled;
        } else if (button == buttonEditHUD) {
            ClientProxy.setLastSettingsTab(tabSelected);
            Minecraft.getInstance().setScreen(new GuiHUDSettings());
        } else if (button == buttonNewGroup) {
            if (settings != null) {
                SettingsGroup group = settings.createCustomGroup(textNewGroupName.getValue());
                textNewGroupName.setValue("");
                GuiGroupElement element = new GuiGroupElement(font, renderables, group);
                groups.addElement(element);
                groups.scrollBottom();
                groupClicked(element);
                groupsActions.scrollBottom();

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

            if (group.hasUser(user)) {
                textNewUser.setError(Component.translatable("mapfrontiers.new_user_error_user_repeated"));
                return;
            }

            group.addUser(user);
            GuiUserElement element = new GuiUserElement(font, renderables, user);
            users.addElement(element);
            users.scrollBottom();

            textNewUser.setValue("");

            sendChangesToServer();
        } else if (button == buttonDone) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }

    @Override
    public void removed() {
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
        ClientProxy.configUpdated();
        ClientProxy.setLastSettingsTab(tabSelected);
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedSettingsProfileEvent(UpdatedSettingsProfileEvent event) {
        if ((event.profile.updateSettings == SettingsProfile.State.Enabled) == canEditGroups) {
            return;
        }

        Minecraft.getInstance().setScreen(new GuiFrontierSettings());
    }

    public void linkClicked(boolean open, AbstractWidget widget) {
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

        Minecraft.getInstance().setScreen(new GuiFrontierSettings());
    }

    public void setFrontierSettings(FrontierSettings settings) {
        this.settings = settings;

        groups.removeAll();
        groups.addElement(new GuiGroupElement(font, renderables, settings.getOPsGroup()));
        groups.addElement(new GuiGroupElement(font, renderables, settings.getOwnersGroup()));
        groups.addElement(new GuiGroupElement(font, renderables, settings.getEveryoneGroup()));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groups.addElement(new GuiGroupElement(font, renderables, group));
        }

        updateGroupsActions();

        resetLabels();
        updateButtonsVisibility();
    }

    private void resetLabels() {
        for (GuiSimpleLabel label : labels) {
            removeWidget(label);
        }

        labels.clear();
        labelTooltips.clear();

        if (tabSelected == Tab.Credits) {
            labels.add(new GuiSimpleLabel(font, width / 2, height / 2 - 106, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_created_by"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, width / 2, height / 2 - 58, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_many_thanks"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, width / 2, height / 2 - 28, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_project"), GuiColors.SETTINGS_TEXT_MEDIUM));
            labels.add(new GuiSimpleLabel(font, width / 2, height / 2 + 22, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_patreon"), GuiColors.SETTINGS_TEXT_MEDIUM));
            labels.add(new GuiSimpleLabel(font, 50, height - 54, GuiSimpleLabel.Align.Left,
                    Component.translatable("mapfrontiers.credits_translation"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, width - 48, height - 54, GuiSimpleLabel.Align.Right,
                    Component.translatable(MapFrontiers.VERSION), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
        } else if (tabSelected == Tab.General) {
            labels.add(new GuiSimpleLabel(font, width / 2, 54, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.frontiers"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 72, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("nameVisibility"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("nameVisibility"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 88, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("hideNamesThatDontFit"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("hideNamesThatDontFit"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 104, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("polygonsOpacity"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("polygonsOpacity"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 120, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("snapDistance"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("snapDistance"));
            labels.add(new GuiSimpleLabel(font, width / 2, 154, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.hud"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, width / 2 - 120, 174, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("hud.enabled"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("hud.enabled"));
        } else if (tabSelected == Tab.Groups) {
            if (settings != null) {
                GuiGroupElement element = (GuiGroupElement) groups.getSelectedElement();
                if (element != null && element.getGroup().isSpecial()) {
                    SettingsGroup group = element.getGroup();
                    if (group == settings.getOPsGroup()) {
                        labels.add(new GuiSimpleLabel(font, 250, 82, GuiSimpleLabel.Align.Left,
                                Component.translatable("mapfrontiers.group_ops_desc"), GuiColors.SETTINGS_TEXT));
                    } else if (group == settings.getOwnersGroup()) {
                        labels.add(new GuiSimpleLabel(font, 250, 82, GuiSimpleLabel.Align.Left,
                                Component.translatable("mapfrontiers.group_owners_desc"), GuiColors.SETTINGS_TEXT));
                    } else if (group == settings.getEveryoneGroup()) {
                        labels.add(new GuiSimpleLabel(font, 250, 82, GuiSimpleLabel.Align.Left,
                                Component.translatable("mapfrontiers.group_everyone_desc"), GuiColors.SETTINGS_TEXT));
                    }
                }
            }
        } else if (tabSelected == Tab.Actions) {
            int x = width / 2 - 55;
            labels.add(new GuiSimpleLabel(font, x, 54, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.create_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 60, 54, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.delete_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 120, 54, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 180, 54, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_settings"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 240, 54, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.personal_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
        }

        for (GuiSimpleLabel label : labels) {
            addRenderableOnly(label);
        }
    }

    private void addLabelWithTooltip(GuiSimpleLabel label, List<Component> tooltip) {
        labels.add(label);
        labelTooltips.put(label, tooltip);
    }

    private void updateButtonsVisibility() {
        buttonWeb.visible = tabSelected == Tab.Credits;
        buttonProject.visible = tabSelected == Tab.Credits;
        buttonPatreon.visible = tabSelected == Tab.Credits;
        buttonNameVisibility.visible = tabSelected == Tab.General;
        buttonHideNamesThatDontFit.visible = tabSelected == Tab.General;
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
        textGroupName.visible = tabSelected == Tab.Groups;
    }

    public void groupClicked(GuiGroupElement element) {
        groups.selectElement(element);
        textGroupName.setValue(element.getGroup().getName());
        textGroupName.setEditable(!element.getGroup().isSpecial());
        textGroupName.setBordered(!element.getGroup().isSpecial());
        textGroupName.setFocus(false);

        resetLabels();
        updateUsers();
    }

    @Override
    public void elementClicked(GuiScrollBox scrollBox, ScrollElement element) {
        if (scrollBox == groups) {
            groupClicked((GuiGroupElement) element);
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
    public void updatedValue(TextIntBox textIntBox, int value) {
        if (textSnapDistance == textIntBox) {
            ConfigData.snapDistance = value;
        }
    }

    @Override
    public void updatedValue(TextDoubleBox textDoubleBox, double value) {
        if (textPolygonsOpacity == textDoubleBox) {
            ConfigData.polygonsOpacity = value;
        }
    }

    @Override
    public void lostFocus(TextBox textBox, String value) {
        if (textGroupName == textBox && tabSelected == Tab.Groups) {
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
                users.addElement(new GuiUserElement(font, renderables, user));
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
