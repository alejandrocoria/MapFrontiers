package games.alejandrocoria.mapfrontiers.client.gui.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.PathMarkerSelectorWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.PathStylePreviewWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxIdentifier;
import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class PathStyleDialog extends AutoScaledScreen {
    private static final Component titleLabel = Component.translatable("mapfrontiers.title_path_style");
    private static final Component defaultTitleLabel = Component.translatable("mapfrontiers.title_default_path_style");
    private static final Component defaultDescriptionLabel = Component.translatable("mapfrontiers.path_style_default_description");
    private static final Component startLabel = Component.translatable("mapfrontiers.start");
    private static final Component endLabel = Component.translatable("mapfrontiers.end");
    private static final Component middleLabel = Component.translatable("mapfrontiers.middle");
    private static final Component middlePointsLabel = Component.translatable("mapfrontiers.middle_points");
    private static final Component segmentsLabel = Component.translatable("mapfrontiers.segments");
    private static final Component labelsAndBannerLabel = Component.translatable("mapfrontiers.labels_and_banner");
    private static final Component previewLabel = Component.translatable("mapfrontiers.preview");
    private static final Component invalidIdentifierLabel = Component.translatable("mapfrontiers.path_style_invalid_identifier");
    private static final Component labelsRequiredLabel = Component.translatable("mapfrontiers.path_style_labels_required");
    private static final Component replaceDefaultLabel = Component.translatable("mapfrontiers.replace_with_default_path_style");
    private static final Component doneLabel = Component.translatable("gui.done");

    private final @Nullable FrontierData.PathStyle defaultStyle;
    private final Consumer<FrontierData.PathStyle> afterDoneCallback;
    private FrontierData.PathStyle workingStyle;

    private PathStylePreviewWidget previewWidget;
    private MultiLineTextWidget warningWidget;
    private CheckBoxButton checkLabelAtStart;
    private CheckBoxButton checkLabelAtEnd;
    private CheckBoxButton checkLabelAtMiddle;
    private MarkerRow startRow;
    private MarkerRow endRow;
    private MarkerRow middleRow;
    private MarkerRow segmentRow;
    private boolean syncingWidgets = false;

    public PathStyleDialog(FrontierData.PathStyle initialStyle, FrontierData.PathStyle defaultStyle, Consumer<FrontierData.PathStyle> afterDoneCallback) {
        super(titleLabel, 760, 280);
        this.defaultStyle = new FrontierData.PathStyle(defaultStyle);
        this.afterDoneCallback = afterDoneCallback;
        this.workingStyle = new FrontierData.PathStyle(initialStyle);
    }

    public PathStyleDialog(FrontierData.PathStyle initialStyle, Consumer<FrontierData.PathStyle> afterDoneCallback) {
        super(defaultTitleLabel, 760, 280);
        this.defaultStyle = null;
        this.afterDoneCallback = afterDoneCallback;
        this.workingStyle = new FrontierData.PathStyle(initialStyle);
    }

    @Override
    protected void initScreen() {
        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        if (defaultStyle == null) {
            MultiLineTextWidget description = mainLayout.addChild(new MultiLineTextWidget(defaultDescriptionLabel.copy().withColor(ColorConstants.TEXT), font));
            description.setMaxWidth(700);
            description.setCentered(true);
        }

        LinearLayout columns = LinearLayout.horizontal().spacing(12);
        columns.defaultCellSetting().alignVerticallyTop();
        mainLayout.addChild(columns);

        GridLayout leftGrid = new GridLayout().spacing(4);
        leftGrid.defaultCellSetting().alignVerticallyMiddle();
        columns.addChild(leftGrid);

        int row = 0;
        startRow = createMarkerRow(leftGrid, row++, startLabel, workingStyle.startMarker, value -> workingStyle.startMarker = value);
        endRow = createMarkerRow(leftGrid, row++, endLabel, workingStyle.endMarker, value -> workingStyle.endMarker = value);
        middleRow = createMarkerRow(leftGrid, row++, middlePointsLabel, workingStyle.middleMarker, value -> workingStyle.middleMarker = value);
        segmentRow = createMarkerRow(leftGrid, row++, segmentsLabel, workingStyle.segmentMarker, value -> workingStyle.segmentMarker = value);

        leftGrid.addChild(SpacerElement.height(8), row++, 0);
        leftGrid.addChild(new StringWidget(labelsAndBannerLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT), row++, 0, 1, 3);

        LinearLayout labelsRow = LinearLayout.horizontal().spacing(10);
        leftGrid.addChild(labelsRow, row++, 0, 1, 3);

        checkLabelAtStart = createLocationCheckBox(labelsRow, startLabel, workingStyle.labelAtStart, value -> workingStyle.labelAtStart = value);
        checkLabelAtEnd = createLocationCheckBox(labelsRow, endLabel, workingStyle.labelAtEnd, value -> workingStyle.labelAtEnd = value);
        checkLabelAtMiddle = createLocationCheckBox(labelsRow, middleLabel, workingStyle.labelAtMiddle, value -> workingStyle.labelAtMiddle = value);

        LinearLayout rightColumn = LinearLayout.vertical().spacing(6);
        rightColumn.defaultCellSetting().alignHorizontallyCenter();
        columns.addChild(rightColumn);

        rightColumn.addChild(new StringWidget(previewLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));
        previewWidget = rightColumn.addChild(new PathStylePreviewWidget());

        warningWidget = rightColumn.addChild(new MultiLineTextWidget(Component.empty(), font));
        warningWidget.setMaxWidth(220);

        if (defaultStyle != null) {
            rightColumn.addChild(new SimpleButton(font, 170, replaceDefaultLabel, b -> replaceWithDefaultStyle()));
        }

        mainLayout.addChild(new SimpleButton(font, 100, doneLabel, b -> onClose()));
        updateWarningAndPreview();
    }

    private MarkerRow createMarkerRow(GridLayout layout, int row, Component label, Identifier initialValue, Consumer<Identifier> setter) {
        layout.addChild(new StringWidget(label, font).setColor(ColorConstants.TEXT), row, 0);

        PathMarkerSelectorWidget selector = new PathMarkerSelectorWidget(initialValue, value -> {
            if (syncingWidgets) {
                return;
            }

            setter.accept(value);
            selectorValueChanged();
        });
        layout.addChild(selector, row, 1);

        TextBoxIdentifier textBox = new TextBoxIdentifier(font, 190);
        textBox.setMaxLength(100);
        MarkerRow markerRow = new MarkerRow(selector, textBox, setter, initialValue);
        textBox.setValueChangedCallback(markerRow::onTextChanged);
        textBox.setValue(initialValue.toString());
        layout.addChild(textBox, row, 2, LayoutSettings.defaults().alignHorizontallyLeft());
        return markerRow;
    }

    private void selectorValueChanged() {
        updateWarningAndPreview();
    }

    private CheckBoxButton createLocationCheckBox(LinearLayout parent, Component label, boolean value, Consumer<Boolean> setter) {
        LinearLayout row = LinearLayout.horizontal().spacing(4);
        parent.addChild(row);

        CheckBoxButton checkBox = row.addChild(new CheckBoxButton(value, b -> {
            if (syncingWidgets) {
                return;
            }

            setter.accept(b.isChecked());
            updateWarningAndPreview();
        }));
        row.addChild(new StringWidget(label, font).setColor(ColorConstants.TEXT));
        return checkBox;
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
        endRow.applyValue(workingStyle.endMarker);
        middleRow.applyValue(workingStyle.middleMarker);
        segmentRow.applyValue(workingStyle.segmentMarker);
        setCheckBoxValue(checkLabelAtStart, workingStyle.labelAtStart);
        setCheckBoxValue(checkLabelAtEnd, workingStyle.labelAtEnd);
        setCheckBoxValue(checkLabelAtMiddle, workingStyle.labelAtMiddle);
        syncingWidgets = false;
        updateWarningAndPreview();
    }

    private void updateWarningAndPreview() {
        previewWidget.setPathStyle(workingStyle);
        warningWidget.setMessage(hasAnyLabelLocation()
                ? Component.empty()
                : labelsRequiredLabel.copy().withColor(ColorConstants.TEXT_ERROR));
    }

    private boolean hasAnyLabelLocation() {
        return workingStyle.labelAtStart || workingStyle.labelAtEnd || workingStyle.labelAtMiddle;
    }

    private FrontierData.PathStyle getPersistedStyle() {
        FrontierData.PathStyle persisted = new FrontierData.PathStyle(workingStyle);
        if (!persisted.labelAtStart && !persisted.labelAtEnd && !persisted.labelAtMiddle) {
            persisted.labelAtStart = true;
            persisted.labelAtEnd = false;
            persisted.labelAtMiddle = false;
        }

        return persisted;
    }

    private static void setCheckBoxValue(CheckBoxButton button, boolean value) {
        if (button.isChecked() != value) {
            button.toggle();
        }
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
    }

    @Override
    public void onClose() {
        super.onClose();
        afterDoneCallback.accept(getPersistedStyle());
    }

    private final class MarkerRow {
        private final PathMarkerSelectorWidget selector;
        private final TextBoxIdentifier textBox;
        private final Consumer<Identifier> setter;
        private Identifier appliedValue;

        private MarkerRow(PathMarkerSelectorWidget selector, TextBoxIdentifier textBox, Consumer<Identifier> setter, Identifier initialValue) {
            this.selector = selector;
            this.textBox = textBox;
            this.setter = setter;
            appliedValue = initialValue;
            selector.setSelectedId(initialValue);
        }

        private void onTextChanged(String value) {
            if (syncingWidgets) {
                return;
            }

            try {
                Identifier parsed = Identifier.parse(value);
                appliedValue = parsed;
                setter.accept(parsed);
                selector.setSelectedId(parsed);
                textBox.setError(null);
                updateWarningAndPreview();
            } catch (Exception ignored) {
                textBox.setError(invalidIdentifierLabel);
            }
        }

        private void applyValue(Identifier value) {
            appliedValue = value;
            selector.setSelectedId(value);
            textBox.setError(null);
            textBox.setValue(value.toString());
            setter.accept(value);
        }
    }
}
