package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.util.DefaultValueBinding;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.config.BooleanConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@ParametersAreNonnullByDefault
public class FrontierBehaviorDialog extends PanelDialog {
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final Component RESTORE_DEFAULT_VALUE_LABEL = Component.translatable("mapfrontiers.restore_default_value");

    private final BehaviorSnapshot initialSnapshot;
    private TextBoxInt textTitleAnnouncementDuration;
    private TextBoxInt textTitleAnnouncementTimeout;
    private OptionButton buttonTitleAnnouncementAboveHotbar;
    private OptionButton buttonAnnounceUnnamedFrontiers;
    private TextBoxInt textSnapDistance;
    private TextBoxInt textPathProximityEnterDistance;
    private TextBoxInt textPathProximityExitDistance;
    private DefaultValueBinding<Integer> titleAnnouncementDurationBinding;
    private DefaultValueBinding<Integer> titleAnnouncementTimeoutBinding;
    private DefaultValueBinding<Boolean> titleAnnouncementAboveHotbarBinding;
    private DefaultValueBinding<Boolean> announceUnnamedFrontiersBinding;
    private DefaultValueBinding<Integer> snapDistanceBinding;
    private DefaultValueBinding<Integer> pathProximityEnterDistanceBinding;
    private DefaultValueBinding<Integer> pathProximityExitDistanceBinding;
    private int titleAnnouncementDuration;
    private int titleAnnouncementTimeout;
    private boolean titleAnnouncementAboveHotbar;
    private boolean announceUnnamedFrontiers;
    private int snapDistance;
    private int pathProximityEnterDistance;
    private int pathProximityExitDistance;
    private SimpleButton saveButton;
    private boolean syncingWidgets;

    public FrontierBehaviorDialog() {
        initialSnapshot = BehaviorSnapshot.captureFromConfig();
    }

