package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class ServerTerritoryOperationResult {
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

    private ServerTerritoryOperationResult(Status status, Reason reason, @Nullable FrontierData frontier,
                                           @Nullable CollectionData collection) {
        this.status = status;
        this.reason = reason;
        this.frontier = frontier;
        this.collection = collection;
    }

    public static ServerTerritoryOperationResult success(@Nullable FrontierData frontier) {
        return new ServerTerritoryOperationResult(Status.Success, Reason.None, frontier, null);
    }

    public static ServerTerritoryOperationResult successCollection(@Nullable CollectionData collection) {
        return new ServerTerritoryOperationResult(Status.Success, Reason.None, null, collection);
    }

    public static ServerTerritoryOperationResult rejected(@Nullable FrontierData frontier) {
        return new ServerTerritoryOperationResult(Status.Rejected, Reason.None, frontier, null);
    }

    public static ServerTerritoryOperationResult rejected(Reason reason, @Nullable FrontierData frontier) {
        return new ServerTerritoryOperationResult(Status.Rejected, reason, frontier, null);
    }

    public static ServerTerritoryOperationResult notFound() {
        return new ServerTerritoryOperationResult(Status.NotFound, Reason.None, null, null);
    }

    public static ServerTerritoryOperationResult notFound(Reason reason) {
        return new ServerTerritoryOperationResult(Status.NotFound, reason, null, null);
    }

    public static ServerTerritoryOperationResult ignored(@Nullable FrontierData frontier) {
        return new ServerTerritoryOperationResult(Status.Ignored, Reason.None, frontier, null);
    }

    public static ServerTerritoryOperationResult ignored(Reason reason, @Nullable FrontierData frontier) {
        return new ServerTerritoryOperationResult(Status.Ignored, reason, frontier, null);
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
