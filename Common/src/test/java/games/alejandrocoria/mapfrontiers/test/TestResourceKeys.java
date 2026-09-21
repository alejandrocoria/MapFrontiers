package games.alejandrocoria.mapfrontiers.test;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public final class TestResourceKeys {
    private TestResourceKeys() {
    }

    @SuppressWarnings("unchecked")
    public static ResourceKey<Level> dimension(String path) {
        try {
            MinecraftTestBootstrap.initialize();
            Method create = ResourceKey.class.getDeclaredMethod("create", ResourceLocation.class, ResourceLocation.class);
            create.setAccessible(true);
            return (ResourceKey<Level>) create.invoke(null, new ResourceLocation("minecraft", "dimension"),
                    new ResourceLocation("minecraft", path));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to create test dimension key", e);
        }
    }
}
