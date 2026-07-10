package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.util.DefaultValueBinding;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.config.BooleanConfigEntry;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ConfirmationSettingsDialog extends PanelDialog {
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final Component RESTORE_DEFAULT_VALUE_LABEL = Component.translatable("mapfrontiers.restore_default_value");

    private final ConfirmationSnapshot initialSnapshot;
    private DefaultValueBinding<Boolean> askFrontierDeleteBinding;
    private DefaultValueBinding<Boolean> askCollectionDeleteBinding;
    private DefaultValueBinding<Boolean> askGroupDeleteBinding;
    private DefaultValueBinding<Boolean> askUserDeleteBinding;
    private DefaultValueBinding<Boolean> askTemporaryFrontierCreateBinding;
    private DefaultValueBinding<Boolean> askTemporaryCollectionCreateBinding;
    private SimpleButton saveButton;
    private boolean saved = false;

    public ConfirmationSettingsDialog() {
        initialSnapshot = ConfirmationSnapshot.capture();
    }

    @Override
    protected void initScreen() {
        LinearLayout layout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        layout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(layout);

        GridLayout settingsGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        settingsGrid.defaultCellSetting().alignHorizontallyLeft().alignVerticallyMiddle();
        layout.addChild(settingsGrid, LayoutSettings.defaults().alignHorizontallyCenter());

        int row = 0;
        askFrontierDeleteBinding = addOptionSettingRow(settingsGrid, row++, ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE);
        askCollectionDeleteBinding = addOptionSettingRow(settingsGrid, row++, ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE);
        askGroupDeleteBinding = addOptionSettingRow(settingsGrid, row++, ClientConfig.ASK_CONFIRMATION_GROUP_DELETE);
        askUserDeleteBinding = addOptionSettingRow(settingsGrid, row++, ClientConfig.ASK_CONFIRMATION_USER_DELETE);
        askTemporaryFrontierCreateBinding = addOptionSettingRow(settingsGrid, row++, ClientConfig.ASK_CONFIRMATION_TEMPORARY_FRONTIER_CREATE);
        askTemporaryCollectionCreateBinding = addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_TEMPORARY_COLLECTION_CREATE);

        saveButton = addConfirmButton(SAVE_LABEL, b -> saveAndClose());
        addCancelButton();
        refreshSaveButton();
    }

    @Override
    public void onClose() {
        boolean changed = hasChanges();
        if (!saved && changed) {
            initialSnapshot.apply();
            ClientGlobalEvents.postUpdatedConfigEvent();
        }
        super.onClose();
    }

    private void saveAndClose() {
        boolean changed = hasChanges();
        saved = true;
        if (changed) {
            ClientGlobalEvents.postUpdatedConfigEvent();
        }
        super.onClose();
    }

    private DefaultValueBinding<Boolean> addOptionSettingRow(GridLayout settingsGrid, int row, BooleanConfigEntry entry) {
        OptionButton button = createOnOffOptionButton(entry);
        DefaultValueBinding<Boolean> binding = DefaultValueBinding.forConfigEntry(RESTORE_DEFAULT_VALUE_LABEL, entry,
                entry::get, entry::set, () -> syncOnOffButtonSelection(button, entry.get()));
        settingsGrid.addChild(createConfigLabel(entry), row, 0);
        settingsGrid.addChild(button, row, 1);
        settingsGrid.addChild(binding.button(), row, 2);
        return binding;
    }

    private StringWidget createConfigLabel(BooleanConfigEntry entry) {
        StringWidget label = new StringWidget(entry.translatedName(), font).setColor(ColorConstants.TEXT);
        label.setTooltip(ScreenHelper.tooltip(entry));
        return label;
    }

    private OptionButton createOnOffOptionButton(BooleanConfigEntry entry) {
        OptionButton button = new OptionButton(font, LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH, b -> {
            entry.set(b.getSelected() == 0);
            refreshAllBindings();
        });
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(entry.get() ? 0 : 1);
        return button;
    }

    private void syncOnOffButtonSelection(OptionButton button, boolean value) {
        button.setSelected(value ? 0 : 1);
    }

    private void refreshAllBindings() {
        askFrontierDeleteBinding.refresh();
        askCollectionDeleteBinding.refresh();
        askGroupDeleteBinding.refresh();
        askUserDeleteBinding.refresh();
        askTemporaryFrontierCreateBinding.refresh();
        askTemporaryCollectionCreateBinding.refresh();
        refreshSaveButton();
    }

    private boolean hasChanges() {
        return !initialSnapshot.equals(ConfirmationSnapshot.capture());
    }

    private void refreshSaveButton() {
        if (saveButton != null) {
            saveButton.active = hasChanges();
        }
    }

    private record ConfirmationSnapshot(
            boolean askFrontierDelete,
            boolean askCollectionDelete,
            boolean askGroupDelete,
            boolean askUserDelete,
            boolean askTemporaryFrontierCreate,
            boolean askTemporaryCollectionCreate) {
        private static ConfirmationSnapshot capture() {
            return new ConfirmationSnapshot(
                    ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.get(),
                    ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE.get(),
                    ClientConfig.ASK_CONFIRMATION_GROUP_DELETE.get(),
                    ClientConfig.ASK_CONFIRMATION_USER_DELETE.get(),
                    ClientConfig.ASK_CONFIRMATION_TEMPORARY_FRONTIER_CREATE.get(),
                    ClientConfig.ASK_CONFIRMATION_TEMPORARY_COLLECTION_CREATE.get()
            );
        }

        private void apply() {
            ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.set(askFrontierDelete);
            ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE.set(askCollectionDelete);
            ClientConfig.ASK_CONFIRMATION_GROUP_DELETE.set(askGroupDelete);
            ClientConfig.ASK_CONFIRMATION_USER_DELETE.set(askUserDelete);
            ClientConfig.ASK_CONFIRMATION_TEMPORARY_FRONTIER_CREATE.set(askTemporaryFrontierCreate);
            ClientConfig.ASK_CONFIRMATION_TEMPORARY_COLLECTION_CREATE.set(askTemporaryCollectionCreate);
        }
    }
}
