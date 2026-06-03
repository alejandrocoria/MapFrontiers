package games.alejandrocoria.mapfrontiers.common.territory;

import java.util.EnumSet;

public class FrontierVisibilityMask {
    private final EnumSet<FrontierVisibility> values;

    public FrontierVisibilityMask() {
        values = EnumSet.noneOf(FrontierVisibility.class);
    }

    public FrontierVisibilityMask(boolean setAll) {
        if (setAll) {
            values = EnumSet.allOf(FrontierVisibility.class);
        } else {
            values = EnumSet.noneOf(FrontierVisibility.class);
        }
    }

    public FrontierVisibilityMask(FrontierVisibilityMask other) {
        values = other.values.clone();
    }

    public FrontierVisibilityMask(VisibilityData other) {
        values = EnumSet.noneOf(FrontierVisibility.class);
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (other.get(visibility)) {
                values.add(visibility);
            }
        }
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof FrontierVisibilityMask otherMask) {
            return values.equals(otherMask.values);
        }

        return false;
    }

    public boolean has(FrontierVisibility visibility) {
        return values.contains(visibility);
    }

    public void set(FrontierVisibility visibility, boolean enabled) {
        if (enabled) {
            values.add(visibility);
        } else {
            values.remove(visibility);
        }
    }

    public void clear() {
        values.clear();
    }

    public boolean hasAny() {
        return !values.isEmpty();
    }
}
