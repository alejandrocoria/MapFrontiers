package games.alejandrocoria.mapfrontiers.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {
    String getPlatformName();
    String getModVersion();
    Path getConfigDirectory();
}
