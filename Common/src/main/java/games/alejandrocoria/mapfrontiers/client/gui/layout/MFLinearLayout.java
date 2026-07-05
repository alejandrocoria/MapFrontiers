package games.alejandrocoria.mapfrontiers.client.gui.layout;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.layouts.AbstractLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MFLinearLayout extends LinearLayout {
    private final Orientation orientation;
    private final List<ChildContainer> children = new ArrayList<>();
    private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults();
    private int spacing;

    public MFLinearLayout(int width, int height, Orientation orientation) {
        this(0, 0, width, height, orientation);
    }

    public MFLinearLayout(int x, int y, int width, int height, Orientation orientation) {
        super(x, y, width, height, orientation);
        this.orientation = orientation;
    }

    public static MFLinearLayout horizontal() {
        return new MFLinearLayout(0, 0, Orientation.HORIZONTAL);
    }

    public static MFLinearLayout vertical() {
        return new MFLinearLayout(0, 0, Orientation.VERTICAL);
    }

    public MFLinearLayout spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public LayoutSettings defaultCellSetting() {
        return defaultChildLayoutSetting();
    }

    @Override
    public void arrangeElements() {
        super.arrangeElements();
        if (children.isEmpty()) {
            return;
        }

        int primaryLength = 0;
        int secondaryLength = 0;
        for (ChildContainer child : children) {
            primaryLength += getPrimaryLength(child);
            secondaryLength = Math.max(secondaryLength, getSecondaryLength(child));
        }
        primaryLength += spacing * (children.size() - 1);

        int primaryPosition = getPrimaryPosition(this);
        for (ChildContainer child : children) {
            setPrimaryPosition(child, primaryPosition);
            primaryPosition += getPrimaryLength(child) + spacing;
        }

        int secondaryPosition = getSecondaryPosition(this);
        for (ChildContainer childContainer : children) {
            setSecondaryPosition(childContainer, secondaryPosition, secondaryLength);
        }

        switch (orientation) {
            case HORIZONTAL -> {
                width = primaryLength;
                height = secondaryLength;
            }
            case VERTICAL -> {
                width = secondaryLength;
                height = primaryLength;
            }
        }
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        children.forEach(child -> visitor.accept(child.child));
    }

    @Override
    public LayoutSettings newChildLayoutSettings() {
        return defaultChildLayoutSettings.copy();
    }

    @Override
    public LayoutSettings defaultChildLayoutSetting() {
        return defaultChildLayoutSettings;
    }

    @Override
    public <T extends LayoutElement> T addChild(T child) {
        return addChild(child, newChildLayoutSettings());
    }

    @Override
    public <T extends LayoutElement> T addChild(T child, LayoutSettings layoutSettings) {
        children.add(new ChildContainer(child, layoutSettings));
        return child;
    }

    private int getPrimaryLength(ChildContainer child) {
        return switch (orientation) {
            case HORIZONTAL -> child.getWidth();
            case VERTICAL -> child.getHeight();
        };
    }

    private int getSecondaryLength(ChildContainer child) {
        return switch (orientation) {
            case HORIZONTAL -> child.getHeight();
            case VERTICAL -> child.getWidth();
        };
    }

    private void setPrimaryPosition(ChildContainer child, int position) {
        switch (orientation) {
            case HORIZONTAL -> child.setX(position, child.getWidth());
            case VERTICAL -> child.setY(position, child.getHeight());
        }
    }

    private void setSecondaryPosition(ChildContainer child, int position, int length) {
        switch (orientation) {
            case HORIZONTAL -> child.setY(position, length);
            case VERTICAL -> child.setX(position, length);
        }
    }

    private int getPrimaryPosition(LayoutElement element) {
        return switch (orientation) {
            case HORIZONTAL -> element.getX();
            case VERTICAL -> element.getY();
        };
    }

    private int getSecondaryPosition(LayoutElement element) {
        return switch (orientation) {
            case HORIZONTAL -> element.getY();
            case VERTICAL -> element.getX();
        };
    }

    private static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
        private ChildContainer(LayoutElement child, LayoutSettings layoutSettings) {
            super(child, layoutSettings);
        }
    }
}
