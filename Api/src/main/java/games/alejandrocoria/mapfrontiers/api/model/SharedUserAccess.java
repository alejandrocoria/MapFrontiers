package games.alejandrocoria.mapfrontiers.api.model;

import java.util.EnumSet;
import java.util.Set;

public record SharedUserAccess(UserRef user, Set<FrontierSharePermission> permissions, boolean pending) {
    public SharedUserAccess {
        if (user == null) {
            throw new IllegalArgumentException("Shared user cannot be null");
        }

        EnumSet<FrontierSharePermission> normalized = EnumSet.noneOf(FrontierSharePermission.class);
        if (permissions != null) {
            normalized.addAll(permissions);
        }
        permissions = Set.copyOf(normalized);
    }
}
