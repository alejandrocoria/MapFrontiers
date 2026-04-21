package games.alejandrocoria.mapfrontiers.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.ModSettingsPage;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return s -> {
            ModSettingsPage screen = new ModSettingsPage(true);
            screen.display();
            return screen;
        };
    }
}
