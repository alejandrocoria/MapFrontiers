package games.alejandrocoria.mapfrontiers.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.gui.ModsScreen;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return s -> {
            if (s instanceof ModsScreen) {
                boolean hasConfigScreen = ((ModsScreen) s).getModHasConfigScreen().getOrDefault("mapfrontiers", false);
                if (!hasConfigScreen) {
                    return GuiFrontierSettings.createDummy();
                }
            }
            return new GuiFrontierSettings(s, true);
        };
    }
}
