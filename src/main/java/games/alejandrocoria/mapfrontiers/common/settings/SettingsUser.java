package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.UUID;

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

    public void fillMissingInfo(boolean forceNameUpdate) {
        if (username.isEmpty() && uuid == null) {
            return;
        }

        if (uuid == null) {
            uuid = UUIDHelper.getUUIDFromName(username);
        } else if (username.isEmpty() || forceNameUpdate) {
            username = UUIDHelper.getNameFromUUID(uuid);
            if (username == null) {
                username = "";
            }
        }
    }
}