package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.api.model.FrontierId;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPaletteWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPicker;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleSlider;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.ConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.DeleteConfirmationDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.PathStyleDialog;
import games.alejandrocoria.mapfrontiers.client.gui.dialog.VisibilityDialog;
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
public class FrontierInfo extends AutoScaledScreen {
    static final DateFormat dateFormat = new SimpleDateFormat();
    private static final Component titleLabel = Component.translatable("mapfrontiers.title_info");
    private static final Component assignBannerLabel = Component.translatable("mapfrontiers.assign_banner");
    private static final Component assignBannerWarnLabel = assignBannerLabel.copy().append(Component.literal(ColorConstants.WARNING + " !"));
    private static final Component removeBannerLabel = Component.translatable("mapfrontiers.remove_banner");
    private static final String bannerRotationKey = "mapfrontiers.banner_rotation";
    private static final Component nameLabel = Component.translatable("mapfrontiers.name");
    private static final Component personalLabel = Component.translatable("mapfrontiers.config.Personal");
    private static final Component globalLabel = Component.translatable("mapfrontiers.config.Global");
    private static final String verticesKey = "mapfrontiers.vertices";
    private static final String chunksKey = "mapfrontiers.chunks";
    private static final String pointsKey = "mapfrontiers.points";
    private static final String ownerKey = "mapfrontiers.owner";
    private static final String originalOwnerKey = "mapfrontiers.original_owner";
    private static final String dimensionKey = "mapfrontiers.dimension";
    private static final String sourcePluginKey = "mapfrontiers.source_plugin";
    private static final String temporarySourcePluginKey = "mapfrontiers.temporary_source_plugin";
    private static final String temporaryKey = "mapfrontiers.temporary";
    private static final String areaKey = "mapfrontiers.area";
    private static final String lengthKey = "mapfrontiers.length";
    private static final String perimeterKey = "mapfrontiers.perimeter";
    private static final String createdKey = "mapfrontiers.created";
    private static final String modifiedKey = "mapfrontiers.modified";
    private static final Component visibilityLabel = Component.translatable("mapfrontiers.visibility");
    private static final Component visibilityOverrideLabel = Component.translatable("mapfrontiers.visibility_override");
    private static final Component pathStyleLabel = Component.translatable("mapfrontiers.path_style");
    private static final Component colorLabel = Component.translatable("mapfrontiers.color");
    private static final Component rLabel = Component.literal("R");
    private static final Component gLabel = Component.literal("G");
    private static final Component bLabel = Component.literal("B");
    private static final Component randomColorLabel = Component.translatable("mapfrontiers.random_color");
    private static final Component pasteNameLabel = Component.translatable("mapfrontiers.paste_name");
    private static final Component pasteVisibilityLabel = Component.translatable("mapfrontiers.paste_visibility");
    private static final Component pasteColorLabel = Component.translatable("mapfrontiers.paste_color");
    private static final Component pasteBannerLabel = Component.translatable("mapfrontiers.paste_banner");
    private static final Component selectInMapLabel = Component.translatable("mapfrontiers.select_in_map");
    private static final Component shareSettingsLabel = Component.translatable("mapfrontiers.share_settings");
    private static final Component sendLabel = Component.translatable("mapfrontiers.send");
    private static final Component deleteLabel = Component.translatable("mapfrontiers.delete");
    private static final Component doneLabel = Component.translatable("gui.done");
    private static final Component onLabel = Component.translatable("options.on");
    private static final Component offLabel = Component.translatable("options.off");

