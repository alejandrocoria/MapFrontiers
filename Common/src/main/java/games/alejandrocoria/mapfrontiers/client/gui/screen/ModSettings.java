package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
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
import games.alejandrocoria.mapfrontiers.client.gui.dialog.PathStyleDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.VisibilityDialog;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.config.BooleanConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
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
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.function.Consumer;

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
    private static final Component defaultPathStyleLabel = Component.translatable("mapfrontiers.default_path_style");
    private static final Component forcedVisibilityLabel = Component.translatable("mapfrontiers.forced_visibility");
    private static final Component guiLabel = Component.translatable("mapfrontiers.gui");
    private static final Component hudLabel = Component.translatable("mapfrontiers.hud");
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
    private static final int TABBED_BOX_MARGIN_X = 80;
    private static final int TABBED_BOX_MARGIN_Y = 64;
    private static final int SECTION_SPACING_SMALL = 4;
    private static final int SECTION_SPACING_MEDIUM = 8;
    private static final int DEFAULT_OPTION_WIDTH = 40;
    private static final int DEFAULT_TEXTBOX_WIDTH = 40;
    private static final int FRONTIER_BUTTON_MIN_WIDTH = 144;
    private static final int FRONTIER_BUTTON_HORIZONTAL_PADDING = 8;
    private static final int WIDE_BUTTON_EXTRA_WIDTH = 100;
    private static final int WIDE_TEXTBOX_EXTRA_WIDTH = 300;
    private static final int WIDE_LINK_EXTRA_WIDTH = 200;
    private static final int GROUPS_SCROLL_WIDTH = 160;
    private static final int USERS_SCROLL_WIDTH = 258;
    private static final int ACTIONS_SCROLL_WIDTH = 430;
    private static final int DONE_BUTTON_WIDTH = 140;
    private static final int GROUP_NAME_WIDTH = 140;
    private static final int NEW_USER_WIDTH = 238;

    private final boolean showKeyHint;

    private FrontierSettings settings;
    private TabbedBox tabbedBox;
    private SimpleButton buttonFrontierAppearance;
    private SimpleButton buttonDefaultPathStyle;
    private OptionButton buttonAskConfirmationGroupDelete;
    private OptionButton buttonAskConfirmationUserDelete;
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

    private boolean canEditGroups;
    private Tab tabSelected;
    private int ticksSinceLastUpdate = 0;
    private final boolean subscribeToSettingsProfileEvents;

    public ModSettings(boolean showKeyHint) {
        super(titleLabel, 696, 450);
        this.showKeyHint = showKeyHint;
        subscribeToSettingsProfileEvents = MapFrontiersClient.isJourneyMapPluginAvailable();

        if (subscribeToSettingsProfileEvents) {
            MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
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
    }

    @Override
    public void initScreen() {
        resolveInitialState();
        tabbedBox = createTabbedBox();

        buildCreditsTab();
        buildGeneralTab();
        buildGroupsTab();
        buildActionsTab();

        buildBottomButtons();

        refreshViewState();
        requestInitialDataIfNeeded();
    }

    private void resolveInitialState() {
        if (tabSelected == null) {
            tabSelected = MapFrontiersClient.getLastSettingsTab();
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        canEditGroups = MapFrontiersClient.isModOnServer()
                && profile != null
                && profile.updateSettings == SettingsProfile.State.Enabled;

        if (!canEditGroups && (tabSelected == Tab.Groups || tabSelected == Tab.Actions)) {
            tabSelected = Tab.Credits;
        }
    }

    private TabbedBox createTabbedBox() {
        TabbedBox tabs = content.addChild(new TabbedBox(font, actualWidth - TABBED_BOX_MARGIN_X,
                actualHeight - TABBED_BOX_MARGIN_Y, this::onTabChanged));
        tabs.addTab(tabCreditsLabel, true);
        tabs.addTab(tabGeneralLabel, true);
        tabs.addTab(tabGroupsLabel, canEditGroups);
        tabs.addTab(tabActionsLabel, canEditGroups);
        return tabs;
    }

    private void onTabChanged(int tab) {
        tabSelected = Tab.values()[tab];

        if (tabSelected == Tab.Actions) {
            updateGroupsActions();
        }

        updateButtonsVisibility();
    }

    private void buildCreditsTab() {
        LinearLayout creditsLayout = LinearLayout.vertical().spacing(SECTION_SPACING_SMALL);
        creditsLayout.defaultCellSetting().alignHorizontallyCenter();
        tabbedBox.addChild(creditsLayout, Tab.Credits.ordinal(),
                LayoutSettings.defaults().alignHorizontallyCenter().alignVerticallyTop());

        creditsLayout.addChild(SpacerElement.height(16));
        creditsLayout.addChild(new StringWidget(createdByLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        creditsLayout.addChild(createWideExternalLinkButton(webLinkLabel, webURL));
        creditsLayout.addChild(SpacerElement.height(16));

        creditsLayout.addChild(new StringWidget(manyThanksLabel, font).setColor(ColorConstants.TEXT_MEDIUM));
        creditsLayout.addChild(SpacerElement.height(16));

        creditsLayout.addChild(new StringWidget(projectLabel, font).setColor(ColorConstants.TEXT_MEDIUM));
        creditsLayout.addChild(createExternalLinkButton(curseForgeLinkLabel, curseForgeURL));
        creditsLayout.addChild(createExternalLinkButton(modrinthLinkLabel, modrinthURL));
        creditsLayout.addChild(SpacerElement.height(16));

        creditsLayout.addChild(new StringWidget(patreonLabel, font).setColor(ColorConstants.TEXT_MEDIUM));
        creditsLayout.addChild(createExternalLinkButton(patreonLinkLabel, patreonURL));
    }

    private void buildGeneralTab() {
        LinearLayout generalLayout = LinearLayout.vertical().spacing(SECTION_SPACING_SMALL);
        generalLayout.defaultCellSetting().alignHorizontallyCenter();
        tabbedBox.addChild(generalLayout, Tab.General.ordinal(),
                LayoutSettings.defaults().alignHorizontallyCenter().alignVerticallyTop());

        generalLayout.addChild(SpacerElement.height(16));
        generalLayout.addChild(new StringWidget(frontiersLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));

        GridLayout settingsGrid = new GridLayout().spacing(SECTION_SPACING_SMALL);
        settingsGrid.defaultCellSetting().alignHorizontallyLeft().alignVerticallyMiddle();
        generalLayout.addChild(settingsGrid);

        int row = 0;
        row = buildFrontiersSection(settingsGrid, row);
        row = buildGuiSection(settingsGrid, row);
        buildHudSection(generalLayout, settingsGrid, row);
    }

    private int buildFrontiersSection(GridLayout settingsGrid, int row) {
        row = addIntSettingRow(settingsGrid, row, ClientConfig.TITLE_ANNOUNCEMENT_DURATION,
                createWideIntConfigTextBox(ClientConfig.TITLE_ANNOUNCEMENT_DURATION, DEFAULT_TEXTBOX_WIDTH, 4));
        row = addIntSettingRow(settingsGrid, row, ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT, DEFAULT_TEXTBOX_WIDTH, 4);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.SNAP_DISTANCE, DEFAULT_TEXTBOX_WIDTH, 2);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.PATH_PROXIMITY_ENTER_DISTANCE, DEFAULT_TEXTBOX_WIDTH, 3);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.PATH_PROXIMITY_EXIT_DISTANCE, DEFAULT_TEXTBOX_WIDTH, 3);

        int frontierButtonWidth = ScreenHelper.getPaddedMaxTextWidth(font, FRONTIER_BUTTON_MIN_WIDTH,
                FRONTIER_BUTTON_HORIZONTAL_PADDING, frontierAppearanceLabel, defaultPathStyleLabel,
                forcedVisibilityLabel);

        buttonFrontierAppearance = createWideSimpleButton(frontierButtonWidth, frontierAppearanceLabel,
                b -> onFrontierAppearancePressed());
        settingsGrid.addChild(buttonFrontierAppearance, row++, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());

        buttonDefaultPathStyle = createWideSimpleButton(frontierButtonWidth, defaultPathStyleLabel,
                b -> onDefaultPathStylePressed());
        settingsGrid.addChild(buttonDefaultPathStyle, row++, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());

        settingsGrid.addChild(createWideSimpleButton(frontierButtonWidth, forcedVisibilityLabel,
                b -> onForcedVisibilityPressed()), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        return row;
    }

    private int buildGuiSection(GridLayout settingsGrid, int row) {
        settingsGrid.addChild(SpacerElement.height(SECTION_SPACING_SMALL), row++, 0);
        settingsGrid.addChild(new StringWidget(guiLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        row = addOptionSettingRow(settingsGrid, row, ClientConfig.FULLSCREEN_BUTTONS);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE);
        buttonAskConfirmationGroupDelete = createOnOffOptionButton(ClientConfig.ASK_CONFIRMATION_GROUP_DELETE);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_GROUP_DELETE,
                buttonAskConfirmationGroupDelete);
        buttonAskConfirmationUserDelete = createOnOffOptionButton(ClientConfig.ASK_CONFIRMATION_USER_DELETE);
        return addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_USER_DELETE,
                buttonAskConfirmationUserDelete);
    }

    private void buildHudSection(LinearLayout generalLayout, GridLayout settingsGrid, int row) {
        settingsGrid.addChild(SpacerElement.height(SECTION_SPACING_SMALL), row++, 0);
        settingsGrid.addChild(new StringWidget(hudLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        addOptionSettingRow(settingsGrid, row, ClientConfig.HUD_ENABLED, createOnOffOptionButton(ClientConfig.HUD_ENABLED,
                this::onHudEnabledChanged));

        buttonEditHUD = generalLayout.addChild(createWideSimpleButton(100, editHudLabel, b -> onEditHUDPressed()));
    }

    private void buildGroupsTab() {
        LinearLayout groupsLayout = LinearLayout.horizontal().spacing(SECTION_SPACING_SMALL);
        groupsLayout.defaultCellSetting().alignHorizontallyLeft();
        tabbedBox.addChild(groupsLayout, Tab.Groups.ordinal());

        LinearLayout groupsColumn = LinearLayout.vertical().spacing(SECTION_SPACING_SMALL);
        groupsColumn.defaultCellSetting().alignHorizontallyCenter();
        groupsLayout.addChild(groupsColumn);
        buildGroupsList(groupsColumn);
        buildNewGroupControls(groupsColumn);

        LinearLayout usersColumn = LinearLayout.vertical().spacing(SECTION_SPACING_SMALL);
        usersColumn.defaultCellSetting().alignHorizontallyLeft();
        groupsLayout.addChild(usersColumn);
        buildUsersPanel(usersColumn);
        buildNewUserControls(usersColumn);
    }

    private void buildGroupsList(LinearLayout groupsColumn) {
        groups = groupsColumn.addChild(new ScrollBox(actualHeight - 120, GROUPS_SCROLL_WIDTH, 15));
        groups.setElementClickedCallback(element -> {
            onGroupElementClicked((GroupElement) element);
            updateButtonsVisibility();
        });
        groups.setElementDeletePressedCallback(this::onGroupDeletePressed);
    }

    private void buildNewGroupControls(LinearLayout groupsColumn) {
        LinearLayout newGroupLayout = LinearLayout.horizontal().spacing(SECTION_SPACING_SMALL);
        groupsColumn.addChild(newGroupLayout);

        textNewGroupName = newGroupLayout.addChild(new TextBox(font, GROUP_NAME_WIDTH, I18n.get("mapfrontiers.new_group_name")));
        textNewGroupName.setMaxLength(22);
        textNewGroupName.setSubmitCallback(value -> onNewGroupPressed());

        buttonNewGroup = newGroupLayout.addChild(new IconButton(IconButton.Type.Add, b -> onNewGroupPressed()));
    }

    private void buildUsersPanel(LinearLayout usersColumn) {
        textGroupName = usersColumn.addChild(new TextBox(font, GROUP_NAME_WIDTH));
        textGroupName.setMaxLength(22);
        textGroupName.setLostFocusCallback(this::onGroupNameLostFocus);

        labelGroupDesc = usersColumn.addChild(new MultiLineTextWidget(groupOpsDescLabel.copy().withColor(ColorConstants.TEXT), font));

        users = usersColumn.addChild(new ScrollBox(actualHeight - 160, USERS_SCROLL_WIDTH, 16));
        users.setElementDeletePressedCallback(this::onUserDeletePressed);
    }

    private void buildNewUserControls(LinearLayout usersColumn) {
        LinearLayout newUserLayout = LinearLayout.horizontal().spacing(SECTION_SPACING_SMALL);
        usersColumn.addChild(newUserLayout);

        textNewUser = newUserLayout.addChild(new TextBoxUser(minecraft, font, NEW_USER_WIDTH, I18n.get("mapfrontiers.new_user")));
        textNewUser.setMaxLength(38);
        textNewUser.setSubmitCallback(value -> onNewUserPressed());

        buttonNewUser = newUserLayout.addChild(new IconButton(IconButton.Type.Add, b -> onNewUserPressed()));
    }

    private void buildActionsTab() {
        LinearLayout actionsLayout = LinearLayout.vertical().spacing(SECTION_SPACING_MEDIUM);
        actionsLayout.defaultCellSetting().alignHorizontallyCenter();
        tabbedBox.addChild(actionsLayout, Tab.Actions.ordinal());

        LinearLayout actionsHeader = LinearLayout.horizontal();
        actionsLayout.addChild(actionsHeader);

        labelCreateFrontier = actionsHeader.addChild(createActionsHeaderLabel(createGlobalFrontierLabel));
        labelDeleteFrontier = actionsHeader.addChild(createActionsHeaderLabel(deleteGlobalFrontierLabel));
        labelUpdateFrontier = actionsHeader.addChild(createActionsHeaderLabel(updateGlobalFrontierLabel));
        labelUpdateSettings = actionsHeader.addChild(createActionsHeaderLabel(updateSettingsLabel));
        labelSharePersonalFrontier = actionsHeader.addChild(createActionsHeaderLabel(sharePersonalFrontierLabel));

        groupsActions = actionsLayout.addChild(new ScrollBox(actualHeight - 128, ACTIONS_SCROLL_WIDTH, 15));
    }

    private void buildBottomButtons() {
        bottomButtons.addChild(new SimpleButton(font, DONE_BUTTON_WIDTH, doneLabel, b -> onClose()));
    }

    private LinkButton createExternalLinkButton(Component label, String url) {
        return new LinkButton(font, label, b -> openExternalLink(url));
    }

    private LinkButton createWideExternalLinkButton(Component label, String url) {
        return new LinkButton(font, label, b -> openExternalLink(url)) {
            @Override
            public @NotNull ScreenRectangle getRectangle() {
                return new ScreenRectangle(this.getX() - WIDE_LINK_EXTRA_WIDTH / 2, this.getY(),
                        this.getWidth() + WIDE_LINK_EXTRA_WIDTH, this.getHeight());
            }
        };
    }

    private SimpleButton createWideSimpleButton(int width, Component label, SimpleButton.OnPress onPress) {
        return new SimpleButton(font, width, label, onPress) {
            @Override
            public @NotNull ScreenRectangle getRectangle() {
                return new ScreenRectangle(this.getX(), this.getY(), getWidth() + WIDE_BUTTON_EXTRA_WIDTH,
                        this.getHeight());
            }
        };
    }

    private OptionButton createOnOffOptionButton(BooleanConfigEntry entry) {
        return createOnOffOptionButton(entry, entry::set);
    }

    private OptionButton createOnOffOptionButton(BooleanConfigEntry entry, Consumer<Boolean> consumer) {
        OptionButton button = new OptionButton(font, DEFAULT_OPTION_WIDTH, b -> consumer.accept(b.getSelected() == 0));
        button.addOption(onLabel);
        button.addOption(offLabel);
        button.setSelected(entry.get() ? 0 : 1);
        return button;
    }

    private TextBoxInt createIntConfigTextBox(IntConfigEntry entry, int width, int maxLength) {
        TextBoxInt textBox = new TextBoxInt(entry.defaultValue(), entry.minValue(), entry.maxValue(), font, width);
        textBox.setValue(String.valueOf(entry.get()));
        textBox.setMaxLength(maxLength);
        textBox.setValueChangedCallback(entry::set);
        return textBox;
    }

    private TextBoxInt createWideIntConfigTextBox(IntConfigEntry entry, int width, int maxLength) {
        TextBoxInt textBox = new TextBoxInt(entry.defaultValue(), entry.minValue(), entry.maxValue(), font, width) {
            @Override
            public @NotNull ScreenRectangle getRectangle() {
                return new ScreenRectangle(this.getX() - WIDE_TEXTBOX_EXTRA_WIDTH, this.getY(),
                        this.getWidth() + WIDE_TEXTBOX_EXTRA_WIDTH, this.getHeight());
            }
        };
        textBox.setValue(String.valueOf(entry.get()));
        textBox.setMaxLength(maxLength);
        textBox.setValueChangedCallback(entry::set);
        return textBox;
    }

    private int addIntSettingRow(GridLayout settingsGrid, int row, IntConfigEntry entry, int width, int maxLength) {
        return addIntSettingRow(settingsGrid, row, entry, createIntConfigTextBox(entry, width, maxLength));
    }

    private int addIntSettingRow(GridLayout settingsGrid, int row, IntConfigEntry entry, TextBoxInt textBox) {
        settingsGrid.addChild(createConfigLabel(entry), row, 0);
        settingsGrid.addChild(textBox, row, 1);
        return row + 1;
    }

    private int addOptionSettingRow(GridLayout settingsGrid, int row, BooleanConfigEntry entry) {
        return addOptionSettingRow(settingsGrid, row, entry, createOnOffOptionButton(entry));
    }

    private int addOptionSettingRow(GridLayout settingsGrid, int row, BooleanConfigEntry entry, OptionButton button) {
        settingsGrid.addChild(createConfigLabel(entry), row, 0);
        settingsGrid.addChild(button, row, 1);
        return row + 1;
    }

    private StringWidget createConfigLabel(ConfigEntry<?, ?> entry) {
        StringWidget label = new StringWidget(entry.translatedName(), font).setColor(ColorConstants.TEXT);
        label.setTooltip(tooltip(entry));
        return label;
    }

    private MultiLineTextWidget createActionsHeaderLabel(Component label) {
        MultiLineTextWidget widget = new MultiLineTextWidget(label.copy().withColor(ColorConstants.TEXT_HIGHLIGHT), font);
        widget.setCentered(true);
        return widget;
    }

    private void openExternalLink(String url) {
        MapFrontiersClient.setLastSettingsTab(tabSelected);
        ConfirmLinkScreen.confirmLinkNow(this, url, false);
    }

    private void onFrontierAppearancePressed() {
        new FrontierAppearanceDialog().display();
    }

    private void onDefaultPathStylePressed() {
        new PathStyleDialog(ClientConfig.getDefaultPathStyle(), newPathStyle -> {
            FrontierData.PathStyle currentStyle = ClientConfig.getDefaultPathStyle();
            if (!currentStyle.equals(newPathStyle)) {
                ClientConfig.setDefaultPathStyle(newPathStyle);
                ClientGlobalEvents.postUpdatedConfigEvent();
            }
        }).display();
    }

    private void onForcedVisibilityPressed() {
        new VisibilityDialog(createForcedVisibility(), createForcedVisibilityMask(), this::setForcedVisibility).display();
    }

    private void onEditHUDPressed() {
        MapFrontiersClient.setLastSettingsTab(tabSelected);
        new HUDSettings().display();
    }

    private void onHudEnabledChanged(boolean enabled) {
        ClientConfig.HUD_ENABLED.set(enabled);
        updateButtonsVisibility();
    }

    private void onGroupElementClicked(GroupElement element) {
        groupClicked(element);
    }

    private void onGroupDeletePressed(ScrollElement element) {
        if (groups.getSelectedElement() != null) {
            groupClicked((GroupElement) element);
        }

        if (ClientConfig.ASK_CONFIRMATION_GROUP_DELETE.get()) {
            showDeleteGroupConfirmation(element);
        } else {
            deleteGroup(element);
        }
    }

    private void showDeleteGroupConfirmation(ScrollElement element) {
        new DeleteConfirmationDialog(
                "mapfrontiers.delete_group_dialog",
                response -> {
                    if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                        ClientConfig.ASK_CONFIRMATION_GROUP_DELETE.set(false);
                        buttonAskConfirmationGroupDelete.setSelected(1);
                        ClientGlobalEvents.postUpdatedConfigEvent();
                    }
                    deleteGroup(element);
                }
        ).display();
    }

    private void deleteGroup(ScrollElement element) {
        groups.removeElement(element);
        settings.removeCustomGroup(((GroupElement) element).getGroup());
        sendChangesToServer();
    }

    private void onUserDeletePressed(ScrollElement element) {
        GroupElement selectedGroup = (GroupElement) groups.getSelectedElement();
        if (selectedGroup == null) {
            return;
        }

        SettingsGroup group = selectedGroup.getGroup();
        if (ClientConfig.ASK_CONFIRMATION_USER_DELETE.get()) {
            showDeleteUserConfirmation(group, element);
        } else {
            deleteUser(group, element);
        }
    }

    private void showDeleteUserConfirmation(SettingsGroup group, ScrollElement element) {
        new DeleteConfirmationDialog(
                "mapfrontiers.delete_user_dialog",
                response -> {
                    if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                        ClientConfig.ASK_CONFIRMATION_USER_DELETE.set(false);
                        buttonAskConfirmationUserDelete.setSelected(1);
                        ClientGlobalEvents.postUpdatedConfigEvent();
                    }
                    deleteUser(group, element);
                }
        ).display();
    }

    private void deleteUser(SettingsGroup group, ScrollElement element) {
        users.removeElement(element);
        group.removeUser(((UserElement) element).getUser());
        sendChangesToServer();
    }

    private void onGroupNameLostFocus(String value) {
        if (tabSelected == Tab.Groups) {
            GroupElement groupElement = (GroupElement) groups.getSelectedElement();
            if (groupElement != null) {
                groupElement.getGroup().setName(value);
                sendChangesToServer();
            }
        }
    }

    private void onNewGroupPressed() {
        newGroupPressed();
    }

    private void onNewUserPressed() {
        newUserPressed();
    }

    private void refreshViewState() {
        restoreInitialTabSelection();
        updateButtonsVisibility();
    }

    private void restoreInitialTabSelection() {
        tabbedBox.setTabSelected(tabSelected.ordinal());
    }

    private void requestInitialDataIfNeeded() {
        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketRequestFrontierSettings());
        }
    }

    @Override
    public void repositionElements() {
        tabbedBox.setSize(actualWidth - TABBED_BOX_MARGIN_X, actualHeight - TABBED_BOX_MARGIN_Y);
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
    public boolean keyPressed(KeyEvent event) {
        if (event.input() == GLFW.GLFW_KEY_E && !(getFocused() instanceof EditBox)) {
            onClose();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (GuiEventListener w : children()) {
            if (w instanceof ScrollBox) {
                ((ScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(event);
    }

    private FrontierData.VisibilityData createForcedVisibility() {
        FrontierData.VisibilityData visibilityData = new FrontierData.VisibilityData();
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Frontier, ClientConfig.FRONTIER_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.AnnounceInChat, ClientConfig.ANNOUNCE_IN_CHAT.get() == ClientConfig.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.AnnounceInTitle, ClientConfig.ANNOUNCE_IN_TITLE.get() == ClientConfig.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Fullscreen, ClientConfig.FULLSCREEN_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenName, ClientConfig.FULLSCREEN_NAME_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenOwner, ClientConfig.FULLSCREEN_OWNER_VISIBILITY.get() == ClientConfig.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenBanner, ClientConfig.FULLSCREEN_BANNER_VISIBILITY.get() == ClientConfig.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenDay, ClientConfig.FULLSCREEN_DAY_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenNight, ClientConfig.FULLSCREEN_NIGHT_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenUnderground, ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenTopo, ClientConfig.FULLSCREEN_TOPO_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenBiome, ClientConfig.FULLSCREEN_BIOME_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Minimap, ClientConfig.MINIMAP_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapName, ClientConfig.MINIMAP_NAME_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapOwner, ClientConfig.MINIMAP_OWNER_VISIBILITY.get() == ClientConfig.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapBanner, ClientConfig.MINIMAP_BANNER_VISIBILITY.get() == ClientConfig.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapDay, ClientConfig.MINIMAP_DAY_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapNight, ClientConfig.MINIMAP_NIGHT_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapUnderground, ClientConfig.MINIMAP_UNDERGROUND_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapTopo, ClientConfig.MINIMAP_TOPO_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapBiome, ClientConfig.MINIMAP_BIOME_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Webmap, ClientConfig.WEBMAP_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapName, ClientConfig.WEBMAP_NAME_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapOwner, ClientConfig.WEBMAP_OWNER_VISIBILITY.get() == ClientConfig.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapBanner, ClientConfig.WEBMAP_BANNER_VISIBILITY.get() == ClientConfig.Visibility.Always);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapDay, ClientConfig.WEBMAP_DAY_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapNight, ClientConfig.WEBMAP_NIGHT_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapUnderground, ClientConfig.WEBMAP_UNDERGROUND_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapTopo, ClientConfig.WEBMAP_TOPO_VISIBILITY.get() != ClientConfig.Visibility.Never);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapBiome, ClientConfig.WEBMAP_BIOME_VISIBILITY.get() != ClientConfig.Visibility.Never);
        return visibilityData;
    }

    private FrontierData.VisibilityData createForcedVisibilityMask() {
        FrontierData.VisibilityData visibilityData = new FrontierData.VisibilityData();
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Frontier, ClientConfig.FRONTIER_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.AnnounceInChat, ClientConfig.ANNOUNCE_IN_CHAT.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.AnnounceInTitle, ClientConfig.ANNOUNCE_IN_TITLE.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Fullscreen, ClientConfig.FULLSCREEN_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenName, ClientConfig.FULLSCREEN_NAME_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenOwner, ClientConfig.FULLSCREEN_OWNER_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenBanner, ClientConfig.FULLSCREEN_BANNER_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenDay, ClientConfig.FULLSCREEN_DAY_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenNight, ClientConfig.FULLSCREEN_NIGHT_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenUnderground, ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenTopo, ClientConfig.FULLSCREEN_TOPO_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.FullscreenBiome, ClientConfig.FULLSCREEN_BIOME_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Minimap, ClientConfig.MINIMAP_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapName, ClientConfig.MINIMAP_NAME_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapOwner, ClientConfig.MINIMAP_OWNER_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapBanner, ClientConfig.MINIMAP_BANNER_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapDay, ClientConfig.MINIMAP_DAY_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapNight, ClientConfig.MINIMAP_NIGHT_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapUnderground, ClientConfig.MINIMAP_UNDERGROUND_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapTopo, ClientConfig.MINIMAP_TOPO_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.MinimapBiome, ClientConfig.MINIMAP_BIOME_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.Webmap, ClientConfig.WEBMAP_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapName, ClientConfig.WEBMAP_NAME_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapOwner, ClientConfig.WEBMAP_OWNER_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapBanner, ClientConfig.WEBMAP_BANNER_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapDay, ClientConfig.WEBMAP_DAY_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapNight, ClientConfig.WEBMAP_NIGHT_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapUnderground, ClientConfig.WEBMAP_UNDERGROUND_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapTopo, ClientConfig.WEBMAP_TOPO_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        visibilityData.setValue(FrontierData.VisibilityData.Visibility.WebmapBiome, ClientConfig.WEBMAP_BIOME_VISIBILITY.get() != ClientConfig.Visibility.Custom);
        return visibilityData;
    }

    private void setForcedVisibility(FrontierData.VisibilityData visibilityData, FrontierData.VisibilityData visibilityDataMask) {
        ClientConfig.FRONTIER_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.Frontier));
        ClientConfig.ANNOUNCE_IN_CHAT.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.AnnounceInChat));
        ClientConfig.ANNOUNCE_IN_TITLE.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.AnnounceInTitle));
        ClientConfig.FULLSCREEN_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.Fullscreen));
        ClientConfig.FULLSCREEN_NAME_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenName));
        ClientConfig.FULLSCREEN_OWNER_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenOwner));
        ClientConfig.FULLSCREEN_BANNER_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenBanner));
        ClientConfig.FULLSCREEN_DAY_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenDay));
        ClientConfig.FULLSCREEN_NIGHT_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenNight));
        ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenUnderground));
        ClientConfig.FULLSCREEN_TOPO_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenTopo));
        ClientConfig.FULLSCREEN_BIOME_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.FullscreenBiome));
        ClientConfig.MINIMAP_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.Minimap));
        ClientConfig.MINIMAP_NAME_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapName));
        ClientConfig.MINIMAP_OWNER_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapOwner));
        ClientConfig.MINIMAP_BANNER_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapBanner));
        ClientConfig.MINIMAP_DAY_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapDay));
        ClientConfig.MINIMAP_NIGHT_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapNight));
        ClientConfig.MINIMAP_UNDERGROUND_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapUnderground));
        ClientConfig.MINIMAP_TOPO_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapTopo));
        ClientConfig.MINIMAP_BIOME_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.MinimapBiome));
        ClientConfig.WEBMAP_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.Webmap));
        ClientConfig.WEBMAP_NAME_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapName));
        ClientConfig.WEBMAP_OWNER_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapOwner));
        ClientConfig.WEBMAP_BANNER_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapBanner));
        ClientConfig.WEBMAP_DAY_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapDay));
        ClientConfig.WEBMAP_NIGHT_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapNight));
        ClientConfig.WEBMAP_UNDERGROUND_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapUnderground));
        ClientConfig.WEBMAP_TOPO_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapTopo));
        ClientConfig.WEBMAP_BIOME_VISIBILITY.set(getVisibilityValue(visibilityData, visibilityDataMask, FrontierData.VisibilityData.Visibility.WebmapBiome));
    }

    private ClientConfig.Visibility getVisibilityValue(FrontierData.VisibilityData visibilityData, FrontierData.VisibilityData visibilityDataMask, FrontierData.VisibilityData.Visibility visibility) {
        if (visibilityDataMask.getValue(visibility)) {
            return visibilityData.getValue(visibility) ? ClientConfig.Visibility.Always : ClientConfig.Visibility.Never;
        }
        return ClientConfig.Visibility.Custom;
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
        ClientGlobalEvents.postUpdatedConfigEvent();
        MapFrontiersClient.setLastSettingsTab(tabSelected);
        if (subscribeToSettingsProfileEvents) {
            MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        }
        ClientGlobalEvents.unsubscribeAllEvents(this);
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
        buttonEditHUD.visible = tabSelected == Tab.General && ClientConfig.HUD_ENABLED.get() && minecraft.player != null && MapFrontiersClient.isJourneyMapPluginAvailable();
        buttonFrontierAppearance.visible = tabSelected == Tab.General && minecraft.player != null && MapFrontiersClient.isJourneyMapPluginAvailable();
        buttonDefaultPathStyle.visible = tabSelected == Tab.General && minecraft.player != null && MapFrontiersClient.isJourneyMapPluginAvailable();
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

    private static Tooltip tooltip(ConfigEntry<?, ?> entry) {
        Component component = entry.tooltip();
        return component == null ? null : Tooltip.create(component);
    }
}
