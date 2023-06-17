package games.alejandrocoria.mapfrontiers.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPicker;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.common.util.StringHelper;
import games.alejandrocoria.mapfrontiers.platform.Services;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import journeymap.client.api.util.UIState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

@ParametersAreNonnullByDefault
public class FrontierInfo extends Screen {
    static final DateFormat dateFormat = new SimpleDateFormat();

    private final IClientAPI jmAPI;
    private final Runnable afterClose;

    private float scaleFactor;
    private int actualWidth;
    private int actualHeight;

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
    private IconButton buttonCopy;
    private IconButton buttonPaste;
    private IconButton buttonOpenPasteOptions;
    private IconButton buttonClosePasteOptions;
    private OptionButton buttonPasteName;
    private OptionButton buttonPasteVisibility;
    private OptionButton buttonPasteColor;
    private OptionButton buttonPasteBanner;
    private SimpleLabel labelPasteName;
    private SimpleLabel labelPasteVisibility;
    private SimpleLabel labelPasteColor;
    private SimpleLabel labelPasteBanner;
    private IconButton buttonUndo;
    private IconButton buttonRedo;

    private SimpleButton buttonSelect;
    private SimpleButton buttonShareSettings;
    private SimpleButton buttonDelete;
    private SimpleButton buttonDone;
    private SimpleButton buttonBanner;

    private final List<SimpleLabel> labels;
    private SimpleLabel modifiedLabel;

    private final Stack<FrontierData> undoStack = new Stack<>();
    private final Stack<FrontierData> redoStack = new Stack<>();

    public FrontierInfo(IClientAPI jmAPI, FrontierOverlay frontier) {
        this(jmAPI, frontier, null);
    }

