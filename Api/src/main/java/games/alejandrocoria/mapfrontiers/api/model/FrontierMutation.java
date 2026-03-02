package games.alejandrocoria.mapfrontiers.api.model;

import java.util.Optional;
import java.util.Objects;
import java.util.Set;

public final class FrontierMutation {
    private final Optional<String> name1;
    private final Optional<String> name2;
    private final Optional<Integer> color;
    private final Optional<FrontierShape> shape;
    private final Optional<Set<FrontierVisibilityFlag>> visibility;
    private final Optional<FrontierBanner> banner;
    private final boolean clearBanner;

    private FrontierMutation(Optional<String> name1,
                             Optional<String> name2,
                             Optional<Integer> color,
                             Optional<FrontierShape> shape,
                             Optional<Set<FrontierVisibilityFlag>> visibility,
                             Optional<FrontierBanner> banner,
                             boolean clearBanner) {
        this.name1 = name1;
        this.name2 = name2;
        this.color = color;
        this.shape = shape;
        this.visibility = visibility.map(Set::copyOf);
        this.banner = banner;
        this.clearBanner = clearBanner;

        if (clearBanner && this.banner.isPresent()) {
            throw new IllegalArgumentException("Mutation cannot set and clear banner at the same time");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FrontierMutation empty() {
        return builder().build();
    }

    public static FrontierMutation names(String name1, String name2) {
        return builder().names(name1, name2).build();
    }

    public static FrontierMutation name1(String name1) {
        return builder().name1(name1).build();
    }

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
                && banner.equals(that.banner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name1, name2, color, shape, visibility, banner, clearBanner);
    }

    @Override
    public String toString() {
        return "FrontierMutation[name1=" + name1
                + ", name2=" + name2
                + ", color=" + color
                + ", shape=" + shape
                + ", visibility=" + visibility
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
        private Optional<FrontierBanner> banner = Optional.empty();
        private boolean clearBanner = false;

        private Builder() {
        }

        public Builder name1(String value) {
            name1 = Optional.ofNullable(value);
            return this;
        }

        public Builder name2(String value) {
            name2 = Optional.ofNullable(value);
            return this;
        }

        public Builder names(String value1, String value2) {
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

        public FrontierMutation build() {
            return new FrontierMutation(name1, name2, color, shape, visibility, banner, clearBanner);
        }
    }
}
