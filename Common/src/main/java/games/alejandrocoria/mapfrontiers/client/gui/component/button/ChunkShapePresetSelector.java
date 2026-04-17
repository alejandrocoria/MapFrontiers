package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ChunkShapePresetSelector extends ShapePresetSelector {
    public enum ShapeMeasure {
        None, Width, Length
    }

    private static final int COLUMNS = 4;
    private static final int BUTTON_COUNT = 8;
    private static final int TEXTURE_OFFSET_X = 588;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 32;

    private int size = MIN_SIZE;
    private final Consumer<ChunkShapePresetSelector> callbackShapeUpdated;

    public ChunkShapePresetSelector(Font font, int selected, Consumer<ChunkShapePresetSelector> callbackShapeUpdated) {
        super(font, selected, COLUMNS, BUTTON_COUNT, TEXTURE_OFFSET_X);
        this.callbackShapeUpdated = callbackShapeUpdated;
    }

    public void setSize(int size) {
        this.size = Mth.clamp(size, MIN_SIZE, MAX_SIZE);
    }

    public ShapeMeasure getShapeMeasure() {
        if (selected < 2 || selected == 7) {
            return ShapeMeasure.None;
        } else if (selected < 5) {
            return ShapeMeasure.Width;
        } else {
            return ShapeMeasure.Length;
        }
    }

    public int getChunkCount() {
        int chunks;
        switch (selected) {
            case 1 -> chunks = 1;
            case 2 -> chunks = size * size;
            case 3 -> chunks = Math.max(1, (size - 1) * 4);
            case 4 -> {
                chunks = (size * size + 1) / 2;
                if (size % 2 == 0) {
                    chunks += size;
                }
            }
            case 5, 6 -> chunks = size;
            case 7 -> chunks = 1024;
            default -> chunks = 0;
        }
        return chunks;
    }

    @Override
    protected void onSelectionChanged() {
        callbackShapeUpdated.accept(this);
    }
}
