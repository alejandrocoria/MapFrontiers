package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import net.minecraft.client.gui.Font;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class PathShapePresetSelector extends ShapePresetSelector {
    public enum ShapeMeasure {
        None, Length
    }

    private static final int COLUMNS = 4;
    private static final int BUTTON_COUNT = 8;
    private static final int TEXTURE_OFFSET_X = 980;
    private static final int[] POINT_COUNT = {
            0, 1, 3, 3, 2, 2, 2, 2
    };

    private final Consumer<PathShapePresetSelector> callbackShapeUpdated;

    public PathShapePresetSelector(Font font, int selected, Consumer<PathShapePresetSelector> callbackShapeUpdated) {
        super(font, selected, COLUMNS, BUTTON_COUNT, TEXTURE_OFFSET_X);
        this.callbackShapeUpdated = callbackShapeUpdated;
    }

    public ShapeMeasure getShapeMeasure() {
        return selected < 2 ? ShapeMeasure.None : ShapeMeasure.Length;
    }

    public int getPointCount() {
        return POINT_COUNT[selected];
    }

    @Override
    protected void onSelectionChanged() {
        callbackShapeUpdated.accept(this);
    }
}
