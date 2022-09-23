package games.alejandrocoria.mapfrontiers.common.settings;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nullable;

public class SettingsProfile {
    public enum State {
        Enabled, Owner, Disabled
    }

    public State createFrontier = State.Disabled;
    public State deleteFrontier = State.Disabled;
    public State updateFrontier = State.Disabled;
    public State updateSettings = State.Disabled;
    public State personalFrontier = State.Disabled;

    public void setAction(FrontierSettings.Action action, State state) {
        switch (action) {
            case CreateGlobalFrontier:
                createFrontier = state;
                break;
            case DeleteGlobalFrontier:
                deleteFrontier = state;
                break;
            case UpdateGlobalFrontier:
                updateFrontier = state;
                break;
            case UpdateSettings:
                updateSettings = state;
                break;
            case SharePersonalFrontier:
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof SettingsProfile)) {
            return false;
        }

        SettingsProfile profile = (SettingsProfile) other;

        return createFrontier == profile.createFrontier && deleteFrontier == profile.deleteFrontier
                && updateFrontier == profile.updateFrontier && updateSettings == profile.updateSettings
                && personalFrontier == profile.personalFrontier;
    }

    public static AvailableActions getAvailableActions(@Nullable SettingsProfile profile, @Nullable FrontierData frontier, SettingsUser playerUser) {
        AvailableActions actions = new AvailableActions();

        if (profile == null) {
            if (frontier != null) {
                actions.canDelete = true;
                actions.canUpdate = true;
            }

            return actions;
        }

        if (frontier != null) {
            if (frontier.getPersonal()) {
                actions.canDelete = true;
                actions.canUpdate = frontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateFrontier);
                actions.canShare = ClientProxy.isModOnServer() && profile.personalFrontier == SettingsProfile.State.Enabled;
            } else {
                boolean isOwner = frontier.getOwner().equals(playerUser);
                actions.canDelete = profile.deleteFrontier == SettingsProfile.State.Enabled
                        || (isOwner && profile.deleteFrontier == SettingsProfile.State.Owner);
                actions.canUpdate = profile.updateFrontier == SettingsProfile.State.Enabled
                        || (isOwner && profile.updateFrontier == SettingsProfile.State.Owner);
            }
        }

        return actions;
    }

    public static class AvailableActions {
        public boolean canDelete = false;
        public boolean canUpdate = false;
        public boolean canShare = false;
    }
}
