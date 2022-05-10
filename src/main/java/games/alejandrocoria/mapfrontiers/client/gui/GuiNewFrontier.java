package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiNewFrontier extends Screen implements TextIntBox.TextIntBoxResponder {
    private final IClientAPI jmAPI;

    private GuiOptionButton buttonFrontierType;
    private GuiOptionButton buttonAfterCreate;
    private GuiShapeButtons shapeButtons;
    private GuiSimpleLabel labelSize;
    private TextIntBox textSize;
    private GuiSettingsButton buttonCreateFrontier;
    private GuiSettingsButton buttonCancel;

    public GuiNewFrontier(IClientAPI jmAPI) {
        super(StringTextComponent.EMPTY);
        this.jmAPI = jmAPI;

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        TextComponent title = new TranslationTextComponent("mapfrontiers.title_new_frontier");
        buttons.add(new GuiSimpleLabel(font, width / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        buttons.add(new GuiSimpleLabel(font, width / 2 - 130, height / 2 - 96, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.frontier_type"), GuiColors.SETTINGS_TEXT));
        buttonFrontierType = new GuiOptionButton(font, width / 2, height / 2 - 98, 130, this::buttonPressed);
        buttonFrontierType.addOption(ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.Global));
        buttonFrontierType.addOption(ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.Personal));
        buttonFrontierType.setSelected(0);

        SettingsProfile profile = ClientProxy.getSettingsProfile();

        if (profile.personalFrontier != SettingsProfile.State.Enabled) {
            buttonFrontierType.setSelected(0);
            buttonFrontierType.active = false;
        } else if (profile.createFrontier != SettingsProfile.State.Enabled) {
            buttonFrontierType.setSelected(1);
            buttonFrontierType.active = false;
        }

        buttons.add(new GuiSimpleLabel(font, width / 2 - 130, height / 2 - 80, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.after_creating"), GuiColors.SETTINGS_TEXT));
        buttonAfterCreate = new GuiOptionButton(font, width / 2, height / 2 - 82, 130, this::buttonPressed);
        buttonAfterCreate.addOption(ConfigData.getTranslatedEnum(ConfigData.AfterCreatingFrontier.Info));
        buttonAfterCreate.addOption(ConfigData.getTranslatedEnum(ConfigData.AfterCreatingFrontier.Edit));
        buttonAfterCreate.addOption(ConfigData.getTranslatedEnum(ConfigData.AfterCreatingFrontier.Nothing));
        buttonAfterCreate.setSelected(ConfigData.afterCreatingFrontier.ordinal());

        shapeButtons = new GuiShapeButtons(font, width / 2 - 162, height / 2 - 52, ConfigData.newFrontierShape, (s) -> shapeButtonsUpdated());

        labelSize = new GuiSimpleLabel(font, width / 2 - 80, height / 2 + 90, GuiSimpleLabel.Align.Left, new StringTextComponent(""), GuiColors.WHITE);
        buttons.add(labelSize);
        textSize = new TextIntBox(20, 0, 999, font, width / 2 + 16, height / 2 + 88, 64);
        textSize.setResponder(this);

        buttonCreateFrontier = new GuiSettingsButton(font, width / 2 - 110, height / 2 + 146, 100,
                new TranslationTextComponent("mapfrontiers.create"), this::buttonPressed);
        buttonCancel = new GuiSettingsButton(font, width / 2 + 10, height / 2 + 146, 100,
                new TranslationTextComponent("gui.cancel"), this::buttonPressed);

        addButton(buttonFrontierType);
        addButton(buttonAfterCreate);
        addButton(shapeButtons);
        addButton(textSize);
        addButton(buttonCreateFrontier);
        addButton(buttonCancel);

        shapeButtonsUpdated();
    }

    @Override
    public void tick() {
        textSize.tick();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0) {
            textSize.mouseClicked(mouseX, mouseY, mouseButton);
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonAfterCreate) {
            ConfigData.afterCreatingFrontier = ConfigData.AfterCreatingFrontier.values()[buttonAfterCreate.getSelected()];
            ClientProxy.configUpdated();
        } else if (button == buttonCreateFrontier) {
            boolean personal = buttonFrontierType.getSelected() == 1;
            ClientProxy.getFrontiersOverlayManager(personal).clientCreateNewfrontier(jmAPI.getUIState(Context.UI.Fullscreen).dimension, calculateVertices());
            ForgeHooksClient.popGuiLayer(minecraft);
        } else if (button == buttonCancel) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }

    @Override
    public void updatedValue(TextIntBox textIntBox, int value) {
        if (textSize == textIntBox) {
            if (shapeButtons.getShapeMeasure() == GuiShapeButtons.ShapeMeasure.Width) {
                if (ConfigData.isInRange("newFrontierShapeWidth", value)) {
                    ConfigData.newFrontierShapeWidth = value;
                }
            } else if (shapeButtons.getShapeMeasure() == GuiShapeButtons.ShapeMeasure.Radius) {
                if (ConfigData.isInRange("newFrontierShapeRadius", value)) {
                    ConfigData.newFrontierShapeRadius = value;
                }
            }
        }
    }

    @Override
    public void removed() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedSettingsProfileEvent(UpdatedSettingsProfileEvent event) {
        ForgeHooksClient.popGuiLayer(minecraft);
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiNewFrontier(jmAPI));
    }

    private void shapeButtonsUpdated() {
        int selected = shapeButtons.getSelected();
        ConfigData.newFrontierShape = selected;

        if (selected == 0 || selected == 1) {
            labelSize.visible = false;
            textSize.visible = false;
            return;
        }

        labelSize.visible = true;
        textSize.visible = true;

        if (shapeButtons.getShapeMeasure() == GuiShapeButtons.ShapeMeasure.Width) {
            labelSize.setText(new TranslationTextComponent("mapfrontiers.shape_width"));
            textSize.setValue(String.valueOf(ConfigData.newFrontierShapeWidth));
        } else if (shapeButtons.getShapeMeasure() == GuiShapeButtons.ShapeMeasure.Radius) {
            labelSize.setText(new TranslationTextComponent("mapfrontiers.shape_radius"));
            textSize.setValue(String.valueOf(ConfigData.newFrontierShapeRadius));
        }
    }

    private List<BlockPos> calculateVertices() {
        List<Vector3d> shapeVertices = shapeButtons.getVertices();
        if (shapeVertices == null) {
            return null;
        }

        double radius = 0.0;

        if (shapeButtons.getShapeMeasure() == GuiShapeButtons.ShapeMeasure.Width) {
            radius = ConfigData.newFrontierShapeWidth;
            if (radius < 2) {
                radius = 2;
            }
            if (shapeVertices.size() == 3) {
                radius = radius * Math.sqrt(3.0) / 3.0;
            } else if (shapeVertices.size() == 4) {
                radius = Math.sqrt(radius * radius * 2.0) / 2.0;
            }
        } else if (shapeButtons.getShapeMeasure() == GuiShapeButtons.ShapeMeasure.Radius) {
            radius = ConfigData.newFrontierShapeRadius;
            if (radius < 1) {
                radius = 1;
            }
        }

        Set<BlockPos> polygonVertices = new LinkedHashSet<BlockPos>();
        BlockPos playerPos = minecraft.player.blockPosition();

        for (Vector3d vertex : shapeVertices) {
            int x = (int) Math.round(vertex.x * radius) + playerPos.getX();
            int z = (int) Math.round(vertex.z * radius) + playerPos.getZ();
            polygonVertices.add(new BlockPos(x, 70, z));
        }

        return new ArrayList<>(polygonVertices);
    }
}
