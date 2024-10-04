package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import games.alejandrocoria.mapfrontiers.client.gui.component.TabbedBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.LinkButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.PatreonButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.GroupActionElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.GroupElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.UserElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxUser;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRequestFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings.Action;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ModSettings extends AutoScaledScreen {
    public enum Tab {
        Credits, General, Groups, Actions
    }

    private final boolean showKeyHint;

    private FrontierSettings settings;
    private TabbedBox tabbedBox;
    private LinkButton buttonWeb;
    private LinkButton buttonProject;
    private PatreonButton buttonPatreon;
    private OptionButton buttonFullscreenButtons;
    private OptionButton buttonFullscreenVisibility;
    private OptionButton buttonFullscreenNameVisibility;
    private OptionButton buttonFullscreenOwnerVisibility;
    private OptionButton buttonMinimapVisibility;
    private OptionButton buttonMinimapNameVisibility;
    private OptionButton buttonMinimapOwnerVisibility;
    private OptionButton ButtonTitleAnnouncementAboveHotbar;
    private OptionButton buttonAnnounceUnnamedFrontiers;
    private OptionButton buttonHideNamesThatDontFit;
    private TextBoxDouble textPolygonsOpacity;
    private TextBoxInt textSnapDistance;
    private OptionButton buttonHUDEnabled;
    private SimpleButton buttonEditHUD;
    private ScrollBox groups;
    private ScrollBox users;
    private ScrollBox groupsActions;
    private TextBox textNewGroupName;
    private IconButton buttonNewGroup;
    private TextBoxUser textNewUser;
    private IconButton buttonNewUser;
    private TextBox textGroupName;
    private SimpleButton buttonDone;

    private final List<SimpleLabel> labels;
    private final Map<SimpleLabel, List<Component>> labelTooltips;
    private boolean canEditGroups;
    private Tab tabSelected;
    private int ticksSinceLastUpdate = 0;

    public ModSettings(boolean showKeyHint) {
        super(Component.translatable("mapfrontiers.title_settings"), 696, 366);
        this.showKeyHint = showKeyHint;
        labels = new ArrayList<>();
        labelTooltips = new HashMap<>();

        ClientEventHandler.subscribeUpdatedSettingsProfileEvent(this, profile -> {
            if ((profile.updateSettings == SettingsProfile.State.Enabled) == canEditGroups) {
                return;
            }

            if (tabSelected != null) {
                MapFrontiersClient.setLastSettingsTab(tabSelected);
            }

            onClose();
            new ModSettings(showKeyHint).display();
        });
    }

    private ModSettings() {
        super(Component.empty());
        showKeyHint = false;
        labels = null;
        labelTooltips = null;
    }

    public static ModSettings createDummy() {
        return new ModSettings();
    }

    @Override
    public void initScreen() {
        canEditGroups = MapFrontiersClient.isModOnServer() && MapFrontiersClient.getSettingsProfile().updateSettings == SettingsProfile.State.Enabled;

        if (tabSelected == null) {
            tabSelected = MapFrontiersClient.getLastSettingsTab();
        }

        tabbedBox = new TabbedBox(font, 40, 24, actualWidth - 80, actualHeight - 64, tab -> {
            tabSelected = Tab.values()[tab];

            if (tabSelected == Tab.Actions) {
                updateGroupsActions();
            }

            resetLabels();
            updateButtonsVisibility();
        });
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

        buttonWeb = new LinkButton(font, actualWidth / 2, actualHeight / 2 - 98, Component.literal("alejandrocoria.games"), (b) -> {
            MapFrontiersClient.setLastSettingsTab(tabSelected);
            ConfirmLinkScreen.confirmLinkNow(this, "https://alejandrocoria.games", false);
        });

        buttonProject = new LinkButton(font, actualWidth / 2, actualHeight / 2 - 20,
                Component.literal("curseforge.com/minecraft/mc-mods/mapfrontiers"), (b) -> {
            MapFrontiersClient.setLastSettingsTab(tabSelected);
            ConfirmLinkScreen.confirmLinkNow(this, "https://www.curseforge.com/minecraft/mc-mods/mapfrontiers", false);
        });

        buttonPatreon = new PatreonButton(actualWidth / 2, actualHeight / 2 + 36, (b) -> {
            MapFrontiersClient.setLastSettingsTab(tabSelected);
            ConfirmLinkScreen.confirmLinkNow(this, "https://www.patreon.com/alejandrocoria", false);
        });

        buttonFullscreenButtons = new OptionButton(font, actualWidth / 2 + 80, 54, 40, this::buttonPressed);
        buttonFullscreenButtons.addOption(Component.translatable("options.on"));
        buttonFullscreenButtons.addOption(Component.translatable("options.off"));
        buttonFullscreenButtons.setSelected(Config.fullscreenButtons ? 0 : 1);

        buttonFullscreenVisibility = new OptionButton(font, actualWidth / 2 - 120, 106, 80, this::buttonPressed);
        buttonFullscreenVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Custom));
        buttonFullscreenVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Always));
        buttonFullscreenVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Never));
        buttonFullscreenVisibility.setSelected(Config.fullscreenVisibility.ordinal());

        buttonFullscreenNameVisibility = new OptionButton(font, actualWidth / 2 - 120, 122, 80, this::buttonPressed);
        buttonFullscreenNameVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Custom));
        buttonFullscreenNameVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Always));
        buttonFullscreenNameVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Never));
        buttonFullscreenNameVisibility.setSelected(Config.fullscreenNameVisibility.ordinal());

        buttonFullscreenOwnerVisibility = new OptionButton(font, actualWidth / 2 - 120, 138, 80, this::buttonPressed);
        buttonFullscreenOwnerVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Custom));
        buttonFullscreenOwnerVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Always));
        buttonFullscreenOwnerVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Never));
        buttonFullscreenOwnerVisibility.setSelected(Config.fullscreenOwnerVisibility.ordinal());

        buttonMinimapVisibility = new OptionButton(font, actualWidth / 2 + 140, 106, 80, this::buttonPressed);
        buttonMinimapVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Custom));
        buttonMinimapVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Always));
        buttonMinimapVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Never));
        buttonMinimapVisibility.setSelected(Config.minimapVisibility.ordinal());

        buttonMinimapNameVisibility = new OptionButton(font, actualWidth / 2 + 140, 122, 80, this::buttonPressed);
        buttonMinimapNameVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Custom));
        buttonMinimapNameVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Always));
        buttonMinimapNameVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Never));
        buttonMinimapNameVisibility.setSelected(Config.minimapNameVisibility.ordinal());

        buttonMinimapOwnerVisibility = new OptionButton(font, actualWidth / 2 + 140, 138, 80, this::buttonPressed);
        buttonMinimapOwnerVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Custom));
        buttonMinimapOwnerVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Always));
        buttonMinimapOwnerVisibility.addOption(Config.getTranslatedEnum(Config.Visibility.Never));
        buttonMinimapOwnerVisibility.setSelected(Config.minimapOwnerVisibility.ordinal());

        ButtonTitleAnnouncementAboveHotbar = new OptionButton(font, actualWidth / 2 + 80, 170, 40, this::buttonPressed);
        ButtonTitleAnnouncementAboveHotbar.addOption(Component.translatable("options.on"));
        ButtonTitleAnnouncementAboveHotbar.addOption(Component.translatable("options.off"));
        ButtonTitleAnnouncementAboveHotbar.setSelected(Config.titleAnnouncementAboveHotbar ? 0 : 1);

        buttonAnnounceUnnamedFrontiers = new OptionButton(font, actualWidth / 2 + 80, 186, 40, this::buttonPressed);
        buttonAnnounceUnnamedFrontiers.addOption(Component.translatable("options.on"));
        buttonAnnounceUnnamedFrontiers.addOption(Component.translatable("options.off"));
        buttonAnnounceUnnamedFrontiers.setSelected(Config.announceUnnamedFrontiers ? 0 : 1);

        buttonHideNamesThatDontFit = new OptionButton(font, actualWidth / 2 + 80, 202, 40, this::buttonPressed);
        buttonHideNamesThatDontFit.addOption(Component.translatable("options.on"));
        buttonHideNamesThatDontFit.addOption(Component.translatable("options.off"));
        buttonHideNamesThatDontFit.setSelected(Config.hideNamesThatDontFit ? 0 : 1);

        textPolygonsOpacity = new TextBoxDouble(0.4, 0.0, 1.0, font, actualWidth / 2 + 80, 218, 40);
        textPolygonsOpacity.setValue(String.valueOf(Config.polygonsOpacity));
        textPolygonsOpacity.setMaxLength(6);
        textPolygonsOpacity.setValueChangedCallback(value -> Config.polygonsOpacity = value);

        textSnapDistance = new TextBoxInt(8, 0, 16, font, actualWidth / 2 + 80, 234, 40);
        textSnapDistance.setValue(String.valueOf(Config.snapDistance));
        textSnapDistance.setMaxLength(2);
        textSnapDistance.setValueChangedCallback(value -> Config.snapDistance = value);

        buttonHUDEnabled = new OptionButton(font, actualWidth / 2 + 80, 288, 40, this::buttonPressed);
        buttonHUDEnabled.addOption(Component.translatable("options.on"));
        buttonHUDEnabled.addOption(Component.translatable("options.off"));
        buttonHUDEnabled.setSelected(Config.hudEnabled ? 0 : 1);

        buttonEditHUD = new SimpleButton(font, actualWidth / 2 - 50, 308, 100, Component.translatable("mapfrontiers.edit_hud"), this::buttonPressed);

        groups = new ScrollBox(50, 50, 160, actualHeight - 120, 16);
        groups.setElementClickedCallback(element -> {
            groupClicked((GroupElement) element);
            updateButtonsVisibility();
        });
        groups.setElementDeletedCallback(element -> {
            if (groups.getSelectedElement() != null) {
                groupClicked((GroupElement) groups.getSelectedElement());
            }
            settings.removeCustomGroup(((GroupElement) element).getGroup());
            sendChangesToServer();
        });

        users = new ScrollBox(250, 82, 258, actualHeight - 150, 16);
        users.setElementDeletedCallback(element -> {
            SettingsGroup group = ((GroupElement) groups.getSelectedElement()).getGroup();
            group.removeUser(((UserElement) element).getUser());
            sendChangesToServer();
        });

        groupsActions = new ScrollBox(actualWidth / 2 - 215, 82, 430, actualHeight - 128, 16);

        textNewGroupName = new TextBox(font, 50, actualHeight - 61, 140, I18n.get("mapfrontiers.new_group_name"));
        textNewGroupName.setMaxLength(22);

        buttonNewGroup = new IconButton(192, actualHeight - 61, IconButton.Type.Add, this::buttonPressed);

        textNewUser = new TextBoxUser(minecraft, font, 250, actualHeight - 61, 238, I18n.get("mapfrontiers.new_user"));
        textNewUser.setMaxLength(38);

        buttonNewUser = new IconButton(490, actualHeight - 61, IconButton.Type.Add, this::buttonPressed);

        textGroupName = new TextBox(font, 250, 50, 140);
        textGroupName.setMaxLength(22);
        textGroupName.setEditable(false);
        textGroupName.setBordered(false);
        textGroupName.setLostFocusCallback(value -> {
            if (tabSelected == Tab.Groups) {
                GroupElement groupElement = (GroupElement) groups.getSelectedElement();
                if (groupElement != null) {
                    groupElement.getGroup().setName(value);
                    sendChangesToServer();
                }
            }
        });

        buttonDone = new SimpleButton(font, actualWidth / 2 - 70, actualHeight - 28, 140,
                Component.translatable("gui.done"), this::buttonPressed);

        addRenderableWidget(tabbedBox);
        addRenderableWidget(buttonWeb);
        addRenderableWidget(buttonProject);
        addRenderableWidget(buttonPatreon);
        addRenderableWidget(buttonFullscreenButtons);
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

        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketRequestFrontierSettings());
        }
    }

    @Override
    public void tick() {
        if (!canEditGroups || settings == null) {
            return;
        }

        ++ticksSinceLastUpdate;

        if (ticksSinceLastUpdate >= 100) {
            ticksSinceLastUpdate = 0;
            PacketHandler.sendToServer(new PacketRequestFrontierSettings(settings.getChangeCounter()));

            ClientPacketListener handler = minecraft.getConnection();
            if (handler == null) {
                return;
            }

            for (ScrollElement element : users.getElements()) {
                UserElement userElement = (UserElement) element;
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
    public void renderScaledScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Rendering manually so the background is not scaled.
        for(GuiEventListener child : children()) {
            if (child instanceof Renderable renderable)
                renderable.render(graphics, mouseX, mouseY, partialTicks);
        }

        for (SimpleLabel label : labels) {
            if (label.visible) {
                label.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        for (SimpleLabel label : labels) {
            if (label.visible && label.isHovered()) {
                List<Component> tooltip = labelTooltips.get(label);
                if (tooltip != null) {
                    graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
                }

                break;
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_E && !(getFocused() instanceof EditBox)) {
            onClose();
            return true;
        } else {
            return super.keyPressed(key, value, modifier);
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
        if (button == buttonFullscreenButtons) {
            Config.fullscreenButtons = buttonFullscreenButtons.getSelected() == 0;
        } else if (button == buttonFullscreenVisibility) {
            Config.fullscreenVisibility = Config.Visibility.values()[buttonFullscreenVisibility.getSelected()];
        } else if (button == buttonFullscreenNameVisibility) {
            Config.fullscreenNameVisibility = Config.Visibility.values()[buttonFullscreenNameVisibility.getSelected()];
        } else if (button == buttonFullscreenOwnerVisibility) {
            Config.fullscreenOwnerVisibility = Config.Visibility.values()[buttonFullscreenOwnerVisibility.getSelected()];
        } else if (button == buttonMinimapVisibility) {
            Config.minimapVisibility = Config.Visibility.values()[buttonMinimapVisibility.getSelected()];
        } else if (button == buttonMinimapNameVisibility) {
            Config.minimapNameVisibility = Config.Visibility.values()[buttonMinimapNameVisibility.getSelected()];
        } else if (button == buttonMinimapOwnerVisibility) {
            Config.minimapOwnerVisibility = Config.Visibility.values()[buttonMinimapOwnerVisibility.getSelected()];
        } else if (button == ButtonTitleAnnouncementAboveHotbar) {
            Config.titleAnnouncementAboveHotbar = ButtonTitleAnnouncementAboveHotbar.getSelected() == 0;
        } else if (button == buttonAnnounceUnnamedFrontiers) {
            Config.announceUnnamedFrontiers = buttonAnnounceUnnamedFrontiers.getSelected() == 0;
        } else if (button == buttonHideNamesThatDontFit) {
            Config.hideNamesThatDontFit = buttonHideNamesThatDontFit.getSelected() == 0;
        } else if (button == buttonHUDEnabled) {
            Config.hudEnabled = buttonHUDEnabled.getSelected() == 0;
            updateButtonsVisibility();
        } else if (button == buttonEditHUD) {
            MapFrontiersClient.setLastSettingsTab(tabSelected);
            new HUDSettings().display();
        } else if (button == buttonNewGroup) {
            if (settings != null) {
                SettingsGroup group = settings.createCustomGroup(textNewGroupName.getValue());
                textNewGroupName.setValue("");
                GroupElement element = new GroupElement(font, this, group);
                groups.addElement(element);
                groups.scrollBottom();
                groupClicked(element);
                groupsActions.scrollBottom();

                sendChangesToServer();
            }
        } else if (button == buttonNewUser) {
            SettingsGroup group = ((GroupElement) groups.getSelectedElement()).getGroup();
            SettingsUser user = new SettingsUser();

            String usernameOrUUID = textNewUser.getValue();
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

            if (group.hasUser(user)) {
                textNewUser.setError(Component.translatable("mapfrontiers.new_user_error_user_repeated"));
                return;
            }

            group.addUser(user);
            UserElement element = new UserElement(font, this, user);
            users.addElement(element);
            users.scrollBottom();

            textNewUser.setValue("");

            sendChangesToServer();
        } else if (button == buttonDone) {
            onClose();
        }
    }

    @Override
    public void removed() {
        ClientEventHandler.postUpdatedConfigEvent();
        MapFrontiersClient.setLastSettingsTab(tabSelected);
        ClientEventHandler.unsuscribeAllEvents(this);
    }

    public void setFrontierSettings(FrontierSettings settings) {
        this.settings = settings;

        GroupElement selectedElement = (GroupElement) groups.getSelectedElement();
        int selectedIndex = groups.getSelectedIndex();

        groups.removeAll();
        groups.addElement(new GroupElement(font, this, settings.getOPsGroup()));
        groups.addElement(new GroupElement(font, this, settings.getOwnersGroup()));
        groups.addElement(new GroupElement(font, this, settings.getEveryoneGroup()));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groups.addElement(new GroupElement(font, this, group));
        }

        updateGroupsActions();

        resetLabels();
        updateButtonsVisibility();

        if (selectedElement != null) {
            groups.selectElementIf(element -> ((GroupElement) element).getGroup().getName().equals(selectedElement.getGroup().getName()));
        }

        if (groups.getSelectedElement() == null) {
            groups.selectIndex(selectedIndex);
        }

        if (groups.getSelectedElement() != null) {
            groupClicked((GroupElement) groups.getSelectedElement());
        }
    }

    private void resetLabels() {
        labels.clear();
        labelTooltips.clear();

        if (tabSelected == Tab.Credits) {
            labels.add(new SimpleLabel(font, actualWidth / 2, actualHeight / 2 - 106, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_created_by"), ColorConstants.TEXT_HIGHLIGHT));
            labels.add(new SimpleLabel(font, actualWidth / 2, actualHeight / 2 - 58, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_many_thanks", Services.PLATFORM.getPlatformName()), ColorConstants.TEXT_HIGHLIGHT));
            labels.add(new SimpleLabel(font, actualWidth / 2, actualHeight / 2 - 28, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_project"), ColorConstants.TEXT_MEDIUM));
            labels.add(new SimpleLabel(font, actualWidth / 2, actualHeight / 2 + 22, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.credits_patreon"), ColorConstants.TEXT_MEDIUM));
            labels.add(new SimpleLabel(font, 50, actualHeight - 54, SimpleLabel.Align.Left,
                    Component.translatable("mapfrontiers.credits_translation"), ColorConstants.TEXT_HIGHLIGHT));
        } else if (tabSelected == Tab.General) {
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 120, 56, SimpleLabel.Align.Left,
                            Config.getTranslatedName("fullscreenButtons"), ColorConstants.TEXT),
                    Config.getTooltip("fullscreenButtons"));
            labels.add(new SimpleLabel(font, actualWidth / 2, 90, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.frontiers"), ColorConstants.TEXT_HIGHLIGHT));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 300, 108, SimpleLabel.Align.Left,
                            Config.getTranslatedName("fullscreenVisibility"), ColorConstants.TEXT),
                    Config.getTooltip("fullscreenVisibility"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 300, 124, SimpleLabel.Align.Left,
                            Config.getTranslatedName("fullscreenNameVisibility"), ColorConstants.TEXT),
                    Config.getTooltip("fullscreenNameVisibility"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 300, 140, SimpleLabel.Align.Left,
                            Config.getTranslatedName("fullscreenOwnerVisibility"), ColorConstants.TEXT),
                    Config.getTooltip("fullscreenOwnerVisibility"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 + 30, 108, SimpleLabel.Align.Left,
                            Config.getTranslatedName("minimapVisibility"), ColorConstants.TEXT),
                    Config.getTooltip("minimapVisibility"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 + 30, 124, SimpleLabel.Align.Left,
                            Config.getTranslatedName("minimapNameVisibility"), ColorConstants.TEXT),
                    Config.getTooltip("minimapNameVisibility"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 + 30, 140, SimpleLabel.Align.Left,
                            Config.getTranslatedName("minimapOwnerVisibility"), ColorConstants.TEXT),
                    Config.getTooltip("minimapOwnerVisibility"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 120, 172, SimpleLabel.Align.Left,
                            Config.getTranslatedName("titleAnnouncementAboveHotbar"), ColorConstants.TEXT),
                    Config.getTooltip("titleAnnouncementAboveHotbar"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 120, 188, SimpleLabel.Align.Left,
                            Config.getTranslatedName("announceUnnamedFrontiers"), ColorConstants.TEXT),
                    Config.getTooltip("announceUnnamedFrontiers"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 120, 204, SimpleLabel.Align.Left,
                            Config.getTranslatedName("hideNamesThatDontFit"), ColorConstants.TEXT),
                    Config.getTooltip("hideNamesThatDontFit"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 120, 220, SimpleLabel.Align.Left,
                            Config.getTranslatedName("polygonsOpacity"), ColorConstants.TEXT),
                    Config.getTooltip("polygonsOpacity"));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 120, 236, SimpleLabel.Align.Left,
                            Config.getTranslatedName("snapDistance"), ColorConstants.TEXT),
                    Config.getTooltip("snapDistance"));
            labels.add(new SimpleLabel(font, actualWidth / 2, 270, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.hud"), ColorConstants.TEXT_HIGHLIGHT));
            addLabelWithTooltip(
                    new SimpleLabel(font, actualWidth / 2 - 120, 290, SimpleLabel.Align.Left,
                            Config.getTranslatedName("hud.enabled"), ColorConstants.TEXT),
                    Config.getTooltip("hud.enabled"));
        } else if (tabSelected == Tab.Groups) {
            if (settings != null) {
                GroupElement element = (GroupElement) groups.getSelectedElement();
                if (element != null && element.getGroup().isSpecial()) {
                    SettingsGroup group = element.getGroup();
                    if (group == settings.getOPsGroup()) {
                        labels.add(new SimpleLabel(font, 250, 82, SimpleLabel.Align.Left,
                                Component.translatable("mapfrontiers.group_ops_desc"), ColorConstants.TEXT));
                    } else if (group == settings.getOwnersGroup()) {
                        labels.add(new SimpleLabel(font, 250, 82, SimpleLabel.Align.Left,
                                Component.translatable("mapfrontiers.group_owners_desc"), ColorConstants.TEXT));
                    } else if (group == settings.getEveryoneGroup()) {
                        labels.add(new SimpleLabel(font, 250, 82, SimpleLabel.Align.Left,
                                Component.translatable("mapfrontiers.group_everyone_desc"), ColorConstants.TEXT));
                    }
                }
            }
        } else if (tabSelected == Tab.Actions) {
            int x = actualWidth / 2 - 55;
            labels.add(new SimpleLabel(font, x, 47, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.create_global_frontier"), ColorConstants.TEXT_HIGHLIGHT));
            labels.add(new SimpleLabel(font, x + 60, 47, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.delete_global_frontier"), ColorConstants.TEXT_HIGHLIGHT));
            labels.add(new SimpleLabel(font, x + 120, 47, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_global_frontier"), ColorConstants.TEXT_HIGHLIGHT));
            labels.add(new SimpleLabel(font, x + 180, 53, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.update_settings"), ColorConstants.TEXT_HIGHLIGHT));
            labels.add(new SimpleLabel(font, x + 240, 47, SimpleLabel.Align.Center,
                    Component.translatable("mapfrontiers.share_personal_frontier"), ColorConstants.TEXT_HIGHLIGHT));
        }

        if (tabSelected == Tab.Credits || tabSelected == Tab.General) {
            labels.add(new SimpleLabel(font, actualWidth - 48, actualHeight - 54, SimpleLabel.Align.Right,
                    Component.literal(Services.PLATFORM.getModVersion()), ColorConstants.TEXT_HIGHLIGHT));
            if (showKeyHint) {
                Component key = MapFrontiersClient.getOpenSettingsKey();
                if (key != null) {
                    labels.add(new SimpleLabel(font, actualWidth / 2, actualHeight - 54, SimpleLabel.Align.Center,
                            Component.translatable("mapfrontiers.key.open_settings.hint", key), ColorConstants.TEXT_HIGHLIGHT));
                }
            }
        }
    }

    private void addLabelWithTooltip(SimpleLabel label, List<Component> tooltip) {
        labels.add(label);
        labelTooltips.put(label, tooltip);
    }

    private void updateButtonsVisibility() {
        buttonWeb.visible = tabSelected == Tab.Credits;
        buttonProject.visible = tabSelected == Tab.Credits;
        buttonPatreon.visible = tabSelected == Tab.Credits;
        buttonFullscreenButtons.visible = tabSelected == Tab.General;
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
        buttonEditHUD.visible = tabSelected == Tab.General && Config.hudEnabled && minecraft.player != null;
        groups.visible = tabSelected == Tab.Groups;
        users.visible = tabSelected == Tab.Groups;
        groupsActions.visible = tabSelected == Tab.Actions;
        textNewGroupName.visible = tabSelected == Tab.Groups;
        buttonNewGroup.visible = tabSelected == Tab.Groups;
        textNewUser.visible = canAddNewUser();
        buttonNewUser.visible = canAddNewUser();
        textGroupName.visible = tabSelected == Tab.Groups;
    }

    public void groupClicked(GroupElement element) {
        groups.selectElement(element);
        textGroupName.setValue(element.getGroup().getName());
        textGroupName.setEditable(!element.getGroup().isSpecial());
        textGroupName.setBordered(!element.getGroup().isSpecial());
        textGroupName.setFocused(false);

        resetLabels();
        updateUsers();
    }

    private void sendChangesToServer() {
        if (settings != null) {
            settings.advanceChangeCounter();
            PacketHandler.sendToServer(new PacketFrontierSettings(settings));
        }
    }

    private void updateUsers() {
        users.removeAll();
        GroupElement element = (GroupElement) groups.getSelectedElement();
        if (element != null && !element.getGroup().isSpecial()) {
            for (SettingsUser user : element.getGroup().getUsers()) {
                users.addElement(new UserElement(font, this, user));
            }
        }

        buttonNewUser.visible = canAddNewUser();
        textNewUser.visible = canAddNewUser();
    }

    private void updateGroupsActions() {
        if (settings != null) {
            groupsActions.removeAll();
            groupsActions.addElement(new GroupActionElement(font, settings.getOPsGroup(), this::actionChanged));
            groupsActions.addElement(new GroupActionElement(font, settings.getOwnersGroup(), true, this::actionChanged));
            groupsActions.addElement(new GroupActionElement(font, settings.getEveryoneGroup(), this::actionChanged));

            for (SettingsGroup group : settings.getCustomGroups()) {
                groupsActions.addElement(new GroupActionElement(font, group, this::actionChanged));
            }
        }
    }

    private void actionChanged(SettingsGroup group, Action action, boolean checked) {
        if (checked) {
            group.addAction(action);
        } else {
            group.removeAction(action);
        }

        sendChangesToServer();
    }

    private boolean canAddNewUser() {
        if (tabSelected == Tab.Groups && groups.getSelectedElement() != null) {
            SettingsGroup group = ((GroupElement) groups.getSelectedElement()).getGroup();
            return !group.isSpecial();
        }

        return false;
    }
}
