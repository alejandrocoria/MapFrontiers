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
    private static final Component defaultDescriptionLabel = Component.translatable("mapfrontiers.path_style_default_description");
    private static final Component startLabel = Component.translatable("mapfrontiers.start");
    private static final Component endLabel = Component.translatable("mapfrontiers.end");
    private static final Component middleLabel = Component.translatable("mapfrontiers.middle");
    private static final Component innerPointsLabel = Component.translatable("mapfrontiers.inner_points");
    private static final Component segmentsLabel = Component.translatable("mapfrontiers.segments");
    private static final Component labelsAndBannerLabel = Component.translatable("mapfrontiers.labels_and_banner");
    private static final Component labelsRequiredLabel = Component.translatable("mapfrontiers.path_style_labels_required");
    private static final Component replaceDefaultLabel = Component.translatable("mapfrontiers.replace_with_default_path_style");
    private static final Component doneLabel = Component.translatable("gui.done");
    private static final int WARNING_WIDTH = 120;
    private static final int BUTTON_HORIZONTAL_PADDING = 16;

    private final @Nullable FrontierData.PathStyle defaultStyle;
    private final Consumer<FrontierData.PathStyle> afterDoneCallback;
    private FrontierData.PathStyle workingStyle;

    private PathStylePreviewWidget previewWidget;
    private MultiLineTextWidget warningWidget;
    private @Nullable SimpleButton replaceDefaultButton;
    private SimpleButton doneButton;
    private CheckBoxButton checkLabelAtStart;
    private CheckBoxButton checkLabelAtEnd;
    private CheckBoxButton checkLabelAtMiddle;
    private MarkerRow startRow;
    private MarkerRow innerRow;
    private MarkerRow endRow;
    private MarkerRow segmentRow;
    private boolean syncingWidgets = false;

    public PathStyleDialog(FrontierData.PathStyle initialStyle, FrontierData.PathStyle defaultStyle, Consumer<FrontierData.PathStyle> afterDoneCallback) {
        super(Component.empty(), 530, 260);
        this.defaultStyle = new FrontierData.PathStyle(defaultStyle);
        this.afterDoneCallback = afterDoneCallback;
        this.workingStyle = new FrontierData.PathStyle(initialStyle);
    }

    public PathStyleDialog(FrontierData.PathStyle initialStyle, Consumer<FrontierData.PathStyle> afterDoneCallback) {
        super(Component.empty(), 530, 260);
        this.defaultStyle = null;
        this.afterDoneCallback = afterDoneCallback;
        this.workingStyle = new FrontierData.PathStyle(initialStyle);
    }

    @Override
    protected void initScreen() {
        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);
        content.addChild(mainLayout);

        if (defaultStyle == null) {
            MultiLineTextWidget description = mainLayout.addChild(
                    new MultiLineTextWidget(defaultDescriptionLabel.copy().withColor(ColorConstants.TEXT), font),
                    LayoutSettings.defaults().alignHorizontallyCenter());
            description.setMaxWidth(700);
            description.setCentered(true);
        }

        GridLayout markerGrid = new GridLayout().spacing(4);
        markerGrid.defaultCellSetting().alignVerticallyMiddle();
        mainLayout.addChild(markerGrid);

        int row = 0;
        startRow = createMarkerRow(markerGrid, row++, startLabel, workingStyle.startMarker, value -> workingStyle.startMarker = value);
        innerRow = createMarkerRow(markerGrid, row++, innerPointsLabel, workingStyle.innerMarker, value -> workingStyle.innerMarker = value);
        endRow = createMarkerRow(markerGrid, row++, endLabel, workingStyle.endMarker, value -> workingStyle.endMarker = value);
        segmentRow = createMarkerRow(markerGrid, row, segmentsLabel, workingStyle.segmentMarker, value -> workingStyle.segmentMarker = value);

        LinearLayout lowerSection = LinearLayout.horizontal().spacing(12);
        lowerSection.defaultCellSetting().alignVerticallyTop();
        mainLayout.addChild(lowerSection);

        LinearLayout labelLocationsColumn = LinearLayout.vertical().spacing(4);
        lowerSection.addChild(labelLocationsColumn);
        labelLocationsColumn.addChild(new StringWidget(labelsAndBannerLabel, font).setColor(ColorConstants.TEXT_HIGHLIGHT));

        LinearLayout labelsColumn = LinearLayout.vertical().spacing(4);
        labelLocationsColumn.addChild(labelsColumn);

        checkLabelAtStart = createLocationCheckBox(labelsColumn, startLabel, workingStyle.labelAtStart,
                value -> workingStyle.labelAtStart = value);
        checkLabelAtMiddle = createLocationCheckBox(labelsColumn, middleLabel, workingStyle.labelAtMiddle,
                value -> workingStyle.labelAtMiddle = value);
        checkLabelAtEnd = createLocationCheckBox(labelsColumn, endLabel, workingStyle.labelAtEnd,
                value -> workingStyle.labelAtEnd = value);

        labelLocationsColumn.addChild(SpacerElement.width(WARNING_WIDTH));
        warningWidget = labelLocationsColumn.addChild(new MultiLineTextWidget(Component.empty(), font));
        warningWidget.setMaxWidth(WARNING_WIDTH);

        previewWidget = lowerSection.addChild(new PathStylePreviewWidget());

        LinearLayout buttons = LinearLayout.horizontal().spacing(7);
        if (defaultStyle != null) {
            replaceDefaultButton = buttons.addChild(new SimpleButton(font, font.width(replaceDefaultLabel) + BUTTON_HORIZONTAL_PADDING,
                    replaceDefaultLabel, b -> replaceWithDefaultStyle()));
        }
        doneButton = buttons.addChild(new SimpleButton(font, 100, doneLabel, b -> onClose()));
        mainLayout.addChild(buttons, LayoutSettings.defaults().alignHorizontallyCenter());
        updateWarningAndPreview();
    }

    private MarkerRow createMarkerRow(GridLayout layout, int row, Component label, Identifier initialValue, Consumer<Identifier> setter) {
        layout.addChild(new StringWidget(label, font).setColor(ColorConstants.TEXT), row, 0);

        PathMarkerSelectorWidget selector = new PathMarkerSelectorWidget(initialValue, value -> { });
        TextBoxIdentifier textBox = new TextBoxIdentifier(font, 190);
        textBox.setHeight(selector.getHeight());
        textBox.setMaxLength(100);
        textBox.setIdentifier(initialValue);
        MarkerRow markerRow = new MarkerRow(selector, textBox, setter, initialValue);
        selector.setOnPress(markerRow::onSelectorChanged);
        textBox.setValueChangedCallback(markerRow::onTextChanged);
        layout.addChild(selector, row, 1);
        layout.addChild(textBox, row, 2, LayoutSettings.defaults().alignHorizontallyLeft());
        return markerRow;
    }

    private CheckBoxButton createLocationCheckBox(LinearLayout parent, Component label, boolean value, Consumer<Boolean> setter) {
        LinearLayout row = LinearLayout.horizontal().spacing(4);
        row.defaultCellSetting().alignVerticallyBottom();
        parent.addChild(row);

        CheckBoxButton checkBox = row.addChild(new CheckBoxButton(value, b -> {
            if (syncingWidgets) {
                return;
            }

            setter.accept(b.isChecked());
            updateWarningAndPreview();
        }), LayoutSettings.defaults());
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
        innerRow.applyValue(workingStyle.innerMarker);
        endRow.applyValue(workingStyle.endMarker);
        segmentRow.applyValue(workingStyle.segmentMarker);
        setCheckBoxValue(checkLabelAtStart, workingStyle.labelAtStart);
        setCheckBoxValue(checkLabelAtMiddle, workingStyle.labelAtMiddle);
        setCheckBoxValue(checkLabelAtEnd, workingStyle.labelAtEnd);
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
        return workingStyle.labelAtStart || workingStyle.labelAtMiddle || workingStyle.labelAtEnd;
    }

    private FrontierData.PathStyle getPersistedStyle() {
        FrontierData.PathStyle persisted = new FrontierData.PathStyle(workingStyle);
        if (!persisted.labelAtStart && !persisted.labelAtMiddle && !persisted.labelAtEnd) {
            persisted.labelAtStart = true;
            persisted.labelAtMiddle = false;
            persisted.labelAtEnd = false;
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
    public void repositionElements() {
        if (previewWidget != null) {
            previewWidget.setScaleFactor(scaleFactor);
        }
        super.repositionElements();
        if (replaceDefaultButton != null) {
            replaceDefaultButton.setX(content.getX());
            doneButton.setX(content.getX() + (content.getWidth() - doneButton.getWidth()) / 2);
        }
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

        private MarkerRow(PathMarkerSelectorWidget selector, TextBoxIdentifier textBox, Consumer<Identifier> setter, Identifier initialValue) {
            this.selector = selector;
            this.textBox = textBox;
            this.setter = setter;
            selector.setSelectedId(initialValue);
        }

        private void onTextChanged(String value) {
            if (syncingWidgets) {
                return;
            }

            if (!textBox.isInvalid()) {
                Identifier parsed = textBox.getParsedValue();
                if (parsed == null) {
                    return;
                }

                setter.accept(parsed);
                selector.setSelectedId(parsed);
                updateWarningAndPreview();
            }
        }

        private void onSelectorChanged(Identifier value) {
            if (syncingWidgets) {
                return;
            }

            setter.accept(value);
            selector.setSelectedId(value);
            textBox.setIdentifier(value);
            updateWarningAndPreview();
        }

        private void applyValue(Identifier value) {
            selector.setSelectedId(value);
            textBox.setIdentifier(value);
            setter.accept(value);
        }
    }
}
