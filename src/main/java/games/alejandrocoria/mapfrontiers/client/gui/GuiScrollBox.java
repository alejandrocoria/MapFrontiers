package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GuiScrollBox extends Gui {
    public boolean visible = true;
    private int x = 0;
    private int y = 0;
    private int width;
    private int height;
    private int elementHeight;
    private List<ScrollElement> elements;
    private int selected;

    public GuiScrollBox(int x, int y, int width, int height, int elementHeight) {
        elements = new ArrayList<ScrollElement>();
        selected = -1;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.elementHeight = elementHeight;
    }

    public void addElement(ScrollElement element) {
        element.x = x;
        element.y = y + elements.size() * elementHeight;
        elements.add(element);
    }

    public void selectElement(ScrollElement element) {
        selected = elements.indexOf(element);
    }

    public ScrollElement getSelectedElement() {
        if (selected >= 0 && selected < elements.size()) {
            return elements.get(selected);
        }

        return null;
    }

    public void removeSelectedElement() {
        if (selected >= 0 && selected < elements.size()) {
            elements.remove(selected);

            for (int i = selected; i < elements.size(); ++i) {
                elements.get(i).y = y + i * elementHeight;
            }

            if (selected >= elements.size()) {
                selected = elements.size() - 1;
            }
        }
    }

    public void removeAll() {
        elements.clear();
        selected = -1;
    }

    public void drawBox(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            for (int i = 0; i < elements.size(); ++i) {
                elements.get(i).draw(mc, mouseX, mouseY, selected == i);
            }
        }
    }

    public void mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            if (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height) {
                for (ScrollElement element : elements) {
                    element.mousePressed(mc, mouseX, mouseY);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ScrollElement {
        public boolean visible = true;
        public int x = 0;
        public int y = 0;
        protected boolean hovered = false;
        protected int height;
        protected int width;

        public ScrollElement(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public void draw(Minecraft mc, int mouseX, int mouseY, boolean selected) {
        }

        public void mousePressed(Minecraft mc, int mouseX, int mouseY) {
        }
    }
}
