package games.alejandrocoria.mapfrontiers.common.territory.collection;

import java.util.EnumSet;

public class CollectionVisibilityMask {
    private final EnumSet<CollectionVisibilityField> values;

    public CollectionVisibilityMask() {
        values = EnumSet.noneOf(CollectionVisibilityField.class);
    }

    public CollectionVisibilityMask(CollectionVisibilityMask other) {
        values = other.values.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CollectionVisibilityMask otherMask)) {
            return false;
        }

        return values.equals(otherMask.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    public boolean has(CollectionVisibilityField field) {
        return values.contains(field);
    }

    public void set(CollectionVisibilityField field, boolean enabled) {
        if (enabled) {
            values.add(field);
        } else {
            values.remove(field);
        }
    }

    public boolean hasAny() {
        return !values.isEmpty();
    }
}
