package games.alejandrocoria.mapfrontiers.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.ModSettings;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return s -> {
            ModSettings screen = new ModSettings(true);
            screen.display();
            return screen;
        };
    }
}
