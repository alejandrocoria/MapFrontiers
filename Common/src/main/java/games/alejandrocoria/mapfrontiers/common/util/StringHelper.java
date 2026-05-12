package games.alejandrocoria.mapfrontiers.common.util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public final class StringHelper {
    public static <E extends Enum<E>> String enumValuesToString(List<E> list) {
        if (list.isEmpty()) {
            return "";
        }

        if (list.size() == 1) {
            return list.getFirst().name();
        }

        StringBuilder string = new StringBuilder(list.getFirst().name());
        for (int i2 = 1; i2 < list.size() - 1; ++i2) {
            string.append(", ");
            string.append(list.get(i2).name());
        }

        string.append(" or ");
        string.append(list.getLast().name());
        return string.toString();
    }

    private StringHelper() {
    }
}
