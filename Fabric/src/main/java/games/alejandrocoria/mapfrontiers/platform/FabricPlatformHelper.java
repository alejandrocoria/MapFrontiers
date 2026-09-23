package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.platform.services.IPlatformHelper;
import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;

public class FabricPlatformHelper implements IPlatformHelper {
    private static class OptionalMods {
        static final boolean TOROIDAL_WORLD = FabricLoader.getInstance().isModLoaded("toroidal_world");
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

    @Override
    public @Nullable String getModDisplayName(String modId) {
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(modId);
        if (modContainer.isPresent()) {
            return modContainer.get().getMetadata().getName();
        }
        return null;
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public WorldGeometry getWorldGeometry(Level level) {
        return OptionalMods.TOROIDAL_WORLD
                ? FabricToroidalWorldServer.geometryOf(level)
                : WorldGeometry.FLAT;
    }

    @Override
    public WorldGeometry getClientWorldGeometry(ResourceKey<Level> dimension) {
        return OptionalMods.TOROIDAL_WORLD
                ? FabricToroidalWorldClient.geometryOf(dimension)
                : WorldGeometry.FLAT;
    }
}
