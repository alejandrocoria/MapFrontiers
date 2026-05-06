package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPaletteWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPicker;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleSlider;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.DeleteFrontierConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.PathStyleDialog;
import games.alejandrocoria.mapfrontiers.client.gui.screen.dialog.VisibilityDialog;
import games.alejandrocoria.mapfrontiers.client.gui.util.SourcePluginUiHelper;
import games.alejandrocoria.mapfrontiers.client.util.SettingsUserFormatter;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.platform.Services;
import it.unimi.dsi.fastutil.Pair;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.util.UIState;
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
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

@ParametersAreNonnullByDefault
public class FrontierInfoPage extends PageScreen
{
    static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    private static final Component TITLE_LABEL = Component.translatable("mapfrontiers.title_info");
    private static final Component ASSIGN_BANNER_LABEL = Component.translatable("mapfrontiers.assign_banner");
    private static final Component ASSIGN_BANNER_WARN_LABEL = ASSIGN_BANNER_LABEL.copy().append(Component.literal(ColorConstants.WARNING + " !"));
    private static final Component REMOVE_BANNER_LABEL = Component.translatable("mapfrontiers.remove_banner");
    private static final String BANNER_ROTATION_KEY = "mapfrontiers.banner_rotation";
    private static final Component NAME_LABEL = Component.translatable("mapfrontiers.name");
    private static final Component PERSONAL_LABEL = Component.translatable("mapfrontiers.personal_type");
    private static final Component GLOBAL_LABEL = Component.translatable("mapfrontiers.global_type");
    private static final String VERTICES_KEY = "mapfrontiers.vertices";
    private static final String CHUNKS_KEY = "mapfrontiers.chunks";
    private static final String POINTS_KEY = "mapfrontiers.points";
    private static final String OWNER_KEY = "mapfrontiers.owner";
    private static final String COLLECTION_KEY = "mapfrontiers.collection";
    private static final String ORIGINAL_OWNER_KEY = "mapfrontiers.original_owner";
    private static final String DIMENSION_KEY = "mapfrontiers.dimension";
    private static final String TEMPORARY_KEY = "mapfrontiers.temporary";
    private static final String AREA_KEY = "mapfrontiers.area";
    private static final String LENGTH_KEY = "mapfrontiers.length";
    private static final String PERIMETER_KEY = "mapfrontiers.perimeter";
    private static final String CREATED_KEY = "mapfrontiers.created";
    private static final String MODIFIED_KEY = "mapfrontiers.modified";
    private static final Component VISIBILITY_LABEL = Component.translatable("mapfrontiers.visibility");
    private static final Component VISIBILITY_OVERRIDE_LABEL = Component.translatable("mapfrontiers.visibility_override");
    private static final Component PATH_STYLE_LABEL = Component.translatable("mapfrontiers.path_style");
    private static final Component R_LABEL = Component.literal("R");
    private static final Component G_LABEL = Component.literal("G");
    private static final Component B_LABEL = Component.literal("B");
    private static final Component RANDOM_COLOR_LABEL = Component.translatable("mapfrontiers.random_color");
    private static final Component PASTE_NAME_LABEL = Component.translatable("mapfrontiers.paste_name");
    private static final Component PASTE_VISIBILITY_LABEL = Component.translatable("mapfrontiers.paste_visibility");
    private static final Component PASTE_PATH_STYLE_LABEL = Component.translatable("mapfrontiers.paste_path_style");
    private static final Component PASTE_COLOR_LABEL = Component.translatable("mapfrontiers.paste_color");
    private static final Component PASTE_BANNER_LABEL = Component.translatable("mapfrontiers.paste_banner");
    private static final Component SELECT_IN_MAP_LABEL = Component.translatable("mapfrontiers.select_in_map");
    private static final Component SHARE_SETTINGS_LABEL = Component.translatable("mapfrontiers.share_settings");
    private static final Component SEND_LABEL = Component.translatable("mapfrontiers.send");
    private static final Component DELETE_LABEL = Component.translatable("mapfrontiers.delete");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");

    private static final Tooltip VISIBILITY_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.visibility.tooltip"));
    private static final Tooltip VISIBILITY_OVERRIDE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.visibility_override.tooltip"));
    private static final Tooltip COPY_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.copy"));
    private static final Tooltip PASTE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.paste"));
    private static final Tooltip OPEN_PASTE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.open_paste_options"));
    private static final Tooltip CLOSE_PASTE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.close_paste_options"));
    private static final Tooltip UNDO_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.undo"));
    private static final Tooltip REDO_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.redo"));
    private static final Tooltip CHANGE_TO_PERSONAL_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.change_to_personal"));
    private static final Tooltip CHANGE_TO_GLOBAL_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.change_to_global"));
    private static final Tooltip ASSIGN_BANNER_WARN_TOOLTIP = Tooltip.create(Component.literal(ColorConstants.WARNING + "! " + ChatFormatting.RESET).append(Component.translatable("mapfrontiers.assign_banner_warn")));
    private static final int MAIN_LAYOUT_SPACING = LayoutConstants.SPACING_MEDIUM;
    private static final int SECTION_WIDTH = 146;
    private static final int NAME_SECTION_WIDTH = SECTION_WIDTH * 2 + MAIN_LAYOUT_SPACING;
    private static final int DEFAULT_TEXTBOX_HEIGHT = 17;
    private static final int RGB_INLINE_SPACING = 3;
    private static final int RGB_LABEL_HEIGHT = 8;
    private static final int RGB_TEXTBOX_WIDTH = 33;
    private static final int RGB_ROW_SPACER_WIDTH = 4;
    private static final int CLIPBOARD_SPACER_WIDTH = SECTION_WIDTH - LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH;

