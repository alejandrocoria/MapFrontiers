package games.alejandrocoria.mapfrontiers.server.settings;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class ServerSettingsOperationResult {
    public enum Status {
        Success,
        Rejected,
        Ignored
    }

    private final Status status;
    private final List<Runnable> networkActions = new ArrayList<>();

    private ServerSettingsOperationResult(Status status) {
        this.status = status;
    }

    public static ServerSettingsOperationResult success() {
        return new ServerSettingsOperationResult(Status.Success);
    }

    public static ServerSettingsOperationResult rejected() {
        return new ServerSettingsOperationResult(Status.Rejected);
    }

    public static ServerSettingsOperationResult ignored() {
        return new ServerSettingsOperationResult(Status.Ignored);
    }

    public Status getStatus() {
        return status;
    }

    public void addNetworkAction(Runnable action) {
        networkActions.add(action);
    }

    int getNetworkActionCount() {
        return networkActions.size();
    }

    public void dispatchNetworkActions() {
        for (Runnable action : networkActions) {
            action.run();
        }
    }
}
