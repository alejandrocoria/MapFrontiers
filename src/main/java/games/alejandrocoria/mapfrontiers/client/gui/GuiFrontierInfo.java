package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.client.util.StringHelper;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import journeymap.client.ui.ScreenLayerManager;
import journeymap.client.ui.UIManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
@Environment(EnvType.CLIENT)
public class GuiFrontierInfo extends Screen implements TextIntBox.TextIntBoxResponder, TextBox.TextBoxResponder {
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
    private GuiOptionButton buttonVisible;
    private GuiOptionButton buttonFullscreenVisible;
    private GuiOptionButton buttonFullscreenNameVisible;
    private GuiOptionButton buttonFullscreenOwnerVisible;
    private GuiOptionButton buttonMinimapVisible;
    private GuiOptionButton buttonMinimapNameVisible;
    private GuiOptionButton buttonMinimapOwnerVisible;
    private GuiOptionButton buttonAnnounceInChat;
    private GuiOptionButton buttonAnnounceInTitle;
    private TextIntBox textRed;
    private TextIntBox textGreen;
    private TextIntBox textBlue;
    private GuiSettingsButton buttonRandomColor;
    private GuiColorPicker colorPicker;
    private GuiButtonIcon buttonCopy;
    private GuiButtonIcon buttonPaste;
    private GuiButtonIcon buttonOpenPasteOptions;
    private GuiButtonIcon buttonClosePasteOptions;
    private GuiOptionButton buttonPasteName;
    private GuiOptionButton buttonPasteVisibililty;
    private GuiOptionButton buttonPasteColor;
    private GuiOptionButton buttonPasteBanner;
    private GuiSimpleLabel labelPasteName;
    private GuiSimpleLabel labelPasteVisibililty;
    private GuiSimpleLabel labelPasteColor;
    private GuiSimpleLabel labelPasteBanner;
    private GuiButtonIcon buttonUndo;
    private GuiButtonIcon buttonRedo;

    private GuiSettingsButton buttonSelect;
    private GuiSettingsButton buttonShareSettings;
    private GuiSettingsButton buttonDelete;
    private GuiSettingsButton buttonDone;
    private GuiSettingsButton buttonBanner;

    private final List<GuiSimpleLabel> labels;
    private GuiSimpleLabel modifiedLabel;

    private Stack<FrontierData> undoStack = new Stack();
    private Stack<FrontierData> redoStack = new Stack();

    public GuiFrontierInfo(IClientAPI jmAPI, FrontierOverlay frontier) {
        this(jmAPI, frontier, null);
    }

