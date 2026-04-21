package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.mixin.GuiGraphicsAccessor;
import games.alejandrocoria.mapfrontiers.client.mixin.GuiRenderStateAccessor;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import journeymap.api.v2.client.ui.component.LayeredScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class AutoScaledScreen extends LayeredScreen {
    public enum BottomButtonsMode {
        None,
        Floating,
        Integrated
    }

    protected float scaleFactor = 1.f;
    private final int minWidth;
    private final int minHeight;
    private final BottomButtonsMode bottomButtonsMode;
    protected int actualWidth;
    protected int actualHeight;

    protected LinearLayout content;
    private LinearLayout bottomButtons;

    public AutoScaledScreen(Component title) {
        this(title, 0, 0, BottomButtonsMode.None);
    }

    public AutoScaledScreen(Component title, int minWidth, int minHeight, BottomButtonsMode bottomButtonsMode) {
        super(title);
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.bottomButtonsMode = bottomButtonsMode;
    }

    @Override
    public final void init() {
        updateScale(width, height);

        content = LinearLayout.vertical().spacing(LayoutConstants.SCREEN_CONTENT_SPACING);
        content.defaultCellSetting().alignHorizontallyCenter();

        if (bottomButtonsMode != BottomButtonsMode.None) {
            bottomButtons = LinearLayout.horizontal();
            bottomButtons.spacing(LayoutConstants.BOTTOM_BUTTON_SPACING);
        } else {
            bottomButtons = null;
        }

        initScreen();

        if (bottomButtonsMode == BottomButtonsMode.Integrated) {
            content.addChild(bottomButtons);
        }

        content.visitWidgets(this::addRenderableWidget);
        content.visitWidgets((w) -> w.setTabOrderGroup(0));

        if (bottomButtonsMode == BottomButtonsMode.Floating) {
            bottomButtons.visitWidgets(this::addRenderableWidget);
        }

        if (bottomButtonsMode != BottomButtonsMode.None) {
            bottomButtons.visitWidgets((w) -> w.setTabOrderGroup(1));
        }

        repositionElements();
    }

    @Override
    public void repositionElements() {
        content.arrangeElements();
        content.setPosition((actualWidth - content.getWidth()) / 2, (actualHeight - content.getHeight()) / 2);
        if (bottomButtonsMode != BottomButtonsMode.None) {
            bottomButtons.arrangeElements();
            if (bottomButtonsMode == BottomButtonsMode.Floating) {
                bottomButtons.setPosition((actualWidth - bottomButtons.getWidth()) / 2, actualHeight - bottomButtons.getHeight() - LayoutConstants.FLOATING_BUTTON_BOTTOM_MARGIN);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        updateScale(width, height);
        super.resize(width, height);
    }

    private void updateScale(int width, int height) {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(minecraft, this, minWidth, minHeight);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);
    }

    protected abstract void initScreen();

    protected void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {}

    protected void renderScaledScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {}

    protected SimpleButton addBottomButton(SimpleButton child) {
        if (bottomButtons == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " was created with BottomButtonsMode.None");
        }

        return bottomButtons.addChild(child);
    }

    @Override
    protected final void renderPopupScreenBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (minecraft.screen == this) {
            // Do not draw blur if it has already been drawn because Minecraft throws an exception for some reason.
            if (((GuiRenderStateAccessor) ((GuiGraphicsAccessor) graphics).mapfrontiers$getGuiRenderState()).mapfrontiers$setFirstStratumAfterBlur() == Integer.MAX_VALUE) {
                graphics.blurBeforeThisStratum();
            } else {
                graphics.fill(0, 0, width, height, 0xBF000000);
            }
        }
    }

    @Override
    protected final void renderPopupScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        mouseX = (int) (mouseX * scaleFactor);
        mouseY = (int) (mouseY * scaleFactor);

        if (scaleFactor != 1.f) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(1.0f / scaleFactor, 1.0f / scaleFactor);
        }

        if (title.getContents() != PlainTextContents.EMPTY) {
            graphics.drawCenteredString(font, title, this.actualWidth / 2, 12, ColorConstants.WHITE);
        }

        renderScaledBackgroundScreen(graphics, mouseX, mouseY, partialTicks);

        for (GuiEventListener child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        renderScaledScreen(graphics, mouseX, mouseY, partialTicks);

        if (minecraft.screen == this) {
            graphics.renderDeferredElements();
        }

        if (scaleFactor != 1.f) {
            graphics.pose().popMatrix();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.input() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        MouseButtonEvent scaledEvent = new MouseButtonEvent(event.x() * scaleFactor, event.y() * scaleFactor, event.buttonInfo());
        GuiEventListener focused = getFocused();

        if (focused instanceof EditBox editBox && editBox.isFocused() && !editBox.isMouseOver(scaledEvent.x(), scaledEvent.y())) {
            // Text fields apply pending edits when they lose focus, so clear focus before the newly clicked widget processes the event.
            setFocused(null);
        }

        return super.mouseClicked(scaledEvent, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        MouseButtonEvent scaledEvent = new MouseButtonEvent(event.x() * scaleFactor, event.y() * scaleFactor, event.buttonInfo());
        return super.mouseReleased(scaledEvent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hDelta, double vDelta) {
        return super.mouseScrolled(mouseX * scaleFactor, mouseY * scaleFactor, hDelta, vDelta);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent scaledEvent = new MouseButtonEvent(event.x() * scaleFactor, event.y() * scaleFactor, event.buttonInfo());
        return super.mouseDragged(scaledEvent, dragX * scaleFactor, dragY * scaleFactor);
    }

    protected void closeAndReturnToFullscreenMap() {
        if (minecraft == null) {
            return;
        }

        onClose();
        if (minecraft.screen != null && minecraft.screen instanceof AutoScaledScreen autoScaledScreen) {
            autoScaledScreen.closeAndReturnToFullscreenMap();
        }
    }

    protected void drawCenteredBoxBackground(GuiGraphics graphics, int width, int height) {
        int x1 = (actualWidth - width) / 2;
        int x2 = (actualWidth + width) / 2 - 1;
        int y1 = (actualHeight - height) / 2;
        int y2 = (actualHeight + height) / 2 - 1;
        graphics.fill(x1, y1, x2, y2, ColorConstants.SCREEN_BG);
        graphics.hLine(x1, x2, y1, ColorConstants.TAB_BORDER);
        graphics.hLine(x1, x2, y2, ColorConstants.TAB_BORDER);
        graphics.vLine(x1, y1, y2, ColorConstants.TAB_BORDER);
        graphics.vLine(x2, y1, y2, ColorConstants.TAB_BORDER);
    }

    protected void drawCenteredBoxBackground(GuiGraphics graphics) {
        int padding = LayoutConstants.BOX_PADDING * 2;
        drawCenteredBoxBackground(graphics, content.getWidth() + padding, content.getHeight() + padding);
    }
}
