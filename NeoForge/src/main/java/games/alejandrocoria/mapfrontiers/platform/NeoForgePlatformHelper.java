package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.platform.services.IPlatformHelper;
import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;

public class NeoForgePlatformHelper implements IPlatformHelper {
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

    @Override
    public @Nullable String getModDisplayName(String modId) {
        Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(modId);
        if (modContainer.isPresent()) {
            return modContainer.get().getModInfo().getDisplayName();
        }
        return null;
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
    }

    @Override
    public WorldGeometry getWorldGeometry(Level level) {
        return ModList.get().isLoaded("toroidal_world")
                ? NeoForgeToroidalWorldServer.geometryOf(level)
                : WorldGeometry.FLAT;
    }

    @Override
    public WorldGeometry getClientWorldGeometry(ResourceKey<Level> dimension) {
        return ModList.get().isLoaded("toroidal_world")
                ? NeoForgeToroidalWorldClient.geometryOf(dimension)
                : WorldGeometry.FLAT;
    }
}
