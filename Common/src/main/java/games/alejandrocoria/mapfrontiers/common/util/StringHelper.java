package games.alejandrocoria.mapfrontiers.common.util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public final class StringHelper {
    public static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    public static <E extends Enum<E>> String enumValuesToString(List<E> list) {
        if (list.isEmpty()) {
            return "";
        }

        if (list.size() == 1) {
            return list.get(0).name();
        }

        StringBuilder string = new StringBuilder(list.get(0).name());
        for (int i2 = 1; i2 < list.size() - 1; ++i2) {
            string.append(", ");
            string.append(list.get(i2).name());
        }

        string.append(" or ");
        string.append(list.get(list.size() - 1).name());
        return string.toString();
    }

    private StringHelper() {
    }
}