    private final IClientAPI jmAPI;

    private final FrontierOverlay frontier;
    private final boolean hasPathStyle;
    private int frontierHash;
    private TextBox textName1;
    private TextBox textName2;
    private SimpleButton buttonVisibility;
    private SimpleButton buttonVisibilityOverride;
    private @Nullable SimpleButton buttonPathStyle;
    private TextBoxInt textRed;
    private TextBoxInt textGreen;
    private TextBoxInt textBlue;
    private SimpleButton buttonRandomColor;
    private ColorPicker colorPicker;
    private ColorPaletteWidget colorPalette;
    private IconButton buttonCopy;
    private IconButton buttonPaste;
    private IconButton buttonPasteOptions;
    private OptionButton buttonPasteName;
    private OptionButton buttonPasteVisibility;
    private @Nullable OptionButton buttonPastePathStyle;
    private OptionButton buttonPasteColor;
    private OptionButton buttonPasteBanner;
    private StringWidget labelPasteName;
    private StringWidget labelPasteVisibility;
    private @Nullable StringWidget labelPastePathStyle;
    private StringWidget labelPasteColor;
    private StringWidget labelPasteBanner;
    private IconButton buttonUndo;
    private IconButton buttonRedo;
    private IconButton buttonChangeToPersonalGlobal;

    private SimpleButton buttonSelect;
    private SimpleButton buttonShareSettings;
    private SimpleButton buttonDelete;
    private SimpleButton buttonDone;
    private SimpleButton buttonBanner;
    private SimpleSlider sliderBannerRotation;

    private StringWidget modifiedLabel;

    private final Stack<FrontierData> undoStack = new Stack<>();
    private final Stack<FrontierData> redoStack = new Stack<>();

    private boolean saveChangesOnClose = true;

    public FrontierInfoPage(IClientAPI jmAPI, FrontierOverlay frontier) {
        super(TITLE_LABEL);
        this.jmAPI = jmAPI;
        this.frontier = frontier;
        hasPathStyle = frontier.getMode() == FrontierData.Mode.Path;
        frontierHash = frontier.getHash();
        undoStack.push(new FrontierData(frontier));

        MapFrontiersClient.getFrontierEvents().subscribeDeleted(this, frontierID -> {
            if (frontier.getId().equals(frontierID)) {
                saveChangesOnClose = false;
                onClose();
            }
        });

        MapFrontiersClient.getFrontierEvents().subscribeUpdated(this, (frontierOverlay, playerID) -> {
            if (minecraft.player != null && frontier.getId().equals(frontierOverlay.getId())) {
                addToUndo(new FrontierData(frontierOverlay));
                if (playerID != minecraft.player.getId()) {
                    rebuildWidgets();
                    repositionElements();
                } else {
                    if (frontier.getModified() != null) {
                        Component modified = Component.translatable("mapfrontiers.modified", DATE_FORMAT.format(frontier.getModified()));
                        modifiedLabel.setMessage(modified);
                    }
                }
                frontierHash = frontier.getHash();
            }
        });

        MapFrontiersClient.getSettingsProfileEvents().subscribeUpdated(this, profile -> {
            updateButtons();
            updateBannerButton();
        });
    }

    @Override
    protected void initScreen() {
        GridLayout mainLayout = createMainLayout();

        buildBannerSection(mainLayout);
        buildOverviewSection(mainLayout);
        buildInfoSection(mainLayout);
        buildColorSection(mainLayout);
        buildClipboardSection(mainLayout);

        buildBottomButtons();

        refreshViewState();
        setInitialFocus(buttonDone);
    }

    private GridLayout createMainLayout() {
        GridLayout mainLayout = new GridLayout().spacing(MAIN_LAYOUT_SPACING);
        content.addChild(mainLayout);
        return mainLayout;
    }

    private void buildBannerSection(GridLayout mainLayout) {
        LinearLayout bannerColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        bannerColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(bannerColumn, 0, 0);

        buttonBanner = new SimpleButton(font, SECTION_WIDTH, ASSIGN_BANNER_LABEL, b -> onBannerButtonPressed());
        bannerColumn.addChild(buttonBanner);

        sliderBannerRotation = new SimpleSlider(font, SECTION_WIDTH, BANNER_ROTATION_KEY, 0, 360, frontier.getBannerRotation(), this::onBannerRotationChanged);
        bannerColumn.addChild(sliderBannerRotation);
    }

