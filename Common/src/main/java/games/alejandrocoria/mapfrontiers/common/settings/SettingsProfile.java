package games.alejandrocoria.mapfrontiers.common.settings;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SettingsProfile {
    public enum State {
        Enabled, Owner, Disabled;

        public static final State[] VALUES = values();
    }

    public State createFrontier = State.Disabled;
    public State deleteFrontier = State.Disabled;
    public State updateFrontier = State.Disabled;
    public State updateSettings = State.Disabled;
    public State personalFrontier = State.Disabled;

    public void setAction(FrontierSettings.Action action, State state) {
        switch (action) {
            case CreateGlobalFrontier -> createFrontier = state;
            case DeleteGlobalFrontier -> deleteFrontier = state;
            case UpdateGlobalFrontier -> updateFrontier = state;
            case UpdateSettings -> updateSettings = state;
            case SharePersonalFrontier -> personalFrontier = state;
        }
    }

    public boolean isAllEnabled() {
        return createFrontier == State.Enabled && deleteFrontier == State.Enabled && updateFrontier == State.Enabled
                && updateSettings == State.Enabled && personalFrontier == State.Enabled;
    }

    public void fromBytes(ByteBuf buf) {
        createFrontier = State.VALUES[buf.readInt()];
        deleteFrontier = State.VALUES[buf.readInt()];
        updateFrontier = State.VALUES[buf.readInt()];
        updateSettings = State.VALUES[buf.readInt()];
        personalFrontier = State.VALUES[buf.readInt()];
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

        if (other instanceof SettingsProfile profile) {
            return createFrontier == profile.createFrontier && deleteFrontier == profile.deleteFrontier
                    && updateFrontier == profile.updateFrontier && updateSettings == profile.updateSettings
                    && personalFrontier == profile.personalFrontier;
        }

        return false;
    }

    public static AvailableActions getAvailableActions(@Nullable SettingsProfile profile, @Nullable FrontierData frontier, SettingsUser playerUser) {
        AvailableActions actions = new AvailableActions();

        if (profile == null) {
            // While connected to a server with MapFrontiers but profile not received yet,
            // keep actions disabled until permissions are known.
            if (MapFrontiersClient.isModOnServer()) {
                return actions;
            }

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
                actions.canShare = MapFrontiersClient.isModOnServer() && profile.personalFrontier == State.Enabled;
            } else {
                boolean isOwner = frontier.getOwner().equals(playerUser);
                actions.canDelete = profile.deleteFrontier == State.Enabled || (isOwner && profile.deleteFrontier == State.Owner);
                actions.canUpdate = profile.updateFrontier == State.Enabled || (isOwner && profile.updateFrontier == State.Owner);
            }
        }

        return actions;
    }

    public static boolean canUpdateCollection(@Nullable SettingsProfile profile, CollectionData collection, SettingsUser playerUser) {
        if (collection.getPersonal()) {
            return collection.getOwner().equals(playerUser);
        }

        return profile != null && (profile.updateFrontier == State.Enabled
                || (profile.updateFrontier == State.Owner && collection.getOwner().equals(playerUser)));
    }

    public static class AvailableActions {
        public boolean canDelete = false;
        public boolean canUpdate = false;
        public boolean canShare = false;
    }
}