    private static final Tooltip visibilityTooltip = Tooltip.create(Component.translatable("mapfrontiers.visibility.tooltip"));
    private static final Tooltip visibilityOverrideTooltip = Tooltip.create(Component.translatable("mapfrontiers.visibility_override.tooltip"));
    private static final Tooltip copyTooltip = Tooltip.create(Component.translatable("mapfrontiers.copy"));
    private static final Tooltip pasteTooltip = Tooltip.create(Component.translatable("mapfrontiers.paste"));
    private static final Tooltip openPasteTooltip = Tooltip.create(Component.translatable("mapfrontiers.open_paste_options"));
    private static final Tooltip closePasteTooltip = Tooltip.create(Component.translatable("mapfrontiers.close_paste_options"));
    private static final Tooltip undoTooltip = Tooltip.create(Component.translatable("mapfrontiers.undo"));
    private static final Tooltip redoTooltip = Tooltip.create(Component.translatable("mapfrontiers.redo"));
    private static final Tooltip changeToPersonalTooltip = Tooltip.create(Component.translatable("mapfrontiers.change_to_personal"));
    private static final Tooltip changeToGlobalTooltip = Tooltip.create(Component.translatable("mapfrontiers.change_to_global"));
    private static final Tooltip assignBannerWarnTooltip = Tooltip.create(Component.literal(ColorConstants.WARNING + "! " + ChatFormatting.RESET).append(Component.translatable("mapfrontiers.assign_banner_warn")));
    private static final int MAIN_LAYOUT_SPACING = 10;
    private static final int SECTION_WIDTH = 144;
    private static final int NAME_SECTION_WIDTH = SECTION_WIDTH * 2 + MAIN_LAYOUT_SPACING + 1;
    private static final int NAME_MAX_LENGTH = 48;
    private static final int DEFAULT_TEXTBOX_HEIGHT = 20;
    private static final int SECTION_SPACING_SMALL = 2;
    private static final int SECTION_SPACING_MEDIUM = 4;
    private static final int INLINE_SPACING = 3;
    private static final int OPTION_BUTTON_WIDTH = 28;
    private static final int RGB_LABEL_HEIGHT = 8;
    private static final int RGB_TEXTBOX_WIDTH = 33;
    private static final int RGB_ROW_SPACER_WIDTH = 3;
    private static final int CLIPBOARD_SPACER_WIDTH = 116;

    private final IClientAPI jmAPI;

    private final FrontierOverlay frontier;
    private int frontierHash;
    private TextBox textName1;
    private TextBox textName2;
    private SimpleButton buttonVisibility;
    private SimpleButton buttonVisibilityOverride;
    private SimpleButton buttonPathStyle;
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
    private OptionButton buttonPasteColor;
    private OptionButton buttonPasteBanner;
    private StringWidget labelPasteName;
    private StringWidget labelPasteVisibility;
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