    private void buildOverviewSection(GridLayout mainLayout) {
        LinearLayout nameColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        nameColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(nameColumn, 0, 1, 1, 2);

        LinearLayout headerRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_TINY);
        headerRow.addChild(new StringWidget(NAME_LABEL, font).setColor(ColorConstants.WHITE));
        SourcePluginUiHelper.SourcePluginDisplay sourcePluginDisplay = SourcePluginUiHelper.createDisplay(
                font,
                frontier.getSourcePluginId(),
                NAME_SECTION_WIDTH - font.width(NAME_LABEL.getVisualOrderText()) - LayoutConstants.SPACING_TINY * 2
        );
        if (sourcePluginDisplay != null) {
            int sourceWidth = font.width(sourcePluginDisplay.text().getVisualOrderText());
            headerRow.addChild(SpacerElement.width(Math.max(0, NAME_SECTION_WIDTH - font.width(NAME_LABEL.getVisualOrderText()) - sourceWidth - LayoutConstants.SPACING_TINY * 2)));
            StringWidget sourceWidget = new StringWidget(sourcePluginDisplay.text(), font).setColor(ColorConstants.TEXT);
            sourceWidget.setTooltip(sourcePluginDisplay.tooltip());
            headerRow.addChild(sourceWidget);
        }
        nameColumn.addChild(headerRow);

        textName1 = createNameTextBox(frontier.getName1(), value -> {
            if (!frontier.getName1().equals(value)) {
                frontier.setName1(value);
            }
        });
        nameColumn.addChild(textName1);

        textName2 = createNameTextBox(frontier.getName2(), value -> {
            if (!frontier.getName2().equals(value)) {
                frontier.setName2(value);
            }
        });
        nameColumn.addChild(textName2);

