package games.alejandrocoria.mapfrontiers.client.util;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class SettingsUserFormatter {
    public static String getDisplayName(SettingsUser user) {
        return getDisplayName(user, I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC));
    }

    public static String getDisplayName(SettingsUser user, String blank) {
        if (!StringUtils.isBlank(user.username)) {
            return user.username;
        }

        if (user.uuid != null) {
            return user.uuid.toString();
        }

        return blank;
    }

    private SettingsUserFormatter() {
    }
}
