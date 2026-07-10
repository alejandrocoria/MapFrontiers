package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.TextColor;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.PreviewFrontiersWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.layout.MFLinearLayout;
import games.alejandrocoria.mapfrontiers.client.gui.util.DefaultValueBinding;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.DoubleConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FrontierAppearanceDialog extends PanelDialog {
    private static final Component HIDE_NAMES_THAT_DONT_FIT_LABEL = ClientConfig.HIDE_NAMES_THAT_DONT_FIT.translatedName();
    private static final Tooltip HIDE_NAMES_THAT_DONT_FIT_TOOLTIP = ScreenHelper.tooltip(ClientConfig.HIDE_NAMES_THAT_DONT_FIT);
    private static final Component FILL_OPACITY_LABEL = ClientConfig.FILL_OPACITY.translatedName();
    private static final Tooltip FILL_OPACITY_TOOLTIP = ScreenHelper.tooltip(ClientConfig.FILL_OPACITY);
    private static final Component BORDER_WIDTH_LABEL = ClientConfig.BORDER_WIDTH.translatedName();
    private static final Tooltip BORDER_WIDTH_TOOLTIP = ScreenHelper.tooltip(ClientConfig.BORDER_WIDTH);
    private static final Component BORDER_OPACITY_LABEL = ClientConfig.BORDER_OPACITY.translatedName();
    private static final Tooltip BORDER_OPACITY_TOOLTIP = ScreenHelper.tooltip(ClientConfig.BORDER_OPACITY);
    private static final Component PATH_MARKER_SIZE_LABEL = ClientConfig.PATH_MARKER_SIZE.translatedName();
    private static final Tooltip PATH_MARKER_SIZE_TOOLTIP = ScreenHelper.tooltip(ClientConfig.PATH_MARKER_SIZE);
    private static final Component PATH_MARKER_OPACITY_LABEL = ClientConfig.PATH_MARKER_OPACITY.translatedName();
    private static final Tooltip PATH_MARKER_OPACITY_TOOLTIP = ScreenHelper.tooltip(ClientConfig.PATH_MARKER_OPACITY);
    private static final Component TEXT_SIZE_LABEL = ClientConfig.TEXT_SIZE.translatedName();
    private static final Tooltip TEXT_SIZE_TOOLTIP = ScreenHelper.tooltip(ClientConfig.TEXT_SIZE);
    private static final Component TEXT_OPACITY_LABEL = ClientConfig.TEXT_OPACITY.translatedName();
    private static final Tooltip TEXT_OPACITY_TOOLTIP = ScreenHelper.tooltip(ClientConfig.TEXT_OPACITY);
    private static final Component TEXT_COLOR_LABEL = ClientConfig.TEXT_COLOR.translatedName();
    private static final Tooltip TEXT_COLOR_TOOLTIP = ScreenHelper.tooltip(ClientConfig.TEXT_COLOR);
    private static final Component BANNER_SIZE_LABEL = ClientConfig.BANNER_SIZE.translatedName();
    private static final Tooltip BANNER_SIZE_TOOLTIP = ScreenHelper.tooltip(ClientConfig.BANNER_SIZE);
    private static final Component BANNER_OPACITY_LABEL = ClientConfig.BANNER_OPACITY.translatedName();
    private static final Tooltip BANNER_OPACITY_TOOLTIP = ScreenHelper.tooltip(ClientConfig.BANNER_OPACITY);
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final Component RESTORE_DEFAULT_VALUE_LABEL = Component.translatable("mapfrontiers.restore_default_value");

    private final AppearanceSnapshot initialSnapshot;
    private StringWidget labelHideNamesThatDontFit;
    private StringWidget labelFillOpacity;
    private StringWidget labelBorderWidth;
    private StringWidget labelBorderOpacity;
    private StringWidget labelPathMarkerSize;
    private StringWidget labelPathMarkerOpacity;
    private StringWidget labelTextSize;
    private StringWidget labelTextOpacity;
    private StringWidget labelTextColor;
    private StringWidget labelBannerSize;
    private StringWidget labelBannerOpacity;
    private OptionButton buttonHideNamesThatDontFit;
    private TextBoxDouble textFillOpacity;
    private TextBoxInt textBorderWidth;
    private TextBoxDouble textBorderOpacity;
    private TextBoxInt textPathMarkerSize;
    private TextBoxDouble textPathMarkerOpacity;
    private TextBoxInt textTextSize;
    private TextBoxDouble textTextOpacity;
    private OptionButton buttonTextColor;
    private TextBoxInt textBannerSize;
    private TextBoxDouble textBannerOpacity;
    private DefaultValueBinding<Boolean> hideNamesThatDontFitBinding;
    private DefaultValueBinding<Double> fillOpacityBinding;
    private DefaultValueBinding<Integer> borderWidthBinding;
    private DefaultValueBinding<Double> borderOpacityBinding;
    private DefaultValueBinding<Integer> pathMarkerSizeBinding;
    private DefaultValueBinding<Double> pathMarkerOpacityBinding;
    private DefaultValueBinding<Integer> textSizeBinding;
    private DefaultValueBinding<Double> textOpacityBinding;
    private DefaultValueBinding<TextColor> textColorBinding;
    private DefaultValueBinding<Integer> bannerSizeBinding;
    private DefaultValueBinding<Double> bannerOpacityBinding;
    private SimpleButton saveButton;
    private PreviewFrontiersWidget previewFrontiers;
    private boolean saved = false;
    private boolean syncingWidgets;

    public FrontierAppearanceDialog() {
        super();
        initialSnapshot = AppearanceSnapshot.capture();
    }

    @Override
    protected void initScreen() {
        MFLinearLayout mainLayout = MFLinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        MFLinearLayout columnsLayout = MFLinearLayout.horizontal().spacing(LayoutConstants.SPACING_MEDIUM);
        mainLayout.addChild(columnsLayout);

        GridLayout settingsLayout = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        settingsLayout.defaultCellSetting().alignHorizontallyLeft().alignVerticallyMiddle();
        columnsLayout.addChild(settingsLayout);
        int row = 0;

        labelHideNamesThatDontFit = settingsLayout.addChild(new StringWidget(HIDE_NAMES_THAT_DONT_FIT_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelHideNamesThatDontFit.setTooltip(HIDE_NAMES_THAT_DONT_FIT_TOOLTIP);
        buttonHideNamesThatDontFit = settingsLayout.addChild(createOnOffOptionButton(this::setHideNamesThatDontFit), row, 1);
        buttonHideNamesThatDontFit.addOption(ON_LABEL);
        buttonHideNamesThatDontFit.addOption(OFF_LABEL);
        buttonHideNamesThatDontFit.setSelected(ClientConfig.HIDE_NAMES_THAT_DONT_FIT.get() ? 0 : 1);
        hideNamesThatDontFitBinding = createConfigBinding(ClientConfig.HIDE_NAMES_THAT_DONT_FIT, this::syncHideNamesThatDontFitWidgets);
        settingsLayout.addChild(hideNamesThatDontFitBinding.button(), row++, 2);

        addSectionSpacing(settingsLayout, row++);

        labelFillOpacity = settingsLayout.addChild(new StringWidget(FILL_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelFillOpacity.setTooltip(FILL_OPACITY_TOOLTIP);
        textFillOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.FILL_OPACITY), row++, 1);
        textFillOpacity.setMaxLength(6);
        textFillOpacity.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setFillOpacity(value);
            }
        });
        fillOpacityBinding = createConfigBinding(ClientConfig.FILL_OPACITY, this::syncFillOpacityWidgets);
        settingsLayout.addChild(fillOpacityBinding.button(), row - 1, 2);

        labelBorderWidth = settingsLayout.addChild(new StringWidget(BORDER_WIDTH_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderWidth.setTooltip(BORDER_WIDTH_TOOLTIP);
        textBorderWidth = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.BORDER_WIDTH), row++, 1);
        textBorderWidth.setMaxLength(2);
        textBorderWidth.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setBorderWidth(value);
            }
        });
        borderWidthBinding = createConfigBinding(ClientConfig.BORDER_WIDTH, this::syncBorderWidthWidgets);
        settingsLayout.addChild(borderWidthBinding.button(), row - 1, 2);

        labelBorderOpacity = settingsLayout.addChild(new StringWidget(BORDER_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderOpacity.setTooltip(BORDER_OPACITY_TOOLTIP);
        textBorderOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.BORDER_OPACITY), row++, 1);
        textBorderOpacity.setMaxLength(6);
        textBorderOpacity.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setBorderOpacity(value);
            }
        });
        borderOpacityBinding = createConfigBinding(ClientConfig.BORDER_OPACITY, this::syncBorderOpacityWidgets);
        settingsLayout.addChild(borderOpacityBinding.button(), row - 1, 2);

        addSectionSpacing(settingsLayout, row++);

        labelPathMarkerSize = settingsLayout.addChild(new StringWidget(PATH_MARKER_SIZE_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelPathMarkerSize.setTooltip(PATH_MARKER_SIZE_TOOLTIP);
        textPathMarkerSize = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.PATH_MARKER_SIZE), row++, 1);
        textPathMarkerSize.setMaxLength(1);
        textPathMarkerSize.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setPathMarkerSize(value);
            }
        });
        pathMarkerSizeBinding = createConfigBinding(ClientConfig.PATH_MARKER_SIZE, this::syncPathMarkerSizeWidgets);
        settingsLayout.addChild(pathMarkerSizeBinding.button(), row - 1, 2);

        labelPathMarkerOpacity = settingsLayout.addChild(new StringWidget(PATH_MARKER_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelPathMarkerOpacity.setTooltip(PATH_MARKER_OPACITY_TOOLTIP);
        textPathMarkerOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.PATH_MARKER_OPACITY), row++, 1);
        textPathMarkerOpacity.setMaxLength(6);
        textPathMarkerOpacity.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setPathMarkerOpacity(value);
            }
        });
        pathMarkerOpacityBinding = createConfigBinding(ClientConfig.PATH_MARKER_OPACITY, this::syncPathMarkerOpacityWidgets);
        settingsLayout.addChild(pathMarkerOpacityBinding.button(), row - 1, 2);

        addSectionSpacing(settingsLayout, row++);

        labelTextSize = settingsLayout.addChild(new StringWidget(TEXT_SIZE_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextSize.setTooltip(TEXT_SIZE_TOOLTIP);
        textTextSize = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.TEXT_SIZE), row++, 1);
        textTextSize.setMaxLength(2);
        textTextSize.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setTextSize(value);
            }
        });
        textSizeBinding = createConfigBinding(ClientConfig.TEXT_SIZE, this::syncTextSizeWidgets);
        settingsLayout.addChild(textSizeBinding.button(), row - 1, 2);

        labelTextOpacity = settingsLayout.addChild(new StringWidget(TEXT_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextOpacity.setTooltip(TEXT_OPACITY_TOOLTIP);
        textTextOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.TEXT_OPACITY), row++, 1);
        textTextOpacity.setMaxLength(6);
        textTextOpacity.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setTextOpacity(value);
            }
        });
        textOpacityBinding = createConfigBinding(ClientConfig.TEXT_OPACITY, this::syncTextOpacityWidgets);
        settingsLayout.addChild(textOpacityBinding.button(), row - 1, 2);

        labelTextColor = settingsLayout.addChild(new StringWidget(TEXT_COLOR_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextColor.setTooltip(TEXT_COLOR_TOOLTIP);
        buttonTextColor = settingsLayout.addChild(new OptionButton(font, 60, (b) -> {
            if (!syncingWidgets) {
                setTextColor(TextColor.values()[b.getSelected()]);
            }
        }), row++, 1);
        buttonTextColor.addOption(ClientConfig.getTranslatedEnum(TextColor.FrontierColor));
        buttonTextColor.addOption(ClientConfig.getTranslatedEnum(TextColor.FrontierColorBright));
        buttonTextColor.addOption(ClientConfig.getTranslatedEnum(TextColor.White));
        buttonTextColor.setSelected(ClientConfig.TEXT_COLOR.get().ordinal());
        textColorBinding = createConfigBinding(ClientConfig.TEXT_COLOR, this::syncTextColorWidgets);
        settingsLayout.addChild(textColorBinding.button(), row - 1, 2);

        addSectionSpacing(settingsLayout, row++);

        labelBannerSize = settingsLayout.addChild(new StringWidget(BANNER_SIZE_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerSize.setTooltip(BANNER_SIZE_TOOLTIP);
        textBannerSize = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.BANNER_SIZE), row++, 1);
        textBannerSize.setMaxLength(2);
        textBannerSize.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setBannerSize(value);
            }
        });
        bannerSizeBinding = createConfigBinding(ClientConfig.BANNER_SIZE, this::syncBannerSizeWidgets);
        settingsLayout.addChild(bannerSizeBinding.button(), row - 1, 2);

        labelBannerOpacity = settingsLayout.addChild(new StringWidget(BANNER_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerOpacity.setTooltip(BANNER_OPACITY_TOOLTIP);
        textBannerOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.BANNER_OPACITY), row++, 1);
        textBannerOpacity.setMaxLength(6);
        textBannerOpacity.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setBannerOpacity(value);
            }
        });
        bannerOpacityBinding = createConfigBinding(ClientConfig.BANNER_OPACITY, this::syncBannerOpacityWidgets);
        settingsLayout.addChild(bannerOpacityBinding.button(), row - 1, 2);

        previewFrontiers = columnsLayout.addChild(new PreviewFrontiersWidget());

        saveButton = addConfirmButton(SAVE_LABEL, (b) -> saveAndClose());
        addCancelButton();
        refreshSaveButton();
    }

    @Override
    protected void resetContentToMinimumSize() {
        previewFrontiers.setScaleFactor(1.f);
    }

    @Override
    protected void resizeContentToAvailableSpace() {
        previewFrontiers.setScaleFactor(scaleFactor);
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

    private static void addSectionSpacing(GridLayout layout, int row) {
        layout.addChild(SpacerElement.height(4), row, 0, 1, 3);
    }

    private OptionButton createOnOffOptionButton(java.util.function.Consumer<Boolean> onValueChanged) {
        return new OptionButton(font, 60, b -> {
            if (!syncingWidgets) {
                onValueChanged.accept(b.getSelected() == 0);
            }
        });
    }

    private TextBoxInt createIntConfigTextBox(IntConfigEntry entry) {
        TextBoxInt textBox = new TextBoxInt(entry, font, 60);
        textBox.setValue(String.valueOf(entry.get()));
        return textBox;
    }

    private TextBoxDouble createDoubleConfigTextBox(DoubleConfigEntry entry) {
        TextBoxDouble textBox = new TextBoxDouble(entry, font, 60);
        textBox.setValue(String.valueOf(entry.get()));
        return textBox;
    }

    private <T> DefaultValueBinding<T> createConfigBinding(ConfigEntry<T, ?> entry, Runnable syncWidgetsFromState) {
        return DefaultValueBinding.forConfigEntry(RESTORE_DEFAULT_VALUE_LABEL, entry, entry::get,
                value -> setConfigValue(entry, value), syncWidgetsFromState);
    }

    private void setHideNamesThatDontFit(boolean value) {
        setConfigValue(ClientConfig.HIDE_NAMES_THAT_DONT_FIT, value);
        hideNamesThatDontFitBinding.refresh();
    }

    private void setFillOpacity(double value) {
        setConfigValue(ClientConfig.FILL_OPACITY, value);
        fillOpacityBinding.refresh();
    }

    private void setBorderWidth(int value) {
        setConfigValue(ClientConfig.BORDER_WIDTH, value);
        borderWidthBinding.refresh();
    }

    private void setBorderOpacity(double value) {
        setConfigValue(ClientConfig.BORDER_OPACITY, value);
        borderOpacityBinding.refresh();
    }

    private void setPathMarkerSize(int value) {
        setConfigValue(ClientConfig.PATH_MARKER_SIZE, value);
        pathMarkerSizeBinding.refresh();
    }

    private void setPathMarkerOpacity(double value) {
        setConfigValue(ClientConfig.PATH_MARKER_OPACITY, value);
        pathMarkerOpacityBinding.refresh();
    }

    private void setTextSize(int value) {
        setConfigValue(ClientConfig.TEXT_SIZE, value);
        textSizeBinding.refresh();
    }

    private void setTextOpacity(double value) {
        setConfigValue(ClientConfig.TEXT_OPACITY, value);
        textOpacityBinding.refresh();
    }

    private void setTextColor(TextColor value) {
        setConfigValue(ClientConfig.TEXT_COLOR, value);
        textColorBinding.refresh();
    }

    private void setBannerSize(int value) {
        setConfigValue(ClientConfig.BANNER_SIZE, value);
        bannerSizeBinding.refresh();
    }

    private void setBannerOpacity(double value) {
        setConfigValue(ClientConfig.BANNER_OPACITY, value);
        bannerOpacityBinding.refresh();
    }

    private <T> void setConfigValue(ConfigEntry<T, ?> entry, T value) {
        entry.set(value);
        previewFrontiers.configUpdated();
        refreshSaveButton();
    }

    private void syncHideNamesThatDontFitWidgets() {
        syncOnOffButtonSelection(buttonHideNamesThatDontFit, ClientConfig.HIDE_NAMES_THAT_DONT_FIT.get());
    }

    private void syncFillOpacityWidgets() {
        syncTextBoxValue(textFillOpacity, ClientConfig.FILL_OPACITY.get());
    }

    private void syncBorderWidthWidgets() {
        syncTextBoxValue(textBorderWidth, ClientConfig.BORDER_WIDTH.get());
    }

    private void syncBorderOpacityWidgets() {
        syncTextBoxValue(textBorderOpacity, ClientConfig.BORDER_OPACITY.get());
    }

    private void syncPathMarkerSizeWidgets() {
        syncTextBoxValue(textPathMarkerSize, ClientConfig.PATH_MARKER_SIZE.get());
    }

    private void syncPathMarkerOpacityWidgets() {
        syncTextBoxValue(textPathMarkerOpacity, ClientConfig.PATH_MARKER_OPACITY.get());
    }

    private void syncTextSizeWidgets() {
        syncTextBoxValue(textTextSize, ClientConfig.TEXT_SIZE.get());
    }

    private void syncTextOpacityWidgets() {
        syncTextBoxValue(textTextOpacity, ClientConfig.TEXT_OPACITY.get());
    }

    private void syncTextColorWidgets() {
        syncOptionButtonSelection(buttonTextColor, ClientConfig.TEXT_COLOR.get().ordinal());
    }

    private void syncBannerSizeWidgets() {
        syncTextBoxValue(textBannerSize, ClientConfig.BANNER_SIZE.get());
    }

    private void syncBannerOpacityWidgets() {
        syncTextBoxValue(textBannerOpacity, ClientConfig.BANNER_OPACITY.get());
    }

    private void syncOnOffButtonSelection(OptionButton button, boolean value) {
        syncOptionButtonSelection(button, value ? 0 : 1);
    }

    private void syncOptionButtonSelection(OptionButton button, int selected) {
        syncingWidgets = true;
        try {
            button.setSelected(selected);
        } finally {
            syncingWidgets = false;
        }
    }

    private void syncTextBoxValue(TextBoxInt textBox, int value) {
        syncingWidgets = true;
        try {
            textBox.setValue(value);
        } finally {
            syncingWidgets = false;
        }
    }

    private void syncTextBoxValue(TextBoxDouble textBox, double value) {
        syncingWidgets = true;
        try {
            textBox.setValue(value);
        } finally {
            syncingWidgets = false;
        }
    }

    private boolean hasChanges() {
        return !initialSnapshot.equals(AppearanceSnapshot.capture());
    }

    private void refreshSaveButton() {
        if (saveButton != null) {
            saveButton.active = hasChanges();
        }
    }

    private record AppearanceSnapshot(boolean hideNamesThatDontFit,
                                      double fillOpacity,
                                      int borderWidth,
                                      double borderOpacity,
                                      int pathMarkerSize,
                                      double pathMarkerOpacity,
                                      int textSize,
                                      double textOpacity,
                                      TextColor textColor,
                                      int bannerSize,
                                      double bannerOpacity) {
        private static AppearanceSnapshot capture() {
            return new AppearanceSnapshot(
                    ClientConfig.HIDE_NAMES_THAT_DONT_FIT.get(),
                    ClientConfig.FILL_OPACITY.get(),
                    ClientConfig.BORDER_WIDTH.get(),
                    ClientConfig.BORDER_OPACITY.get(),
                    ClientConfig.PATH_MARKER_SIZE.get(),
                    ClientConfig.PATH_MARKER_OPACITY.get(),
                    ClientConfig.TEXT_SIZE.get(),
                    ClientConfig.TEXT_OPACITY.get(),
                    ClientConfig.TEXT_COLOR.get(),
                    ClientConfig.BANNER_SIZE.get(),
                    ClientConfig.BANNER_OPACITY.get()
            );
        }

        private void apply() {
            ClientConfig.HIDE_NAMES_THAT_DONT_FIT.set(hideNamesThatDontFit);
            ClientConfig.FILL_OPACITY.set(fillOpacity);
            ClientConfig.BORDER_WIDTH.set(borderWidth);
            ClientConfig.BORDER_OPACITY.set(borderOpacity);
            ClientConfig.PATH_MARKER_SIZE.set(pathMarkerSize);
            ClientConfig.PATH_MARKER_OPACITY.set(pathMarkerOpacity);
            ClientConfig.TEXT_SIZE.set(textSize);
            ClientConfig.TEXT_OPACITY.set(textOpacity);
            ClientConfig.TEXT_COLOR.set(textColor);
            ClientConfig.BANNER_SIZE.set(bannerSize);
            ClientConfig.BANNER_OPACITY.set(bannerOpacity);
        }
    }
}