    public FrontierInfo(IClientAPI jmAPI, FrontierOverlay frontier) {
        super(titleLabel, 636, 306);
        this.jmAPI = jmAPI;
        this.frontier = frontier;
        frontierHash = frontier.getHash();
        undoStack.push(new FrontierData(frontier));

        MapFrontiersClient.getFrontierEvents().subscribeDeleted(this, frontierID -> {
            if (frontier.getId().equals(frontierID)) {
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
                        Component modified = Component.translatable("mapfrontiers.modified", dateFormat.format(frontier.getModified()));
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
    public void initScreen() {
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
        LinearLayout bannerColumn = LinearLayout.vertical().spacing(SECTION_SPACING_SMALL);
        bannerColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(bannerColumn, 0, 0);

        buttonBanner = new SimpleButton(font, SECTION_WIDTH, assignBannerLabel, b -> onBannerButtonPressed());
        bannerColumn.addChild(buttonBanner);

        sliderBannerRotation = new SimpleSlider(font, SECTION_WIDTH, bannerRotationKey, 0, 360, frontier.getBannerRotation(), this::onBannerRotationChanged);
        bannerColumn.addChild(sliderBannerRotation);
    }

    private void buildOverviewSection(GridLayout mainLayout) {
        LinearLayout nameColumn = LinearLayout.vertical().spacing(SECTION_SPACING_SMALL);
        nameColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(nameColumn, 0, 1, 1, 2);

        LinearLayout headerRow = LinearLayout.horizontal().spacing(SECTION_SPACING_SMALL);
        Component dimension = Component.translatable(dimensionKey, frontier.getDimension().identifier().toString());
        headerRow.addChild(new StringWidget(nameLabel, font).setColor(ColorConstants.INFO_LABEL_TEXT));
        headerRow.addChild(SpacerElement.width(Math.max(0, NAME_SECTION_WIDTH - font.width(nameLabel.getVisualOrderText()) - font.width(dimension.getVisualOrderText()) - SECTION_SPACING_SMALL - 1)));
        headerRow.addChild(new StringWidget(dimension, font).setColor(ColorConstants.TEXT_DIMENSION));
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

        nameColumn.addChild(SpacerElement.height(0));

        Component sourceInfo = Component.empty();
        if (frontier.getSourcePluginId() != null) {
            sourceInfo = frontier.isSessionOnly()
                    ? Component.translatable(temporarySourcePluginKey, frontier.getSourcePluginId())
                    : Component.translatable(sourcePluginKey, frontier.getSourcePluginId());
        } else if (frontier.isSessionOnly()) {
            sourceInfo = Component.translatable(temporaryKey);
        }
        nameColumn.addChild(new StringWidget(sourceInfo, font).setColor(ColorConstants.TEXT_SOURCE_PLUGIN));

        LinearLayout visibilityRow = LinearLayout.horizontal().spacing(MAIN_LAYOUT_SPACING + 1);
        visibilityRow.defaultCellSetting().alignVerticallyMiddle();
        nameColumn.addChild(visibilityRow);

        buttonVisibility = new SimpleButton(font, SECTION_WIDTH, visibilityLabel, b -> onVisibilityButtonPressed());
        buttonVisibility.setTooltip(visibilityTooltip);
        visibilityRow.addChild(buttonVisibility);

        buttonVisibilityOverride = new SimpleButton(font, SECTION_WIDTH, visibilityOverrideLabel, b -> onVisibilityOverrideButtonPressed());
        buttonVisibilityOverride.setTooltip(visibilityOverrideTooltip);
        visibilityRow.addChild(buttonVisibilityOverride);

        LinearLayout pathStyleRow = LinearLayout.horizontal().spacing(MAIN_LAYOUT_SPACING + 1);
        pathStyleRow.defaultCellSetting().alignVerticallyMiddle();
        nameColumn.addChild(pathStyleRow);

        buttonPathStyle = new SimpleButton(font, SECTION_WIDTH, pathStyleLabel, b -> onPathStyleButtonPressed());
        buttonPathStyle.visible = frontier.getMode() == FrontierData.Mode.Path;
        pathStyleRow.addChild(buttonPathStyle);
        pathStyleRow.addChild(SpacerElement.width(SECTION_WIDTH));
    }

    private TextBox createNameTextBox(String initialValue, Consumer<String> setter) {
        TextBox textBox = new TextBox(font, NAME_SECTION_WIDTH);
        textBox.setMaxLength(NAME_MAX_LENGTH);
        textBox.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textBox.setValue(initialValue);
        textBox.setLostFocusCallback(value -> sendNameChangeToServer());
        textBox.setValueChangedCallback(setter);
        return textBox;
    }

    private void buildInfoSection(GridLayout mainLayout) {
        LinearLayout infoColumn = LinearLayout.vertical().spacing(SECTION_SPACING_SMALL);
        mainLayout.addChild(infoColumn, 0, 3, 1, 1, LayoutSettings.defaults().alignHorizontallyLeft());

        MutableComponent owner = Component.translatable(ownerKey, frontier.getOwner().toString());
        if (frontier.wasCopied()) {
            owner.append(Component.literal(ColorConstants.WARNING + " !"));
        }
        StringWidget ownerWidget = infoColumn.addChild(new StringWidget(owner, font).setColor(ColorConstants.WHITE));
        if (frontier.wasCopied()) {
            Tooltip ownerTooltip = Tooltip.create(Component.literal(ColorConstants.WARNING + "! " + ChatFormatting.RESET)
                    .append(Component.translatable(originalOwnerKey, frontier.getCopiedFromUser().toString())));
            ownerWidget.setTooltip(ownerTooltip);
        }

        LinearLayout identityRow = LinearLayout.horizontal().spacing(SECTION_SPACING_MEDIUM);
        infoColumn.addChild(identityRow);

        identityRow.addChild(new StringWidget(frontier.getPersonal() ? personalLabel : globalLabel, font).setColor(ColorConstants.WHITE));
        buttonChangeToPersonalGlobal = identityRow.addChild(new IconButton(IconButton.Type.Swap, b -> onChangePersonalGlobalPressed()));
        buttonChangeToPersonalGlobal.setTooltip(frontier.getPersonal() ? changeToGlobalTooltip : changeToPersonalTooltip);

        Component shapeSummary = switch (frontier.getMode()) {
            case Vertex -> Component.translatable(verticesKey, frontier.getVertexCount());
            case Chunk -> Component.translatable(chunksKey, frontier.getChunkCount());
            case Path -> Component.translatable(pointsKey, frontier.getPointCount());
        };
        infoColumn.addChild(new StringWidget(shapeSummary, font).setColor(ColorConstants.WHITE));

        if (frontier.getMode() != FrontierData.Mode.Path) {
            infoColumn.addChild(new StringWidget(Component.translatable(areaKey, formatMeasurement(frontier.area)), font).setColor(ColorConstants.WHITE));
            infoColumn.addChild(new StringWidget(Component.translatable(perimeterKey, formatMeasurement(frontier.perimeter)), font).setColor(ColorConstants.WHITE));
        } else {
            infoColumn.addChild(new StringWidget(Component.translatable(lengthKey, formatMeasurement(frontier.perimeter)), font).setColor(ColorConstants.WHITE));
        }

        if (frontier.getCreated() != null) {
            infoColumn.addChild(new StringWidget(Component.translatable(createdKey, dateFormat.format(frontier.getCreated())), font).setColor(ColorConstants.WHITE));
        }

        if (frontier.getModified() != null) {
            modifiedLabel = infoColumn.addChild(new StringWidget(Component.translatable(modifiedKey, dateFormat.format(frontier.getModified())), font)
                    .setColor(ColorConstants.WHITE));
        }
    }

    private void buildColorSection(GridLayout mainLayout) {
        colorPicker = new ColorPicker(frontier.getColor(), this::onColorPicked);
        mainLayout.addChild(colorPicker, 1, 1, LayoutSettings.defaults().alignVerticallyBottom());

        LinearLayout colorColumn = LinearLayout.vertical().spacing(SECTION_SPACING_MEDIUM);
        colorColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(colorColumn, 1, 2);

        colorColumn.addChild(new StringWidget(colorLabel, font).setColor(ColorConstants.INFO_LABEL_TEXT), LayoutSettings.defaults().alignHorizontallyLeft());

        LinearLayout rgbRow = LinearLayout.horizontal().spacing(INLINE_SPACING);
        rgbRow.defaultCellSetting().alignVerticallyMiddle();
        colorColumn.addChild(rgbRow);

        rgbRow.addChild(new StringWidget(rLabel, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.INFO_LABEL_TEXT));
        textRed = createRgbTextBox(value -> (frontier.getColor() & 0xff00ffff) | (value << 16));
        rgbRow.addChild(textRed);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(gLabel, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.INFO_LABEL_TEXT));
        textGreen = createRgbTextBox(value -> (frontier.getColor() & 0xffff00ff) | (value << 8));
        rgbRow.addChild(textGreen);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(bLabel, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.INFO_LABEL_TEXT));
        textBlue = createRgbTextBox(value -> (frontier.getColor() & 0xffffff00) | value);
        rgbRow.addChild(textBlue);

        buttonRandomColor = new SimpleButton(font, SECTION_WIDTH, randomColorLabel, b -> onRandomColorPressed());
        colorColumn.addChild(buttonRandomColor);

        colorPalette = new ColorPaletteWidget(frontier.getColor(), color -> {
            colorPicker.setColor(color);
            onColorPicked(color, false);
        });
        colorColumn.addChild(colorPalette);

        syncColorWidgets(frontier.getColor());
    }

    private void buildClipboardSection(GridLayout mainLayout) {
        GridLayout editColumn = new GridLayout().rowSpacing(SECTION_SPACING_MEDIUM);
        editColumn.defaultCellSetting().alignHorizontallyLeft();
        editColumn.addChild(SpacerElement.width(CLIPBOARD_SPACER_WIDTH), 0, 0);
        mainLayout.addChild(editColumn, 1, 3, LayoutSettings.defaults().alignVerticallyBottom());

        labelPasteName = editColumn.addChild(new StringWidget(pasteNameLabel, font).setColor(ColorConstants.TEXT), 0, 0);
        buttonPasteName = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_NAME.get(), ClientConfig.PASTE_NAME::set), 0, 1);

        labelPasteVisibility = editColumn.addChild(new StringWidget(pasteVisibilityLabel, font).setColor(ColorConstants.TEXT), 1, 0);
        buttonPasteVisibility = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_VISIBILITY.get(), ClientConfig.PASTE_VISIBILITY::set), 1, 1);

        labelPasteColor = editColumn.addChild(new StringWidget(pasteColorLabel, font).setColor(ColorConstants.TEXT), 2, 0);
        buttonPasteColor = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_COLOR.get(), ClientConfig.PASTE_COLOR::set), 2, 1);

        labelPasteBanner = editColumn.addChild(new StringWidget(pasteBannerLabel, font).setColor(ColorConstants.TEXT), 3, 0);
        buttonPasteBanner = editColumn.addChild(createBinaryOptionButton(ClientConfig.PASTE_BANNER.get(), ClientConfig.PASTE_BANNER::set), 3, 1);

        LinearLayout editButtons = LinearLayout.horizontal().spacing(INLINE_SPACING);
        editColumn.addChild(editButtons, 4, 0);

        buttonCopy = editButtons.addChild(new IconButton(IconButton.Type.Copy, b -> onCopyPressed()));
        buttonCopy.setTooltip(copyTooltip);

        LinearLayout pasteButtons = LinearLayout.horizontal();
        editButtons.addChild(pasteButtons);

        buttonPaste = pasteButtons.addChild(new IconButton(IconButton.Type.Paste, b -> onPastePressed()));
        buttonPaste.setTooltip(pasteTooltip);

        buttonPasteOptions = pasteButtons.addChild(new IconButton(IconButton.Type.ArrowUp, b -> onPasteOptionsPressed()));
        buttonPasteOptions.setTooltip(openPasteTooltip);

        buttonUndo = editButtons.addChild(new IconButton(IconButton.Type.Undo, b -> undo()));
        buttonUndo.setTooltip(undoTooltip);

        buttonRedo = editButtons.addChild(new IconButton(IconButton.Type.Redo, b -> redo()));
        buttonRedo.setTooltip(redoTooltip);
    }

    private void buildBottomButtons() {
        buttonSelect = bottomButtons.addChild(new SimpleButton(font, SECTION_WIDTH, selectInMapLabel, b -> onSelectInMapPressed()));
        buttonShareSettings = bottomButtons.addChild(new SimpleButton(font, SECTION_WIDTH, shareSettingsLabel, b -> onSharePressed()));
        buttonDelete = bottomButtons.addChild(new SimpleButton(font, SECTION_WIDTH, deleteLabel, b -> onDeletePressed()));
        buttonDelete.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonDone = bottomButtons.addChild(new SimpleButton(font, SECTION_WIDTH, doneLabel, b -> onClose()));
    }

    private TextBoxInt createRgbTextBox(IntUnaryOperator colorComposer) {
        TextBoxInt textBox = new TextBoxInt(0, 0, 255, font, RGB_TEXTBOX_WIDTH);
        textBox.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textBox.setValueChangedCallback(value -> applyColorChange(colorComposer.applyAsInt(value), true));
        return textBox;
    }

    private static String formatMeasurement(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private OptionButton createBinaryOptionButton(boolean defaultValue, Consumer<Boolean> consumer) {
        OptionButton button = new OptionButton(font, OPTION_BUTTON_WIDTH, b -> {
            consumer.accept(b.getSelected() == 0);
            sendCurrentInfoChangesToServer();
        });
        button.addOption(onLabel);
        button.addOption(offLabel);
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
        new VisibilityDialog(frontier.getVisibilityData(), (newVisibilityData, newVisibilityMask) -> {
            if (!newVisibilityData.equals(frontier.getVisibilityData())) {
                frontier.setVisibilityData(newVisibilityData);
                sendVisibilityChangeToServer();
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
        new PathStyleDialog(frontier.getPathStyle(), ClientConfig.getDefaultPathStyle(), newPathStyle -> {
            if (!frontier.getPathStyle().equals(newPathStyle)) {
                frontier.setPathStyle(newPathStyle);
                sendPathStyleChangeToServer();
            }
        }).display();
    }

    private void onColorPicked(int color, boolean dragging) {
        frontier.setColor(color);
        syncColorWidgets(color);

        if (!dragging) {
            sendColorChangeToServer();
        }
    }

    private void onRandomColorPressed() {
        applyColorChange(ColorHelper.getRandomColor(), true);
    }

    private void onCopyPressed() {
        MapFrontiersClient.setClipboard(frontier);
        Minecraft.getInstance().keyboardHandler.setClipboard(frontier.getId().toString());
        updatePasteOptionsVisibility();
    }

    private void onPastePressed() {
        FrontierData clipboard = MapFrontiersClient.getClipboard();
        if (clipboard != null && (ClientConfig.PASTE_NAME.get() || ClientConfig.PASTE_VISIBILITY.get()
                || ClientConfig.PASTE_COLOR.get() || ClientConfig.PASTE_BANNER.get())) {
            setFrontier(clipboard, ClientConfig.PASTE_NAME.get(), ClientConfig.PASTE_VISIBILITY.get(),
                    ClientConfig.PASTE_COLOR.get(), ClientConfig.PASTE_BANNER.get(), false);
            sendCurrentInfoChangesToServer();
            rebuildWidgets();
            repositionElements();
            if (minecraft.getLastInputType().isKeyboard()) {
                setInitialFocus(buttonPaste);
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
            new ShareSettings(frontier).display();
        } else {
            new SendFrontier(frontier).display();
        }
    }

    private void onDeletePressed() {
        if (ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.get()) {
            new DeleteConfirmationDialog(
                    "mapfrontiers.delete_frontier_dialog",
                    response -> {
                        if (response == ConfirmationDialog.Response.ConfirmAlternative) {
                            ClientConfig.ASK_CONFIRMATION_FRONTIER_DELETE.set(false);
                            ClientGlobalEvents.postUpdatedConfigEvent();
                        }
                        deleteFrontier();
                    }
            ).display();
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
    public void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
    }

    @Override
    public void renderScaledScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
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
        sendCurrentInfoChangesToServer();
        MapFrontiersClient.getFrontierEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
        super.onClose();
    }

    private void deleteFrontier() {
        // Unsubscribing to not receive this same event.
        MapFrontiersClient.getFrontierEvents().unsubscribe(this);
        MapFrontiersClient.getSettingsProfileEvents().unsubscribe(this);
        ClientGlobalEvents.unsubscribeAllEvents(this);
        MapFrontiersClient.getOperationService().deleteFrontier(frontier);
        onClose();
    }

    private void applyColorChange(int color, boolean sendToServer) {
        if (color == frontier.getColor()) {
            return;
        }

        frontier.setColor(color);
        colorPicker.setColor(color);
        syncColorWidgets(color);

        if (sendToServer) {
            sendColorChangeToServer();
        }
    }

    private void syncColorWidgets(int color) {
        textRed.setValue((color & 0xff0000) >> 16);
        textGreen.setValue((color & 0x00ff00) >> 8);
        textBlue.setValue(color & 0x0000ff);
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

    private void setFrontier(FrontierData other, boolean name, boolean visibility, boolean color, boolean banner, boolean pathStyle) {
        if (name) {
            frontier.setName1(other.getName1());
            frontier.setName2(other.getName2());
        }
        if (visibility) {
            frontier.setVisibilityData(other.getVisibilityData());
        }
        if (color) {
            frontier.setColor(other.getColor());
        }
        if (banner) {
            frontier.setBannerData(other.getbannerData());
        }
        if (pathStyle && frontier.getMode() == FrontierData.Mode.Path && other.getMode() == FrontierData.Mode.Path) {
            frontier.setPathStyle(other.getPathStyle());
        }
    }

    private void updateBannerButton() {
        if (!frontier.hasBanner()) {
            if (getHeldBanner(minecraft) != null) {
                buttonBanner.setMessage(assignBannerLabel);
                buttonBanner.setTooltip(null);
            } else {
                buttonBanner.setMessage(assignBannerWarnLabel);
                buttonBanner.setTooltip(assignBannerWarnTooltip);
            }
            sliderBannerRotation.visible = false;
        } else {
            buttonBanner.setMessage(removeBannerLabel);
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
            buttonPathStyle.visible = frontier.getMode() == FrontierData.Mode.Path;
            buttonPathStyle.active = actions.canUpdate && frontier.getMode() == FrontierData.Mode.Path;
        }
        textRed.setEditable(actions.canUpdate);
        textGreen.setEditable(actions.canUpdate);
        textBlue.setEditable(actions.canUpdate);
        buttonRandomColor.active = actions.canUpdate;
        colorPicker.active = actions.canUpdate;
        colorPalette.active = actions.canUpdate;
        buttonPaste.active = actions.canUpdate;
        buttonPasteOptions.active = actions.canUpdate;
        if (frontier.getPersonal()) {
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
            buttonShareSettings.setMessage(shareSettingsLabel);
            buttonShareSettings.active = actions.canShare;
        } else {
            buttonShareSettings.setMessage(sendLabel);
        }
    }

    private void updatePasteOptionsVisibility() {
        buttonPaste.visible = buttonPaste.active && MapFrontiersClient.getClipboard() != null;
        buttonPasteOptions.visible = buttonPaste.visible;
        buttonPasteOptions.setType(ClientConfig.PASTE_OPTIONS_VISIBLE.get() ? IconButton.Type.ArrowDown : IconButton.Type.ArrowUp);
        buttonPasteOptions.setTooltip(ClientConfig.PASTE_OPTIONS_VISIBLE.get() ? closePasteTooltip : openPasteTooltip);
        buttonPasteName.visible = buttonPaste.visible && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        buttonPasteVisibility.visible = buttonPaste.visible && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        buttonPasteColor.visible = buttonPaste.visible && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        buttonPasteBanner.visible = buttonPaste.visible && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        labelPasteName.visible = buttonPaste.visible && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        labelPasteVisibility.visible = buttonPaste.visible && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        labelPasteColor.visible = buttonPaste.visible && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
        labelPasteBanner.visible = buttonPaste.visible && ClientConfig.PASTE_OPTIONS_VISIBLE.get();
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
        if (frontier.getMode() == FrontierData.Mode.Path) {
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
}
