package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public void addButtonToScreen(AbstractButton button, Screen screen) {
        Screens.getButtons(screen).add(button);
    }

    @Override
    public void removeButtonOfScreen(AbstractButton button, Screen screen) {
        Screens.getButtons(screen).remove(button);
    }

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public String getModVersion() {
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(MapFrontiers.MODID);
        if (modContainer.isPresent()) {
            return modContainer.get().getMetadata().getVersion().getFriendlyString();
        }
        return "";
    }
}
