package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

@ParametersAreNonnullByDefault
public class SettingsUserShared {
    public enum Action {
        UpdateFrontier, UpdateSettings
    }

    private SettingsUser user;
    private Set<SettingsUserShared.Action> actions;
    private boolean pending;

    public SettingsUserShared() {
        user = new SettingsUser();
        actions = EnumSet.noneOf(SettingsUserShared.Action.class);
        pending = false;
    }

    public SettingsUserShared(SettingsUser user, boolean pending) {
        this.user = user;
        actions = EnumSet.noneOf(SettingsUserShared.Action.class);
        this.pending = pending;
    }

    public SettingsUser getUser() {
        return user;
    }

    public void addAction(SettingsUserShared.Action action) {
        actions.add(action);
    }

    public void removeAction(SettingsUserShared.Action action) {
        actions.remove(action);
    }

    public boolean hasAction(SettingsUserShared.Action action) {
        return actions.contains(action);
    }

    public Set<SettingsUserShared.Action> getActions() {
        return actions;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public boolean isPending() {
        return pending;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        user.readFromNBT(nbt);
        pending = nbt.getBoolean("pending");

        actions.clear();
        NBTTagList actionsTagList = nbt.getTagList("actions", Constants.NBT.TAG_STRING);
        for (int i = 0; i < actionsTagList.tagCount(); ++i) {
            String actionTag = actionsTagList.getStringTagAt(i);

            try {
                SettingsUserShared.Action action = SettingsUserShared.Action.valueOf(actionTag);
                actions.add(action);
            } catch (IllegalArgumentException e) {
                SettingsUserShared.Action[] availableActions = SettingsUserShared.Action.values();

                String availableActionsString = availableActions[0].name();
                for (int i2 = 1; i2 < availableActions.length - 1; ++i2) {
                    availableActionsString += ", ";
                    availableActionsString += availableActions[i2].name();
                }

                availableActionsString += " or ";
                availableActionsString += availableActions[availableActions.length - 1].name();

                String userName = user.username;
                if (userName.isEmpty()) {
                    userName = user.uuid.toString();
                }

                MapFrontiers.LOGGER.warn(String.format("Unknown action in user shared %1$s. Found: \"%2$s\". Expected: %3$s",
                        userName, actionTag, availableActionsString));
            }
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        user.writeToNBT(nbt);

        if (pending) {
            nbt.setBoolean("pending", pending);
        }

        NBTTagList actionsTagList = new NBTTagList();
        for (SettingsUserShared.Action action : actions) {
            NBTTagString actionTag = new NBTTagString(action.name());
            actionsTagList.appendTag(actionTag);
        }

        nbt.setTag("actions", actionsTagList);
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
