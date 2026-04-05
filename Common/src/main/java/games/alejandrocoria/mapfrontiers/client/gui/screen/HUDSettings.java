package games.alejandrocoria.mapfrontiers.client.gui.screen;

import com.mojang.blaze3d.platform.Window;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUD;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUDPlacementHelper;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUDWidget;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntConsumer;

@ParametersAreNonnullByDefault
public class HUDSettings extends AutoScaledScreen {
    private static final Component positionLabel = Component.translatable("mapfrontiers.config.hud.position");
    private static final Tooltip positionTooltip = Tooltip.create(Component.literal("HUD position relative to anchor."));
    private static final Component positionSeparatorLabel = Component.literal("x");
    private static final Component doneLabel = Component.translatable("gui.done");
    private static final Component onLabel = Component.translatable("options.on");
    private static final Component offLabel = Component.translatable("options.off");

    private HUDWidget HUDWidget;
    private OptionButton buttonSlot1;
    private OptionButton buttonSlot2;
    private OptionButton buttonSlot3;
    private OptionButton buttonAnchor;
    private TextBoxInt textPositionX;
    private TextBoxInt textPositionY;
    private OptionButton buttonAutoAdjustAnchor;
    private OptionButton buttonSnapToBorder;
    private final HUD hud;
    private int anchorLineColor = ColorConstants.HUD_ANCHOR_LIGHT;
    private int anchorLineColorTick = 0;

    private Screen previousScreen;

    public HUDSettings() {
        super(Component.empty());
        hud = HUD.asPreview();
    }

    @Override
    public void display() {
        super.display();
        previousScreen = backgroundScreen;
        backgroundScreen = null;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (previousScreen != null) {
            previousScreen.resize(this.width, this.height);
        }
    }

    @Override
    public void initScreen() {
        postInitialConfigUpdate();
        createHUDPreview();

        GridLayout mainLayout = createMainLayout();

        buildSlotsSection(mainLayout);
        buildAppearanceSection(mainLayout);
        buildPlacementSection(mainLayout);
        buildDoneButton(mainLayout);

        refreshViewState();
    }

