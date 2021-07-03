package games.alejandrocoria.mapfrontiers.common.settings;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;

@ParametersAreNonnullByDefault
public class SettingsUserShared {
    public enum Action {
        UpdateFrontier, UpdateSettings;

        public final static Action[] valuesArray = values();
    }

    private final SettingsUser user;
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

    public void setActions(Set<SettingsUserShared.Action> actions) {
        this.actions = actions;
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

    public void readFromNBT(CompoundNBT nbt) {
        user.readFromNBT(nbt);
        pending = nbt.getBoolean("pending");

        actions.clear();
        ListNBT actionsTagList = nbt.getList("actions", Constants.NBT.TAG_STRING);
        for (int i = 0; i < actionsTagList.size(); ++i) {
            String actionTag = actionsTagList.getString(i);

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

    public void writeToNBT(CompoundNBT nbt) {
        user.writeToNBT(nbt);

        if (pending) {
            nbt.putBoolean("pending", pending);
        }

        ListNBT actionsTagList = new ListNBT();
        for (SettingsUserShared.Action action : actions) {
            StringNBT actionTag = StringNBT.valueOf(action.name());
            actionsTagList.add(actionTag);
        }

        nbt.put("actions", actionsTagList);
    }

    public void fromBytes(PacketBuffer buf) {
        user.fromBytes(buf);

        pending = buf.readBoolean();

        actions.clear();
        for (Action action : Action.valuesArray) {
            if (buf.readBoolean()) {
                actions.add(action);
            }
        }
    }

    public void toBytes(PacketBuffer buf) {
        user.toBytes(buf);

        buf.writeBoolean(pending);

        for (Action action : Action.valuesArray) {
            buf.writeBoolean(actions.contains(action));
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

        if (!(other instanceof SettingsUserShared)) {
            return false;
        }

        SettingsUserShared otherUser = (SettingsUserShared) other;

        return user.equals(otherUser.user) && pending == otherUser.pending;
    }
}
