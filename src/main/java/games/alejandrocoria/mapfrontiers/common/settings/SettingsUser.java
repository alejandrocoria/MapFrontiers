package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;

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
            String newUsername = UUIDHelper.getNameFromUUID(uuid);
            if (newUsername != null) {
                username = newUsername;
            } else if (username == null) {
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

    public void fromBytes(ByteBuf buf) {
        boolean isUsername = buf.readBoolean();
        if (isUsername) {
            username = ByteBufUtils.readUTF8String(buf);
            uuid = null;
        } else {
            username = "";
            uuid = UUIDHelper.fromBytes(buf);
        }

        fillMissingInfo(false);
    }

    public void toBytes(ByteBuf buf) {
        if (uuid == null) {
            buf.writeBoolean(true);
            ByteBufUtils.writeUTF8String(buf, username);
        } else {
            buf.writeBoolean(false);
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

        if (other == null || !(other instanceof SettingsUser)) {
            return false;
        }

        SettingsUser user = (SettingsUser) other;

        fillMissingInfo(false);
        user.fillMissingInfo(false);

        if (uuid != null && user != null) {
            return uuid.equals(user.uuid);
        }

        return username.equals(user.username);
    }
}