package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import journeymap.client.api.IClientAPI;
import journeymap.client.data.WorldData;
import journeymap.client.ui.ScreenLayerManager;
import journeymap.client.waypoint.WaypointStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
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
        super(TextComponent.EMPTY);
        this.jmAPI = jmAPI;
        this.fullscreenMap = fullscreenMap;

        ClientProxy.subscribeDeletedFrontierEvent(this, frontierID -> {
            updateFrontiers();
            updateButtons();
        });

        ClientProxy.subscribeNewFrontierEvent(this, (frontierOverlay, playerID) -> {
            updateFrontiers();
            updateButtons();
        });

        ClientProxy.subscribeUpdatedFrontierEvent(this, (frontierOverlay, playerID) -> {
            updateFrontiers();
            updateButtons();
        });

        ClientProxy.subscribeUpdatedSettingsProfileEvent(this, profile -> {
            updateButtons();
        });
    }

    @Override
    public void init() {
        Component title = new TranslatableComponent("mapfrontiers.title_frontiers");
        addRenderableOnly(new GuiSimpleLabel(font, width / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        frontiers = new GuiScrollBox(width / 2 - 300, 50, 450, height - 100, 24, this);

        addRenderableOnly(new GuiSimpleLabel(font, width / 2 + 170, 74, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("mapfrontiers.filter_type"), GuiColors.SETTINGS_TEXT));

        filterType = new GuiScrollBox(width / 2 + 170, 86, 200, 48, 16, this);
        filterType.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.All), ConfigData.FilterFrontierType.All.ordinal()));
        filterType.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.Global), ConfigData.FilterFrontierType.Global.ordinal()));
        filterType.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.Personal), ConfigData.FilterFrontierType.Personal.ordinal()));
        filterType.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierType.ordinal());

        addRenderableOnly(new GuiSimpleLabel(font, width / 2 + 170, 144, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("mapfrontiers.filter_owner"), GuiColors.SETTINGS_TEXT));

        filterOwner = new GuiScrollBox(width / 2 + 170, 156, 200, 48, 16, this);
        filterOwner.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierOwner.All), ConfigData.FilterFrontierOwner.All.ordinal()));
        filterOwner.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierOwner.You), ConfigData.FilterFrontierOwner.You.ordinal()));
        filterOwner.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierOwner.Others), ConfigData.FilterFrontierOwner.Others.ordinal()));
        filterOwner.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierOwner.ordinal());

        addRenderableOnly(new GuiSimpleLabel(font, width / 2 + 170, 214, GuiSimpleLabel.Align.Left,
                new TranslatableComponent("mapfrontiers.filter_dimension"), GuiColors.SETTINGS_TEXT));

        filterDimension = new GuiScrollBox(width / 2 + 170, 226, 200, height - 296, 16, this);
        filterDimension.addElement(new GuiRadioListElement(font, new TranslatableComponent("mapfrontiers.config.All"), "all".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, new TranslatableComponent("mapfrontiers.config.Current"), "current".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, new TextComponent("minecraft:overworld"), "minecraft:overworld".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, new TextComponent("minecraft:the_nether"), "minecraft:the_nether".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, new TextComponent("minecraft:the_end"), "minecraft:the_end".hashCode()));
        addDimensionsToFilter();
        filterDimension.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierDimension.hashCode());
        if (filterDimension.getSelectedElement() == null) {
            ConfigData.filterFrontierDimension = "all";
            filterDimension.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierDimension.hashCode());
        }

        buttonResetFilters = new GuiSettingsButton(font, width / 2 + 170, 50, 110,
                new TranslatableComponent("mapfrontiers.reset_filters"), this::buttonPressed);

        buttonCreate = new GuiSettingsButton(font, width / 2 - 295, height - 28, 110,
                new TranslatableComponent("mapfrontiers.create"), this::buttonPressed);
        buttonInfo = new GuiSettingsButton(font, width / 2 - 175, height - 28, 110,
                new TranslatableComponent("mapfrontiers.info"), this::buttonPressed);
        buttonDelete = new GuiSettingsButton(font, width / 2 - 55, height - 28, 110,
                new TranslatableComponent("mapfrontiers.delete"), this::buttonPressed);
        buttonDelete.setTextColors(GuiColors.SETTINGS_BUTTON_TEXT_DELETE, GuiColors.SETTINGS_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonVisible = new GuiSettingsButton(font, width / 2 + 65, height - 28, 110,
                new TranslatableComponent("mapfrontiers.hide"), this::buttonPressed);
        buttonDone = new GuiSettingsButton(font, width / 2 + 185, height - 28, 110,
                new TranslatableComponent("gui.done"), this::buttonPressed);

        addRenderableWidget(frontiers);
        addRenderableWidget(filterType);
        addRenderableWidget(filterOwner);
        addRenderableWidget(filterDimension);
        addRenderableWidget(buttonResetFilters);
        addRenderableWidget(buttonCreate);
        addRenderableWidget(buttonInfo);
        addRenderableWidget(buttonDelete);
        addRenderableWidget(buttonVisible);
        addRenderableWidget(buttonDone);

        updateFrontiers();

        if (fullscreenMap.getSelected() != null) {
            frontiers.selectElementIf((element) -> ((GuiFrontierListElement) element).getFrontier().getId().equals(fullscreenMap.getSelected().getId()));
        }

        updateButtons();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack, 0);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (GuiEventListener w : children()) {
            if (w instanceof GuiScrollBox) {
                ((GuiScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
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
            ScreenLayerManager.popLayer();
            ScreenLayerManager.pushLayer(new GuiNewFrontier(jmAPI));
        } else if (button == buttonInfo) {
            ScreenLayerManager.popLayer();
            FrontierOverlay frontier = ((GuiFrontierListElement) frontiers.getSelectedElement()).getFrontier();
            ScreenLayerManager.pushLayer(new GuiFrontierInfo(jmAPI, frontier,
                    () -> ScreenLayerManager.pushLayer(new GuiFrontierList(jmAPI, fullscreenMap))));
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
            ScreenLayerManager.popLayer();
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
        ClientProxy.unsuscribeAllEvents(this);
    }

    private void addDimensionsToFilter() {
        List<WorldData.DimensionProvider> dimensionProviders = WorldData.getDimensionProviders(WaypointStore.INSTANCE.getLoadedDimensions());
        for (WorldData.DimensionProvider dimension : dimensionProviders) {
            if (!dimension.getDimensionId().equals("minecraft:overworld") && !dimension.getDimensionId().equals("minecraft:the_nether") && !dimension.getDimensionId().equals("minecraft:the_end")) {
                filterDimension.addElement(new GuiRadioListElement(font, new TextComponent(dimension.getDimensionId()), dimension.getDimensionId().hashCode()));
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
        FrontierData selectedFrontier = frontiers.getSelectedElement() == null ? null : ((GuiFrontierListElement) frontiers.getSelectedElement()).getFrontier();
        UUID frontierID = selectedFrontier == null ? null : selectedFrontier.getId();

        frontiers.removeAll();

        if (ConfigData.filterFrontierType == ConfigData.FilterFrontierType.All || ConfigData.filterFrontierType == ConfigData.FilterFrontierType.Personal) {
            for (ArrayList<FrontierOverlay> dimension : ClientProxy.getFrontiersOverlayManager(true).getAllFrontiers().values()) {
                for (FrontierOverlay frontier : dimension) {
                    if (checkFilterOwner(frontier) && checkFilterDimension(frontier)) {
                        frontiers.addElement(new GuiFrontierListElement(font, (List<GuiEventListener>) children(), frontier));
                    }
                }
            }
        }

        if (ConfigData.filterFrontierType == ConfigData.FilterFrontierType.All || ConfigData.filterFrontierType == ConfigData.FilterFrontierType.Global) {
            for (ArrayList<FrontierOverlay> dimension : ClientProxy.getFrontiersOverlayManager(false).getAllFrontiers().values()) {
                for (FrontierOverlay frontier : dimension) {
                    if (checkFilterOwner(frontier) && checkFilterDimension(frontier)) {
                        frontiers.addElement(new GuiFrontierListElement(font, (List<GuiEventListener>) children(), frontier));
                    }
                }
            }
        }

        if (frontierID != null) {
            frontiers.selectElementIf((element) -> ((GuiFrontierListElement) element).getFrontier().getId().equals(frontierID));
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
            buttonVisible.setMessage(new TranslatableComponent("mapfrontiers.hide"));
        } else {
            buttonVisible.setMessage(new TranslatableComponent("mapfrontiers.show"));
        }
    }
}
