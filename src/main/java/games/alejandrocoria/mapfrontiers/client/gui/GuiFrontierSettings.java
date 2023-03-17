package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.gui.GuiScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
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

    private float scaleFactor;
    private int actualWidth;
    private int actualHeight;

    private FrontierSettings settings;
    private GuiTabbedBox tabbedBox;
    private GuiLinkButton buttonWeb;
    private GuiLinkButton buttonProject;
    private GuiPatreonButton buttonPatreon;
    private GuiOptionButton buttonFullscreenVisibility;
    private GuiOptionButton buttonFullscreenNameVisibility;
    private GuiOptionButton buttonFullscreenOwnerVisibility;
    private GuiOptionButton buttonMinimapVisibility;
    private GuiOptionButton buttonMinimapNameVisibility;
    private GuiOptionButton buttonMinimapOwnerVisibility;
    private GuiOptionButton ButtonTitleAnnouncementAboveHotbar;
    private GuiOptionButton buttonAnnounceUnnamedFrontiers;
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
        if (ClientProxy.isModOnServer()) {
            PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings());
        }

        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        scaleFactor = ScreenHelper.getScaleFactorThatFit(this, 696, 326);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);

        Component title = Component.translatable("mapfrontiers.title_settings");
        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        canEditGroups = ClientProxy.isModOnServer() && ClientProxy.getSettingsProfile().updateSettings == SettingsProfile.State.Enabled;

        if (tabSelected == null) {
            tabSelected = ClientProxy.getLastSettingsTab();
        }

        tabbedBox = new GuiTabbedBox(font, 40, 24, actualWidth - 80, actualHeight - 64, this);
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

        buttonWeb = new GuiLinkButton(font, actualWidth / 2, actualHeight / 2 - 98, Component.literal("alejandrocoria.games"),
                "https://alejandrocoria.games", (open) -> linkClicked(open, buttonWeb));

        buttonProject = new GuiLinkButton(font, actualWidth / 2, actualHeight / 2 - 20,
                Component.literal("curseforge.com/minecraft/mc-mods/mapfrontiers"),
                "https://www.curseforge.com/minecraft/mc-mods/mapfrontiers", (open) -> linkClicked(open, buttonProject));

        buttonPatreon = new GuiPatreonButton(actualWidth / 2, actualHeight / 2 + 36, "https://www.patreon.com/alejandrocoria",
                (open) -> linkClicked(open, buttonPatreon));

        buttonFullscreenVisibility = new GuiOptionButton(font, actualWidth / 2 - 120, 70, 80, this::buttonPressed);
        buttonFullscreenVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Custom));
        buttonFullscreenVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Always));
        buttonFullscreenVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Never));
        buttonFullscreenVisibility.setSelected(ConfigData.fullscreenVisibility.ordinal());

        buttonFullscreenNameVisibility = new GuiOptionButton(font, actualWidth / 2 - 120, 86, 80, this::buttonPressed);
        buttonFullscreenNameVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Custom));
        buttonFullscreenNameVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Always));
        buttonFullscreenNameVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Never));
        buttonFullscreenNameVisibility.setSelected(ConfigData.fullscreenNameVisibility.ordinal());

        buttonFullscreenOwnerVisibility = new GuiOptionButton(font, actualWidth / 2 - 120, 102, 80, this::buttonPressed);
        buttonFullscreenOwnerVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Custom));
        buttonFullscreenOwnerVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Always));
        buttonFullscreenOwnerVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Never));
        buttonFullscreenOwnerVisibility.setSelected(ConfigData.fullscreenOwnerVisibility.ordinal());

        buttonMinimapVisibility = new GuiOptionButton(font, actualWidth / 2 + 140, 70, 80, this::buttonPressed);
        buttonMinimapVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Custom));
        buttonMinimapVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Always));
        buttonMinimapVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Never));
        buttonMinimapVisibility.setSelected(ConfigData.minimapVisibility.ordinal());

        buttonMinimapNameVisibility = new GuiOptionButton(font, actualWidth / 2 + 140, 86, 80, this::buttonPressed);
        buttonMinimapNameVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Custom));
        buttonMinimapNameVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Always));
        buttonMinimapNameVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Never));
        buttonMinimapNameVisibility.setSelected(ConfigData.minimapNameVisibility.ordinal());

        buttonMinimapOwnerVisibility = new GuiOptionButton(font, actualWidth / 2 + 140, 102, 80, this::buttonPressed);
        buttonMinimapOwnerVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Custom));
        buttonMinimapOwnerVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Always));
        buttonMinimapOwnerVisibility.addOption(ConfigData.getTranslatedEnum(ConfigData.Visibility.Never));
        buttonMinimapOwnerVisibility.setSelected(ConfigData.minimapOwnerVisibility.ordinal());

        ButtonTitleAnnouncementAboveHotbar = new GuiOptionButton(font, actualWidth / 2 + 80, 134, 40, this::buttonPressed);
        ButtonTitleAnnouncementAboveHotbar.addOption(Component.translatable("options.on"));
        ButtonTitleAnnouncementAboveHotbar.addOption(Component.translatable("options.off"));
        ButtonTitleAnnouncementAboveHotbar.setSelected(ConfigData.titleAnnouncementAboveHotbar ? 0 : 1);

        buttonAnnounceUnnamedFrontiers = new GuiOptionButton(font, actualWidth / 2 + 80, 150, 40, this::buttonPressed);
        buttonAnnounceUnnamedFrontiers.addOption(Component.translatable("options.on"));
        buttonAnnounceUnnamedFrontiers.addOption(Component.translatable("options.off"));
        buttonAnnounceUnnamedFrontiers.setSelected(ConfigData.announceUnnamedFrontiers ? 0 : 1);

        buttonHideNamesThatDontFit = new GuiOptionButton(font, actualWidth / 2 + 80, 166, 40, this::buttonPressed);
        buttonHideNamesThatDontFit.addOption(Component.translatable("options.on"));
        buttonHideNamesThatDontFit.addOption(Component.translatable("options.off"));
        buttonHideNamesThatDontFit.setSelected(ConfigData.hideNamesThatDontFit ? 0 : 1);

        textPolygonsOpacity = new TextDoubleBox(0.4, 0.0, 1.0, font, actualWidth / 2 + 80, 182, 40);
        textPolygonsOpacity.setValue(String.valueOf(ConfigData.polygonsOpacity));
        textPolygonsOpacity.setMaxLength(6);
        textPolygonsOpacity.setResponder(this);

        textSnapDistance = new TextIntBox(8, 0, 16, font, actualWidth / 2 + 80, 198, 40);
        textSnapDistance.setValue(String.valueOf(ConfigData.snapDistance));
        textSnapDistance.setMaxLength(2);
        textSnapDistance.setResponder(this);

        buttonHUDEnabled = new GuiOptionButton(font, actualWidth / 2 + 80, 252, 40, this::buttonPressed);
        buttonHUDEnabled.addOption(Component.translatable("options.on"));
        buttonHUDEnabled.addOption(Component.translatable("options.off"));
        buttonHUDEnabled.setSelected(ConfigData.hudEnabled ? 0 : 1);

        buttonEditHUD = new GuiSettingsButton(font, actualWidth / 2 - 50, 272, 100,
                Component.translatable("mapfrontiers.edit_hud"), this::buttonPressed);

        groups = new GuiScrollBox(50, 50, 160, actualHeight - 120, 16, this);
        users = new GuiScrollBox(250, 82, 258, actualHeight - 150, 16, this);
        groupsActions = new GuiScrollBox(actualWidth / 2 - 215, 82, 430, actualHeight - 128, 16, this);

        textNewGroupName = new TextBox(font, 50, actualHeight - 61, 140,
                I18n.get("mapfrontiers.new_group_name"));
        textNewGroupName.setMaxLength(22);
        textNewGroupName.setResponder(this);

        buttonNewGroup = new GuiButtonIcon(192, actualHeight - 61, GuiButtonIcon.Type.Add, this::buttonPressed);

        textNewUser = new TextUserBox(minecraft, font, 250, actualHeight - 61, 238,
                I18n.get("mapfrontiers.new_user"));
        textNewUser.setMaxLength(38);
        textNewUser.setResponder(this);

        buttonNewUser = new GuiButtonIcon(490, actualHeight - 61, GuiButtonIcon.Type.Add, this::buttonPressed);

        textGroupName = new TextBox(font, 250, 50, 140);
        textGroupName.setMaxLength(22);
        textGroupName.setResponder(this);
        textGroupName.setEditable(false);
        textGroupName.setBordered(false);

        buttonDone = new GuiSettingsButton(font, actualWidth / 2 - 70, actualHeight - 28, 140,
                Component.translatable("gui.done"), this::buttonPressed);

        addRenderableWidget(tabbedBox);
        addRenderableWidget(buttonWeb);
        addRenderableWidget(buttonProject);
        addRenderableWidget(buttonPatreon);
        addRenderableWidget(buttonFullscreenVisibility);
        addRenderableWidget(buttonFullscreenNameVisibility);
        addRenderableWidget(buttonFullscreenOwnerVisibility);
        addRenderableWidget(buttonMinimapVisibility);
        addRenderableWidget(buttonMinimapNameVisibility);
        addRenderableWidget(buttonMinimapOwnerVisibility);
        addRenderableWidget(ButtonTitleAnnouncementAboveHotbar);
        addRenderableWidget(buttonAnnounceUnnamedFrontiers);
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

        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (scaleFactor != 1.f) {
            matrixStack.pushPose();
            matrixStack.scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

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

        if (scaleFactor != 1.f) {
            matrixStack.popPose();
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
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return super.mouseClicked(mouseX * scaleFactor, mouseY * scaleFactor, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        for (Widget w : renderables) {
            if (w instanceof GuiScrollBox) {
                ((GuiScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX * scaleFactor, mouseY * scaleFactor, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX * scaleFactor, mouseY * scaleFactor, button, dragX * scaleFactor, dragY * scaleFactor);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonFullscreenVisibility) {
            ConfigData.fullscreenVisibility = ConfigData.Visibility.values()[buttonFullscreenVisibility.getSelected()];
        } else if (button == buttonFullscreenNameVisibility) {
            ConfigData.fullscreenNameVisibility = ConfigData.Visibility.values()[buttonFullscreenNameVisibility.getSelected()];
        } else if (button == buttonFullscreenOwnerVisibility) {
            ConfigData.fullscreenOwnerVisibility = ConfigData.Visibility.values()[buttonFullscreenOwnerVisibility.getSelected()];
        } else if (button == buttonMinimapVisibility) {
            ConfigData.minimapVisibility = ConfigData.Visibility.values()[buttonMinimapVisibility.getSelected()];
        } else if (button == buttonMinimapNameVisibility) {
            ConfigData.minimapNameVisibility = ConfigData.Visibility.values()[buttonMinimapNameVisibility.getSelected()];
        } else if (button == buttonMinimapOwnerVisibility) {
            ConfigData.minimapOwnerVisibility = ConfigData.Visibility.values()[buttonMinimapOwnerVisibility.getSelected()];
        } else if (button == ButtonTitleAnnouncementAboveHotbar) {
            ConfigData.titleAnnouncementAboveHotbar = ButtonTitleAnnouncementAboveHotbar.getSelected() == 0;
        } else if (button == buttonAnnounceUnnamedFrontiers) {
            ConfigData.announceUnnamedFrontiers = buttonAnnounceUnnamedFrontiers.getSelected() == 0;
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
            labels.add(new GuiSimpleLabel(font, actualWidth / 2, actualHeight / 2 - 106, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_created_by"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, actualWidth / 2, actualHeight / 2 - 58, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_many_thanks"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, actualWidth / 2, actualHeight / 2 - 28, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_project"), GuiColors.SETTINGS_TEXT_MEDIUM));
            labels.add(new GuiSimpleLabel(font, actualWidth / 2, actualHeight / 2 + 22, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_patreon"), GuiColors.SETTINGS_TEXT_MEDIUM));
            labels.add(new GuiSimpleLabel(font, 50, actualHeight - 54, GuiSimpleLabel.Align.Left,
                    Component.translatable("mapfrontiers.credits_translation"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, actualWidth - 48, actualHeight - 54, GuiSimpleLabel.Align.Right,
                    Component.translatable(MapFrontiers.VERSION), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
        } else if (tabSelected == Tab.General) {
            labels.add(new GuiSimpleLabel(font, actualWidth / 2, 54, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.frontiers"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 - 300, 72, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("fullscreenVisibility"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("fullscreenVisibility"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 - 300, 88, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("fullscreenNameVisibility"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("fullscreenNameVisibility"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 - 300, 104, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("fullscreenOwnerVisibility"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("fullscreenOwnerVisibility"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 + 30, 72, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("minimapVisibility"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("minimapVisibility"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 + 30, 88, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("minimapNameVisibility"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("minimapNameVisibility"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 + 30, 104, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("minimapOwnerVisibility"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("minimapOwnerVisibility"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 - 120, 136, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("titleAnnouncementAboveHotbar"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("titleAnnouncementAboveHotbar"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 - 120, 152, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("announceUnnamedFrontiers"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("announceUnnamedFrontiers"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 - 120, 168, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("hideNamesThatDontFit"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("hideNamesThatDontFit"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 - 120, 184, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("polygonsOpacity"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("polygonsOpacity"));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 - 120, 200, GuiSimpleLabel.Align.Left,
                            ConfigData.getTranslatedName("snapDistance"), GuiColors.SETTINGS_TEXT),
                    ConfigData.getTooltip("snapDistance"));
            labels.add(new GuiSimpleLabel(font, actualWidth / 2, 234, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.hud"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            addLabelWithTooltip(
                    new GuiSimpleLabel(font, actualWidth / 2 - 120, 254, GuiSimpleLabel.Align.Left,
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
            int x = actualWidth / 2 - 55;
            labels.add(new GuiSimpleLabel(font, x, 47, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.create_global_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 60, 47, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.delete_global_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 120, 47, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_global_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 180, 53, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_settings"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
            labels.add(new GuiSimpleLabel(font, x + 240, 47, GuiSimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.share_personal_frontier"), GuiColors.SETTINGS_TEXT_HIGHLIGHT));
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
        buttonFullscreenVisibility.visible = tabSelected == Tab.General;
        buttonFullscreenNameVisibility.visible = tabSelected == Tab.General;
        buttonFullscreenOwnerVisibility.visible = tabSelected == Tab.General;
        buttonMinimapVisibility.visible = tabSelected == Tab.General;
        buttonMinimapNameVisibility.visible = tabSelected == Tab.General;
        buttonMinimapOwnerVisibility.visible = tabSelected == Tab.General;
        ButtonTitleAnnouncementAboveHotbar.visible = tabSelected == Tab.General;
        buttonAnnounceUnnamedFrontiers.visible = tabSelected == Tab.General;
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
