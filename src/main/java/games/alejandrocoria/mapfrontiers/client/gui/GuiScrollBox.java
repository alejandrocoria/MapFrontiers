package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiScrollBox extends Gui {
    public boolean visible = true;
    private int id;
    private int x = 0;
    private int y = 0;
    private int width;
    private int height;
    private boolean hovered = false;
    private int elementHeight;
    private int scrollStart = 0;
    private int scrollHeight = 0;
    private List<ScrollElement> elements;
    private int selected;
    private ScrollBoxResponder responder;

    public GuiScrollBox(int id, int x, int y, int width, int height, int elementHeight, ScrollBoxResponder responder) {
        this.id = id;
        elements = new ArrayList<ScrollElement>();
        selected = -1;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.elementHeight = elementHeight;
        scrollHeight = height / elementHeight;
        this.responder = responder;
    }

    public int getId() {
        return id;
    }

    public void addElement(ScrollElement element) {
        element.setX(x);
        element.setY(y + elements.size() * elementHeight);
        elements.add(element);
        updateScrollWindow();
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

    private void removeElement(ScrollElement element, ListIterator<ScrollElement> it) {
        ScrollElement removed = element;
        removed.delete();
        it.remove();

        if (selected == elements.size()) {
            selected = elements.size() - 1;
        }

        for (int i = 0; i < elements.size(); ++i) {
            elements.get(i).setY(y + i * elementHeight);
        }

        updateScrollWindow();

        if (responder != null) {
            responder.elementDelete(id, removed);
        }
    }

    public void removeAll() {
        elements.clear();
        selected = -1;
        updateScrollWindow();
    }

    public void scroll(int amount) {
        if (visible && hovered) {
            if (amount < 0 && scrollStart == 0) {
                return;
            } else if (amount > 0 && scrollStart + scrollHeight >= elements.size()) {
                return;
            }

            scrollStart += amount;
            updateScrollWindow();
        }
    }

    public void scrollBottom() {
        scrollStart = elements.size() - scrollHeight;
        updateScrollWindow();
    }

    public void drawBox(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            for (int i = 0; i < elements.size(); ++i) {
                elements.get(i).draw(mc, mouseX, mouseY, selected == i);
            }
        }
    }

    public void mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (visible && hovered) {
            ListIterator<ScrollElement> it = elements.listIterator();
            while (it.hasNext()) {
                ScrollElement element = it.next();
                ScrollElement.Action action = element.mousePressed(mc, mouseX, mouseY);
                if (action == ScrollElement.Action.Deleted) {
                    removeElement(element, it);
                } else if (action == ScrollElement.Action.Clicked) {
                    selectElement(element);
                    if (responder != null) {
                        responder.elementClicked(id, element);
                    }
                }

            }
        }
    }

    private void updateScrollWindow() {
        if (elements.size() <= scrollHeight) {
            scrollStart = 0;
        } else {
            int bottomExtra = elements.size() - (scrollStart + scrollHeight);
            if (bottomExtra < 0) {
                scrollStart += bottomExtra;
            }

            if (scrollStart < 0) {
                scrollStart = 0;
            }
        }

        for (int i = 0; i < elements.size(); ++i) {
            if (i < scrollStart || i >= scrollStart + scrollHeight) {
                elements.get(i).visible = false;
            } else {
                elements.get(i).visible = true;
                elements.get(i).setY(y + (i - scrollStart) * elementHeight);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public interface ScrollBoxResponder {
        public void elementClicked(int id, ScrollElement element);

        public void elementDelete(int id, ScrollElement element);
    }

    @SideOnly(Side.CLIENT)
    public static class ScrollElement {
        static enum Action {
            None, Clicked, Deleted
        }

        public boolean visible = true;
        protected int x = 0;
        protected int y = 0;
        protected boolean hovered = false;
        protected int height;
        protected int width;

        public ScrollElement(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public void delete() {
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void draw(Minecraft mc, int mouseX, int mouseY, boolean selected) {
        }

        public Action mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return Action.None;
        }
    }
}
