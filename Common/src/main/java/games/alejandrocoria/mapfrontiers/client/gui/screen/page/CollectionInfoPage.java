package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPaletteWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPicker;
import games.alejandrocoria.mapfrontiers.client.gui.component.PluginSourceBadge;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleSlider;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteCollectionConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.SettingsUserFormatter;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

@ParametersAreNonnullByDefault
public class CollectionInfoPage extends PageScreen {
    private static final String COLLECTION_VIEW_ZOOM_KEY = "mapfrontiers.collection_view_zoom";
    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_collection_info");
    private static final Component NAME_LABEL = Component.translatable("mapfrontiers.name");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final Component DELETE_LABEL = Component.translatable("mapfrontiers.delete");
    private static final Component RANDOM_COLOR_LABEL = Component.translatable("mapfrontiers.random_color");
    private static final Component PERSONAL_LABEL = Component.translatable("mapfrontiers.personal_type");
    private static final Component GLOBAL_LABEL = Component.translatable("mapfrontiers.global_type");
    private static final String TYPE_KEY = "mapfrontiers.type";
    private static final String TEMPORARY_KEY = "mapfrontiers.temporary";
    private static final String OWNER_KEY = "mapfrontiers.owner";
    private static final String ORIGINAL_OWNER_KEY = "mapfrontiers.original_owner";
    private static final String FRONTIERS_COUNT_KEY = "mapfrontiers.collection_frontiers_count";
    private static final String AREA_KEY = "mapfrontiers.area";
    private static final String LENGTH_KEY = "mapfrontiers.length";
    private static final String CREATED_KEY = "mapfrontiers.created";
    private static final String MODIFIED_KEY = "mapfrontiers.modified";
    private static final Component R_LABEL = Component.literal("R");
    private static final Component G_LABEL = Component.literal("G");
    private static final Component B_LABEL = Component.literal("B");
    private static final Tooltip COPY_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.copy.tooltip"));
    private static final Tooltip PASTE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.paste.tooltip"));
    private static final Tooltip OPEN_PASTE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.open_paste_options.tooltip"));
    private static final Tooltip CLOSE_PASTE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.close_paste_options.tooltip"));
    private static final Tooltip UNDO_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.undo.tooltip"));
    private static final Tooltip REDO_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.redo.tooltip"));
    private static final Component PASTE_NAME_LABEL = Component.translatable("mapfrontiers.paste_name");
    private static final Component PASTE_COLOR_LABEL = Component.translatable("mapfrontiers.paste_color");
    private static final Component PASTE_COLLECTION_VIEW_ZOOM_LABEL = Component.translatable("mapfrontiers.paste_collection_view_zoom");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final Component DISABLED_LABEL = Component.translatable("mapfrontiers.disabled");
    private static final int SECTION_WIDTH = 146;
    private static final int NAME_SECTION_WIDTH = SECTION_WIDTH * 2 + LayoutConstants.SPACING_MEDIUM;
    private static final int DEFAULT_TEXTBOX_HEIGHT = 17;
    private static final int RGB_LABEL_HEIGHT = 8;
    private static final int RGB_TEXTBOX_WIDTH = 33;
    private static final int RGB_ROW_SPACER_WIDTH = 4;
    private static final int RGB_INLINE_SPACING = 3;
    private static final int CLIPBOARD_SPACER_WIDTH = SECTION_WIDTH - LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH;

    private final UUID collectionId;
    private final CollectionData originalCollection;
    private final CollectionData collection;
    private final Stack<CollectionData> undoStack = new Stack<>();
    private final Stack<CollectionData> redoStack = new Stack<>();
    private boolean saveChangesOnClose = true;
    private boolean syncingWidgets = false;

    private TextBox textName;
    private TextBoxInt textRed;
    private TextBoxInt textGreen;
    private TextBoxInt textBlue;
    private SimpleSlider sliderCollectionViewZoom;
    private ColorPicker colorPicker;
    private ColorPaletteWidget colorPalette;
    private SimpleButton buttonRandomColor;
    private IconButton buttonCopy;
    private IconButton buttonPaste;
    private IconButton buttonPasteOptions;
    private IconButton buttonUndo;
    private IconButton buttonRedo;
    private OptionButton buttonPasteName;
    private OptionButton buttonPasteColor;
    private OptionButton buttonPasteCollectionViewZoom;
    private SimpleButton buttonDelete;
    private SimpleButton buttonDone;
    private StringWidget labelPasteName;
    private StringWidget labelPasteColor;
    private StringWidget labelPasteCollectionViewZoom;
    private StringWidget ownerLabel;
    private StringWidget typeLabel;
    private StringWidget frontiersCountLabel;
    private StringWidget areaLabel;
    private StringWidget lengthLabel;
    private @Nullable StringWidget createdLabel;
    private @Nullable StringWidget modifiedLabel;

