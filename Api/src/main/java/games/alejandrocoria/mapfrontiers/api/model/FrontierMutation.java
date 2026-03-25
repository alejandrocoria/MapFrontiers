package games.alejandrocoria.mapfrontiers.api.model;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Partial frontier update payload.
 * Only present fields are applied when this mutation is used.
 * <p>
 * For now, MapFrontiers limits each frontier name field to 17 characters.
 */
@SuppressWarnings("unused")
public final class FrontierMutation {
    private static final int MAX_NAME_LENGTH = 17;

    private final Optional<String> name1;
    private final Optional<String> name2;
    private final Optional<Integer> color;
    private final Optional<FrontierShape> shape;
    private final Optional<Set<FrontierVisibilityFlag>> visibility;
    private final Set<FrontierVisibilityFlag> visibilityToAdd;
    private final Set<FrontierVisibilityFlag> visibilityToRemove;
    private final Optional<FrontierBanner> banner;
    private final boolean clearBanner;

    private FrontierMutation(Optional<String> name1,
                             Optional<String> name2,
                             Optional<Integer> color,
                             Optional<FrontierShape> shape,
                             Optional<Set<FrontierVisibilityFlag>> visibility,
                             Set<FrontierVisibilityFlag> visibilityToAdd,
                             Set<FrontierVisibilityFlag> visibilityToRemove,
                             Optional<FrontierBanner> banner,
                             boolean clearBanner) {
        this.name1 = name1;
        this.name2 = name2;
        this.color = color;
        this.shape = shape;
        this.visibility = visibility.map(Set::copyOf);
        this.visibilityToAdd = Set.copyOf(visibilityToAdd);
        this.visibilityToRemove = Set.copyOf(visibilityToRemove);
        this.banner = banner;
        this.clearBanner = clearBanner;

        if (clearBanner && this.banner.isPresent()) {
            throw new IllegalArgumentException("Mutation cannot set and clear banner at the same time");
        }
    }

    /**
     * Creates a builder for combining multiple changes in one mutation.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a mutation with no changes.
     */
    public static FrontierMutation empty() {
        return builder().build();
    }

    /**
     * Returns a mutation that updates both name fields.
     * For now, each name field is limited to 17 characters.
     *
     * @throws IllegalArgumentException when either name exceeds 17 characters
     */
    public static FrontierMutation names(String name1, String name2) {
        return builder().names(name1, name2).build();
    }

    /**
     * Returns a mutation that updates the first name field.
     * For now, the name field is limited to 17 characters.
     *
     * @throws IllegalArgumentException when the name exceeds 17 characters
     */
    public static FrontierMutation name1(String name1) {
        return builder().name1(name1).build();
    }

    /**
     * Returns a mutation that updates the second name field.
     * For now, the name field is limited to 17 characters.
     *
     * @throws IllegalArgumentException when the name exceeds 17 characters
     */
    public static FrontierMutation name2(String name2) {
        return builder().name2(name2).build();
    }

    public static FrontierMutation color(int color) {
        return builder().color(color).build();
    }

    public static FrontierMutation shape(FrontierShape shape) {
        return builder().shape(shape).build();
    }

    public static FrontierMutation visibility(Set<FrontierVisibilityFlag> visibility) {
        return builder().visibility(visibility).build();
    }

    /**
     * Returns a mutation that adds visibility flags to the current set.
     */
    public static FrontierMutation addVisibility(Set<FrontierVisibilityFlag> visibility) {
        return builder().addVisibility(visibility).build();
    }

    /**
     * Returns a mutation that removes visibility flags from the current set.
     */
    public static FrontierMutation removeVisibility(Set<FrontierVisibilityFlag> visibility) {
        return builder().removeVisibility(visibility).build();
    }

    public static FrontierMutation banner(FrontierBanner banner) {
        return builder().banner(banner).build();
    }

    public static FrontierMutation withClearedBanner() {
        return builder().clearBanner().build();
    }

    public Optional<String> name1() {
        return name1;
    }

    public Optional<String> name2() {
        return name2;
    }

    public Optional<Integer> color() {
        return color;
    }

    public Optional<FrontierShape> shape() {
        return shape;
    }

    public Optional<Set<FrontierVisibilityFlag>> visibility() {
        return visibility;
    }

    public Set<FrontierVisibilityFlag> visibilityToAdd() {
        return visibilityToAdd;
    }

    public Set<FrontierVisibilityFlag> visibilityToRemove() {
        return visibilityToRemove;
    }

