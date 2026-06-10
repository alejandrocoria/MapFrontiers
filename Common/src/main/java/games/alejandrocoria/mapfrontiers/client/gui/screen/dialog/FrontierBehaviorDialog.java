package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
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

@ParametersAreNonnullByDefault
public class FrontierBehaviorDialog extends PanelDialog {
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");

    private TextBoxInt textTitleAnnouncementDuration;
    private TextBoxInt textTitleAnnouncementTimeout;
    private OptionButton buttonTitleAnnouncementAboveHotbar;
    private OptionButton buttonAnnounceUnnamedFrontiers;
    private TextBoxInt textSnapDistance;
    private TextBoxInt textPathProximityEnterDistance;
    private TextBoxInt textPathProximityExitDistance;

    @Override
    protected void initScreen() {
        LinearLayout layout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        layout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(layout);

        GridLayout settingsGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        settingsGrid.defaultCellSetting().alignHorizontallyLeft().alignVerticallyMiddle();
        layout.addChild(settingsGrid, LayoutSettings.defaults().alignHorizontallyCenter());

        int row = 0;
        textTitleAnnouncementDuration = createIntConfigTextBox(ClientConfig.TITLE_ANNOUNCEMENT_DURATION, 4);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.TITLE_ANNOUNCEMENT_DURATION, textTitleAnnouncementDuration);
        textTitleAnnouncementTimeout = createIntConfigTextBox(ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT, 4);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.TITLE_ANNOUNCEMENT_TIMEOUT, textTitleAnnouncementTimeout);
        buttonTitleAnnouncementAboveHotbar = createOnOffOptionButton(ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR.get());
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.TITLE_ANNOUNCEMENT_ABOVE_HOTBAR, buttonTitleAnnouncementAboveHotbar);
        buttonAnnounceUnnamedFrontiers = createOnOffOptionButton(ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS.get());
        row = addOptionSettingRow(settingsGrid, row, ClientConfig.ANNOUNCE_UNNAMED_FRONTIERS, buttonAnnounceUnnamedFrontiers);

        row = addSectionSpacing(settingsGrid, row);

        textSnapDistance = createIntConfigTextBox(ClientConfig.SNAP_DISTANCE, 2);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.SNAP_DISTANCE, textSnapDistance);

        row = addSectionSpacing(settingsGrid, row);

        textPathProximityEnterDistance = createIntConfigTextBox(ClientConfig.PATH_PROXIMITY_ENTER_DISTANCE, 3);
        row = addIntSettingRow(settingsGrid, row, ClientConfig.PATH_PROXIMITY_ENTER_DISTANCE, textPathProximityEnterDistance);
        textPathProximityExitDistance = createIntConfigTextBox(ClientConfig.PATH_PROXIMITY_EXIT_DISTANCE, 3);
        addIntSettingRow(settingsGrid, row, ClientConfig.PATH_PROXIMITY_EXIT_DISTANCE, textPathProximityExitDistance);

        addConfirmButton(SAVE_LABEL, b -> saveAndClose());
        addCancelButton();
    }

    private int addIntSettingRow(GridLayout settingsGrid, int row, IntConfigEntry entry, TextBoxInt textBox) {
        settingsGrid.addChild(createConfigLabel(entry), row, 0);
        settingsGrid.addChild(textBox, row, 1);
        return row + 1;
    }

    private int addOptionSettingRow(GridLayout settingsGrid, int row, BooleanConfigEntry entry, OptionButton button) {
        settingsGrid.addChild(createConfigLabel(entry), row, 0);
        settingsGrid.addChild(button, row, 1);
        return row + 1;
    }

    private int addSectionSpacing(GridLayout settingsGrid, int row) {
        settingsGrid.addChild(SpacerElement.height(4), row, 0, 1, 2);
        return row + 1;
    }

    private StringWidget createConfigLabel(ConfigEntry<?, ?> entry) {
        StringWidget label = new StringWidget(entry.translatedName(), font).setColor(ColorConstants.TEXT);
        label.setTooltip(ScreenHelper.tooltip(entry));
        return label;
    }

    private OptionButton createOnOffOptionButton(boolean value) {
        OptionButton button = new OptionButton(font, LayoutConstants.SETTING_CONTROL_WIDTH, OptionButton.DO_NOTHING);
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(value ? 0 : 1);
        return button;
    }

    private TextBoxInt createIntConfigTextBox(IntConfigEntry entry, int maxLength) {
        TextBoxInt textBox = new TextBoxInt(entry, font, LayoutConstants.SETTING_CONTROL_WIDTH);
        textBox.setValue(String.valueOf(entry.get()));
        textBox.setMaxLength(maxLength);
        return textBox;
    }

    private void saveAndClose() {
        clearTextBoxFocus();

        int titleAnnouncementDuration = textTitleAnnouncementDuration.clamped();
        int titleAnnouncementTimeout = textTitleAnnouncementTimeout.clamped();
        boolean titleAnnouncementAboveHotbar = buttonTitleAnnouncementAboveHotbar.getSelected() == 0;
        boolean announceUnnamedFrontiers = buttonAnnounceUnnamedFrontiers.getSelected() == 0;
        int snapDistance = textSnapDistance.clamped();
        int pathProximityEnterDistance = textPathProximityEnterDistance.clamped();
        int pathProximityExitDistance = Math.max(pathProximityEnterDistance, textPathProximityExitDistance.clamped());

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
}
