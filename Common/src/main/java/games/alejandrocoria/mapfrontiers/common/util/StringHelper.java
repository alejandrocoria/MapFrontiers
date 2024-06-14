package games.alejandrocoria.mapfrontiers.common.util;

import net.minecraft.client.gui.Font;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class StringHelper {
    public static int getMaxWidth(Font font, String... strings) {
        int maxWidth = 0;

        for (String s : strings) {
            int width = font.width(s);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        return maxWidth;
    }

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
