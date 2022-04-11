package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.event.DeletedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.NewFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import journeymap.client.api.IClientAPI;
import journeymap.client.data.WorldData;
import journeymap.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.Widget;
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
import java.util.*;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierList extends Screen implements GuiScrollBox.ScrollBoxResponder {
    private final IClientAPI jmAPI;
    private final GuiFullscreenMap fullscreenMap;

    private GuiScrollBox frontiers;
    private GuiScrollBox filterType;
    private GuiScrollBox filterOwner;
    private GuiScrollBox filterDimension;
    private GuiSettingsButton buttonResetFilters;
    private GuiSettingsButton buttonCreate;
    private GuiSettingsButton buttonInfo;
    private GuiSettingsButton buttonDelete;
    private GuiSettingsButton buttonVisible;
    private GuiSettingsButton buttonDone;

    public GuiFrontierList(IClientAPI jmAPI, GuiFullscreenMap fullscreenMap) {
        super(StringTextComponent.EMPTY);
        this.jmAPI = jmAPI;
        this.fullscreenMap = fullscreenMap;

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        TextComponent title = new TranslationTextComponent("mapfrontiers.title_frontiers");
        buttons.add(new GuiSimpleLabel(font, width / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        frontiers = new GuiScrollBox(width / 2 - 300, 50, 450, height - 100, 24, this);

        buttons.add(new GuiSimpleLabel(font, width / 2 + 170, 74, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.filter_type"), GuiColors.SETTINGS_TEXT));

        filterType = new GuiScrollBox(width / 2 + 170, 86, 200, 48, 16, this);
        filterType.addElement(new GuiRadioListElement(font, new TranslationTextComponent("mapfrontiers.all"), ConfigData.FilterFrontierType.All.ordinal()));
        filterType.addElement(new GuiRadioListElement(font, new TranslationTextComponent("mapfrontiers.global"), ConfigData.FilterFrontierType.Global.ordinal()));
        filterType.addElement(new GuiRadioListElement(font, new TranslationTextComponent("mapfrontiers.personal"), ConfigData.FilterFrontierType.Personal.ordinal()));
        filterType.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierType.ordinal());

        buttons.add(new GuiSimpleLabel(font, width / 2 + 170, 144, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.filter_owner"), GuiColors.SETTINGS_TEXT));

        filterOwner = new GuiScrollBox(width / 2 + 170, 156, 200, 48, 16, this);
        filterOwner.addElement(new GuiRadioListElement(font, new TranslationTextComponent("mapfrontiers.all"), ConfigData.FilterFrontierOwner.All.ordinal()));
        filterOwner.addElement(new GuiRadioListElement(font, new TranslationTextComponent("mapfrontiers.you"), ConfigData.FilterFrontierOwner.You.ordinal()));
        filterOwner.addElement(new GuiRadioListElement(font, new TranslationTextComponent("mapfrontiers.others"), ConfigData.FilterFrontierOwner.Others.ordinal()));
        filterOwner.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierOwner.ordinal());

        buttons.add(new GuiSimpleLabel(font, width / 2 + 170, 214, GuiSimpleLabel.Align.Left,
                new TranslationTextComponent("mapfrontiers.filter_dimension"), GuiColors.SETTINGS_TEXT));

        filterDimension = new GuiScrollBox(width / 2 + 170, 226, 200, height - 296, 16, this);
        filterDimension.addElement(new GuiRadioListElement(font, new TranslationTextComponent("mapfrontiers.all"), "all".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, new TranslationTextComponent("mapfrontiers.current"), "current".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, new StringTextComponent("minecraft:overworld"), "minecraft:overworld".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, new StringTextComponent("minecraft:the_nether"), "minecraft:the_nether".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, new StringTextComponent("minecraft:the_end"), "minecraft:the_end".hashCode()));
        addDimensionsToFilter();
        filterDimension.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierDimension.hashCode());
        if (filterDimension.getSelectedElement() == null) {
            ConfigData.filterFrontierDimension = "all";
            filterDimension.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierDimension.hashCode());
        }

        buttonResetFilters = new GuiSettingsButton(font, width / 2 + 170, 50, 110,
                new TranslationTextComponent("mapfrontiers.reset_filters"), this::buttonPressed);

        buttonCreate = new GuiSettingsButton(font, width / 2 - 295, height - 28, 110,
                new TranslationTextComponent("mapfrontiers.create"), this::buttonPressed);
        buttonInfo = new GuiSettingsButton(font, width / 2 - 175, height - 28, 110,
                new TranslationTextComponent("mapfrontiers.info"), this::buttonPressed);
        buttonDelete = new GuiSettingsButton(font, width / 2 - 55, height - 28, 110,
                new TranslationTextComponent("mapfrontiers.delete"), this::buttonPressed);
        buttonDelete.setTextColors(GuiColors.SETTINGS_BUTTON_TEXT_DELETE, GuiColors.SETTINGS_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonVisible = new GuiSettingsButton(font, width / 2 + 65, height - 28, 110,
                new TranslationTextComponent("mapfrontiers.hide"), this::buttonPressed);
        buttonDone = new GuiSettingsButton(font, width / 2 + 185, height - 28, 110,
                new TranslationTextComponent("gui.done"), this::buttonPressed);

        addButton(frontiers);
        addButton(filterType);
        addButton(filterOwner);
        addButton(filterDimension);
        addButton(buttonResetFilters);
        addButton(buttonCreate);
        addButton(buttonInfo);
        addButton(buttonDelete);
        addButton(buttonVisible);
        addButton(buttonDone);

        updateFrontiers();

        if (fullscreenMap.getSelected() != null) {
            frontiers.selectElementIf((element) -> ((GuiFrontierListElement) element).getFrontier().getId().equals(fullscreenMap.getSelected().getId()));
        }

        updateButtons();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Widget w : buttons) {
            if (w instanceof GuiScrollBox) {
                ((GuiScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedSettingsProfileEvent(UpdatedSettingsProfileEvent event) {
        updateButtons();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onNewFrontierEvent(NewFrontierEvent event) {
        updateFrontiers();
        updateButtons();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedFrontierEvent(UpdatedFrontierEvent event) {
        updateFrontiers();
        updateButtons();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeletedFrontierEvent(DeletedFrontierEvent event) {
        updateFrontiers();
        updateButtons();
    }

    protected void buttonPressed(Button button) {
        if (button == buttonResetFilters) {
            ConfigData.filterFrontierType = ConfigData.FilterFrontierType.All;
            filterType.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierType.ordinal());
            ConfigData.filterFrontierOwner = ConfigData.FilterFrontierOwner.All;
            filterOwner.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierOwner.ordinal());
            ConfigData.filterFrontierDimension = "all";
            filterDimension.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierDimension.hashCode());
            updateFrontiers();
            updateButtons();
        } else if (button == buttonCreate) {
            ForgeHooksClient.popGuiLayer(minecraft);
            ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiNewFrontier(jmAPI));
        } else if (button == buttonInfo) {
            ForgeHooksClient.popGuiLayer(minecraft);
            FrontierOverlay frontier = ((GuiFrontierListElement) frontiers.getSelectedElement()).getFrontier();
            ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiFrontierInfo(jmAPI, frontier,
                    () -> ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiFrontierList(jmAPI, fullscreenMap))));
        } else if (button == buttonDelete) {
            FrontierOverlay frontier = ((GuiFrontierListElement) frontiers.getSelectedElement()).getFrontier();
            FrontiersOverlayManager frontierManager = ClientProxy.getFrontiersOverlayManager(frontier.getPersonal());
            frontierManager.clientDeleteFrontier(frontier);
            frontiers.removeElement(frontiers.getSelectedElement());
            updateButtons();
        } else if (button == buttonVisible) {
            FrontierOverlay frontier = ((GuiFrontierListElement) frontiers.getSelectedElement()).getFrontier();
            frontier.setVisible(!frontier.getVisible());
            updateButtons();
        } else if (button == buttonDone) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }

    @Override
    public void elementClicked(GuiScrollBox scrollBox, GuiScrollBox.ScrollElement element) {
        if (scrollBox == frontiers) {
            FrontierOverlay frontier = ((GuiFrontierListElement) element).getFrontier();
            fullscreenMap.selectFrontier(frontier);
        } else if (scrollBox == filterType) {
            int selected = ((GuiRadioListElement) element).getId();
            ConfigData.filterFrontierType = ConfigData.FilterFrontierType.values()[selected];
            updateFrontiers();
        } else if (scrollBox == filterOwner) {
            int selected = ((GuiRadioListElement) element).getId();
            ConfigData.filterFrontierOwner = ConfigData.FilterFrontierOwner.values()[selected];
            updateFrontiers();
        } else if (scrollBox == filterDimension) {
            int selected = ((GuiRadioListElement) element).getId();
            if (selected == "all".hashCode()) {
                ConfigData.filterFrontierDimension = "all";
            } else if (selected == "current".hashCode()) {
                ConfigData.filterFrontierDimension = "current";
            } else {
                ConfigData.filterFrontierDimension = getDimensionFromHash(selected);
            }
            updateFrontiers();
        }

        updateButtons();
    }

    @Override
    public void elementDelete(GuiScrollBox scrollBox, GuiScrollBox.ScrollElement element) {
        updateButtons();
    }

    @Override
    public void removed() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    private void addDimensionsToFilter() {
        List<WorldData.DimensionProvider> dimensionProviders = WorldData.getDimensionProviders(WaypointStore.INSTANCE.getLoadedDimensions());
        for (WorldData.DimensionProvider dimension : dimensionProviders) {
            if (!dimension.getDimensionId().equals("minecraft:overworld") && !dimension.getDimensionId().equals("minecraft:the_nether") && !dimension.getDimensionId().equals("minecraft:the_end")) {
                filterDimension.addElement(new GuiRadioListElement(font, new StringTextComponent(dimension.getDimensionId()), dimension.getDimensionId().hashCode()));
            }
        }
    }

    private String getDimensionFromHash(int hash) {
        List<WorldData.DimensionProvider> dimensionProviders = WorldData.getDimensionProviders(WaypointStore.INSTANCE.getLoadedDimensions());
        for (WorldData.DimensionProvider dimension : dimensionProviders) {
            if (dimension.getDimensionId().hashCode() == hash) {
                return dimension.getDimensionId();
            }
        }

        return "";
    }

    private void updateFrontiers() {
        frontiers.removeAll();

        if (ConfigData.filterFrontierType == ConfigData.FilterFrontierType.All || ConfigData.filterFrontierType == ConfigData.FilterFrontierType.Personal) {
            for (ArrayList<FrontierOverlay> dimension : ClientProxy.getFrontiersOverlayManager(true).getAllFrontiers().values()) {
                for (FrontierOverlay frontier : dimension) {
                    if (checkFilterOwner(frontier) && checkFilterDimension(frontier)) {
                        frontiers.addElement(new GuiFrontierListElement(font, buttons, frontier));
                    }
                }
            }
        }

        if (ConfigData.filterFrontierType == ConfigData.FilterFrontierType.All || ConfigData.filterFrontierType == ConfigData.FilterFrontierType.Global) {
            for (ArrayList<FrontierOverlay> dimension : ClientProxy.getFrontiersOverlayManager(false).getAllFrontiers().values()) {
                for (FrontierOverlay frontier : dimension) {
                    if (checkFilterOwner(frontier) && checkFilterDimension(frontier)) {
                        frontiers.addElement(new GuiFrontierListElement(font, buttons, frontier));
                    }
                }
            }
        }
    }

    private boolean checkFilterOwner(FrontierOverlay frontier) {
        if (ConfigData.filterFrontierOwner == ConfigData.FilterFrontierOwner.All) {
            return true;
        }

        boolean ownerIsPlayer = frontier.getOwner().equals(new SettingsUser(minecraft.player));

        if (ConfigData.filterFrontierOwner == ConfigData.FilterFrontierOwner.You) {
            return ownerIsPlayer;
        } else {
            return !ownerIsPlayer;
        }
    }

    private boolean checkFilterDimension(FrontierOverlay frontier) {
        if (ConfigData.filterFrontierDimension.equals("all")) {
            return true;
        }

        String dimension = ConfigData.filterFrontierDimension;
        if (dimension.equals("current")) {
            dimension = minecraft.level.dimension().location().toString();
        }

        return frontier.getDimension().location().toString().equals(dimension);
    }

    private void updateButtons() {
        SettingsProfile profile = ClientProxy.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(Minecraft.getInstance().player);
        FrontierData frontier = frontiers.getSelectedElement() == null ? null : ((GuiFrontierListElement) frontiers.getSelectedElement()).getFrontier();
        SettingsProfile.AvailableActions actions = profile.getAvailableActions(frontier, playerUser);


        buttonCreate.visible = actions.canCreate;
        buttonInfo.visible = frontiers.getSelectedElement() != null;
        buttonDelete.visible = actions.canDelete;
        buttonVisible.visible = actions.canUpdate;

        if (frontier != null && frontier.getVisible()) {
            buttonVisible.setMessage(new TranslationTextComponent("mapfrontiers.hide"));
        } else {
            buttonVisible.setMessage(new TranslationTextComponent("mapfrontiers.show"));
        }
    }
}
