package games.alejandrocoria.mapfrontiers.common.settings;

import net.minecraft.nbt.NBTTagCompound;

public class SettingsProfile {
    public enum State {
        Enabled, Owner, Disabled
    }

    public State createFrontier = State.Disabled;
    public State deleteFrontier = State.Disabled;
    public State updateFrontier = State.Disabled;
    public State updateSettings = State.Disabled;

    public void setAction(FrontierSettings.Action action, State state) {
        switch (action) {
        case CreateFrontier:
            createFrontier = state;
            break;
        case DeleteFrontier:
            deleteFrontier = state;
            break;
        case UpdateFrontier:
            updateFrontier = state;
            break;
        case UpdateSettings:
            updateSettings = state;
            break;
        }
    }

    public boolean isAllEnabled() {
        return createFrontier == State.Enabled && deleteFrontier == State.Enabled && updateFrontier == State.Enabled
                && updateSettings == State.Enabled;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        createFrontier = stringToState(nbt.getString("createFrontier"));
        deleteFrontier = stringToState(nbt.getString("deleteFrontier"));
        updateFrontier = stringToState(nbt.getString("updateFrontier"));
        updateSettings = stringToState(nbt.getString("updateSettings"));
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("createFrontier", createFrontier.name());
        nbt.setString("deleteFrontier", deleteFrontier.name());
        nbt.setString("updateFrontier", updateFrontier.name());
        nbt.setString("updateSettings", updateSettings.name());
    }

    private State stringToState(String string) {
        try {
            State state = State.valueOf(string);
            return state;
        } catch (IllegalArgumentException e) {
            return State.Disabled;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || !(other instanceof SettingsProfile)) {
            return false;
        }

        SettingsProfile profile = (SettingsProfile) other;

        return createFrontier == profile.createFrontier && deleteFrontier == profile.deleteFrontier
                && updateFrontier == profile.updateFrontier && updateSettings == profile.updateSettings;
    }
}
