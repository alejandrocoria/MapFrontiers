package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class SettingsUser {
    public String username;
    public UUID uuid;

    public SettingsUser() {
        username = "";
    }

    public SettingsUser(EntityPlayer player) {
        username = player.getName();
        uuid = player.getUniqueID();
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
            username = UUIDHelper.getNameFromUUID(uuid);
            if (username == null) {
                username = "";
            }
        }
    }

    public void readFromNBT(NBTTagCompound nbt) {
        username = nbt.getString("username");
        try {
            uuid = UUID.fromString(nbt.getString("UUID"));
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }

        fillMissingInfo(true);
    }

    public void writeToNBT(NBTTagCompound nbt) {
        fillMissingInfo(true);

        nbt.setString("username", username);
        nbt.setString("UUID", uuid.toString());
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

        if (other == null || !(other instanceof SettingsUser)) {
            return false;
        }

        SettingsUser user = (SettingsUser) other;

        fillMissingInfo(false);
        user.fillMissingInfo(false);

        return uuid.equals(user.uuid);
    }
}