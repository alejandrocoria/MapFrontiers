package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.event.DeletedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import journeymap.client.ui.UIManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
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
    private GuiOptionButton buttonShowName;
    private GuiOptionButton buttonShowOwner;
    private GuiOptionButton buttonAnnounceInChat;
    private TextIntBox textRed;
    private TextIntBox textGreen;
    private TextIntBox textBlue;
    private GuiSettingsButton buttonRandomColor;
    private GuiColorPicker colorPicker;

    private GuiSettingsButton buttonSelect;
    private GuiSettingsButton buttonShareSettings;
    private GuiSettingsButton buttonDelete;
    private GuiSettingsButton buttonDone;
    private GuiSettingsButton buttonBanner;

    private GuiSimpleLabel modifiedLabel;

    public GuiFrontierInfo(IClientAPI jmAPI, FrontierOverlay frontier) {
        this(jmAPI, frontier, null);
    }
    public GuiFrontierInfo(IClientAPI jmAPI, FrontierOverlay frontier, @Nullable  Runnable afterClose) {
        super(StringTextComponent.EMPTY);
        this.jmAPI = jmAPI;
        this.afterClose = afterClose;
        frontiersOverlayManager = ClientProxy.getFrontiersOverlayManager(frontier.getPersonal());
        this.frontier = frontier;

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        scaleFactor = ScreenHelper.getScaleFactorThatFit(this, 627, 336);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);

        TextComponent title = new TranslationTextComponent("mapfrontiers.title_info");
        buttons.add(new GuiSimpleLabel(font, actualWidth / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        int leftSide = actualWidth / 2 - 154;
        int rightSide = actualWidth / 2 + 10;
        int top = actualHeight / 2 - 142;

        buttons.add(new GuiSimpleLabel(font, leftSide, top, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.name"), GuiColors.LABEL_TEXT));

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

        buttons.add(new GuiSimpleLabel(font, leftSide, top + 70, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.show_name"), GuiColors.SETTINGS_TEXT));
        buttonShowName = new GuiOptionButton(font, leftSide + 116, top + 68, 28, this::buttonPressed);
        buttonShowName.addOption(new TranslationTextComponent("options.on"));
        buttonShowName.addOption(new TranslationTextComponent("options.off"));
        buttonShowName.setSelected(frontier.getNameVisible() ? 0 : 1);

        buttons.add(new GuiSimpleLabel(font, leftSide, top + 86, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.show_owner"), GuiColors.SETTINGS_TEXT));
        buttonShowOwner = new GuiOptionButton(font, leftSide + 116, top + 84, 28, this::buttonPressed);
        buttonShowOwner.addOption(new TranslationTextComponent("options.on"));
        buttonShowOwner.addOption(new TranslationTextComponent("options.off"));
        buttonShowOwner.setSelected(frontier.getOwnerVisible() ? 0 : 1);

        buttons.add(new GuiSimpleLabel(font, leftSide, top + 102, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.announce_in_chat"), GuiColors.SETTINGS_TEXT));
        buttonAnnounceInChat = new GuiOptionButton(font, leftSide + 116, top + 100, 28, this::buttonPressed);
        buttonAnnounceInChat.addOption(new TranslationTextComponent("options.on"));
        buttonAnnounceInChat.addOption(new TranslationTextComponent("options.off"));
        buttonAnnounceInChat.setSelected(frontier.getAnnounceInChat() ? 0 : 1);

        buttons.add(new GuiSimpleLabel(font, leftSide, top + 118, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.color"), GuiColors.LABEL_TEXT));

        buttons.add(new GuiSimpleLabel(font, leftSide - 11, top + 136, GuiSimpleLabel.Align.Left,
                new StringTextComponent("R"), GuiColors.LABEL_TEXT));

        textRed = new TextIntBox(0, 0, 255, font, leftSide, top + 130, 29);
        textRed.setResponder(this);
        textRed.setHeight(20);
        textRed.setWidth(34);

        buttons.add(new GuiSimpleLabel(font, leftSide + 44, top + 136, GuiSimpleLabel.Align.Left,
                new StringTextComponent("G"), GuiColors.LABEL_TEXT));

        textGreen = new TextIntBox(0, 0, 255, font, leftSide + 55, top + 130, 29);
        textGreen.setResponder(this);
        textGreen.setHeight(20);
        textGreen.setWidth(34);

        buttons.add(new GuiSimpleLabel(font, leftSide + 99, top + 136, GuiSimpleLabel.Align.Left,
                new StringTextComponent("B"), GuiColors.LABEL_TEXT));

        textBlue = new TextIntBox(0, 0, 255, font, leftSide + 110, top + 130, 29);
        textBlue.setResponder(this);
        textBlue.setHeight(20);
        textBlue.setWidth(34);

        textRed.setValue((frontier.getColor() & 0xff0000) >> 16);
        textGreen.setValue((frontier.getColor() & 0x00ff00) >> 8);
        textBlue.setValue(frontier.getColor() & 0x0000ff);

        buttonRandomColor = new GuiSettingsButton(font, rightSide, top + 190, 144,
                new TranslationTextComponent("mapfrontiers.random_color"), this::buttonPressed);

        colorPicker = new GuiColorPicker(leftSide + 2, top + 156, frontier.getColor(), (picker, dragging) -> colorPickerUpdated(dragging));

        TextComponent type = new TranslationTextComponent("mapfrontiers.type",
                new TranslationTextComponent(frontier.getPersonal() ? "mapfrontiers.config.Personal" : "mapfrontiers.config.Global"));
        buttons.add(new GuiSimpleLabel(font, rightSide, top, GuiSimpleLabel.Align.Left, type, GuiColors.WHITE));

        TextComponent owner = new TranslationTextComponent("mapfrontiers.owner", frontier.getOwner());
        buttons.add(new GuiSimpleLabel(font, rightSide, top + 16, GuiSimpleLabel.Align.Left, owner, GuiColors.WHITE));

        TextComponent dimension = new TranslationTextComponent("mapfrontiers.dimension", frontier.getDimension().location().toString());
        buttons.add(new GuiSimpleLabel(font, rightSide, top + 32, GuiSimpleLabel.Align.Left, dimension, GuiColors.WHITE));

        TextComponent mode = new TranslationTextComponent("mapfrontiers.mode",
                new TranslationTextComponent(frontier.getMode() == FrontierData.Mode.Vertex ? "mapfrontiers.config.Vertex" : "mapfrontiers.config.Chunk"));
        buttons.add(new GuiSimpleLabel(font, rightSide, top + 48, GuiSimpleLabel.Align.Left, mode, GuiColors.WHITE));

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            TextComponent vertices = new TranslationTextComponent("mapfrontiers.vertices", frontier.getVertexCount());
            buttons.add(new GuiSimpleLabel(font, rightSide, top + 64, GuiSimpleLabel.Align.Left, vertices, GuiColors.WHITE));
        } else {
            TextComponent chunks = new TranslationTextComponent("mapfrontiers.chunks", frontier.getChunkCount());
            buttons.add(new GuiSimpleLabel(font, rightSide, top + 64, GuiSimpleLabel.Align.Left, chunks, GuiColors.WHITE));
        }

        TextComponent area = new TranslationTextComponent("mapfrontiers.area", frontier.area);
        buttons.add(new GuiSimpleLabel(font, rightSide, top + 80, GuiSimpleLabel.Align.Left, area, GuiColors.WHITE));

        TextComponent perimeter = new TranslationTextComponent("mapfrontiers.perimeter", frontier.perimeter);
        buttons.add(new GuiSimpleLabel(font, rightSide, top + 96, GuiSimpleLabel.Align.Left, perimeter, GuiColors.WHITE));

        if (frontier.getCreated() != null) {
            TextComponent created = new TranslationTextComponent("mapfrontiers.created", dateFormat.format(frontier.getCreated()));
            buttons.add(new GuiSimpleLabel(font, rightSide, top + 112, GuiSimpleLabel.Align.Left, created, GuiColors.WHITE));
        }

        if (frontier.getModified() != null) {
            TextComponent modified = new TranslationTextComponent("mapfrontiers.modified", dateFormat.format(frontier.getModified()));
            modifiedLabel = new GuiSimpleLabel(font, rightSide, top + 128, GuiSimpleLabel.Align.Left, modified, GuiColors.WHITE);
            buttons.add(modifiedLabel);
        }

        buttonSelect = new GuiSettingsButton(font, leftSide - 154, top + 290, 144,
                new TranslationTextComponent("mapfrontiers.select_in_map"), this::buttonPressed);
        buttonShareSettings = new GuiSettingsButton(font, leftSide, top + 290, 144,
                new TranslationTextComponent("mapfrontiers.share_settings"), this::buttonPressed);
        buttonShareSettings.visible = frontier.getPersonal();
        buttonDelete = new GuiSettingsButton(font, rightSide, top + 290, 144,
                new TranslationTextComponent("mapfrontiers.delete"), this::buttonPressed);
        buttonDelete.setTextColors(GuiColors.SETTINGS_BUTTON_TEXT_DELETE, GuiColors.SETTINGS_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonDone = new GuiSettingsButton(font, rightSide + 154, top + 290, 144,
                new TranslationTextComponent("gui.done"), this::buttonPressed);

        buttonBanner = new GuiSettingsButton(font, leftSide - 152, top, 144,
                new TranslationTextComponent("mapfrontiers.assign_banner"), this::buttonPressed);

        addButton(textName1);
        addButton(textName2);
        addButton(buttonShowName);
        addButton(buttonShowOwner);
        addButton(buttonAnnounceInChat);
        addButton(textRed);
        addButton(textGreen);
        addButton(textBlue);
        addButton(buttonRandomColor);
        addButton(colorPicker);
        addButton(buttonSelect);
        addButton(buttonShareSettings);
        addButton(buttonDelete);
        addButton(buttonDone);
        addButton(buttonBanner);

        updateBannerButton();
        updateButtons();
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
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (scaleFactor != 1.f) {
            matrixStack.pushPose();
            matrixStack.scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (frontier.hasBanner()) {
            frontier.renderBanner(minecraft, matrixStack, actualWidth / 2 - 276, actualHeight / 2 - 122, 4);
        }

        if (buttonBanner.visible && buttonBanner.isHovered()) {
            if (!frontier.hasBanner() && ClientProxy.getHeldBanner() == null) {
                StringTextComponent prefix = new StringTextComponent(GuiColors.WARNING + "! " + TextFormatting.RESET);
                renderTooltip(matrixStack, prefix.append(new TranslationTextComponent("mapfrontiers.assign_banner_warn")), mouseX, mouseY);
            }
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

        for (Widget w : buttons) {
            if (w instanceof GuiColorPicker) {
                ((GuiColorPicker) w).mouseReleased(mouseX, mouseY, button);
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

    protected void buttonPressed(Button button) {
        if (button == buttonShowName) {
            frontier.setNameVisible(buttonShowName.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonShowOwner) {
            frontier.setOwnerVisible(buttonShowOwner.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonAnnounceInChat) {
            frontier.setAnnounceInChat(buttonAnnounceInChat.getSelected() == 0);
            sendChangesToServer();
        } else if (button == buttonRandomColor) {
            int newColor = ColorHelper.getRandomColor();
            frontier.setColor(newColor);
            colorPicker.setColor(newColor);
            textRed.setValue((newColor & 0xff0000) >> 16);
            textGreen.setValue((newColor & 0x00ff00) >> 8);
            textBlue.setValue(newColor & 0x0000ff);
            sendChangesToServer();
        } else if (button == buttonSelect) {
            BlockPos center = frontier.getCenter();
            ForgeHooksClient.popGuiLayer(minecraft);
            UIManager.INSTANCE.openFullscreenMap().centerOn(center.getX(), center.getZ());
        } else if (button == buttonShareSettings) {
            ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiShareSettings(frontiersOverlayManager, frontier));
        } else if (button == buttonDelete) {
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
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public void onClose() {
        ForgeHooksClient.popGuiLayer(minecraft);

        if (afterClose != null) {
            afterClose.run();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedSettingsProfileEvent(UpdatedSettingsProfileEvent event) {
        updateButtons();
        updateBannerButton();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedFrontierEvent(UpdatedFrontierEvent event) {
        if (frontier.getId().equals(event.frontierOverlay.getId())) {
            if (event.playerID != Minecraft.getInstance().player.getId()) {
                init(minecraft, actualWidth, actualHeight);
            } else {
                if (frontier.getModified() != null) {
                    TextComponent modified = new TranslationTextComponent("mapfrontiers.modified", dateFormat.format(frontier.getModified()));
                    modifiedLabel.setText(modified);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeletedFrontierEvent(DeletedFrontierEvent event) {
        if (frontier.getId().equals(event.frontierID)) {
            onClose();
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

    private void updateBannerButton() {
        if (!frontier.hasBanner()) {
            TranslationTextComponent message = new TranslationTextComponent("mapfrontiers.assign_banner");
            if (ClientProxy.getHeldBanner() == null) {
                message.append(new StringTextComponent(GuiColors.WARNING + " !"));
            }

            buttonBanner.setMessage(message);
        } else {
            buttonBanner.setMessage(new TranslationTextComponent("mapfrontiers.remove_banner"));
        }
    }

    private void updateButtons() {
        SettingsProfile profile = ClientProxy.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(Minecraft.getInstance().player);
        SettingsProfile.AvailableActions actions = profile.getAvailableActions(frontier, playerUser);

        textName1.setEditable(actions.canUpdate);
        textName2.setEditable(actions.canUpdate);
        buttonShowName.active = actions.canUpdate;
        buttonShowOwner.active = actions.canUpdate;
        buttonAnnounceInChat.active = actions.canUpdate;
        textRed.setEditable(actions.canUpdate);
        textGreen.setEditable(actions.canUpdate);
        textBlue.setEditable(actions.canUpdate);
        buttonRandomColor.visible = actions.canUpdate;
        colorPicker.active = actions.canUpdate;
        buttonDelete.visible = actions.canDelete;
        buttonBanner.visible = actions.canUpdate;
        buttonSelect.visible = frontier.getDimension().equals(jmAPI.getUIState(Context.UI.Fullscreen).dimension);
    }

    private void sendChangesToServer() {
        frontiersOverlayManager.clientUpdatefrontier(frontier);
    }
}
