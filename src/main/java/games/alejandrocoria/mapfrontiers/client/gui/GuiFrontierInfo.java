package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.client.util.StringHelper;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
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
        super(CommonComponents.EMPTY);
        this.jmAPI = jmAPI;
        this.afterClose = afterClose;
        frontiersOverlayManager = ClientProxy.getFrontiersOverlayManager(frontier.getPersonal());
        this.frontier = frontier;

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(this, 627, 336);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);

        Component title = Component.translatable("mapfrontiers.title_info");
        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        int leftSide = actualWidth / 2 - 154;
        int rightSide = actualWidth / 2 + 10;
        int top = actualHeight / 2 - 142;

        addRenderableOnly(new GuiSimpleLabel(font, leftSide, top, GuiSimpleLabel.Align.Left,
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

        addRenderableOnly(new GuiSimpleLabel(font, leftSide, top + 70, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_frontier"), GuiColors.SETTINGS_TEXT));
        buttonVisible = new GuiOptionButton(font, leftSide + 116, top + 68, 28, this::buttonPressed);
        buttonVisible.addOption(Component.translatable("options.on"));
        buttonVisible.addOption(Component.translatable("options.off"));
        buttonVisible.setSelected(frontier.getVisible() ? 0 : 1);

        addRenderableOnly(new GuiSimpleLabel(font, rightSide, top + 70, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_fullscreen"), GuiColors.SETTINGS_TEXT));
        buttonFullscreenVisible = new GuiOptionButton(font, rightSide + 116, top + 68, 28, this::buttonPressed);
        buttonFullscreenVisible.addOption(Component.translatable("options.on"));
        buttonFullscreenVisible.addOption(Component.translatable("options.off"));
        buttonFullscreenVisible.setSelected(frontier.getFullscreenVisible() ? 0 : 1);

        addRenderableOnly(new GuiSimpleLabel(font, rightSide, top + 86, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_name"), GuiColors.SETTINGS_TEXT));
        buttonFullscreenNameVisible = new GuiOptionButton(font, rightSide + 116, top + 84, 28, this::buttonPressed);
        buttonFullscreenNameVisible.addOption(Component.translatable("options.on"));
        buttonFullscreenNameVisible.addOption(Component.translatable("options.off"));
        buttonFullscreenNameVisible.setSelected(frontier.getFullscreenNameVisible() ? 0 : 1);

        addRenderableOnly(new GuiSimpleLabel(font, rightSide, top + 102, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_owner"), GuiColors.SETTINGS_TEXT));
        buttonFullscreenOwnerVisible = new GuiOptionButton(font, rightSide + 116, top + 100, 28, this::buttonPressed);
        buttonFullscreenOwnerVisible.addOption(Component.translatable("options.on"));
        buttonFullscreenOwnerVisible.addOption(Component.translatable("options.off"));
        buttonFullscreenOwnerVisible.setSelected(frontier.getFullscreenOwnerVisible() ? 0 : 1);

        addRenderableOnly(new GuiSimpleLabel(font, rightSide + 154, top + 70, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_minimap"), GuiColors.SETTINGS_TEXT));
        buttonMinimapVisible = new GuiOptionButton(font, rightSide + 154 + 116, top + 68, 28, this::buttonPressed);
        buttonMinimapVisible.addOption(Component.translatable("options.on"));
        buttonMinimapVisible.addOption(Component.translatable("options.off"));
        buttonMinimapVisible.setSelected(frontier.getMinimapVisible() ? 0 : 1);

        addRenderableOnly(new GuiSimpleLabel(font, rightSide + 154, top + 86, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_name"), GuiColors.SETTINGS_TEXT));
        buttonMinimapNameVisible = new GuiOptionButton(font, rightSide + 154 + 116, top + 84, 28, this::buttonPressed);
        buttonMinimapNameVisible.addOption(Component.translatable("options.on"));
        buttonMinimapNameVisible.addOption(Component.translatable("options.off"));
        buttonMinimapNameVisible.setSelected(frontier.getMinimapNameVisible() ? 0 : 1);

        addRenderableOnly(new GuiSimpleLabel(font, rightSide + 154, top + 102, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.show_owner"), GuiColors.SETTINGS_TEXT));
        buttonMinimapOwnerVisible = new GuiOptionButton(font, rightSide + 154 + 116, top + 100, 28, this::buttonPressed);
        buttonMinimapOwnerVisible.addOption(Component.translatable("options.on"));
        buttonMinimapOwnerVisible.addOption(Component.translatable("options.off"));
        buttonMinimapOwnerVisible.setSelected(frontier.getMinimapOwnerVisible() ? 0 : 1);

        addRenderableOnly(new GuiSimpleLabel(font, leftSide, top + 86, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.announce_in_chat"), GuiColors.SETTINGS_TEXT));
        buttonAnnounceInChat = new GuiOptionButton(font, leftSide + 116, top + 84, 28, this::buttonPressed);
        buttonAnnounceInChat.addOption(Component.translatable("options.on"));
        buttonAnnounceInChat.addOption(Component.translatable("options.off"));
        buttonAnnounceInChat.setSelected(frontier.getAnnounceInChat() ? 0 : 1);

        addRenderableOnly(new GuiSimpleLabel(font, leftSide, top + 102, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.announce_in_title"), GuiColors.SETTINGS_TEXT));
        buttonAnnounceInTitle = new GuiOptionButton(font, leftSide + 116, top + 100, 28, this::buttonPressed);
        buttonAnnounceInTitle.addOption(Component.translatable("options.on"));
        buttonAnnounceInTitle.addOption(Component.translatable("options.off"));
        buttonAnnounceInTitle.setSelected(frontier.getAnnounceInTitle() ? 0 : 1);

        addRenderableOnly(new GuiSimpleLabel(font, rightSide, top + 152, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.color"), GuiColors.LABEL_TEXT));

        addRenderableOnly(new GuiSimpleLabel(font, rightSide - 11, top + 170, GuiSimpleLabel.Align.Left,
                Component.literal("R"), GuiColors.LABEL_TEXT));

        textRed = new TextIntBox(0, 0, 255, font, rightSide, top + 164, 29);
        textRed.setResponder(this);
        textRed.setHeight(20);
        textRed.setWidth(34);

        addRenderableOnly(new GuiSimpleLabel(font, rightSide + 44, top + 170, GuiSimpleLabel.Align.Left,
                Component.literal("G"), GuiColors.LABEL_TEXT));

        textGreen = new TextIntBox(0, 0, 255, font, rightSide + 55, top + 164, 29);
        textGreen.setResponder(this);
        textGreen.setHeight(20);
        textGreen.setWidth(34);

        addRenderableOnly(new GuiSimpleLabel(font, rightSide + 99, top + 170, GuiSimpleLabel.Align.Left,
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


        int offset1 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Personal")),
                I18n.get("mapfrontiers.type", I18n.get("mapfrontiers.config.Global"))) + 10;

        int offset2 = StringHelper.getMaxWidth(font,
                I18n.get("mapfrontiers.vertices", 9999),
                I18n.get("mapfrontiers.chunks", 9999)) + 10;

        Component type = Component.translatable("mapfrontiers.type",
                Component.translatable(frontier.getPersonal() ? "mapfrontiers.config.Personal" : "mapfrontiers.config.Global"));
        addRenderableOnly(new GuiSimpleLabel(font, rightSide, top, GuiSimpleLabel.Align.Left, type, GuiColors.WHITE));

        if (frontier.getMode() == FrontierData.Mode.Vertex) {
            Component vertices = Component.translatable("mapfrontiers.vertices", frontier.getVertexCount());
            addRenderableOnly(new GuiSimpleLabel(font, rightSide + offset1, top, GuiSimpleLabel.Align.Left, vertices, GuiColors.WHITE));
        } else {
            Component chunks = Component.translatable("mapfrontiers.chunks", frontier.getChunkCount());
            addRenderableOnly(new GuiSimpleLabel(font, rightSide + offset1, top, GuiSimpleLabel.Align.Left, chunks, GuiColors.WHITE));
        }

        Component owner = Component.translatable("mapfrontiers.owner", frontier.getOwner());
        addRenderableOnly(new GuiSimpleLabel(font, rightSide + offset1 + offset2, top, GuiSimpleLabel.Align.Left, owner, GuiColors.WHITE));

        Component dimension = Component.translatable("mapfrontiers.dimension", frontier.getDimension().location().toString());
        addRenderableOnly(new GuiSimpleLabel(font, rightSide, top + 10, GuiSimpleLabel.Align.Left, dimension, GuiColors.SETTINGS_TEXT_DIMENSION));

        Component area = Component.translatable("mapfrontiers.area", frontier.area);
        addRenderableOnly(new GuiSimpleLabel(font, rightSide, top + 32, GuiSimpleLabel.Align.Left, area, GuiColors.WHITE));

        Component perimeter = Component.translatable("mapfrontiers.perimeter", frontier.perimeter);
        addRenderableOnly(new GuiSimpleLabel(font, rightSide, top + 42, GuiSimpleLabel.Align.Left, perimeter, GuiColors.WHITE));

        if (frontier.getCreated() != null) {
            Component created = Component.translatable("mapfrontiers.created", dateFormat.format(frontier.getCreated()));
            addRenderableOnly(new GuiSimpleLabel(font, rightSide + 154, top + 32, GuiSimpleLabel.Align.Left, created, GuiColors.WHITE));
        }

        if (frontier.getModified() != null) {
            Component modified = Component.translatable("mapfrontiers.modified", dateFormat.format(frontier.getModified()));
            modifiedLabel = new GuiSimpleLabel(font, rightSide + 154, top + 42, GuiSimpleLabel.Align.Left, modified, GuiColors.WHITE);
            addRenderableOnly(modifiedLabel);
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

        buttonBanner = new GuiSettingsButton(font, leftSide - 154, top, 144,
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
        addRenderableWidget(buttonSelect);
        addRenderableWidget(buttonShareSettings);
        addRenderableWidget(buttonDelete);
        addRenderableWidget(buttonDone);
        addRenderableWidget(buttonBanner);

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
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (scaleFactor != 1.f) {
            matrixStack.pushPose();
            matrixStack.scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (frontier.hasBanner()) {
            frontier.renderBanner(minecraft, matrixStack, actualWidth / 2 - 279, actualHeight / 2 - 122, 4);
        }

        if (buttonBanner.visible && buttonBanner.isHoveredOrFocused()) {
            if (!frontier.hasBanner() && ClientProxy.getHeldBanner() == null) {
                MutableComponent prefix = Component.literal(GuiColors.WARNING + "! " + ChatFormatting.RESET);
                renderTooltip(matrixStack, prefix.append(Component.translatable("mapfrontiers.assign_banner_warn")), mouseX, mouseY);
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

        for (GuiEventListener w : children()) {
            if (w instanceof GuiScrollBox) {
                ((GuiScrollBox) w).mouseReleased();
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
        } else if (button == buttonSelect) {
            BlockPos center = frontier.getCenter();
            ForgeHooksClient.popGuiLayer(minecraft);
            UIManager.INSTANCE.openFullscreenMap().centerOn(center.getX(), center.getZ());
        } else if (button == buttonShareSettings) {
            ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiShareSettings(frontiersOverlayManager, frontier));
        } else if (button == buttonDelete) {
            // Unsubscribing to not receive this same event.
            MinecraftForge.EVENT_BUS.unregister(this);
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
            if (event.playerID != minecraft.player.getId()) {
                init(minecraft, width, height);
            } else {
                if (frontier.getModified() != null) {
                    Component modified = Component.translatable("mapfrontiers.modified", dateFormat.format(frontier.getModified()));
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
        buttonDelete.visible = actions.canDelete;
        buttonBanner.visible = actions.canUpdate;
        buttonSelect.visible = frontier.getDimension().equals(jmAPI.getUIState(Context.UI.Fullscreen).dimension);
        buttonShareSettings.visible = actions.canShare;
    }

    private void sendChangesToServer() {
        frontiersOverlayManager.clientUpdatefrontier(frontier);
    }
}
