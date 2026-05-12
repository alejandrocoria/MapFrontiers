package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
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
    private static final int OPTION_WIDTH = 40;

    private final ConfirmationSnapshot initialSnapshot;
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
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_GROUP_DELETE);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_USER_DELETE);
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_TEMPORARY_FRONTIER_CREATE);
        addOptionSettingRow(settingsGrid, row, ClientConfig.ASK_CONFIRMATION_TEMPORARY_COLLECTION_CREATE);

        addConfirmButton(SAVE_LABEL, b -> saveAndClose());
        addCancelButton();
    }

    @Override
    public void onClose() {
        if (!saved) {
            initialSnapshot.apply();
        }
        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private void saveAndClose() {
        saved = true;
        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private int addOptionSettingRow(GridLayout settingsGrid, int row, BooleanConfigEntry entry) {
        settingsGrid.addChild(createConfigLabel(entry), row, 0);
        settingsGrid.addChild(createOnOffOptionButton(entry), row, 1);
        return row + 1;
    }

    private StringWidget createConfigLabel(BooleanConfigEntry entry) {
        StringWidget label = new StringWidget(entry.translatedName(), font).setColor(ColorConstants.TEXT);
        label.setTooltip(ScreenHelper.tooltip(entry));
        return label;
    }

    private OptionButton createOnOffOptionButton(BooleanConfigEntry entry) {
        OptionButton button = new OptionButton(font, OPTION_WIDTH, b -> entry.set(b.getSelected() == 0));
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(entry.get() ? 0 : 1);
        return button;
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