        LinearLayout collectionInfoColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_TINY);
        nameColumn.addChild(collectionInfoColumn);

        LinearLayout collectionHeaderRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_TINY);
        collectionHeaderRow.defaultCellSetting().alignVerticallyMiddle();
        collectionInfoColumn.addChild(collectionHeaderRow);

        Component dimension = Component.translatable(DIMENSION_KEY, frontier.getDimension().identifier().toString());
        int dimensionWidth = font.width(dimension.getVisualOrderText());
        boolean hasCollection = frontier.hasCollection();
        if (hasCollection) {
            Component collectionLabel = Component.translatable(COLLECTION_KEY);
            collectionHeaderRow.addChild(new StringWidget(collectionLabel, font).setColor(ColorConstants.WHITE));
            int collectionLabelWidth = font.width(collectionLabel.getVisualOrderText());
            collectionHeaderRow.addChild(SpacerElement.width(Math.max(0,
                    NAME_SECTION_WIDTH - collectionLabelWidth - dimensionWidth - LayoutConstants.SPACING_TINY * 2)));
        } else {
            collectionHeaderRow.addChild(SpacerElement.width(Math.max(0, NAME_SECTION_WIDTH - dimensionWidth - LayoutConstants.SPACING_TINY)));
        }
        collectionHeaderRow.addChild(new StringWidget(dimension, font).setColor(ColorConstants.TEXT_DIMENSION));

        if (hasCollection) {
            Component collectionName = Component.empty();
            CollectionData collection = MapFrontiersClient.getCollection(frontier.getCollectionId());
            if (collection != null) {
                collectionName = StringUtil.isBlank(collection.getName())
                        ? Component.translatable("mapfrontiers.unnamed", ChatFormatting.ITALIC)
                        : Component.literal(collection.getName());
            }
            collectionInfoColumn.addChild(new StringWidget(collectionName, font).setColor(ColorConstants.WHITE));
        }

        LinearLayout visibilityRow = LinearLayout.horizontal().spacing(MAIN_LAYOUT_SPACING);
        visibilityRow.defaultCellSetting().alignVerticallyMiddle();
        nameColumn.addChild(visibilityRow);

        buttonVisibility = new SimpleButton(font, SECTION_WIDTH, VISIBILITY_LABEL, b -> onVisibilityButtonPressed());
        buttonVisibility.setTooltip(VISIBILITY_TOOLTIP);
        visibilityRow.addChild(buttonVisibility);

        buttonVisibilityOverride = new SimpleButton(font, SECTION_WIDTH, VISIBILITY_OVERRIDE_LABEL, b -> onVisibilityOverrideButtonPressed());
        buttonVisibilityOverride.setTooltip(VISIBILITY_OVERRIDE_TOOLTIP);
        visibilityRow.addChild(buttonVisibilityOverride);

        if (hasPathStyle) {
            LinearLayout pathStyleRow = LinearLayout.horizontal().spacing(MAIN_LAYOUT_SPACING);
            pathStyleRow.defaultCellSetting().alignVerticallyMiddle();
            nameColumn.addChild(pathStyleRow);

            buttonPathStyle = new SimpleButton(font, SECTION_WIDTH, PATH_STYLE_LABEL, b -> onPathStyleButtonPressed());
            pathStyleRow.addChild(buttonPathStyle);
            pathStyleRow.addChild(SpacerElement.width(SECTION_WIDTH));
        }
    }

    private TextBox createNameTextBox(String initialValue, Consumer<String> setter) {
        TextBox textBox = new TextBox(font, NAME_SECTION_WIDTH);
        textBox.setMaxLength(FrontierData.MAX_NAME_CHARACTERS);
        textBox.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textBox.setValue(initialValue);
        textBox.setLostFocusCallback(value -> sendNameChangeToServer());
        textBox.setValueChangedCallback(setter);
        return textBox;
    }

    private void buildInfoSection(GridLayout mainLayout) {
        LinearLayout infoColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_TINY);
        mainLayout.addChild(infoColumn, 0, 3, 1, 1, LayoutSettings.defaults().alignHorizontallyLeft());

        MutableComponent owner = Component.translatable(OWNER_KEY, SettingsUserFormatter.getDisplayName(frontier.getOwner()));
        if (frontier.wasCopied()) {
            owner.append(Component.literal(ColorConstants.WARNING + " !"));
        }
        StringWidget ownerWidget = infoColumn.addChild(new StringWidget(owner, font).setColor(ColorConstants.WHITE));
        if (frontier.wasCopied()) {
            Tooltip ownerTooltip = Tooltip.create(Component.literal(ColorConstants.WARNING + "! " + ChatFormatting.RESET)
                    .append(Component.translatable(ORIGINAL_OWNER_KEY, SettingsUserFormatter.getDisplayName(frontier.getCopiedFromUser()))));
            ownerWidget.setTooltip(ownerTooltip);
        }

        LinearLayout identityRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        infoColumn.addChild(identityRow);

        identityRow.addChild(new StringWidget(getFrontierTypeLabel(), font).setColor(ColorConstants.WHITE));
        buttonChangeToPersonalGlobal = identityRow.addChild(new IconButton(IconButton.Type.Swap, b -> onChangePersonalGlobalPressed()));
        buttonChangeToPersonalGlobal.setTooltip(frontier.getPersonal() ? CHANGE_TO_GLOBAL_TOOLTIP : CHANGE_TO_PERSONAL_TOOLTIP);

        Component shapeSummary = switch (frontier.getMode()) {
            case Vertex -> Component.translatable(VERTICES_KEY, frontier.getVertexCount());
            case Chunk -> Component.translatable(CHUNKS_KEY, frontier.getChunkCount());
            case Path -> Component.translatable(POINTS_KEY, frontier.getPointCount());
        };
        infoColumn.addChild(new StringWidget(shapeSummary, font).setColor(ColorConstants.WHITE));

        if (frontier.getMode() != FrontierData.Mode.Path) {
            infoColumn.addChild(new StringWidget(Component.translatable(AREA_KEY, formatMeasurement(frontier.area)), font).setColor(ColorConstants.WHITE));
            infoColumn.addChild(new StringWidget(Component.translatable(PERIMETER_KEY, formatMeasurement(frontier.perimeter)), font).setColor(ColorConstants.WHITE));
        } else {
            infoColumn.addChild(new StringWidget(Component.translatable(LENGTH_KEY, formatMeasurement(frontier.perimeter)), font).setColor(ColorConstants.WHITE));
        }

        if (frontier.getCreated() != null) {
            infoColumn.addChild(new StringWidget(Component.translatable(CREATED_KEY, DATE_FORMAT.format(frontier.getCreated())), font).setColor(ColorConstants.WHITE));
        }

        if (frontier.getModified() != null) {
            modifiedLabel = infoColumn.addChild(new StringWidget(Component.translatable(MODIFIED_KEY, DATE_FORMAT.format(frontier.getModified())), font).setColor(ColorConstants.WHITE));
        }
    }

    private void buildColorSection(GridLayout mainLayout) {
        colorPicker = new ColorPicker(frontier.getColor(), this::onColorPicked);
        mainLayout.addChild(colorPicker, 1, 1, LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter());

        LinearLayout colorColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        colorColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(colorColumn, 1, 2, LayoutSettings.defaults().alignVerticallyBottom());

        LinearLayout rgbRow = LinearLayout.horizontal().spacing(RGB_INLINE_SPACING);
        rgbRow.defaultCellSetting().alignVerticallyMiddle();
        colorColumn.addChild(rgbRow);

        rgbRow.addChild(new StringWidget(R_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_R));
        textRed = createRgbTextBox(value -> (frontier.getColor() & 0xFF00FFFF) | (value << 16));
        rgbRow.addChild(textRed);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(G_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_G));
        textGreen = createRgbTextBox(value -> (frontier.getColor() & 0xFFFF00FF) | (value << 8));
        rgbRow.addChild(textGreen);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(B_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_B));
        textBlue = createRgbTextBox(value -> (frontier.getColor() & 0xFFFFFF00) | value);
        rgbRow.addChild(textBlue);

        buttonRandomColor = new SimpleButton(font, SECTION_WIDTH, RANDOM_COLOR_LABEL, b -> onRandomColorPressed());
        colorColumn.addChild(buttonRandomColor);

        colorPalette = new ColorPaletteWidget(frontier.getColor(), color -> {
            colorPicker.setColor(color);
            onColorPicked(color, false);
        });
        colorColumn.addChild(colorPalette);

        syncColorWidgets(frontier.getColor());
    }

    private void buildClipboardSection(GridLayout mainLayout) {
        GridLayout editColumn = new GridLayout().rowSpacing(LayoutConstants.SPACING_SMALL);
        editColumn.defaultCellSetting().alignHorizontallyLeft();
        editColumn.addChild(SpacerElement.width(CLIPBOARD_SPACER_WIDTH), 0, 0);
        mainLayout.addChild(editColumn, 1, 3, LayoutSettings.defaults().alignVerticallyBottom());

        int row = 0;
        labelPasteName = editColumn.addChild(new StringWidget(PASTE_NAME_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        buttonPasteName = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_NAME.get(), ClientConfig.PASTE_NAME::set), row++, 1);

        labelPasteVisibility = editColumn.addChild(new StringWidget(PASTE_VISIBILITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        buttonPasteVisibility = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_VISIBILITY.get(), ClientConfig.PASTE_VISIBILITY::set), row++, 1);

        if (hasPathStyle) {
            labelPastePathStyle = editColumn.addChild(new StringWidget(PASTE_PATH_STYLE_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
            buttonPastePathStyle = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_PATH_STYLE.get(), ClientConfig.PASTE_PATH_STYLE::set), row++, 1);
        }

        labelPasteColor = editColumn.addChild(new StringWidget(PASTE_COLOR_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        buttonPasteColor = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_COLOR.get(), ClientConfig.PASTE_COLOR::set), row++, 1);

        labelPasteBanner = editColumn.addChild(new StringWidget(PASTE_BANNER_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        buttonPasteBanner = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_BANNER.get(), ClientConfig.PASTE_BANNER::set), row++, 1);

        LinearLayout editButtons = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        editColumn.addChild(editButtons, row, 0);

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
        buttonShareSettings = addBottomButton(new SimpleButton(font, SECTION_WIDTH, SHARE_SETTINGS_LABEL, b -> onSharePressed()));
        buttonDelete = addBottomButton(new SimpleButton(font, SECTION_WIDTH, DELETE_LABEL, b -> onDeletePressed()));
        buttonDelete.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonDone = addBottomButton(new SimpleButton(font, SECTION_WIDTH, DONE_LABEL, b -> onClose()));
    }

    private TextBoxInt createRgbTextBox(IntUnaryOperator colorComposer) {
        TextBoxInt textBox = new TextBoxInt(0, 0, 255, font, RGB_TEXTBOX_WIDTH);
        textBox.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textBox.setValueChangedCallback(value -> applyColorChange(colorComposer.applyAsInt(value)));
        return textBox;
    }

    private static String formatMeasurement(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private OptionButton createBinaryOptionButton(boolean defaultValue, Consumer<Boolean> consumer) {
        OptionButton button = new OptionButton(font, LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH, b -> {
            consumer.accept(b.getSelected() == 0);
        });
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(defaultValue ? 0 : 1);
        return button;
    }

    private void onBannerButtonPressed() {
        if (!frontier.hasBanner()) {
            ItemStack heldBanner = getHeldBanner(minecraft);
            if (heldBanner != null) {
                frontier.setBanner(heldBanner);
            }
        } else {
            frontier.setBanner(null);
        }

        updateBannerButton();
        sendBannerChangeToServer();
    }

    private void onBannerRotationChanged(int angle, boolean dragging) {
        frontier.setBannerRotation(angle);
        if (!dragging) {
            sendBannerChangeToServer();
        }
    }

    private void onChangePersonalGlobalPressed() {
        if (frontier.isSessionOnly()) {
            return;
        }

        if (frontier.getPersonal()) {
            showChangeToGlobalConfirmation();
        } else {
            showChangeToPersonalConfirmation();
        }
    }

    private void showChangeToGlobalConfirmation() {
        new ConfirmationDialog(
                "mapfrontiers.change_to_global_frontier_dialog",
                "mapfrontiers.change_to_global_frontier_dialog_desc",
                "mapfrontiers.change_to_global",
                "gui.cancel",
                null,
                response -> changeToGlobal()
        ).display();
    }

    private void showChangeToPersonalConfirmation() {
        new ConfirmationDialog(
                "mapfrontiers.change_to_personal_frontier_dialog",
                "mapfrontiers.change_to_personal_frontier_dialog_desc",
                "mapfrontiers.change_to_personal",
                "gui.cancel",
                null,
                response -> changeToPersonal()
        ).display();
    }

    private void onVisibilityButtonPressed() {
        FrontierData.VisibilityData baseVisibilityData = frontier.getVisibilityData();
        new VisibilityDialog(baseVisibilityData, (newVisibilityData, newVisibilityMask) -> {
            if (newVisibilityData.equals(baseVisibilityData)) {
                return;
            }
            if (newVisibilityData.equals(frontier.getVisibilityData())) {
                return;
            }

            Runnable applyChange = () -> {
                frontier.setVisibilityData(newVisibilityData);
                sendVisibilityChangeToServer();
            };
            if (!frontier.getVisibilityData().equals(baseVisibilityData)) {
                showFrontierChangedConfirmation(applyChange);
            } else {
                applyChange.run();
            }
        }).display();
    }

    private void onVisibilityOverrideButtonPressed() {
        Pair<FrontierData.VisibilityData, FrontierData.VisibilityData> override = MapFrontiersClient.getLocalOverrides().getVisibility(frontier.getId());
        new VisibilityDialog(override.first(), override.second(), (newVisibilityData, newVisibilityMask) -> {
            if (!newVisibilityData.equals(override.first()) || !newVisibilityMask.equals(override.second())) {
                Pair<FrontierData.VisibilityData, FrontierData.VisibilityData> newOverride = Pair.of(newVisibilityData, newVisibilityMask);
                MapFrontiersClient.getLocalOverrides().setVisibility(frontier.getId(), newOverride);
                frontier.setVisibilityOverride(newOverride);
            }
        }).display();
    }

    private void onPathStyleButtonPressed() {
        FrontierData.PathStyle basePathStyle = frontier.getPathStyle();
        new PathStyleDialog(basePathStyle, ClientConfig.getDefaultPathStyle(), newPathStyle -> {
            if (newPathStyle.equals(basePathStyle)) {
                return;
            }
            if (newPathStyle.equals(frontier.getPathStyle())) {
                return;
            }

            Runnable applyChange = () -> {
                frontier.setPathStyle(newPathStyle);
                sendPathStyleChangeToServer();
            };
            if (!frontier.getPathStyle().equals(basePathStyle)) {
                showFrontierChangedConfirmation(applyChange);
            } else {
                applyChange.run();
            }
        }).display();
    }

    private void showFrontierChangedConfirmation(Runnable applyChange) {
        new ConfirmationDialog(
                "mapfrontiers.frontier_changed_dialog",
                "mapfrontiers.frontier_changed_dialog_desc",
                "mapfrontiers.apply_changes",
                "gui.cancel",
                null,
                response -> applyChange.run()
        ).display();
    }

    private void onColorPicked(int color, boolean dragging) {
        frontier.setColor(color);
        syncColorWidgets(color);

        if (!dragging) {
            sendColorChangeToServer();
        }
    }

    private void onRandomColorPressed() {
        applyColorChange(ColorHelper.getRandomColor());
    }

    private void onCopyPressed() {
        MapFrontiersClient.setFrontierClipboard(frontier);
        Minecraft.getInstance().keyboardHandler.setClipboard(frontier.getId().toString());
        updatePasteOptionsVisibility();
    }

    private void onPastePressed() {
        FrontierData clipboard = MapFrontiersClient.getFrontierClipboard();
        if (clipboard != null) {
            boolean pastePathStyleEnabled = hasPathStyle
                    && ClientConfig.PASTE_PATH_STYLE.get()
                    && clipboard.getMode() == FrontierData.Mode.Path;
            if (ClientConfig.PASTE_NAME.get() || ClientConfig.PASTE_VISIBILITY.get()
                    || pastePathStyleEnabled || ClientConfig.PASTE_COLOR.get() || ClientConfig.PASTE_BANNER.get()) {
                setFrontier(clipboard, ClientConfig.PASTE_NAME.get(), ClientConfig.PASTE_VISIBILITY.get(),
                        pastePathStyleEnabled, ClientConfig.PASTE_COLOR.get(), ClientConfig.PASTE_BANNER.get());
                sendCurrentInfoChangesToServer();
                rebuildWidgets();
                repositionElements();
                if (minecraft.getLastInputType().isKeyboard()) {
                    setInitialFocus(buttonPaste);
                }
            }
        }
    }

    private void onPasteOptionsPressed() {
        ClientConfig.PASTE_OPTIONS_VISIBLE.set(!ClientConfig.PASTE_OPTIONS_VISIBLE.get());
        updatePasteOptionsVisibility();
        ClientGlobalEvents.postUpdatedConfigEvent();
    }

    private void onSelectInMapPressed() {
        BlockPos center = frontier.getCenter();
        closeAndReturnToFullscreenMap();
        Services.JOURNEYMAP.fullscreenMapCenterOn(center.getX(), center.getZ());
    }

    private void onSharePressed() {
        if (MapFrontiersClient.isModOnServer()) {
            new ShareSettingsPage(frontier).display();
        } else {
            new SendFrontierPage(frontier).display();
        }
    }

    private void onDeletePressed() {
        if (ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.get()) {
            new DeleteFrontierConfirmationDialog(frontier, response -> {
                if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                    ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.set(false);
                    ClientGlobalEvents.postUpdatedConfigEvent();
                }
                deleteFrontier();
            }).display();
        } else {
            deleteFrontier();
        }
    }

    private void refreshViewState() {
        updateBannerButton();
        updateButtons();
        updatePasteOptionsVisibility();
        updateUndoRedoVisibility();
    }

    @Override
    protected void renderScaledScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (frontier.getBannerRenderer().hasBanner()) {
            frontier.getBannerRenderer().renderBanner(graphics, buttonBanner.getX() + buttonBanner.getWidth() / 2, sliderBannerRotation.getY() + 25, 3);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (GuiEventListener w : children()) {
            if (w instanceof ColorPicker cp) {
                cp.finishSelection();
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
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public void onClose() {
        if (saveChangesOnClose) {
            sendCurrentInfoChangesToServer();
        }
        unsubscribeEvents();
        super.onClose();
    }

    private void deleteFrontier() {
        saveChangesOnClose = false;
        // Unsubscribing to not receive this same event.
        unsubscribeEvents();
        MapFrontiersClient.getOperationService().deleteFrontier(frontier);
        onClose();
    }

    private void unsubscribeEvents() {
        MapFrontiersClient.getFrontierEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
    }

    private void applyColorChange(int color) {
        if (color == frontier.getColor()) {
            return;
        }

        frontier.setColor(color);
        colorPicker.setColor(color);
        syncColorWidgets(color);

        sendColorChangeToServer();
    }

    private void syncColorWidgets(int color) {
        textRed.setValue((color & 0xFF0000) >> 16);
        textGreen.setValue((color & 0x00FF00) >> 8);
        textBlue.setValue(color & 0x0000FF);
        if (colorPalette != null) {
            colorPalette.setColor(color);
        }
    }

    private void undo() {
        if (undoStack.size() == 1) {
            return;
        }

        redoStack.push(undoStack.pop());
        setFrontier(undoStack.peek(), true, true, true, true, true);
        sendCurrentInfoChangesToServer();
        rebuildWidgets();
        repositionElements();
        if (minecraft.getLastInputType().isKeyboard()) {
            if (undoStack.size() == 1) {
                setInitialFocus(buttonRedo);
            } else {
                setInitialFocus(buttonUndo);
            }
        }
    }

    private void redo() {
        if (redoStack.empty()) {
            return;
        }

        setFrontier(redoStack.peek(), true, true, true, true, true);
        undoStack.push(redoStack.pop());
        sendCurrentInfoChangesToServer();
        rebuildWidgets();
        repositionElements();
        if (minecraft.getLastInputType().isKeyboard()) {
            if (redoStack.empty()) {
                setInitialFocus(buttonUndo);
            } else {
                setInitialFocus(buttonRedo);
            }
        }
    }

    private void setFrontier(FrontierData other, boolean name, boolean visibility, boolean pathStyle, boolean color, boolean banner) {
        if (name) {
            frontier.setName1(other.getName1());
            frontier.setName2(other.getName2());
        }
        if (visibility) {
            frontier.setVisibilityData(other.getVisibilityData());
        }
        if (pathStyle && frontier.getMode() == FrontierData.Mode.Path && other.getMode() == FrontierData.Mode.Path) {
            frontier.setPathStyle(other.getPathStyle());
        }
        if (color) {
            frontier.setColor(other.getColor());
        }
        if (banner) {
            frontier.setBannerData(other.getbannerData());
        }
    }

    private void updateBannerButton() {
        if (!frontier.hasBanner()) {
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

    private static ItemStack getHeldBanner(@Nullable Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return null;
        }

        ItemStack mainhand = minecraft.player.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offhand = minecraft.player.getItemBySlot(EquipmentSlot.OFFHAND);
        ItemStack heldBanner = null;

        if (mainhand.getItem() instanceof BannerItem) {
            heldBanner = mainhand;
        } else if (offhand.getItem() instanceof BannerItem) {
            heldBanner = offhand;
        }

        return heldBanner;
    }

    private void changeToGlobal() {
        undoStack.clear();
        redoStack.clear();
        MapFrontiersClient.getOperationService().changeToGlobalAction(new FrontierId(frontier.getId()));
    }

    private void changeToPersonal() {
        undoStack.clear();
        redoStack.clear();
        MapFrontiersClient.getOperationService().changeToPersonalAction(new FrontierId(frontier.getId()));
    }

    private void updateButtons() {
        if (minecraft.player == null) {
            return;
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(minecraft.player);
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontier, playerUser);

        textName1.setEditable(actions.canUpdate);
        textName2.setEditable(actions.canUpdate);
        buttonVisibility.active = actions.canUpdate;
        if (buttonPathStyle != null) {
            buttonPathStyle.visible = hasPathStyle;
            buttonPathStyle.active = actions.canUpdate && hasPathStyle;
        }
        textRed.setEditable(actions.canUpdate);
        textGreen.setEditable(actions.canUpdate);
        textBlue.setEditable(actions.canUpdate);
        buttonRandomColor.active = actions.canUpdate;
        colorPicker.active = actions.canUpdate;
        colorPalette.active = actions.canUpdate;
        buttonPaste.active = actions.canUpdate;
        buttonPasteOptions.active = actions.canUpdate;
        if (frontier.isSessionOnly()) {
            buttonChangeToPersonalGlobal.visible = false;
        } else if (frontier.getPersonal()) {
            buttonChangeToPersonalGlobal.visible = MapFrontiersClient.isModOnServer()
                    && frontier.getOwner().equals(playerUser)
                    && profile != null
                    && profile.createFrontier == SettingsProfile.State.Enabled;
        } else {
            buttonChangeToPersonalGlobal.visible = actions.canDelete;
        }
        buttonDelete.active = actions.canDelete;
        buttonBanner.visible = actions.canUpdate;
        sliderBannerRotation.visible = actions.canUpdate && frontier.hasBanner();
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        buttonSelect.active = uiState != null && frontier.getDimension().equals(uiState.dimension);
        if (MapFrontiersClient.isModOnServer()) {
            buttonShareSettings.setMessage(SHARE_SETTINGS_LABEL);
            buttonShareSettings.active = actions.canShare;
        } else {
            buttonShareSettings.setMessage(SEND_LABEL);
        }
    }

    private void updatePasteOptionsVisibility() {
        FrontierData clipboard = MapFrontiersClient.getFrontierClipboard();
        boolean hasClipboard = clipboard != null;
        boolean showPaste = buttonPaste.active && hasClipboard;
        boolean showPasteOptions = showPaste && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        boolean pathStyleOptionVisible = showPasteOptions && hasPathStyle && clipboard.getMode() == FrontierData.Mode.Path;

        buttonPaste.visible = showPaste;
        buttonPasteOptions.visible = showPaste;
        buttonPasteOptions.setType(ClientConfig.PASTE_OPTIONS_VISIBLE.get() ? IconButton.Type.CollapseOptions : IconButton.Type.ExpandOptions);
        buttonPasteOptions.setTooltip(ClientConfig.PASTE_OPTIONS_VISIBLE.get() ? CLOSE_PASTE_TOOLTIP : OPEN_PASTE_TOOLTIP);
        buttonPasteName.visible = showPasteOptions;
        buttonPasteVisibility.visible = showPasteOptions;
        if (buttonPastePathStyle != null) {
            buttonPastePathStyle.visible = pathStyleOptionVisible;
        }
        buttonPasteColor.visible = showPasteOptions;
        buttonPasteBanner.visible = showPasteOptions;
        labelPasteName.visible = showPasteOptions;
        labelPasteVisibility.visible = showPasteOptions;
        if (labelPastePathStyle != null) {
            labelPastePathStyle.visible = pathStyleOptionVisible;
        }
        labelPasteColor.visible = showPasteOptions;
        labelPasteBanner.visible = showPasteOptions;
    }

    private void updateUndoRedoVisibility() {
        buttonUndo.visible = buttonPaste.active && undoStack.size() > 1;
        buttonRedo.visible = buttonPaste.active && redoStack.size() > 0;
    }

    private void sendNameChangeToServer() {
        FrontierChange change = new FrontierChange();
        change.setName(frontier.getName1(), frontier.getName2());
        sendChangeToServer(change);
    }

    private void sendVisibilityChangeToServer() {
        FrontierChange change = new FrontierChange();
        change.setVisibility(frontier.getVisibilityData());
        sendChangeToServer(change);
    }

    private void sendColorChangeToServer() {
        FrontierChange change = new FrontierChange();
        change.setColor(frontier.getColor());
        sendChangeToServer(change);
    }

    private void sendBannerChangeToServer() {
        FrontierChange change = new FrontierChange();
        change.setBanner(frontier.getbannerData());
        sendChangeToServer(change);
    }

    private void sendPathStyleChangeToServer() {
        FrontierChange change = new FrontierChange();
        change.setPathStyle(frontier.getPathStyle());
        sendChangeToServer(change);
    }

    private void sendCurrentInfoChangesToServer() {
        FrontierChange change = new FrontierChange();
        change.setName(frontier.getName1(), frontier.getName2());
        change.setVisibility(frontier.getVisibilityData());
        change.setColor(frontier.getColor());
        change.setBanner(frontier.getbannerData());
        if (hasPathStyle) {
            change.setPathStyle(frontier.getPathStyle());
        }
        sendChangeToServer(change);
    }

    private void sendChangeToServer(FrontierChange change) {
        if (change.isEmpty()) {
            return;
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(minecraft.player);
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontier, playerUser);

        if (actions.canUpdate) {
            if (frontier.getHash() != frontierHash) {
                frontierHash = frontier.getHash();
                MapFrontiersClient.getOperationService().updateFrontier(frontier, change);
            }
        }
    }

    private void addToUndo(FrontierData frontier) {
        boolean add = undoStack.empty();
        if (!add) {
            FrontierData u = undoStack.peek();
            if (!Objects.equals(u.getName1(), frontier.getName1())
                    || !Objects.equals(u.getName2(), frontier.getName2())
                    || !Objects.equals(u.getVisibilityData(), frontier.getVisibilityData())
                    || u.getColor() != frontier.getColor()
                    || !Objects.equals(u.getbannerData(), frontier.getbannerData())
                    || (u.getMode() == FrontierData.Mode.Path && frontier.getMode() == FrontierData.Mode.Path
                    && !Objects.equals(u.getPathStyle(), frontier.getPathStyle()))) {
                add = true;
            }
        }

        if (add) {
            undoStack.push(frontier);

            if (!redoStack.empty()) {
                redoStack.clear();
            }

            updateUndoRedoVisibility();
        }
    }

    private Component getFrontierTypeLabel() {
        Component baseType = frontier.getPersonal() ? PERSONAL_LABEL : GLOBAL_LABEL;
        if (frontier.isSessionOnly()) {
            return Component.translatable(TEMPORARY_KEY).append(Component.literal(" ")).append(baseType);
        }
        return baseType;
    }

}
