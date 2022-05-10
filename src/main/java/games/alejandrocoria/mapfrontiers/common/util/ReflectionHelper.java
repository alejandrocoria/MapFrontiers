package games.alejandrocoria.mapfrontiers.common.util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;

@ParametersAreNonnullByDefault
public class ReflectionHelper {
    @SuppressWarnings("unchecked")
    public static <T> T getPrivateField(Object obj, String fieldName)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }

    private ReflectionHelper() {

    }
}