    public CollectionInfoPage(CollectionData collection) {
        super(TITLE_LABEL);
        this.collectionId = collection.getId();
        this.originalCollection = new CollectionData(collection);
        this.collection = new CollectionData(collection);
        undoStack.push(createMetadataSnapshot());

        MapFrontiersClient.getCollectionEvents().subscribeDeleted(this, deletedId -> {
            if (collectionId.equals(deletedId)) {
                saveChangesOnClose = false;
                onClose();
            }
        });

        MapFrontiersClient.getCollectionEvents().subscribeUpdated(this, updatedCollection -> {
            if (collectionId.equals(updatedCollection.getId()) && !hasChanges()) {
                applyExternalCollectionUpdate(updatedCollection);
            }
        });

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> refreshViewState());
    }

    @Override
    protected void initScreen() {
        GridLayout mainLayout = new GridLayout().spacing(LayoutConstants.SPACING_MEDIUM);
        content.addChild(mainLayout);

        buildOverviewSection(mainLayout);
        buildInfoSection(mainLayout);
        buildColorSection(mainLayout);
        buildClipboardSection(mainLayout);
        buildBottomButtons();

        refreshInfoLabels();
        refreshViewState();
        setInitialFocus(buttonDone);
    }

    private void buildOverviewSection(GridLayout mainLayout) {
        LinearLayout overviewColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        overviewColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(overviewColumn, 0, 0, 1, 2);

        LinearLayout headerRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_TINY);
        headerRow.addChild(new StringWidget(NAME_LABEL, font).setColor(ColorConstants.WHITE));
        PluginSourceBadge sourceBadge = new PluginSourceBadge(font, collection.getSourcePluginId(), true);
        int sourceWidth = sourceBadge.getWidth();
        headerRow.addChild(SpacerElement.width(Math.max(0,
                NAME_SECTION_WIDTH - font.width(NAME_LABEL.getVisualOrderText()) - sourceWidth - LayoutConstants.SPACING_TINY * 2)));
        headerRow.addChild(sourceBadge);
        overviewColumn.addChild(headerRow);

        textName = new TextBox(font, NAME_SECTION_WIDTH);
        textName.setMaxLength(CollectionData.MAX_NAME_CHARACTERS);
        textName.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textName.setValue(collection.getName());
        textName.setValueChangedCallback(this::onNameChanged);
        textName.setLostFocusCallback(value -> addCurrentStateToUndo());
        overviewColumn.addChild(textName);

        sliderCollectionViewZoom = new SimpleSlider(font, NAME_SECTION_WIDTH, COLLECTION_VIEW_ZOOM_KEY,
                CollectionData.getCollectionViewZoomLevels(), collection.getCollectionViewZoom(),
                this::onCollectionViewZoomChanged, CollectionInfoPage::formatCollectionViewZoomLabel);
        overviewColumn.addChild(sliderCollectionViewZoom);
    }

    private void buildInfoSection(GridLayout mainLayout) {
        LinearLayout infoColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_TINY);
        mainLayout.addChild(infoColumn, 0, 2, LayoutSettings.defaults().alignHorizontallyLeft());

        ownerLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE));
        typeLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE));
        frontiersCountLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE));
        areaLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE));
        lengthLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE));

        if (collection.getCreated() != null) {
            createdLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE));
        }
        if (collection.getModified() != null) {
            modifiedLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.WHITE));
        }
    }

    private void buildColorSection(GridLayout mainLayout) {
        colorPicker = new ColorPicker(collection.getColor(), this::onColorPicked);
        mainLayout.addChild(colorPicker, 1, 0, LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter());

        LinearLayout colorColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        colorColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(colorColumn, 1, 1, LayoutSettings.defaults().alignVerticallyBottom());

        LinearLayout rgbRow = LinearLayout.horizontal().spacing(RGB_INLINE_SPACING);
        rgbRow.defaultCellSetting().alignVerticallyMiddle();
        colorColumn.addChild(rgbRow);

        rgbRow.addChild(new StringWidget(R_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_R));
        textRed = createRgbTextBox(value -> (collection.getColor() & 0xFF00FFFF) | (value << 16));
        rgbRow.addChild(textRed);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(G_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_G));
        textGreen = createRgbTextBox(value -> (collection.getColor() & 0xFFFF00FF) | (value << 8));
        rgbRow.addChild(textGreen);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(B_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_B));
        textBlue = createRgbTextBox(value -> (collection.getColor() & 0xFFFFFF00) | value);
        rgbRow.addChild(textBlue);

        buttonRandomColor = new SimpleButton(font, SECTION_WIDTH, RANDOM_COLOR_LABEL, b -> onRandomColorPressed());
        colorColumn.addChild(buttonRandomColor);

        colorPalette = new ColorPaletteWidget(collection.getColor(), color -> applyColorChange(color, true));
        colorColumn.addChild(colorPalette);

        syncColorWidgets(collection.getColor());
    }

    private void buildClipboardSection(GridLayout mainLayout) {
        GridLayout editColumn = new GridLayout().rowSpacing(LayoutConstants.SPACING_SMALL);
        editColumn.defaultCellSetting().alignHorizontallyLeft();
        editColumn.addChild(SpacerElement.width(CLIPBOARD_SPACER_WIDTH), 0, 0);
        mainLayout.addChild(editColumn, 1, 2, LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyLeft());

        labelPasteName = editColumn.addChild(new StringWidget(PASTE_NAME_LABEL, font).setColor(ColorConstants.TEXT), 0, 0);
        buttonPasteName = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_NAME.get(), ClientConfig.PASTE_NAME::set), 0, 1);

        labelPasteColor = editColumn.addChild(new StringWidget(PASTE_COLOR_LABEL, font).setColor(ColorConstants.TEXT), 1, 0);
        buttonPasteColor = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_COLOR.get(), ClientConfig.PASTE_COLOR::set), 1, 1);

        labelPasteCollectionViewZoom = editColumn.addChild(new StringWidget(PASTE_COLLECTION_VIEW_ZOOM_LABEL, font).setColor(ColorConstants.TEXT), 2, 0);
        buttonPasteCollectionViewZoom = editColumn.addChild(
                createBinaryOptionButton(ClientConfig.PASTE_COLLECTION_VIEW_ZOOM.get(), ClientConfig.PASTE_COLLECTION_VIEW_ZOOM::set), 2, 1);

        LinearLayout editButtons = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        editColumn.addChild(editButtons, 3, 0);

        buttonCopy = editButtons.addChild(new IconButton(IconButton.Type.Copy, b -> onCopyPressed()));
        buttonCopy.setTooltip(COPY_TOOLTIP);

        LinearLayout pasteButtons = LinearLayout.horizontal();
        editButtons.addChild(pasteButtons);

        buttonPaste = pasteButtons.addChild(new IconButton(IconButton.Type.Paste, b -> onPastePressed()));
        buttonPaste.setTooltip(PASTE_TOOLTIP);

        buttonPasteOptions = pasteButtons.addChild(new IconButton(IconButton.Type.ExpandOptions, b -> onPasteOptionsPressed()));
        buttonPasteOptions.setTooltip(OPEN_PASTE_TOOLTIP);

        buttonUndo = editButtons.addChild(new IconButton(IconButton.Type.Undo, b -> undo()));
        buttonUndo.setTooltip(UNDO_TOOLTIP);

        buttonRedo = editButtons.addChild(new IconButton(IconButton.Type.Redo, b -> redo()));
        buttonRedo.setTooltip(REDO_TOOLTIP);
    }

    private void buildBottomButtons() {
        buttonDelete = addBottomButton(new SimpleButton(font, SECTION_WIDTH, DELETE_LABEL, b -> onDeletePressed()));
        buttonDelete.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonDone = addBottomButton(new SimpleButton(font, SECTION_WIDTH, DONE_LABEL, b -> onClose()));
    }

    private TextBoxInt createRgbTextBox(IntUnaryOperator colorComposer) {
        TextBoxInt textBox = new TextBoxInt(0, 0, 255, font, RGB_TEXTBOX_WIDTH);
        textBox.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textBox.setValueChangedCallback(value -> applyColorChange(colorComposer.applyAsInt(value), true));
        return textBox;
    }

    private OptionButton createBinaryOptionButton(boolean defaultValue, Consumer<Boolean> consumer) {
        OptionButton button = new OptionButton(font, LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH, b -> consumer.accept(b.getSelected() == 0));
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(defaultValue ? 0 : 1);
        return button;
    }

    private void onNameChanged(String value) {
        if (syncingWidgets || Objects.equals(collection.getName(), value)) {
            return;
        }

        collection.setName(value);
    }

    private void onCopyPressed() {
        MapFrontiersClient.setCollectionClipboard(createClipboardSnapshot());
        minecraft.keyboardHandler.setClipboard(collection.getId().toString());
        refreshViewState();
    }

    private void onPastePressed() {
        CollectionData clipboard = MapFrontiersClient.getCollectionClipboard();
        if (clipboard == null || !canUpdateCollection()) {
            return;
        }

        boolean changed = applyEditableMetadata(clipboard,
                ClientConfig.PASTE_NAME.get(),
                ClientConfig.PASTE_COLOR.get(),
                ClientConfig.PASTE_COLLECTION_VIEW_ZOOM.get());
        if (!changed) {
            return;
        }

        addCurrentStateToUndo();
        refreshViewState();
        if (minecraft.getLastInputType().isKeyboard()) {
            setInitialFocus(buttonPaste);
        }
    }

    private void onPasteOptionsPressed() {
        ClientConfig.PASTE_OPTIONS_VISIBLE.set(!ClientConfig.PASTE_OPTIONS_VISIBLE.get());
        refreshViewState();
        ClientGlobalEvents.postUpdatedConfigEvent();
    }

    private void onDeletePressed() {
        if (ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE.get()) {
            new DeleteCollectionConfirmationDialog(collection, response -> {
                if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                    ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE.set(false);
                    ClientGlobalEvents.postUpdatedConfigEvent();
                }
                deleteCollection();
            }).display();
        } else {
            deleteCollection();
        }
    }

    private void onRandomColorPressed() {
        applyColorChange(ColorHelper.getRandomColor(), true);
    }

    private void onCollectionViewZoomChanged(int zoom, boolean dragging) {
        if (syncingWidgets || collection.getCollectionViewZoom() == zoom) {
            return;
        }

        collection.setCollectionViewZoom(zoom);
        if (!dragging) {
            addCurrentStateToUndo();
        }
    }

    private void applyColorChange(int color, boolean trackUndo) {
        if (syncingWidgets) {
            return;
        }

        if (color != collection.getColor()) {
            collection.setColor(color);
            colorPicker.setColor(color);
            syncColorWidgets(color);
        }

        if (trackUndo) {
            addCurrentStateToUndo();
        }
    }

    private void onColorPicked(int color, boolean dragging) {
        if (syncingWidgets) {
            return;
        }

        if (color != collection.getColor()) {
            collection.setColor(color);
            syncColorWidgets(color);
        }

        if (!dragging) {
            addCurrentStateToUndo();
        }
    }

    private boolean applyEditableMetadata(CollectionData source, boolean pasteName, boolean pasteColor, boolean pasteCollectionViewZoom) {
        boolean changed = false;
        syncingWidgets = true;
        try {
            if (pasteName && !Objects.equals(collection.getName(), source.getName())) {
                collection.setName(source.getName());
                textName.setValue(source.getName());
                changed = true;
            }

            if (pasteColor && collection.getColor() != source.getColor()) {
                collection.setColor(source.getColor());
                colorPicker.setColor(source.getColor());
                syncColorWidgets(source.getColor());
                changed = true;
            }

            if (pasteCollectionViewZoom && collection.getCollectionViewZoom() != source.getCollectionViewZoom()) {
                collection.setCollectionViewZoom(source.getCollectionViewZoom());
                sliderCollectionViewZoom.setValue(source.getCollectionViewZoom());
                changed = true;
            }
        } finally {
            syncingWidgets = false;
        }

        return changed;
    }

    private void syncColorWidgets(int color) {
        syncingWidgets = true;
        try {
            textRed.setValue((color & 0xFF0000) >> 16);
            textGreen.setValue((color & 0x00FF00) >> 8);
            textBlue.setValue(color & 0x0000FF);
            colorPalette.setColor(color);
        } finally {
            syncingWidgets = false;
        }
    }

    private void refreshInfoLabels() {
        List<FrontierOverlay> frontiers = MapFrontiersClient.getFrontiersInCollection(collectionId);
        float totalArea = 0.f;
        float totalPathLength = 0.f;
        for (FrontierOverlay frontier : frontiers) {
            if (frontier.getShape() == FrontierShape.Path) {
                totalPathLength += frontier.perimeter;
            } else {
                totalArea += frontier.area;
            }
        }

        MutableComponent owner = Component.translatable(OWNER_KEY, SettingsUserFormatter.getDisplayName(collection.getOwner()));
        if (collection.wasCopied()) {
            owner.append(Component.literal(ColorConstants.WARNING + " !"));
            ownerLabel.setTooltip(Tooltip.create(Component.literal(ColorConstants.WARNING + "! " + ChatFormatting.RESET)
                    .append(Component.translatable(ORIGINAL_OWNER_KEY, SettingsUserFormatter.getDisplayName(collection.getCopiedFromUser())))));
        } else {
            ownerLabel.setTooltip(null);
        }
        ownerLabel.setMessage(owner);
        typeLabel.setMessage(Component.translatable(TYPE_KEY, getCollectionTypeLabel()));
        frontiersCountLabel.setMessage(Component.translatable(FRONTIERS_COUNT_KEY, frontiers.size()));
        areaLabel.setMessage(Component.translatable(AREA_KEY, formatMeasurement(totalArea)));
        lengthLabel.setMessage(Component.translatable(LENGTH_KEY, formatMeasurement(totalPathLength)));
        if (createdLabel != null && collection.getCreated() != null) {
            createdLabel.setMessage(Component.translatable(CREATED_KEY, FrontierInfoPage.DATE_FORMAT.format(collection.getCreated())));
        }
        if (modifiedLabel != null && collection.getModified() != null) {
            modifiedLabel.setMessage(Component.translatable(MODIFIED_KEY, FrontierInfoPage.DATE_FORMAT.format(collection.getModified())));
        }
    }

    private void refreshViewState() {
        boolean editable = canUpdateCollection();
        textName.setEditable(editable);
        textRed.setEditable(editable);
        textGreen.setEditable(editable);
        textBlue.setEditable(editable);
        colorPicker.active = editable;
        colorPalette.active = editable;
        buttonRandomColor.active = editable;
        sliderCollectionViewZoom.active = editable;
        buttonDelete.active = canDeleteCollection();
        updatePasteOptionsVisibility(editable);
        updateUndoRedoVisibility(editable);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (GuiEventListener child : children()) {
            if (child instanceof ColorPicker picker) {
                picker.finishSelection();
            }
        }

        sliderCollectionViewZoom.mouseReleased();

        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.input() == GLFW.GLFW_KEY_Z && event.hasControlDown() && !event.hasShiftDown() && !event.hasAltDown()) {
            undo();
            return true;
        } else if (event.input() == GLFW.GLFW_KEY_Z && event.hasControlDown() && event.hasShiftDown() && !event.hasAltDown()) {
            redo();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (saveChangesOnClose && hasChanges() && canUpdateCollection()) {
            MapFrontiersClient.getOperationService().updateCollection(collection);
        }

        MapFrontiersClient.getCollectionEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        super.onClose();
    }

    private void applyExternalCollectionUpdate(CollectionData updatedCollection) {
        originalCollection.updateFromData(updatedCollection);
        collection.updateFromData(updatedCollection);
        undoStack.clear();
        redoStack.clear();
        undoStack.push(createMetadataSnapshot());

        syncingWidgets = true;
        try {
            textName.setValue(collection.getName());
            colorPicker.setColor(collection.getColor());
            syncColorWidgets(collection.getColor());
            sliderCollectionViewZoom.setValue(collection.getCollectionViewZoom());
        } finally {
            syncingWidgets = false;
        }

        refreshInfoLabels();
        refreshViewState();
    }

    private void undo() {
        if (undoStack.size() == 1) {
            return;
        }

        redoStack.push(undoStack.pop());
        applyEditableMetadata(undoStack.peek(), true, true, true);
        refreshViewState();
        if (minecraft.getLastInputType().isKeyboard()) {
            setInitialFocus(undoStack.size() == 1 ? buttonRedo : buttonUndo);
        }
    }

    private void redo() {
        if (redoStack.empty()) {
            return;
        }

        CollectionData snapshot = redoStack.pop();
        applyEditableMetadata(snapshot, true, true, true);
        undoStack.push(new CollectionData(snapshot));
        refreshViewState();
        if (minecraft.getLastInputType().isKeyboard()) {
            setInitialFocus(redoStack.empty() ? buttonUndo : buttonRedo);
        }
    }

    private void addCurrentStateToUndo() {
        CollectionData snapshot = createMetadataSnapshot();
        if (sameEditableMetadata(undoStack.peek(), snapshot)) {
            return;
        }

        undoStack.push(snapshot);
        redoStack.clear();
        refreshViewState();
    }

    private void updatePasteOptionsVisibility(boolean editable) {
        boolean hasClipboard = MapFrontiersClient.getCollectionClipboard() != null;
        buttonPaste.active = editable && hasClipboard;
        buttonPaste.visible = buttonPaste.active;
        buttonPasteOptions.active = editable && hasClipboard;
        buttonPasteOptions.visible = buttonPaste.visible;
        buttonPasteOptions.setType(ClientConfig.PASTE_OPTIONS_VISIBLE.get() ? IconButton.Type.CollapseOptions : IconButton.Type.ExpandOptions);
        buttonPasteOptions.setTooltip(ClientConfig.PASTE_OPTIONS_VISIBLE.get() ? CLOSE_PASTE_TOOLTIP : OPEN_PASTE_TOOLTIP);

        boolean optionsVisible = buttonPaste.visible && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        labelPasteName.visible = optionsVisible;
        buttonPasteName.visible = optionsVisible;
        labelPasteColor.visible = optionsVisible;
        buttonPasteColor.visible = optionsVisible;
        labelPasteCollectionViewZoom.visible = optionsVisible;
        buttonPasteCollectionViewZoom.visible = optionsVisible;
    }

    private void updateUndoRedoVisibility(boolean editable) {
        buttonUndo.active = editable && undoStack.size() > 1;
        buttonRedo.active = editable && !redoStack.empty();
        buttonUndo.visible = buttonUndo.active;
        buttonRedo.visible = buttonRedo.active;
    }

    private CollectionData createMetadataSnapshot() {
        CollectionData snapshot = new CollectionData(collection);
        snapshot.setName(collection.getName());
        snapshot.setColor(collection.getColor());
        snapshot.setCollectionViewZoom(collection.getCollectionViewZoom());
        return snapshot;
    }

    private CollectionData createClipboardSnapshot() {
        CollectionData snapshot = new CollectionData();
        snapshot.setName(collection.getName());
        snapshot.setColor(collection.getColor());
        snapshot.setCollectionViewZoom(collection.getCollectionViewZoom());
        return snapshot;
    }

    private boolean hasChanges() {
        return !sameEditableMetadata(originalCollection, collection);
    }

    private static boolean sameEditableMetadata(CollectionData first, CollectionData second) {
        return Objects.equals(first.getName(), second.getName())
                && first.getColor() == second.getColor()
                && first.getCollectionViewZoom() == second.getCollectionViewZoom();
    }

    private boolean canUpdateCollection() {
        if (minecraft.player == null) {
            return false;
        }

        SettingsUser playerUser = new SettingsUser(minecraft.player);
        if (collection.getPersonal()) {
            return canManageLocalPersonalCollection(playerUser);
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        return profile != null && (profile.updateFrontier == SettingsProfile.State.Enabled
                || (profile.updateFrontier == SettingsProfile.State.Owner && collection.getOwner().equals(playerUser)));
    }

    private boolean canDeleteCollection() {
        if (minecraft.player == null) {
            return false;
        }

        SettingsUser playerUser = new SettingsUser(minecraft.player);
        if (collection.getPersonal()) {
            return canManageLocalPersonalCollection(playerUser);
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        return profile != null && (profile.deleteFrontier == SettingsProfile.State.Enabled
                || (profile.deleteFrontier == SettingsProfile.State.Owner && collection.getOwner().equals(playerUser)));
    }

    private boolean canManageLocalPersonalCollection(SettingsUser playerUser) {
        return collection.getOwner().equals(playerUser);
    }

    private Component getCollectionTypeLabel() {
        Component baseType = collection.getPersonal() ? PERSONAL_LABEL : GLOBAL_LABEL;
        if (collection.isSessionOnly()) {
            return Component.translatable(TEMPORARY_KEY).append(Component.literal(" ")).append(baseType);
        }
        return baseType;
    }

    private void deleteCollection() {
        if (!canDeleteCollection()) {
            return;
        }

        saveChangesOnClose = false;
        MapFrontiersClient.getCollectionEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        MapFrontiersClient.getOperationService().deleteCollection(collection);
        super.onClose();
    }

    private static String formatMeasurement(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static Component formatCollectionViewZoomLabel(int zoom) {
        if (zoom == CollectionData.COLLECTION_VIEW_DISABLED_ZOOM) {
            return DISABLED_LABEL;
        }

        return Component.literal(Integer.toString(zoom));
    }
}
