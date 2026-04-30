package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class IconButton extends ButtonBase {
    public enum Type {
        Add            (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/add.png"),             39, 13),
        Remove         (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/remove.png"),          39, 13),
        Send           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/send.png"),            39, 13),
        Copy           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/copy.png"),            51, 17),
        Paste          (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/paste.png"),           51, 17),
        ExpandOptions  (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/expand_options.png"),  24, 17),
        CollapseOptions(Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/collapse_options.png"),24, 17),
        Undo           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/undo.png"),            51, 17),
        Redo           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/redo.png"),            51, 17),
        Swap           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/swap.png"),            27, 10),
        SortUp         (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/sort_up.png"),         21,  5),
        SortDown       (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/sort_down.png"),       21,  5),
        Collapsed      (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/collapsed.png"),       24,  8),
        Expanded       (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/expanded.png"),        24,  8);

        final Identifier texture;
        final int textureWidth;
        final int height;
        final int frameWidth;

        Type(Identifier texture, int textureWidth, int height) {
            if (textureWidth % 3 != 0) {
                throw new IllegalArgumentException("Icon texture width must be divisible by 3: " + textureWidth);
            }
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.height = height;
            frameWidth = textureWidth / 3;
        }
    }

    private Type type;

    public IconButton(Type type, OnPress pressedAction) {
        super(0, 0, type.frameWidth, type.height, Component.empty(), pressedAction, Button.DEFAULT_NARRATION);
        this.type = type;
    }

    public void setType(Type type) {
        this.type = type;
        setSize(type.frameWidth, type.height);
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int stateIndex = 0;
        if (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard()) {
            stateIndex = 2;
        } else if (isHovered()) {
            stateIndex = 1;
        }

        int u = type.frameWidth * stateIndex;
        graphics.blit(RenderPipelines.GUI_TEXTURED, type.texture, getX(), getY(), u, 0, width, height,
                type.textureWidth, type.height);
    }
}
