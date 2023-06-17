package games.alejandrocoria.mapfrontiers.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.FullscreenMap;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.FrontierListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.RadioListElement;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.platform.Services;
import journeymap.client.api.IClientAPI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class FrontierList extends Screen {
    private final IClientAPI jmAPI;
    private final FullscreenMap fullscreenMap;

    private float scaleFactor;
    private int actualWidth;
    private int actualHeight;

    private ScrollBox frontiers;
    private ScrollBox filterType;
    private ScrollBox filterOwner;
    private ScrollBox filterDimension;
    private SimpleButton buttonResetFilters;
    private SimpleButton buttonCreate;
    private SimpleButton buttonInfo;
    private SimpleButton buttonDelete;
    private SimpleButton buttonVisible;
    private SimpleButton buttonDone;

    private final List<SimpleLabel> labels;

    public FrontierList(IClientAPI jmAPI, FullscreenMap fullscreenMap) {
        super(Component.translatable("mapfrontiers.title_frontiers"));
        this.jmAPI = jmAPI;
        this.fullscreenMap = fullscreenMap;
        labels = new ArrayList<>();

        ClientEventHandler.subscribeDeletedFrontierEvent(this, frontierID -> {
            updateFrontiers();
            updateButtons();
        });

        ClientEventHandler.subscribeNewFrontierEvent(this, (frontierOverlay, playerID) -> {
            updateFrontiers();
            updateButtons();
        });

        ClientEventHandler.subscribeUpdatedFrontierEvent(this, (frontierOverlay, playerID) -> {
            updateFrontiers();
            updateButtons();
        });

        ClientEventHandler.subscribeUpdatedSettingsProfileEvent(this, profile -> {
            updateButtons();
        });
    }

    @Override
    public void init() {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(minecraft, this, 772, 332);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);

        labels.clear();

        frontiers = new ScrollBox(actualWidth / 2 - 300, 50, 450, actualHeight - 100, 24);
        frontiers.setElementDeletedCallback(element -> updateButtons());
        frontiers.setElementClickedCallback(element -> {
            FrontierOverlay frontier = ((FrontierListElement) element).getFrontier();
            fullscreenMap.selectFrontier(frontier);
            updateButtons();
        });

        labels.add(new SimpleLabel(font, actualWidth / 2 + 170, 74, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.filter_type"), ColorConstants.TEXT));

        filterType = new ScrollBox(actualWidth / 2 + 170, 86, 200, 48, 16);
        filterType.addElement(new RadioListElement(font, Config.getTranslatedEnum(Config.FilterFrontierType.All), Config.FilterFrontierType.All.ordinal()));
        filterType.addElement(new RadioListElement(font, Config.getTranslatedEnum(Config.FilterFrontierType.Global), Config.FilterFrontierType.Global.ordinal()));
        filterType.addElement(new RadioListElement(font, Config.getTranslatedEnum(Config.FilterFrontierType.Personal), Config.FilterFrontierType.Personal.ordinal()));
        filterType.selectElementIf((element) -> ((RadioListElement) element).getId() == Config.filterFrontierType.ordinal());
        filterType.setElementDeletedCallback(element -> updateButtons());
        filterType.setElementClickedCallback(element -> {
            int selected = ((RadioListElement) element).getId();
            Config.filterFrontierType = Config.FilterFrontierType.values()[selected];
            updateFrontiers();
            ClientEventHandler.postUpdatedConfigEvent();
            updateButtons();
        });

        labels.add(new SimpleLabel(font, actualWidth / 2 + 170, 144, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.filter_owner"), ColorConstants.TEXT));

        filterOwner = new ScrollBox(actualWidth / 2 + 170, 156, 200, 48, 16);
        filterOwner.addElement(new RadioListElement(font, Config.getTranslatedEnum(Config.FilterFrontierOwner.All), Config.FilterFrontierOwner.All.ordinal()));
        filterOwner.addElement(new RadioListElement(font, Config.getTranslatedEnum(Config.FilterFrontierOwner.You), Config.FilterFrontierOwner.You.ordinal()));
        filterOwner.addElement(new RadioListElement(font, Config.getTranslatedEnum(Config.FilterFrontierOwner.Others), Config.FilterFrontierOwner.Others.ordinal()));
        filterOwner.selectElementIf((element) -> ((RadioListElement) element).getId() == Config.filterFrontierOwner.ordinal());
        filterOwner.setElementDeletedCallback(element -> updateButtons());
        filterOwner.setElementClickedCallback(element -> {
            int selected = ((RadioListElement) element).getId();
            Config.filterFrontierOwner = Config.FilterFrontierOwner.values()[selected];
            updateFrontiers();
            ClientEventHandler.postUpdatedConfigEvent();
            updateButtons();
        });

        labels.add(new SimpleLabel(font, actualWidth / 2 + 170, 214, SimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.filter_dimension"), ColorConstants.TEXT));

        filterDimension = new ScrollBox(actualWidth / 2 + 170, 226, 200, actualHeight - 296, 16);
        filterDimension.addElement(new RadioListElement(font, Component.translatable("mapfrontiers.config.All"), "all".hashCode()));
        filterDimension.addElement(new RadioListElement(font, Component.translatable("mapfrontiers.config.Current"), "current".hashCode()));
        filterDimension.addElement(new RadioListElement(font, Component.literal("minecraft:overworld"), "minecraft:overworld".hashCode()));
        filterDimension.addElement(new RadioListElement(font, Component.literal("minecraft:the_nether"), "minecraft:the_nether".hashCode()));
        filterDimension.addElement(new RadioListElement(font, Component.literal("minecraft:the_end"), "minecraft:the_end".hashCode()));
        addDimensionsToFilter();
        filterDimension.selectElementIf((element) -> ((RadioListElement) element).getId() == Config.filterFrontierDimension.hashCode());
        filterDimension.setElementDeletedCallback(element -> updateButtons());
        filterDimension.setElementClickedCallback(element -> {
            int selected = ((RadioListElement) element).getId();
            if (selected == "all".hashCode()) {
                Config.filterFrontierDimension = "all";
            } else if (selected == "current".hashCode()) {
                Config.filterFrontierDimension = "current";
            } else {
                Config.filterFrontierDimension = getDimensionFromHash(selected);
            }
            updateFrontiers();
            ClientEventHandler.postUpdatedConfigEvent();
            updateButtons();
        });
        if (filterDimension.getSelectedElement() == null) {
            Config.filterFrontierDimension = "all";
            filterDimension.selectElementIf((element) -> ((RadioListElement) element).getId() == Config.filterFrontierDimension.hashCode());
        }

        buttonResetFilters = new SimpleButton(font, actualWidth / 2 + 170, 50, 110,
                Component.translatable("mapfrontiers.reset_filters"), this::buttonPressed);

        buttonCreate = new SimpleButton(font, actualWidth / 2 - 295, actualHeight - 28, 110,
                Component.translatable("mapfrontiers.create"), this::buttonPressed);
        buttonInfo = new SimpleButton(font, actualWidth / 2 - 175, actualHeight - 28, 110,
                Component.translatable("mapfrontiers.info"), this::buttonPressed);
        buttonDelete = new SimpleButton(font, actualWidth / 2 - 55, actualHeight - 28, 110,
                Component.translatable("mapfrontiers.delete"), this::buttonPressed);
        buttonDelete.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_DELETE, ColorConstants.SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT);
        buttonVisible = new SimpleButton(font, actualWidth / 2 + 65, actualHeight - 28, 110,
                Component.translatable("mapfrontiers.hide"), this::buttonPressed);
        buttonDone = new SimpleButton(font, actualWidth / 2 + 185, actualHeight - 28, 110,
                Component.translatable("gui.done"), this::buttonPressed);

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
            frontiers.selectElementIf((element) -> ((FrontierListElement) element).getFrontier().getId().equals(fullscreenMap.getSelected().getId()));
        }

        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);

        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (scaleFactor != 1.f) {
            graphics.pose().pushPose();
            graphics.pose().scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

        graphics.drawCenteredString(font, title, this.actualWidth / 2, 8, ColorConstants.WHITE);
        super.render(graphics, mouseX, mouseY, partialTicks);

        for (SimpleLabel label : labels) {
            if (label.visible) {
                label.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        if (scaleFactor != 1.f) {
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return super.mouseClicked(mouseX * scaleFactor, mouseY * scaleFactor, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        for (GuiEventListener w : children()) {
            if (w instanceof ScrollBox) {
                ((ScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX * scaleFactor, mouseY * scaleFactor, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX * scaleFactor, mouseY * scaleFactor, button, dragX * scaleFactor, dragY * scaleFactor);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonResetFilters) {
            Config.filterFrontierType = Config.FilterFrontierType.All;
            filterType.selectElementIf((element) -> ((RadioListElement) element).getId() == Config.filterFrontierType.ordinal());
            Config.filterFrontierOwner = Config.FilterFrontierOwner.All;
            filterOwner.selectElementIf((element) -> ((RadioListElement) element).getId() == Config.filterFrontierOwner.ordinal());
            Config.filterFrontierDimension = "all";
            filterDimension.selectElementIf((element) -> ((RadioListElement) element).getId() == Config.filterFrontierDimension.hashCode());
            updateFrontiers();
            updateButtons();
        } else if (button == buttonCreate) {
            Services.PLATFORM.popGuiLayer();
            Services.PLATFORM.pushGuiLayer(new NewFrontier(jmAPI));
        } else if (button == buttonInfo) {
            Services.PLATFORM.popGuiLayer();
            FrontierOverlay frontier = ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
            Services.PLATFORM.pushGuiLayer(new FrontierInfo(jmAPI, frontier,
                    () -> Services.PLATFORM.pushGuiLayer(new FrontierList(jmAPI, fullscreenMap))));
        } else if (button == buttonDelete) {
            FrontierOverlay frontier = ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
            FrontiersOverlayManager frontierManager = MapFrontiersClient.getFrontiersOverlayManager(frontier.getPersonal());
            frontierManager.clientDeleteFrontier(frontier);
            frontiers.removeElement(frontiers.getSelectedElement());
            updateButtons();
        } else if (button == buttonVisible) {
            FrontierOverlay frontier = ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
            frontier.setVisible(!frontier.getVisible());
            updateButtons();
        } else if (button == buttonDone) {
            Services.PLATFORM.popGuiLayer();
        }
    }

    @Override
    public void removed() {
        ClientEventHandler.unsuscribeAllEvents(this);
    }

    private void addDimensionsToFilter() {
        List<String> dimensions = Services.JOURNEYMAP.getDimensionList();
        for (String dimension : dimensions) {
            if (!dimension.equals("minecraft:overworld") && !dimension.equals("minecraft:the_nether") && !dimension.equals("minecraft:the_end")) {
                filterDimension.addElement(new RadioListElement(font, Component.literal(dimension), dimension.hashCode()));
            }
        }
    }

    private String getDimensionFromHash(int hash) {
        List<String> dimensions = Services.JOURNEYMAP.getDimensionList();
        for (String dimension : dimensions) {
            if (dimension.hashCode() == hash) {
                return dimension;
            }
        }

        return "";
    }

    private void updateFrontiers() {
        FrontierData selectedFrontier = frontiers.getSelectedElement() == null ? null : ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
        UUID frontierID = selectedFrontier == null ? null : selectedFrontier.getId();

        frontiers.removeAll();

        if (Config.filterFrontierType == Config.FilterFrontierType.All || Config.filterFrontierType == Config.FilterFrontierType.Personal) {
            for (ArrayList<FrontierOverlay> dimension : MapFrontiersClient.getFrontiersOverlayManager(true).getAllFrontiers().values()) {
                for (FrontierOverlay frontier : dimension) {
                    if (checkFilterOwner(frontier) && checkFilterDimension(frontier)) {
                        frontiers.addElement(new FrontierListElement(font, frontier));
                    }
                }
            }
        }

        if (Config.filterFrontierType == Config.FilterFrontierType.All || Config.filterFrontierType == Config.FilterFrontierType.Global) {
            for (ArrayList<FrontierOverlay> dimension : MapFrontiersClient.getFrontiersOverlayManager(false).getAllFrontiers().values()) {
                for (FrontierOverlay frontier : dimension) {
                    if (checkFilterOwner(frontier) && checkFilterDimension(frontier)) {
                        frontiers.addElement(new FrontierListElement(font, frontier));
                    }
                }
            }
        }

        if (frontierID != null) {
            frontiers.selectElementIf((element) -> ((FrontierListElement) element).getFrontier().getId().equals(frontierID));
        }
    }

    private boolean checkFilterOwner(FrontierOverlay frontier) {
        if (Config.filterFrontierOwner == Config.FilterFrontierOwner.All) {
            return true;
        }

        boolean ownerIsPlayer = minecraft.player != null && frontier.getOwner().equals(new SettingsUser(minecraft.player));

        if (Config.filterFrontierOwner == Config.FilterFrontierOwner.You) {
            return ownerIsPlayer;
        } else {
            return !ownerIsPlayer;
        }
    }

    private boolean checkFilterDimension(FrontierOverlay frontier) {
        if (Config.filterFrontierDimension.equals("all")) {
            return true;
        }

        String dimension = Config.filterFrontierDimension;
        if (dimension.equals("current") && minecraft.level != null) {
            dimension = minecraft.level.dimension().location().toString();
        }

        return frontier.getDimension().location().toString().equals(dimension);
    }

    private void updateButtons() {
        if (minecraft.player == null) {
            return;
        }

        SettingsProfile profile = MapFrontiersClient.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(minecraft.player);
        FrontierData frontier = frontiers.getSelectedElement() == null ? null : ((FrontierListElement) frontiers.getSelectedElement()).getFrontier();
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontier, playerUser);

        buttonInfo.visible = frontiers.getSelectedElement() != null;
        buttonDelete.visible = actions.canDelete;
        buttonVisible.visible = actions.canUpdate;

        if (frontier != null && frontier.getVisible()) {
            buttonVisible.setMessage(Component.translatable("mapfrontiers.hide"));
        } else {
            buttonVisible.setMessage(Component.translatable("mapfrontiers.show"));
        }
    }
}
