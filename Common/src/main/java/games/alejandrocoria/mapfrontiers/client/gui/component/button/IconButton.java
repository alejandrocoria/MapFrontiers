package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class IconButton extends ButtonBase {
    private static final Identifier texture = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/buttons.png");
    private static final int textureSizeX = 34;
    private static final int textureSizeY = 133;

    public enum Type {
        Add       ( 0,   0, 13,   0, 13, 13),
        Remove    ( 0,  13, 13,  13, 13, 13),
        Send      ( 0,  26, 13,  26, 13, 13),
        Copy      ( 0,  39, 17,  39, 17, 17),
        Paste     ( 0,  56, 17,  56, 17, 17),
        ArrowUp   ( 0,  73,  8,  73,  8, 17),
        ArrowDown (18,  73, 26,  73,  8, 17),
        Undo      ( 0,  90, 17,  90, 17, 17),
        Redo      ( 0, 107, 17, 107, 17, 17),
        Swap      ( 0, 124,  9, 124,  9,  9),
        SortUp    (27,   0, 27,   6,  7,  6),
        SortDown  (27,  12, 27,  18,  7,  6);

        final int texX;
        final int texY;
        final int texHoverX;
        final int texHoverY;
        final int width;
        final int height;

        Type(int texX, int texY, int texHoverX, int texHoverY, int width, int height) {
            this.texX = texX;
            this.texY = texY;
            this.texHoverX = texHoverX;
            this.texHoverY = texHoverY;
            this.width = width;
            this.height = height;
        }
    }

    private Type type;

    public IconButton(Type type, OnPress pressedAction) {
        super(0, 0, type.width, type.height, Component.empty(), pressedAction, Button.DEFAULT_NARRATION);
        this.type = type;
    }

    public void setType(Type type) {
        this.type = type;
        setSize(type.width, type.height);
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (isHoveredOrKeyboardFocused()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), type.texHoverX, type.texHoverY, width, height, textureSizeX, textureSizeY);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), type.texX, type.texY, width, height, textureSizeX, textureSizeY);
        }
    }
}
