package games.alejandrocoria.mapfrontiers.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class ConfigEntry<T, SELF extends ConfigEntry<T, SELF>> {
    private final List<String> path;
    private final String pathString;
    private final T defaultValue;
    private T value;
    @Nullable
    private String translationKey;
    @Nullable
    private String comment;

    protected ConfigEntry(T defaultValue, String... path) {
        this.path = List.copyOf(Arrays.asList(path));
        this.pathString = String.join(".", this.path);
        this.defaultValue = copyValue(defaultValue);
        value = copyValue(defaultValue);
    }

    protected abstract SELF self();

    protected abstract ReadResult<T> readValue(Object rawValue);

    protected abstract boolean isValidValue(T newValue);

    protected abstract Object writeValue(T currentValue);

    protected abstract Component defaultValueComponent(T currentValue);

    protected T copyValue(T source) {
        return source;
    }

    @Nullable
    protected String describeConstraints() {
        return null;
    }

    public final SELF translation(String newTranslationKey) {
        translationKey = newTranslationKey;
        return self();
    }

    public final SELF comment(String newComment) {
        comment = newComment;
        return self();
    }

    public final List<String> path() {
        return path;
    }

    public final String pathString() {
        return pathString;
    }

    public final T get() {
        return copyValue(value);
    }

    public final T defaultValue() {
        return copyValue(defaultValue);
    }

    public final void set(T newValue) {
        if (!isValidValue(newValue)) {
            throw new IllegalArgumentException("Invalid value for config entry " + pathString + ": " + newValue);
        }

        value = copyValue(newValue);
    }

    public final void reset() {
        value = copyValue(defaultValue);
    }

    public final boolean isDefault() {
        return Objects.equals(value, defaultValue);
    }

    public final boolean load(CommentedConfig config) {
        Object rawValue = config.get(pathString);
        if (rawValue == null) {
            reset();
            return true;
        }

        ReadResult<T> result = readValue(rawValue);
        if (!result.valid()) {
            reset();
            return true;
        }

        value = copyValue(result.value());
        return result.dirty();
    }

    public final void save(CommentedConfig config) {
        config.set(pathString, writeValue(value));

        String tomlComment = buildTomlComment();
        if (tomlComment != null) {
            config.setComment(pathString, tomlComment);
        }
    }

    public final boolean isInRange(Object candidateValue) {
        return readValue(candidateValue).valid();
    }

    public final Component translatedName() {
        if (translationKey == null) {
            return CommonComponents.EMPTY;
        }

        return Component.translatable(translationKey);
    }

    @Nullable
    public final Component tooltipComponent() {
        if (translationKey == null) {
            return null;
        }

        MutableComponent tooltipComponent = Component.translatable(translationKey + ".tooltip");
        tooltipComponent.append("\n\n");
        tooltipComponent.append(defaultTooltipComponent());
        return tooltipComponent;
    }

    public final Component defaultTooltipComponent() {
        return Component.translatable("mapfrontiers.default", defaultValueComponent(defaultValue))
                .withStyle(Style.EMPTY.withBold(true));
    }

    @Nullable
    private String buildTomlComment() {
        String constraints = describeConstraints();
        if (comment == null && constraints == null) {
            return null;
        }

        if (comment == null) {
            return constraints;
        }

        if (constraints == null) {
            return comment;
        }

        return comment + "\n" + constraints;
    }

    protected final Component booleanComponent(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    protected final Component literalComponent(Object value) {
        return Component.literal(Objects.toString(value));
    }

    protected record ReadResult<T>(boolean valid, boolean dirty, @Nullable T value) {
        public static <T> ReadResult<T> valid(T value) {
            return new ReadResult<>(true, false, value);
        }

        public static <T> ReadResult<T> valid(T value, boolean dirty) {
            return new ReadResult<>(true, dirty, value);
        }

        public static <T> ReadResult<T> invalid() {
            return new ReadResult<>(false, false, null);
        }
    }
}
