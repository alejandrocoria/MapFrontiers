package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class VertexShapePresetSelector extends ShapePresetSelector {
    private static final int COLUMNS = 6;
    private static final int BUTTON_COUNT = 12;
    private static final int TEXTURE_OFFSET_X = 0;
    private static final int MIN_CUSTOM_VERTEX_COUNT = 3;
    private static final int MAX_CUSTOM_VERTEX_COUNT = 999;
    private static final int[] VERTEX_COUNT = {
            0, 1, 3, 3, 3, 3, 4, 4, 6, 6, 8, 16
    };

    private static final double[] VERTEX_ANGLE = {
            0.0, 0.0, -90.0, 0.0, 90.0, 180.0, 45.0, 0.0, 30.0, 0.0, 22.5, 0.0
    };

    public enum ShapeMeasure {
        None, Width, Radius
    }

    private final Consumer<VertexShapePresetSelector> callbackShapeUpdated;

    public VertexShapePresetSelector(Font font, int selected, Consumer<VertexShapePresetSelector> callbackShapeUpdated) {
        super(font, selected, COLUMNS, BUTTON_COUNT, TEXTURE_OFFSET_X);
        this.callbackShapeUpdated = callbackShapeUpdated;
    }

    public ShapeMeasure getShapeMeasure() {
        if (selected < 2) {
            return ShapeMeasure.None;
        } else if (selected < 7) {
            return ShapeMeasure.Width;
        } else {
            return ShapeMeasure.Radius;
        }
    }

    public int getVertexCount() {
        return VERTEX_COUNT[selected];
    }

    public List<Vec2> getVertices() {
        return getVertices(VERTEX_COUNT[selected], VERTEX_ANGLE[selected] / 180.0 * Math.PI);
    }

    public List<Vec2> getVertices(int count) {
        return getVertices(Mth.clamp(count, MIN_CUSTOM_VERTEX_COUNT, MAX_CUSTOM_VERTEX_COUNT), 0.0);
    }

    private List<Vec2> getVertices(int count, double angleOffset) {
        if (count == 0) {
            return null;
        }

        List<Vec2> vertices = new ArrayList<>();

        if (count == 1) {
            vertices.add(Vec2.ZERO);
            return vertices;
        }

        for (int i = 0; i < count; ++i) {
            double vertexAngle = Math.PI * 2 / count * i + angleOffset;
            vertices.add(new Vec2((float) Math.cos(vertexAngle), (float) Math.sin(vertexAngle)));
        }

        return vertices;
    }

    @Override
    protected void onSelectionChanged() {
        callbackShapeUpdated.accept(this);
    }
}
