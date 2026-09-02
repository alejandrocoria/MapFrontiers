package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
public class FrontierSharingChange {
    private @Nullable List<SettingsUserShared> usersShared;
    private long sharingRevision;

    public FrontierSharingChange() {
    }

    public FrontierSharingChange(FrontierSharingChange other) {
        usersShared = copyUsersShared(other.usersShared);
        sharingRevision = other.sharingRevision;
    }

    public FrontierSharingChange(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            int size = buf.readInt();
            if (size > 0) {
                usersShared = new ArrayList<>(size);
                for (int i = 0; i < size; ++i) {
                    SettingsUserShared userShared = new SettingsUserShared();
                    userShared.fromBytes(buf);
                    usersShared.add(userShared);
                }
            }
        }
        sharingRevision = buf.readLong();
    }

    public static FrontierSharingChange fromFrontierData(FrontierData frontier) {
        FrontierSharingChange change = new FrontierSharingChange();
        change.setUsersShared(frontier.getUsersShared());
        change.setSharingRevision(frontier.getSharingRevision());
        return change;
    }

    public void toBytes(FriendlyByteBuf buf) {
        if (usersShared == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeInt(usersShared.size());
            for (SettingsUserShared userShared : usersShared) {
                userShared.toBytes(buf);
            }
        }
        buf.writeLong(sharingRevision);
    }

    public @Nullable List<SettingsUserShared> getUsersShared() {
        return copyUsersShared(usersShared);
    }

    public void setUsersShared(@Nullable List<SettingsUserShared> usersShared) {
        this.usersShared = copyUsersShared(usersShared);
    }

    public long getSharingRevision() {
        return sharingRevision;
    }

    public void setSharingRevision(long sharingRevision) {
        this.sharingRevision = sharingRevision;
    }

    public boolean hasSameFunctionalState(FrontierSharingChange other) {
        if (usersShared == null || usersShared.isEmpty()) {
            return other.usersShared == null || other.usersShared.isEmpty();
        }
        if (other.usersShared == null || usersShared.size() != other.usersShared.size()) {
            return false;
        }

        for (int i = 0; i < usersShared.size(); ++i) {
            SettingsUserShared user = usersShared.get(i);
            SettingsUserShared otherUser = other.usersShared.get(i);
            if (!Objects.equals(user.getUser().username, otherUser.getUser().username)
                    || !Objects.equals(user.getUser().uuid, otherUser.getUser().uuid)
                    || user.isPending() != otherUser.isPending()
                    || !user.getActions().equals(otherUser.getActions())) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable List<SettingsUserShared> copyUsersShared(@Nullable List<SettingsUserShared> usersShared) {
        if (usersShared == null || usersShared.isEmpty()) {
            return null;
        }

        List<SettingsUserShared> copiedUsersShared = new ArrayList<>(usersShared.size());
        for (SettingsUserShared userShared : usersShared) {
            copiedUsersShared.add(new SettingsUserShared(userShared));
        }

        return copiedUsersShared;
    }
}