    @Override
    protected void initScreen() {
        initializeState();

        LinearLayout layout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        layout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(layout);

        GridLayout settingsGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        settingsGrid.defaultCellSetting().alignHorizontallyLeft().alignVerticallyMiddle();
        layout.addChild(settingsGrid, LayoutSettings.defaults().alignHorizontallyCenter());

        int row = 0;
        textTitleAnnouncementDuration = createIntConfigTextBox(ClientConfig.TITLE_ANNOUNCEMENT_DURATION, 4, this::setTitleAnnouncementDuration);
        titleAnnouncementDurationBinding = createConfigBinding(ClientConfig.TITLE_ANNOUNCEMENT_DURATION, () -> titleAnnouncementDuration,
                this::setTitleAnnouncementDuration, this::syncTitleAnnouncementDurationWidgets);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.TITLE_ANNOUNCEMENT_DURATION, textTitleAnnouncementDuration, titleAnnouncementDurationBinding.button());
        textTitleAnnouncementTimeout = createIntConfigTextBox(ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT, 4, this::setTitleAnnouncementTimeout);
        titleAnnouncementTimeoutBinding = createConfigBinding(ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT, () -> titleAnnouncementTimeout,
                this::setTitleAnnouncementTimeout, this::syncTitleAnnouncementTimeoutWidgets);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT, textTitleAnnouncementTimeout, titleAnnouncementTimeoutBinding.button());
        buttonTitleAnnouncementAboveHotbar = createOnOffOptionButton(titleAnnouncementAboveHotbar, this::setTitleAnnouncementAboveHotbar);
        titleAnnouncementAboveHotbarBinding = createConfigBinding(ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR, () -> titleAnnouncementAboveHotbar,
                this::setTitleAnnouncementAboveHotbar, this::syncTitleAnnouncementAboveHotbarWidgets);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR, buttonTitleAnnouncementAboveHotbar, titleAnnouncementAboveHotbarBinding.button());
        buttonAnnounceUnnamedFrontiers = createOnOffOptionButton(announceUnnamedFrontiers, this::setAnnounceUnnamedFrontiers);
        announceUnnamedFrontiersBinding = createConfigBinding(ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS, () -> announceUnnamedFrontiers,
                this::setAnnounceUnnamedFrontiers, this::syncAnnounceUnnamedFrontiersWidgets);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS, buttonAnnounceUnnamedFrontiers, announceUnnamedFrontiersBinding.button());

        row = addSectionSpacing(settingsGrid, row);

        textSnapDistance = createIntConfigTextBox(ClientConfig.SNAP_DISTANCE, 2, this::setSnapDistance);
        snapDistanceBinding = createConfigBinding(ClientConfig.SNAP_DISTANCE, () -> snapDistance, this::setSnapDistance, this::syncSnapDistanceWidgets);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.SNAP_DISTANCE, textSnapDistance, snapDistanceBinding.button());

        row = addSectionSpacing(settingsGrid, row);

        textPathProximityEnterDistance = createIntConfigTextBox(ClientConfig.PATH_PROXIMITY_ENTER_DISTANCE, 3, this::setPathProximityEnterDistance);
        pathProximityEnterDistanceBinding = createConfigBinding(ClientConfig.PATH_PROXIMITY_ENTER_DISTANCE, () -> pathProximityEnterDistance,
                this::setPathProximityEnterDistance, this::syncPathProximityEnterDistanceWidgets);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.PATH_PROXIMITY_ENTER_DISTANCE, textPathProximityEnterDistance,
                pathProximityEnterDistanceBinding.button());
        textPathProximityExitDistance = createIntConfigTextBox(ClientConfig.PATH_PROXIMITY_EXIT_DISTANCE, 3, this::setPathProximityExitDistance);
        pathProximityExitDistanceBinding = createConfigBinding(ClientConfig.PATH_PROXIMITY_EXIT_DISTANCE, () -> pathProximityExitDistance,
                this::setPathProximityExitDistance, this::syncPathProximityExitDistanceWidgets);
        addIntSettingRow(settingsGrid, row, ClientConfig.PATH_PROXIMITY_EXIT_DISTANCE, textPathProximityExitDistance,
                pathProximityExitDistanceBinding.button());

        saveButton = addConfirmButton(SAVE_LABEL, b -> saveAndClose());
        addCancelButton();
        refreshSaveButton();
    }

    private void initializeState() {
        titleAnnouncementDuration = ClientConfig.TITLE_ANNOUNCEMENT_DURATION.get();
        titleAnnouncementTimeout = ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT.get();
        titleAnnouncementAboveHotbar = ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR.get();
        announceUnnamedFrontiers = ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS.get();
        snapDistance = ClientConfig.SNAP_DISTANCE.get();
        pathProximityEnterDistance = ClientConfig.PATH_PROXIMITY_ENTER_DISTANCE.get();
        pathProximityExitDistance = ClientConfig.PATH_PROXIMITY_EXIT_DISTANCE.get();
    }

    private int addIntSettingRow(GridLayout settingsGrid, int row, IntConfigEntry entry, TextBoxInt textBox, IconButton restoreButton) {
        settingsGrid.addChild(createConfigLabel(entry), row, 0);
        settingsGrid.addChild(textBox, row, 1);
        settingsGrid.addChild(restoreButton, row, 2);
        return row + 1;
    }

    private int addOptionSettingRow(GridLayout settingsGrid, int row, BooleanConfigEntry entry, OptionButton button, IconButton restoreButton) {
        settingsGrid.addChild(createConfigLabel(entry), row, 0);
        settingsGrid.addChild(button, row, 1);
        settingsGrid.addChild(restoreButton, row, 2);
        return row + 1;
    }

    private int addSectionSpacing(GridLayout settingsGrid, int row) {
        settingsGrid.addChild(SpacerElement.height(4), row, 0, 1, 3);
        return row + 1;
    }

    private StringWidget createConfigLabel(ConfigEntry<?, ?> entry) {
        StringWidget label = new StringWidget(entry.translatedName(), font).setColor(ColorConstants.TEXT);
        label.setTooltip(ScreenHelper.tooltip(entry));
        return label;
    }

    private OptionButton createOnOffOptionButton(boolean value, Consumer<Boolean> onValueChanged) {
        OptionButton button = new OptionButton(font, LayoutConstants.SETTING_CONTROL_WIDTH, b -> {
            if (!syncingWidgets) {
                onValueChanged.accept(b.getSelected() == 0);
            }
        });
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(value ? 0 : 1);
        return button;
    }

    private TextBoxInt createIntConfigTextBox(IntConfigEntry entry, int maxLength, IntConsumer onValueChanged) {
        TextBoxInt textBox = new TextBoxInt(entry, font, LayoutConstants.SETTING_CONTROL_WIDTH);
        textBox.setValue(String.valueOf(entry.get()));
        textBox.setMaxLength(maxLength);
        textBox.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                onValueChanged.accept(value);
            }
        });
        return textBox;
    }

    private <T> DefaultValueBinding<T> createConfigBinding(ConfigEntry<T, ?> entry, java.util.function.Supplier<T> currentValue,
                                                           Consumer<T> restoreValue, Runnable syncWidgetsFromState) {
        return DefaultValueBinding.forConfigEntry(RESTORE_DEFAULT_VALUE_LABEL, entry, currentValue, restoreValue, syncWidgetsFromState);
    }

    private void setTitleAnnouncementDuration(int value) {
        titleAnnouncementDuration = value;
        if (titleAnnouncementDurationBinding != null) {
            titleAnnouncementDurationBinding.refresh();
        }
        refreshSaveButton();
    }

    private void setTitleAnnouncementTimeout(int value) {
        titleAnnouncementTimeout = value;
        if (titleAnnouncementTimeoutBinding != null) {
            titleAnnouncementTimeoutBinding.refresh();
        }
        refreshSaveButton();
    }

    private void setTitleAnnouncementAboveHotbar(boolean value) {
        titleAnnouncementAboveHotbar = value;
        if (titleAnnouncementAboveHotbarBinding != null) {
            titleAnnouncementAboveHotbarBinding.refresh();
        }
        refreshSaveButton();
    }

    private void setAnnounceUnnamedFrontiers(boolean value) {
        announceUnnamedFrontiers = value;
        if (announceUnnamedFrontiersBinding != null) {
            announceUnnamedFrontiersBinding.refresh();
        }
        refreshSaveButton();
    }

    private void setSnapDistance(int value) {
        snapDistance = value;
        if (snapDistanceBinding != null) {
            snapDistanceBinding.refresh();
        }
        refreshSaveButton();
    }

    private void setPathProximityEnterDistance(int value) {
        pathProximityEnterDistance = value;
        refreshPathProximityBindings();
    }

    private void setPathProximityExitDistance(int value) {
        pathProximityExitDistance = value;
        refreshPathProximityBindings();
    }

    private void refreshPathProximityBindings() {
        if (pathProximityEnterDistanceBinding != null) {
            pathProximityEnterDistanceBinding.refresh();
        }
        if (pathProximityExitDistanceBinding != null) {
            pathProximityExitDistanceBinding.refresh();
        }
        refreshSaveButton();
    }

    private void syncTitleAnnouncementDurationWidgets() {
        syncTextBoxValue(textTitleAnnouncementDuration, titleAnnouncementDuration);
    }

    private void syncTitleAnnouncementTimeoutWidgets() {
        syncTextBoxValue(textTitleAnnouncementTimeout, titleAnnouncementTimeout);
    }

    private void syncTitleAnnouncementAboveHotbarWidgets() {
        syncOnOffButtonSelection(buttonTitleAnnouncementAboveHotbar, titleAnnouncementAboveHotbar);
    }

    private void syncAnnounceUnnamedFrontiersWidgets() {
        syncOnOffButtonSelection(buttonAnnounceUnnamedFrontiers, announceUnnamedFrontiers);
    }

    private void syncSnapDistanceWidgets() {
        syncTextBoxValue(textSnapDistance, snapDistance);
    }

    private void syncPathProximityEnterDistanceWidgets() {
        syncTextBoxValue(textPathProximityEnterDistance, pathProximityEnterDistance);
        refreshPathProximityBindings();
    }

    private void syncPathProximityExitDistanceWidgets() {
        syncTextBoxValue(textPathProximityExitDistance, pathProximityExitDistance);
        refreshPathProximityBindings();
    }

    private void syncTextBoxValue(TextBoxInt textBox, int value) {
        syncingWidgets = true;
        try {
            textBox.setValue(value);
        } finally {
            syncingWidgets = false;
        }
    }

    private void syncOnOffButtonSelection(OptionButton button, boolean value) {
        syncingWidgets = true;
        try {
            button.setSelected(value ? 0 : 1);
        } finally {
            syncingWidgets = false;
        }
    }

    private void saveAndClose() {
        clearTextBoxFocus();

        int pathProximityExitDistance = Math.max(pathProximityEnterDistance, this.pathProximityExitDistance);

        ClientConfig.TITLE_ANNOUNCEMENT_DURATION.set(titleAnnouncementDuration);
        ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT.set(titleAnnouncementTimeout);
        ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR.set(titleAnnouncementAboveHotbar);
        ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS.set(announceUnnamedFrontiers);
        ClientConfig.SNAP_DISTANCE.set(snapDistance);
        ClientConfig.PATH_PROXIMITY_ENTER_DISTANCE.set(pathProximityEnterDistance);
        ClientConfig.PATH_PROXIMITY_EXIT_DISTANCE.set(pathProximityExitDistance);

        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private void clearTextBoxFocus() {
        textTitleAnnouncementDuration.setFocused(false);
        textTitleAnnouncementTimeout.setFocused(false);
        textSnapDistance.setFocused(false);
        textPathProximityEnterDistance.setFocused(false);
        textPathProximityExitDistance.setFocused(false);
    }

    private boolean hasChanges() {
        return !initialSnapshot.equals(getPersistedSnapshot());
    }

    private void refreshSaveButton() {
        if (saveButton != null) {
            saveButton.active = hasChanges();
        }
    }

    private BehaviorSnapshot getPersistedSnapshot() {
        return new BehaviorSnapshot(titleAnnouncementDuration, titleAnnouncementTimeout, titleAnnouncementAboveHotbar,
                announceUnnamedFrontiers, snapDistance, pathProximityEnterDistance,
                Math.max(pathProximityEnterDistance, pathProximityExitDistance));
    }

    private record BehaviorSnapshot(int titleAnnouncementDuration,
                                    int titleAnnouncementTimeout,
                                    boolean titleAnnouncementAboveHotbar,
                                    boolean announceUnnamedFrontiers,
                                    int snapDistance,
                                    int pathProximityEnterDistance,
                                    int pathProximityExitDistance) {
        private static BehaviorSnapshot captureFromConfig() {
            return new BehaviorSnapshot(
                    ClientConfig.TITLE_ANNOUNCEMENT_DURATION.get(),
                    ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT.get(),
                    ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR.get(),
                    ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS.get(),
                    ClientConfig.SNAP_DISTANCE.get(),
                    ClientConfig.PATH_PROXIMITY_ENTER_DISTANCE.get(),
                    ClientConfig.PATH_PROXIMITY_EXIT_DISTANCE.get()
            );
        }
    }
}
