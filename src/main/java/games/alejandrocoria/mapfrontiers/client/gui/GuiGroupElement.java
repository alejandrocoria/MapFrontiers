package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiGroupElement extends GuiScrollBox.ScrollElement {
    private final FontRenderer fontRenderer;
    private SettingsGroup group;
    private GroupResponder responder;

    public GuiGroupElement(FontRenderer fontRenderer, SettingsGroup group, GroupResponder responder) {
        super(160, 16);
        this.fontRenderer = fontRenderer;
        this.group = group;
        this.responder = responder;
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean selected) {
        if (visible) {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            int color = 0xbbbbbb;
            if (selected) {
                color = 0xffffff;
            }

            if (hovered) {
                Gui.drawRect(x, y, x + width, y + height, 0xff222222);
            }

            fontRenderer.drawString(group.getName(), x + 4, y + 4, color);
        } else {
            hovered = false;
        }
    }

    @Override
    public void mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (visible && hovered && responder != null) {
            responder.groupClicked(this);
        }
    }

    @SideOnly(Side.CLIENT)
    public interface GroupResponder {
        public void groupClicked(GuiGroupElement element);
    }
}
