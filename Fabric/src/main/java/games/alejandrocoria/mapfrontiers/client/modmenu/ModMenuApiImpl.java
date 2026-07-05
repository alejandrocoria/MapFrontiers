package games.alejandrocoria.mapfrontiers.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.gui.ModsScreen;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.screen.page.ModSettingsPage;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> {
            if (screen instanceof ModsScreen modsScreen) {
                boolean hasConfigScreen = modsScreen.getModHasConfigScreen().getOrDefault(MapFrontiers.MODID, false);
                if (!hasConfigScreen) {
                    return ModSettingsPage.createDummy();
                }
            }
            return new ModSettingsPage(screen, true);
        };
    }
}
