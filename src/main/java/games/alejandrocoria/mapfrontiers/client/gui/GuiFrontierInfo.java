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
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierInfo extends Screen implements TextColorBox.TextColorBoxResponder, TextBox.TextBoxResponder {
    private IClientAPI jmAPI;

    private FrontiersOverlayManager frontiersOverlayManager;
    private FrontierOverlay frontier;
    private TextBox textName1;
    private TextBox textName2;
    private TextColorBox textRed;
    private TextColorBox textGreen;
    private TextColorBox textBlue;

    private GuiSettingsButton buttonSelect;
    private GuiSettingsButton buttonShareSettings;
    private GuiSettingsButton buttonDelete;
    private GuiSettingsButton buttonDone;
    private GuiSettingsButton buttonBanner;

    private final List<GuiSimpleLabel> labels;
    private final Map<GuiSimpleLabel, List<Component>> labelTooltips;

    public GuiFrontierInfo(IClientAPI jmAPI, FrontierOverlay frontier) {
        super(TextComponent.EMPTY);
        this.jmAPI = jmAPI;
        frontiersOverlayManager = ClientProxy.getFrontiersOverlayManager(frontier.getPersonal());
        this.frontier = frontier;
        labels = new ArrayList<>();
        labelTooltips = new HashMap<>();
    }

    @Override
    public void init() {
        int leftSide = width / 2 - 137;
        int rightSide = width / 2 + 10;
        int top = height / 2 - 62;

        labels.add(new GuiSimpleLabel(font, leftSide, top, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("mapfrontiers.name"), GuiColors.LABEL_TEXT));

        String defaultText = "Add name";
        textName1 = new TextBox(font, leftSide, top + 12, 127, defaultText);
        textName1.setMaxLength(17);
        textName1.setHeight(20);
        textName1.setResponder(this);
        textName1.setValue(frontier.getName1());
        textName1.setFrame(true);
        textName1.setCentered(false);
        textName2 = new TextBox(font, leftSide, top + 40, 127, defaultText);
        textName2.setMaxLength(17);
        textName2.setHeight(20);
        textName2.setResponder(this);
        textName2.setValue(frontier.getName2());
        textName2.setFrame(true);
        textName2.setCentered(false);

        labels.add(new GuiSimpleLabel(font, leftSide, top + 70, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("mapfrontiers.color"), GuiColors.LABEL_TEXT));

        labels.add(new GuiSimpleLabel(font, leftSide - 11, top + 88, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("R"), GuiColors.LABEL_TEXT));

        textRed = new TextColorBox(255, font, leftSide, top + 82);
        textRed.setResponder(this);
        textRed.setHeight(20);

        labels.add(new GuiSimpleLabel(font, leftSide + 38, top + 88, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("G"), GuiColors.LABEL_TEXT));

        textGreen = new TextColorBox(255, font, leftSide + 49, top + 82);
        textGreen.setResponder(this);
        textGreen.setHeight(20);

        labels.add(new GuiSimpleLabel(font, leftSide + 87, top + 88, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("B"), GuiColors.LABEL_TEXT));

        textBlue = new TextColorBox(255, font, leftSide + 98, top + 82);
        textBlue.setResponder(this);
        textBlue.setHeight(20);

        textRed.setValue((frontier.getColor() & 0xff0000) >> 16);
        textGreen.setValue((frontier.getColor() & 0x00ff00) >> 8);
        textBlue.setValue(frontier.getColor() & 0x0000ff);

        Component type = new TranslatableComponent("mapfrontiers.type",
                new TranslatableComponent(frontier.getPersonal() ? "mapfrontiers.personal" : "mapfrontiers.global"));
        labels.add(new GuiSimpleLabel(font, rightSide, top, GuiSimpleLabel.Align.Left, type, GuiColors.WHITE));

        Component dimension = new TranslatableComponent("mapfrontiers.dimension", frontier.getDimension().location().toString());
        labels.add(new GuiSimpleLabel(font, rightSide, top + 16, GuiSimpleLabel.Align.Left, dimension, GuiColors.WHITE));

        Component vertices = new TranslatableComponent("mapfrontiers.vertices", frontier.getVertexCount());
        labels.add(new GuiSimpleLabel(font, rightSide, top + 32, GuiSimpleLabel.Align.Left, vertices, GuiColors.WHITE));

        Component area = new TranslatableComponent("mapfrontiers.area", frontier.area);
        labels.add(new GuiSimpleLabel(font, rightSide, top + 48, GuiSimpleLabel.Align.Left, area, GuiColors.WHITE));

        Component perimeter = new TranslatableComponent("mapfrontiers.perimeter", frontier.perimeter);
        labels.add(new GuiSimpleLabel(font, rightSide, top + 64, GuiSimpleLabel.Align.Left, perimeter, GuiColors.WHITE));

        buttonSelect = new GuiSettingsButton(font, leftSide, top + 110, 140,
                new TranslatableComponent("mapfrontiers.select_in_map"), this::buttonPressed);
        buttonShareSettings = new GuiSettingsButton(font, leftSide + 150, top + 110, 140,
                new TranslatableComponent("mapfrontiers.share_settings"), this::buttonPressed);
        buttonShareSettings.visible = frontier.getPersonal();
        buttonDelete = new GuiSettingsButton(font, leftSide, top + 132, 140,
                new TranslatableComponent("mapfrontiers.delete"), this::buttonPressed);
        buttonDone = new GuiSettingsButton(font, leftSide + 150, top + 132, 140,
                new TranslatableComponent("gui.done"), this::buttonPressed);

        buttonBanner = new GuiSettingsButton(font, leftSide - 150, top, 140,
                new TranslatableComponent("mapfrontiers.assign_banner"), this::buttonPressed);

        addRenderableWidget(textName1);
        addRenderableWidget(textName2);
        addRenderableWidget(textRed);
        addRenderableWidget(textGreen);
        addRenderableWidget(textBlue);
        addRenderableWidget(buttonSelect);
        addRenderableWidget(buttonShareSettings);
        addRenderableWidget(buttonDelete);
        addRenderableWidget(buttonDone);
        addRenderableWidget(buttonBanner);

        updateBannerButton();
    }

    @Override
    public void tick() {
        textName1.tick();
        textName2.tick();
        textRed.tick();
        textGreen.tick();
        textBlue.tick();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        for (GuiSimpleLabel label : labels) {
            label.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        fill(matrixStack, width / 2 + 10, height / 2 + 19, width / 2 + 32, height / 2 + 41,
                GuiColors.COLOR_INDICATOR_BORDER);
        fill(matrixStack, width / 2 + 11, height / 2 + 20, width / 2 + 31, height / 2 + 40,
                frontier.getColor() | 0xff000000);

        if (frontier.hasBanner()) {
            frontier.renderBanner(minecraft, matrixStack, width / 2 - 260, height / 2 - 40, 4);
        }

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
        if (button == buttonSelect) {
            // @Incomplete
            ForgeHooksClient.popGuiLayer(minecraft);
        } else if (button == buttonShareSettings) {
            GuiShareSettings guiShareSettings = new GuiShareSettings(null, frontiersOverlayManager, frontier);
            ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), guiShareSettings);
        } else if (button == buttonDelete) {
            frontiersOverlayManager.clientDeleteFrontier(frontier);
            ForgeHooksClient.popGuiLayer(minecraft);
        } else if (button == buttonDone) {
            ForgeHooksClient.popGuiLayer(minecraft);
        } else if (button == buttonBanner) {
            if (!frontier.hasBanner()) {
                ItemStack heldBanner = ClientProxy.getHeldBanner();
                if (heldBanner != null) {
                    frontier.setBanner(heldBanner);
                }
            } else {
                frontier.setBanner(null);
            }
            updateBannerButton();
            sendChangesToServer();
        }
    }

    @Override
    public void updatedValue(TextColorBox textBox, int value) {
        if (textRed == textBox) {
            int newColor = (frontier.getColor() & 0xff00ffff) | (value << 16);
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                sendChangesToServer();
            }
        } else if (textGreen == textBox) {
            int newColor = (frontier.getColor() & 0xffff00ff) | (value << 8);
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                sendChangesToServer();
            }
        } else if (textBlue == textBox) {
            int newColor = (frontier.getColor() & 0xffffff00) | value;
            if (newColor != frontier.getColor()) {
                frontier.setColor(newColor);
                sendChangesToServer();
            }
        }
    }

    @Override
    public void updatedValue(TextBox textBox, String value) {
        if (textName1 == textBox) {
            if (!frontier.getName1().equals(value)) {
                frontier.setName1(value);
            }
        } else if (textName2 == textBox) {
            if (!frontier.getName2().equals(value)) {
                frontier.setName2(value);
            }
        }
    }

    @Override
    public void lostFocus(TextBox textBox, String value) {
        sendChangesToServer();
    }

    private void updateBannerButton() {
        if (!frontier.hasBanner()) {
            TranslatableComponent message = new TranslatableComponent("mapfrontiers.assign_banner");
            if (ClientProxy.getHeldBanner() == null) {
                message.append(new TextComponent(" !"));
            }

            buttonBanner.setMessage(message);
        } else {
            buttonBanner.setMessage(new TranslatableComponent("mapfrontiers.remove_banner"));
        }
    }

    private void sendChangesToServer() {
        frontiersOverlayManager.clientUpdatefrontier(frontier);
    }
}
