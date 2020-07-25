package games.alejandrocoria.mapfrontiers.common.settings;

import io.netty.buffer.ByteBuf;

public class SettingsProfile {
    public enum State {
        Enabled, Owner, Disabled;

        public final static State[] valuesArray = values();
    }

    public State createFrontier = State.Disabled;
    public State deleteFrontier = State.Disabled;
    public State updateFrontier = State.Disabled;
    public State updateSettings = State.Disabled;
    public State personalFrontier = State.Disabled;

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
        case PersonalFrontier:
            personalFrontier = state;
            break;
        }
    }

    public boolean isAllEnabled() {
        return createFrontier == State.Enabled && deleteFrontier == State.Enabled && updateFrontier == State.Enabled
                && updateSettings == State.Enabled && personalFrontier == State.Enabled;
    }

    public void fromBytes(ByteBuf buf) {
        createFrontier = State.values()[buf.readInt()];
        deleteFrontier = State.values()[buf.readInt()];
        updateFrontier = State.values()[buf.readInt()];
        updateSettings = State.values()[buf.readInt()];
        personalFrontier = State.values()[buf.readInt()];
    }

    public void toBytes(ByteBuf buf) {
        buf.writeInt(createFrontier.ordinal());
        buf.writeInt(deleteFrontier.ordinal());
        buf.writeInt(updateFrontier.ordinal());
        buf.writeInt(updateSettings.ordinal());
        buf.writeInt(personalFrontier.ordinal());
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
                && updateFrontier == profile.updateFrontier && updateSettings == profile.updateSettings
                && personalFrontier == profile.personalFrontier;
    }
}
