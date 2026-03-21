package games.alejandrocoria.mapfrontiers.common.frontier.server;

import games.alejandrocoria.mapfrontiers.common.FrontierData;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class ServerFrontierCommandResult {
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

    private ServerFrontierCommandResult(Status status, Reason reason, @Nullable FrontierData frontier) {
        this.status = status;
        this.reason = reason;
        this.frontier = frontier;
    }

    public static ServerFrontierCommandResult success(@Nullable FrontierData frontier) {
        return new ServerFrontierCommandResult(Status.Success, Reason.None, frontier);
    }

    public static ServerFrontierCommandResult rejected(@Nullable FrontierData frontier) {
        return new ServerFrontierCommandResult(Status.Rejected, Reason.None, frontier);
    }

    public static ServerFrontierCommandResult rejected(Reason reason, @Nullable FrontierData frontier) {
        return new ServerFrontierCommandResult(Status.Rejected, reason, frontier);
    }

    public static ServerFrontierCommandResult notFound() {
        return new ServerFrontierCommandResult(Status.NotFound, Reason.None, null);
    }

    public static ServerFrontierCommandResult notFound(Reason reason) {
        return new ServerFrontierCommandResult(Status.NotFound, reason, null);
    }

    public static ServerFrontierCommandResult ignored(@Nullable FrontierData frontier) {
        return new ServerFrontierCommandResult(Status.Ignored, Reason.None, frontier);
    }

    public static ServerFrontierCommandResult ignored(Reason reason, @Nullable FrontierData frontier) {
        return new ServerFrontierCommandResult(Status.Ignored, reason, frontier);
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
