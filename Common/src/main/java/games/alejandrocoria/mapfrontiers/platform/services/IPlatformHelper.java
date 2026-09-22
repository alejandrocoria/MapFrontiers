package games.alejandrocoria.mapfrontiers.platform.services;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.nio.file.Path;

public interface IPlatformHelper {
    String getPlatformName();
    String getModVersion();
    @Nullable String getModDisplayName(String modId);
    Path getConfigDirectory();
    boolean isDevelopmentEnvironment();

    default WorldGeometry getWorldGeometry(Level level) {
        return WorldGeometry.FLAT;
    }

    default WorldGeometry getClientWorldGeometry(ResourceKey<Level> dimension) {
        return WorldGeometry.FLAT;
    }
}