    @Override
    public void tick() {
        ++anchorLineColorTick;

        if (anchorLineColorTick >= 3) {
            anchorLineColorTick = 0;
            if (anchorLineColor == ColorConstants.HUD_ANCHOR_LIGHT) {
                anchorLineColor = ColorConstants.HUD_ANCHOR_DARK;
            } else {
                anchorLineColor = ColorConstants.HUD_ANCHOR_LIGHT;
            }
        }
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (Services.JOURNEYMAP.isMinimapEnabled()) {
            Services.JOURNEYMAP.drawMinimapPreview(graphics);
        }

        drawAnchor(graphics, minecraft.getWindow());
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.input() == GLFW.GLFW_KEY_E && !(getFocused() instanceof EditBox)) {
            onClose();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    private void postInitialConfigUpdate() {
        ClientGlobalEvents.postUpdatedConfigEvent();
    }

    private void createHUDPreview() {
        HUDWidget = addRenderableWidget(new HUDWidget(hud, Services.JOURNEYMAP.isMinimapEnabled(), widget -> HUDUpdated()));
    }

    private GridLayout createMainLayout() {
        GridLayout mainLayout = new GridLayout().spacing(4);
        content.addChild(mainLayout);
        return mainLayout;
    }

    private void buildSlotsSection(GridLayout mainLayout) {
        buttonSlot1 = addHUDSlotRow(mainLayout, 0, ClientConfig.HUD_SLOT_1, ClientConfig.HUD_SLOT_1.get().ordinal());
        buttonSlot2 = addHUDSlotRow(mainLayout, 1, ClientConfig.HUD_SLOT_2, ClientConfig.HUD_SLOT_2.get().ordinal());
        buttonSlot3 = addHUDSlotRow(mainLayout, 2, ClientConfig.HUD_SLOT_3, ClientConfig.HUD_SLOT_3.get().ordinal());
    }

    private void buildAppearanceSection(GridLayout mainLayout) {
        addIntSettingRow(mainLayout, 3, ClientConfig.HUD_TEXT_SIZE, ClientConfig.HUD_TEXT_SIZE.get(), 1, 8, 64, 1,
                ClientConfig.HUD_TEXT_SIZE::set, this::postConfigUpdatedAndRefreshPosition);
        addIntSettingRow(mainLayout, 4, ClientConfig.HUD_BANNER_SIZE, ClientConfig.HUD_BANNER_SIZE.get(), 3, 8, 64, 1,
                ClientConfig.HUD_BANNER_SIZE::set, this::postConfigUpdatedAndRefreshPosition);
    }

    private void buildPlacementSection(GridLayout mainLayout) {
        mainLayout.addChild(createConfigLabel(ClientConfig.HUD_ANCHOR), 0, 3);
        buttonAnchor = createAnchorButton();
        mainLayout.addChild(buttonAnchor, 0, 4);

        mainLayout.addChild(createConfigLabel(positionLabel, positionTooltip), 1, 3);
        mainLayout.addChild(createPositionLayout(), 1, 4);

        mainLayout.addChild(createConfigLabel(ClientConfig.HUD_AUTO_ADJUST_ANCHOR), 2, 3);
        buttonAutoAdjustAnchor = createOnOffOptionButton(ClientConfig.HUD_AUTO_ADJUST_ANCHOR.get(), () -> {
            ClientConfig.HUD_AUTO_ADJUST_ANCHOR.set(buttonAutoAdjustAnchor.getSelected() == 0);
            postConfigUpdatedOnly();
        });
        mainLayout.addChild(buttonAutoAdjustAnchor, 2, 4);

        mainLayout.addChild(createConfigLabel(ClientConfig.HUD_SNAP_TO_BORDER), 3, 3);
        buttonSnapToBorder = createOnOffOptionButton(ClientConfig.HUD_SNAP_TO_BORDER.get(), () -> {
            ClientConfig.HUD_SNAP_TO_BORDER.set(buttonSnapToBorder.getSelected() == 0);
            postConfigUpdatedOnly();
        });
        mainLayout.addChild(buttonSnapToBorder, 3, 4);
    }

    private void buildDoneButton(GridLayout mainLayout) {
        SimpleButton buttonDone = new SimpleButton(font, 100, doneLabel, button -> onClose());
        mainLayout.addChild(buttonDone, 5, 0, 1, 5, LayoutSettings.defaults().alignHorizontallyCenter());
    }

    private OptionButton addHUDSlotRow(GridLayout mainLayout, int column, ConfigEntry<?, ?> entry, int selectedValue) {
        mainLayout.addChild(createConfigLabel(entry), column, 0);
        OptionButton button = createHUDSlotButton(selectedValue);
        mainLayout.addChild(button, column, 1);
        return button;
    }

    private void addIntSettingRow(GridLayout mainLayout, int row, ConfigEntry<?, ?> entry, int value, int min, int max, int width, int maxLength,
                                        IntConsumer setter, Runnable onChanged) {
        mainLayout.addChild(createConfigLabel(entry), row, 0);
        TextBoxInt textBox = createIntConfigTextBox(value, min, max, width, maxLength, setter, onChanged);
        mainLayout.addChild(textBox, row, 1);
    }

    private StringWidget createConfigLabel(Component label, Tooltip tooltip) {
        StringWidget stringWidget = new StringWidget(label, font).setColor(ColorConstants.TEXT);
        stringWidget.setTooltip(tooltip);
        return stringWidget;
    }

    private StringWidget createConfigLabel(ConfigEntry<?, ?> entry) {
        return createConfigLabel(entry.translatedName(), tooltip(entry));
    }

    private OptionButton createHUDSlotButton(int selectedValue) {
        OptionButton button = new OptionButton(font, 64, pressedButton -> updateSlots());
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDSlot.None));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDSlot.Name));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDSlot.Owner));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDSlot.Banner));
        button.setSelected(selectedValue);
        return button;
    }

    private OptionButton createAnchorButton() {
        OptionButton button = new OptionButton(font, 134, pressedButton -> {
            ClientConfig.HUD_ANCHOR.set(ClientConfig.HUDAnchor.values()[pressedButton.getSelected()]);
            postConfigUpdatedAndRefreshPosition();
        });
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.ScreenTop));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.ScreenTopRight));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.ScreenRight));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.ScreenBottomRight));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.ScreenBottom));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.ScreenBottomLeft));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.ScreenLeft));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.ScreenTopLeft));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.Minimap));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.MinimapHorizontal));
        button.addOption(ClientConfig.getTranslatedEnum(ClientConfig.HUDAnchor.MinimapVertical));
        button.setSelected(ClientConfig.HUD_ANCHOR.get().ordinal());
        return button;
    }

    private LinearLayout createPositionLayout() {
        LinearLayout positionLayout = LinearLayout.horizontal();

        textPositionX = createPositionTextBox(ClientConfig.HUD_X_POSITION.get(), ClientConfig.HUD_X_POSITION::set, 61);
        positionLayout.addChild(textPositionX);

        positionLayout.addChild(SpacerElement.width(3));
        positionLayout.addChild(new StringWidget(positionSeparatorLabel, font).setColor(ColorConstants.TEXT_DARK));
        positionLayout.addChild(SpacerElement.width(2));

        textPositionY = createPositionTextBox(ClientConfig.HUD_Y_POSITION.get(), ClientConfig.HUD_Y_POSITION::set, 62);
        positionLayout.addChild(textPositionY);

        return positionLayout;
    }

    private TextBoxInt createPositionTextBox(int value, IntConsumer setter, int width) {
        return createIntConfigTextBox(value, Integer.MIN_VALUE, Integer.MAX_VALUE, width, 5, setter, this::postConfigUpdatedOnly);
    }

    private TextBoxInt createIntConfigTextBox(int value, int min, int max, int width, int maxLength, IntConsumer setter, Runnable onChanged) {
        TextBoxInt textBox = new TextBoxInt(value, min, max, font, width);
        textBox.setValue(String.valueOf(value));
        textBox.setMaxLength(maxLength);
        textBox.setValueChangedCallback(newValue -> {
            setter.accept(newValue);
            onChanged.run();
        });
        return textBox;
    }

    private OptionButton createOnOffOptionButton(boolean value, Runnable onChanged) {
        OptionButton button = new OptionButton(font, 134, pressedButton -> onChanged.run());
        addOnOffOptions(button);
        button.setSelected(value ? 0 : 1);
        return button;
    }

    private void addOnOffOptions(OptionButton button) {
        button.addOption(onLabel);
        button.addOption(offLabel);
    }

    private void postConfigUpdatedAndRefreshPosition() {
        ClientGlobalEvents.postUpdatedConfigEvent();
        updatePosition();
    }

    private void postConfigUpdatedOnly() {
        ClientGlobalEvents.postUpdatedConfigEvent();
    }

    private void refreshViewState() {
        updateSlotsValidity();
        updatePosition();
    }

    private void drawAnchor(GuiGraphicsExtractor graphics, Window mainWindow) {
        float factor = (float) mainWindow.getGuiScale();
        graphics.pose().pushMatrix();
        graphics.pose().scale(1.f / factor, 1.f / factor);

        int directionX = 0;
        int directionY = 0;
        int length = 25;
        HUDPlacementHelper.Point anchor = HUDPlacementHelper.getHUDAnchor(ClientConfig.HUD_ANCHOR.get());

        int displayWidth = mainWindow.getWidth();
        int displayHeight = mainWindow.getHeight();

        if (anchor.x < displayWidth / 2) {
            directionX = 1;
        } else if (anchor.x > displayWidth / 2) {
            directionX = -1;
            --anchor.x;
        }

        if (anchor.y < displayHeight / 2) {
            directionY = 1;
        } else if (anchor.y > displayHeight / 2) {
            directionY = -1;
            --anchor.y;
        }

        if (ClientConfig.HUD_ANCHOR.get() == ClientConfig.HUDAnchor.Minimap) {
            directionX = -directionX;
            directionY = -directionY;

            if (directionX == 1) {
                ++anchor.x;
            }
            if (directionY == 1) {
                ++anchor.y;
            }
        }

        if (directionX == 0) {
            graphics.horizontalLine(anchor.x - length, anchor.x + length, anchor.y, anchorLineColor);
        } else {
            graphics.horizontalLine(anchor.x, anchor.x + length * directionX, anchor.y, anchorLineColor);
        }

        if (directionY == 0) {
            graphics.verticalLine(anchor.x, anchor.y - length, anchor.y + length, anchorLineColor);
        } else {
            graphics.verticalLine(anchor.x, anchor.y, anchor.y + length * directionY, anchorLineColor);
        }

        graphics.pose().popMatrix();
    }

    private void updateSlots() {
        updateSlotsValidity();
        boolean updated = false;
        if (buttonSlot1.getColor() == ColorConstants.TEXT || buttonSlot1.getColor() == ColorConstants.TEXT_HIGHLIGHT) {
            ClientConfig.HUD_SLOT_1.set(ClientConfig.HUDSlot.values()[buttonSlot1.getSelected()]);
            updated = true;
        }

        if (buttonSlot2.getColor() == ColorConstants.TEXT || buttonSlot2.getColor() == ColorConstants.TEXT_HIGHLIGHT) {
            ClientConfig.HUD_SLOT_2.set(ClientConfig.HUDSlot.values()[buttonSlot2.getSelected()]);
            updated = true;
        }

        if (buttonSlot3.getColor() == ColorConstants.TEXT || buttonSlot3.getColor() == ColorConstants.TEXT_HIGHLIGHT) {
            ClientConfig.HUD_SLOT_3.set(ClientConfig.HUDSlot.values()[buttonSlot3.getSelected()]);
            updated = true;
        }

        if (updated) {
            ClientGlobalEvents.postUpdatedConfigEvent();
            updatePosition();
        }
    }

    private void updateSlotsValidity() {
        ClientConfig.HUDSlot slot1 = ClientConfig.HUDSlot.values()[buttonSlot1.getSelected()];
        ClientConfig.HUDSlot slot2 = ClientConfig.HUDSlot.values()[buttonSlot2.getSelected()];
        ClientConfig.HUDSlot slot3 = ClientConfig.HUDSlot.values()[buttonSlot3.getSelected()];

        buttonSlot1.setColor(ColorConstants.TEXT, ColorConstants.TEXT_HIGHLIGHT);
        buttonSlot2.setColor(ColorConstants.TEXT, ColorConstants.TEXT_HIGHLIGHT);
        buttonSlot3.setColor(ColorConstants.TEXT, ColorConstants.TEXT_HIGHLIGHT);

        if (slot1 != ClientConfig.HUDSlot.None && slot1 == slot2) {
            buttonSlot1.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
            buttonSlot2.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
        }

        if (slot1 != ClientConfig.HUDSlot.None && slot1 == slot3) {
            buttonSlot1.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
            buttonSlot3.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
        }

        if (slot2 != ClientConfig.HUDSlot.None && slot2 == slot3) {
            buttonSlot2.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
            buttonSlot3.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
        }
    }

    @Override
    public void onClose() {
        backgroundScreen = previousScreen;
        super.onClose();
    }

    @Override
    public void removed() {
        ClientGlobalEvents.postUpdatedConfigEvent();
    }

    private void updatePosition() {
        HUDPlacementHelper.Point anchorPoint = HUDPlacementHelper.getHUDAnchor(ClientConfig.HUD_ANCHOR.get());
        HUDPlacementHelper.Point originPoint = HUDPlacementHelper.getHUDOrigin(ClientConfig.HUD_ANCHOR.get(), hud.getWidth(), hud.getHeight());
        HUDPlacementHelper.Point positionPoint = new HUDPlacementHelper.Point();
        positionPoint.x = ClientConfig.HUD_X_POSITION.get() + anchorPoint.x - originPoint.x;
        positionPoint.y = ClientConfig.HUD_Y_POSITION.get() + anchorPoint.y - originPoint.y;
        HUDWidget.setPositionHUD(positionPoint);
    }

    private void HUDUpdated() {
        buttonAnchor.setSelected(ClientConfig.HUD_ANCHOR.get().ordinal());
        textPositionX.setValue(String.valueOf(ClientConfig.HUD_X_POSITION.get()));
        textPositionY.setValue(String.valueOf(ClientConfig.HUD_Y_POSITION.get()));
    }

    private static Tooltip tooltip(ConfigEntry<?, ?> entry) {
        Component component = entry.tooltip();
        return component == null ? null : Tooltip.create(component);
    }
}
