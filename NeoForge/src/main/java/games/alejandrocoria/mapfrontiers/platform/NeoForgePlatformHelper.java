package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.platform.services.IPlatformHelper;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

import java.util.Optional;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public void addButtonToScreen(AbstractButton button, Screen screen) {
        screen.renderables.add(button);
    }

    @Override
    public void removeButtonOfScreen(AbstractButton button, Screen screen) {
        screen.renderables.remove(button);
    }

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public String getModVersion() {
        Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(MapFrontiers.MODID);
        if (modContainer.isPresent()) {
            return modContainer.get().getModInfo().getVersion().toString();
        }
        return "";
    }
}
