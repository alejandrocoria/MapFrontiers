package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.TabbedBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.LinkButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.GroupActionElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.GroupElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.UserElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxUser;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.DeleteConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.FrontierAppearanceDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.VisibilityDialog;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class ModSettings extends AutoScaledScreen {
    public enum Tab {
        Credits, General, Groups, Actions
    }

    private static final Component titleLabel = Component.translatable("mapfrontiers.title_settings");
    private static final Component tabCreditsLabel = Component.translatable("mapfrontiers.credits");
    private static final Component tabGeneralLabel = Component.translatable("mapfrontiers.general");
    private static final Component tabGroupsLabel = Component.translatable("mapfrontiers.groups");
    private static final Component tabActionsLabel = Component.translatable("mapfrontiers.actions");
    private static final Component createdByLabel = Component.translatable("mapfrontiers.credits_created_by");
    private static final Component manyThanksLabel = Component.translatable("mapfrontiers.credits_many_thanks", Services.PLATFORM.getPlatformName());
    private static final Component projectLabel = Component.translatable("mapfrontiers.credits_project");
    private static final Component patreonLabel = Component.translatable("mapfrontiers.credits_patreon");
    private static final Component webLinkLabel = Component.literal("alejandrocoria.games");
    private static final String webURL = "https://alejandrocoria.games";
    private static final Component curseForgeLinkLabel = Component.literal("curseforge.com/minecraft/mc-mods/mapfrontiers");
    private static final String curseForgeURL = "https://www.curseforge.com/minecraft/mc-mods/mapfrontiers";
    private static final Component modrinthLinkLabel = Component.literal("modrinth.com/mod/mapfrontiers");
    private static final String modrinthURL = "https://modrinth.com/mod/mapfrontiers";
    private static final Component patreonLinkLabel = Component.literal("patreon.com/alejandrocoria");
    private static final String patreonURL = "https://patreon.com/alejandrocoria";
    private static final Component creditsTranslationLabel = Component.translatable("mapfrontiers.credits_translation");
    private static final Component versionLabel = Component.literal(Services.PLATFORM.getModVersion());
    private static final String keyHintkey = "mapfrontiers.key.open_settings.hint";
    private static final Component frontiersLabel = Component.translatable("mapfrontiers.frontiers");
    private static final Component frontierAppearanceLabel = Component.translatable("mapfrontiers.frontier_appearance");
    private static final Component forcedVisibilityLabel = Component.translatable("mapfrontiers.forced_visibility");
    private static final Component titleAnnouncementDurationLabel = Config.getTranslatedName("titleAnnouncementDuration");
    private static final Tooltip titleAnnouncementDurationTooltip = Config.getTooltip("titleAnnouncementDuration");
    private static final Component titleAnnouncementTimeoutLabel = Config.getTranslatedName("titleAnnouncementTimeout");
    private static final Tooltip titleAnnouncementTimeoutTooltip = Config.getTooltip("titleAnnouncementTimeout");
    private static final Component titleAnnouncementAboveHotbarLabel = Config.getTranslatedName("titleAnnouncementAboveHotbar");
    private static final Tooltip titleAnnouncementAboveHotbarTooltip = Config.getTooltip("titleAnnouncementAboveHotbar");
    private static final Component announceUnnamedFrontiersLabel = Config.getTranslatedName("announceUnnamedFrontiers");
    private static final Tooltip announceUnnamedFrontiersTooltip = Config.getTooltip("announceUnnamedFrontiers");
    private static final Component snapDistanceLabel = Config.getTranslatedName("snapDistance");
    private static final Tooltip snapDistanceTooltip = Config.getTooltip("snapDistance");
    private static final Component guiLabel = Component.translatable("mapfrontiers.gui");
    private static final Component fullscreenButtonsLabel = Config.getTranslatedName("fullscreenButtons");
    private static final Tooltip fullscreenButtonsTooltip = Config.getTooltip("fullscreenButtons");
    private static final Component askConfirmationFrontierDeleteLabel = Config.getTranslatedName("askConfirmationFrontierDelete");
    private static final Tooltip askConfirmationFrontierDeleteTooltip = Config.getTooltip("askConfirmationFrontierDelete");
    private static final Component askConfirmationGroupDeleteLabel = Config.getTranslatedName("askConfirmationGroupDelete");
    private static final Tooltip askConfirmationGroupDeleteTooltip = Config.getTooltip("askConfirmationGroupDelete");
    private static final Component askConfirmationUserDeleteLabel = Config.getTranslatedName("askConfirmationUserDelete");
    private static final Tooltip askConfirmationUserDeleteTooltip = Config.getTooltip("askConfirmationUserDelete");
    private static final Component hudLabel = Component.translatable("mapfrontiers.hud");
    private static final Component hudEnabledLabel = Config.getTranslatedName("hud.enabled");
    private static final Tooltip hudEnabledTooltip = Config.getTooltip("hud.enabled");
    private static final Component onLabel = Component.translatable("options.on");
    private static final Component offLabel = Component.translatable("options.off");
    private static final Component editHudLabel = Component.translatable("mapfrontiers.edit_hud");
    private static final Component groupOpsDescLabel = Component.translatable("mapfrontiers.group_ops_desc");
    private static final Component groupOwnersDescLabel = Component.translatable("mapfrontiers.group_owners_desc");
    private static final Component groupEveryoneDescLabel = Component.translatable("mapfrontiers.group_everyone_desc");
    private static final Component createGlobalFrontierLabel = Component.translatable("mapfrontiers.create_global_frontier");
    private static final Component deleteGlobalFrontierLabel = Component.translatable("mapfrontiers.delete_global_frontier");
    private static final Component updateGlobalFrontierLabel = Component.translatable("mapfrontiers.update_global_frontier");
    private static final Component updateSettingsLabel = Component.translatable("mapfrontiers.update_settings");
    private static final Component sharePersonalFrontierLabel = Component.translatable("mapfrontiers.share_personal_frontier");
    private static final Component doneLabel = Component.translatable("gui.done");

    private final boolean showKeyHint;

    private FrontierSettings settings;
    private TabbedBox tabbedBox;
    private LinkButton buttonWeb;
    private LinkButton buttonCurseForge;
    private LinkButton buttonModrinth;
    private StringWidget labelTitleAnnouncementDuration;
    private StringWidget labelTitleAnnouncementTimeout;
    private StringWidget labelTitleAnnouncementAboveHotbar;
    private StringWidget labelAnnounceUnnamedFrontiers;
    private StringWidget labelSnapDistance;
    private StringWidget labelFullscreenButtons;
    private StringWidget labelAskConfirmationFrontierDelete;
    private StringWidget labelAskConfirmationGroupDelete;
    private StringWidget labelAskConfirmationUserDelete;
    private StringWidget labelHUDEnabled;
    private TextBoxInt textTitleAnnouncementDuration;
    private TextBoxInt textTitleAnnouncementTimeout;
    private OptionButton buttonTitleAnnouncementAboveHotbar;
    private OptionButton buttonAnnounceUnnamedFrontiers;
    private TextBoxInt textSnapDistance;
    private SimpleButton buttonFrontierAppearance;
    private OptionButton buttonFullscreenButtons;
    private OptionButton buttonAskConfirmationFrontierDelete;
    private OptionButton buttonAskConfirmationGroupDelete;
    private OptionButton buttonAskConfirmationUserDelete;
    private OptionButton buttonHUDEnabled;
    private SimpleButton buttonEditHUD;
    private ScrollBox groups;
    private MultiLineTextWidget labelGroupDesc;
    private ScrollBox users;
    private TextBox textNewGroupName;
    private IconButton buttonNewGroup;
    private TextBoxUser textNewUser;
    private IconButton buttonNewUser;
    private TextBox textGroupName;
    private MultiLineTextWidget labelCreateFrontier;
    private MultiLineTextWidget labelDeleteFrontier;
    private MultiLineTextWidget labelUpdateFrontier;
    private MultiLineTextWidget labelUpdateSettings;
    private MultiLineTextWidget labelSharePersonalFrontier;
    private ScrollBox groupsActions;
    private SimpleButton buttonDone;

    private boolean canEditGroups;
    private Tab tabSelected;
    private int ticksSinceLastUpdate = 0;

    public ModSettings(boolean showKeyHint) {
        super(titleLabel, 696, 366);
        this.showKeyHint = showKeyHint;

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

    @Override
    public void initScreen() {
        if (tabSelected == null) {
            tabSelected = MapFrontiersClient.getLastSettingsTab();
        }

        canEditGroups = MapFrontiersClient.isModOnServer() && MapFrontiersClient.getSettingsProfile().updateSettings == SettingsProfile.State.Enabled;
        if (!canEditGroups) {
            if (tabSelected == Tab.Groups || tabSelected == Tab.Actions) {
                tabSelected = Tab.Credits;
            }
        }

        tabbedBox = content.addChild(new TabbedBox(font, actualWidth - 80, actualHeight - 64, (tab) -> {
            tabSelected = Tab.values()[tab];

            if (tabSelected == Tab.Actions) {
                updateGroupsActions();
            }

            updateButtonsVisibility();
        }));
        tabbedBox.addTab(tabCreditsLabel, true);
        tabbedBox.addTab(tabGeneralLabel, true);
        tabbedBox.addTab(tabGroupsLabel, canEditGroups);
        tabbedBox.addTab(tabActionsLabel, canEditGroups);

        LinearLayout creditsLayout = LinearLayout.vertical().spacing(4);
        creditsLayout.defaultCellSetting().alignHorizontallyCenter();
        tabbedBox.addChild(creditsLayout, Tab.Credits.ordinal(), LayoutSettings.defaults().alignHorizontallyCenter().alignVerticallyTop());

        creditsLayout.addChild(SpacerElement.height(16));
        creditsLayout.addChild(new StringWidget(createdByLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        buttonWeb = creditsLayout.addChild(new LinkButton(font, webLinkLabel, (b) -> {
            MapFrontiersClient.setLastSettingsTab(tabSelected);
            ConfirmLinkScreen.confirmLinkNow(this, webURL, false);
        }) {
            public @NotNull ScreenRectangle getRectangle() {
                return new ScreenRectangle(this.getX() - 100, this.getY(), this.getWidth() + 200, this.getHeight());
            }
        });
        creditsLayout.addChild(SpacerElement.height(16));

        creditsLayout.addChild(new StringWidget(manyThanksLabel, font).setColor(ColorConstants.TEXT_MEDIUM));
        creditsLayout.addChild(SpacerElement.height(16));

        creditsLayout.addChild(new StringWidget(projectLabel, font).setColor(ColorConstants.TEXT_MEDIUM));
        buttonCurseForge = creditsLayout.addChild(new LinkButton(font, curseForgeLinkLabel, (b) -> {
            MapFrontiersClient.setLastSettingsTab(tabSelected);
            ConfirmLinkScreen.confirmLinkNow(this, curseForgeURL, false);
        }));
        buttonModrinth = creditsLayout.addChild(new LinkButton(font, modrinthLinkLabel, (b) -> {
            MapFrontiersClient.setLastSettingsTab(tabSelected);
            ConfirmLinkScreen.confirmLinkNow(this, modrinthURL, false);
        }));
        creditsLayout.addChild(SpacerElement.height(16));

        creditsLayout.addChild(new StringWidget(patreonLabel, font).setColor(ColorConstants.TEXT_MEDIUM));
        buttonModrinth = creditsLayout.addChild(new LinkButton(font, patreonLinkLabel, (b) -> {
            MapFrontiersClient.setLastSettingsTab(tabSelected);
            ConfirmLinkScreen.confirmLinkNow(this, patreonURL, false);
        }));

        LinearLayout generalLayout = LinearLayout.vertical().spacing(4);
        generalLayout.defaultCellSetting().alignHorizontallyCenter();
        tabbedBox.addChild(generalLayout, Tab.General.ordinal(), LayoutSettings.defaults().alignHorizontallyCenter().alignVerticallyTop());

        generalLayout.addChild(SpacerElement.height(16));
        generalLayout.addChild(new StringWidget(frontiersLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));

        GridLayout miscLayout = new GridLayout().spacing(4);
        miscLayout.defaultCellSetting().alignHorizontallyLeft();
        generalLayout.addChild(miscLayout);
        int row = 0;

        labelTitleAnnouncementDuration = miscLayout.addChild(new StringWidget(titleAnnouncementDurationLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTitleAnnouncementDuration.setTooltip(titleAnnouncementDurationTooltip);
        textTitleAnnouncementDuration = miscLayout.addChild(new TextBoxInt(70, 0, 1200, font, 40), row++, 1);
        textTitleAnnouncementDuration.setValue(String.valueOf(Config.titleAnnouncementDuration));
        textTitleAnnouncementDuration.setMaxLength(4);
        textTitleAnnouncementDuration.setValueChangedCallback(value -> Config.titleAnnouncementDuration = value);

        labelTitleAnnouncementTimeout = miscLayout.addChild(new StringWidget(titleAnnouncementTimeoutLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTitleAnnouncementTimeout.setTooltip(titleAnnouncementTimeoutTooltip);
        textTitleAnnouncementTimeout = miscLayout.addChild(new TextBoxInt(0, 0, 1200, font, 40), row++, 1);
        textTitleAnnouncementTimeout.setValue(String.valueOf(Config.titleAnnouncementTimeout));
        textTitleAnnouncementTimeout.setMaxLength(4);
        textTitleAnnouncementTimeout.setValueChangedCallback(value -> Config.titleAnnouncementTimeout = value);

        labelTitleAnnouncementAboveHotbar = miscLayout.addChild(new StringWidget(titleAnnouncementAboveHotbarLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTitleAnnouncementAboveHotbar.setTooltip(titleAnnouncementAboveHotbarTooltip);
        buttonTitleAnnouncementAboveHotbar = miscLayout.addChild(new OptionButton(font, 40, (b) -> Config.titleAnnouncementAboveHotbar = b.getSelected() == 0) {
            public @NotNull ScreenRectangle getRectangle() {
                return new ScreenRectangle(this.getX() - 300, this.getY(), this.getWidth() + 300, this.getHeight());
            }
        }, row++, 1);
        buttonTitleAnnouncementAboveHotbar.addOption(onLabel);
        buttonTitleAnnouncementAboveHotbar.addOption(offLabel);
        buttonTitleAnnouncementAboveHotbar.setSelected(Config.titleAnnouncementAboveHotbar ? 0 : 1);

        labelAnnounceUnnamedFrontiers = miscLayout.addChild(new StringWidget(announceUnnamedFrontiersLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelAnnounceUnnamedFrontiers.setTooltip(announceUnnamedFrontiersTooltip);
        buttonAnnounceUnnamedFrontiers = miscLayout.addChild(new OptionButton(font, 40, (b) -> Config.announceUnnamedFrontiers = b.getSelected() == 0), row++, 1);
        buttonAnnounceUnnamedFrontiers.addOption(onLabel);
        buttonAnnounceUnnamedFrontiers.addOption(offLabel);
        buttonAnnounceUnnamedFrontiers.setSelected(Config.announceUnnamedFrontiers ? 0 : 1);

        labelSnapDistance = miscLayout.addChild(new StringWidget(snapDistanceLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelSnapDistance.setTooltip(snapDistanceTooltip);
        textSnapDistance = miscLayout.addChild(new TextBoxInt(8, 0, 16, font, 40), row++, 1);
        textSnapDistance.setValue(String.valueOf(Config.snapDistance));
        textSnapDistance.setMaxLength(2);
        textSnapDistance.setValueChangedCallback(value -> Config.snapDistance = value);

        buttonFrontierAppearance = new SimpleButton(font, 144, frontierAppearanceLabel, (b) -> {
            new FrontierAppearanceDialog().display();
        }) {
            public @NotNull ScreenRectangle getRectangle() {
                return new ScreenRectangle(this.getX(), this.getY(), getWidth() + 100, this.getHeight());
            }
        };
        miscLayout.addChild(buttonFrontierAppearance, row++, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());

        SimpleButton buttonVisibility = new SimpleButton(font, 144, forcedVisibilityLabel, (b) -> {
            new VisibilityDialog(createForcedVisibility(), createForcedVisibilityMask(), this::setForcedVisibility).display();
        }) {
            public @NotNull ScreenRectangle getRectangle() {
                return new ScreenRectangle(this.getX(), this.getY(), getWidth() + 100, this.getHeight());
            }
        };
        miscLayout.addChild(buttonVisibility, row++, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());

        miscLayout.addChild(SpacerElement.height(4), row++, 0);
        miscLayout.addChild(new StringWidget(guiLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT), row++, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());

        labelFullscreenButtons = miscLayout.addChild(new StringWidget(fullscreenButtonsLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelFullscreenButtons.setTooltip(fullscreenButtonsTooltip);
        buttonFullscreenButtons = miscLayout.addChild(new OptionButton(font, 40, (b) -> Config.fullscreenButtons = b.getSelected() == 0), row++, 1);
        buttonFullscreenButtons.addOption(onLabel);
        buttonFullscreenButtons.addOption(offLabel);
        buttonFullscreenButtons.setSelected(Config.fullscreenButtons ? 0 : 1);

        labelAskConfirmationFrontierDelete = miscLayout.addChild(new StringWidget(askConfirmationFrontierDeleteLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelAskConfirmationFrontierDelete.setTooltip(askConfirmationFrontierDeleteTooltip);
        buttonAskConfirmationFrontierDelete = miscLayout.addChild(new OptionButton(font, 40, (b) -> Config.askConfirmationFrontierDelete = b.getSelected() == 0), row++, 1);
        buttonAskConfirmationFrontierDelete.addOption(onLabel);
        buttonAskConfirmationFrontierDelete.addOption(offLabel);
        buttonAskConfirmationFrontierDelete.setSelected(Config.askConfirmationFrontierDelete ? 0 : 1);

        labelAskConfirmationGroupDelete = miscLayout.addChild(new StringWidget(askConfirmationGroupDeleteLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelAskConfirmationGroupDelete.setTooltip(askConfirmationGroupDeleteTooltip);
        buttonAskConfirmationGroupDelete = miscLayout.addChild(new OptionButton(font, 40, (b) -> Config.askConfirmationGroupDelete = b.getSelected() == 0), row++, 1);
        buttonAskConfirmationGroupDelete.addOption(onLabel);
        buttonAskConfirmationGroupDelete.addOption(offLabel);
        buttonAskConfirmationGroupDelete.setSelected(Config.askConfirmationGroupDelete ? 0 : 1);

        labelAskConfirmationUserDelete = miscLayout.addChild(new StringWidget(askConfirmationUserDeleteLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelAskConfirmationUserDelete.setTooltip(askConfirmationUserDeleteTooltip);
        buttonAskConfirmationUserDelete = miscLayout.addChild(new OptionButton(font, 40, (b) -> Config.askConfirmationUserDelete = b.getSelected() == 0), row++, 1);
        buttonAskConfirmationUserDelete.addOption(onLabel);
        buttonAskConfirmationUserDelete.addOption(offLabel);
        buttonAskConfirmationUserDelete.setSelected(Config.askConfirmationUserDelete ? 0 : 1);

        miscLayout.addChild(SpacerElement.height(4), row++, 0);
        miscLayout.addChild(new StringWidget(hudLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT), row++, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());

        labelHUDEnabled = miscLayout.addChild(new StringWidget(hudEnabledLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelHUDEnabled.setTooltip(hudEnabledTooltip);
        buttonHUDEnabled = miscLayout.addChild(new OptionButton(font, 40, (b) -> {
            Config.hudEnabled = b.getSelected() == 0;
            updateButtonsVisibility();
        }), row++, 1);
        buttonHUDEnabled.addOption(onLabel);
        buttonHUDEnabled.addOption(offLabel);
        buttonHUDEnabled.setSelected(Config.hudEnabled ? 0 : 1);

        buttonEditHUD = generalLayout.addChild(new SimpleButton(font, 100, editHudLabel, (b) -> {
            MapFrontiersClient.setLastSettingsTab(tabSelected);
            new HUDSettings().display();
        }) {
            public @NotNull ScreenRectangle getRectangle() {
                return new ScreenRectangle(this.getX(), this.getY(), getWidth() + 100, this.getHeight());
            }
        });

        LinearLayout groupsLayout = LinearLayout.horizontal().spacing(4);
        groupsLayout.defaultCellSetting().alignHorizontallyLeft();
        tabbedBox.addChild(groupsLayout, Tab.Groups.ordinal());

        LinearLayout groupsCol = LinearLayout.vertical().spacing(4);
        groupsCol.defaultCellSetting().alignHorizontallyCenter();
        groupsLayout.addChild(groupsCol);

        groups = groupsCol.addChild(new ScrollBox(actualHeight - 120, 160, 16));
        groups.setElementClickedCallback(element -> {
            groupClicked((GroupElement) element);
            updateButtonsVisibility();
        });
        groups.setElementDeletePressedCallback(element -> {
            if (groups.getSelectedElement() != null) {
                groupClicked((GroupElement) element);
            }
            if (Config.askConfirmationGroupDelete) {
                new DeleteConfirmationDialog(
                        "mapfrontiers.delete_group_dialog",
                        response -> {
                            if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                                Config.askConfirmationGroupDelete = false;
                                buttonAskConfirmationGroupDelete.setSelected(1);
                                ClientEventHandler.postUpdatedConfigEvent();
                            }
                            groups.removeElement(element);
                            settings.removeCustomGroup(((GroupElement) element).getGroup());
                            sendChangesToServer();
                        }
                ).display();
            } else {
                groups.removeElement(element);
                settings.removeCustomGroup(((GroupElement) element).getGroup());
                sendChangesToServer();
            }
        });

        LinearLayout newGroupLayout = LinearLayout.horizontal().spacing(4);
        groupsCol.addChild(newGroupLayout);

        textNewGroupName = newGroupLayout.addChild(new TextBox(font, 140, I18n.get("mapfrontiers.new_group_name")));
        textNewGroupName.setMaxLength(22);
        textNewGroupName.setSubmitCallback((value) -> newGroupPressed());

        buttonNewGroup = newGroupLayout.addChild(new IconButton(IconButton.Type.Add, (b) -> newGroupPressed()));

        LinearLayout usersCol = LinearLayout.vertical().spacing(4);
        usersCol.defaultCellSetting().alignHorizontallyLeft();
        groupsLayout.addChild(usersCol);

        textGroupName = usersCol.addChild(new TextBox(font, 140));
        textGroupName.setMaxLength(22);
        textGroupName.setLostFocusCallback(value -> {
            if (tabSelected == Tab.Groups) {
                GroupElement groupElement = (GroupElement) groups.getSelectedElement();
                if (groupElement != null) {
                    groupElement.getGroup().setName(value);
                    sendChangesToServer();
                }
            }
        });

        labelGroupDesc = usersCol.addChild(new MultiLineTextWidget(groupOpsDescLabel, font).setColor(ColorConstants.TEXT));

        users = usersCol.addChild(new ScrollBox(actualHeight - 160, 258, 16));
        users.setElementDeletePressedCallback(element -> {
            SettingsGroup group = ((GroupElement) groups.getSelectedElement()).getGroup();
            if (Config.askConfirmationUserDelete) {
                new DeleteConfirmationDialog(
                        "mapfrontiers.delete_user_dialog",
                        response -> {
                            if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                                Config.askConfirmationUserDelete = false;
                                buttonAskConfirmationUserDelete.setSelected(1);
                                ClientEventHandler.postUpdatedConfigEvent();
                            }
                            users.removeElement(element);
                            group.removeUser(((UserElement) element).getUser());
                            sendChangesToServer();
                        }
                ).display();
            } else {
                users.removeElement(element);
                group.removeUser(((UserElement) element).getUser());
                sendChangesToServer();
            }
        });

        LinearLayout newUserLayout = LinearLayout.horizontal().spacing(4);
        usersCol.addChild(newUserLayout);

        textNewUser = newUserLayout.addChild(new TextBoxUser(minecraft, font, 238, I18n.get("mapfrontiers.new_user")));
        textNewUser.setMaxLength(38);
        textNewUser.setSubmitCallback((value) -> newUserPressed());

        buttonNewUser = newUserLayout.addChild(new IconButton(IconButton.Type.Add, (b) -> newUserPressed()));

        LinearLayout actionsLayout = LinearLayout.vertical().spacing(8);
        actionsLayout.defaultCellSetting().alignHorizontallyCenter();
        tabbedBox.addChild(actionsLayout, Tab.Actions.ordinal());

        LinearLayout actionsHeader = LinearLayout.horizontal();
        actionsLayout.addChild(actionsHeader);

        labelCreateFrontier = actionsHeader.addChild(new MultiLineTextWidget(createGlobalFrontierLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        labelCreateFrontier.setCentered(true);
        labelDeleteFrontier = actionsHeader.addChild(new MultiLineTextWidget(deleteGlobalFrontierLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        labelDeleteFrontier.setCentered(true);
        labelUpdateFrontier = actionsHeader.addChild(new MultiLineTextWidget(updateGlobalFrontierLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        labelUpdateFrontier.setCentered(true);
        labelUpdateSettings = actionsHeader.addChild(new MultiLineTextWidget(updateSettingsLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        labelUpdateSettings.setCentered(true);
        labelSharePersonalFrontier = actionsHeader.addChild(new MultiLineTextWidget(sharePersonalFrontierLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        labelSharePersonalFrontier.setCentered(true);

        groupsActions = actionsLayout.addChild(new ScrollBox(actualHeight - 128, 430, 16));

        buttonDone = bottomButtons.addChild(new SimpleButton(font, 140, doneLabel, (b) -> onClose()));

        tabbedBox.setTabSelected(tabSelected.ordinal());
        updateButtonsVisibility();

        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketRequestFrontierSettings());
        }
    }

    @Override
    public void repositionElements() {
        tabbedBox.setSize(actualWidth - 80, actualHeight - 64);
        groups.setHeight(actualHeight - 120);
        users.setHeight(actualHeight - 160);
        groupsActions.setHeight(actualHeight - 128);
        super.repositionElements();

        textNewUser.setY(textNewGroupName.getY());
        buttonNewUser.setY(buttonNewGroup.getY());

        labelCreateFrontier.setX(groupsActions.getX() + 160 - labelCreateFrontier.getWidth() / 2);
        labelDeleteFrontier.setX(groupsActions.getX() + 220 - labelDeleteFrontier.getWidth() / 2);
        labelUpdateFrontier.setX(groupsActions.getX() + 280 - labelUpdateFrontier.getWidth() / 2);
        labelUpdateSettings.setX(groupsActions.getX() + 340 - labelUpdateSettings.getWidth() / 2);
        labelSharePersonalFrontier.setX(groupsActions.getX() + 400 - labelSharePersonalFrontier.getWidth() / 2);
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
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        tabbedBox.renderBackground(graphics, mouseX, mouseY, partialTicks);

        if (tabSelected == Tab.Credits || tabSelected == Tab.General) {
            int y = tabbedBox.getY() + tabbedBox.getHeight() - 19;
            graphics.drawString(font, creditsTranslationLabel, tabbedBox.getX() + 10, y, ColorConstants.TEXT_HIGHLIGHT);
            graphics.drawString(font, versionLabel, tabbedBox.getX() + tabbedBox.getWidth() - font.width(versionLabel) - 10, y, ColorConstants.TEXT_HIGHLIGHT);
            if (showKeyHint) {
                Component key = MapFrontiersClient.getOpenSettingsKey();
                if (key != null) {
                    graphics.drawCenteredString(font, Component.translatable(keyHintkey, key), tabbedBox.getX() + tabbedBox.getWidth() / 2, y, ColorConstants.TEXT_HIGHLIGHT);
                }
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

    private FrontierData.VisibilityData createForcedVisibility() {
        FrontierData.VisibilityData visibilityData = new FrontierData.VisibilityData();
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Frontier, Config.frontierVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.AnnounceInChat, Config.announceInChat == Config.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.AnnounceInTitle, Config.announceInTitle == Config.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Fullscreen, Config.fullscreenVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenName, Config.fullscreenNameVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenOwner, Config.fullscreenOwnerVisibility == Config.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenBanner, Config.fullscreenBannerVisibility == Config.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenDay, Config.fullscreenDayVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenNight, Config.fullscreenNightVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenUnderground, Config.fullscreenUndergroundVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenTopo, Config.fullscreenTopoVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenBiome, Config.fullscreenBiomeVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Minimap, Config.minimapVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapName, Config.minimapNameVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapOwner, Config.minimapOwnerVisibility == Config.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapBanner, Config.minimapBannerVisibility == Config.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapDay, Config.minimapDayVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapNight, Config.minimapNightVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapUnderground, Config.minimapUndergroundVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapTopo, Config.minimapTopoVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapBiome, Config.minimapBiomeVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Webmap, Config.webmapVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapName, Config.webmapNameVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapOwner, Config.webmapOwnerVisibility == Config.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapBanner, Config.webmapBannerVisibility == Config.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapDay, Config.webmapDayVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapNight, Config.webmapNightVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapUnderground, Config.webmapUndergroundVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapTopo, Config.webmapTopoVisibility != Config.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapBiome, Config.webmapBiomeVisibility != Config.Visibility.Never);
        return visibilityData;
    }

    private FrontierData.VisibilityData createForcedVisibilityMask() {
        FrontierData.VisibilityData visibilityData = new FrontierData.VisibilityData();
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Frontier, Config.frontierVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.AnnounceInChat, Config.announceInChat != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.AnnounceInTitle, Config.announceInTitle != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Fullscreen, Config.fullscreenVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenName, Config.fullscreenNameVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenOwner, Config.fullscreenOwnerVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenBanner, Config.fullscreenBannerVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenDay, Config.fullscreenDayVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenNight, Config.fullscreenNightVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenUnderground, Config.fullscreenUndergroundVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenTopo, Config.fullscreenTopoVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenBiome, Config.fullscreenBiomeVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Minimap, Config.minimapVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapName, Config.minimapNameVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapOwner, Config.minimapOwnerVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapBanner, Config.minimapBannerVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapDay, Config.minimapDayVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapNight, Config.minimapNightVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapUnderground, Config.minimapUndergroundVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapTopo, Config.minimapTopoVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapBiome, Config.minimapBiomeVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Webmap, Config.webmapVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapName, Config.webmapNameVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapOwner, Config.webmapOwnerVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapBanner, Config.webmapBannerVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapDay, Config.webmapDayVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapNight, Config.webmapNightVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapUnderground, Config.webmapUndergroundVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapTopo, Config.webmapTopoVisibility != Config.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapBiome, Config.webmapBiomeVisibility != Config.Visibility.Custom);
        return visibilityData;
    }

    private void setForcedVisibility(FrontierData.VisibilityData visibilityData, FrontierData.VisibilityData visibilityDataMask) {
        Config.frontierVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.Frontier);
        Config.announceInChat = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.AnnounceInChat);
        Config.announceInTitle = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.AnnounceInTitle);
        Config.fullscreenVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.Fullscreen);
        Config.fullscreenNameVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenName);
        Config.fullscreenOwnerVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenOwner);
        Config.fullscreenBannerVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenBanner);
        Config.fullscreenDayVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenDay);
        Config.fullscreenNightVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenNight);
        Config.fullscreenUndergroundVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenUnderground);
        Config.fullscreenTopoVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenTopo);
        Config.fullscreenBiomeVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenBiome);
        Config.minimapVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.Minimap);
        Config.minimapNameVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapName);
        Config.minimapOwnerVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapOwner);
        Config.minimapBannerVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapBanner);
        Config.minimapDayVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapDay);
        Config.minimapNightVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapNight);
        Config.minimapUndergroundVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapUnderground);
        Config.minimapTopoVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapTopo);
        Config.minimapBiomeVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapBiome);
        Config.webmapVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.Webmap);
        Config.webmapNameVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapName);
        Config.webmapOwnerVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapOwner);
        Config.webmapBannerVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapBanner);
        Config.webmapDayVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapDay);
        Config.webmapNightVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapNight);
        Config.webmapUndergroundVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapUnderground);
        Config.webmapTopoVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapTopo);
        Config.webmapBiomeVisibility = getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapBiome);
    }

    private Config.Visibility getVisibilityValue(FrontierData.VisibilityData visibilityData, FrontierData.VisibilityData visibilityDataMask, FrontierData.VisibilityData.Visibility visibility) {
        if (visibilityDataMask.getValue(visibility)) {
            return visibilityData.getValue(visibility) ? Config.Visibility.Always : Config.Visibility.Never;
        }
        return Config.Visibility.Custom;
    }

    private void newGroupPressed() {
        if (settings != null) {
            SettingsGroup group = settings.createCustomGroup(textNewGroupName.getValue());
            textNewGroupName.setValue("");
            GroupElement element = new GroupElement(font, group);
            groups.addElement(element);
            groups.scrollBottom();
            groupClicked(element);
            groupsActions.scrollBottom();

            sendChangesToServer();
        }
    }

    private void newUserPressed() {
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
        UserElement element = new UserElement(font, user);
        users.addElement(element);
        users.scrollBottom();

        textNewUser.setValue("");

        sendChangesToServer();
    }

    @Override
    public void onClose() {
        ClientEventHandler.postUpdatedConfigEvent();
        MapFrontiersClient.setLastSettingsTab(tabSelected);
        ClientEventHandler.unsubscribeAllEvents(this);
        super.onClose();
    }

    public void setFrontierSettings(FrontierSettings settings) {
        this.settings = settings;

        GroupElement selectedElement = (GroupElement) groups.getSelectedElement();
        int selectedIndex = groups.getSelectedIndex();

        groups.removeAll();
        groups.addElement(new GroupElement(font, settings.getOPsGroup()));
        groups.addElement(new GroupElement(font, settings.getOwnersGroup()));
        groups.addElement(new GroupElement(font, settings.getEveryoneGroup()));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groups.addElement(new GroupElement(font, group));
        }

        updateGroupsActions();
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

    private void updateButtonsVisibility() {
        buttonEditHUD.visible = tabSelected == Tab.General && Config.hudEnabled && minecraft.player != null;
        buttonFrontierAppearance.visible = tabSelected == Tab.General && minecraft.player != null;
        textNewUser.visible = canAddNewUser();
        buttonNewUser.visible = canAddNewUser();
    }

    public void groupClicked(GroupElement element) {
        groups.selectElement(element);
        SettingsGroup group = element.getGroup();
        textGroupName.setValue(group.getName());
        textGroupName.setEditable(!group.isSpecial());
        textGroupName.setBordered(!group.isSpecial());
        textGroupName.setFocused(false);

        if (group == settings.getOPsGroup()) {
            labelGroupDesc.setMessage(groupOpsDescLabel);
        } else if (group == settings.getOwnersGroup()) {
            labelGroupDesc.setMessage(groupOwnersDescLabel);
        } else if (group == settings.getEveryoneGroup()) {
            labelGroupDesc.setMessage(groupEveryoneDescLabel);
        } else {
            labelGroupDesc.setMessage(Component.empty());
        }

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
                users.addElement(new UserElement(font, user));
            }
        }

        buttonNewUser.visible = canAddNewUser();
        textNewUser.visible = canAddNewUser();
        ticksSinceLastUpdate = 100;
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
