package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.ColorInputMode;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@ParametersAreNonnullByDefault
public class ColorInputTabsWidget implements Layout {
    public static final int WIDTH = 113;
    private static final int FIELD_HEIGHT = 17;
    private static final int FIELD_SPACING = 3;
    private static final int TAB_BOX_HEIGHT = 41;
    private static final int COMPACT_FIELD_WIDTH = 33;
    private static final int COMPACT_FIELDS_TOTAL_WIDTH = COMPACT_FIELD_WIDTH * 3 + FIELD_SPACING * 2;
    private static final Component RGB_LABEL = Component.translatable("mapfrontiers.rgb");
    private static final Component HSV_LABEL = Component.translatable("mapfrontiers.hsv");
    private static final Component HEX_LABEL = Component.translatable("mapfrontiers.hex");

    private final IntConsumer colorChanged;
    private final TabbedBox tabbedBox;
    private final LinearLayout rgbRow = LinearLayout.horizontal().spacing(FIELD_SPACING);
    private final LinearLayout hsvRow = LinearLayout.horizontal().spacing(FIELD_SPACING);
    private final TextBoxInt textRed;
    private final TextBoxInt textGreen;
    private final TextBoxInt textBlue;
    private final TextBoxInt textHue;
    private final TextBoxInt textSaturation;
    private final TextBoxInt textValue;
    private final TextBox textHex;
    private ColorInputMode selectedMode;
    private int color;
    private boolean editable = true;
    private boolean syncing = false;
    private boolean suppressModeChanged = false;

    public ColorInputTabsWidget(Font font, int initialColor, IntConsumer colorChanged) {
        this.colorChanged = colorChanged;
        this.color = initialColor | 0xFF000000;

        tabbedBox = new TabbedBox(font, this::onTabChanged);
        int baseTabWidth = WIDTH / 3;
        tabbedBox.addTab(RGB_LABEL, true, baseTabWidth);
        tabbedBox.addTab(HSV_LABEL, true, WIDTH - baseTabWidth * 2 - 1);
        tabbedBox.addTab(HEX_LABEL, true, baseTabWidth);
        tabbedBox.setContentTopSpacing(3);
        tabbedBox.setSize(WIDTH, TAB_BOX_HEIGHT);

        textRed = createRgbTextBox(font, ignored -> applyRgbFieldsColor());
        textGreen = createRgbTextBox(font, ignored -> applyRgbFieldsColor());
        textBlue = createRgbTextBox(font, ignored -> applyRgbFieldsColor());
        rgbRow.addChild(textRed);
        rgbRow.addChild(textGreen);
        rgbRow.addChild(textBlue);
        tabbedBox.addChild(rgbRow, ColorInputMode.RGB.ordinal(),
                LayoutSettings.defaults().alignHorizontallyCenter().alignVerticallyTop());

        textHue = createHsvTextBox(font, 0, 360, ignored -> applyHsvFieldsColor());
        textSaturation = createHsvTextBox(font, 0, 100, ignored -> applyHsvFieldsColor());
        textValue = createHsvTextBox(font, 0, 100, ignored -> applyHsvFieldsColor());
        hsvRow.addChild(textHue);
        hsvRow.addChild(textSaturation);
        hsvRow.addChild(textValue);
        tabbedBox.addChild(hsvRow, ColorInputMode.HSV.ordinal(),
                LayoutSettings.defaults().alignHorizontallyCenter().alignVerticallyTop());

        textHex = new TextBox(font, COMPACT_FIELDS_TOTAL_WIDTH);
        textHex.setMaxLength(10);
        textHex.setHeight(FIELD_HEIGHT);
        textHex.setValueChangedCallback(this::onHexChanged);
        tabbedBox.addChild(textHex, ColorInputMode.Hexadecimal.ordinal(),
                LayoutSettings.defaults().alignHorizontallyCenter().alignVerticallyTop());

        setSelectedMode(ClientConfig.COLOR_INPUT_MODE.get(), false);
        setColor(this.color);
    }

    public void setColor(int color) {
        this.color = color | 0xFF000000;

        syncing = true;
        try {
            textRed.setValue((this.color >> 16) & 0xFF);
            textGreen.setValue((this.color >> 8) & 0xFF);
            textBlue.setValue(this.color & 0xFF);

            float[] hsv = Color.RGBtoHSB((this.color >> 16) & 0xFF, (this.color >> 8) & 0xFF, this.color & 0xFF, null);
            int hue = Math.round(hsv[0] * 360.0f);
            if (hue == 360) {
                hue = 0;
            }
            textHue.setValue(hue);
            textSaturation.setValue(Math.round(hsv[1] * 100.0f));
            textValue.setValue(Math.round(hsv[2] * 100.0f));
            textHex.setValue(formatHex(this.color));
        } finally {
            syncing = false;
        }
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        syncing = true;
        try {
            updateEditability();
        } finally {
            syncing = false;
        }
    }

