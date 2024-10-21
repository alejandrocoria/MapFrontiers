package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPaletteWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPicker;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.platform.Services;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.util.UIState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class FrontierInfo extends AutoScaledScreen {
    static final DateFormat dateFormat = new SimpleDateFormat();
    private static final Component titleLabel = Component.translatable("mapfrontiers.title_info");
    private static final Component assignBannerLabel = Component.translatable("mapfrontiers.assign_banner");
    private static final Component assignBannerWarnLabel = assignBannerLabel.copy().append(Component.literal(ColorConstants.WARNING + " !"));
    private static final Component removeBannerLabel = Component.translatable("mapfrontiers.remove_banner");
    private static final Component nameLabel = Component.translatable("mapfrontiers.name");
    private static final Component personalLabel = Component.translatable("mapfrontiers.config.Personal");
    private static final Component globalLabel = Component.translatable("mapfrontiers.config.Global");
    private static final String verticesKey = "mapfrontiers.vertices";
    private static final String chunksKey = "mapfrontiers.chunks";
    private static final String ownerKey = "mapfrontiers.owner";
    private static final String dimensionKey = "mapfrontiers.dimension";
    private static final String areaKey = "mapfrontiers.area";
    private static final String perimeterKey = "mapfrontiers.perimeter";
    private static final String createdKey = "mapfrontiers.created";
    private static final String modifiedKey = "mapfrontiers.modified";
    private static final Component showFrontierLabel = Component.translatable("mapfrontiers.show_frontier");
    private static final Component announceInChatLabel = Component.translatable("mapfrontiers.announce_in_chat");
    private static final Component announceInTitleLabel = Component.translatable("mapfrontiers.announce_in_title");
    private static final Component showFullscreenLabel = Component.translatable("mapfrontiers.show_fullscreen");
    private static final Component showNameLabel = Component.translatable("mapfrontiers.show_name");
    private static final Component showOwnerLabel = Component.translatable("mapfrontiers.show_owner");
    private static final Component showMinimapLabel = Component.translatable("mapfrontiers.show_minimap");
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
    private static final Component deleteLabel = Component.translatable("mapfrontiers.delete");
    private static final Component doneLabel = Component.translatable("gui.done");
    private static final Component onLabel = Component.translatable("options.on");
    private static final Component offLabel = Component.translatable("options.off");

    private static final Tooltip copyTooltip = Tooltip.create(Component.translatable("mapfrontiers.copy"));
    private static final Tooltip pasteTooltip = Tooltip.create(Component.translatable("mapfrontiers.paste"));
    private static final Tooltip openPasteTooltip = Tooltip.create(Component.translatable("mapfrontiers.open_paste_options"));
    private static final Tooltip closePasteTooltip = Tooltip.create(Component.translatable("mapfrontiers.close_paste_options"));
    private static final Tooltip undoTooltip = Tooltip.create(Component.translatable("mapfrontiers.undo"));
    private static final Tooltip redoTooltip = Tooltip.create(Component.translatable("mapfrontiers.redo"));
    private static final Tooltip assignBannerWarnTooltip = Tooltip.create(Component.literal(ColorConstants.WARNING + "! " + ChatFormatting.RESET).append(Component.translatable("mapfrontiers.assign_banner_warn")));

    private final IClientAPI jmAPI;

    private final FrontiersOverlayManager frontiersOverlayManager;
    private final FrontierOverlay frontier;
    private TextBox textName1;
    private TextBox textName2;
    private OptionButton buttonVisible;
    private OptionButton buttonFullscreenVisible;
    private OptionButton buttonFullscreenNameVisible;
    private OptionButton buttonFullscreenOwnerVisible;
    private OptionButton buttonMinimapVisible;
    private OptionButton buttonMinimapNameVisible;
    private OptionButton buttonMinimapOwnerVisible;
    private OptionButton buttonAnnounceInChat;
    private OptionButton buttonAnnounceInTitle;
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

    private SimpleButton buttonSelect;
    private SimpleButton buttonShareSettings;
    private SimpleButton buttonDelete;
    private SimpleButton buttonDone;
    private SimpleButton buttonBanner;

    private StringWidget modifiedLabel;

    private final Stack<FrontierData> undoStack = new Stack<>();
    private final Stack<FrontierData> redoStack = new Stack<>();

    public FrontierInfo(IClientAPI jmAPI, FrontierOverlay frontier) {
        super(titleLabel, 636, 350);
        this.jmAPI = jmAPI;
        frontiersOverlayManager = MapFrontiersClient.getFrontiersOverlayManager(frontier.getPersonal());
        this.frontier = frontier;
        undoStack.push(new FrontierData(frontier));

        ClientEventHandler.subscribeDeletedFrontierEvent(this, frontierID -> {
            if (frontier.getId().equals(frontierID)) {
                onClose();
            }
        });

        ClientEventHandler.subscribeUpdatedFrontierEvent(this, (frontierOverlay, playerID) -> {
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
            }
        });

        ClientEventHandler.subscribeUpdatedSettingsProfileEvent(this, profile -> {
            updateButtons();
            updateBannerButton();
        });
    }

    @Override
    public void initScreen() {
        GridLayout mainLayout = new GridLayout().spacing(10);
        content.addChild(mainLayout);

        buttonBanner = new SimpleButton(font, 144, assignBannerLabel, (b) -> {
            if (!frontier.hasBanner()) {
                ItemStack heldBanner = getHeldBanner(minecraft);
                if (heldBanner != null) {
                    frontier.setBanner(heldBanner);
                }
            } else {
                frontier.setBanner(null);
            }
            updateBannerButton();
            sendChangesToServer();
        });
        mainLayout.addChild(buttonBanner, 0, 0);

        LinearLayout nameColumn = LinearLayout.vertical().spacing(2);
        nameColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(nameColumn, 0, 1, 2, 1);

        nameColumn.addChild(new StringWidget(nameLabel, font).setColor(ColorConstants.INFO_LABEL_TEXT));
        textName1 = new TextBox(font, 144);
        textName1.setMaxLength(17);
        textName1.setHeight(20);
        textName1.setValue(frontier.getName1());
        textName1.setLostFocusCallback(value -> sendChangesToServer());
        textName1.setValueChangedCallback(value -> {
            if (!frontier.getName1().equals(value)) {
                frontier.setName1(value);
            }
        });
        nameColumn.addChild(textName1);
        textName2 = new TextBox(font, 144);
        textName2.setMaxLength(17);
        textName2.setHeight(20);
        textName2.setValue(frontier.getName2());
        textName2.setLostFocusCallback(value -> sendChangesToServer());
        textName2.setValueChangedCallback(value -> {
            if (!frontier.getName2().equals(value)) {
                frontier.setName2(value);
            }
        });
        nameColumn.addChild(textName2);

        LinearLayout dataRow1 = LinearLayout.vertical();
        mainLayout.addChild(dataRow1, 0, 2, 1, 2, LayoutSettings.defaults().alignHorizontallyLeft());
        LinearLayout dataRow1sub = LinearLayout.horizontal().spacing(12);
        dataRow1.addChild(dataRow1sub);

        dataRow1sub.addChild(new StringWidget(frontier.getPersonal() ? personalLabel : globalLabel, font).setColor(ColorConstants.WHITE));

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            Component vertices = Component.translatable(verticesKey, frontier.getVertexCount());
            dataRow1sub.addChild(new StringWidget(vertices, font).setColor(ColorConstants.WHITE));
        } else {
            Component chunks = Component.translatable(chunksKey, frontier.getChunkCount());
            dataRow1sub.addChild(new StringWidget(chunks, font).setColor(ColorConstants.WHITE));
        }

        Component owner = Component.translatable(ownerKey, frontier.getOwner().toString());
        dataRow1sub.addChild(new StringWidget(owner, font).setColor(ColorConstants.WHITE));

        Component dimension = Component.translatable(dimensionKey, frontier.getDimension().location().toString());
        dataRow1.addChild(new StringWidget(dimension, font).setColor(ColorConstants.TEXT_DIMENSION));

        LinearLayout dataRow2Col1 = LinearLayout.vertical();
        mainLayout.addChild(dataRow2Col1, 1, 2, LayoutSettings.defaults().alignHorizontallyLeft());

        Component area = Component.translatable(areaKey, frontier.area);
        dataRow2Col1.addChild(new StringWidget(area, font).setColor(ColorConstants.WHITE));

        Component perimeter = Component.translatable(perimeterKey, frontier.perimeter);
        dataRow2Col1.addChild(new StringWidget(perimeter, font).setColor(ColorConstants.WHITE));

        LinearLayout dataRow2Col2 = LinearLayout.vertical();
        mainLayout.addChild(dataRow2Col2, 1, 3, LayoutSettings.defaults().alignHorizontallyLeft());

        if (frontier.getCreated() != null) {
            Component created = Component.translatable(createdKey, dateFormat.format(frontier.getCreated()));
            dataRow2Col2.addChild(new StringWidget(created, font).setColor(ColorConstants.WHITE));
        }

        if (frontier.getModified() != null) {
            Component modified = Component.translatable(modifiedKey, dateFormat.format(frontier.getModified()));
            modifiedLabel = dataRow2Col2.addChild(new StringWidget(modified, font).setColor(ColorConstants.WHITE));
        }

        GridLayout visibilityCol1 = new GridLayout().rowSpacing(4);
        visibilityCol1.defaultCellSetting().alignHorizontallyLeft();
        visibilityCol1.addChild(SpacerElement.width(116), 0, 0);
        mainLayout.addChild(visibilityCol1, 2, 1);

        visibilityCol1.addChild(new StringWidget(showFrontierLabel, font).setColor(ColorConstants.TEXT), 0, 0);
        buttonVisible = visibilityCol1.addChild(createVisibilityOptionButton(frontier.getVisible(), frontier::setVisible), 0, 1);

        visibilityCol1.addChild(new StringWidget(announceInChatLabel, font).setColor(ColorConstants.TEXT), 1, 0);
        buttonAnnounceInChat = visibilityCol1.addChild(createVisibilityOptionButton(frontier.getAnnounceInChat(), frontier::setAnnounceInChat), 1, 1);

        visibilityCol1.addChild(new StringWidget(announceInTitleLabel, font).setColor(ColorConstants.TEXT), 2, 0);
        buttonAnnounceInTitle = visibilityCol1.addChild(createVisibilityOptionButton(frontier.getAnnounceInTitle(), frontier::setAnnounceInTitle), 2, 1);

        GridLayout visibilityCol2 = new GridLayout().rowSpacing(4);
        visibilityCol2.defaultCellSetting().alignHorizontallyLeft();
        visibilityCol2.addChild(SpacerElement.width(116), 0, 0);
        mainLayout.addChild(visibilityCol2, 2, 2);

        visibilityCol2.addChild(new StringWidget(showFullscreenLabel, font).setColor(ColorConstants.TEXT), 0, 0);
        buttonFullscreenVisible = visibilityCol2.addChild(createVisibilityOptionButton(frontier.getFullscreenVisible(), frontier::setFullscreenVisible), 0, 1);

        visibilityCol2.addChild(new StringWidget(showNameLabel, font).setColor(ColorConstants.TEXT), 1, 0);
        buttonFullscreenNameVisible = visibilityCol2.addChild(createVisibilityOptionButton(frontier.getFullscreenNameVisible(), frontier::setFullscreenNameVisible), 1, 1);

        visibilityCol2.addChild(new StringWidget(showOwnerLabel, font).setColor(ColorConstants.TEXT), 2, 0);
        buttonFullscreenOwnerVisible = visibilityCol2.addChild(createVisibilityOptionButton(frontier.getFullscreenOwnerVisible(), frontier::setFullscreenOwnerVisible), 2, 1);

        GridLayout visibilityCol3 = new GridLayout().rowSpacing(4);
        visibilityCol3.defaultCellSetting().alignHorizontallyLeft();
        visibilityCol3.addChild(SpacerElement.width(116), 0, 0);
        mainLayout.addChild(visibilityCol3, 2, 3);

        visibilityCol3.addChild(new StringWidget(showMinimapLabel, font).setColor(ColorConstants.TEXT), 0, 0);
        buttonMinimapVisible = visibilityCol3.addChild(createVisibilityOptionButton(frontier.getMinimapVisible(), frontier::setMinimapVisible), 0, 1);

        visibilityCol3.addChild(new StringWidget(showNameLabel, font).setColor(ColorConstants.TEXT), 1, 0);
        buttonMinimapNameVisible = visibilityCol3.addChild(createVisibilityOptionButton(frontier.getMinimapNameVisible(), frontier::setMinimapNameVisible), 1, 1);

        visibilityCol3.addChild(new StringWidget(showOwnerLabel, font).setColor(ColorConstants.TEXT), 2, 0);
        buttonMinimapOwnerVisible = visibilityCol3.addChild(createVisibilityOptionButton(frontier.getMinimapOwnerVisible(), frontier::setMinimapOwnerVisible), 2, 1);

        colorPicker = new ColorPicker(frontier.getColor(), (color, dragging) -> {
            colorPalette.setColor(color);
            colorPickerUpdated(color, dragging);
        });
        mainLayout.addChild(colorPicker, 3, 1, LayoutSettings.defaults().alignVerticallyBottom());

        LinearLayout colorCol = LinearLayout.vertical().spacing(4);
        colorCol.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(colorCol, 3, 2);

        colorCol.addChild(new StringWidget(colorLabel, font).setColor(ColorConstants.INFO_LABEL_TEXT), LayoutSettings.defaults().alignHorizontallyLeft());

        LinearLayout rgbRow = LinearLayout.horizontal().spacing(3);
        rgbRow.defaultCellSetting().alignVerticallyMiddle();
        colorCol.addChild(rgbRow);

        rgbRow.addChild(new StringWidget(rLabel, font, 8).setColor(ColorConstants.INFO_LABEL_TEXT));
        textRed = new TextBoxInt(0, 0, 255, font, 29);
        textRed.setHeight(20);
        textRed.setWidth(34);
        textRed.setValueChangedCallback(value -> {
            int newColor = (frontier.getColor() & 0xff00ffff) | (value << 16);
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                colorPicker.setColor(newColor);
                colorPalette.setColor(newColor);
                sendChangesToServer();
            }
        });
        rgbRow.addChild(textRed);
        rgbRow.addChild(SpacerElement.width(1));

        rgbRow.addChild(new StringWidget(gLabel, font, 8).setColor(ColorConstants.INFO_LABEL_TEXT));
        textGreen = new TextBoxInt(0, 0, 255, font, 29);
        textGreen.setHeight(20);
        textGreen.setWidth(34);
        textGreen.setValueChangedCallback(value -> {
            int newColor = (frontier.getColor() & 0xffff00ff) | (value << 8);
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                colorPicker.setColor(newColor);
                colorPalette.setColor(newColor);
                sendChangesToServer();
            }
        });
        rgbRow.addChild(textGreen);
        rgbRow.addChild(SpacerElement.width(1));

        rgbRow.addChild(new StringWidget(bLabel, font, 8).setColor(ColorConstants.INFO_LABEL_TEXT));
        textBlue = new TextBoxInt(0, 0, 255, font, 29);
        textBlue.setHeight(20);
        textBlue.setWidth(34);
        textBlue.setValueChangedCallback(value -> {
            int newColor = (frontier.getColor() & 0xffffff00) | value;
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                colorPicker.setColor(newColor);
                colorPalette.setColor(newColor);
                sendChangesToServer();
            }
        });
        rgbRow.addChild(textBlue);

        textRed.setValue((frontier.getColor() & 0xff0000) >> 16);
        textGreen.setValue((frontier.getColor() & 0x00ff00) >> 8);
        textBlue.setValue(frontier.getColor() & 0x0000ff);

        buttonRandomColor = new SimpleButton(font, 144, randomColorLabel, (b) -> {
            int newColor = ColorHelper.getRandomColor();
            frontier.setColor(newColor);
            colorPicker.setColor(newColor);
            textRed.setValue((newColor & 0xff0000) >> 16);
            textGreen.setValue((newColor & 0x00ff00) >> 8);
            textBlue.setValue(newColor & 0x0000ff);
            sendChangesToServer();
        });
        colorCol.addChild(buttonRandomColor);

        colorPalette = new ColorPaletteWidget(frontier.getColor(), (color) -> {
            colorPicker.setColor(color);
            colorPickerUpdated(color, false);
        });
        colorCol.addChild(colorPalette);

        GridLayout editCol = new GridLayout().rowSpacing(4);
        editCol.defaultCellSetting().alignHorizontallyLeft();
        editCol.addChild(SpacerElement.width(116), 0, 0);
        mainLayout.addChild(editCol, 3, 3, LayoutSettings.defaults().alignVerticallyBottom());

        labelPasteName = editCol.addChild(new StringWidget(pasteNameLabel, font).setColor(ColorConstants.TEXT), 0, 0);
        buttonPasteName = editCol.addChild(createVisibilityOptionButton(Config.pasteName, (value) -> Config.pasteName = value), 0, 1);

        labelPasteVisibility = editCol.addChild(new StringWidget(pasteVisibilityLabel, font).setColor(ColorConstants.TEXT), 1, 0);
        buttonPasteVisibility = editCol.addChild(createVisibilityOptionButton(Config.pasteVisibility, (value) -> Config.pasteVisibility = value), 1, 1);

        labelPasteColor = editCol.addChild(new StringWidget(pasteColorLabel, font).setColor(ColorConstants.TEXT), 2, 0);
        buttonPasteColor = editCol.addChild(createVisibilityOptionButton(Config.pasteColor, (value) -> Config.pasteColor = value), 2, 1);

        labelPasteBanner = editCol.addChild(new StringWidget(pasteBannerLabel, font).setColor(ColorConstants.TEXT), 3, 0);
        buttonPasteBanner = editCol.addChild(createVisibilityOptionButton(Config.pasteBanner, (value) -> Config.pasteBanner = value), 3, 1);

        LinearLayout editButtons = LinearLayout.horizontal().spacing(3);
        editCol.addChild(editButtons, 4, 0);

        buttonCopy = editButtons.addChild(new IconButton(IconButton.Type.Copy, (b) -> {
            MapFrontiersClient.setClipboard(frontier);
            updatePasteOptionsVisibility();
        }));
        buttonCopy.setTooltip(copyTooltip);

        LinearLayout pasteButtons = LinearLayout.horizontal();
        editButtons.addChild(pasteButtons);

        buttonPaste = pasteButtons.addChild(new IconButton(IconButton.Type.Paste, (b) -> {
            FrontierData clipboard = MapFrontiersClient.getClipboard();
            if (clipboard != null && (Config.pasteName || Config.pasteVisibility || Config.pasteColor || Config.pasteBanner)) {
                setFrontier(clipboard, Config.pasteName, Config.pasteVisibility, Config.pasteColor, Config.pasteBanner);
                sendChangesToServer();
                rebuildWidgets();
                repositionElements();
            }
        }));
        buttonPaste.setTooltip(pasteTooltip);

        buttonPasteOptions = pasteButtons.addChild(new IconButton(IconButton.Type.ArrowUp, (b) -> {
            Config.pasteOptionsVisible = !Config.pasteOptionsVisible;
            updatePasteOptionsVisibility();
            ClientEventHandler.postUpdatedConfigEvent();
        }));
        buttonPasteOptions.setTooltip(openPasteTooltip);

        buttonUndo = editButtons.addChild(new IconButton(IconButton.Type.Undo, (b) -> undo()));
        buttonUndo.setTooltip(undoTooltip);
        buttonRedo = editButtons.addChild(new IconButton(IconButton.Type.Redo, (b) -> redo()));
        buttonRedo.setTooltip(redoTooltip);

        buttonSelect = bottomButtons.addChild(new SimpleButton(font, 144, selectInMapLabel, (b) -> {
            BlockPos center = frontier.getCenter();
            closeAndReturnToFullscreenMap();
            Services.JOURNEYMAP.fullscreenMapCenterOn(center.getX(), center.getZ());
        }));
        buttonShareSettings = bottomButtons.addChild(new SimpleButton(font, 144, shareSettingsLabel, (b) -> new ShareSettings(frontiersOverlayManager, frontier).display()));
        buttonDelete = bottomButtons.addChild(new SimpleButton(font, 144, deleteLabel, (b) -> {
            // Unsubscribing to not receive this same event.
            ClientEventHandler.unsuscribeAllEvents(this);
            frontiersOverlayManager.clientDeleteFrontier(frontier);
            onClose();
        }));
        buttonDelete.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonDone = bottomButtons.addChild(new SimpleButton(font, 144, doneLabel, (b) -> onClose()));

        updateBannerButton();
        updateButtons();
        updatePasteOptionsVisibility();
        updateUndoRedoVisibility();
    }

    private OptionButton createVisibilityOptionButton(boolean defaultValue, Consumer<Boolean> consumer) {
        OptionButton button = new OptionButton(font, 28, (b) -> {
            consumer.accept(b.getSelected() == 0);
            sendChangesToServer();
        });
        button.addOption(onLabel);
        button.addOption(offLabel);
        button.setSelected(defaultValue ? 0 : 1);
        return button;
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
    }

    @Override
    public void renderScaledScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (frontier.hasBanner()) {
            frontier.renderBanner(minecraft, graphics, buttonBanner.getX() + buttonBanner.getWidth() / 2 - 44, buttonBanner.getY() + 20, 4);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (GuiEventListener w : children()) {
            if (w instanceof ColorPicker) {
                w.mouseReleased(mouseX, mouseY, button);
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_Z && Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown()) {
            undo();
            return true;
        } else if (key == GLFW.GLFW_KEY_Z && Screen.hasControlDown() && Screen.hasShiftDown() && !Screen.hasAltDown()) {
            redo();
            return true;
        } else {
            return super.keyPressed(key, value, modifier);
        }
    }

    @Override
    public void onClose() {
        sendChangesToServer();
        ClientEventHandler.unsuscribeAllEvents(this);
        super.onClose();
    }

    private void colorPickerUpdated(int color, boolean dragging) {
        frontier.setColor(color);
        textRed.setValue((frontier.getColor() & 0xff0000) >> 16);
        textGreen.setValue((frontier.getColor() & 0x00ff00) >> 8);
        textBlue.setValue(frontier.getColor() & 0x0000ff);

        if (!dragging) {
            sendChangesToServer();
        }
    }

    private void undo() {
        if (undoStack.size() == 1) {
            return;
        }

        redoStack.push(undoStack.pop());
        setFrontier(undoStack.peek(), true, true, true, true);
        sendChangesToServer();
        rebuildWidgets();
        repositionElements();
    }

    private void redo() {
        if (redoStack.empty()) {
            return;
        }

        setFrontier(redoStack.peek(), true, true, true, true);
        undoStack.push(redoStack.pop());
        sendChangesToServer();
        rebuildWidgets();
        repositionElements();
    }

    private void setFrontier(FrontierData other, boolean name, boolean visibility, boolean color, boolean banner) {
        if (name) {
            frontier.setName1(other.getName1());
            frontier.setName2(other.getName2());
        }
        if (visibility) {
            frontier.setVisible(other.getVisible());
            frontier.setFullscreenVisible(other.getFullscreenVisible());
            frontier.setFullscreenNameVisible(other.getFullscreenNameVisible());
            frontier.setFullscreenOwnerVisible(other.getFullscreenOwnerVisible());
            frontier.setMinimapVisible(other.getMinimapVisible());
            frontier.setMinimapNameVisible(other.getMinimapNameVisible());
            frontier.setMinimapOwnerVisible(other.getMinimapOwnerVisible());
            frontier.setAnnounceInChat(other.getAnnounceInChat());
            frontier.setAnnounceInTitle(other.getAnnounceInTitle());
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
                buttonBanner.setMessage(assignBannerLabel);
                buttonBanner.setTooltip(null);
            } else {
                buttonBanner.setMessage(assignBannerWarnLabel);
                buttonBanner.setTooltip(assignBannerWarnTooltip);
            }
        } else {
            buttonBanner.setMessage(removeBannerLabel);
            buttonBanner.setTooltip(null);
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

    private void updateButtons() {
        if (minecraft.player == null) {
            return;
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(minecraft.player);
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontier, playerUser);

        textName1.setEditable(actions.canUpdate);
        textName2.setEditable(actions.canUpdate);
        buttonVisible.active = actions.canUpdate;
        buttonFullscreenVisible.active = actions.canUpdate;
        buttonFullscreenNameVisible.active = actions.canUpdate;
        buttonFullscreenOwnerVisible.active = actions.canUpdate;
        buttonMinimapVisible.active = actions.canUpdate;
        buttonMinimapNameVisible.active = actions.canUpdate;
        buttonMinimapOwnerVisible.active = actions.canUpdate;
        buttonAnnounceInChat.active = actions.canUpdate;
        buttonAnnounceInTitle.active = actions.canUpdate;
        textRed.setEditable(actions.canUpdate);
        textGreen.setEditable(actions.canUpdate);
        textBlue.setEditable(actions.canUpdate);
        buttonRandomColor.active = actions.canUpdate;
        colorPicker.active = actions.canUpdate;
        colorPalette.active = actions.canUpdate;
        buttonPaste.active = actions.canUpdate;
        buttonPasteOptions.active = actions.canUpdate;
        buttonDelete.active = actions.canDelete;
        buttonBanner.visible = actions.canUpdate;
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        buttonSelect.active = uiState != null && frontier.getDimension().equals(uiState.dimension);
        buttonShareSettings.active = actions.canShare;
    }

    private void updatePasteOptionsVisibility() {
        buttonPaste.visible = buttonPaste.active && MapFrontiersClient.getClipboard() != null;
        buttonPasteOptions.visible = buttonPaste.visible;
        buttonPasteOptions.setType(Config.pasteOptionsVisible ? IconButton.Type.ArrowDown : IconButton.Type.ArrowUp);
        buttonPasteOptions.setTooltip(Config.pasteOptionsVisible ? closePasteTooltip : openPasteTooltip);
        buttonPasteName.visible = buttonPaste.visible && Config.pasteOptionsVisible;
        buttonPasteVisibility.visible = buttonPaste.visible && Config.pasteOptionsVisible;
        buttonPasteColor.visible = buttonPaste.visible && Config.pasteOptionsVisible;
        buttonPasteBanner.visible = buttonPaste.visible && Config.pasteOptionsVisible;
        labelPasteName.visible = buttonPaste.visible && Config.pasteOptionsVisible;
        labelPasteVisibility.visible = buttonPaste.visible && Config.pasteOptionsVisible;
        labelPasteColor.visible = buttonPaste.visible && Config.pasteOptionsVisible;
        labelPasteBanner.visible = buttonPaste.visible && Config.pasteOptionsVisible;
    }

    private void updateUndoRedoVisibility() {
        buttonUndo.visible = buttonPaste.active && undoStack.size() > 1;
        buttonRedo.visible = buttonPaste.active && redoStack.size() > 0;
    }

    private void sendChangesToServer() {
        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(minecraft.player);
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontier, playerUser);

        if (actions.canUpdate) {
            frontiersOverlayManager.clientUpdateFrontier(frontier);
        }
    }

    private void addToUndo(FrontierData frontier) {
        boolean add = undoStack.empty();
        if (!add) {
            FrontierData u = undoStack.peek();
            if (!Objects.equals(u.getName1(), (frontier.getName1()))
                    || !Objects.equals(u.getName2(), (frontier.getName2()))
                    || u.getVisible() != frontier.getVisible()
                    || u.getFullscreenVisible() != frontier.getFullscreenVisible()
                    || u.getFullscreenNameVisible() != frontier.getFullscreenNameVisible()
                    || u.getFullscreenOwnerVisible() != frontier.getFullscreenOwnerVisible()
                    || u.getMinimapVisible() != frontier.getMinimapVisible()
                    || u.getMinimapNameVisible() != frontier.getMinimapNameVisible()
                    || u.getMinimapOwnerVisible() != frontier.getMinimapOwnerVisible()
                    || u.getAnnounceInChat() != frontier.getAnnounceInChat()
                    || u.getAnnounceInTitle() != frontier.getAnnounceInTitle()
                    || u.getColor() != frontier.getColor()
                    || !Objects.equals(u.getbannerData(), frontier.getbannerData())) {
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
