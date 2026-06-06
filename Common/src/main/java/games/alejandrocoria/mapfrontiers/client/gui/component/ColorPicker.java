package games.alejandrocoria.mapfrontiers.client.gui.component;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class ColorPicker extends AbstractWidgetNoNarration {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/color_picker.png");
    private static final int TEXTURE_WIDTH = 274;
    private static final int TEXTURE_HEIGHT = 135;
    private static final int WIDTH = 141;
    private static final int HEIGHT = 128;
    private static final int HS_SIZE = 128;
    private static final int HS_CENTER = 64;
    private static final double HS_RADIUS = 64.0;
    private static final double HS_CLICK_RADIUS = 66.0;
    private static final int V_X = 132;
    private static final int V_WIDTH = 8;
    private static final int V_HEIGHT = 128;
    private static final int V_SELECTION_X = 131;
    private static final int HS_SELECTION_OFFSET = 2;
    private static final int HS_FOCUS_OFFSET = 3;
    private static final int V_MARKER_OFFSET_Y = 2;
    private static final double HS_SPEED = 80.0;
    private static final double V_SPEED = 120.0;
    private static final double HS_FINE_SPEED = 25.0;
    private static final double V_FINE_SPEED = 35.0;
    private static final int HS_BACKGROUND_U = 0;
    private static final int HS_BACKGROUND_V = 0;
    private static final int HS_BACKGROUND_DISABLED_U = 145;
    private static final int HS_BACKGROUND_DISABLED_V = 0;
    private static final int V_BACKGROUND_U = 129;
    private static final int V_BACKGROUND_V = 0;
    private static final int V_BORDER_U = 137;
    private static final int V_BORDER_V = 0;
    private static final int HS_SELECTION_U = 0;
    private static final int HS_SELECTION_V = 129;
    private static final int HS_SELECTION_DISABLED_U = 145;
    private static final int HS_SELECTION_DISABLED_V = 129;
    private static final int HS_SELECTION_SIZE = 5;
    private static final int V_SELECTION_U = 5;
    private static final int V_SELECTION_V = 129;
    private static final int V_SELECTION_DISABLED_U = 150;
    private static final int V_SELECTION_DISABLED_V = 129;
    private static final int V_SELECTION_WIDTH = 10;
    private static final int V_SELECTION_HEIGHT = 5;
    private static final int HS_FOCUS_U = 15;
    private static final int HS_FOCUS_V = 128;
    private static final int HS_FOCUS_SIZE = 7;
    private static final int V_FOCUS_U = 22;
    private static final int V_FOCUS_V = 129;
    private static final int V_FOCUS_WIDTH = 10;
    private static final int V_FOCUS_HEIGHT = 5;

    private enum FocusPart {
        HS,
        V
    }

    private double hsX;
    private double hsY;
    private double v;
    private double focusedHsX;
    private double focusedHsY;
    private double focusedV;
    private int color;
    private int colorFullBrightness;
    private long lastKeyboardMoveTimeNanos = -1L;
    private boolean hsGrabbed = false;
    private boolean vGrabbed = false;
    private FocusPart focusedPart = FocusPart.HS;
    private final BiConsumer<Integer, Boolean> callbackColorUpdated;

    public ColorPicker(int color, BiConsumer<Integer, Boolean> callbackColorUpdated) {
        super(0, 0, WIDTH, HEIGHT, Component.empty());
        this.callbackColorUpdated = callbackColorUpdated;
        setColor(color);
    }

    public void setColor(int newColor) {
        color = newColor;
        float[] hsv = Color.RGBtoHSB((newColor >> 16) & 0xFF, (newColor >> 8) & 0xFF, newColor & 0xFF, null);
        double angle = hsv[0] * Math.PI * 2.0;
        double dist = hsv[1] * HS_RADIUS;
        hsX = dist * Math.cos(angle);
        hsY = dist * Math.sin(angle);
        v = 127.5 - hsv[2] * 127.5;
        colorFullBrightness = Color.HSBtoRGB(hsv[0], hsv[1], 1.f);

        if (!isFocused()) {
            syncFocusedToSelection();
        }
    }

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        if (!visible || !active) {
            return null;
        }

        if (!isFocused()) {
            syncFocusedToSelection();
            focusedPart = getEntryFocusPart(navigationEvent);
            return ComponentPath.leaf(this);
        }

        if (navigationEvent instanceof FocusNavigationEvent.TabNavigation tabNavigation) {
            syncFocusedToSelection();
            if (tabNavigation.forward()) {
                if (focusedPart == FocusPart.HS) {
                    focusedPart = FocusPart.V;
                    return ComponentPath.leaf(this);
                }
                return null;
            }

            if (focusedPart == FocusPart.V) {
                focusedPart = FocusPart.HS;
                return ComponentPath.leaf(this);
            }
            return null;
        }

        if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            return ComponentPath.leaf(this);
        }

        return ComponentPath.leaf(this);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused() || !visible || !active) {
            return false;
        }

        if (focusedPart == FocusPart.V) {
            if (event.input() == GLFW.GLFW_KEY_HOME) {
                focusedV = 0.0;
                return true;
            }
            if (event.input() == GLFW.GLFW_KEY_END) {
                focusedV = 127.99;
                return true;
            }
        }

        if (!event.isSelection()) {
            return false;
        }

        applyFocusedSelection(false);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        hsGrabbed = false;
        vGrabbed = false;

        updateMouse(event.x(), event.y(), false);

        return hsGrabbed || vGrabbed;
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        updateMouse(event.x(), event.y(), false);
        hsGrabbed = false;
        vGrabbed = false;
    }

    @Override
    public void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        updateMouse(event.x(), event.y(), true);
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    public void finishSelection() {
        if (!hsGrabbed && !vGrabbed) {
            return;
        }

        callbackColorUpdated.accept(color, false);
        hsGrabbed = false;
        vGrabbed = false;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        updateKeyboardMovement();
        renderHsBackground(graphics);
        renderVBackground(graphics);
        renderSelectedMarkers(graphics);
        renderFocusedMarkers(graphics);
    }

    private void updateMouse(double mouseX, double mouseY, boolean dragging) {
        if (!vGrabbed) {
            double localX = mouseX - (getX() + HS_CENTER);
            double localY = mouseY - (getY() + HS_CENTER);
            double dist = Math.sqrt(localX * localX + localY * localY);

            if (dist < HS_CLICK_RADIUS) {
                hsGrabbed = true;
            }

            if (hsGrabbed) {
                if (dist >= HS_RADIUS) {
                    localX = localX / dist * HS_RADIUS;
                    localY = localY / dist * HS_RADIUS;
                }
                hsX = localX;
                hsY = localY;
                focusedHsX = hsX;
                focusedHsY = hsY;
                focusedPart = FocusPart.HS;
                updateColor(dragging);
                return;
            }
        }

        if (!hsGrabbed) {
            double localX = mouseX - (getX() + V_X);
            double localY = mouseY - getY();

            if (localX >= 0 && localX < V_WIDTH && localY >= 0 && localY < V_HEIGHT) {
                vGrabbed = true;
            }

            if (vGrabbed) {
                v = Math.clamp(localY, 0.0, 127.99);
                focusedV = v;
                focusedPart = FocusPart.V;
                updateColor(dragging);
            }
        }

    }

    private void updateColor(boolean dragging) {
        double dist = Math.sqrt(hsX * hsX + hsY * hsY);
        double hue = Math.atan2(hsY, hsX) / (Math.PI * 2.0);
        double sat = dist / 64.0;
        double lum = 1.0 - v / 128.0;

        color = Color.HSBtoRGB((float) hue, (float) sat, (float) lum);
        colorFullBrightness = Color.HSBtoRGB((float) hue, (float) sat, 1.f);
        callbackColorUpdated.accept(color, dragging);
    }

    private void applyFocusedSelection(boolean dragging) {
        hsX = focusedHsX;
        hsY = focusedHsY;
        v = focusedV;
        updateColor(dragging);
    }

    private FocusPart getEntryFocusPart(FocusNavigationEvent navigationEvent) {
        if (navigationEvent instanceof FocusNavigationEvent.TabNavigation tabNavigation) {
            return tabNavigation.forward() ? FocusPart.HS : FocusPart.V;
        }

        if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            return switch (arrowNavigation.direction()) {
                case LEFT -> FocusPart.V;
                case RIGHT -> FocusPart.HS;
                case UP, DOWN -> focusedPart;
            };
        }

        return switch (navigationEvent.getVerticalDirectionForInitialFocus()) {
            case UP, DOWN -> focusedPart;
            case LEFT -> FocusPart.V;
            case RIGHT -> FocusPart.HS;
        };
    }

    private void moveFocusedSelection(ScreenDirection direction, double amount) {
        if (focusedPart == FocusPart.HS) {
            switch (direction) {
                case LEFT -> setFocusedHs(focusedHsX - amount, focusedHsY);
                case RIGHT -> setFocusedHs(focusedHsX + amount, focusedHsY);
                case UP -> setFocusedHs(focusedHsX, focusedHsY - amount);
                case DOWN -> setFocusedHs(focusedHsX, focusedHsY + amount);
            }
            return;
        }

        if (direction == ScreenDirection.UP || direction == ScreenDirection.DOWN) {
            double nextV = direction == ScreenDirection.UP ? focusedV - amount : focusedV + amount;
            focusedV = Math.clamp(nextV, 0.0, 127.99);
        }
    }

    private void updateKeyboardMovement() {
        if (!isFocused() || !visible || !active || hsGrabbed || vGrabbed) {
            lastKeyboardMoveTimeNanos = -1L;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        boolean leftPressed = InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT);
        boolean rightPressed = InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT);
        boolean upPressed = InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_UP);
        boolean downPressed = InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN);

        if (!leftPressed && !rightPressed && !upPressed && !downPressed) {
            lastKeyboardMoveTimeNanos = -1L;
            return;
        }

        long now = System.nanoTime();
        if (lastKeyboardMoveTimeNanos == -1L) {
            lastKeyboardMoveTimeNanos = now;
            return;
        }

        double deltaSeconds = (now - lastKeyboardMoveTimeNanos) / 1_000_000_000.0;
        lastKeyboardMoveTimeNanos = now;
        if (deltaSeconds <= 0.0) {
            return;
        }

        if (focusedPart == FocusPart.HS) {
            double moveX = (rightPressed ? 1.0 : 0.0) - (leftPressed ? 1.0 : 0.0);
            double moveY = (downPressed ? 1.0 : 0.0) - (upPressed ? 1.0 : 0.0);
            if (moveX == 0.0 && moveY == 0.0) {
                return;
            }

            double magnitude = Math.sqrt(moveX * moveX + moveY * moveY);
            double speed = ScreenHelper.hasShiftDown() ? HS_FINE_SPEED : HS_SPEED;
            setFocusedHs(focusedHsX + moveX / magnitude * speed * deltaSeconds,
                    focusedHsY + moveY / magnitude * speed * deltaSeconds);
            return;
        }

        if (upPressed == downPressed) {
            return;
        }

        double speed = ScreenHelper.hasShiftDown() ? V_FINE_SPEED : V_SPEED;
        moveFocusedSelection(upPressed ? ScreenDirection.UP : ScreenDirection.DOWN, speed * deltaSeconds);
    }

    private void setFocusedHs(double newFocusedHsX, double newFocusedHsY) {
        double dist = Math.sqrt(newFocusedHsX * newFocusedHsX + newFocusedHsY * newFocusedHsY);
        if (dist > HS_RADIUS) {
            focusedHsX = newFocusedHsX / dist * HS_RADIUS;
            focusedHsY = newFocusedHsY / dist * HS_RADIUS;
            return;
        }

        focusedHsX = newFocusedHsX;
        focusedHsY = newFocusedHsY;
    }

    private void syncFocusedToSelection() {
        focusedHsX = hsX;
        focusedHsY = hsY;
        focusedV = v;
    }

    private void renderHsBackground(GuiGraphicsExtractor graphics) {
        int backgroundU = active ? HS_BACKGROUND_U : HS_BACKGROUND_DISABLED_U;
        int backgroundV = active ? HS_BACKGROUND_V : HS_BACKGROUND_DISABLED_V;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), backgroundU, backgroundV,
                HS_SIZE, HS_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private void renderVBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX() + V_X, getY(), V_BACKGROUND_U, V_BACKGROUND_V,
                V_WIDTH, V_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                active ? colorFullBrightness : ColorConstants.COLOR_PICKER_VALUE_DISABLED_TINT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX() + V_X, getY(), V_BORDER_U, V_BORDER_V,
                V_WIDTH, V_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT, ColorConstants.WHITE);
    }

    private void renderSelectedMarkers(GuiGraphicsExtractor graphics) {
        graphics.fill(getX() + (int) hsX + HS_CENTER, getY() + (int) hsY + HS_CENTER,
                getX() + (int) hsX + HS_CENTER + 1, getY() + (int) hsY + HS_CENTER + 1, colorFullBrightness);
        graphics.fill(getX() + V_SELECTION_X, getY() + (int) v, getX() + V_SELECTION_X + V_SELECTION_WIDTH - 2,
                getY() + (int) v + 1, color);

        int hsSelectionU = active ? HS_SELECTION_U : HS_SELECTION_DISABLED_U;
        int hsSelectionV = active ? HS_SELECTION_V : HS_SELECTION_DISABLED_V;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX() + (int) hsX + HS_CENTER - HS_SELECTION_OFFSET,
                getY() + (int) hsY + HS_CENTER - HS_SELECTION_OFFSET, hsSelectionU, hsSelectionV,
                HS_SELECTION_SIZE, HS_SELECTION_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        int vSelectionU = active ? V_SELECTION_U : V_SELECTION_DISABLED_U;
        int vSelectionV = active ? V_SELECTION_V : V_SELECTION_DISABLED_V;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX() + V_SELECTION_X, getY() + (int) v - V_MARKER_OFFSET_Y,
                vSelectionU, vSelectionV, V_SELECTION_WIDTH, V_SELECTION_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private void renderFocusedMarkers(GuiGraphicsExtractor graphics) {
        if (!isKeyboardFocused()) {
            return;
        }

        if (focusedPart == FocusPart.HS) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    getX() + (int) focusedHsX + HS_CENTER - HS_FOCUS_OFFSET,
                    getY() + (int) focusedHsY + HS_CENTER - HS_FOCUS_OFFSET,
                    HS_FOCUS_U, HS_FOCUS_V, HS_FOCUS_SIZE, HS_FOCUS_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            return;
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX() + V_SELECTION_X,
                getY() + (int) focusedV - V_MARKER_OFFSET_Y, V_FOCUS_U, V_FOCUS_V,
                V_FOCUS_WIDTH, V_FOCUS_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private boolean isKeyboardFocused() {
        return isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
    }
}
