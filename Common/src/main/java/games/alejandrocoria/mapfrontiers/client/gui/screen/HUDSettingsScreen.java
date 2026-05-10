package games.alejandrocoria.mapfrontiers.client.gui.screen;

import com.mojang.blaze3d.platform.Window;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.HUDAnchor;
import games.alejandrocoria.mapfrontiers.client.config.HUDSlot;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUD;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUDPlacementHelper;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUDWidget;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class HUDSettingsScreen extends AutoScaledScreen {
    private static final Component POSITION_LABEL = Component.translatable("mapfrontiers.config.hud.position");
    private static final Tooltip POSITION_TOOLTIP = Tooltip.create(Component.literal("HUD position relative to anchor."));
    private static final Component POSITION_SEPARATOR_LABEL = Component.literal("x");
    private static final Component DONE_LABEL = Component.translatable("gui.done");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");

    private HUDWidget HUDWidget;
    private final List<OptionButton> slotButtons = new ArrayList<>();
    private OptionButton buttonAnchor;
    private TextBoxInt textPositionX;
    private TextBoxInt textPositionY;
    private OptionButton buttonAutoAdjustAnchor;
    private OptionButton buttonSnapToBorder;
    private final HUD hud;
    private final List<HUDSlot> configuredSlots = new ArrayList<>();
    private int anchorLineColor = ColorConstants.HUD_ANCHOR_LIGHT;
    private int anchorLineColorTick = 0;

    private Screen previousScreen;

    public HUDSettingsScreen() {
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
    protected void initScreen() {
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
    protected void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (Services.JOURNEYMAP.isMinimapEnabled()) {
            Services.JOURNEYMAP.drawMinimapPreview(graphics);
        }

        drawAnchor(graphics, minecraft.getWindow());
        drawCenteredBoxBackground(graphics);
    }

    private void postInitialConfigUpdate() {
        ClientGlobalEvents.postUpdatedConfigEvent();
    }

    private void createHUDPreview() {
        HUDWidget = addRenderableWidget(new HUDWidget(hud, Services.JOURNEYMAP.isMinimapEnabled(), widget -> HUDUpdated()));
    }

    private GridLayout createMainLayout() {
        GridLayout mainLayout = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        mainLayout.defaultCellSetting().alignVerticallyMiddle();
        content.addChild(mainLayout);
        return mainLayout;
    }

    private void buildSlotsSection(GridLayout mainLayout) {
        slotButtons.clear();
        configuredSlots.clear();
        configuredSlots.addAll(ClientConfig.getHUDSlots());
        for (int i = 0; i < configuredSlots.size(); ++i) {
            slotButtons.add(addHUDSlotRow(mainLayout, i, configuredSlots.get(i).ordinal()));
        }
    }

    private void buildAppearanceSection(GridLayout mainLayout) {
        int row = configuredSlots.size();
        addIntSettingRow(mainLayout, row++, ClientConfig.HUD_TEXT_SIZE, 64, 1, this::postConfigUpdatedAndRefreshPosition);
        addIntSettingRow(mainLayout, row, ClientConfig.HUD_BANNER_SIZE, 64, 1, this::postConfigUpdatedAndRefreshPosition);
    }

    private void buildPlacementSection(GridLayout mainLayout) {
        mainLayout.addChild(createConfigLabel(ClientConfig.HUD_ANCHOR), 0, 3);
        buttonAnchor = createAnchorButton();
        mainLayout.addChild(buttonAnchor, 0, 4);

        mainLayout.addChild(createConfigLabel(POSITION_LABEL, POSITION_TOOLTIP), 1, 3);
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
        SimpleButton buttonDone = new SimpleButton(font, LayoutConstants.PANEL_BUTTON_WIDTH, DONE_LABEL, button -> onClose());
        mainLayout.addChild(buttonDone, 5, 0, 1, 5, LayoutSettings.defaults().alignHorizontallyCenter());
    }

    private OptionButton addHUDSlotRow(GridLayout mainLayout, int column, int selectedValue) {
        String slotKey = "mapfrontiers.config.hud.slot" + (column + 1);
        mainLayout.addChild(createConfigLabel(Component.translatable(slotKey), createHUDSlotTooltip(column)), column, 0);
        OptionButton button = createHUDSlotButton(selectedValue);
        mainLayout.addChild(button, column, 1);
        return button;
    }

    private Tooltip createHUDSlotTooltip(int slotIndex) {
        String slotKey = "mapfrontiers.config.hud.slot" + (slotIndex + 1);
        Component defaultSlotLabel = ClientConfig.getTranslatedEnum(ClientConfig.getDefaultHUDSlot(slotIndex));
        Component defaultLine = Component.translatable("mapfrontiers.default", defaultSlotLabel)
                .copy()
                .withStyle(Style.EMPTY.withBold(true));
        Component tooltipText = Component.translatable(slotKey + ".tooltip")
                .copy()
                .append("\n\n")
                .append(defaultLine);
        return Tooltip.create(tooltipText);
    }

    private void addIntSettingRow(GridLayout mainLayout, int row, IntConfigEntry entry, int width, int maxLength, Runnable onChanged) {
        mainLayout.addChild(createConfigLabel(entry), row, 0);
        TextBoxInt textBox = createIntConfigTextBox(entry, width, maxLength, onChanged);
        mainLayout.addChild(textBox, row, 1);
    }

    private StringWidget createConfigLabel(Component label,@Nullable Tooltip tooltip) {
        StringWidget stringWidget = new StringWidget(label, font).setColor(ColorConstants.TEXT);
        stringWidget.setTooltip(tooltip);
        return stringWidget;
    }

    private StringWidget createConfigLabel(ConfigEntry<?, ?> entry) {
        return createConfigLabel(entry.translatedName(), ScreenHelper.tooltip(entry));
    }

    private OptionButton createHUDSlotButton(int selectedValue) {
        OptionButton button = new OptionButton(font, 64, pressedButton -> updateSlots());
        for (HUDSlot slot : HUDSlot.values()) {
            button.addOption(ClientConfig.getTranslatedEnum(slot));
        }
        button.setSelected(selectedValue);
        return button;
    }

    private OptionButton createAnchorButton() {
        OptionButton button = new OptionButton(font, 134, pressedButton -> {
            ClientConfig.HUD_ANCHOR.set(HUDAnchor.VALUES[pressedButton.getSelected()]);
            postConfigUpdatedAndRefreshPosition();
        });
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.ScreenTop));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.ScreenTopRight));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.ScreenRight));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.ScreenBottomRight));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.ScreenBottom));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.ScreenBottomLeft));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.ScreenLeft));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.ScreenTopLeft));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.Minimap));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.MinimapHorizontal));
        button.addOption(ClientConfig.getTranslatedEnum(HUDAnchor.MinimapVertical));
        button.setSelected(ClientConfig.HUD_ANCHOR.get().ordinal());
        return button;
    }

    private LinearLayout createPositionLayout() {
        LinearLayout positionLayout = LinearLayout.horizontal();

        textPositionX = createPositionTextBox(ClientConfig.HUD_X_POSITION, 61);
        positionLayout.addChild(textPositionX);

        positionLayout.addChild(SpacerElement.width(3));
        positionLayout.addChild(new StringWidget(POSITION_SEPARATOR_LABEL, font).setColor(ColorConstants.TEXT_DARK));
        positionLayout.addChild(SpacerElement.width(2));

        textPositionY = createPositionTextBox(ClientConfig.HUD_Y_POSITION, 62);
        positionLayout.addChild(textPositionY);

        return positionLayout;
    }

    private TextBoxInt createPositionTextBox(IntConfigEntry entry, int width) {
        return createIntConfigTextBox(entry, width, 5, this::postConfigUpdatedOnly);
    }

    private TextBoxInt createIntConfigTextBox(IntConfigEntry entry, int width, int maxLength, Runnable onChanged) {
        TextBoxInt textBox = new TextBoxInt(entry, font, width);
        textBox.setValue(String.valueOf(entry.get()));
        textBox.setMaxLength(maxLength);
        textBox.setValueChangedCallback(newValue -> {
            entry.set(newValue);
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
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
    }

    private void postConfigUpdatedAndRefreshPosition() {
        ClientGlobalEvents.postUpdatedConfigEvent();
        updatePosition();
    }

    private void postConfigUpdatedOnly() {
        ClientGlobalEvents.postUpdatedConfigEvent();
    }

    private void refreshViewState() {
        updateSlotsStyle();
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

        if (ClientConfig.HUD_ANCHOR.get() == HUDAnchor.Minimap) {
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
        configuredSlots.clear();
        for (int i = 0; i < slotButtons.size(); ++i) {
            configuredSlots.add(getSelectedSlot(slotButtons.get(i)));
        }
        ClientConfig.setHUDSlots(configuredSlots);

        updateSlotsStyle();
        ClientGlobalEvents.postUpdatedConfigEvent();
        updatePosition();
    }

    private void updateSlotsStyle() {
        for (int i = 0; i < slotButtons.size(); ++i) {
            slotButtons.get(i).setColor(ColorConstants.TEXT, ColorConstants.TEXT_HIGHLIGHT);
        }

        for (int i = 0; i < slotButtons.size(); ++i) {
            HUDSlot leftSlot = getSelectedSlot(slotButtons.get(i));
            if (leftSlot == HUDSlot.None) {
                continue;
            }

            for (int j = i + 1; j < slotButtons.size(); ++j) {
                if (leftSlot == getSelectedSlot(slotButtons.get(j))) {
                    slotButtons.get(i).setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
                    slotButtons.get(j).setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
                }
            }
        }
    }

    private HUDSlot getSelectedSlot(OptionButton button) {
        return HUDSlot.values()[button.getSelected()];
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
}
