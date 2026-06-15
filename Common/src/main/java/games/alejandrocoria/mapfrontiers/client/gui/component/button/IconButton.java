package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class IconButton extends ButtonBase {
    private static final long RANDOM_ICON_SWAP_INTERVAL_MS = 200L;
    private static final RandomSource RANDOM = RandomSource.create();

    public enum Type {
        Add            (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/add.png"),             33, 11),
        Remove         (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/remove.png"),          33, 11),
        Send           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/send.png"),            33, 11),
        MoveHere       (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/move_here.png"),       33, 11),
        Show           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/show.png"),            33, 11),
        Hide           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/hide.png"),            33, 11),
        Random         (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/random.png"),          48, 96, 6),
        Copy           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/copy.png"),            51, 17),
        Paste          (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/paste.png"),           51, 17),
        ExpandOptions  (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/expand_options.png"),  24, 17),
        CollapseOptions(Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/collapse_options.png"),24, 17),
        Undo           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/undo.png"),            51, 17),
        Redo           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/redo.png"),            51, 17),
        Swap           (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/swap.png"),            27, 10),
        SortUp         (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/sort_up.png"),         21,  5),
        SortDown       (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/sort_down.png"),       21,  5),
        Collapsed      (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/collapsed.png"),       33, 11),
        Expanded       (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/expanded.png"),        33, 11),
        RestoreDefault (Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/icon_buttons/restore_default.png"), 39, 13);

        final Identifier texture;
        final int textureWidth;
        final int textureHeight;
        final int variantCount;
        final int frameWidth;
        final int frameHeight;

        Type(Identifier texture, int textureWidth, int textureHeight) {
            this(texture, textureWidth, textureHeight, 1);
        }

        Type(Identifier texture, int textureWidth, int textureHeight, int variantCount) {
            if (textureWidth % 3 != 0) {
                throw new IllegalArgumentException("Icon texture width must be divisible by 3: " + textureWidth);
            }
            if (textureHeight % variantCount != 0) {
                throw new IllegalArgumentException("Icon texture height must be divisible by variant count: " + textureHeight);
            }
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.variantCount = variantCount;
            frameWidth = textureWidth / 3;
            frameHeight = textureHeight / variantCount;
        }

        boolean hasVariants() {
            return variantCount > 1;
        }

        boolean shouldSkipVariant(int currentVariant, int nextVariant) {
            // Avoid opposite dice faces; with rows ordered as faces 1-6, opposite variant indexes sum to 5.
            return nextVariant == currentVariant || this == Random && currentVariant + nextVariant == 5;
        }
    }

    private Type type;
    private int currentVariant;
    private long nextVariantChangeTime;
    private boolean wasAnimating;

    public IconButton(Type type, OnPress pressedAction) {
        super(0, 0, type.frameWidth, type.frameHeight, Component.empty(), pressedAction, Button.DEFAULT_NARRATION);
        setType(type);
    }

    public void setType(Type type) {
        this.type = type;
        setSize(type.frameWidth, type.frameHeight);
        currentVariant = randomVariant(type, -1);
        nextVariantChangeTime = 0L;
        wasAnimating = false;
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int stateIndex = 0;
        int color = ColorConstants.ICON_COLOR_NORMAL;
        if (!active) {
            color = ColorConstants.ICON_COLOR_DISABLED;
        } else if (isKeyboardFocused()) {
            stateIndex = 2;
        } else if (isHovered()) {
            stateIndex = 1;
        }

        updateAnimatedVariant(active && isHoveredOrKeyboardFocused());

        int u = type.frameWidth * stateIndex;
        int v = type.frameHeight * currentVariant;
        graphics.blit(RenderPipelines.GUI_TEXTURED, type.texture, getX(), getY(), u, v, width, height,
                type.textureWidth, type.textureHeight, color);
    }

    private void updateAnimatedVariant(boolean animating) {
        if (!type.hasVariants()) {
            return;
        }

        if (!animating) {
            wasAnimating = false;
            return;
        }

        long currentTime = Util.getMillis();
        if (!wasAnimating) {
            nextVariantChangeTime = currentTime + RANDOM_ICON_SWAP_INTERVAL_MS;
            wasAnimating = true;
            return;
        }

        if (currentTime >= nextVariantChangeTime) {
            currentVariant = randomVariant(type, currentVariant);
            nextVariantChangeTime = currentTime + RANDOM_ICON_SWAP_INTERVAL_MS;
        }
    }

    private static int randomVariant(Type type, int currentVariant) {
        if (!type.hasVariants()) {
            return 0;
        }

        int nextVariant;
        do {
            nextVariant = RANDOM.nextInt(type.variantCount);
        } while (currentVariant >= 0 && type.shouldSkipVariant(currentVariant, nextVariant));

        return nextVariant;
    }
}
