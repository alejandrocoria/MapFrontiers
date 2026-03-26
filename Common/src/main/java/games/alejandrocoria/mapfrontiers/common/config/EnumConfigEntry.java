package games.alejandrocoria.mapfrontiers.common.config;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class EnumConfigEntry<E extends Enum<E>> extends ConfigEntry<E, EnumConfigEntry<E>> {
    private final Class<E> enumClass;

    public EnumConfigEntry(Class<E> enumClass, E defaultValue, String... path) {
        super(defaultValue, path);
        this.enumClass = enumClass;
    }

    @Override
    protected EnumConfigEntry<E> self() {
        return this;
    }

    @Override
    protected ReadResult<E> readValue(Object rawValue) {
        if (enumClass.isInstance(rawValue)) {
            return ReadResult.valid(enumClass.cast(rawValue));
        }
        if (rawValue instanceof String stringValue) {
            try {
                return ReadResult.valid(Enum.valueOf(enumClass, stringValue));
            } catch (IllegalArgumentException ignored) {
                return ReadResult.invalid();
            }
        }

        return ReadResult.invalid();
    }

    @Override
    protected boolean isValidValue(E newValue) {
        return newValue != null;
    }

    @Override
    protected Object writeValue(E currentValue) {
        return currentValue.name();
    }

    @Override
    protected Component defaultValueComponent(E currentValue) {
        return Component.translatable("mapfrontiers.config." + currentValue.name());
    }

    @Override
    protected String describeConstraints() {
        return "Allowed Values: " + Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
