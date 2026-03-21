package games.alejandrocoria.mapfrontiers.common.frontier.server;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class ServerSettingsCommandResult {
    public enum Status {
        Success,
        Rejected,
        Ignored
    }

    private final Status status;
    private final List<Runnable> networkActions = new ArrayList<>();

    private ServerSettingsCommandResult(Status status) {
        this.status = status;
    }

    public static ServerSettingsCommandResult success() {
        return new ServerSettingsCommandResult(Status.Success);
    }

    public static ServerSettingsCommandResult rejected() {
        return new ServerSettingsCommandResult(Status.Rejected);
    }

    public static ServerSettingsCommandResult ignored() {
        return new ServerSettingsCommandResult(Status.Ignored);
    }

    public Status getStatus() {
        return status;
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
