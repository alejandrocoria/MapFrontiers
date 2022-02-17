package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import journeymap.client.api.IClientAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierList extends Screen implements GuiScrollBox.ScrollBoxResponder {
    private IClientAPI jmAPI;

    private GuiScrollBox frontiers;
    private GuiSettingsButton buttonCreate;
    private GuiSettingsButton buttonDelete;
    private GuiSettingsButton buttonDone;

    private final List<GuiSimpleLabel> labels;
    private final Map<GuiSimpleLabel, List<Component>> labelTooltips;

    public GuiFrontierList(IClientAPI jmAPI) {
        super(TextComponent.EMPTY);
        this.jmAPI = jmAPI;

        labels = new ArrayList<>();
        labelTooltips = new HashMap<>();
    }

    @Override
    public void init() {
        frontiers = new GuiScrollBox(width / 2 - 300, 50, 600, height - 120, 21, this);

        buttonCreate = new GuiSettingsButton(font, width / 2 - 230, height - 30, 140,
                new TranslatableComponent("mapfrontiers.create"), this::buttonPressed);
        buttonDelete = new GuiSettingsButton(font, width / 2 - 70, height - 30, 140,
                new TranslatableComponent("mapfrontiers.delete"), this::buttonPressed);
        buttonDone = new GuiSettingsButton(font, width / 2 + 90, height - 30, 140,
                new TranslatableComponent("gui.done"), this::buttonPressed);

        addRenderableWidget(frontiers);
        addRenderableWidget(buttonCreate);
        addRenderableWidget(buttonDelete);
        addRenderableWidget(buttonDone);

        updateFrontiers();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        for (GuiSimpleLabel label : labels) {
            label.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        for (GuiSimpleLabel label : labels) {
            if (label.isHoveredOrFocused()) {
                List<Component> tooltip = labelTooltips.get(label);
                if (tooltip == null) {
                    continue;
                }

                renderTooltip(matrixStack, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    protected void buttonPressed(Button button) {
        if (button == buttonCreate) {
            ForgeHooksClient.popGuiLayer(minecraft);
            GuiNewFrontier guiNewFrontier = new GuiNewFrontier(jmAPI);
            ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), guiNewFrontier);
        } else if (button == buttonDelete) {
            FrontierOverlay frontier = ((GuiFrontierListElement) frontiers.getSelectedElement()).getFrontier();
            FrontiersOverlayManager frontierManager = ClientProxy.getFrontiersOverlayManager(frontier.getPersonal());
            frontierManager.clientDeleteFrontier(frontier);
            frontiers.removeElement(frontiers.getSelectedElement());
        } else if (button == buttonDone) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }

    @Override
    public void elementClicked(GuiScrollBox scrollBox, GuiScrollBox.ScrollElement element) {
        if (scrollBox == frontiers) {

        }
    }

    @Override
    public void elementDelete(GuiScrollBox scrollBox, GuiScrollBox.ScrollElement element) {

    }

    private void updateFrontiers() {
        frontiers.removeAll();

        for (ArrayList<FrontierOverlay> dimension : ClientProxy.getFrontiersOverlayManager(true).getAllFrontiers().values()) {
            for (FrontierOverlay frontier : dimension) {
                frontiers.addElement(new GuiFrontierListElement(font, renderables, frontier));
            }
        }

        for (ArrayList<FrontierOverlay> dimension : ClientProxy.getFrontiersOverlayManager(false).getAllFrontiers().values()) {
            for (FrontierOverlay frontier : dimension) {
                frontiers.addElement(new GuiFrontierListElement(font, renderables, frontier));
            }
        }
    }
}
