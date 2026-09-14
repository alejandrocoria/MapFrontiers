package games.alejandrocoria.mapfrontiers.client.util;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class PlayerNameFormatter {
    public static String getDisplayName(@Nullable PlayerId playerId) {
        return getDisplayName(playerId, I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC));
    }

    public static String getDisplayName(@Nullable PlayerId playerId, String blank) {
        if (playerId == null) {
            return blank;
        }

        String username = MapFrontiersClient.resolvePlayerName(playerId);
        return StringUtils.isBlank(username) ? playerId.uuid().toString() : username;
    }

    private PlayerNameFormatter() {
    }
}
