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
    private int scrollBarPos = 0;
    private int scrollBarHeight = 0;
    private boolean scrollBarHovered = false;
    private boolean scrollBarGrabbed = false;
    private int scrollBarGrabbedYPos = 0;
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
        this.elementHeight = elementHeight;
        scrollHeight = height / elementHeight;
        this.height = scrollHeight * elementHeight;
        this.responder = responder;
    }

    public int getId() {
        return id;
    }

    public List<ScrollElement> getElements() {
        return elements;
    }

    public void addElement(ScrollElement element) {
        element.setX(x);
        element.setY(y + elements.size() * elementHeight);
        elements.add(element);
        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();
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

        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();

        if (responder != null) {
            responder.elementDelete(id, removed);
        }
    }

    public void removeAll() {
        elements.clear();
        selected = -1;
        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();
    }

    public void scroll(int amount) {
        if (visible && (hovered || scrollBarHovered) && !scrollBarGrabbed) {
            if (amount < 0 && scrollStart == 0) {
                return;
            } else if (amount > 0 && scrollStart + scrollHeight >= elements.size()) {
                return;
            }

            scrollStart += amount;
            updateScrollWindow();
            updateScrollBar();
        }
    }

    public void scrollBottom() {
        scrollStart = elements.size() - scrollHeight;
        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();
    }

    public void drawBox(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            for (int i = 0; i < elements.size(); ++i) {
                elements.get(i).draw(mc, mouseX, mouseY, selected == i);
            }

            if (scrollBarHeight > 0) {
                scrollBarHovered = mouseX >= x + width + 5 && mouseY >= y && mouseX < x + width + 15 && mouseY < y + height;

                int barColor = GuiColors.SETTINGS_SCROLLBAR;
                if (scrollBarGrabbed) {
                    barColor = GuiColors.SETTINGS_SCROLLBAR_GRABBED;
                } else if (scrollBarHovered) {
                    barColor = GuiColors.SETTINGS_SCROLLBAR_HOVERED;
                }

                Gui.drawRect(x + width + 5, y, x + width + 15, y + height, GuiColors.SETTINGS_SCROLLBAR_BG);
                Gui.drawRect(x + width + 5, y + scrollBarPos, x + width + 15, y + scrollBarPos + scrollBarHeight, barColor);
            }
        } else {
            hovered = false;
            scrollBarHovered = false;
        }
    }

    public void mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            if (scrollBarHovered) {
                if (mouseY < y + scrollBarPos) {
                    scroll(-1);
                } else if (mouseY > y + scrollBarPos + scrollBarHeight) {
                    scroll(1);
                } else {
                    scrollBarGrabbed = true;
                    scrollBarGrabbedYPos = mouseY - y - scrollBarPos;
                }
            }

            if (hovered && !scrollBarGrabbed) {
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
    }

    public void mouseReleased(Minecraft mc, int mouseX, int mouseY) {
        if (visible && scrollBarHeight > 0 && scrollBarGrabbed) {
            scrollBarGrabbed = false;
            updateScrollBar();
        }
    }

    public void mouseClickMove(Minecraft mc, int mouseX, int mouseY) {
        if (visible && scrollBarHeight > 0 && scrollBarGrabbed) {
            int delta = mouseY - y - scrollBarPos - scrollBarGrabbedYPos;

            if (delta == 0) {
                return;
            }

            scrollBarPos += delta;
            if (scrollBarPos < 0) {
                scrollBarPos = 0;
            } else if (scrollBarPos + scrollBarHeight > height) {
                scrollBarPos = height - scrollBarHeight;
            }

            int newScrollStart = Math.round(((float) scrollBarPos) / height * elements.size());

            if (newScrollStart != scrollStart) {
                scrollStart = newScrollStart;
                updateScrollWindow();
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

    private void updateScrollBar() {
        if (elements.size() <= scrollHeight) {
            scrollBarHeight = 0;
            scrollBarHovered = false;
            scrollBarGrabbed = false;
            return;
        }

        scrollBarHeight = Math.round(((float) scrollHeight) / elements.size() * height);
        scrollBarPos = Math.round(((float) scrollStart) / elements.size() * height);
        if (scrollBarPos + scrollBarHeight > height) {
            scrollBarPos = height - scrollBarHeight;
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
