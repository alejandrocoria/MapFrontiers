package games.alejandrocoria.mapfrontiers.client.gui.screen;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import journeymap.api.v2.client.ui.component.LayeredScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.lwjgl.glfw.GLFW;

public abstract class AutoScaledScreen extends LayeredScreen {
    private float scaleFactor = 1.f;
    private final int minWidth;
    private final int minHeight;
    protected int actualWidth;
    protected int actualHeight;

    protected LinearLayout content;
    protected LinearLayout bottomButtons;

    public AutoScaledScreen(Component title) {
        this(title, 0, 0);
    }

    public AutoScaledScreen(Component title, int minWidth, int minHeight) {
        super(title);
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    @Override
    public final void init() {
        updateScale(width, height);

        content = LinearLayout.vertical();
        bottomButtons = LinearLayout.horizontal();
        initScreen();
        content.visitWidgets(this::addRenderableWidget);
        content.visitWidgets((w) -> w.setTabOrderGroup(0));
        bottomButtons.visitWidgets(this::addRenderableWidget);
        bottomButtons.visitWidgets((w) -> w.setTabOrderGroup(1));
        bottomButtons.spacing(7);

        repositionElements();
    }

    @Override
    public void repositionElements() {
        content.arrangeElements();
        content.setPosition((actualWidth - content.getWidth()) / 2, (actualHeight - content.getHeight()) / 2);
        bottomButtons.arrangeElements();
        bottomButtons.setPosition((actualWidth - bottomButtons.getWidth()) / 2, actualHeight - 15 - bottomButtons.getHeight() / 2);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        updateScale(width, height);
        super.resize(minecraft, width, height);
    }

    private void updateScale(int width, int height) {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(minecraft, this, minWidth, minHeight);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);
    }

    protected abstract void initScreen();

    protected void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {}

    protected void renderScaledScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {}

    @Override
    protected final void renderPopupScreenBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBlurredBackground(partialTicks);
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
            graphics.drawCenteredString(font, title, this.actualWidth / 2, 11, ColorConstants.WHITE);
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
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, value, modifier);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX * scaleFactor, mouseY * scaleFactor, button);
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
        int x2 = (actualWidth + width) / 2;
        int y1 = (actualHeight - height) / 2;
        int y2 = (actualHeight + height) / 2;
        graphics.fill(x1, y1, x2, y2, ColorConstants.SCREEN_BG);
        graphics.hLine(x1, x2, y1, ColorConstants.TAB_BORDER);
        graphics.hLine(x1, x2, y2, ColorConstants.TAB_BORDER);
        graphics.vLine(x1, y1, y2, ColorConstants.TAB_BORDER);
        graphics.vLine(x2, y1, y2, ColorConstants.TAB_BORDER);
    }
}
