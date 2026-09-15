package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.common.network.OperationResolution;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

@ParametersAreNonnullByDefault
final class PendingOptimisticSharingUpdates {
    enum Type {
        Add, Remove, Update
    }

    static final class Intent {
        private final Type type;
        private final SettingsUserShared userShared;

        private Intent(Type type, SettingsUserShared userShared) {
            this.type = type;
            this.userShared = new SettingsUserShared(userShared);
        }

        static Intent add(SettingsUserShared userShared) {
            return new Intent(Type.Add, userShared);
        }

        static Intent remove(SettingsUser user) {
            return new Intent(Type.Remove, new SettingsUserShared(new SettingsUser(user), false));
        }

        static Intent update(SettingsUserShared userShared) {
            return new Intent(Type.Update, userShared);
        }

        Type type() {
            return type;
        }

        SettingsUserShared userShared() {
            return new SettingsUserShared(userShared);
        }

        void replayOn(FrontierSharingChange change) {
            List<SettingsUserShared> users = change.getUsersShared();
            if (users == null) {
                users = new ArrayList<>();
            }

            SettingsUser target = userShared.getUser();
            SettingsUserShared existing = users.stream()
                    .filter(candidate -> candidate.getUser().equals(target))
                    .findFirst()
                    .orElse(null);
            switch (type) {
                case Add -> {
                    if (existing == null) {
                        users.add(new SettingsUserShared(userShared));
                    }
                }
                case Remove -> users.removeIf(candidate -> candidate.getUser().equals(target));
                case Update -> {
                    if (existing != null) {
                        existing.setActions(userShared.getActions());
                    }
                }
            }
            change.setUsersShared(users);
        }
    }

    record Outbound(UUID frontierId, Intent intent, long baseRevision, long requestId) {
    }

    record Reconciliation(boolean ignored, @Nullable FrontierSharingChange visibleChange,
                          @Nullable Outbound nextOutbound) {
        static Reconciliation ignoredResult() {
            return new Reconciliation(true, null, null);
        }

        static Reconciliation apply(FrontierSharingChange visibleChange, @Nullable Outbound nextOutbound) {
            return new Reconciliation(false, new FrontierSharingChange(visibleChange), nextOutbound);
        }
    }

    private static final class State {
        private final ArrayDeque<Intent> intents = new ArrayDeque<>();
        private long requestId;
    }

    private final Map<UUID, State> states = new HashMap<>();

    @Nullable Outbound submit(UUID frontierId, Intent intent, long baseRevision, LongSupplier requestIds) {
        State state = states.computeIfAbsent(frontierId, ignored -> new State());
        state.intents.addLast(intent);
        if (state.intents.size() > 1) {
            return null;
        }

        state.requestId = requestIds.getAsLong();
        return new Outbound(frontierId, intent, baseRevision, state.requestId);
    }

    Reconciliation reconcile(UUID frontierId, FrontierSharingChange authoritativeChange, long currentRevision,
                             int playerId, int currentPlayerId, long requestId, OperationResolution resolution,
                             LongSupplier requestIds) {
        long authoritativeRevision = authoritativeChange.getSharingRevision();
        if (authoritativeRevision < currentRevision) {
            return Reconciliation.ignoredResult();
        }

        State state = states.get(frontierId);
        if (state == null) {
            return authoritativeRevision == currentRevision
                    ? Reconciliation.ignoredResult()
                    : Reconciliation.apply(authoritativeChange, null);
        }

        boolean knownResponse = playerId == currentPlayerId && requestId == state.requestId;
        if (!knownResponse) {
            if (authoritativeRevision == currentRevision) {
                return Reconciliation.ignoredResult();
            }

            states.remove(frontierId);
            return Reconciliation.apply(authoritativeChange, null);
        }

        if (resolution == OperationResolution.Rejected) {
            states.remove(frontierId);
            return Reconciliation.apply(authoritativeChange, null);
        }

        state.intents.removeFirst();
        if (state.intents.isEmpty()) {
            states.remove(frontierId);
            return Reconciliation.apply(authoritativeChange, null);
        }

        FrontierSharingChange visibleChange = new FrontierSharingChange(authoritativeChange);
        for (Intent queuedIntent : state.intents) {
            queuedIntent.replayOn(visibleChange);
        }

        long nextRequestId = requestIds.getAsLong();
        state.requestId = nextRequestId;
        Outbound nextOutbound = new Outbound(frontierId, state.intents.getFirst(), authoritativeRevision,
                nextRequestId);
        return Reconciliation.apply(visibleChange, nextOutbound);
    }

    void clear(UUID frontierId) {
        states.remove(frontierId);
    }

    void clearAll() {
        states.clear();
    }

    boolean hasPending(UUID frontierId) {
        return states.containsKey(frontierId);
    }

}
