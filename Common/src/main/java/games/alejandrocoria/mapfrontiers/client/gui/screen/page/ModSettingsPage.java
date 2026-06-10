package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.FrontierDisplayVisibility;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
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
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxUser;
import games.alejandrocoria.mapfrontiers.client.gui.screen.HUDSettingsScreen;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.CollectionAppearanceDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.CollectionVisibilityDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.ConfirmationSettingsDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.FrontierAppearanceDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.FrontierBehaviorDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.FrontierVisibilityDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.NewCollectionDefaultsDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.NewFrontierDefaultsDialog;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.config.BooleanConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRequestFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings.Action;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityField;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityMask;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibility;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityMask;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ModSettingsPage extends PageScreen {
    public enum Tab {
        Credits, General, Groups, Actions
    }

    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_settings");
    private static final Component TAB_CREDITS_LABEL = Component.translatable("mapfrontiers.credits");
    private static final Component TAB_GENERAL_LABEL = Component.translatable("mapfrontiers.general");
    private static final Component TAB_GROUPS_LABEL = Component.translatable("mapfrontiers.groups");
    private static final Component TAB_ACTIONS_LABEL = Component.translatable("mapfrontiers.actions");
    private static final Component CREATED_BY_LABEL = Component.translatable("mapfrontiers.credits_created_by");
    private static final Component MANY_THANKS_LABEL = Component.translatable("mapfrontiers.credits_many_thanks", Services.PLATFORM.getPlatformName());
    private static final Component PROJECT_LABEL = Component.translatable("mapfrontiers.credits_project");
    private static final Component PATREON_LABEL = Component.translatable("mapfrontiers.credits_patreon");
    private static final Component WEB_LINK_LABEL = Component.literal("alejandrocoria.games");
    private static final String WEB_URL = "https://alejandrocoria.games";
    private static final Component CURSE_FORGE_LINK_LABEL = Component.literal("curseforge.com/minecraft/mc-mods/mapfrontiers");
    private static final String CURSE_FORGE_URL = "https://www.curseforge.com/minecraft/mc-mods/mapfrontiers";
    private static final Component MODRINTH_LINK_LABEL = Component.literal("modrinth.com/mod/mapfrontiers");
    private static final String MODRINTH_URL = "https://modrinth.com/mod/mapfrontiers";
    private static final Component PATREON_LINK_LABEL = Component.literal("patreon.com/alejandrocoria");
    private static final String PATREON_URL = "https://patreon.com/alejandrocoria";
    private static final Component CREDITS_TRANSLATION_LABEL = Component.translatable("mapfrontiers.credits_translation");
    private static final Component VERSION_LABEL = Component.literal(Services.PLATFORM.getModVersion());
    private static final String KEY_HINT_KEY = "mapfrontiers.key.open_settings.hint";
    private static final Component FRONTIERS_LABEL = Component.translatable("mapfrontiers.frontiers");
    private static final Component COLLECTIONS_LABEL = Component.translatable("mapfrontiers.collections");
    private static final Component BEHAVIOR_LABEL = Component.translatable("mapfrontiers.behavior");
    private static final Component APPEARANCE_LABEL = Component.translatable("mapfrontiers.appearance");
    private static final Component DEFAULTS_LABEL = Component.translatable("mapfrontiers.defaults");
    private static final Component FORCED_VISIBILITY_LABEL = Component.translatable("mapfrontiers.forced_visibility");
    private static final Component GUI_LABEL = Component.translatable("mapfrontiers.gui");
    private static final Component CONFIRMATION_DIALOGS_LABEL = Component.translatable("mapfrontiers.confirmation_dialogs");
    private static final Component HUD_LABEL = Component.translatable("mapfrontiers.hud");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final Component EDIT_HUD_LABEL = Component.translatable("mapfrontiers.edit_hud");
    private static final Component GROUP_OPS_DESC_LABEL = Component.translatable("mapfrontiers.group_ops_desc");
    private static final Component GROUP_OWNERS_DESC_LABEL = Component.translatable("mapfrontiers.group_owners_desc");
    private static final Component GROUP_EVERYONE_DESC_LABEL = Component.translatable("mapfrontiers.group_everyone_desc");
    private static final Component CREATE_GLOBAL_FRONTIER_LABEL = Component.translatable("mapfrontiers.create_global_frontier");
    private static final Component DELETE_GLOBAL_FRONTIER_LABEL = Component.translatable("mapfrontiers.delete_global_frontier");
    private static final Component UPDATE_GLOBAL_FRONTIER_LABEL = Component.translatable("mapfrontiers.update_global_frontier");
    private static final Component UPDATE_SETTINGS_LABEL = Component.translatable("mapfrontiers.update_settings");
    private static final Component SHARE_PERSONAL_FRONTIER_LABEL = Component.translatable("mapfrontiers.share_personal_frontier");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final Tooltip ADD_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.add.tooltip"));
    private static final int BUTTON_HORIZONTAL_PADDING = 8;
    private static final int WIDE_LINK_EXTRA_WIDTH = 200;
    private static final int GROUPS_SCROLL_WIDTH = 160;
    private static final int USERS_SCROLL_WIDTH = 258;
    private static final int ACTIONS_SCROLL_WIDTH = 430;
    private static final int GROUP_NAME_WIDTH = 140;
    private static final int GROUPS_MIN_ROWS = 19;
    private static final int USERS_MIN_ROWS = 16;
    private static final int ACTIONS_MIN_ROWS = 19;
    private static final int GROUPS_ELEMENT_HEIGHT = 15;
    private static final int USERS_ELEMENT_HEIGHT = 15;
    private static final int ACTIONS_ELEMENT_HEIGHT = 15;
    private static final int GROUPS_VERTICAL_MARGIN = 120;
    private static final int USERS_VERTICAL_MARGIN = 159;
    private static final int ACTIONS_VERTICAL_MARGIN = 138;

    private final boolean showKeyHint;

    private FrontierSettings settings;
    private TabbedBox tabbedBox;
    private SimpleButton buttonFrontierAppearance;
    private SimpleButton buttonCollectionAppearance;
    private SimpleButton buttonConfirmationDialogs;
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

    public ModSettingsPage(boolean showKeyHint) {
        super(TITLE_LABEL);
        this.showKeyHint = showKeyHint;
        subscribeToSettingsProfileEvents = MapFrontiersClient.isJourneyMapPluginAvailable();

        if (subscribeToSettingsProfileEvents) {
            MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
                if ((profile.updateSettings == SettingsProfile.State.Enabled) == canEditGroups) {
                    return;
                }

                onSettingsProfileUpdated();
            });
        }
    }

    @Override
    protected void initScreen() {
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
        TabbedBox tabs = content.addChild(new TabbedBox(font, this::onTabChanged));
        tabs.addTab(TAB_CREDITS_LABEL, true);
        tabs.addTab(TAB_GENERAL_LABEL, true);
        tabs.addTab(TAB_GROUPS_LABEL, canEditGroups);
        tabs.addTab(TAB_ACTIONS_LABEL, canEditGroups);
        return tabs;
    }

    private void onTabChanged(int tab) {
        tabSelected = Tab.values()[tab];

        if (tabSelected == Tab.Actions) {
            updateGroupsActions();
        }

        refreshControlState();
    }

    private void onSettingsProfileUpdated() {
        resolveInitialState();
        refreshPermissionsState();
    }

    private void buildCreditsTab() {
        LinearLayout creditsLayout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        creditsLayout.defaultCellSetting().alignHorizontallyCenter();
        tabbedBox.addChild(creditsLayout, Tab.Credits.ordinal());

        creditsLayout.addChild(new StringWidget(CREATED_BY_LABEL, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        creditsLayout.addChild(createWideExternalLinkButton(WEB_LINK_LABEL, WEB_URL));
        creditsLayout.addChild(SpacerElement.height(16));

        creditsLayout.addChild(new StringWidget(MANY_THANKS_LABEL, font).setColor(ColorConstants.TEXT_MEDIUM));
        creditsLayout.addChild(SpacerElement.height(16));

        creditsLayout.addChild(new StringWidget(PROJECT_LABEL, font).setColor(ColorConstants.TEXT_MEDIUM));
        creditsLayout.addChild(createExternalLinkButton(CURSE_FORGE_LINK_LABEL, CURSE_FORGE_URL));
        creditsLayout.addChild(createExternalLinkButton(MODRINTH_LINK_LABEL, MODRINTH_URL));
        creditsLayout.addChild(SpacerElement.height(16));

        creditsLayout.addChild(new StringWidget(PATREON_LABEL, font).setColor(ColorConstants.TEXT_MEDIUM));
        creditsLayout.addChild(createExternalLinkButton(PATREON_LINK_LABEL, PATREON_URL));
    }

    private void buildGeneralTab() {
        LinearLayout generalLayout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        generalLayout.defaultCellSetting().alignHorizontallyCenter();
        tabbedBox.addChild(generalLayout, Tab.General.ordinal());

        generalLayout.addChild(new StringWidget(FRONTIERS_LABEL, font).setColor(ColorConstants.TEXT_HIGHLIGHT));

        GridLayout settingsGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        settingsGrid.defaultCellSetting().alignHorizontallyLeft().alignVerticallyMiddle();
        generalLayout.addChild(settingsGrid);

        int row = 0;
        row = buildFrontiersSection(settingsGrid, row);
        row = buildCollectionsSection(settingsGrid, row);
        row = buildGuiSection(settingsGrid, row);
        buildHudSection(generalLayout, settingsGrid, row);
    }

    private int buildFrontiersSection(GridLayout settingsGrid, int row) {
        int buttonWidth = ScreenHelper.getPaddedMaxTextWidth(font, LayoutConstants.PAGE_BUTTON_WIDTH,
                BUTTON_HORIZONTAL_PADDING, BEHAVIOR_LABEL, APPEARANCE_LABEL, DEFAULTS_LABEL, FORCED_VISIBILITY_LABEL);

        settingsGrid.addChild(new SimpleButton(font, buttonWidth, BEHAVIOR_LABEL,
                b -> onFrontierBehaviorPressed()), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        buttonFrontierAppearance = new SimpleButton(font, buttonWidth, APPEARANCE_LABEL,
                b -> onFrontierAppearancePressed());
        settingsGrid.addChild(buttonFrontierAppearance, row++, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());

        settingsGrid.addChild(new SimpleButton(font, buttonWidth, DEFAULTS_LABEL,
                b -> onNewFrontierDefaultsPressed()), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        settingsGrid.addChild(new SimpleButton(font, buttonWidth, FORCED_VISIBILITY_LABEL,
                b -> onForcedFrontierVisibilityPressed()), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        return row;
    }

    private int buildCollectionsSection(GridLayout settingsGrid, int row) {
        settingsGrid.addChild(SpacerElement.height(LayoutConstants.SPACING_SMALL), row++, 0);
        settingsGrid.addChild(new StringWidget(COLLECTIONS_LABEL, font).setColor(ColorConstants.TEXT_HIGHLIGHT), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        int buttonWidth = ScreenHelper.getPaddedMaxTextWidth(font, LayoutConstants.PAGE_BUTTON_WIDTH,
                BUTTON_HORIZONTAL_PADDING, APPEARANCE_LABEL, DEFAULTS_LABEL, FORCED_VISIBILITY_LABEL);

        buttonCollectionAppearance = new SimpleButton(font, buttonWidth, APPEARANCE_LABEL,
                b -> onCollectionAppearancePressed());
        settingsGrid.addChild(buttonCollectionAppearance, row++, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());

        settingsGrid.addChild(new SimpleButton(font, buttonWidth, DEFAULTS_LABEL,
                b -> onNewCollectionDefaultsPressed()), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        settingsGrid.addChild(new SimpleButton(font, buttonWidth, FORCED_VISIBILITY_LABEL,
                b -> onForcedCollectionVisibilityPressed()), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        return row;
    }

    private int buildGuiSection(GridLayout settingsGrid, int row) {
        settingsGrid.addChild(SpacerElement.height(LayoutConstants.SPACING_SMALL), row++, 0);
        settingsGrid.addChild(new StringWidget(GUI_LABEL, font).setColor(ColorConstants.TEXT_HIGHLIGHT), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        row = addOptionSettingRow(settingsGrid, row, ClientConfig.FULLSCREEN_BUTTONS);
        buttonConfirmationDialogs = new SimpleButton(font, LayoutConstants.PAGE_BUTTON_WIDTH, CONFIRMATION_DIALOGS_LABEL,
                b -> onConfirmationDialogsPressed());
        settingsGrid.addChild(buttonConfirmationDialogs, row, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyCenter());
        return row + 1;
    }

    private void buildHudSection(LinearLayout generalLayout, GridLayout settingsGrid, int row) {
        settingsGrid.addChild(SpacerElement.height(LayoutConstants.SPACING_SMALL), row++, 0);
        settingsGrid.addChild(new StringWidget(HUD_LABEL, font).setColor(ColorConstants.TEXT_HIGHLIGHT), row++, 0, 1, 2,
                LayoutSettings.defaults().alignHorizontallyCenter());

        addOptionSettingRow(settingsGrid, row, ClientConfig.HUD_ENABLED, createOnOffOptionButton(ClientConfig.HUD_ENABLED,
                this::onHudEnabledChanged));

        buttonEditHUD = generalLayout.addChild(new SimpleButton(font, LayoutConstants.PAGE_BUTTON_WIDTH, EDIT_HUD_LABEL,
                b -> onEditHUDPressed()));
    }

    private void buildGroupsTab() {
        LinearLayout groupsLayout = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        groupsLayout.defaultCellSetting().alignHorizontallyLeft();
        tabbedBox.addChild(groupsLayout, Tab.Groups.ordinal());

        LinearLayout groupsColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        groupsColumn.defaultCellSetting().alignHorizontallyCenter();
        groupsLayout.addChild(groupsColumn);
        buildGroupsList(groupsColumn);
        buildNewGroupControls(groupsColumn);

        LinearLayout usersColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        usersColumn.defaultCellSetting().alignHorizontallyLeft();
        groupsLayout.addChild(usersColumn);
        buildUsersPanel(usersColumn);
        buildNewUserControls(usersColumn);
    }

    private void buildGroupsList(LinearLayout groupsColumn) {
        groups = groupsColumn.addChild(new ScrollBox(ScrollBox.rowsToHeight(GROUPS_MIN_ROWS, GROUPS_ELEMENT_HEIGHT),
                GROUPS_SCROLL_WIDTH, GROUPS_ELEMENT_HEIGHT));
        groups.setElementClickedCallback(element -> {
            onGroupElementClicked((GroupElement) element);
            refreshControlState();
        });
        groups.setElementDeletePressedCallback(this::onGroupDeletePressed);
    }

    private void buildNewGroupControls(LinearLayout groupsColumn) {
        LinearLayout newGroupLayout = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        newGroupLayout.defaultCellSetting().alignVerticallyMiddle();
        groupsColumn.addChild(newGroupLayout);

        textNewGroupName = newGroupLayout.addChild(new TextBox(font, GROUP_NAME_WIDTH, I18n.get("mapfrontiers.new_group_name")));
        textNewGroupName.setMaxLength(22);
        textNewGroupName.setSubmitCallback(value -> onNewGroupPressed());

        buttonNewGroup = newGroupLayout.addChild(new IconButton(IconButton.Type.Add, b -> onNewGroupPressed()));
        buttonNewGroup.setTooltip(ADD_TOOLTIP);
    }

    private void buildUsersPanel(LinearLayout usersColumn) {
        textGroupName = usersColumn.addChild(new TextBox(font, GROUP_NAME_WIDTH));
        textGroupName.setMaxLength(22);
        textGroupName.setLostFocusCallback(this::onGroupNameLostFocus);

        labelGroupDesc = usersColumn.addChild(new MultiLineTextWidget(GROUP_OPS_DESC_LABEL.copy().withColor(ColorConstants.TEXT), font));

        users = usersColumn.addChild(new ScrollBox(ScrollBox.rowsToHeight(USERS_MIN_ROWS, USERS_ELEMENT_HEIGHT),
                USERS_SCROLL_WIDTH, USERS_ELEMENT_HEIGHT));
        users.setElementDeletePressedCallback(this::onUserDeletePressed);
    }

    private void buildNewUserControls(LinearLayout usersColumn) {
        LinearLayout newUserLayout = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        newUserLayout.defaultCellSetting().alignVerticallyMiddle();
        usersColumn.addChild(newUserLayout);

        textNewUser = newUserLayout.addChild(new TextBoxUser(minecraft, font, LayoutConstants.USER_TEXTBOX_WIDTH, I18n.get("mapfrontiers.new_user")));
        textNewUser.setMaxLength(38);
        textNewUser.setSubmitCallback(value -> onNewUserPressed());

        buttonNewUser = newUserLayout.addChild(new IconButton(IconButton.Type.Add, b -> onNewUserPressed()));
        buttonNewUser.setTooltip(ADD_TOOLTIP);
    }

    private void buildActionsTab() {
        LinearLayout actionsLayout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        actionsLayout.defaultCellSetting().alignHorizontallyCenter();
        tabbedBox.addChild(actionsLayout, Tab.Actions.ordinal());

        LinearLayout actionsHeader = LinearLayout.horizontal();
        actionsLayout.addChild(actionsHeader);

        labelCreateFrontier = actionsHeader.addChild(createActionsHeaderLabel(CREATE_GLOBAL_FRONTIER_LABEL));
        labelDeleteFrontier = actionsHeader.addChild(createActionsHeaderLabel(DELETE_GLOBAL_FRONTIER_LABEL));
        labelUpdateFrontier = actionsHeader.addChild(createActionsHeaderLabel(UPDATE_GLOBAL_FRONTIER_LABEL));
        labelUpdateSettings = actionsHeader.addChild(createActionsHeaderLabel(UPDATE_SETTINGS_LABEL));
        labelSharePersonalFrontier = actionsHeader.addChild(createActionsHeaderLabel(SHARE_PERSONAL_FRONTIER_LABEL));

        groupsActions = actionsLayout.addChild(new ScrollBox(ScrollBox.rowsToHeight(ACTIONS_MIN_ROWS, ACTIONS_ELEMENT_HEIGHT),
                ACTIONS_SCROLL_WIDTH, ACTIONS_ELEMENT_HEIGHT));
        groupsActions.setHorizontalEdgeNavigation(ScrollBox.HorizontalEdgeNavigation.WRAP_WITHIN_ROW);
    }

    private void buildBottomButtons() {
        addBottomButton(new SimpleButton(font, LayoutConstants.PAGE_BUTTON_WIDTH, DONE_LABEL, b -> onClose()));
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

    private OptionButton createOnOffOptionButton(BooleanConfigEntry entry) {
        return createOnOffOptionButton(entry, entry::set);
    }

    private OptionButton createOnOffOptionButton(BooleanConfigEntry entry, Consumer<Boolean> consumer) {
        OptionButton button = new OptionButton(font, LayoutConstants.SETTING_CONTROL_WIDTH,
                b -> consumer.accept(b.getSelected() == 0));
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(entry.get() ? 0 : 1);
        return button;
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
        label.setTooltip(ScreenHelper.tooltip(entry));
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
        if (!areJourneyMapPreviewActionsAvailable()) {
            return;
        }
        new FrontierAppearanceDialog().display();
    }

    private void onFrontierBehaviorPressed() {
        new FrontierBehaviorDialog().display();
    }

    private void onCollectionAppearancePressed() {
        if (!areJourneyMapPreviewActionsAvailable()) {
            return;
        }
        new CollectionAppearanceDialog().display();
    }

    private void onNewFrontierDefaultsPressed() {
        new NewFrontierDefaultsDialog().display();
    }

    private void onNewCollectionDefaultsPressed() {
        new NewCollectionDefaultsDialog().display();
    }

    private void onForcedFrontierVisibilityPressed() {
        new FrontierVisibilityDialog(createForcedFrontierVisibility(), createForcedFrontierVisibilityMask(), this::setForcedFrontierVisibility).display();
    }

    private void onForcedCollectionVisibilityPressed() {
        new CollectionVisibilityDialog(createForcedCollectionVisibility(), createForcedCollectionVisibilityMask(),
                this::setForcedCollectionVisibility).display();
    }

    private void onEditHUDPressed() {
        if (!areJourneyMapPreviewActionsAvailable()) {
            return;
        }
        MapFrontiersClient.setLastSettingsTab(tabSelected);
        new HUDSettingsScreen().display();
    }

    private void onConfirmationDialogsPressed() {
        new ConfirmationSettingsDialog().display();
    }

    private void onHudEnabledChanged(boolean enabled) {
        ClientConfig.HUD_ENABLED.set(enabled);
        refreshControlState();
    }

    private void onGroupElementClicked(GroupElement element) {
        groupClicked(element);
    }

    private void onGroupDeletePressed(ScrollElement element) {
        if (((GroupElement) element).getGroup().isSpecial()) {
            return;
        }

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
                        ClientGlobalEvents.postUpdatedConfigEvent();
                    }
                    deleteGroup(element);
                }
        ).display();
    }

    private void deleteGroup(ScrollElement element) {
        if (((GroupElement) element).getGroup().isSpecial()) {
            return;
        }

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
        refreshControlState();
    }

    private void refreshPermissionsState() {
        if (tabbedBox == null) {
            return;
        }

        tabbedBox.setTabEnabled(Tab.Groups.ordinal(), canEditGroups);
        tabbedBox.setTabEnabled(Tab.Actions.ordinal(), canEditGroups);

        if (!tabbedBox.isTabEnabled(tabSelected.ordinal())) {
            tabSelected = Tab.Credits;
        }

        tabbedBox.setTabSelected(tabSelected.ordinal());
        refreshControlState();

        if (canEditGroups) {
            if (settings != null) {
                updateGroupsActions();
                if (groups.getSelectedElement() != null) {
                    groupClicked((GroupElement) groups.getSelectedElement());
                }
            } else {
                requestInitialDataIfNeeded();
            }
        }
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
    protected void resetContentToMinimumSize() {
        tabbedBox.setSizeToContent();
        groups.setViewportHeight(ScrollBox.rowsToHeight(GROUPS_MIN_ROWS, GROUPS_ELEMENT_HEIGHT));
        users.setViewportHeight(ScrollBox.rowsToHeight(USERS_MIN_ROWS, USERS_ELEMENT_HEIGHT));
        groupsActions.setViewportHeight(ScrollBox.rowsToHeight(ACTIONS_MIN_ROWS, ACTIONS_ELEMENT_HEIGHT));
    }

    @Override
    protected void resizeContentToAvailableSpace() {
        tabbedBox.setSize(availableWidth(LayoutConstants.PAGE_MARGIN * 2), availableHeight(LayoutConstants.PAGE_MARGIN * 2));
        groups.setViewportHeight(Math.max(ScrollBox.rowsToHeight(GROUPS_MIN_ROWS, GROUPS_ELEMENT_HEIGHT),
                availableHeight(GROUPS_VERTICAL_MARGIN)));
        users.setViewportHeight(Math.max(ScrollBox.rowsToHeight(USERS_MIN_ROWS, USERS_ELEMENT_HEIGHT),
                availableHeight(USERS_VERTICAL_MARGIN)));
        groupsActions.setViewportHeight(Math.max(ScrollBox.rowsToHeight(ACTIONS_MIN_ROWS, ACTIONS_ELEMENT_HEIGHT),
                availableHeight(ACTIONS_VERTICAL_MARGIN)));
    }

    @Override
    public void repositionElements() {
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
    protected void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        tabbedBox.renderBackground(graphics, mouseX, mouseY, partialTicks);

        if (tabSelected == Tab.Credits || tabSelected == Tab.General) {
            int y = tabbedBox.getY() + tabbedBox.getHeight() - 19;
            graphics.text(font, CREDITS_TRANSLATION_LABEL, tabbedBox.getX() + 10, y, ColorConstants.TEXT_HIGHLIGHT);
            graphics.text(font, VERSION_LABEL, tabbedBox.getX() + tabbedBox.getWidth() - font.width(VERSION_LABEL) - 10, y, ColorConstants.TEXT_HIGHLIGHT);
            if (showKeyHint) {
                Component key = MapFrontiersClient.getOpenSettingsKey();
                if (key != null) {
                    graphics.centeredText(font, Component.translatable(KEY_HINT_KEY, key), tabbedBox.getX() + tabbedBox.getWidth() / 2, y, ColorConstants.TEXT_HIGHLIGHT);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (MapFrontiersClient.matchesOpenSettingsKey(event) && !isTextFieldFocused()) {
            onClose();
            return true;
        }

        return super.keyPressed(event);
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

    private FrontierVisibilityData createForcedFrontierVisibility() {
        FrontierVisibilityData visibilityData = new FrontierVisibilityData();
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            visibilityData.set(visibility, getForcedFrontierVisibilityValue(visibility));
        }
        return visibilityData;
    }

    private FrontierVisibilityMask createForcedFrontierVisibilityMask() {
        FrontierVisibilityMask visibilityMask = new FrontierVisibilityMask();
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            visibilityMask.set(visibility, getForcedFrontierVisibilitySetting(visibility).get() != FrontierDisplayVisibility.Custom);
        }
        return visibilityMask;
    }

    private CollectionVisibilityData createForcedCollectionVisibility() {
        CollectionVisibilityData visibilityData = new CollectionVisibilityData();
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (field.isBoolean()) {
                visibilityData.setBoolean(field, getForcedCollectionBooleanValue(field));
            } else {
                visibilityData.setZoom(field, getForcedCollectionZoomValue(field));
            }
        }
        return visibilityData;
    }

    private CollectionVisibilityMask createForcedCollectionVisibilityMask() {
        CollectionVisibilityMask visibilityMask = new CollectionVisibilityMask();
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            visibilityMask.set(field, isForcedCollectionVisibilityField(field));
        }
        return visibilityMask;
    }

    private void setForcedFrontierVisibility(FrontierVisibilityData visibilityData, FrontierVisibilityMask visibilityDataMask) {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            setForcedFrontierVisibilityField(visibilityData, visibilityDataMask, visibility);
        }
    }

    private void setForcedCollectionVisibility(CollectionVisibilityData visibilityData, CollectionVisibilityMask visibilityMask) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            setForcedCollectionVisibilityField(visibilityData, visibilityMask, field);
        }
    }

    private FrontierDisplayVisibility getFrontierVisibilityValue(FrontierVisibilityData visibilityData, FrontierVisibilityMask visibilityDataMask,
                                                                 FrontierVisibility visibility) {
        if (visibilityDataMask.has(visibility)) {
            return visibilityData.get(visibility) ? FrontierDisplayVisibility.Always : FrontierDisplayVisibility.Never;
        }
        return FrontierDisplayVisibility.Custom;
    }

    private boolean getForcedFrontierVisibilityValue(FrontierDisplayVisibility visibility, boolean customValue) {
        return switch (visibility) {
            case Always -> true;
            case Never -> false;
            case Custom -> customValue;
        };
    }

    private FrontierDisplayVisibility getFrontierVisibilityValue(boolean value, boolean masked) {
        if (masked) {
            return value ? FrontierDisplayVisibility.Always : FrontierDisplayVisibility.Never;
        }
        return FrontierDisplayVisibility.Custom;
    }

    private boolean getForcedFrontierVisibilityValue(FrontierVisibility visibility) {
        return getForcedFrontierVisibilityValue(getForcedFrontierVisibilitySetting(visibility).get(), visibility.getDefaultValue());
    }

    private ConfigEntry<FrontierDisplayVisibility, ?> getForcedFrontierVisibilitySetting(FrontierVisibility visibility) {
        return switch (visibility) {
            case Frontier -> ClientConfig.FRONTIER_VISIBILITY;
            case AnnounceInChat -> ClientConfig.ANNOUNCE_IN_CHAT;
            case AnnounceInTitle -> ClientConfig.ANNOUNCE_IN_TITLE;
            case MentionCollection -> ClientConfig.MENTION_COLLECTION;
            case Fullscreen -> ClientConfig.FULLSCREEN_VISIBILITY;
            case FullscreenName -> ClientConfig.FULLSCREEN_NAME_VISIBILITY;
            case FullscreenCollection -> ClientConfig.FULLSCREEN_COLLECTION_VISIBILITY;
            case FullscreenOwner -> ClientConfig.FULLSCREEN_OWNER_VISIBILITY;
            case FullscreenBanner -> ClientConfig.FULLSCREEN_BANNER_VISIBILITY;
            case FullscreenDay -> ClientConfig.FULLSCREEN_DAY_VISIBILITY;
            case FullscreenNight -> ClientConfig.FULLSCREEN_NIGHT_VISIBILITY;
            case FullscreenUnderground -> ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY;
            case FullscreenTopo -> ClientConfig.FULLSCREEN_TOPO_VISIBILITY;
            case FullscreenBiome -> ClientConfig.FULLSCREEN_BIOME_VISIBILITY;
            case Minimap -> ClientConfig.MINIMAP_VISIBILITY;
            case MinimapName -> ClientConfig.MINIMAP_NAME_VISIBILITY;
            case MinimapCollection -> ClientConfig.MINIMAP_COLLECTION_VISIBILITY;
            case MinimapOwner -> ClientConfig.MINIMAP_OWNER_VISIBILITY;
            case MinimapBanner -> ClientConfig.MINIMAP_BANNER_VISIBILITY;
            case MinimapDay -> ClientConfig.MINIMAP_DAY_VISIBILITY;
            case MinimapNight -> ClientConfig.MINIMAP_NIGHT_VISIBILITY;
            case MinimapUnderground -> ClientConfig.MINIMAP_UNDERGROUND_VISIBILITY;
            case MinimapTopo -> ClientConfig.MINIMAP_TOPO_VISIBILITY;
            case MinimapBiome -> ClientConfig.MINIMAP_BIOME_VISIBILITY;
            case Webmap -> ClientConfig.WEBMAP_VISIBILITY;
            case WebmapName -> ClientConfig.WEBMAP_NAME_VISIBILITY;
            case WebmapCollection -> ClientConfig.WEBMAP_COLLECTION_VISIBILITY;
            case WebmapOwner -> ClientConfig.WEBMAP_OWNER_VISIBILITY;
            case WebmapBanner -> ClientConfig.WEBMAP_BANNER_VISIBILITY;
            case WebmapDay -> ClientConfig.WEBMAP_DAY_VISIBILITY;
            case WebmapNight -> ClientConfig.WEBMAP_NIGHT_VISIBILITY;
            case WebmapUnderground -> ClientConfig.WEBMAP_UNDERGROUND_VISIBILITY;
            case WebmapTopo -> ClientConfig.WEBMAP_TOPO_VISIBILITY;
            case WebmapBiome -> ClientConfig.WEBMAP_BIOME_VISIBILITY;
        };
    }

    private boolean getForcedCollectionBooleanValue(CollectionVisibilityField field) {
        return getForcedFrontierVisibilityValue(getForcedCollectionVisibilitySetting(field), field.getDefaultBooleanValue());
    }

    private int getForcedCollectionZoomValue(CollectionVisibilityField field) {
        return switch (field) {
            case FullscreenZoom -> ClientConfig.getNormalizedCollectionFullscreenZoom();
            case MinimapZoom -> ClientConfig.getNormalizedCollectionMinimapZoom();
            case WebmapZoom -> ClientConfig.getNormalizedCollectionWebmapZoom();
            default -> throw new IllegalArgumentException("Field " + field + " is not a zoom field");
        };
    }

    private boolean isForcedCollectionVisibilityField(CollectionVisibilityField field) {
        return switch (field) {
            case Visible,
                 FullscreenName,
                 FullscreenOwner,
                 FullscreenBanner,
                 MinimapName,
                 MinimapOwner,
                 MinimapBanner,
                 WebmapName,
                 WebmapOwner,
                 WebmapBanner -> getForcedCollectionVisibilitySetting(field) != FrontierDisplayVisibility.Custom;
            case FullscreenZoom -> ClientConfig.COLLECTION_FULLSCREEN_ZOOM_FORCED.get();
            case MinimapZoom -> ClientConfig.COLLECTION_MINIMAP_ZOOM_FORCED.get();
            case WebmapZoom -> ClientConfig.COLLECTION_WEBMAP_ZOOM_FORCED.get();
        };
    }

    private FrontierDisplayVisibility getForcedCollectionVisibilitySetting(CollectionVisibilityField field) {
        return switch (field) {
            case Visible -> ClientConfig.COLLECTION_VISIBILITY.get();
            case FullscreenName -> ClientConfig.COLLECTION_FULLSCREEN_NAME_VISIBILITY.get();
            case FullscreenOwner -> ClientConfig.COLLECTION_FULLSCREEN_OWNER_VISIBILITY.get();
            case FullscreenBanner -> ClientConfig.COLLECTION_FULLSCREEN_BANNER_VISIBILITY.get();
            case MinimapName -> ClientConfig.COLLECTION_MINIMAP_NAME_VISIBILITY.get();
            case MinimapOwner -> ClientConfig.COLLECTION_MINIMAP_OWNER_VISIBILITY.get();
            case MinimapBanner -> ClientConfig.COLLECTION_MINIMAP_BANNER_VISIBILITY.get();
            case WebmapName -> ClientConfig.COLLECTION_WEBMAP_NAME_VISIBILITY.get();
            case WebmapOwner -> ClientConfig.COLLECTION_WEBMAP_OWNER_VISIBILITY.get();
            case WebmapBanner -> ClientConfig.COLLECTION_WEBMAP_BANNER_VISIBILITY.get();
            default -> throw new IllegalArgumentException("Field " + field + " is not a boolean visibility field");
        };
    }

    private void setForcedCollectionVisibilityField(CollectionVisibilityData visibilityData, CollectionVisibilityMask visibilityMask,
                                                    CollectionVisibilityField field) {
        switch (field) {
            case Visible -> ClientConfig.COLLECTION_VISIBILITY.set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
            case FullscreenZoom -> {
                ClientConfig.COLLECTION_FULLSCREEN_ZOOM_FORCED.set(visibilityMask.has(field));
                ClientConfig.COLLECTION_FULLSCREEN_ZOOM.set(visibilityData.getZoom(field));
            }
            case MinimapZoom -> {
                ClientConfig.COLLECTION_MINIMAP_ZOOM_FORCED.set(visibilityMask.has(field));
                ClientConfig.COLLECTION_MINIMAP_ZOOM.set(visibilityData.getZoom(field));
            }
            case WebmapZoom -> {
                ClientConfig.COLLECTION_WEBMAP_ZOOM_FORCED.set(visibilityMask.has(field));
                ClientConfig.COLLECTION_WEBMAP_ZOOM.set(visibilityData.getZoom(field));
            }
            case FullscreenName -> ClientConfig.COLLECTION_FULLSCREEN_NAME_VISIBILITY
                    .set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
            case FullscreenOwner -> ClientConfig.COLLECTION_FULLSCREEN_OWNER_VISIBILITY
                    .set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
            case FullscreenBanner -> ClientConfig.COLLECTION_FULLSCREEN_BANNER_VISIBILITY
                    .set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
            case MinimapName -> ClientConfig.COLLECTION_MINIMAP_NAME_VISIBILITY
                    .set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
            case MinimapOwner -> ClientConfig.COLLECTION_MINIMAP_OWNER_VISIBILITY
                    .set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
            case MinimapBanner -> ClientConfig.COLLECTION_MINIMAP_BANNER_VISIBILITY
                    .set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
            case WebmapName -> ClientConfig.COLLECTION_WEBMAP_NAME_VISIBILITY
                    .set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
            case WebmapOwner -> ClientConfig.COLLECTION_WEBMAP_OWNER_VISIBILITY
                    .set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
            case WebmapBanner -> ClientConfig.COLLECTION_WEBMAP_BANNER_VISIBILITY
                    .set(getFrontierVisibilityValue(visibilityData.getBoolean(field), visibilityMask.has(field)));
        }
    }

    private void setForcedFrontierVisibilityField(FrontierVisibilityData visibilityData, FrontierVisibilityMask visibilityMask,
                                                  FrontierVisibility visibility) {
        getForcedFrontierVisibilitySetting(visibility).set(getFrontierVisibilityValue(visibilityData, visibilityMask, visibility));
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
        refreshControlState();

        if (selectedElement != null) {
            groups.setSelectedElementIf(element -> ((GroupElement) element).getGroup().getName().equals(selectedElement.getGroup().getName()));
        }

        if (groups.getSelectedElement() == null) {
            if (selectedIndex >= 0 && selectedIndex < groups.getElements().size()) {
                groups.setSelectedElement(groups.getElements().get(selectedIndex));
            }
        }

        if (groups.getSelectedElement() == null && !groups.getElements().isEmpty()) {
            groups.setSelectedElement(groups.getElements().getFirst());
        }

        if (groups.getSelectedElement() != null) {
            groupClicked((GroupElement) groups.getSelectedElement());
        }
    }

    private void refreshControlState() {
        boolean previewActionsAvailable = areJourneyMapPreviewActionsAvailable();
        buttonEditHUD.active = ClientConfig.HUD_ENABLED.get() && previewActionsAvailable;
        buttonFrontierAppearance.active = previewActionsAvailable;
        buttonCollectionAppearance.active = previewActionsAvailable;

        boolean canAddUser = canAddNewUser();
        textNewUser.setEditable(canAddUser);
        buttonNewUser.active = canAddUser;
    }

    public void groupClicked(GroupElement element) {
        groups.setSelectedElement(element);
        SettingsGroup group = element.getGroup();
        textGroupName.setValue(group.getName());
        textGroupName.setEditable(!group.isSpecial());
        textGroupName.setBordered(!group.isSpecial());
        textGroupName.setFocused(false);

        if (group == settings.getOPsGroup()) {
            labelGroupDesc.setMessage(GROUP_OPS_DESC_LABEL);
        } else if (group == settings.getOwnersGroup()) {
            labelGroupDesc.setMessage(GROUP_OWNERS_DESC_LABEL);
        } else if (group == settings.getEveryoneGroup()) {
            labelGroupDesc.setMessage(GROUP_EVERYONE_DESC_LABEL);
        } else {
            labelGroupDesc.setMessage(Component.empty());
        }
        repositionElements();

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

        refreshControlState();
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

    private boolean areJourneyMapPreviewActionsAvailable() {
        return minecraft.player != null && MapFrontiersClient.isJourneyMapPluginAvailable();
    }
}
