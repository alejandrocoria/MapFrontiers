package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapHelper;
import games.alejandrocoria.mapfrontiers.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IJourneyMapHelper JOURNEYMAP = load(IJourneyMapHelper.class);

    public static <T> T load(Class<T> clazz) {

        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}
