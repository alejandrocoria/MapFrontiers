package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Optional;

public class ForgePlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public String getModVersion() {
        Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(MapFrontiers.MODID);
        if (modContainer.isPresent()) {
            return modContainer.get().getModInfo().getVersion().toString();
        }
        return "";
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
