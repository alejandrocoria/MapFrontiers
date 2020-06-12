package games.alejandrocoria.mapfrontiers.common.settings;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.nbt.NBTTagCompound;

@ParametersAreNonnullByDefault
public class SettingsUserShared {
    private SettingsUser user;
    private boolean pending;

    public SettingsUserShared() {
        user = new SettingsUser();
        pending = false;
    }

    public SettingsUserShared(SettingsUser user, boolean pending) {
        this.user = user;
        this.pending = pending;
    }

    public SettingsUser getUser() {
        return user;
    }

    public boolean isPending() {
        return pending;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        user.readFromNBT(nbt);
        pending = nbt.getBoolean("pending");
    }

    public void writeToNBT(NBTTagCompound nbt) {
        user.writeToNBT(nbt);

        if (pending) {
            nbt.setBoolean("pending", pending);
        }
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int hash = 1;
        hash = prime * hash + user.hashCode();
        hash = prime * hash + (pending ? 1231 : 1237);

        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || !(other instanceof SettingsUserShared)) {
            return false;
        }

        SettingsUserShared otherUser = (SettingsUserShared) other;

        return user.equals(otherUser.user) && pending == otherUser.pending;
    }
}
