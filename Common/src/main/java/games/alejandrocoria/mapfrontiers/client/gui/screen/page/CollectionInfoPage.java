package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorInputTabsWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPaletteWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPicker;
import games.alejandrocoria.mapfrontiers.client.gui.component.PluginSourceBadge;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleSlider;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.CollectionVisibilityDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteCollectionConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.territory.BannerDataHelper;
import games.alejandrocoria.mapfrontiers.client.territory.BannerRenderer;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionLocalOverrides;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.SettingsUserFormatter;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityMask;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.platform.Services;
import it.unimi.dsi.fastutil.Pair;
import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.common.Context;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import java.util.UUID;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class CollectionInfoPage extends PageScreen {
    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_collection_info");
    private static final Component NAME_LABEL = Component.translatable("mapfrontiers.name");
    private static final Component SELECT_IN_MAP_LABEL = Component.translatable("mapfrontiers.select_in_map");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final Component DELETE_LABEL = Component.translatable("mapfrontiers.delete");
    private static final Component ASSIGN_BANNER_LABEL = Component.translatable("mapfrontiers.assign_banner");
    private static final Component ASSIGN_BANNER_WARN_LABEL = ASSIGN_BANNER_LABEL.copy().append(Component.literal(ColorConstants.WARNING + " !"));
    private static final Component REMOVE_BANNER_LABEL = Component.translatable("mapfrontiers.remove_banner");
    private static final String BANNER_ROTATION_KEY = "mapfrontiers.banner_rotation";
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
    private static final Component VISIBILITY_LABEL = Component.translatable("mapfrontiers.visibility");
    private static final Component VISIBILITY_OVERRIDE_LABEL = Component.translatable("mapfrontiers.visibility_override");
    private static final Tooltip COPY_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.copy.tooltip"));
    private static final Tooltip PASTE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.paste.tooltip"));
    private static final Tooltip OPEN_PASTE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.open_paste_options.tooltip"));
    private static final Tooltip CLOSE_PASTE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.close_paste_options.tooltip"));
    private static final Tooltip UNDO_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.undo.tooltip"));
    private static final Tooltip REDO_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.redo.tooltip"));
    private static final Tooltip ASSIGN_BANNER_WARN_TOOLTIP = Tooltip.create(Component.literal(ColorConstants.WARNING + "! " + ChatFormatting.RESET)
            .append(Component.translatable("mapfrontiers.assign_banner_warn.tooltip")));
    private static final Tooltip VISIBILITY_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.collection_visibility.tooltip"));
    private static final Tooltip VISIBILITY_OVERRIDE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.collection_visibility_override.tooltip"));
    private static final Component PASTE_NAME_LABEL = Component.translatable("mapfrontiers.paste_name");
    private static final Component PASTE_COLOR_LABEL = Component.translatable("mapfrontiers.paste_color");
    private static final Component PASTE_BANNER_LABEL = Component.translatable("mapfrontiers.paste_banner");
    private static final Component PASTE_VISIBILITY_LABEL = Component.translatable("mapfrontiers.paste_visibility");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final int SECTION_WIDTH = 146;
    private static final int NAME_SECTION_WIDTH = SECTION_WIDTH * 2 + LayoutConstants.SPACING_MEDIUM;
    private static final int DEFAULT_TEXTBOX_HEIGHT = 17;
    private static final int CLIPBOARD_SPACER_WIDTH = SECTION_WIDTH - LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH;

    private final UUID collectionId;
    private final CollectionData originalCollection;
    private final CollectionData collection;
    private final Stack<CollectionData> undoStack = new Stack<>();
    private final Stack<CollectionData> redoStack = new Stack<>();
    private final BannerRenderer bannerRenderer = new BannerRenderer();
    private boolean saveChangesOnClose = true;
    private boolean syncingWidgets = false;

    private TextBox textName;
    private SimpleButton buttonVisibility;
    private SimpleButton buttonVisibilityOverride;
    private SimpleButton buttonBanner;
    private SimpleSlider sliderBannerRotation;
    private ColorInputTabsWidget colorInputs;
    private ColorPicker colorPicker;
    private ColorPaletteWidget colorPalette;
    private IconButton buttonCopy;
    private IconButton buttonPaste;
    private IconButton buttonPasteOptions;
    private IconButton buttonUndo;
    private IconButton buttonRedo;
    private OptionButton buttonPasteName;
    private OptionButton buttonPasteColor;
    private OptionButton buttonPasteVisibility;
    private OptionButton buttonPasteBanner;
    private SimpleButton buttonSelect;
    private SimpleButton buttonDelete;
    private SimpleButton buttonDone;
    private StringWidget labelPasteName;
    private StringWidget labelPasteColor;
    private StringWidget labelPasteVisibility;
    private StringWidget labelPasteBanner;
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
        syncBannerRenderer();
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

        buildBannerSection(mainLayout);
        buildOverviewSection(mainLayout);
        buildInfoSection(mainLayout);
        buildColorSection(mainLayout);
        buildClipboardSection(mainLayout);
        buildBottomButtons();

        refreshInfoLabels();
        refreshViewState();
        setInitialFocus(buttonDone);
    }

    private void buildBannerSection(GridLayout mainLayout) {
        LinearLayout bannerColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        bannerColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(bannerColumn, 0, 0);

        buttonBanner = new SimpleButton(font, SECTION_WIDTH, ASSIGN_BANNER_LABEL, b -> onBannerButtonPressed());
        bannerColumn.addChild(buttonBanner);

        sliderBannerRotation = new SimpleSlider(font, SECTION_WIDTH, BANNER_ROTATION_KEY, 0, 360, collection.getBannerRotation(), this::onBannerRotationChanged);
        bannerColumn.addChild(sliderBannerRotation);
    }

    private void buildOverviewSection(GridLayout mainLayout) {
        LinearLayout overviewColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        overviewColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(overviewColumn, 0, 1, 1, 2);

        LinearLayout headerRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_TINY);
        headerRow.addChild(new StringWidget(NAME_LABEL, font).setColor(ColorConstants.COLLECTION_INFO_TEXT));
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

        LinearLayout visibilityRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_MEDIUM);
        visibilityRow.defaultCellSetting().alignVerticallyMiddle();
        overviewColumn.addChild(visibilityRow);

        buttonVisibility = new SimpleButton(font, SECTION_WIDTH, VISIBILITY_LABEL, b -> onVisibilityButtonPressed());
        buttonVisibility.setTooltip(VISIBILITY_TOOLTIP);
        visibilityRow.addChild(buttonVisibility);

        buttonVisibilityOverride = new SimpleButton(font, SECTION_WIDTH, VISIBILITY_OVERRIDE_LABEL,
                b -> onVisibilityOverrideButtonPressed());
        buttonVisibilityOverride.setTooltip(VISIBILITY_OVERRIDE_TOOLTIP);
        visibilityRow.addChild(buttonVisibilityOverride);
    }

    private void buildInfoSection(GridLayout mainLayout) {
        LinearLayout infoColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_TINY);
        mainLayout.addChild(infoColumn, 0, 3, LayoutSettings.defaults().alignHorizontallyLeft());

        ownerLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.COLLECTION_INFO_TEXT));
        typeLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.COLLECTION_INFO_TEXT));
        frontiersCountLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.COLLECTION_INFO_TEXT));
        areaLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.COLLECTION_INFO_TEXT));
        lengthLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.COLLECTION_INFO_TEXT));

        if (collection.getCreated() != null) {
            createdLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.COLLECTION_INFO_TEXT));
        }
        if (collection.getModified() != null) {
            modifiedLabel = infoColumn.addChild(new StringWidget(Component.empty(), font).setColor(ColorConstants.COLLECTION_INFO_TEXT));
        }
    }

    private void buildColorSection(GridLayout mainLayout) {
        colorPicker = new ColorPicker(collection.getColor(), this::onColorPicked);
        mainLayout.addChild(colorPicker, 1, 1, LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter());

        LinearLayout colorColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        colorColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(colorColumn, 1, 2, LayoutSettings.defaults().alignVerticallyBottom());

        colorInputs = new ColorInputTabsWidget(font, collection.getColor(), color -> applyColorChange(color, true));
        colorColumn.addChild(colorInputs);

        colorPalette = new ColorPaletteWidget(collection.getColor(), color -> applyColorChange(color, true));
        colorColumn.addChild(colorPalette);

        syncColorWidgets(collection.getColor());
    }

    private void buildClipboardSection(GridLayout mainLayout) {
        GridLayout editColumn = new GridLayout().rowSpacing(LayoutConstants.SPACING_SMALL);
        editColumn.defaultCellSetting().alignHorizontallyLeft();
        editColumn.addChild(SpacerElement.width(CLIPBOARD_SPACER_WIDTH), 0, 0);
        mainLayout.addChild(editColumn, 1, 3, LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyLeft());

        labelPasteName = editColumn.addChild(new StringWidget(PASTE_NAME_LABEL, font).setColor(ColorConstants.TEXT), 0, 0);
        buttonPasteName = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_NAME.get(), ClientConfig.PASTE_NAME::set), 0, 1);

        labelPasteColor = editColumn.addChild(new StringWidget(PASTE_COLOR_LABEL, font).setColor(ColorConstants.TEXT), 1, 0);
        buttonPasteColor = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_COLOR.get(), ClientConfig.PASTE_COLOR::set), 1, 1);

        labelPasteVisibility = editColumn.addChild(new StringWidget(PASTE_VISIBILITY_LABEL, font).setColor(ColorConstants.TEXT), 2, 0);
        buttonPasteVisibility = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_VISIBILITY.get(), ClientConfig.PASTE_VISIBILITY::set), 2, 1);

        labelPasteBanner = editColumn.addChild(new StringWidget(PASTE_BANNER_LABEL, font).setColor(ColorConstants.TEXT), 3, 0);
        buttonPasteBanner = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_BANNER.get(), ClientConfig.PASTE_BANNER::set), 3, 1);

        LinearLayout editButtons = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        editColumn.addChild(editButtons, 4, 0);

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
        buttonSelect = addBottomButton(new SimpleButton(font, SECTION_WIDTH, SELECT_IN_MAP_LABEL, b -> onSelectInMapPressed()));
        buttonDelete = addBottomButton(new SimpleButton(font, SECTION_WIDTH, DELETE_LABEL, b -> onDeletePressed()));
        buttonDelete.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_NORMAL, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonDone = addBottomButton(new SimpleButton(font, SECTION_WIDTH, DONE_LABEL, b -> onClose()));
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
                ClientConfig.PASTE_BANNER.get(),
                ClientConfig.PASTE_VISIBILITY.get());
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
        new DeleteCollectionConfirmationDialog(collection, ClientConfig.ASK_CONFIRMATION_COLLECTION_DELETE,
                response -> deleteCollection()).display();
    }

    private void onSelectInMapPressed() {
        BlockPos center = getCollectionCenterInCurrentFullscreenDimension();
        if (center == null) {
            return;
        }

        closeAndReturnToFullscreenMap();
        Services.JOURNEYMAP.fullscreenMapCenterOn(center.getX(), center.getZ());
    }

    private void onBannerButtonPressed() {
        if (!collection.hasBanner()) {
            ItemStack heldBanner = getHeldBanner(minecraft);
            if (heldBanner == null) {
                return;
            }
            collection.setBannerData(BannerDataHelper.fromBannerItem(heldBanner));
        } else {
            collection.setBannerData(null);
        }

        syncingWidgets = true;
        try {
            sliderBannerRotation.setValue(collection.getBannerRotation());
        } finally {
            syncingWidgets = false;
        }
        syncBannerRenderer();
        updateBannerButton();
        addCurrentStateToUndo();
    }

    private void onBannerRotationChanged(int angle, boolean dragging) {
        if (syncingWidgets || collection.getBannerRotation() == angle) {
            return;
        }

        collection.setBannerRotation(angle);
        bannerRenderer.setRotation(angle);
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

    private boolean applyEditableMetadata(CollectionData source, boolean pasteName, boolean pasteColor, boolean pasteBanner,
                                         boolean pasteVisibility) {
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

            if (pasteBanner && !Objects.equals(collection.getBannerData(), source.getBannerData())) {
                collection.setBannerData(source.getBannerData());
                syncBannerRenderer();
                sliderBannerRotation.setValue(collection.getBannerRotation());
                changed = true;
            }

            if (pasteVisibility && !collection.getVisibilityData().equals(source.getVisibilityData())) {
                collection.setVisibilityData(source.getVisibilityData());
                changed = true;
            }
        } finally {
            syncingWidgets = false;
        }

        updateBannerButton();
        return changed;
    }

    private void syncColorWidgets(int color) {
        syncingWidgets = true;
        try {
            colorInputs.setColor(color);
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
        colorInputs.setEditable(editable);
        colorPicker.active = editable;
        colorPalette.active = editable;
        buttonVisibility.active = editable;
        buttonVisibilityOverride.active = true;
        buttonBanner.active = editable;
        sliderBannerRotation.active = editable;
        buttonSelect.active = getCollectionCenterInCurrentFullscreenDimension() != null;
        buttonDelete.active = canDeleteCollection();
        updateBannerButton();
        updatePasteOptionsVisibility(editable);
        refreshUndoRedoState(editable);
        repositionElements();
    }

    @Override
    protected void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderScaledBackgroundScreen(graphics, mouseX, mouseY, partialTicks);
        if (colorInputs != null) {
            colorInputs.renderTabbedBoxBackground(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected void renderScaledScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (bannerRenderer.hasBanner()) {
            bannerRenderer.renderBanner(graphics, buttonBanner.getX() + buttonBanner.getWidth() / 2, sliderBannerRotation.getY() + 25, 3);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (GuiEventListener child : children()) {
            if (child instanceof ColorPicker picker) {
                picker.finishSelection();
            }
        }

        sliderBannerRotation.mouseReleased();

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
            MapFrontiersClient.getOperationService().submitCollectionUpdate(collection);
        }

        bannerRenderer.releaseTexture();
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
            syncBannerRenderer();
            sliderBannerRotation.setValue(collection.getBannerRotation());
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
        applyEditableMetadata(undoStack.peek(), true, true, true, true);
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
        applyEditableMetadata(snapshot, true, true, true, true);
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
        boolean canPaste = editable && hasClipboard;
        buttonPaste.active = canPaste;
        buttonPasteOptions.active = canPaste;
        buttonPasteOptions.setType(ClientConfig.PASTE_OPTIONS_VISIBLE.get() ? IconButton.Type.CollapseOptions : IconButton.Type.ExpandOptions);
        buttonPasteOptions.setTooltip(ClientConfig.PASTE_OPTIONS_VISIBLE.get() ? CLOSE_PASTE_TOOLTIP : OPEN_PASTE_TOOLTIP);

        boolean optionsVisible = canPaste && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        labelPasteName.visible = optionsVisible;
        buttonPasteName.visible = optionsVisible;
        labelPasteColor.visible = optionsVisible;
        buttonPasteColor.visible = optionsVisible;
        labelPasteVisibility.visible = optionsVisible;
        buttonPasteVisibility.visible = optionsVisible;
        labelPasteBanner.visible = optionsVisible;
        buttonPasteBanner.visible = optionsVisible;
    }

    private void refreshUndoRedoState(boolean editable) {
        buttonUndo.active = editable && undoStack.size() > 1;
        buttonRedo.active = editable && !redoStack.empty();
    }

    private CollectionData createMetadataSnapshot() {
        CollectionData snapshot = new CollectionData(collection);
        snapshot.setName(collection.getName());
        snapshot.setColor(collection.getColor());
        snapshot.setBannerData(collection.getBannerData());
        snapshot.setVisibilityData(collection.getVisibilityData());
        return snapshot;
    }

    private CollectionData createClipboardSnapshot() {
        CollectionData snapshot = new CollectionData();
        snapshot.setName(collection.getName());
        snapshot.setColor(collection.getColor());
        snapshot.setBannerData(collection.getBannerData());
        snapshot.setVisibilityData(collection.getVisibilityData());
        return snapshot;
    }

    private boolean hasChanges() {
        return !sameEditableMetadata(originalCollection, collection);
    }

    private static boolean sameEditableMetadata(CollectionData first, CollectionData second) {
        return Objects.equals(first.getName(), second.getName())
                && first.getColor() == second.getColor()
                && Objects.equals(first.getBannerData(), second.getBannerData())
                && first.getVisibilityData().equals(second.getVisibilityData());
    }

    private void onVisibilityButtonPressed() {
        CollectionVisibilityData baseVisibilityData = collection.getVisibilityData();
        new CollectionVisibilityDialog(baseVisibilityData, ClientConfig.getDefaultCollectionVisibility(), (newVisibilityData, newVisibilityMask) -> {
            if (newVisibilityData.equals(baseVisibilityData)) {
                return;
            }
            if (newVisibilityData.equals(collection.getVisibilityData())) {
                return;
            }

            collection.setVisibilityData(newVisibilityData);
            addCurrentStateToUndo();
            refreshViewState();
        }).display();
    }

    private void onVisibilityOverrideButtonPressed() {
        Pair<CollectionVisibilityData, CollectionVisibilityMask> override =
                MapFrontiersClient.getCollectionLocalOverrides().getVisibility(collectionId);
        CollectionVisibilityData baseVisibilityData = CollectionLocalOverrides.resolveVisibility(collection.getVisibilityData(), override);
        CollectionVisibilityMask initialVisibilityMask = new CollectionVisibilityMask(override.second());
        CollectionVisibilityData initialVisibilityData = baseVisibilityData.normalized(initialVisibilityMask);
        new CollectionVisibilityDialog(baseVisibilityData, override.second(), (newVisibilityData, newVisibilityMask) -> {
            if (!newVisibilityData.equals(initialVisibilityData) || !newVisibilityMask.equals(initialVisibilityMask)) {
                Pair<CollectionVisibilityData, CollectionVisibilityMask> newOverride =
                        Pair.of(new CollectionVisibilityData(newVisibilityData), new CollectionVisibilityMask(newVisibilityMask));
                MapFrontiersClient.getCollectionLocalOverrides().setVisibility(collectionId, newOverride);
                MapFrontiersClient.refreshCollectionVisibilityOverride(collectionId);
            }
        }).display();
    }

    private boolean canUpdateCollection() {
        if (minecraft.player == null) {
            return false;
        }

        return SettingsProfile.canUpdateCollection(MapFrontiersClient.getSettingsProfile(), collection, new SettingsUser(minecraft.player));
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
        bannerRenderer.releaseTexture();
        MapFrontiersClient.getCollectionEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        MapFrontiersClient.getOperationService().deleteCollection(collection);
        super.onClose();
    }

    private void updateBannerButton() {
        if (!collection.hasBanner()) {
            if (getHeldBanner(minecraft) != null) {
                buttonBanner.setMessage(ASSIGN_BANNER_LABEL);
                buttonBanner.setTooltip(null);
            } else {
                buttonBanner.setMessage(ASSIGN_BANNER_WARN_LABEL);
                buttonBanner.setTooltip(ASSIGN_BANNER_WARN_TOOLTIP);
            }
            sliderBannerRotation.visible = false;
        } else {
            buttonBanner.setMessage(REMOVE_BANNER_LABEL);
            buttonBanner.setTooltip(null);
            sliderBannerRotation.visible = true;
        }
    }

    private void syncBannerRenderer() {
        bannerRenderer.releaseTexture();
        if (collection.getBannerData() != null) {
            bannerRenderer.createTexture(collection.getId(), collection.getBannerData());
        }
    }

    private static @Nullable ItemStack getHeldBanner(@Nullable Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return null;
        }

        ItemStack mainhand = minecraft.player.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offhand = minecraft.player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (mainhand.getItem() instanceof BannerItem) {
            return mainhand;
        }
        if (offhand.getItem() instanceof BannerItem) {
            return offhand;
        }

        return null;
    }

    private static String formatMeasurement(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private @Nullable BlockPos getCollectionCenterInCurrentFullscreenDimension() {
        var jmApi = MapFrontiersClient.getJmAPI();
        if (jmApi == null) {
            return null;
        }

        UIState uiState = jmApi.getUIState(Context.UI.Fullscreen);
        if (uiState == null) {
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean hasAreaFrontier = false;

        for (FrontierOverlay frontier : MapFrontiersClient.getFrontiersInCollection(collectionId, uiState.dimension)) {
            if (frontier.getShape() == FrontierShape.Path) {
                continue;
            }

            hasAreaFrontier = true;
            minX = Math.min(minX, frontier.topLeft.getX());
            minZ = Math.min(minZ, frontier.topLeft.getZ());
            maxX = Math.max(maxX, frontier.bottomRight.getX());
            maxZ = Math.max(maxZ, frontier.bottomRight.getZ());
        }

        if (!hasAreaFrontier) {
            return null;
        }

        return new BlockPos((minX + maxX) / 2, 70, (minZ + maxZ) / 2);
    }

}
