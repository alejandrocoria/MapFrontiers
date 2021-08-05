package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import journeymap.client.JourneymapClient;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.Sounds;
import games.alejandrocoria.mapfrontiers.client.util.StringHelper;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import journeymap.client.api.display.Context;
import journeymap.client.api.impl.ClientAPI;
import journeymap.client.data.DataCache;
import journeymap.client.model.EntityDTO;
import journeymap.client.model.MapState;
import journeymap.client.model.MapType;
import journeymap.client.model.MapType.Name;
import journeymap.client.properties.MiniMapProperties;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.map.GridRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFrontierBook extends Screen implements TextColorBox.TextColorBoxResponder, TextBox.TextBoxResponder {
    private enum DeleteBookmarkPosition {
        Hidden, Normal, Open
    }

    private static final int bookImageHeight = 182;
    private static final int bookImageWidth = 312;
    private static final int bookTextureSize = 512;

    private final FrontiersOverlayManager frontiersOverlayManager;
    private int lastFrontierHash;
    private int currPage = 0;
    private final ResourceKey<Level> currentDimension;
    private final ResourceKey<Level> dimension;
    private final ResourceLocation bookPageTexture;
    private int frontiersPageStart;
    private final ItemStack heldBanner;

    private GuiBookPages bookPages;
    private GuiButtonIcon buttonClose;
    private GuiButtonIcon buttonNextPage;
    private GuiButtonIcon buttonPreviousPage;
    private GuiButtonIcon buttonBackToIndex;
    private GuiButtonIcon buttonNextVertex;
    private GuiButtonIcon buttonPreviousVertex;
    private GuiBookmark buttonNew;
    private GuiBookmark buttonDelete;
    private DeleteBookmarkPosition deleteBookmarkPosition;
    private GuiBookButton buttonDeleteConfirm;
    private GuiBookTag buttonNameVisible;
    private GuiBookTag buttonAddVertex;
    private GuiBookTag buttonRemoveVertex;
    private GuiBookTag buttonFinish;
    private GuiBookTag buttonBanner;
    private GuiButtonIcon buttonSliceUp;
    private GuiButtonIcon buttonSliceDown;
    private GuiSliderSlice sliderSlice;
    private GuiBookButton buttonEditShareSettings;

    private TextColorBox textRed;
    private TextColorBox textGreen;
    private TextColorBox textBlue;
    private TextBox textName1;
    private TextBox textName2;
    private GuiSimpleLabel labelFrontiernumber;

    private final List<IndexEntryButton> indexEntryButtons;
    private final List<GuiSimpleLabel> labels;
    private final boolean personal;

    private static MapState state;
    private static GridRenderer gridRenderer;
    private static MiniMapProperties miniMapProperties;
    private static int zoom = 1;

    public GuiFrontierBook(FrontiersOverlayManager frontiersOverlayManager, boolean personal, ResourceKey<Level> currentDimension,
            ResourceKey<Level> dimension, @Nullable ItemStack heldBanner) {
        super(TextComponent.EMPTY);
        this.frontiersOverlayManager = frontiersOverlayManager;
        this.currentDimension = currentDimension;
        this.dimension = dimension;

        this.heldBanner = heldBanner;

        bookPageTexture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/gui.png");
        indexEntryButtons = new ArrayList<>();
        labels = new ArrayList<>();

        this.personal = personal;

        state = new MapState();
        gridRenderer = new GridRenderer(Context.UI.Minimap);
        gridRenderer.setGridSize(3);
        miniMapProperties = new MiniMapProperties(777);
    }

    @Override
    public void init() {
        Sounds.playSoundOpenBook();

        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        int offsetFromScreenLeft = (width - bookImageWidth) / 2;
        int offsetFromScreenTop = (height - bookImageHeight) / 2;
        int rightPageCornerX = width / 2;
        int rightPageCornerY = offsetFromScreenTop;

        bookPages = new GuiBookPages(offsetFromScreenLeft, offsetFromScreenTop, 0, bookImageHeight, bookImageWidth,
                bookImageHeight, bookTextureSize, bookTextureSize, bookPageTexture);

        buttonClose = new GuiButtonIcon(offsetFromScreenLeft + bookImageWidth - 8, offsetFromScreenTop - 5, 13, 13, 471, 64, 23,
                bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonNextPage = new GuiButtonIcon(offsetFromScreenLeft + bookImageWidth - 27, offsetFromScreenTop + bookImageHeight - 18,
                21, 13, 468, 0, 23, bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonPreviousPage = new GuiButtonIcon(offsetFromScreenLeft + 6, offsetFromScreenTop + bookImageHeight - 18, 21, 13, 468,
                13, 23, bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonBackToIndex = new GuiButtonIcon(rightPageCornerX - 8, rightPageCornerY + bookImageHeight - 18, 16, 13, 470, 51, 23,
                bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonNextVertex = new GuiButtonIcon(offsetFromScreenLeft + bookImageWidth / 2 - 25, offsetFromScreenTop + 148, 9, 11,
                473, 78, 23, bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonPreviousVertex = new GuiButtonIcon(offsetFromScreenLeft + 16, offsetFromScreenTop + 148, 9, 11, 473, 90, 23,
                bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonNew = new GuiBookmark(rightPageCornerX + 6, rightPageCornerY - 18, 21, 18,
                new TranslatableComponent("mapfrontiers.new"), bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonDelete = new GuiBookmark(rightPageCornerX + 62, rightPageCornerY + 4, 44, 18,
                new TranslatableComponent("mapfrontiers.delete"), bookPageTexture, bookTextureSize, this::buttonPressed);
        buttonDelete.addYPosition(rightPageCornerY - 18);
        buttonDelete.addYPosition(rightPageCornerY - 40);
        deleteBookmarkPosition = DeleteBookmarkPosition.Hidden;

        buttonDeleteConfirm = new GuiBookButton(font, buttonDelete.x, buttonDelete.y + 26, buttonDelete.getWidth(),
                new TranslatableComponent("mapfrontiers.confirm_delete"), false, true, this::buttonPressed);
        buttonDelete.addWidget(buttonDeleteConfirm);

        int leftTagsX = offsetFromScreenLeft + 3;
        int leftTagsWidth = StringHelper.getMaxWidth(font, I18n.get("mapfrontiers.show_name"),
                I18n.get("mapfrontiers.hide_name"), I18n.get("mapfrontiers.finish"), I18n.get("mapfrontiers.reopen"),
                I18n.get("mapfrontiers.add_vertex"), I18n.get("mapfrontiers.remove_vertex"));
        leftTagsWidth += 12;

        int rightTagsWidth = StringHelper.getMaxWidth(font, I18n.get("mapfrontiers.assign_banner") + " !",
                I18n.get("mapfrontiers.remove_banner"));
        rightTagsWidth += 12;
        rightTagsWidth = Math.max(rightTagsWidth, 89);

        buttonNameVisible = new GuiBookTag(leftTagsX, offsetFromScreenTop + 12, leftTagsWidth, true, TextComponent.EMPTY,
                bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonAddVertex = new GuiBookTag(leftTagsX, offsetFromScreenTop + 32, leftTagsWidth, true,
                new TranslatableComponent("mapfrontiers.add_vertex"), bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonRemoveVertex = new GuiBookTag(leftTagsX, offsetFromScreenTop + 52, leftTagsWidth, true,
                new TranslatableComponent("mapfrontiers.remove_vertex"), bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonFinish = new GuiBookTag(leftTagsX, offsetFromScreenTop + 72, leftTagsWidth, true, TextComponent.EMPTY,
                bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonBanner = new GuiBookTag(offsetFromScreenLeft + bookImageWidth - 3, offsetFromScreenTop + 12, rightTagsWidth, false,
                TextComponent.EMPTY, bookPageTexture, bookTextureSize, this::buttonPressed);

        buttonSliceUp = new GuiButtonIcon(offsetFromScreenLeft + bookImageWidth / 2 - 21,
                offsetFromScreenTop + bookImageHeight / 2 - 45, 13, 9, 471, 102, 23, bookPageTexture,
                bookTextureSize, this::buttonPressed);

        buttonSliceDown = new GuiButtonIcon(offsetFromScreenLeft + bookImageWidth / 2 - 21,
                offsetFromScreenTop + bookImageHeight / 2 + 36, 13, 9, 471, 111, 23, bookPageTexture,
                bookTextureSize, this::buttonPressed);

        sliderSlice = new GuiSliderSlice(offsetFromScreenLeft + bookImageWidth / 2 - 21,
                offsetFromScreenTop + bookImageHeight / 2 - 35, bookPageTexture, bookTextureSize, this::buttonPressed);

        Component shareSettingsText = new TranslatableComponent("mapfrontiers.share_settings");
        buttonEditShareSettings = new GuiBookButton(font, rightPageCornerX + 12, rightPageCornerY + 88,
                font.width(shareSettingsText.getString()) + 5, shareSettingsText, true, false, this::buttonPressed);

        int textColorX = rightPageCornerX + 44;
        int textColorY = rightPageCornerY + 18;

        textRed = new TextColorBox(255, font, textColorX, textColorY);
        textRed.setResponder(this);

        textGreen = new TextColorBox(255, font, textColorX + 26, textColorY);
        textGreen.setResponder(this);

        textBlue = new TextColorBox(255, font, textColorX + 52, textColorY);
        textBlue.setResponder(this);

        int textNameX = offsetFromScreenLeft + bookImageWidth / 4 - 56;
        int textNameY = offsetFromScreenTop + 10;
        String defaultText = "Add name";
        textName1 = new TextBox(font, textNameX, textNameY, 113, defaultText);
        textName1.setMaxLength(17);
        textName1.setResponder(this);
        textName2 = new TextBox(font, textNameX, textNameY + 14, 113, defaultText);
        textName2.setMaxLength(17);
        textName2.setResponder(this);

        addRenderableWidget(buttonDelete);
        addRenderableWidget(buttonDeleteConfirm);
        addRenderableWidget(bookPages);
        addRenderableWidget(buttonClose);
        addRenderableWidget(buttonNextPage);
        addRenderableWidget(buttonPreviousPage);
        addRenderableWidget(buttonBackToIndex);
        addRenderableWidget(buttonNextVertex);
        addRenderableWidget(buttonPreviousVertex);
        addRenderableWidget(buttonNew);
        addRenderableWidget(buttonNameVisible);
        addRenderableWidget(buttonAddVertex);
        addRenderableWidget(buttonRemoveVertex);
        addRenderableWidget(buttonFinish);
        addRenderableWidget(buttonBanner);
        addRenderableWidget(buttonSliceUp);
        addRenderableWidget(buttonSliceDown);
        addRenderableWidget(sliderSlice);
        addRenderableWidget(buttonEditShareSettings);
        addRenderableWidget(textRed);
        addRenderableWidget(textGreen);
        addRenderableWidget(textBlue);
        addRenderableWidget(textName1);
        addRenderableWidget(textName2);

        updateIndexEntries();

        int selected = frontiersOverlayManager.getFrontierIndexSelected(dimension);
        if (selected < 0) {
            changePage(0, false);
        } else {
            changePage(selected + frontiersPageStart, false);
        }
    }

    @Override
    public void tick() {
        if (isInFrontierPage()) {
            textRed.tick();
            textGreen.tick();
            textBlue.tick();
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (personal) {
            RenderSystem.setShaderColor(0.f, 0.5f, 1.f, 1.f);
        } else {
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.f);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, bookPageTexture);
        int offsetFromScreenLeft = (width - bookImageWidth) / 2;
        int offsetFromScreenTop = (height - bookImageHeight) / 2;

        blit(matrixStack, offsetFromScreenLeft, offsetFromScreenTop, 0, 0, bookImageWidth, bookImageHeight, bookTextureSize,
                bookTextureSize);

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        int rightPageCornerX = width / 2;
        int rightPageCornerY = (height - bookImageHeight) / 2;

        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();

            fill(matrixStack, rightPageCornerX + 123, rightPageCornerY + 14, rightPageCornerX + 143, rightPageCornerY + 33,
                    GuiColors.COLOR_INDICATOR_BORDER);
            fill(matrixStack, rightPageCornerX + 125, rightPageCornerY + 16, rightPageCornerX + 141, rightPageCornerY + 31,
                    frontier.getColor() | 0xff000000);

            if (frontier.getVertexCount() > 0) {
                drawMap(matrixStack, mouseX, mouseY);
            }

            if (frontier.hasBanner()) {
                frontier.renderBanner(minecraft, matrixStack, offsetFromScreenLeft + bookImageWidth + 6, offsetFromScreenTop + 27,
                        3);
            }
        }

        for (GuiSimpleLabel label : labels) {
            label.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        if (buttonClose.visible && buttonClose.isHovered()) {
            renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.close"), mouseX, mouseY);
        }

        if (buttonNextPage.visible && buttonNextPage.isHovered()) {
            renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.next_page"), mouseX, mouseY);
        }

        if (buttonPreviousPage.visible && buttonPreviousPage.isHovered()) {
            renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.previous_page"), mouseX, mouseY);
        }

        if (buttonBackToIndex.visible && buttonBackToIndex.isHovered()) {
            renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.back_to_index"), mouseX, mouseY);
        }

        if (buttonNextVertex.visible && buttonNextVertex.isHovered()) {
            renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.next_vertex"), mouseX, mouseY);
        }

        if (buttonPreviousVertex.visible && buttonPreviousVertex.isHovered()) {
            renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.previous_vertex"), mouseX, mouseY);
        }

        if (buttonSliceUp.visible && buttonSliceUp.isHovered()) {
            int slice = sliderSlice.getSlice();
            renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.vertical_chunk", slice), mouseX, mouseY);
        }

        if (buttonSliceDown.visible && buttonSliceDown.isHovered()) {
            int slice = sliderSlice.getSlice();
            if (slice == FrontierOverlay.SurfaceSlice) {
                renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.vertical_chunk_surface"), mouseX, mouseY);
            } else {
                renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.vertical_chunk", slice), mouseX, mouseY);
            }
        }

        if (sliderSlice.visible && sliderSlice.isHovered()) {
            int slice = sliderSlice.getSlice();
            if (slice == FrontierOverlay.SurfaceSlice) {
                renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.vertical_chunk_surface"), mouseX, mouseY);
            } else {
                renderTooltip(matrixStack, new TranslatableComponent("mapfrontiers.vertical_chunk", slice), mouseX, mouseY);
            }
        }

        if (buttonNameVisible.visible && buttonNameVisible.isHovered()) {
            TextComponent prefix = new TextComponent(GuiColors.WARNING + "! " + ChatFormatting.RESET);
            if (ConfigData.nameVisibility == ConfigData.NameVisibility.Show) {
                renderTooltip(matrixStack, prefix.append(new TranslatableComponent("mapfrontiers.show_name_warn")), mouseX, mouseY);
            } else if (ConfigData.nameVisibility == ConfigData.NameVisibility.Hide) {
                renderTooltip(matrixStack, prefix.append(new TranslatableComponent("mapfrontiers.hide_name_warn")), mouseX, mouseY);
            }
        }

        if (buttonBanner.visible && buttonBanner.isHovered()) {
            FrontierOverlay frontier = getCurrentFrontier();
            if (!frontier.hasBanner() && heldBanner == null) {
                TextComponent prefix = new TextComponent(GuiColors.WARNING + "! " + ChatFormatting.RESET);
                renderTooltip(matrixStack, prefix.append(new TranslatableComponent("mapfrontiers.assign_banner_warn")),
                        mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_E && !(getFocused() instanceof EditBox)) {
            onClose();
            return true;
        } else {
            return super.keyPressed(key, value, modifier);
        }
    }

    @Override
    public void updatedValue(TextColorBox textBox, int value) {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();

            if (textRed == textBox) {
                int newColor = (frontier.getColor() & 0xff00ffff) | (value << 16);
                if (newColor != frontier.getColor()) {
                    frontier.setColor(newColor);
                    updateIndexEntries();
                    updateButtonsVisibility();
                    sendChangesToServer();
                }
            } else if (textGreen == textBox) {
                int newColor = (frontier.getColor() & 0xffff00ff) | (value << 8);
                if (newColor != frontier.getColor()) {
                    frontier.setColor(newColor);
                    updateIndexEntries();
                    updateButtonsVisibility();
                    sendChangesToServer();
                }
            } else if (textBlue == textBox) {
                int newColor = (frontier.getColor() & 0xffffff00) | value;
                if (newColor != frontier.getColor()) {
                    frontier.setColor(newColor);
                    updateIndexEntries();
                    updateButtonsVisibility();
                    sendChangesToServer();
                }
            }
        }
    }

    @Override
    public void updatedValue(TextBox textBox, String value) {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();

            if (textName1 == textBox) {
                if (!frontier.getName1().equals(value)) {
                    frontier.setName1(value);
                    updateIndexEntries();
                    updateButtonsVisibility();
                }
            } else if (textName2 == textBox) {
                if (!frontier.getName2().equals(value)) {
                    frontier.setName2(value);
                    updateIndexEntries();
                    updateButtonsVisibility();
                }
            }
        }
    }

    @Override
    public void lostFocus(TextBox textBox, String value) {
        sendChangesToServer();
    }

    protected void buttonPressed(Button button) {
        if (button == buttonClose) {
            minecraft.setScreen(null);
        } else if (button == buttonNextPage) {
            changePage(currPage + 1);
        } else if (button == buttonPreviousPage) {
            changePage(currPage - 1);
        } else if (button == buttonBackToIndex) {
            changePage(0);
        } else if (button == buttonNextVertex) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.selectNextVertex();
            resetLabels();
            updateButtonsVisibility();
        } else if (button == buttonPreviousVertex) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.selectPreviousVertex();
            resetLabels();
            updateButtonsVisibility();
        } else if (button == buttonNew) {
            frontiersOverlayManager.clientCreateNewfrontier(dimension);
        } else if (button == buttonDelete) {
            if (deleteBookmarkPosition == DeleteBookmarkPosition.Normal) {
                changeDeleteBookmarkPosition(DeleteBookmarkPosition.Open);
            } else if (deleteBookmarkPosition == DeleteBookmarkPosition.Open) {
                changeDeleteBookmarkPosition(DeleteBookmarkPosition.Normal);
            }
        } else if (button == buttonDeleteConfirm) {
            if (deleteBookmarkPosition == DeleteBookmarkPosition.Open) {
                changeDeleteBookmarkPosition(DeleteBookmarkPosition.Normal);
                frontiersOverlayManager.clientDeleteFrontier(dimension, getCurrentFrontierIndex());
            }
        } else if (button == buttonNameVisible) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setNameVisible(!frontier.getNameVisible());
            resetTextName();
            sendChangesToServer();
        } else if (button == buttonAddVertex) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.addVertex(minecraft.player.blockPosition());
            resetLabels();
            updateButtonsVisibility();
            updateGridRenderer();
            sendChangesToServer();
        } else if (button == buttonRemoveVertex) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.removeSelectedVertex();
            resetLabels();
            resetFinishButton();
            updateButtonsVisibility();
            updateGridRenderer();
            sendChangesToServer();
        } else if (button == buttonFinish) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setClosed(!frontier.getClosed());
            resetFinishButton();
            updateButtonsVisibility();
            updateGridRenderer();
            sendChangesToServer();
        } else if (button == buttonBanner) {
            FrontierOverlay frontier = getCurrentFrontier();
            if (!frontier.hasBanner()) {
                if (heldBanner != null) {
                    frontier.setBanner(heldBanner);
                }
            } else {
                frontier.setBanner(null);
            }
            resetBannerButton();
            sendChangesToServer();
        } else if (button == buttonSliceUp) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setMapSlice(frontier.getMapSlice() + 1);
            resetSliceSlider();
            updateButtonsVisibility();
            updateGridRenderer();
            sendChangesToServer();
        } else if (button == buttonSliceDown) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setMapSlice(frontier.getMapSlice() - 1);
            resetSliceSlider();
            updateButtonsVisibility();
            updateGridRenderer();
            sendChangesToServer();
        } else if (button == sliderSlice) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setMapSlice(sliderSlice.getSlice());
            updateButtonsVisibility();
            updateGridRenderer();
            sendChangesToServer();
        } else if (button == buttonEditShareSettings) {
            FrontierOverlay frontier = getCurrentFrontier();
            minecraft.setScreen(new GuiShareSettings(this, frontiersOverlayManager, frontier));
        } else {
            for (IndexEntryButton indexButton : indexEntryButtons) {
                if (button == indexButton) {
                    changePage(indexButton.getPage());
                    break;
                }
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
        sendChangesToServer();
        super.onClose();
    }

    public void newFrontierMessage(FrontierOverlay frontierOverlay, int playerID) {
        if (frontierOverlay.getDimension() != dimension || frontierOverlay.getPersonal() != personal) {
            return;
        }

        if (playerID == -1 || minecraft.player.getId() == playerID) {
            int index = frontiersOverlayManager.getFrontierIndex(frontierOverlay);
            if (index >= 0) {
                updateIndexEntries();
                changePage(frontiersPageStart + index);
            }
        } else {
            if (isInFrontierPage()) {
                int currentFrontierIndex = getCurrentFrontierIndex();
                updateIndexEntries();
                currPage = frontiersPageStart + currentFrontierIndex;
                updatePageControls();
            } else {
                updateIndexEntries();
                reloadPage(false);
            }
        }
    }

    public void updateFrontierMessage(FrontierOverlay frontierOverlay, int playerID) {
        if (frontierOverlay.getDimension() != dimension || frontierOverlay.getPersonal() != personal) {
            return;
        }

        boolean updatePage = false;
        if (isInFrontierPage()) {
            FrontierOverlay currentFrontier = getCurrentFrontier();
            if (currentFrontier.getId().equals(frontierOverlay.getId())) {
                updatePage = true;
            }
        } else {
            updatePage = true;
        }

        if (updatePage) {
            reloadPage(false);
        }
    }

    public void deleteFrontierMessage(int index, ResourceKey<Level> dimension, boolean personal, int playerID) {
        if (dimension != this.dimension || personal != this.personal) {
            return;
        }

        if (isInFrontierPage()) {
            int currentFrontierIndex = getCurrentFrontierIndex();
            updateIndexEntries();

            if (index == currentFrontierIndex) {
                changePage(frontiersPageStart + currentFrontierIndex, false);
            } else {
                if (index < currentFrontierIndex) {
                    --currentFrontierIndex;
                }

                currPage = frontiersPageStart + currentFrontierIndex;
                updatePageControls();
            }
        } else {
            updateIndexEntries();
            if (currPage > 0 && currPage == frontiersPageStart) {
                changePage(currPage - 1, false);
            } else {
                reloadPage(false);
            }
        }
    }

    private void sendChangesToServer() {
        if (isInFrontierPage() && lastFrontierHash != getCurrentFrontier().getHash()) {
            frontiersOverlayManager.clientUpdatefrontier(dimension, getCurrentFrontierIndex());
            lastFrontierHash = getCurrentFrontier().getHash();
        }
    }

    public void reloadPage(boolean syncFrontierWithServer) {
        changePage(currPage, syncFrontierWithServer, false);
    }

    private void changePage(int newPage) {
        changePage(newPage, true, true);
    }

    private void changePage(int newPage, boolean syncFrontierWithServer) {
        changePage(newPage, syncFrontierWithServer, true);
    }

    private void changePage(int newPage, boolean syncFrontierWithServer, boolean playSound) {
        if (playSound) {
            Sounds.playSoundTurnPage();
        }

        if (syncFrontierWithServer) {
            sendChangesToServer();
        }

        if (newPage >= getPageCount()) {
            newPage = getPageCount() - 1;
        } else if (newPage < 0) {
            newPage = 0;
        }

        currPage = newPage;

        if (isInFrontierPage()) {
            frontiersOverlayManager.setFrontierIndexSelected(dimension, getCurrentFrontierIndex());
            lastFrontierHash = getCurrentFrontier().getHash();
        } else {
            frontiersOverlayManager.setFrontierIndexSelected(dimension, -1);
        }

        bookPages.setDoublePage(currPage != 0);

        resetLabels();
        updateGridRenderer();
        resetTextColor();
        resetTextName();
        resetFinishButton();
        resetBannerButton();
        resetSliceSlider();
        updateButtonsVisibility();
    }

    private void updateIndexEntries() {
        renderables.removeAll(indexEntryButtons);
        children().removeAll(indexEntryButtons);
        indexEntryButtons.clear();

        frontiersPageStart = ((frontiersOverlayManager.getFrontierCount(dimension) - 1) / 6 + 1) / 2 + 1;

        int offsetFromScreenLeft = (width - bookImageWidth) / 2;
        int offsetFromScreenTop = (height - bookImageHeight) / 2;
        int posX = offsetFromScreenLeft + bookImageWidth / 2 + 10;
        int posY = offsetFromScreenTop + 35;
        int page = frontiersPageStart;
        int count = 0;
        boolean rightPage = true;
        for (FrontierOverlay frontier : frontiersOverlayManager.getAllFrontiers(dimension)) {
            String name1 = frontier.getName1();
            String name2 = frontier.getName2();

            if (name1.isEmpty() && name2.isEmpty()) {
                name1 = I18n.get("mapfrontiers.index_unnamed_1", ChatFormatting.ITALIC);
                name2 = I18n.get("mapfrontiers.index_unnamed_2", ChatFormatting.ITALIC);
            }

            IndexEntryButton entryButton = new IndexEntryButton(posX, posY, bookImageWidth / 2 - 20, page, name1, name2,
                    frontier.getColor(), rightPage, this::buttonPressed);
            entryButton.visible = false;
            indexEntryButtons.add(entryButton);
            addRenderableWidget(entryButton);

            ++page;
            posY += 21;
            ++count;
            if (count % 6 == 0) {
                posY = offsetFromScreenTop + 35;
                if (rightPage) {
                    posX = offsetFromScreenLeft + 10;
                    rightPage = false;
                } else {
                    posX = offsetFromScreenLeft + bookImageWidth / 2 + 10;
                    rightPage = true;
                }
            }
        }
    }

    private void beginStencil(PoseStack matrixStack, double x, double y, double width, double height) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.stencilFunc(GL11.GL_NEVER, 1, 0xff);
        RenderSystem.stencilOp(GL11.GL_REPLACE, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.stencilMask(0xff);
        RenderSystem.clearStencil(0);
        fill(matrixStack, (int) x, (int) y, (int) (x + width), (int) (y + height), 0xffffff);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xff);
    }

    private void endStencil() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private void updateGridRenderer() {
        if (!isInFrontierPage())
            return;

        FrontierOverlay frontier = getCurrentFrontier();

        if (frontier.getVertexCount() == 0) {
            return;
        }

        BlockPos topLeft = frontier.topLeft;
        BlockPos bottomRight = frontier.bottomRight;
        BlockPos center = new BlockPos((topLeft.getX() + bottomRight.getX()) / 2, 70, (topLeft.getZ() + bottomRight.getZ()) / 2);

        int maxSize = bottomRight.getX() - topLeft.getX();
        if (bottomRight.getZ() - topLeft.getZ() > maxSize) {
            maxSize = bottomRight.getZ() - topLeft.getZ();
        }

        zoom = 1;
        int size = 100;
        while (maxSize + 12 > size) {
            size *= 2;
            zoom *= 2;
        }

        if (frontier.getMapSlice() == FrontierOverlay.NoSlice) {
            EntityDTO player = DataCache.getPlayer();
            if (player.underground) {
                frontier.setMapSlice(player.chunkCoordY);
            } else {
                frontier.setMapSlice(FrontierOverlay.SurfaceSlice);
            }
        }

        MapType mapType = new MapType(frontier.getMapSlice() == FrontierOverlay.SurfaceSlice ? Name.day : Name.underground,
                frontier.getMapSlice(), frontier.getDimension());
        state.setMapType(mapType);
        state.refresh(minecraft, minecraft.player, miniMapProperties);
        state.setZoom(0);
        gridRenderer.clear();
        gridRenderer.setContext(state.getWorldDir(), mapType);
        gridRenderer.center(state.getWorldDir(), mapType, center.getX(), center.getZ(), state.getZoom());

        int displayWidth = minecraft.getWindow().getWidth();
        int displayHeight = minecraft.getWindow().getHeight();
        boolean highQuality = JourneymapClient.getInstance().getCoreProperties().tileHighDisplayQuality.get();
        gridRenderer.updateTiles(mapType, state.getZoom(), highQuality, displayWidth, displayHeight, true, 0.0, 0.0);
        gridRenderer.updateUIState(true);
    }

    private void drawMap(PoseStack matrixStack, int mouseX, int mouseY) {
        double x = width / 2 - bookImageWidth / 4;
        double y = height / 2;
        int displayWidth = minecraft.getWindow().getWidth();
        int displayHeight = minecraft.getWindow().getHeight();

        beginStencil(matrixStack, x - 500.0, y - 500.0, 1000.0, 1000.0);

//        GL11.glPushMatrix();
//        GL11.glTranslated(-bookImageWidth / 4, 0.0, 0.0);
//        GL11.glTranslated((width - displayWidth) / 2.0, (height - displayHeight) / 2.0, 0.0);
//
//        GL11.glTranslated(displayWidth / 2.0, displayHeight / 2.0, 0.0);
//        GL11.glScaled(1.0 / zoom, 1.0 / zoom, 1.0);
//        GL11.glTranslated(-displayWidth / 2.0, -displayHeight / 2.0, 0.0);

        matrixStack.pushPose();
        matrixStack.translate(-bookImageWidth / 4, 0.0, 0.0);
        matrixStack.translate((width - displayWidth) / 2.0, (height - displayHeight) / 2.0, 0.0);
        matrixStack.translate(displayWidth / 2.0, displayHeight / 2.0, 0.0);
        matrixStack.scale(1.f / zoom, 1.f / zoom, 1.f);
        matrixStack.translate(-displayWidth / 2.0, -displayHeight / 2.0, 0.0);

        gridRenderer.draw(matrixStack, 1.f, 1.f, 0.0, 0.0, miniMapProperties.showGrid.get());
        List<DrawStep> drawSteps = new ArrayList<>();
        ClientAPI.INSTANCE.getDrawSteps(drawSteps, gridRenderer.getUIState());
        gridRenderer.draw(matrixStack, drawSteps, 0.0, 0.0, 1, 0);

        matrixStack.popPose();
        //GL11.glPopMatrix();

        endStencil();
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
    }

    private void resetLabels() {
        labels.clear();

        int offsetFromScreenLeft = (width - bookImageWidth) / 2;
        int offsetFromScreenTop = (height - bookImageHeight) / 2;
        int rightPageCornerX = width / 2;
        int rightPageCornerY = (height - bookImageHeight) / 2;

        if (isInFrontierPage()) {
            Component frontierNumber = new TranslatableComponent("mapfrontiers.frontier_number",
                    getCurrentFrontierIndex() + 1, frontiersOverlayManager.getFrontierCount(dimension));
            labelFrontiernumber = new GuiSimpleLabel(font, offsetFromScreenLeft + bookImageWidth - 28,
                    offsetFromScreenTop + bookImageHeight - 15, GuiSimpleLabel.Align.Right, frontierNumber);
            labels.add(labelFrontiernumber);

            Component color = new TranslatableComponent("mapfrontiers.color");
            labels.add(new GuiSimpleLabel(font, rightPageCornerX + 13, rightPageCornerY + 20, GuiSimpleLabel.Align.Left, color));

            FrontierOverlay frontier = getCurrentFrontier();

            Component area = new TranslatableComponent("mapfrontiers.area", frontier.area);
            labels.add(new GuiSimpleLabel(font, rightPageCornerX + 13, rightPageCornerY + 40, GuiSimpleLabel.Align.Left, area));

            Component perimeter = new TranslatableComponent("mapfrontiers.perimeter", frontier.perimeter);
            labels.add(
                    new GuiSimpleLabel(font, rightPageCornerX + 13, rightPageCornerY + 52, GuiSimpleLabel.Align.Left, perimeter));

            String ownerString;
            if (!StringUtils.isBlank(frontier.getOwner().username)) {
                ownerString = frontier.getOwner().username;
            } else if (frontier.getOwner().uuid != null) {
                ownerString = frontier.getOwner().uuid.toString();
                ownerString = ownerString.substring(0, 14) + '\n' + ownerString.substring(14);
            } else {
                ownerString = I18n.get("mapfrontiers.unknown", ChatFormatting.ITALIC);
            }
            Component owner = new TranslatableComponent("mapfrontiers.owner", ownerString);
            labels.add(new GuiSimpleLabel(font, rightPageCornerX + 13, rightPageCornerY + 64, GuiSimpleLabel.Align.Left, owner));

            if (personal) {
                int sharedCount = 0;
                if (frontier.getUsersShared() != null) {
                    sharedCount = frontier.getUsersShared().size();
                }

                Component shared = new TranslatableComponent("mapfrontiers.shared", sharedCount);
                labels.add(new GuiSimpleLabel(font, rightPageCornerX + 13, rightPageCornerY + 76, GuiSimpleLabel.Align.Left,
                        shared));
            }

            SettingsUser playerUser = new SettingsUser(minecraft.player);
            boolean isOwner = frontier.getOwner().equals(playerUser);
            boolean canUpdate = false;

            if (personal) {
                canUpdate = frontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateFrontier);
            } else {
                SettingsProfile profile = ClientProxy.getSettingsProfile();
                if (profile.updateFrontier == SettingsProfile.State.Enabled
                        || (isOwner && profile.updateFrontier == SettingsProfile.State.Owner)) {
                    canUpdate = true;
                }
            }

            if (dimension == currentDimension && canUpdate) {
                if (frontier.getSelectedVertexIndex() >= 0) {
                    TranslatableComponent vertex = new TranslatableComponent("mapfrontiers.vertex_number",
                            frontier.getSelectedVertexIndex() + 1, frontier.getVertexCount());
                    vertex.append(String.format("\nX: %1$d  Z: %2$d", frontier.getSelectedVertex().getX(),
                            frontier.getSelectedVertex().getZ()));
                    labels.add(new GuiSimpleLabel(font, offsetFromScreenLeft + bookImageWidth / 4, rightPageCornerY + 150,
                            GuiSimpleLabel.Align.Center, vertex));
                } else if (frontier.getVertexCount() > 0) {
                    Component vertex = new TranslatableComponent("mapfrontiers.no_vertex_selected",
                            ChatFormatting.ITALIC);
                    labels.add(new GuiSimpleLabel(font, offsetFromScreenLeft + bookImageWidth / 4, rightPageCornerY + 150,
                            GuiSimpleLabel.Align.Center, vertex));
                }
            }
        } else {
            if (currPage > 0) {
                Component index = new TranslatableComponent("mapfrontiers.index_number", currPage * 2,
                        ChatFormatting.BOLD);
                labels.add(new GuiSimpleLabel(font, offsetFromScreenLeft + bookImageWidth / 4, rightPageCornerY + 18,
                        GuiSimpleLabel.Align.Center, index));
            }

            if (currPage < frontiersPageStart - 1 || (frontiersOverlayManager.getFrontierCount(dimension) - 1) / 6 % 2 == 0) {
                Component index;
                if (currPage > 0) {
                    index = new TranslatableComponent("mapfrontiers.index_number", currPage * 2 + 1, ChatFormatting.BOLD);
                } else {
                    index = new TranslatableComponent("mapfrontiers.index", ChatFormatting.BOLD);
                }
                labels.add(new GuiSimpleLabel(font, rightPageCornerX + bookImageWidth / 4, rightPageCornerY + 18,
                        GuiSimpleLabel.Align.Center, index));
            }
        }
    }

    private void resetTextColor() {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();
            textRed.setValue((frontier.getColor() & 0xff0000) >> 16);
            textGreen.setValue((frontier.getColor() & 0x00ff00) >> 8);
            textBlue.setValue(frontier.getColor() & 0x0000ff);
        }
    }

    private void resetTextName() {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();
            textName1.setValue(frontier.getName1());
            textName2.setValue(frontier.getName2());

            String suffix = "";
            if (ConfigData.nameVisibility != ConfigData.NameVisibility.Manual) {
                suffix += " " + GuiColors.WARNING + "!";
            }

            if (frontier.getNameVisible()) {
                buttonNameVisible.setMessage(new TranslatableComponent("mapfrontiers.show_name").append(suffix));
            } else {
                buttonNameVisible.setMessage(new TranslatableComponent("mapfrontiers.hide_name").append(suffix));
            }
        }
    }

    private void resetFinishButton() {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();
            if (frontier.getClosed()) {
                buttonFinish.setMessage(new TranslatableComponent("mapfrontiers.reopen"));
            } else {
                buttonFinish.setMessage(new TranslatableComponent("mapfrontiers.finish"));
            }
        }
    }

    private void resetBannerButton() {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();
            if (!frontier.hasBanner()) {
                String suffix = "";
                if (heldBanner == null) {
                    suffix += " " + GuiColors.WARNING + "!";
                }

                buttonBanner.setMessage(new TranslatableComponent("mapfrontiers.assign_banner").append(suffix));
            } else {
                buttonBanner.setMessage(new TranslatableComponent("mapfrontiers.remove_banner"));
            }
        }
    }

    private void resetSliceSlider() {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();
            if (frontier.getMapSlice() != FrontierOverlay.NoSlice) {
                sliderSlice.changeSlice(frontier.getMapSlice());
            }
        }
    }

    private void updateButtonsVisibility() {
        buttonClose.visible = true;
        buttonNextPage.visible = (currPage < getPageCount() - 1);
        buttonPreviousPage.visible = currPage > 0;

        SettingsProfile profile = ClientProxy.getSettingsProfile();
        FrontierOverlay frontier = null;
        SettingsUser playerUser = new SettingsUser(minecraft.player);
        boolean isOwner = false;
        boolean canCreate;
        boolean canDelete = false;
        boolean canUpdate = false;

        if (isInFrontierPage()) {
            frontier = getCurrentFrontier();
            isOwner = frontier.getOwner().equals(playerUser);
        }

        if (personal) {
            canCreate = true;
            if (isInFrontierPage()) {
                canDelete = true;
                canUpdate = frontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateFrontier);
            }
        } else {
            canCreate = profile.createFrontier == SettingsProfile.State.Enabled;
            canDelete = profile.deleteFrontier == SettingsProfile.State.Enabled
                    || (isOwner && profile.deleteFrontier == SettingsProfile.State.Owner);
            canUpdate = profile.updateFrontier == SettingsProfile.State.Enabled
                    || (isOwner && profile.updateFrontier == SettingsProfile.State.Owner);
        }

        if (isInFrontierPage()) {
            if (canDelete) {
                changeDeleteBookmarkPosition(DeleteBookmarkPosition.Normal);
            } else {
                changeDeleteBookmarkPosition(DeleteBookmarkPosition.Hidden);
            }

            buttonBackToIndex.visible = true;
            buttonNameVisible.visible = canUpdate;
            buttonEditShareSettings.visible = personal;

            textRed.active = canUpdate;
            textGreen.active = canUpdate;
            textBlue.active = canUpdate;
            textName1.active = canUpdate;
            textName2.active = canUpdate;

            textRed.visible = true;
            textGreen.visible = true;
            textBlue.visible = true;
            textName1.visible = true;
            textName2.visible = true;

            if (canUpdate && frontier.getVertexCount() > 0) {
                buttonSliceUp.visible = frontier.getMapSlice() < 16;
                buttonSliceDown.visible = frontier.getMapSlice() > 0;
                sliderSlice.visible = true;
            } else {
                buttonSliceUp.visible = false;
                buttonSliceDown.visible = false;
                sliderSlice.visible = false;
            }
            for (IndexEntryButton indexButton : indexEntryButtons) {
                indexButton.visible = false;
            }
        } else {
            changeDeleteBookmarkPosition(DeleteBookmarkPosition.Hidden);
            buttonBackToIndex.visible = false;
            buttonNameVisible.visible = false;
            buttonSliceUp.visible = false;
            buttonSliceDown.visible = false;
            sliderSlice.visible = false;
            buttonEditShareSettings.visible = false;
            textRed.visible = false;
            textGreen.visible = false;
            textBlue.visible = false;
            textName1.visible = false;
            textName2.visible = false;
            for (int i = 0; i < indexEntryButtons.size(); ++i) {
                if (i >= currPage * 12 - 6 && i < currPage * 12 + 6) {
                    indexEntryButtons.get(i).visible = true;
                } else {
                    indexEntryButtons.get(i).visible = false;
                }
            }
        }

        if (isInFrontierPage() && (currentDimension == dimension) && canUpdate) {
            buttonFinish.visible = frontier.getVertexCount() > 2;
            buttonBanner.visible = true;
            buttonAddVertex.visible = true;
            if (frontier.getVertexCount() == 0) {
                buttonNextVertex.visible = false;
                buttonPreviousVertex.visible = false;
            } else {
                buttonNextVertex.visible = true;
                buttonPreviousVertex.visible = true;
            }

            if (frontier.getSelectedVertexIndex() == -1) {
                buttonRemoveVertex.visible = false;
            } else {
                buttonRemoveVertex.visible = true;
            }
        } else {
            buttonFinish.visible = false;
            buttonBanner.visible = false;
            buttonAddVertex.visible = false;
            buttonNextVertex.visible = false;
            buttonPreviousVertex.visible = false;
            buttonRemoveVertex.visible = false;
        }

        if (currentDimension == dimension && canCreate) {
            buttonNew.visible = true;
        } else {
            buttonNew.visible = false;
        }
    }

    private void updatePageControls() {
        buttonNextPage.visible = (currPage < getPageCount() - 1);
        buttonPreviousPage.visible = currPage > 0;

        if (isInFrontierPage()) {
            int offsetFromScreenLeft = (width - bookImageWidth) / 2;
            int offsetFromScreenTop = (height - bookImageHeight) / 2;

            if (labelFrontiernumber != null) {
                labels.remove(labelFrontiernumber);
            }

            Component frontierNumber = new TranslatableComponent("mapfrontiers.frontier_number",
                    getCurrentFrontierIndex() + 1, frontiersOverlayManager.getFrontierCount(dimension));
            labelFrontiernumber = new GuiSimpleLabel(font, offsetFromScreenLeft + bookImageWidth - 28,
                    offsetFromScreenTop + bookImageHeight - 15, GuiSimpleLabel.Align.Right, frontierNumber);
            labels.add(labelFrontiernumber);
        }
    }

    private void changeDeleteBookmarkPosition(DeleteBookmarkPosition newPosition) {
        deleteBookmarkPosition = newPosition;
        buttonDelete.changePosition(deleteBookmarkPosition.ordinal());
        buttonDeleteConfirm.active = deleteBookmarkPosition == DeleteBookmarkPosition.Open;
    }

    private int getPageCount() {
        return frontiersOverlayManager.getFrontierCount(dimension) + frontiersPageStart;
    }

    private FrontierOverlay getCurrentFrontier() {
        return frontiersOverlayManager.getFrontier(dimension, getCurrentFrontierIndex());
    }

    private int getCurrentFrontierIndex() {
        return currPage - frontiersPageStart;
    }

    private boolean isInFrontierPage() {
        return currPage >= frontiersPageStart;
    }
}
