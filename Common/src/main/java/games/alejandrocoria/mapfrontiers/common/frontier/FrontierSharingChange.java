package games.alejandrocoria.mapfrontiers.common.frontier;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class FrontierSharingChange {
    private @Nullable List<SettingsUserShared> usersShared;

    public FrontierSharingChange() {
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
    }

    public static FrontierSharingChange fromFrontierData(FrontierData frontier) {
        FrontierSharingChange change = new FrontierSharingChange();
        change.setUsersShared(frontier.getUsersShared());
        return change;
    }

    public void toBytes(FriendlyByteBuf buf) {
        if (usersShared == null) {
            buf.writeBoolean(false);
            return;
        }

        buf.writeBoolean(true);
        buf.writeInt(usersShared.size());
        for (SettingsUserShared userShared : usersShared) {
            userShared.toBytes(buf);
        }
    }

    public @Nullable List<SettingsUserShared> getUsersShared() {
        return copyUsersShared(usersShared);
    }

    public void setUsersShared(@Nullable List<SettingsUserShared> usersShared) {
        this.usersShared = copyUsersShared(usersShared);
    }

    private static @Nullable List<SettingsUserShared> copyUsersShared(@Nullable List<SettingsUserShared> usersShared) {
        if (usersShared == null || usersShared.isEmpty()) {
            return null;
        }

        List<SettingsUserShared> copiedUsersShared = new ArrayList<>(usersShared.size());
        for (SettingsUserShared userShared : usersShared) {
            SettingsUser user = new SettingsUser();
            user.username = userShared.getUser().username;
            user.uuid = userShared.getUser().uuid;

            SettingsUserShared copiedUserShared = new SettingsUserShared(user, userShared.isPending());
            copiedUserShared.setActions(userShared.getActions().isEmpty()
                    ? java.util.EnumSet.noneOf(SettingsUserShared.Action.class)
                    : java.util.EnumSet.copyOf(userShared.getActions()));
            copiedUsersShared.add(copiedUserShared);
        }

        return copiedUsersShared;
    }
}
