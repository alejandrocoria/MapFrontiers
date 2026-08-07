package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.common.Context;

import java.util.Objects;

public final class OverlayActivation {
    private static final Context.UI[][] UI_ARRAYS = createUiArrays();
    private static final Context.MapType[][] MAP_TYPE_ARRAYS = createMapTypeArrays();

    private final int uiMask;
    private final int mapTypeMask;

    private OverlayActivation(int uiMask, int mapTypeMask) {
        this.uiMask = uiMask;
        this.mapTypeMask = mapTypeMask;
    }

    public static OverlayActivation of(Context.UI ui, Context.MapType... mapTypes) {
        return of(new Context.UI[]{Objects.requireNonNull(ui, "ui")}, mapTypes);
    }

    public static OverlayActivation of(Context.UI[] uis, Context.MapType[] mapTypes) {
        return new OverlayActivation(toMask(uis), toMask(mapTypes));
    }

    Context.UI[] getUis() {
        return UI_ARRAYS[uiMask];
    }

    Context.MapType[] getMapTypes() {
        return MAP_TYPE_ARRAYS[mapTypeMask];
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof OverlayActivation other
                && uiMask == other.uiMask
                && mapTypeMask == other.mapTypeMask;
    }

    @Override
    public int hashCode() {
        return 31 * uiMask + mapTypeMask;
    }

    private static int toMask(Context.UI[] values) {
        Objects.requireNonNull(values, "uis");
        int mask = 0;
        for (Context.UI value : values) {
            mask |= 1 << Objects.requireNonNull(value, "ui").ordinal();
        }
        return mask;
    }

    private static int toMask(Context.MapType[] values) {
        Objects.requireNonNull(values, "mapTypes");
        int mask = 0;
        for (Context.MapType value : values) {
            mask |= 1 << Objects.requireNonNull(value, "mapType").ordinal();
        }
        return mask;
    }

    private static Context.UI[][] createUiArrays() {
        Context.UI[] values = Context.UI.values();
        Context.UI[][] arrays = new Context.UI[1 << values.length][];
        for (int mask = 0; mask < arrays.length; mask++) {
            arrays[mask] = select(values, mask, Context.UI[]::new);
        }
        return arrays;
    }

    private static Context.MapType[][] createMapTypeArrays() {
        Context.MapType[] values = Context.MapType.values();
        Context.MapType[][] arrays = new Context.MapType[1 << values.length][];
        for (int mask = 0; mask < arrays.length; mask++) {
            arrays[mask] = select(values, mask, Context.MapType[]::new);
        }
        return arrays;
    }

    private static <E> E[] select(E[] values, int mask, java.util.function.IntFunction<E[]> arrayFactory) {
        int size = Integer.bitCount(mask);
        E[] selected = arrayFactory.apply(size);
        int index = 0;
        for (int ordinal = 0; ordinal < values.length; ordinal++) {
            if ((mask & 1 << ordinal) != 0) {
                selected[index++] = values[ordinal];
            }
        }
        return selected;
    }
}
