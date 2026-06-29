package games.alejandrocoria.mapfrontiers.client.gui.util;

import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public final class DefaultValueBinding<T> {
    private final IconButton button;
    private final Supplier<T> currentValue;
    private final Supplier<T> defaultValue;
    private final Consumer<T> restoreValue;
    private final Runnable syncWidgetsFromState;
    private final Tooltip tooltip;
    private final BiPredicate<T, T> equality;

    public DefaultValueBinding(Supplier<T> currentValue, Supplier<T> defaultValue, Consumer<T> restoreValue,
                               Runnable syncWidgetsFromState, Tooltip tooltip) {
        this(currentValue, defaultValue, restoreValue, syncWidgetsFromState, tooltip, Objects::equals);
    }

    public DefaultValueBinding(Supplier<T> currentValue, Supplier<T> defaultValue, Consumer<T> restoreValue,
                               Runnable syncWidgetsFromState, Tooltip tooltip,
                               BiPredicate<T, T> equality) {
        this.currentValue = currentValue;
        this.defaultValue = defaultValue;
        this.restoreValue = restoreValue;
        this.syncWidgetsFromState = syncWidgetsFromState;
        this.tooltip = tooltip;
        this.equality = equality;

        button = new IconButton(IconButton.Type.RestoreDefault, b -> restoreDefault());
        refresh();
    }

    public IconButton button() {
        return button;
    }

    public boolean isAtDefault() {
        return equality.test(currentValue.get(), defaultValue.get());
    }

    public void restoreDefault() {
        restoreValue.accept(defaultValue.get());
        syncWidgetsFromState.run();
        refresh();
    }

    public void refresh() {
        button.active = !isAtDefault();
        button.setTooltip(tooltip);
    }

    public static <T> DefaultValueBinding<T> forConfigEntry(Component actionLabel, ConfigEntry<T, ?> entry,
                                                            Supplier<T> currentValue, Consumer<T> restoreValue,
                                                            Runnable syncWidgetsFromState) {
        return new DefaultValueBinding<>(currentValue, entry::defaultValue, restoreValue, syncWidgetsFromState,
                createRestoreTooltip(actionLabel, entry.defaultTooltipComponent()));
    }

    public static Tooltip createRestoreTooltip(Component actionLabel, Component defaultValueLine) {
        MutableComponent tooltip = actionLabel.copy();
        tooltip.append("\n\n");
        tooltip.append(defaultValueLine);
        return Tooltip.create(tooltip);
    }
}
