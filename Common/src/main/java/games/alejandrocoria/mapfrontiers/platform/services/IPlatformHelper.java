package games.alejandrocoria.mapfrontiers.platform.services;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;

public interface IPlatformHelper {
    void pushGuiLayer(Screen screen);
    void popGuiLayer();
    void addButtonToScreen(AbstractButton button, Screen screen);
    void removeButtonOfScreen(AbstractButton button, Screen screen);
    String getPlatformName();
    String getModVersion();
}
