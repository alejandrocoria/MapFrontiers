package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiNewFrontier extends Screen {
    private final IClientAPI jmAPI;

    private GuiOptionButton buttonFrontierType;
    private GuiOptionButton buttonAddVertexInPosition;
    private GuiOptionButton buttonAfterCreate;
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

        buttons.add(new GuiSimpleLabel(font, width / 2 - 150, height / 2 - 64, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.frontier_type"), GuiColors.SETTINGS_TEXT));
        buttonFrontierType = new GuiOptionButton(font, width / 2 + 20, height / 2 - 66, 130, this::buttonPressed);
        buttonFrontierType.addOption(new TranslationTextComponent("mapfrontiers.global"));
        buttonFrontierType.addOption(new TranslationTextComponent("mapfrontiers.personal"));
        buttonFrontierType.setSelected(0);

        SettingsProfile profile = ClientProxy.getSettingsProfile();

        if (profile.personalFrontier != SettingsProfile.State.Enabled) {
            buttonFrontierType.setSelected(0);
            buttonFrontierType.active = false;
        } else if (profile.createFrontier != SettingsProfile.State.Enabled) {
            buttonFrontierType.setSelected(1);
            buttonFrontierType.active = false;
        }

        buttons.add(new GuiSimpleLabel(font, width / 2 - 150, height / 2 - 48, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.add_vertex_in_position"), GuiColors.SETTINGS_TEXT));
        buttonAddVertexInPosition = new GuiOptionButton(font, width / 2 + 20, height / 2 - 50, 130, this::buttonPressed);
        buttonAddVertexInPosition.addOption(new TranslationTextComponent("options.on"));
        buttonAddVertexInPosition.addOption(new TranslationTextComponent("options.off"));
        buttonAddVertexInPosition.setSelected(ConfigData.addVertexToNewFrontier ? 0 : 1);

        buttons.add(new GuiSimpleLabel(font, width / 2 - 150, height / 2 - 32, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.after_creating"), GuiColors.SETTINGS_TEXT));
        buttonAfterCreate = new GuiOptionButton(font, width / 2 + 20, height / 2 - 34, 130, this::buttonPressed);
        buttonAfterCreate.addOption(new TranslationTextComponent("mapfrontiers.go_info"));
        buttonAfterCreate.addOption(new TranslationTextComponent("mapfrontiers.go_edit"));
        buttonAfterCreate.addOption(new TranslationTextComponent("mapfrontiers.do_nothing"));
        buttonAfterCreate.setSelected(ConfigData.afterCreatingFrontier.ordinal());

        buttonCreateFrontier = new GuiSettingsButton(font, width / 2 - 110, height / 2 + 50, 100,
                new TranslationTextComponent("mapfrontiers.create"), this::buttonPressed);
        buttonCancel = new GuiSettingsButton(font, width / 2 + 10, height / 2 + 50, 100,
                new TranslationTextComponent("gui.cancel"), this::buttonPressed);

        addButton(buttonFrontierType);
        addButton(buttonAddVertexInPosition);
        addButton(buttonAfterCreate);
        addButton(buttonCreateFrontier);
        addButton(buttonCancel);
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonAddVertexInPosition) {
            ConfigData.addVertexToNewFrontier = buttonAddVertexInPosition.getSelected() == 0;
            ClientProxy.configUpdated();
        } else if (button == buttonAfterCreate) {
            ConfigData.afterCreatingFrontier = ConfigData.AfterCreatingFrontier.values()[buttonAfterCreate.getSelected()];
            ClientProxy.configUpdated();
        } else if (button == buttonCreateFrontier) {
            boolean personal = buttonFrontierType.getSelected() == 1;
            ClientProxy.getFrontiersOverlayManager(personal).clientCreateNewfrontier(jmAPI.getUIState(Context.UI.Fullscreen).dimension);
            ForgeHooksClient.popGuiLayer(minecraft);
        } else if (button == buttonCancel) {
            ForgeHooksClient.popGuiLayer(minecraft);
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
}