    public void clearFocus() {
        textRed.setFocused(false);
        textGreen.setFocused(false);
        textBlue.setFocused(false);
        textHue.setFocused(false);
        textSaturation.setFocused(false);
        textValue.setFocused(false);
        textHex.setFocused(false);
    }

    public void renderTabbedBoxBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        tabbedBox.renderBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        tabbedBox.visitChildren(visitor);
    }

    @Override
    public void arrangeElements() {
        tabbedBox.arrangeElements();
    }

    @Override
    public int getWidth() {
        return tabbedBox.getWidth();
    }

    @Override
    public int getHeight() {
        return tabbedBox.getHeight();
    }

    @Override
    public void setX(int x) {
        tabbedBox.setX(x);
    }

    @Override
    public void setY(int y) {
        tabbedBox.setY(y);
    }

    @Override
    public int getX() {
        return tabbedBox.getX();
    }

    @Override
    public int getY() {
        return tabbedBox.getY();
    }

    private TextBoxInt createRgbTextBox(Font font, IntConsumer callback) {
        TextBoxInt textBox = new TextBoxInt(0, 0, 255, font, COMPACT_FIELD_WIDTH);
        textBox.setHeight(FIELD_HEIGHT);
        textBox.setValueChangedCallback(value -> {
            if (!syncing) {
                callback.accept(value);
            }
        });
        return textBox;
    }

    private TextBoxInt createHsvTextBox(Font font, int min, int max, IntConsumer callback) {
        TextBoxInt textBox = new TextBoxInt(min, min, max, font, COMPACT_FIELD_WIDTH);
        textBox.setHeight(FIELD_HEIGHT);
        textBox.setValueChangedCallback(value -> {
            if (!syncing) {
                callback.accept(value);
            }
        });
        return textBox;
    }

    private void onTabChanged(int tab) {
        selectedMode = ColorInputMode.values()[tab];
        updateEditability();
        setColor(color);

        if (!suppressModeChanged) {
            ClientConfig.COLOR_INPUT_MODE.set(selectedMode);
            ClientGlobalEvents.postUpdatedConfigEvent();
        }
    }

    private void setSelectedMode(ColorInputMode mode, boolean notify) {
        if (selectedMode == mode) {
            return;
        }

        suppressModeChanged = !notify;
        try {
            tabbedBox.setTabSelected(mode.ordinal());
        } finally {
            suppressModeChanged = false;
        }
    }

    private void updateEditability() {
        tabbedBox.setInteractive(editable);
        textRed.setEditable(editable);
        textGreen.setEditable(editable);
        textBlue.setEditable(editable);
        textHue.setEditable(editable);
        textSaturation.setEditable(editable);
        textValue.setEditable(editable);
        textHex.setEditable(editable);
    }

    private void applyColor(int color) {
        int normalizedColor = color | 0xFF000000;
        if (this.color == normalizedColor) {
            return;
        }

        this.color = normalizedColor;
        setColor(this.color);
        colorChanged.accept(this.color);
    }

    private void applyHsvFieldsColor() {
        int hue = textHue.clamped();
        if (hue == 360) {
            hue = 0;
        }

        int saturation = textSaturation.clamped();
        int value = textValue.clamped();
        int color = Color.HSBtoRGB(hue / 360.0f, saturation / 100.0f, value / 100.0f);
        applyColor(color);
    }

    private void applyRgbFieldsColor() {
        applyColor(composeRgbColor(textRed.clamped(), textGreen.clamped(), textBlue.clamped()));
    }

    private void onHexChanged(String value) {
        if (syncing) {
            return;
        }

        String normalized = normalizeHexInput(value);
        if (!normalized.equals(value)) {
            syncing = true;
            try {
                textHex.setValue(normalized);
            } finally {
                syncing = false;
            }
        }

        if (normalized.length() == 6) {
            applyColor(Integer.parseInt(normalized, 16));
        }
    }

    private static String normalizeHexInput(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        } else if (trimmed.regionMatches(true, 0, "0x", 0, 2)) {
            trimmed = trimmed.substring(2);
        }

        StringBuilder hex = new StringBuilder(8);
        for (int i = 0; i < trimmed.length() && hex.length() < 8; i++) {
            char c = trimmed.charAt(i);
            if (Character.digit(c, 16) >= 0) {
                hex.append(Character.toUpperCase(c));
            }
        }

        if (hex.length() == 8) {
            return hex.substring(2);
        }

        if (hex.length() > 6) {
            return hex.substring(0, 6);
        }

        return hex.toString();
    }

    private static int composeRgbColor(int red, int green, int blue) {
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static String formatHex(int color) {
        return String.format(Locale.ROOT, "%06X", color & 0x00FFFFFF);
    }
}
