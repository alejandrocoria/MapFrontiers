package games.alejandrocoria.mapfrontiers.server.frontier;

import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class ServerFrontierOperationResult {
    public enum Status {
        Success,
        Rejected,
        NotFound,
        Ignored
    }

    public enum Reason {
        None,
        InvitationExpired,
        FrontierMissing,
        SharedUserMissing,
        AlreadyAccepted,
        WrongTarget
    }

    private final Status status;
    private final Reason reason;
    private final @Nullable FrontierData frontier;
    private final @Nullable CollectionData collection;
    private final List<Runnable> networkActions = new ArrayList<>();

    private ServerFrontierOperationResult(Status status, Reason reason, @Nullable FrontierData frontier,
                                          @Nullable CollectionData collection) {
        this.status = status;
        this.reason = reason;
        this.frontier = frontier;
        this.collection = collection;
    }

    public static ServerFrontierOperationResult success(@Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Success, Reason.None, frontier, null);
    }

    public static ServerFrontierOperationResult successCollection(@Nullable CollectionData collection) {
        return new ServerFrontierOperationResult(Status.Success, Reason.None, null, collection);
    }

    public static ServerFrontierOperationResult rejected(@Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Rejected, Reason.None, frontier, null);
    }

    public static ServerFrontierOperationResult rejected(Reason reason, @Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Rejected, reason, frontier, null);
    }

    public static ServerFrontierOperationResult notFound() {
        return new ServerFrontierOperationResult(Status.NotFound, Reason.None, null, null);
    }

    public static ServerFrontierOperationResult notFound(Reason reason) {
        return new ServerFrontierOperationResult(Status.NotFound, reason, null, null);
    }

    public static ServerFrontierOperationResult ignored(@Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Ignored, Reason.None, frontier, null);
    }

    public static ServerFrontierOperationResult ignored(Reason reason, @Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Ignored, reason, frontier, null);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == Status.Success;
    }

    public Reason getReason() {
        return reason;
    }

    public @Nullable FrontierData getFrontier() {
        return frontier;
    }

    public @Nullable CollectionData getCollection() {
        return collection;
    }

    public void addNetworkAction(Runnable action) {
        networkActions.add(action);
    }

    public void dispatchNetworkActions() {
        for (Runnable action : networkActions) {
            action.run();
        }
    }
}
