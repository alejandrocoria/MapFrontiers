package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.AbstractWidgetNoNarration;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
abstract class ShapePresetSelector extends AbstractWidgetNoNarration {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/shape_presets.png");
    private static final int TEXTURE_WIDTH = 1372;
    private static final int TEXTURE_HEIGHT = 98;
    private static final int BUTTON_SIZE = 49;
    private static final int BUTTON_SPACING = 55;
    private static final int BUTTON_Y_OFFSET = 18;
    private static final int CLICK_X_OFFSET = 3;
    private static final int CLICK_Y_OFFSET = 15;
    private static final int LABEL_Y_OFFSET = 2;

    private final int columns;
    private final int buttonCount;
    private final int textureOffsetX;
    private final StringWidget labelShapes;
    protected int selected;
    private int focusedIndex;

    protected ShapePresetSelector(Font font, int selected, int columns, int buttonCount, int textureOffsetX) {
        super(0, 0, columns * BUTTON_SPACING - 6, 122, Component.empty());
        this.columns = columns;
        this.buttonCount = buttonCount;
        this.textureOffsetX = textureOffsetX;
        this.selected = clampSelected(selected);
        this.focusedIndex = this.selected;
        labelShapes = new StringWidget(Component.translatable("mapfrontiers.initial_shape"), font, StringWidget.Align.Center)
                .setColor(ColorConstants.WHITE);
    }

    public int getSelected() {
        return selected;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        labelShapes.setX(x + getWidth() / 2);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        labelShapes.setY(y + LABEL_Y_OFFSET);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        if (!visible || !active) {
            return null;
        }

        if (!isFocused()) {
            focusedIndex = selected;
            return ComponentPath.leaf(this);
        }

        if (navigationEvent instanceof FocusNavigationEvent.TabNavigation) {
            return null;
        }

        if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            int nextIndex = getNextFocusedIndex(arrowNavigation.direction());
            if (nextIndex == -1) {
                return null;
            }

            focusedIndex = nextIndex;
            return ComponentPath.leaf(this);
        }

        return ComponentPath.leaf(this);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused() || !visible || !active || !event.isSelection()) {
            return false;
        }

        if (selected != focusedIndex) {
            selected = focusedIndex;
            onSelectionChanged();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int hovered = getButtonIndex(event.x(), event.y());
        if (hovered == -1) {
            return false;
        }

        selected = hovered;
        focusedIndex = hovered;
        onSelectionChanged();
        return true;
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < buttonCount; ++i) {
            int col = i % columns;
            int row = i / columns;
            int texX = textureOffsetX + i * BUTTON_SIZE;
            int texY = i == selected ? BUTTON_SIZE : 0;
            int x = getX() + col * BUTTON_SPACING;
            int y = getY() + row * BUTTON_SPACING + BUTTON_Y_OFFSET;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, texX, texY, BUTTON_SIZE, BUTTON_SIZE,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
            if (i == selected) {
                graphics.fill(x, y, x + BUTTON_SIZE, y + 1, ColorConstants.WHITE);
                graphics.fill(x, y + BUTTON_SIZE - 1, x + BUTTON_SIZE, y + BUTTON_SIZE, ColorConstants.WHITE);
                graphics.fill(x, y, x + 1, y + BUTTON_SIZE, ColorConstants.WHITE);
                graphics.fill(x + BUTTON_SIZE - 1, y, x + BUTTON_SIZE, y + BUTTON_SIZE, ColorConstants.WHITE);
            }
        }

        if (isKeyboardFocused()) {
            int col = focusedIndex % columns;
            int row = focusedIndex / columns;
            int x = getX() + col * BUTTON_SPACING;
            int y = getY() + row * BUTTON_SPACING + BUTTON_Y_OFFSET;
            graphics.outline(x - 1, y - 1, BUTTON_SIZE + 2, BUTTON_SIZE + 2, ColorConstants.WHITE);
        }

        labelShapes.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    protected abstract void onSelectionChanged();

    private int getButtonIndex(double mouseX, double mouseY) {
        double col = (mouseX - getX() + CLICK_X_OFFSET) / BUTTON_SPACING;
        double row = (mouseY - getY() - CLICK_Y_OFFSET) / BUTTON_SPACING;
        if (col < 0.0 || col >= columns || row < 0.0) {
            return -1;
        }

        int index = (int) col + (int) row * columns;
        if (index < 0 || index >= buttonCount) {
            return -1;
        }

        double cellX = (mouseX - getX()) - (int) col * BUTTON_SPACING;
        double cellY = (mouseY - getY() - BUTTON_Y_OFFSET) - (int) row * BUTTON_SPACING;
        if (cellX < 0.0 || cellX >= BUTTON_SIZE || cellY < 0.0 || cellY >= BUTTON_SIZE) {
            return -1;
        }

        return index;
    }

    private int clampSelected(int selected) {
        return Mth.clamp(selected, 0, buttonCount - 1);
    }

    private int getNextFocusedIndex(ScreenDirection direction) {
        int nextIndex = switch (direction) {
            case LEFT -> focusedIndex % columns == 0 ? -1 : focusedIndex - 1;
            case RIGHT -> focusedIndex % columns == columns - 1 ? -1 : focusedIndex + 1;
            case UP -> focusedIndex - columns;
            case DOWN -> focusedIndex + columns;
        };

        return nextIndex >= 0 && nextIndex < buttonCount ? nextIndex : -1;
    }

    private boolean isKeyboardFocused() {
        return isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
    }
}
