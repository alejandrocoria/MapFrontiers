package games.alejandrocoria.mapfrontiers.common.settings;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class SettingsUser implements Comparable<SettingsUser> {
    public String username;
    public UUID uuid;

    public SettingsUser() {
        username = "";
    }

    public SettingsUser(Player player) {
        username = player.getName().getString();
        uuid = player.getUUID();
    }

    public boolean isEmpty() {
        return uuid == null && StringUtils.isBlank(username);
    }

    public void fillMissingInfo(boolean forceNameUpdate, @Nullable MinecraftServer server) {
        if (isEmpty()) {
            return;
        }

        if (uuid == null) {
            uuid = UUIDHelper.getUUIDFromName(username, server);
        } else if (StringUtils.isBlank(username) || forceNameUpdate) {
            String newUsername = UUIDHelper.getNameFromUUID(uuid, server);
            if (newUsername != null) {
                username = newUsername;
            } else if (username == null) {
                username = "";
            }
        }
    }

    public void readFromNBT(CompoundTag nbt) {
        username = nbt.getStringOr("username", "");
        try {
            uuid = UUID.fromString(nbt.getStringOr("UUID", ""));
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("username", username);
        if (uuid != null) {
            nbt.putString("UUID", uuid.toString());
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        boolean hasUsername = buf.readBoolean();
        if (hasUsername) {
            username = buf.readUtf(17);
        } else {
            username = "";
        }

        boolean hasUUID = buf.readBoolean();
        if (hasUUID) {
            uuid = UUIDHelper.fromBytes(buf);
        } else {
            uuid = null;
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        if (StringUtils.isBlank(username)) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeUtf(username, 17);
        }

        if (uuid == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            UUIDHelper.toBytes(buf, uuid);
        }
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof SettingsUser user) {
            if (uuid != null) {
                return uuid.equals(user.uuid);
            }

            return username.equals(user.username);
        }

        return false;
    }

    @Override
    public String toString() {
        return toString(I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC));
    }

    public String toString(String blank) {
        if (!StringUtils.isBlank(username)) {
            return username;
        } else if (uuid != null) {
            return uuid.toString();
        } else {
            return blank;
        }
    }

    @Override
    public int compareTo(SettingsUser other) {
        if (StringUtils.isBlank(username) && StringUtils.isBlank(other.username)) {
            if (uuid == null || other.uuid == null) {
                return 0;
            } else {
                return uuid.compareTo(other.uuid);
            }
        }

        if (username == null) {
            return other.username == null ? 0 : -1;
        }

        if (other.username == null) {
            return 1;
        }

        return username.compareTo(other.username);
    }
}
