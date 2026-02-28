package games.alejandrocoria.mapfrontiers.api.model;

import java.util.Optional;
import java.util.Set;

public record FrontierMutation(Optional<String> name1,
                               Optional<String> name2,
                               Optional<Integer> color,
                               Optional<FrontierShape> shape,
                               Optional<Set<FrontierVisibilityFlag>> visibility,
                               Optional<FrontierBanner> banner) {
    public FrontierMutation {
        name1 = name1 == null ? Optional.empty() : name1;
        name2 = name2 == null ? Optional.empty() : name2;
        color = color == null ? Optional.empty() : color;
        shape = shape == null ? Optional.empty() : shape;
        visibility = visibility == null ? Optional.empty() : visibility;
        banner = banner == null ? Optional.empty() : banner;
    }

    public static FrontierMutation empty() {
        return new FrontierMutation(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