    public FrontierInfo(IClientAPI jmAPI, FrontierOverlay frontier, @Nullable  Runnable afterClose) {
        super(Component.translatable("mapfrontiers.title_info"));
        this.jmAPI = jmAPI;
        this.afterClose = afterClose;
        frontiersOverlayManager = MapFrontiersClient.getFrontiersOverlayManager(frontier.getPersonal());
        this.frontier = frontier;
        labels = new ArrayList<>();
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
                    init(minecraft, width, height);
                } else {
                    if (frontier.getModified() != null) {
                        Component modified = Component.translatable("mapfrontiers.modified", dateFormat.format(frontier.getModified()));
                        modifiedLabel.setText(modified);
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
    public void init() {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(minecraft, this, 627, 336);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);

        int leftSide = actualWidth / 2 - 154;
        int rightSide = actualWidth / 2 + 10;
        int top = actualHeight / 2 - 142;

        labels.clear();

        labels.add(new SimpleLabel(font, leftSide, top, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.name"), ColorConstants.INFO_LABEL_TEXT));

        textName1 = new TextBox(font, leftSide, top + 12, 144);
        textName1.setMaxLength(17);
        textName1.setHeight(20);
        textName1.setValue(frontier.getName1());
        textName1.setLostFocusCallback(value -> sendChangesToServer());
        textName1.setValueChangedCallback(value -> {
            if (!frontier.getName1().equals(value)) {
                frontier.setName1(value);
            }
        });
        textName2 = new TextBox(font, leftSide, top + 40, 144);
        textName2.setMaxLength(17);
        textName2.setHeight(20);
        textName2.setValue(frontier.getName2());
        textName2.setLostFocusCallback(value -> sendChangesToServer());
        textName2.setValueChangedCallback(value -> {
            if (!frontier.getName2().equals(value)) {
                frontier.setName2(value);
            }
        });

        labels.add(new SimpleLabel(font, leftSide, top + 70, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_frontier"), ColorConstants.TEXT));
        buttonVisible = new OptionButton(font, leftSide + 116, top + 68, 28, this::buttonPressed);
        buttonVisible.addOption(Component.translatable("options.on"));
        buttonVisible.addOption(Component.translatable("options.off"));
        buttonVisible.setSelected(frontier.getVisible() ? 0 : 1);

        labels.add(new SimpleLabel(font, rightSide, top + 70, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_fullscreen"), ColorConstants.TEXT));
        buttonFullscreenVisible = new OptionButton(font, rightSide + 116, top + 68, 28, this::buttonPressed);
        buttonFullscreenVisible.addOption(Component.translatable("options.on"));
        buttonFullscreenVisible.addOption(Component.translatable("options.off"));
        buttonFullscreenVisible.setSelected(frontier.getFullscreenVisible() ? 0 : 1);

        labels.add(new SimpleLabel(font, rightSide, top + 86, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_name"), ColorConstants.TEXT));
        buttonFullscreenNameVisible = new OptionButton(font, rightSide + 116, top + 84, 28, this::buttonPressed);
        buttonFullscreenNameVisible.addOption(Component.translatable("options.on"));
        buttonFullscreenNameVisible.addOption(Component.translatable("options.off"));
        buttonFullscreenNameVisible.setSelected(frontier.getFullscreenNameVisible() ? 0 : 1);

        labels.add(new SimpleLabel(font, rightSide, top + 102, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_owner"), ColorConstants.TEXT));
        buttonFullscreenOwnerVisible = new OptionButton(font, rightSide + 116, top + 100, 28, this::buttonPressed);
        buttonFullscreenOwnerVisible.addOption(Component.translatable("options.on"));
        buttonFullscreenOwnerVisible.addOption(Component.translatable("options.off"));
        buttonFullscreenOwnerVisible.setSelected(frontier.getFullscreenOwnerVisible() ? 0 : 1);

        labels.add(new SimpleLabel(font, rightSide + 154, top + 70, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_minimap"), ColorConstants.TEXT));
        buttonMinimapVisible = new OptionButton(font, rightSide + 154 + 116, top + 68, 28, this::buttonPressed);
        buttonMinimapVisible.addOption(Component.translatable("options.on"));
        buttonMinimapVisible.addOption(Component.translatable("options.off"));
        buttonMinimapVisible.setSelected(frontier.getMinimapVisible() ? 0 : 1);

        labels.add(new SimpleLabel(font, rightSide + 154, top + 86, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_name"), ColorConstants.TEXT));
        buttonMinimapNameVisible = new OptionButton(font, rightSide + 154 + 116, top + 84, 28, this::buttonPressed);
        buttonMinimapNameVisible.addOption(Component.translatable("options.on"));
        buttonMinimapNameVisible.addOption(Component.translatable("options.off"));
        buttonMinimapNameVisible.setSelected(frontier.getMinimapNameVisible() ? 0 : 1);

        labels.add(new SimpleLabel(font, rightSide + 154, top + 102, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_owner"), ColorConstants.TEXT));
        buttonMinimapOwnerVisible = new OptionButton(font, rightSide + 154 + 116, top + 100, 28, this::buttonPressed);
        buttonMinimapOwnerVisible.addOption(Component.translatable("options.on"));
        buttonMinimapOwnerVisible.addOption(Component.translatable("options.off"));
        buttonMinimapOwnerVisible.setSelected(frontier.getMinimapOwnerVisible() ? 0 : 1);

        labels.add(new SimpleLabel(font, leftSide, top + 86, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.announce_in_chat"), ColorConstants.TEXT));
        buttonAnnounceInChat = new OptionButton(font, leftSide + 116, top + 84, 28, this::buttonPressed);
        buttonAnnounceInChat.addOption(Component.translatable("options.on"));
        buttonAnnounceInChat.addOption(Component.translatable("options.off"));
        buttonAnnounceInChat.setSelected(frontier.getAnnounceInChat() ? 0 : 1);

        labels.add(new SimpleLabel(font, leftSide, top + 102, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.announce_in_title"), ColorConstants.TEXT));
        buttonAnnounceInTitle = new OptionButton(font, leftSide + 116, top + 100, 28, this::buttonPressed);
        buttonAnnounceInTitle.addOption(Component.translatable("options.on"));
        buttonAnnounceInTitle.addOption(Component.translatable("options.off"));
        buttonAnnounceInTitle.setSelected(frontier.getAnnounceInTitle() ? 0 : 1);

        labels.add(new SimpleLabel(font, rightSide, top + 152, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.color"), ColorConstants.INFO_LABEL_TEXT));

        labels.add(new SimpleLabel(font, rightSide - 11, top + 170, SimpleLabel.Align.Left,
                Component.literal("R"), ColorConstants.INFO_LABEL_TEXT));

        textRed = new TextBoxInt(0, 0, 255, font, rightSide, top + 164, 29);
        textRed.setHeight(20);
        textRed.setWidth(34);
        textRed.setValueChangedCallback(value -> {
            int newColor = (frontier.getColor() & 0xff00ffff) | (value << 16);
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                colorPicker.setColor(newColor);
                sendChangesToServer();
            }
        });

        labels.add(new SimpleLabel(font, rightSide + 44, top + 170, SimpleLabel.Align.Left,
                Component.literal("G"), ColorConstants.INFO_LABEL_TEXT));

        textGreen = new TextBoxInt(0, 0, 255, font, rightSide + 55, top + 164, 29);
        textGreen.setHeight(20);
        textGreen.setWidth(34);
        textGreen.setValueChangedCallback(value -> {
            int newColor = (frontier.getColor() & 0xffff00ff) | (value << 8);
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                colorPicker.setColor(newColor);
                sendChangesToServer();
            }
        });

        labels.add(new SimpleLabel(font, rightSide + 99, top + 170, SimpleLabel.Align.Left,
                Component.literal("B"), ColorConstants.INFO_LABEL_TEXT));

        textBlue = new TextBoxInt(0, 0, 255, font, rightSide + 110, top + 164, 29);
        textBlue.setHeight(20);
        textBlue.setWidth(34);
        textBlue.setValueChangedCallback(value -> {
            int newColor = (frontier.getColor() & 0xffffff00) | value;
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                colorPicker.setColor(newColor);
                sendChangesToServer();
            }
        });

        textRed.setValue((frontier.getColor() & 0xff0000) >> 16);
        textGreen.setValue((frontier.getColor() & 0x00ff00) >> 8);
        textBlue.setValue(frontier.getColor() & 0x0000ff);

        buttonRandomColor = new SimpleButton(font, rightSide, top + 190, 145,
                Component.translatable("mapfrontiers.random_color"), this::buttonPressed);

        colorPicker = new ColorPicker(leftSide + 2, top + 156, frontier.getColor(), (picker, dragging) -> colorPickerUpdated(dragging));

        buttonCopy = new IconButton(rightSide + 154, top + 263, IconButton.Type.Copy, this::buttonPressed);
        buttonPaste = new IconButton(rightSide + 174, top + 263, IconButton.Type.Paste, this::buttonPressed);
        buttonOpenPasteOptions = new IconButton(rightSide + 191, top + 263, IconButton.Type.ArrowUp, this::buttonPressed);
        buttonClosePasteOptions = new IconButton(rightSide + 191, top + 263, IconButton.Type.ArrowDown, this::buttonPressed);

        labelPasteName = new SimpleLabel(font, rightSide + 154, top + 200, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.paste_name"), ColorConstants.TEXT);
        labels.add(labelPasteName);
        buttonPasteName = new OptionButton(font, rightSide + 154 + 116, top + 198, 28, this::buttonPressed);
        buttonPasteName.addOption(Component.translatable("options.on"));
        buttonPasteName.addOption(Component.translatable("options.off"));
        buttonPasteName.setSelected(Config.pasteName ? 0 : 1);

        labelPasteVisibility = new SimpleLabel(font, rightSide + 154, top + 216, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.paste_visibility"), ColorConstants.TEXT);
        labels.add(labelPasteVisibility);
        buttonPasteVisibility = new OptionButton(font, rightSide + 154 + 116, top + 214, 28, this::buttonPressed);
        buttonPasteVisibility.addOption(Component.translatable("options.on"));
        buttonPasteVisibility.addOption(Component.translatable("options.off"));
        buttonPasteVisibility.setSelected(Config.pasteVisibility ? 0 : 1);

        labelPasteColor = new SimpleLabel(font, rightSide + 154, top + 232, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.paste_color"), ColorConstants.TEXT);
        labels.add(labelPasteColor);
        buttonPasteColor = new OptionButton(font, rightSide + 154 + 116, top + 230, 28, this::buttonPressed);
        buttonPasteColor.addOption(Component.translatable("options.on"));
        buttonPasteColor.addOption(Component.translatable("options.off"));
        buttonPasteColor.setSelected(Config.pasteColor ? 0 : 1);

        labelPasteBanner = new SimpleLabel(font, rightSide + 154, top + 248, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.paste_banner"), ColorConstants.TEXT);
        labels.add(labelPasteBanner);
        buttonPasteBanner = new OptionButton(font, rightSide + 154 + 116, top + 246, 28, this::buttonPressed);
        buttonPasteBanner.addOption(Component.translatable("options.on"));
        buttonPasteBanner.addOption(Component.translatable("options.off"));
        buttonPasteBanner.setSelected(Config.pasteBanner ? 0 : 1);

        buttonUndo = new IconButton(rightSide + 202, top + 263, IconButton.Type.Undo, this::buttonPressed);
        buttonRedo = new IconButton(rightSide + 222, top + 263, IconButton.Type.Redo, this::buttonPressed);

        int offset1 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Personal")),
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Global"))) + 10;

        int offset2 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.vertices", 9999),
                I18n.get("mapfrontiers.chunks", 9999)) + 10;

        Component type = Component.translatable("mapfrontiers.type",
                Component.translatable(frontier.getPersonal() ? "mapfrontiers.config.Personal" : "mapfrontiers.config.Global"));
        labels.add(new SimpleLabel(font, rightSide, top, SimpleLabel.Align.Left, type, ColorConstants.WHITE));

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            Component vertices = Component.translatable("mapfrontiers.vertices", frontier.getVertexCount());
            labels.add(new SimpleLabel(font, rightSide + offset1, top, SimpleLabel.Align.Left, vertices, ColorConstants.WHITE));
        } else {
            Component chunks = Component.translatable("mapfrontiers.chunks", frontier.getChunkCount());
            labels.add(new SimpleLabel(font, rightSide + offset1, top, SimpleLabel.Align.Left, chunks, ColorConstants.WHITE));
        }

        Component owner = Component.translatable("mapfrontiers.owner", frontier.getOwner());
        labels.add(new SimpleLabel(font, rightSide + offset1 + offset2, top, SimpleLabel.Align.Left, owner, ColorConstants.WHITE));

        Component dimension = Component.translatable("mapfrontiers.dimension", frontier.getDimension().location().toString());
        labels.add(new SimpleLabel(font, rightSide, top + 10, SimpleLabel.Align.Left, dimension, ColorConstants.TEXT_DIMENSION));

        Component area = Component.translatable("mapfrontiers.area", frontier.area);
        labels.add(new SimpleLabel(font, rightSide, top + 32, SimpleLabel.Align.Left, area, ColorConstants.WHITE));

        Component perimeter = Component.translatable("mapfrontiers.perimeter", frontier.perimeter);
        labels.add(new SimpleLabel(font, rightSide, top + 42, SimpleLabel.Align.Left, perimeter, ColorConstants.WHITE));

        if (frontier.getCreated() != null) {
            Component created = Component.translatable("mapfrontiers.created", dateFormat.format(frontier.getCreated()));
            labels.add(new SimpleLabel(font, rightSide + 154, top + 32, SimpleLabel.Align.Left, created, ColorConstants.WHITE));
        }

        if (frontier.getModified() != null) {
            Component modified = Component.translatable("mapfrontiers.modified", dateFormat.format(frontier.getModified()));
            modifiedLabel = new SimpleLabel(font, rightSide + 154, top + 42, SimpleLabel.Align.Left, modified, ColorConstants.WHITE);
            labels.add(modifiedLabel);
        }

        buttonSelect = new SimpleButton(font, leftSide - 154, top + 290, 144,
                Component.translatable("mapfrontiers.select_in_map"), this::buttonPressed);
        buttonShareSettings = new SimpleButton(font, leftSide, top + 290, 144,
                Component.translatable("mapfrontiers.share_settings"), this::buttonPressed);
        buttonDelete = new SimpleButton(font, rightSide, top + 290, 144,
                Component.translatable("mapfrontiers.delete"), this::buttonPressed);
        buttonDelete.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonDone = new SimpleButton(font, rightSide + 154, top + 290, 144,
                Component.translatable("gui.done"), this::buttonPressed);

        buttonBanner = new SimpleButton(font, leftSide - 152, top, 144,
                Component.translatable("mapfrontiers.assign_banner"), this::buttonPressed);

        addRenderableWidget(textName1);
        addRenderableWidget(textName2);
        addRenderableWidget(buttonVisible);
        addRenderableWidget(buttonFullscreenVisible);
        addRenderableWidget(buttonFullscreenNameVisible);
        addRenderableWidget(buttonFullscreenOwnerVisible);
        addRenderableWidget(buttonMinimapVisible);
        addRenderableWidget(buttonMinimapNameVisible);
        addRenderableWidget(buttonMinimapOwnerVisible);
        addRenderableWidget(buttonAnnounceInChat);
        addRenderableWidget(buttonAnnounceInTitle);
        addRenderableWidget(textRed);
        addRenderableWidget(textGreen);
        addRenderableWidget(textBlue);
        addRenderableWidget(buttonRandomColor);
        addRenderableWidget(colorPicker);
        addRenderableWidget(buttonCopy);
        addRenderableWidget(buttonPaste);
        addRenderableWidget(buttonOpenPasteOptions);
        addRenderableWidget(buttonClosePasteOptions);
        addRenderableWidget(buttonPasteName);
        addRenderableWidget(buttonPasteVisibility);
        addRenderableWidget(buttonPasteColor);
        addRenderableWidget(buttonPasteBanner);
        addRenderableWidget(buttonUndo);
        addRenderableWidget(buttonRedo);
        addRenderableWidget(buttonSelect);
        addRenderableWidget(buttonShareSettings);
        addRenderableWidget(buttonDelete);
        addRenderableWidget(buttonDone);
        addRenderableWidget(buttonBanner);

        updateBannerButton();
        updateButtons();
        updatePasteOptionsVisibility();
        updateUndoRedoVisibility();
    }

    @Override
    public void tick() {
        textName1.tick();
        textName2.tick();
        textRed.tick();
        textGreen.tick();
        textBlue.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);

        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (scaleFactor != 1.f) {
            graphics.pose().pushPose();
            graphics.pose().scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

        graphics.drawCenteredString(font, title, this.actualWidth / 2, 8, ColorConstants.WHITE);
        super.render(graphics, mouseX, mouseY, partialTicks);

        if (frontier.hasBanner()) {
            frontier.renderBanner(minecraft, graphics, actualWidth / 2 - 276, actualHeight / 2 - 122, 4);
        }

        for (SimpleLabel label : labels) {
            if (label.visible) {
                label.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        if (buttonBanner.visible && buttonBanner.isHovered()) {
            if (!frontier.hasBanner() && getHeldBanner(minecraft) == null) {
                MutableComponent prefix = Component.literal(ColorConstants.WARNING + "! " + ChatFormatting.RESET);
                graphics.renderTooltip(font, prefix.append(Component.translatable("mapfrontiers.assign_banner_warn")), mouseX, mouseY);
            }
        } else if (buttonCopy.isHovered()) {
            graphics.renderTooltip(font, Component.translatable("mapfrontiers.copy"), mouseX, mouseY);
        } else if (buttonPaste.visible && buttonPaste.isHovered()) {
            graphics.renderTooltip(font, Component.translatable("mapfrontiers.paste"), mouseX, mouseY);
        } else if (buttonOpenPasteOptions.visible && buttonOpenPasteOptions.isHovered()) {
            graphics.renderTooltip(font, Component.translatable("mapfrontiers.open_paste_options"), mouseX, mouseY);
        } else if (buttonClosePasteOptions.visible && buttonClosePasteOptions.isHovered()) {
            graphics.renderTooltip(font, Component.translatable("mapfrontiers.close_paste_options"), mouseX, mouseY);
        } else if (buttonUndo.visible && buttonUndo.isHovered()) {
            graphics.renderTooltip(font, Component.translatable("mapfrontiers.undo"), mouseX, mouseY);
        } else if (buttonRedo.visible && buttonRedo.isHovered()) {
            graphics.renderTooltip(font, Component.translatable("mapfrontiers.redo"), mouseX, mouseY);
        }

        if (scaleFactor != 1.f) {
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (mouseButton == 0) {
            textName1.mouseClicked(mouseX, mouseY, mouseButton);
            textName2.mouseClicked(mouseX, mouseY, mouseButton);
            textRed.mouseClicked(mouseX, mouseY, mouseButton);
            textGreen.mouseClicked(mouseX, mouseY, mouseButton);
            textBlue.mouseClicked(mouseX, mouseY, mouseButton);
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        for (GuiEventListener w : children()) {
            if (w instanceof ColorPicker) {
                w.mouseReleased(mouseX, mouseY, button);
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX * scaleFactor, mouseY * scaleFactor, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX * scaleFactor, mouseY * scaleFactor, button, dragX * scaleFactor, dragY * scaleFactor);
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

    protected void buttonPressed(Button button) {
        if (button == buttonVisible) {
            frontier.setVisible(buttonVisible.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonFullscreenVisible) {
            frontier.setFullscreenVisible(buttonFullscreenVisible.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonFullscreenNameVisible) {
            frontier.setFullscreenNameVisible(buttonFullscreenNameVisible.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonFullscreenOwnerVisible) {
            frontier.setFullscreenOwnerVisible(buttonFullscreenOwnerVisible.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonMinimapVisible) {
            frontier.setMinimapVisible(buttonMinimapVisible.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonMinimapNameVisible) {
            frontier.setMinimapNameVisible(buttonMinimapNameVisible.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonMinimapOwnerVisible) {
            frontier.setMinimapOwnerVisible(buttonMinimapOwnerVisible.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonAnnounceInChat) {
            frontier.setAnnounceInChat(buttonAnnounceInChat.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonAnnounceInTitle) {
            frontier.setAnnounceInTitle(buttonAnnounceInTitle.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonRandomColor) {
            int newColor = ColorHelper.getRandomColor();
            frontier.setColor(newColor);
            colorPicker.setColor(newColor);
            textRed.setValue((newColor & 0xff0000) >> 16);
            textGreen.setValue((newColor & 0x00ff00) >> 8);
            textBlue.setValue(newColor & 0x0000ff);
            sendChangesToServer();
        } else if (button == buttonCopy) {
            MapFrontiersClient.setClipboard(frontier);
            updatePasteOptionsVisibility();
        } else if (button == buttonPaste) {
            FrontierData clipboard = MapFrontiersClient.getClipboard();
            if (clipboard != null && (Config.pasteName || Config.pasteVisibility || Config.pasteColor || Config.pasteBanner)) {
                setFrontier(clipboard, Config.pasteName, Config.pasteVisibility, Config.pasteColor, Config.pasteBanner);
                sendChangesToServer();
                init(minecraft, width, height);
            }
        } else if (button == buttonOpenPasteOptions) {
            Config.pasteOptionsVisible = true;
            updatePasteOptionsVisibility();
            ClientEventHandler.postUpdatedConfigEvent();
        } else if (button == buttonClosePasteOptions) {
            Config.pasteOptionsVisible = false;
            updatePasteOptionsVisibility();
            ClientEventHandler.postUpdatedConfigEvent();
        } else if (button == buttonPasteName) {
            Config.pasteName = buttonPasteName.getSelected() == 0;
            ClientEventHandler.postUpdatedConfigEvent();
        } else if (button == buttonPasteVisibility) {
            Config.pasteVisibility = buttonPasteVisibility.getSelected() == 0;
            ClientEventHandler.postUpdatedConfigEvent();
        } else if (button == buttonPasteColor) {
            Config.pasteColor = buttonPasteColor.getSelected() == 0;
            ClientEventHandler.postUpdatedConfigEvent();
        } else if (button == buttonPasteBanner) {
            Config.pasteBanner = buttonPasteBanner.getSelected() == 0;
            ClientEventHandler.postUpdatedConfigEvent();
        } else if (button == buttonUndo) {
            undo();
        } else if (button == buttonRedo) {
            redo();
        } else if (button == buttonSelect) {
            BlockPos center = frontier.getCenter();
            Services.PLATFORM.popGuiLayer();
            Services.JOURNEYMAP.fullscreenMapCenterOn(center.getX(), center.getZ());
        } else if (button == buttonShareSettings) {
            Services.PLATFORM.pushGuiLayer(new ShareSettings(frontiersOverlayManager, frontier));
        } else if (button == buttonDelete) {
            // Unsubscribing to not receive this same event.
            ClientEventHandler.unsuscribeAllEvents(this);
            frontiersOverlayManager.clientDeleteFrontier(frontier);
            onClose();
        } else if (button == buttonDone) {
            onClose();
        } else if (button == buttonBanner) {
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
        }
    }

    @Override
    public void removed() {
        sendChangesToServer();
        ClientEventHandler.unsuscribeAllEvents(this);
    }

    @Override
    public void onClose() {
        Services.PLATFORM.popGuiLayer();

        if (afterClose != null) {
            afterClose.run();
        }
    }

    private void colorPickerUpdated(boolean dragging) {
        frontier.setColor(colorPicker.getColor());
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
        init(minecraft, width, height);
    }

    private void redo() {
        if (redoStack.empty()) {
            return;
        }

        setFrontier(redoStack.peek(), true, true, true, true);
        undoStack.push(redoStack.pop());
        sendChangesToServer();
        init(minecraft, width, height);
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
            MutableComponent message = Component.translatable("mapfrontiers.assign_banner");
            if (getHeldBanner(minecraft) == null) {
                message.append(Component.literal(ColorConstants.WARNING + " !"));
            }

            buttonBanner.setMessage(message);
        } else {
            buttonBanner.setMessage(Component.translatable("mapfrontiers.remove_banner"));
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
        buttonRandomColor.visible = actions.canUpdate;
        colorPicker.active = actions.canUpdate;
        buttonPaste.active = actions.canUpdate;
        buttonOpenPasteOptions.active = actions.canUpdate;
        buttonClosePasteOptions.active = actions.canUpdate;
        buttonDelete.visible = actions.canDelete;
        buttonBanner.visible = actions.canUpdate;
        UIState uiState = jmAPI.getUIState(Context.UI.Fullscreen);
        buttonSelect.visible = uiState != null && frontier.getDimension().equals(uiState.dimension);
        buttonShareSettings.visible = actions.canShare;
    }

    private void updatePasteOptionsVisibility() {
        buttonPaste.visible = MapFrontiersClient.getClipboard() != null;
        buttonOpenPasteOptions.visible = buttonPaste.visible && !Config.pasteOptionsVisible;
        buttonClosePasteOptions.visible = buttonPaste.visible && Config.pasteOptionsVisible;
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
        frontiersOverlayManager.clientUpdatefrontier(frontier);
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
