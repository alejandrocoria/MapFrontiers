package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.mixin.client.GuiGraphicsAccessor;
import games.alejandrocoria.mapfrontiers.mixin.client.GuiRenderStateAccessor;
import journeymap.api.v2.client.ui.component.LayeredScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private int minimumWidth;
    private int minimumHeight;
    private final BottomButtonsMode bottomButtonsMode;
    protected int actualWidth;
    protected int actualHeight;

    protected LinearLayout content;
    private LinearLayout bottomButtons;

    public AutoScaledScreen(Component title) {
        this(title, BottomButtonsMode.None);
    }

    public AutoScaledScreen(Component title, BottomButtonsMode bottomButtonsMode) {
        super(title);
        this.bottomButtonsMode = bottomButtonsMode;
    }

    @Override
    public final void init() {
        scaleFactor = 1.f;
        actualWidth = width;
        actualHeight = height;

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
        resetContentToMinimumSize();
        arrangeLayouts();
        updateMinimumSizeFromLayout();
        updateScale(width, height);
        resizeContentToAvailableSpace();
        arrangeLayouts();
        positionContent();
        positionBottomButtons();
    }

    private void arrangeLayouts() {
        content.arrangeElements();
        if (bottomButtonsMode == BottomButtonsMode.Floating) {
            bottomButtons.arrangeElements();
        }
    }

    private void updateMinimumSizeFromLayout() {
        minimumWidth = content.getWidth() + getMinimumLayoutExtraWidth();
        minimumHeight = content.getHeight() + getMinimumLayoutExtraHeight();

        if (bottomButtonsMode == BottomButtonsMode.Floating) {
            minimumWidth = Math.max(minimumWidth, bottomButtons.getWidth() + getMinimumLayoutExtraWidth());
            int bottomButtonsHeight = bottomButtons.getHeight() + LayoutConstants.FLOATING_BUTTON_BOTTOM_MARGIN;
            minimumHeight = Math.max(minimumHeight, content.getHeight()
                    + Math.max(getMinimumLayoutExtraHeight(), bottomButtonsHeight * 2));
        }
    }

    protected int getMinimumLayoutExtraWidth() {
        return LayoutConstants.BOX_PADDING * 2;
    }

    protected int getMinimumLayoutExtraHeight() {
        return LayoutConstants.BOX_PADDING * 2;
    }

    protected void resetContentToMinimumSize() {
    }

    protected void resizeContentToAvailableSpace() {
    }

    protected void positionContent() {
        content.setPosition((actualWidth - content.getWidth()) / 2, (actualHeight - content.getHeight()) / 2);
    }

    private void positionBottomButtons() {
        if (bottomButtonsMode == BottomButtonsMode.Floating) {
            bottomButtons.setPosition((actualWidth - bottomButtons.getWidth()) / 2,
                    actualHeight - bottomButtons.getHeight() - LayoutConstants.FLOATING_BUTTON_BOTTOM_MARGIN);
        }
    }

    protected int availableWidth(int horizontalMargin) {
        return Math.max(0, actualWidth - horizontalMargin);
    }

    protected int availableHeight(int verticalMargin) {
        return Math.max(0, actualHeight - verticalMargin);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        super.resize(width, height);
    }

    private void updateScale(int width, int height) {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(minecraft, this, minimumWidth, minimumHeight);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);
    }

    protected abstract void initScreen();

    protected void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {}

    protected void renderScaledScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {}

    protected SimpleButton addBottomButton(SimpleButton child) {
        if (bottomButtons == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " was created with BottomButtonsMode.None");
        }

        return bottomButtons.addChild(child);
    }

    @Override
    protected final void renderPopupScreenBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
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
    protected final void renderPopupScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        mouseX = (int) (mouseX * scaleFactor);
        mouseY = (int) (mouseY * scaleFactor);

        if (scaleFactor != 1.f) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(1.0f / scaleFactor, 1.0f / scaleFactor);
        }

        if (title.getContents() != PlainTextContents.EMPTY) {
            graphics.centeredText(font, title, this.actualWidth / 2, 12, ColorConstants.WHITE);
        }

        renderScaledBackgroundScreen(graphics, mouseX, mouseY, partialTicks);

        for (GuiEventListener child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTicks);
            }
        }

        renderScaledScreen(graphics, mouseX, mouseY, partialTicks);

        if (minecraft.screen == this) {
            graphics.extractDeferredElements(mouseX, mouseY, partialTicks);
        }

        if (scaleFactor != 1.f) {
            graphics.pose().popMatrix();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.input() == GLFW.GLFW_KEY_ESCAPE || shouldCloseFromInventoryKey(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    protected boolean isTextFieldFocused() {
        return getFocused() instanceof EditBox editBox && editBox.isFocused();
    }

    private boolean shouldCloseFromInventoryKey(KeyEvent event) {
        return minecraft != null && minecraft.options.keyInventory.matches(event) && !isTextFieldFocused();
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

    protected void drawCenteredBoxBackground(GuiGraphicsExtractor graphics, int width, int height) {
        int x1 = (actualWidth - width) / 2;
        int x2 = (actualWidth + width) / 2 - 1;
        int y1 = (actualHeight - height) / 2;
        int y2 = (actualHeight + height) / 2 - 1;
        graphics.fill(x1, y1, x2, y2, ColorConstants.SCREEN_BG);
        graphics.horizontalLine(x1, x2, y1, ColorConstants.TAB_BORDER);
        graphics.horizontalLine(x1, x2, y2, ColorConstants.TAB_BORDER);
        graphics.verticalLine(x1, y1, y2, ColorConstants.TAB_BORDER);
        graphics.verticalLine(x2, y1, y2, ColorConstants.TAB_BORDER);
    }

    protected void drawCenteredBoxBackground(GuiGraphicsExtractor graphics) {
        int padding = LayoutConstants.BOX_PADDING * 2;
        drawCenteredBoxBackground(graphics, content.getWidth() + padding, content.getHeight() + padding);
    }
}
