package games.alejandrocoria.mapfrontiers.platform.services;

import javax.annotation.Nullable;
import java.nio.file.Path;

public interface IPlatformHelper {
    String getPlatformName();
    String getModVersion();
    @Nullable String getModDisplayName(String modId);
    Path getConfigDirectory();
}
