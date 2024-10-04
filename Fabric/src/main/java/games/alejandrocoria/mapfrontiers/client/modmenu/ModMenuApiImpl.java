package games.alejandrocoria.mapfrontiers.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.gui.ModsScreen;
import games.alejandrocoria.mapfrontiers.client.gui.screen.ModSettings;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return s -> {
            if (s instanceof ModsScreen) {
                boolean hasConfigScreen = ((ModsScreen) s).getModHasConfigScreen().getOrDefault("mapfrontiers", false);
                if (!hasConfigScreen) {
                    return ModSettings.createDummy();
                }
            }
            ModSettings screen = new ModSettings(true);
            screen.display();
            return screen;
        };
    }
}
