package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import journeymap.api.v2.client.ui.component.LayeredScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
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
    public void resize(Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        super.resize(minecraft, width, height);
    }

    private void updateScale(int width, int height) {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(minecraft, this, minimumWidth, minimumHeight);
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
            renderBlurredBackground(partialTicks);
        }
    }

    @Override
    protected final void renderPopupScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        mouseX = (int) (mouseX * scaleFactor);
        mouseY = (int) (mouseY * scaleFactor);

        if (scaleFactor != 1.f) {
            graphics.pose().pushPose();
            graphics.pose().scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

        if (title.getContents() != PlainTextContents.EMPTY) {
            graphics.drawCenteredString(font, title, this.actualWidth / 2, 12, ColorConstants.SCREEN_TITLE_TEXT);
        }

        renderScaledBackgroundScreen(graphics, mouseX, mouseY, partialTicks);

        for (GuiEventListener child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        renderScaledScreen(graphics, mouseX, mouseY, partialTicks);

        if (scaleFactor != 1.f) {
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || shouldCloseFromInventoryKey(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected boolean isTextFieldFocused() {
        return getFocused() instanceof EditBox editBox && editBox.isFocused();
    }

    private boolean shouldCloseFromInventoryKey(int keyCode, int scanCode) {
        return minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode) && !isTextFieldFocused();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double scaledMouseX = mouseX * scaleFactor;
        double scaledMouseY = mouseY * scaleFactor;
        GuiEventListener focused = getFocused();

        if (focused instanceof EditBox editBox && editBox.isFocused() && !editBox.isMouseOver(scaledMouseX, scaledMouseY)) {
            // Text fields apply pending edits when they lose focus, so clear focus before the newly clicked widget processes the event.
            setFocused(null);
        }

        return super.mouseClicked(scaledMouseX, scaledMouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX * scaleFactor, mouseY * scaleFactor, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hDelta, double vDelta) {
        return super.mouseScrolled(mouseX * scaleFactor, mouseY * scaleFactor, hDelta, vDelta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX * scaleFactor, mouseY * scaleFactor, button, dragX * scaleFactor, dragY * scaleFactor);
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
        graphics.fill(x1, y1, x2, y2, ColorConstants.SCREEN_FRAME_BG);
        graphics.renderOutline(x1, y1, x2 - x1 + 1, y2 - y1 + 1, ColorConstants.TAB_BORDER_NORMAL);
    }

    protected void drawCenteredBoxBackground(GuiGraphics graphics) {
        int padding = LayoutConstants.BOX_PADDING * 2;
        drawCenteredBoxBackground(graphics, content.getWidth() + padding, content.getHeight() + padding);
    }
}
