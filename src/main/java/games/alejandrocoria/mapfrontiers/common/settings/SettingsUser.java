package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.UUID;

import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.resources.I18n;
import org.apache.commons.lang3.StringUtils;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class SettingsUser {
    public String username;
    public UUID uuid;

    public SettingsUser() {
        username = "";
    }

    public SettingsUser(PlayerEntity player) {
        username = player.getName().getString();
        uuid = player.getUUID();
    }

    public SettingsUser(String username, UUID uuid) {
        if (username == null) {
            this.username = "";
        } else {
            this.username = username;
        }

        this.uuid = uuid;

        fillMissingInfo(false);
    }

    public SettingsUser(String username) {
        if (username == null) {
            this.username = "";
        } else {
            this.username = username;
        }

        fillMissingInfo(false);
    }

    public SettingsUser(UUID uuid) {
        this.uuid = uuid;
        fillMissingInfo(true);
    }

    public boolean isEmpty() {
        return uuid == null && StringUtils.isBlank(username);
    }

    public void fillMissingInfo(boolean forceNameUpdate) {
        if (isEmpty()) {
            return;
        }

        if (uuid == null) {
            uuid = UUIDHelper.getUUIDFromName(username);
        } else if (StringUtils.isBlank(username) || forceNameUpdate) {
            String newUsername = UUIDHelper.getNameFromUUID(uuid);
            if (newUsername != null) {
                username = newUsername;
            } else if (username == null) {
                username = "";
            }
        }
    }

    public void readFromNBT(CompoundNBT nbt) {
        username = nbt.getString("username");
        try {
            uuid = UUID.fromString(nbt.getString("UUID"));
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }

        fillMissingInfo(true);
    }

    public void writeToNBT(CompoundNBT nbt) {
        fillMissingInfo(true);

        nbt.putString("username", username);
        nbt.putString("UUID", uuid.toString());
    }

    public void fromBytes(PacketBuffer buf) {
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

        fillMissingInfo(false);
    }

    public void toBytes(PacketBuffer buf) {
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
        fillMissingInfo(false);

        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof SettingsUser)) {
            return false;
        }

        SettingsUser user = (SettingsUser) other;

        fillMissingInfo(false);
        user.fillMissingInfo(false);

        if (uuid != null) {
            return uuid.equals(user.uuid);
        }

        return username.equals(user.username);
    }

    @Override
    public String toString() {
        if (StringUtils.isBlank(username)) {
            if (uuid == null) {
                return I18n.get("mapfrontiers.unnamed", TextFormatting.ITALIC);
            } else {
                return uuid.toString();
            }
        }

        return username;
    }
}
