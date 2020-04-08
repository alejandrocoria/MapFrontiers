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
        case DeleteFrontier:
            deleteFrontier = state;
        case UpdateFrontier:
            updateFrontier = state;
        case UpdateSettings:
            updateSettings = state;
        }
    }

    public boolean isAllEnabled() {
        return createFrontier == State.Enabled && deleteFrontier == State.Enabled && updateFrontier == State.Enabled
                && updateSettings == State.Enabled;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        createFrontier = stringToState(nbt.getString("createFrontier"));
        deleteFrontier = stringToState(nbt.getString("deleteFrontier"));
        updateFrontier = stringToState(nbt.getString("createFrontier"));
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
}
