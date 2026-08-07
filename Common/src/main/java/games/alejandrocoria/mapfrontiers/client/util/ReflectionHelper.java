package games.alejandrocoria.mapfrontiers.client.util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@ParametersAreNonnullByDefault
public final class ReflectionHelper {
    @SuppressWarnings("unchecked")
    public static <T> T getPrivateField(Object obj, String fieldName)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }

    public static boolean hasPublicMethod(Class<?> owner, String methodName, int parameterCount) {
        return findPublicMethod(owner, methodName, parameterCount) != null;
    }

    public static boolean invokePublicMethodIfPresent(Object target, String methodName, Object... arguments) {
        Method method = findPublicMethod(target.getClass(), methodName, arguments.length);
        if (method == null) {
            return false;
        }

        try {
            method.invoke(target, arguments);
            return true;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke " + target.getClass().getName() + "." + methodName, e);
        }
    }

    private static Method findPublicMethod(Class<?> owner, String methodName, int parameterCount) {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    private ReflectionHelper() {
    }
}
