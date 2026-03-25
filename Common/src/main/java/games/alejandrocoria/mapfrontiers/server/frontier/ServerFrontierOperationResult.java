package games.alejandrocoria.mapfrontiers.server.frontier;

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
    private final List<Runnable> networkActions = new ArrayList<>();

    private ServerFrontierOperationResult(Status status, Reason reason, @Nullable FrontierData frontier) {
        this.status = status;
        this.reason = reason;
        this.frontier = frontier;
    }

    public static ServerFrontierOperationResult success(@Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Success, Reason.None, frontier);
    }

    public static ServerFrontierOperationResult rejected(@Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Rejected, Reason.None, frontier);
    }

    public static ServerFrontierOperationResult rejected(Reason reason, @Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Rejected, reason, frontier);
    }

    public static ServerFrontierOperationResult notFound() {
        return new ServerFrontierOperationResult(Status.NotFound, Reason.None, null);
    }

    public static ServerFrontierOperationResult notFound(Reason reason) {
        return new ServerFrontierOperationResult(Status.NotFound, reason, null);
    }

    public static ServerFrontierOperationResult ignored(@Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Ignored, Reason.None, frontier);
    }

    public static ServerFrontierOperationResult ignored(Reason reason, @Nullable FrontierData frontier) {
        return new ServerFrontierOperationResult(Status.Ignored, reason, frontier);
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

    public void addNetworkAction(Runnable action) {
        networkActions.add(action);
    }

    public void dispatchNetworkActions() {
        for (Runnable action : networkActions) {
            action.run();
        }
    }
}
