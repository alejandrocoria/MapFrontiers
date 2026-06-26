package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public abstract class ButtonBase extends Button {
    protected ButtonBase(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(x, y, width, height, message, onPress, createNarration);
    }

    protected boolean isHoveredOrKeyboardFocused() {
        return isHovered() || (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard());
    }

    protected boolean isKeyboardFocused() {
        return isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
    }
}
