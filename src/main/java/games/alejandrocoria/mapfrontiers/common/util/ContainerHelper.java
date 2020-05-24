package games.alejandrocoria.mapfrontiers.common.util;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ContainerHelper {
    public static <T> int getIndexFromLambda(List<T> container, IntPredicate op) {
        return IntStream.range(0, container.size()).filter(op).findFirst().orElse(-1);
    }

    private ContainerHelper() {

    }
}