    public Optional<FrontierBanner> banner() {
        return banner;
    }

    public boolean clearBanner() {
        return clearBanner;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FrontierMutation that)) {
            return false;
        }
        return clearBanner == that.clearBanner
                && name1.equals(that.name1)
                && name2.equals(that.name2)
                && color.equals(that.color)
                && shape.equals(that.shape)
                && visibility.equals(that.visibility)
                && visibilityToAdd.equals(that.visibilityToAdd)
                && visibilityToRemove.equals(that.visibilityToRemove)
                && banner.equals(that.banner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name1, name2, color, shape, visibility, visibilityToAdd, visibilityToRemove, banner, clearBanner);
    }

    @Override
    public String toString() {
        return "FrontierMutation[name1=" + name1
                + ", name2=" + name2
                + ", color=" + color
                + ", shape=" + shape
                + ", visibility=" + visibility
                + ", visibilityToAdd=" + visibilityToAdd
                + ", visibilityToRemove=" + visibilityToRemove
                + ", banner=" + banner
                + ", clearBanner=" + clearBanner
                + "]";
    }

    public static final class Builder {
        private Optional<String> name1 = Optional.empty();
        private Optional<String> name2 = Optional.empty();
        private Optional<Integer> color = Optional.empty();
        private Optional<FrontierShape> shape = Optional.empty();
        private Optional<Set<FrontierVisibilityFlag>> visibility = Optional.empty();
        private final EnumSet<FrontierVisibilityFlag> visibilityToAdd = EnumSet.noneOf(FrontierVisibilityFlag.class);
        private final EnumSet<FrontierVisibilityFlag> visibilityToRemove = EnumSet.noneOf(FrontierVisibilityFlag.class);
        private Optional<FrontierBanner> banner = Optional.empty();
        private boolean clearBanner = false;

        private Builder() {
        }

        /**
         * Sets the first name field.
         * For now, the name field is limited to 17 characters.
         *
         * @throws IllegalArgumentException when the name exceeds 17 characters
         */
        public Builder name1(String value) {
            validateNameLength("name1", value);
            name1 = Optional.ofNullable(value);
            return this;
        }

        /**
         * Sets the second name field.
         * For now, the name field is limited to 17 characters.
         *
         * @throws IllegalArgumentException when the name exceeds 17 characters
         */
        public Builder name2(String value) {
            validateNameLength("name2", value);
            name2 = Optional.ofNullable(value);
            return this;
        }

        /**
         * Sets both name fields.
         * For now, each name field is limited to 17 characters.
         *
         * @throws IllegalArgumentException when either name exceeds 17 characters
         */
        public Builder names(String value1, String value2) {
            validateNameLength("name1", value1);
            validateNameLength("name2", value2);
            name1 = Optional.ofNullable(value1);
            name2 = Optional.ofNullable(value2);
            return this;
        }

        public Builder color(Integer value) {
            color = Optional.ofNullable(value);
            return this;
        }

        public Builder shape(FrontierShape value) {
            shape = Optional.ofNullable(value);
            return this;
        }

        public Builder visibility(Set<FrontierVisibilityFlag> value) {
            visibility = Optional.ofNullable(value);
            return this;
        }

        /**
         * Adds visibility flags to the current frontier visibility set.
         */
        public Builder addVisibility(Set<FrontierVisibilityFlag> value) {
            if (value != null) {
                visibilityToAdd.addAll(value);
                visibilityToRemove.removeAll(value);
            }
            return this;
        }

        /**
         * Removes visibility flags from the current frontier visibility set.
         */
        public Builder removeVisibility(Set<FrontierVisibilityFlag> value) {
            if (value != null) {
                visibilityToRemove.addAll(value);
                visibilityToAdd.removeAll(value);
            }
            return this;
        }

        public Builder banner(FrontierBanner value) {
            banner = Optional.ofNullable(value);
            clearBanner = false;
            return this;
        }

        public Builder clearBanner() {
            clearBanner = true;
            banner = Optional.empty();
            return this;
        }

        /**
         * Builds an immutable mutation from the current builder state.
         */
        public FrontierMutation build() {
            return new FrontierMutation(name1, name2, color, shape, visibility, visibilityToAdd, visibilityToRemove, banner, clearBanner);
        }
    }

    private static void validateNameLength(String fieldName, String value) {
        if (value != null && value.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(fieldName + " cannot be longer than " + MAX_NAME_LENGTH + " characters");
        }
    }
}
