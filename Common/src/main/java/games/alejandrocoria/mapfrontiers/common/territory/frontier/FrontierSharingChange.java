package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class FrontierSharingChange {
    private @Nullable List<FrontierUserAccess> userAccesses;
    private long sharingRevision;

    public FrontierSharingChange() {
    }

    public FrontierSharingChange(FrontierSharingChange other) {
        userAccesses = copyUserAccesses(other.userAccesses);
        sharingRevision = other.sharingRevision;
    }

    public FrontierSharingChange(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            int size = buf.readInt();
            if (size > 0) {
                userAccesses = new ArrayList<>(size);
                for (int i = 0; i < size; ++i) {
                    userAccesses.add(FrontierUserAccess.fromBytes(buf));
                }
            }
        }
        sharingRevision = buf.readLong();
    }

    public static FrontierSharingChange fromFrontierData(FrontierData frontier) {
        FrontierSharingChange change = new FrontierSharingChange();
        change.setUserAccesses(frontier.getUserAccesses());
        change.setSharingRevision(frontier.getSharingRevision());
        return change;
    }

    public void toBytes(FriendlyByteBuf buf) {
        if (userAccesses == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeInt(userAccesses.size());
            for (FrontierUserAccess userShared : userAccesses) {
                userShared.toBytes(buf);
            }
        }
        buf.writeLong(sharingRevision);
    }

    public @Nullable List<FrontierUserAccess> getUserAccesses() {
        return copyUserAccesses(userAccesses);
    }

    public void setUserAccesses(@Nullable List<FrontierUserAccess> userAccesses) {
        this.userAccesses = copyUserAccesses(userAccesses);
    }

    public long getSharingRevision() {
        return sharingRevision;
    }

    public void setSharingRevision(long sharingRevision) {
        this.sharingRevision = sharingRevision;
    }

    public boolean hasSameFunctionalState(FrontierSharingChange other) {
        if (userAccesses == null || userAccesses.isEmpty()) {
            return other.userAccesses == null || other.userAccesses.isEmpty();
        }
        if (other.userAccesses == null || userAccesses.size() != other.userAccesses.size()) {
            return false;
        }

        for (int i = 0; i < userAccesses.size(); ++i) {
            FrontierUserAccess user = userAccesses.get(i);
            FrontierUserAccess otherUser = other.userAccesses.get(i);
            if (!user.hasSameState(otherUser)) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable List<FrontierUserAccess> copyUserAccesses(@Nullable List<FrontierUserAccess> userAccesses) {
        if (userAccesses == null || userAccesses.isEmpty()) {
            return null;
        }

        List<FrontierUserAccess> copiedUserAccesses = new ArrayList<>(userAccesses.size());
        for (FrontierUserAccess userShared : userAccesses) {
            copiedUserAccesses.add(new FrontierUserAccess(userShared));
        }

        return copiedUserAccesses;
    }
}
