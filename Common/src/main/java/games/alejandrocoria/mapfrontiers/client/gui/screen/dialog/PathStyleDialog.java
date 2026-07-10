package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.PathMarkerSelectorWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.PathStylePreviewWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxIdentifier;
import games.alejandrocoria.mapfrontiers.client.gui.layout.MFLinearLayout;
import games.alejandrocoria.mapfrontiers.client.gui.util.DefaultValueBinding;
import games.alejandrocoria.mapfrontiers.common.config.StringConfigEntry;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class PathStyleDialog extends PanelDialog {
    private static final Component DEFAULT_DESCRIPTION_LABEL = Component.translatable("mapfrontiers.path_style_default_description");
    private static final Component START_LABEL = Component.translatable("mapfrontiers.start");
    private static final Component END_LABEL = Component.translatable("mapfrontiers.end");
    private static final Component MIDDLE_LABEL = Component.translatable("mapfrontiers.middle");
    private static final Component INNER_POINTS_LABEL = Component.translatable("mapfrontiers.inner_points");
    private static final Component SEGMENTS_LABEL = Component.translatable("mapfrontiers.segments");
    private static final Component LABELS_AND_BANNER_LABEL = Component.translatable("mapfrontiers.labels_and_banner");
    private static final Component LABELS_REQUIRED_LABEL = Component.translatable("mapfrontiers.path_style_labels_required");
    private static final Component RESTORE_DEFAULT_VALUE_LABEL = Component.translatable("mapfrontiers.restore_default_value");
    private static final Component REPLACE_DEFAULT_LABEL = Component.translatable("mapfrontiers.replace_with_default_path_style");
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final int WARNING_WIDTH = 120;
    private static final int BUTTON_HORIZONTAL_PADDING = 16;

    private final FrontierData.PathStyle initialPersistedStyle;
    private final @Nullable FrontierData.PathStyle defaultStyle;
    private final boolean restoreToClientDefaults;
    private final Consumer<FrontierData.PathStyle> saveCallback;
    private FrontierData.PathStyle workingStyle;

    private PathStylePreviewWidget previewWidget;
    private MultiLineTextWidget warningWidget;
    private CheckBoxButton checkLabelAtStart;
    private CheckBoxButton checkLabelAtEnd;
    private CheckBoxButton checkLabelAtMiddle;
    private MarkerRow startRow;
    private MarkerRow innerRow;
    private MarkerRow endRow;
    private MarkerRow segmentRow;
    private DefaultValueBinding<LabelLocationsState> labelLocationsBinding;
    private SimpleButton saveButton;
    private boolean syncingWidgets = false;

    public PathStyleDialog(FrontierData.PathStyle initialStyle, FrontierData.PathStyle defaultStyle, Consumer<FrontierData.PathStyle> saveCallback) {
        this(initialStyle, defaultStyle, false, saveCallback);
    }

    public PathStyleDialog(FrontierData.PathStyle initialStyle, Consumer<FrontierData.PathStyle> saveCallback) {
        this(initialStyle, null, false, saveCallback);
    }

    public static PathStyleDialog forClientDefaults(FrontierData.PathStyle initialStyle, Consumer<FrontierData.PathStyle> saveCallback) {
        return new PathStyleDialog(initialStyle, null, true, saveCallback);
    }

    private PathStyleDialog(FrontierData.PathStyle initialStyle, @Nullable FrontierData.PathStyle defaultStyle,
                            boolean restoreToClientDefaults, Consumer<FrontierData.PathStyle> saveCallback) {
        super();
        this.initialPersistedStyle = normalizePersistedStyle(initialStyle);
        this.defaultStyle = defaultStyle == null ? null : new FrontierData.PathStyle(defaultStyle);
        this.restoreToClientDefaults = restoreToClientDefaults;
        this.saveCallback = saveCallback;
        this.workingStyle = new FrontierData.PathStyle(initialStyle);
    }

    @Override
    protected void initScreen() {
        MFLinearLayout mainLayout = MFLinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        content.addChild(mainLayout);

        if (defaultStyle == null) {
            MultiLineTextWidget description = mainLayout.addChild(
                    new MultiLineTextWidget(DEFAULT_DESCRIPTION_LABEL.copy().withStyle(style -> style.withColor(ColorConstants.TEXT)), font),
                    LayoutSettings.defaults().alignHorizontallyCenter());
            description.setMaxWidth(700);
            description.setCentered(true);
        }

        GridLayout markerGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        markerGrid.defaultCellSetting().alignVerticallyMiddle();
        mainLayout.addChild(markerGrid);

        int row = 0;
        startRow = createMarkerRow(markerGrid, row++, START_LABEL, workingStyle.startMarker,
                ClientConfig.FRONTIER_DEFAULT_PATH_STYLE_START, () -> workingStyle.startMarker, value -> workingStyle.startMarker = value);
        innerRow = createMarkerRow(markerGrid, row++, INNER_POINTS_LABEL, workingStyle.innerMarker,
                ClientConfig.FRONTIER_DEFAULT_PATH_STYLE_INNER, () -> workingStyle.innerMarker, value -> workingStyle.innerMarker = value);
        endRow = createMarkerRow(markerGrid, row++, END_LABEL, workingStyle.endMarker,
                ClientConfig.FRONTIER_DEFAULT_PATH_STYLE_END, () -> workingStyle.endMarker, value -> workingStyle.endMarker = value);
        segmentRow = createMarkerRow(markerGrid, row, SEGMENTS_LABEL, workingStyle.segmentMarker,
                ClientConfig.FRONTIER_DEFAULT_PATH_STYLE_SEGMENT, () -> workingStyle.segmentMarker, value -> workingStyle.segmentMarker = value);

        MFLinearLayout lowerSection = MFLinearLayout.horizontal().spacing(12);
        lowerSection.defaultCellSetting().alignVerticallyTop();
        mainLayout.addChild(lowerSection);

        MFLinearLayout labelLocationsColumn = MFLinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        lowerSection.addChild(labelLocationsColumn);
        MFLinearLayout labelLocationsHeader = MFLinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        labelLocationsHeader.defaultCellSetting().alignVerticallyMiddle();
        labelLocationsColumn.addChild(labelLocationsHeader);
        labelLocationsHeader.addChild(new StringWidget(LABELS_AND_BANNER_LABEL, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        labelLocationsBinding = createLabelLocationsBinding();
        if (labelLocationsBinding != null) {
            labelLocationsHeader.addChild(labelLocationsBinding.button());
        }

        MFLinearLayout labelsColumn = MFLinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        labelLocationsColumn.addChild(labelsColumn);

        checkLabelAtStart = createLocationCheckBox(labelsColumn, START_LABEL, workingStyle.labelAtStart,
                value -> workingStyle.labelAtStart = value);
        checkLabelAtMiddle = createLocationCheckBox(labelsColumn, MIDDLE_LABEL, workingStyle.labelAtMiddle,
                value -> workingStyle.labelAtMiddle = value);
        checkLabelAtEnd = createLocationCheckBox(labelsColumn, END_LABEL, workingStyle.labelAtEnd,
                value -> workingStyle.labelAtEnd = value);

        labelLocationsColumn.addChild(SpacerElement.width(WARNING_WIDTH));
        warningWidget = labelLocationsColumn.addChild(new MultiLineTextWidget(Component.empty(), font));
        warningWidget.setMaxWidth(WARNING_WIDTH);

        previewWidget = lowerSection.addChild(new PathStylePreviewWidget());

        if (defaultStyle != null) {
            MFLinearLayout defaultActionRow = MFLinearLayout.horizontal();
            defaultActionRow.addChild(new SimpleButton(font, font.width(REPLACE_DEFAULT_LABEL) + BUTTON_HORIZONTAL_PADDING,
                    REPLACE_DEFAULT_LABEL, b -> replaceWithDefaultStyle()));
            mainLayout.addChild(defaultActionRow, LayoutSettings.defaults().alignHorizontallyCenter());
        }

        saveButton = addConfirmButton(SAVE_LABEL, (b) -> saveAndClose());
        addCancelButton();

        updateWarningAndPreview();
    }

    private MarkerRow createMarkerRow(GridLayout layout, int row, Component label, ResourceLocation initialValue,
                                      StringConfigEntry entry, java.util.function.Supplier<ResourceLocation> getter,
                                      Consumer<ResourceLocation> setter) {
        layout.addChild(new StringWidget(label, font).setColor(ColorConstants.TEXT), row, 0);

        PathMarkerSelectorWidget selector = new PathMarkerSelectorWidget(initialValue, value -> { });
        TextBoxIdentifier textBox = new TextBoxIdentifier(font, 190);
        textBox.setHeight(selector.getHeight());
        textBox.setMaxLength(100);
        textBox.setIdentifier(initialValue);
        MarkerRow markerRow = new MarkerRow(selector, textBox, getter, setter, initialValue);
        selector.setOnPress(markerRow::onSelectorChanged);
        textBox.setValueChangedCallback(markerRow::onTextChanged);
        layout.addChild(selector, row, 1);
        layout.addChild(textBox, row, 2, LayoutSettings.defaults().alignHorizontallyLeft());
        markerRow.setRestoreBinding(createMarkerRestoreBinding(entry, markerRow));
        if (markerRow.getRestoreBinding() != null) {
            layout.addChild(markerRow.getRestoreBinding().button(), row, 3);
        }
        return markerRow;
    }

    private CheckBoxButton createLocationCheckBox(MFLinearLayout parent, Component label, boolean value, Consumer<Boolean> setter) {
        MFLinearLayout row = MFLinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        row.defaultCellSetting().alignVerticallyBottom();
        parent.addChild(row);

        CheckBoxButton checkBox = row.addChild(new CheckBoxButton(value, b -> {
            if (syncingWidgets) {
                return;
            }

            setter.accept(b.isChecked());
            updateWarningAndPreview();
            if (labelLocationsBinding != null) {
                labelLocationsBinding.refresh();
            }
        }), LayoutSettings.defaults());
        row.addChild(new StringWidget(label, font).setColor(ColorConstants.TEXT));
        return checkBox;
    }

    private @Nullable DefaultValueBinding<ResourceLocation> createMarkerRestoreBinding(StringConfigEntry entry, MarkerRow markerRow) {
        if (!restoreToClientDefaults) {
            return null;
        }

        return new DefaultValueBinding<>(markerRow::getCurrentValue,
                () -> new ResourceLocation(entry.defaultValue()),
                markerRow::setCurrentValue,
                () -> {
                    markerRow.syncWidgetsFromState();
                    updateWarningAndPreview();
                },
                DefaultValueBinding.createRestoreTooltip(RESTORE_DEFAULT_VALUE_LABEL, entry.defaultTooltipComponent()));
    }

    private @Nullable DefaultValueBinding<LabelLocationsState> createLabelLocationsBinding() {
        if (!restoreToClientDefaults) {
            return null;
        }

        LabelLocationsState defaultState = getDefaultLabelLocationsState();
        return new DefaultValueBinding<>(this::getCurrentLabelLocationsState, () -> defaultState, this::applyLabelLocationsState,
                this::syncLabelLocationWidgets,
                DefaultValueBinding.createRestoreTooltip(RESTORE_DEFAULT_VALUE_LABEL,
                        createDefaultLine(createLabelLocationsSummary(defaultState))));
    }

    private void replaceWithDefaultStyle() {
        if (defaultStyle == null) {
            return;
        }

        workingStyle = new FrontierData.PathStyle(defaultStyle);
        syncWidgetsFromStyle();
    }

    private void syncWidgetsFromStyle() {
        syncingWidgets = true;
        startRow.applyValue(workingStyle.startMarker);
        innerRow.applyValue(workingStyle.innerMarker);
        endRow.applyValue(workingStyle.endMarker);
        segmentRow.applyValue(workingStyle.segmentMarker);
        setCheckBoxValue(checkLabelAtStart, workingStyle.labelAtStart);
        setCheckBoxValue(checkLabelAtMiddle, workingStyle.labelAtMiddle);
        setCheckBoxValue(checkLabelAtEnd, workingStyle.labelAtEnd);
        syncingWidgets = false;
        startRow.refreshRestoreBinding();
        innerRow.refreshRestoreBinding();
        endRow.refreshRestoreBinding();
        segmentRow.refreshRestoreBinding();
        if (labelLocationsBinding != null) {
            labelLocationsBinding.refresh();
        }
        updateWarningAndPreview();
    }

    private void updateWarningAndPreview() {
        previewWidget.setPathStyle(workingStyle);
        warningWidget.setMessage(hasAnyLabelLocation()
                ? Component.empty()
                : LABELS_REQUIRED_LABEL.copy().withStyle(style -> style.withColor(ColorConstants.TEXT_ERROR_NORMAL)));
        refreshSaveButton();
    }

    private boolean hasAnyLabelLocation() {
        return workingStyle.labelAtStart || workingStyle.labelAtMiddle || workingStyle.labelAtEnd;
    }

    private FrontierData.PathStyle getPersistedStyle() {
        return normalizePersistedStyle(workingStyle);
    }

    private static void setCheckBoxValue(CheckBoxButton button, boolean value) {
        if (button.isChecked() != value) {
            button.toggle();
        }
    }

    private LabelLocationsState getCurrentLabelLocationsState() {
        return new LabelLocationsState(workingStyle.labelAtStart, workingStyle.labelAtMiddle, workingStyle.labelAtEnd);
    }

    private LabelLocationsState getDefaultLabelLocationsState() {
        return new LabelLocationsState(
                ClientConfig.FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_START.defaultValue(),
                ClientConfig.FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_MIDDLE.defaultValue(),
                ClientConfig.FRONTIER_DEFAULT_PATH_STYLE_LABEL_AT_END.defaultValue());
    }

    private void applyLabelLocationsState(LabelLocationsState state) {
        workingStyle.labelAtStart = state.start();
        workingStyle.labelAtMiddle = state.middle();
        workingStyle.labelAtEnd = state.end();
    }

    private void syncLabelLocationWidgets() {
        syncingWidgets = true;
        setCheckBoxValue(checkLabelAtStart, workingStyle.labelAtStart);
        setCheckBoxValue(checkLabelAtMiddle, workingStyle.labelAtMiddle);
        setCheckBoxValue(checkLabelAtEnd, workingStyle.labelAtEnd);
        syncingWidgets = false;
        if (labelLocationsBinding != null) {
            labelLocationsBinding.refresh();
        }
        updateWarningAndPreview();
    }

    private Component createDefaultLine(Component defaultValueComponent) {
        return Component.translatable("mapfrontiers.default", defaultValueComponent)
                .withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true));
    }

    private Component createLabelLocationsSummary(LabelLocationsState state) {
        java.util.List<String> parts = new java.util.ArrayList<>(3);
        if (state.start()) {
            parts.add(START_LABEL.getString());
        }
        if (state.middle()) {
            parts.add(MIDDLE_LABEL.getString());
        }
        if (state.end()) {
            parts.add(END_LABEL.getString());
        }
        return Component.literal(String.join(", ", parts));
    }

    @Override
    protected void resetContentToMinimumSize() {
        if (previewWidget != null) {
            previewWidget.setScaleFactor(1.f);
        }
    }

    @Override
    protected void resizeContentToAvailableSpace() {
        if (previewWidget != null) {
            previewWidget.setScaleFactor(scaleFactor);
        }
    }

    private void saveAndClose() {
        super.onClose();
        saveCallback.accept(getPersistedStyle());
    }

    private boolean hasChanges() {
        return !initialPersistedStyle.equals(getPersistedStyle());
    }

    private void refreshSaveButton() {
        if (saveButton != null) {
            saveButton.active = hasChanges();
        }
    }

    private static FrontierData.PathStyle normalizePersistedStyle(FrontierData.PathStyle source) {
        FrontierData.PathStyle persisted = new FrontierData.PathStyle(source);
        persisted.normalizeForPersistence();
        return persisted;
    }

    private final class MarkerRow {
        private final PathMarkerSelectorWidget selector;
        private final TextBoxIdentifier textBox;
        private final java.util.function.Supplier<ResourceLocation> getter;
        private final Consumer<ResourceLocation> setter;
        private @Nullable DefaultValueBinding<ResourceLocation> restoreBinding;

        private MarkerRow(PathMarkerSelectorWidget selector, TextBoxIdentifier textBox, java.util.function.Supplier<ResourceLocation> getter,
                          Consumer<ResourceLocation> setter, ResourceLocation initialValue) {
            this.selector = selector;
            this.textBox = textBox;
            this.getter = getter;
            this.setter = setter;
            selector.setSelectedId(initialValue);
        }

        private void onTextChanged(String value) {
            if (syncingWidgets) {
                return;
            }

            if (!textBox.isInvalid()) {
                ResourceLocation parsed = textBox.getParsedValue();
                if (parsed == null) {
                    return;
                }

                setter.accept(parsed);
                selector.setSelectedId(parsed);
                refreshRestoreBinding();
                updateWarningAndPreview();
            }
        }

        private void onSelectorChanged(ResourceLocation value) {
            if (syncingWidgets) {
                return;
            }

            setter.accept(value);
            selector.setSelectedId(value);
            textBox.setIdentifier(value);
            refreshRestoreBinding();
            updateWarningAndPreview();
        }

        private void applyValue(ResourceLocation value) {
            selector.setSelectedId(value);
            textBox.setIdentifier(value);
            setter.accept(value);
        }

        private void setCurrentValue(ResourceLocation value) {
            setter.accept(value);
        }

        private ResourceLocation getCurrentValue() {
            return getter.get();
        }

        private void syncWidgetsFromState() {
            ResourceLocation value = getter.get();
            selector.setSelectedId(value);
            textBox.setIdentifier(value);
            refreshRestoreBinding();
        }

        private void setRestoreBinding(@Nullable DefaultValueBinding<ResourceLocation> restoreBinding) {
            this.restoreBinding = restoreBinding;
        }

        private @Nullable DefaultValueBinding<ResourceLocation> getRestoreBinding() {
            return restoreBinding;
        }

        private void refreshRestoreBinding() {
            if (restoreBinding != null) {
                restoreBinding.refresh();
            }
        }
    }

    private record LabelLocationsState(boolean start, boolean middle, boolean end) {
    }
}