    public GuiFrontierInfo(IClientAPI jmAPI, FrontierOverlay frontier, @Nullable  Runnable afterClose) {
        super(Component.translatable("mapfrontiers.title_info"));
        this.jmAPI = jmAPI;
        this.afterClose = afterClose;
        frontiersOverlayManager = ClientProxy.getFrontiersOverlayManager(frontier.getPersonal());
        this.frontier = frontier;
        labels = new ArrayList<>();
        undoStack.push(new FrontierData(frontier));

        ClientProxy.subscribeDeletedFrontierEvent(this, frontierID -> {
            if (frontier.getId().equals(frontierID)) {
                onClose();
            }
        });

        ClientProxy.subscribeUpdatedFrontierEvent(this, (frontierOverlay, playerID) -> {
            if (frontier.getId().equals(frontierOverlay.getId())) {
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

        ClientProxy.subscribeUpdatedSettingsProfileEvent(this, profile -> {
            updateButtons();
            updateBannerButton();
        });
    }

    @Override
    public void init() {
        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        scaleFactor = ScreenHelper.getScaleFactorThatFit(this, 627, 336);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);

        int leftSide = actualWidth / 2 - 154;
        int rightSide = actualWidth / 2 + 10;
        int top = actualHeight / 2 - 142;

        labels.clear();

        labels.add(new GuiSimpleLabel(font, leftSide, top, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.name"), GuiColors.LABEL_TEXT));

        textName1 = new TextBox(font, leftSide, top + 12, 144);
        textName1.setMaxLength(17);
        textName1.setHeight(20);
        textName1.setResponder(this);
        textName1.setValue(frontier.getName1());
        textName2 = new TextBox(font, leftSide, top + 40, 144);
        textName2.setMaxLength(17);
        textName2.setHeight(20);
        textName2.setResponder(this);
        textName2.setValue(frontier.getName2());

        labels.add(new GuiSimpleLabel(font, leftSide, top + 70, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_frontier"), GuiColors.SETTINGS_TEXT));
        buttonVisible = new GuiOptionButton(font, leftSide + 116, top + 68, 28, this::buttonPressed);
        buttonVisible.addOption(Component.translatable("options.on"));
        buttonVisible.addOption(Component.translatable("options.off"));
        buttonVisible.setSelected(frontier.getVisible() ? 0 : 1);

        labels.add(new GuiSimpleLabel(font, rightSide, top + 70, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_fullscreen"), GuiColors.SETTINGS_TEXT));
        buttonFullscreenVisible = new GuiOptionButton(font, rightSide + 116, top + 68, 28, this::buttonPressed);
        buttonFullscreenVisible.addOption(Component.translatable("options.on"));
        buttonFullscreenVisible.addOption(Component.translatable("options.off"));
        buttonFullscreenVisible.setSelected(frontier.getFullscreenVisible() ? 0 : 1);

        labels.add(new GuiSimpleLabel(font, rightSide, top + 86, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_name"), GuiColors.SETTINGS_TEXT));
        buttonFullscreenNameVisible = new GuiOptionButton(font, rightSide + 116, top + 84, 28, this::buttonPressed);
        buttonFullscreenNameVisible.addOption(Component.translatable("options.on"));
        buttonFullscreenNameVisible.addOption(Component.translatable("options.off"));
        buttonFullscreenNameVisible.setSelected(frontier.getFullscreenNameVisible() ? 0 : 1);

        labels.add(new GuiSimpleLabel(font, rightSide, top + 102, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_owner"), GuiColors.SETTINGS_TEXT));
        buttonFullscreenOwnerVisible = new GuiOptionButton(font, rightSide + 116, top + 100, 28, this::buttonPressed);
        buttonFullscreenOwnerVisible.addOption(Component.translatable("options.on"));
        buttonFullscreenOwnerVisible.addOption(Component.translatable("options.off"));
        buttonFullscreenOwnerVisible.setSelected(frontier.getFullscreenOwnerVisible() ? 0 : 1);

        labels.add(new GuiSimpleLabel(font, rightSide + 154, top + 70, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_minimap"), GuiColors.SETTINGS_TEXT));
        buttonMinimapVisible = new GuiOptionButton(font, rightSide + 154 + 116, top + 68, 28, this::buttonPressed);
        buttonMinimapVisible.addOption(Component.translatable("options.on"));
        buttonMinimapVisible.addOption(Component.translatable("options.off"));
        buttonMinimapVisible.setSelected(frontier.getMinimapVisible() ? 0 : 1);

        labels.add(new GuiSimpleLabel(font, rightSide + 154, top + 86, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_name"), GuiColors.SETTINGS_TEXT));
        buttonMinimapNameVisible = new GuiOptionButton(font, rightSide + 154 + 116, top + 84, 28, this::buttonPressed);
        buttonMinimapNameVisible.addOption(Component.translatable("options.on"));
        buttonMinimapNameVisible.addOption(Component.translatable("options.off"));
        buttonMinimapNameVisible.setSelected(frontier.getMinimapNameVisible() ? 0 : 1);

        labels.add(new GuiSimpleLabel(font, rightSide + 154, top + 102, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_owner"), GuiColors.SETTINGS_TEXT));
        buttonMinimapOwnerVisible = new GuiOptionButton(font, rightSide + 154 + 116, top + 100, 28, this::buttonPressed);
        buttonMinimapOwnerVisible.addOption(Component.translatable("options.on"));
        buttonMinimapOwnerVisible.addOption(Component.translatable("options.off"));
        buttonMinimapOwnerVisible.setSelected(frontier.getMinimapOwnerVisible() ? 0 : 1);

        labels.add(new GuiSimpleLabel(font, leftSide, top + 86, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.announce_in_chat"), GuiColors.SETTINGS_TEXT));
        buttonAnnounceInChat = new GuiOptionButton(font, leftSide + 116, top + 84, 28, this::buttonPressed);
        buttonAnnounceInChat.addOption(Component.translatable("options.on"));
        buttonAnnounceInChat.addOption(Component.translatable("options.off"));
        buttonAnnounceInChat.setSelected(frontier.getAnnounceInChat() ? 0 : 1);

        labels.add(new GuiSimpleLabel(font, leftSide, top + 102, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.announce_in_title"), GuiColors.SETTINGS_TEXT));
        buttonAnnounceInTitle = new GuiOptionButton(font, leftSide + 116, top + 100, 28, this::buttonPressed);
        buttonAnnounceInTitle.addOption(Component.translatable("options.on"));
        buttonAnnounceInTitle.addOption(Component.translatable("options.off"));
        buttonAnnounceInTitle.setSelected(frontier.getAnnounceInTitle() ? 0 : 1);

        labels.add(new GuiSimpleLabel(font, rightSide, top + 152, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.color"), GuiColors.LABEL_TEXT));

        labels.add(new GuiSimpleLabel(font, rightSide - 11, top + 170, GuiSimpleLabel.Align.Left,
                Component.literal("R"), GuiColors.LABEL_TEXT));

        textRed = new TextIntBox(0, 0, 255, font, rightSide, top + 164, 29);
        textRed.setResponder(this);
        textRed.setHeight(20);
        textRed.setWidth(34);

        labels.add(new GuiSimpleLabel(font, rightSide + 44, top + 170, GuiSimpleLabel.Align.Left,
                Component.literal("G"), GuiColors.LABEL_TEXT));

        textGreen = new TextIntBox(0, 0, 255, font, rightSide + 55, top + 164, 29);
        textGreen.setResponder(this);
        textGreen.setHeight(20);
        textGreen.setWidth(34);

        labels.add(new GuiSimpleLabel(font, rightSide + 99, top + 170, GuiSimpleLabel.Align.Left,
                Component.literal("B"), GuiColors.LABEL_TEXT));

        textBlue = new TextIntBox(0, 0, 255, font, rightSide + 110, top + 164, 29);
        textBlue.setResponder(this);
        textBlue.setHeight(20);
        textBlue.setWidth(34);

        textRed.setValue((frontier.getColor() & 0xff0000) >> 16);
        textGreen.setValue((frontier.getColor() & 0x00ff00) >> 8);
        textBlue.setValue(frontier.getColor() & 0x0000ff);

        buttonRandomColor = new GuiSettingsButton(font, rightSide, top + 190, 145,
                Component.translatable("mapfrontiers.random_color"), this::buttonPressed);

        colorPicker = new GuiColorPicker(leftSide + 2, top + 156, frontier.getColor(), (picker, dragging) -> colorPickerUpdated(dragging));

        buttonCopy = new GuiButtonIcon(rightSide + 154, top + 263, GuiButtonIcon.Type.Copy, this::buttonPressed);
        buttonPaste = new GuiButtonIcon(rightSide + 174, top + 263, GuiButtonIcon.Type.Paste, this::buttonPressed);
        buttonOpenPasteOptions = new GuiButtonIcon(rightSide + 191, top + 263, GuiButtonIcon.Type.ArrowUp, this::buttonPressed);
        buttonClosePasteOptions = new GuiButtonIcon(rightSide + 191, top + 263, GuiButtonIcon.Type.ArrowDown, this::buttonPressed);

        labelPasteName = new GuiSimpleLabel(font, rightSide + 154, top + 200, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.paste_name"), GuiColors.SETTINGS_TEXT);
        labels.add(labelPasteName);
        buttonPasteName = new GuiOptionButton(font, rightSide + 154 + 116, top + 198, 28, this::buttonPressed);
        buttonPasteName.addOption(Component.translatable("options.on"));
        buttonPasteName.addOption(Component.translatable("options.off"));
        buttonPasteName.setSelected(ConfigData.pasteName ? 0 : 1);

        labelPasteVisibililty = new GuiSimpleLabel(font, rightSide + 154, top + 216, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.paste_visibility"), GuiColors.SETTINGS_TEXT);
        labels.add(labelPasteVisibililty);
        buttonPasteVisibililty = new GuiOptionButton(font, rightSide + 154 + 116, top + 214, 28, this::buttonPressed);
        buttonPasteVisibililty.addOption(Component.translatable("options.on"));
        buttonPasteVisibililty.addOption(Component.translatable("options.off"));
        buttonPasteVisibililty.setSelected(ConfigData.pasteVisibility ? 0 : 1);

        labelPasteColor = new GuiSimpleLabel(font, rightSide + 154, top + 232, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.paste_color"), GuiColors.SETTINGS_TEXT);
        labels.add(labelPasteColor);
        buttonPasteColor = new GuiOptionButton(font, rightSide + 154 + 116, top + 230, 28, this::buttonPressed);
        buttonPasteColor.addOption(Component.translatable("options.on"));
        buttonPasteColor.addOption(Component.translatable("options.off"));
        buttonPasteColor.setSelected(ConfigData.pasteColor ? 0 : 1);

        labelPasteBanner = new GuiSimpleLabel(font, rightSide + 154, top + 248, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.paste_banner"), GuiColors.SETTINGS_TEXT);
        labels.add(labelPasteBanner);
        buttonPasteBanner = new GuiOptionButton(font, rightSide + 154 + 116, top + 246, 28, this::buttonPressed);
        buttonPasteBanner.addOption(Component.translatable("options.on"));
        buttonPasteBanner.addOption(Component.translatable("options.off"));
        buttonPasteBanner.setSelected(ConfigData.pasteBanner ? 0 : 1);

        buttonUndo = new GuiButtonIcon(rightSide + 202, top + 263, GuiButtonIcon.Type.Undo, this::buttonPressed);
        buttonRedo = new GuiButtonIcon(rightSide + 222, top + 263, GuiButtonIcon.Type.Redo, this::buttonPressed);

        int offset1 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Personal")),
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Global"))) + 10;

        int offset2 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.vertices", 9999),
                I18n.get("mapfrontiers.chunks", 9999)) + 10;

        Component type = Component.translatable("mapfrontiers.type",
                Component.translatable(frontier.getPersonal() ? "mapfrontiers.config.Personal" : "mapfrontiers.config.Global"));
        labels.add(new GuiSimpleLabel(font, rightSide, top, GuiSimpleLabel.Align.Left, type, GuiColors.WHITE));

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            Component vertices = Component.translatable("mapfrontiers.vertices", frontier.getVertexCount());
            labels.add(new GuiSimpleLabel(font, rightSide + offset1, top, GuiSimpleLabel.Align.Left, vertices, GuiColors.WHITE));
        } else {
            Component chunks = Component.translatable("mapfrontiers.chunks", frontier.getChunkCount());
            labels.add(new GuiSimpleLabel(font, rightSide + offset1, top, GuiSimpleLabel.Align.Left, chunks, GuiColors.WHITE));
        }

        Component owner = Component.translatable("mapfrontiers.owner", frontier.getOwner());
        labels.add(new GuiSimpleLabel(font, rightSide + offset1 + offset2, top, GuiSimpleLabel.Align.Left, owner, GuiColors.WHITE));

        Component dimension = Component.translatable("mapfrontiers.dimension", frontier.getDimension().location().toString());
        labels.add(new GuiSimpleLabel(font, rightSide, top + 10, GuiSimpleLabel.Align.Left, dimension, GuiColors.SETTINGS_TEXT_DIMENSION));

        Component area = Component.translatable("mapfrontiers.area", frontier.area);
        labels.add(new GuiSimpleLabel(font, rightSide, top + 32, GuiSimpleLabel.Align.Left, area, GuiColors.WHITE));

        Component perimeter = Component.translatable("mapfrontiers.perimeter", frontier.perimeter);
        labels.add(new GuiSimpleLabel(font, rightSide, top + 42, GuiSimpleLabel.Align.Left, perimeter, GuiColors.WHITE));

        if (frontier.getCreated() != null) {
            Component created = Component.translatable("mapfrontiers.created", dateFormat.format(frontier.getCreated()));
            labels.add(new GuiSimpleLabel(font, rightSide + 154, top + 32, GuiSimpleLabel.Align.Left, created, GuiColors.WHITE));
        }

        if (frontier.getModified() != null) {
            Component modified = Component.translatable("mapfrontiers.modified", dateFormat.format(frontier.getModified()));
            modifiedLabel = new GuiSimpleLabel(font, rightSide + 154, top + 42, GuiSimpleLabel.Align.Left, modified, GuiColors.WHITE);
            labels.add(modifiedLabel);
        }

        buttonSelect = new GuiSettingsButton(font, leftSide - 154, top + 290, 144,
                Component.translatable("mapfrontiers.select_in_map"), this::buttonPressed);
        buttonShareSettings = new GuiSettingsButton(font, leftSide, top + 290, 144,
                Component.translatable("mapfrontiers.share_settings"), this::buttonPressed);
        buttonDelete = new GuiSettingsButton(font, rightSide, top + 290, 144,
                Component.translatable("mapfrontiers.delete"), this::buttonPressed);
        buttonDelete.setTextColors(GuiColors.SETTINGS_BUTTON_TEXT_DELETE, GuiColors.SETTINGS_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonDone = new GuiSettingsButton(font, rightSide + 154, top + 290, 144,
                Component.translatable("gui.done"), this::buttonPressed);

        buttonBanner = new GuiSettingsButton(font, leftSide - 152, top, 144,
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
        addRenderableWidget(buttonPasteVisibililty);
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
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (scaleFactor != 1.f) {
            matrixStack.pushPose();
            matrixStack.scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

        drawCenteredString(matrixStack, font, title, this.actualWidth / 2, 8, GuiColors.WHITE);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (frontier.hasBanner()) {
            frontier.renderBanner(minecraft, matrixStack, actualWidth / 2 - 276, actualHeight / 2 - 122, 4);
        }

        for (GuiSimpleLabel label : labels) {
            if (label.visible) {
                label.render(matrixStack, mouseX, mouseY, partialTicks);
            }
        }

        if (buttonBanner.visible && buttonBanner.isHoveredOrFocused()) {
            if (!frontier.hasBanner() && ClientProxy.getHeldBanner() == null) {
                MutableComponent prefix = Component.literal(GuiColors.WARNING + "! " + ChatFormatting.RESET);
                renderTooltip(matrixStack, prefix.append(Component.translatable("mapfrontiers.assign_banner_warn")), mouseX, mouseY);
            }
        } else if (buttonCopy.isHoveredOrFocused()) {
            renderTooltip(matrixStack, Component.translatable("mapfrontiers.copy"), mouseX, mouseY);
        } else if (buttonPaste.visible && buttonPaste.isHoveredOrFocused()) {
            renderTooltip(matrixStack, Component.translatable("mapfrontiers.paste"), mouseX, mouseY);
        } else if (buttonOpenPasteOptions.visible && buttonOpenPasteOptions.isHoveredOrFocused()) {
            renderTooltip(matrixStack, Component.translatable("mapfrontiers.open_paste_options"), mouseX, mouseY);
        } else if (buttonClosePasteOptions.visible && buttonClosePasteOptions.isHoveredOrFocused()) {
            renderTooltip(matrixStack, Component.translatable("mapfrontiers.close_paste_options"), mouseX, mouseY);
        } else if (buttonUndo.visible && buttonUndo.isHoveredOrFocused()) {
            renderTooltip(matrixStack, Component.translatable("mapfrontiers.undo"), mouseX, mouseY);
        } else if (buttonRedo.visible && buttonRedo.isHoveredOrFocused()) {
            renderTooltip(matrixStack, Component.translatable("mapfrontiers.redo"), mouseX, mouseY);
        }

        if (scaleFactor != 1.f) {
            matrixStack.popPose();
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
            if (w instanceof GuiColorPicker) {
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
            ClientProxy.setClipboard(frontier);
            updatePasteOptionsVisibility();
        } else if (button == buttonPaste) {
            FrontierData clipboard = ClientProxy.getClipboard();
            if (clipboard != null && (ConfigData.pasteName || ConfigData.pasteVisibility || ConfigData.pasteColor || ConfigData.pasteBanner)) {
                setFrontier(clipboard, ConfigData.pasteName, ConfigData.pasteVisibility, ConfigData.pasteColor, ConfigData.pasteBanner);
                sendChangesToServer();
                init(minecraft, width, height);
            }
        } else if (button == buttonOpenPasteOptions) {
            ConfigData.pasteOptionsVisible = true;
            updatePasteOptionsVisibility();
            ClientProxy.configUpdated();
        } else if (button == buttonClosePasteOptions) {
            ConfigData.pasteOptionsVisible = false;
            updatePasteOptionsVisibility();
            ClientProxy.configUpdated();
        } else if (button == buttonPasteName) {
            ConfigData.pasteName = buttonPasteName.getSelected() == 0;
            ClientProxy.configUpdated();
        } else if (button == buttonPasteVisibililty) {
            ConfigData.pasteVisibility = buttonPasteVisibililty.getSelected() == 0;
            ClientProxy.configUpdated();
        } else if (button == buttonPasteColor) {
            ConfigData.pasteColor = buttonPasteColor.getSelected() == 0;
            ClientProxy.configUpdated();
        } else if (button == buttonPasteBanner) {
            ConfigData.pasteBanner = buttonPasteBanner.getSelected() == 0;
            ClientProxy.configUpdated();
        } else if (button == buttonUndo) {
            undo();
        } else if (button == buttonRedo) {
            redo();
        } else if (button == buttonSelect) {
            BlockPos center = frontier.getCenter();
            ScreenLayerManager.popLayer();
            UIManager.INSTANCE.openFullscreenMap().centerOn(center.getX(), center.getZ());
        } else if (button == buttonShareSettings) {
            ScreenLayerManager.pushLayer(new GuiShareSettings(frontiersOverlayManager, frontier));
        } else if (button == buttonDelete) {
            // Unsubscribing to not receive this same event.
            ClientProxy.unsuscribeAllEvents(this);
            frontiersOverlayManager.clientDeleteFrontier(frontier);
            onClose();
        } else if (button == buttonDone) {
            onClose();
        } else if (button == buttonBanner) {
            if (!frontier.hasBanner()) {
                ItemStack heldBanner = ClientProxy.getHeldBanner();
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
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
        ClientProxy.unsuscribeAllEvents(this);
    }

    @Override
    public void onClose() {
        ScreenLayerManager.popLayer();

        if (afterClose != null) {
            afterClose.run();
        }
    }

    @Override
    public void updatedValue(TextIntBox textBox, int value) {
        if (textRed == textBox) {
            int newColor = (frontier.getColor() & 0xff00ffff) | (value << 16);
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                colorPicker.setColor(newColor);
                sendChangesToServer();
            }
        } else if (textGreen == textBox) {
            int newColor = (frontier.getColor() & 0xffff00ff) | (value << 8);
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                colorPicker.setColor(newColor);
                sendChangesToServer();
            }
        } else if (textBlue == textBox) {
            int newColor = (frontier.getColor() & 0xffffff00) | value;
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                colorPicker.setColor(newColor);
                sendChangesToServer();
            }
        }
    }

    @Override
    public void updatedValue(TextBox textBox, String value) {
        if (textName1 == textBox) {
            if (!frontier.getName1().equals(value)) {
                frontier.setName1(value);
            }
        } else if (textName2 == textBox) {
            if (!frontier.getName2().equals(value)) {
                frontier.setName2(value);
            }
        }
    }

    @Override
    public void lostFocus(TextBox textBox, String value) {
        sendChangesToServer();
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
            if (ClientProxy.getHeldBanner() == null) {
                message.append(Component.literal(GuiColors.WARNING + " !"));
            }

            buttonBanner.setMessage(message);
        } else {
            buttonBanner.setMessage(Component.translatable("mapfrontiers.remove_banner"));
        }
    }

    private void updateButtons() {
        SettingsProfile profile = ClientProxy.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(Minecraft.getInstance().player);
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
        buttonSelect.visible = frontier.getDimension().equals(jmAPI.getUIState(Context.UI.Fullscreen).dimension);
        buttonShareSettings.visible = actions.canShare;
    }

    private void updatePasteOptionsVisibility() {
        buttonPaste.visible = ClientProxy.getClipboard() != null;
        buttonOpenPasteOptions.visible = buttonPaste.visible && !ConfigData.pasteOptionsVisible;
        buttonClosePasteOptions.visible = buttonPaste.visible && ConfigData.pasteOptionsVisible;
        buttonPasteName.visible = buttonPaste.visible && ConfigData.pasteOptionsVisible;
        buttonPasteVisibililty.visible = buttonPaste.visible && ConfigData.pasteOptionsVisible;
        buttonPasteColor.visible = buttonPaste.visible && ConfigData.pasteOptionsVisible;
        buttonPasteBanner.visible = buttonPaste.visible && ConfigData.pasteOptionsVisible;
        labelPasteName.visible = buttonPaste.visible && ConfigData.pasteOptionsVisible;
        labelPasteVisibililty.visible = buttonPaste.visible && ConfigData.pasteOptionsVisible;
        labelPasteColor.visible = buttonPaste.visible && ConfigData.pasteOptionsVisible;
        labelPasteBanner.visible = buttonPaste.visible && ConfigData.pasteOptionsVisible;
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
