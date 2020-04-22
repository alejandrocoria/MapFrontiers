package games.alejandrocoria.mapfrontiers.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.Sounds;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import journeymap.client.api.display.Context;
import journeymap.client.api.impl.ClientAPI;
import journeymap.client.data.DataCache;
import journeymap.client.model.EntityDTO;
import journeymap.client.model.MapState;
import journeymap.client.model.MapType;
import journeymap.client.model.MapType.Name;
import journeymap.client.properties.MiniMapProperties;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.map.GridRenderer;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiFrontierBook extends GuiScreen implements TextColorBox.TextColorBoxResponder, TextBox.TextBoxResponder {
    private enum DeleteBookmarkPosition {
        Normal, Hidden, Open
    };

    private static final int bookImageHeight = 182;
    private static final int bookImageWidth = 312;
    private static final int bookTextureSize = 512;
    private static final TextFormatting chatPlayerColor = TextFormatting.YELLOW;

    private FrontiersOverlayManager frontiersOverlayManager;
    private int lastFrontierHash;
    private int currPage = 0;
    private int currentDimension;
    private int dimension;
    private ResourceLocation bookPageTexture;
    private int frontiersPageStart;

    private GuiButtonIcon buttonClose;
    private GuiButtonIcon buttonNextPage;
    private GuiButtonIcon buttonPreviousPage;
    private GuiButtonIcon buttonBackToIndex;
    private GuiButtonIcon buttonNextVertex;
    private GuiButtonIcon buttonPreviousVertex;
    private GuiBookmark buttonNew;
    private GuiBookmark buttonDelete;
    private DeleteBookmarkPosition deleteBookmarkPosition;
    private GuiLabel labelDeleteConfirm;
    private GuiBookTag buttonNameVisible;
    private GuiBookTag buttonAddVertex;
    private GuiBookTag buttonRemoveVertex;
    private GuiBookTag buttonFinish;
    private GuiButtonIcon buttonSliceUp;
    private GuiButtonIcon buttonSliceDown;
    private GuiSliderSlice sliderSlice;

    private TextColorBox textRed;
    private TextColorBox textGreen;
    private TextColorBox textBlue;
    private TextBox textName1;
    private TextBox textName2;
    private GuiSimpleLabel labelFrontiernumber;

    private List<IndexEntryButton> indexEntryButtons;
    private List<GuiSimpleLabel> labels;

    private static final MapState state = new MapState();
    private static final GridRenderer gridRenderer = new GridRenderer(Context.UI.Minimap, 3);
    private static final MiniMapProperties miniMapProperties = new MiniMapProperties(777);
    private static int zoom = 1;

    public GuiFrontierBook(FrontiersOverlayManager frontiersOverlayManager, int currentDimension, int dimension) {
        this.frontiersOverlayManager = frontiersOverlayManager;
        this.currentDimension = currentDimension;
        this.dimension = dimension;

        bookPageTexture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/gui.png");
        indexEntryButtons = new ArrayList<IndexEntryButton>();
        labels = new ArrayList<GuiSimpleLabel>();
    }

    @Override
    public void initGui() {
        Sounds.playSoundOpenBook();

        buttonList.clear();
        Keyboard.enableRepeatEvents(true);

        int offsetFromScreenLeft = (width - bookImageWidth) / 2;
        int offsetFromScreenTop = (height - bookImageHeight) / 2;
        int rightPageCornerX = width / 2;
        int rightPageCornerY = offsetFromScreenTop;
        int id = 0;

        buttonClose = new GuiButtonIcon(++id, offsetFromScreenLeft + bookImageWidth - 8, offsetFromScreenTop - 5, 13, 13, 471, 64,
                23, bookPageTexture, bookTextureSize);

        buttonNextPage = new GuiButtonIcon(++id, offsetFromScreenLeft + bookImageWidth - 27,
                offsetFromScreenTop + bookImageHeight - 18, 21, 13, 468, 0, 23, bookPageTexture, bookTextureSize);

        buttonPreviousPage = new GuiButtonIcon(++id, offsetFromScreenLeft + 6, offsetFromScreenTop + bookImageHeight - 18, 21, 13,
                468, 13, 23, bookPageTexture, bookTextureSize);

        buttonBackToIndex = new GuiButtonIcon(++id, rightPageCornerX - 8, rightPageCornerY + bookImageHeight - 18, 16, 13, 470,
                51, 23, bookPageTexture, bookTextureSize);

        buttonNextVertex = new GuiButtonIcon(++id, offsetFromScreenLeft + bookImageWidth / 2 - 25, offsetFromScreenTop + 148, 9,
                11, 473, 78, 23, bookPageTexture, bookTextureSize);

        buttonPreviousVertex = new GuiButtonIcon(++id, offsetFromScreenLeft + 16, offsetFromScreenTop + 148, 9, 11, 473, 90, 23,
                bookPageTexture, bookTextureSize);

        buttonNew = new GuiBookmark(++id, rightPageCornerX + 6, rightPageCornerY - 18, 21, 18, I18n.format("mapfrontiers.new"),
                bookPageTexture, bookTextureSize);

        buttonDelete = new GuiBookmark(++id, rightPageCornerX + 62, rightPageCornerY - 18, 44, 18,
                I18n.format("mapfrontiers.delete"), bookPageTexture, bookTextureSize);
        buttonDelete.addYPosition(rightPageCornerY + 4);
        buttonDelete.addYPosition(rightPageCornerY - 40);
        deleteBookmarkPosition = DeleteBookmarkPosition.Normal;

        labelDeleteConfirm = new GuiLabel(fontRenderer, ++id, buttonDelete.x, buttonDelete.y + 30, buttonDelete.width, 12,
                0xffffff);
        labelDeleteConfirm.setCentered();
        labelDeleteConfirm.addLine(I18n.format("mapfrontiers.confirm_delete"));
        buttonDelete.addlabel(labelDeleteConfirm);

        int leftTagsX = offsetFromScreenLeft + 3;
        int tagsWidth = getMaxWidth(I18n.format("mapfrontiers.show_name"), I18n.format("mapfrontiers.hide_name"),
                I18n.format("mapfrontiers.finish"), I18n.format("mapfrontiers.reopen"), I18n.format("mapfrontiers.add_vertex"),
                I18n.format("mapfrontiers.remove_vertex"));
        tagsWidth += 12;

        buttonNameVisible = new GuiBookTag(++id, leftTagsX, offsetFromScreenTop + 12, tagsWidth, true, "", bookPageTexture,
                bookTextureSize);

        buttonAddVertex = new GuiBookTag(++id, leftTagsX, offsetFromScreenTop + 32, tagsWidth, true,
                I18n.format("mapfrontiers.add_vertex"), bookPageTexture, bookTextureSize);

        buttonRemoveVertex = new GuiBookTag(++id, leftTagsX, offsetFromScreenTop + 52, tagsWidth, true,
                I18n.format("mapfrontiers.remove_vertex"), bookPageTexture, bookTextureSize);

        buttonFinish = new GuiBookTag(++id, leftTagsX, offsetFromScreenTop + 72, tagsWidth, true, "", bookPageTexture,
                bookTextureSize);

        buttonSliceUp = new GuiButtonIcon(++id, offsetFromScreenLeft + bookImageWidth / 2 - 21,
                offsetFromScreenTop + bookImageHeight / 2 - 45, 13, 9, 471, 102, 23, bookPageTexture, bookTextureSize);

        buttonSliceDown = new GuiButtonIcon(++id, offsetFromScreenLeft + bookImageWidth / 2 - 21,
                offsetFromScreenTop + bookImageHeight / 2 + 36, 13, 9, 471, 111, 23, bookPageTexture, bookTextureSize);

        sliderSlice = new GuiSliderSlice(++id, offsetFromScreenLeft + bookImageWidth / 2 - 21,
                offsetFromScreenTop + bookImageHeight / 2 - 35, bookPageTexture, bookTextureSize);

        int textColorX = rightPageCornerX + 44;
        int textColorY = rightPageCornerY + 18;

        textRed = new TextColorBox(++id, 255, fontRenderer, textColorX, textColorY);
        textRed.setResponder(this);

        textGreen = new TextColorBox(++id, 255, fontRenderer, textColorX + 26, textColorY);
        textGreen.setResponder(this);

        textBlue = new TextColorBox(++id, 255, fontRenderer, textColorX + 52, textColorY);
        textBlue.setResponder(this);

        int textNameX = offsetFromScreenLeft + bookImageWidth / 4 - 56;
        int textNameY = offsetFromScreenTop + 10;
        String defaultText = "Add name";
        textName1 = new TextBox(++id, fontRenderer, textNameX, textNameY, 113, defaultText);
        textName1.setMaxStringLength(17);
        textName1.setResponder(this);
        textName2 = new TextBox(++id, fontRenderer, textNameX, textNameY + 14, 113, defaultText);
        textName2.setMaxStringLength(17);
        textName2.setResponder(this);

        buttonList.add(buttonClose);
        buttonList.add(buttonNextPage);
        buttonList.add(buttonPreviousPage);
        buttonList.add(buttonBackToIndex);
        buttonList.add(buttonNextVertex);
        buttonList.add(buttonPreviousVertex);
        buttonList.add(buttonNew);
        buttonList.add(buttonNameVisible);
        buttonList.add(buttonAddVertex);
        buttonList.add(buttonRemoveVertex);
        buttonList.add(buttonFinish);
        buttonList.add(buttonSliceUp);
        buttonList.add(buttonSliceDown);
        buttonList.add(sliderSlice);

        updateIndexEntries();

        int selected = frontiersOverlayManager.getFrontierIndexSelected(dimension);
        if (selected < 0) {
            changePage(0, false);
        } else {
            changePage(selected + frontiersPageStart, false);
        }
    }

    @Override
    public void updateScreen() {
        if (isInFrontierPage()) {
            textRed.updateCursorCounter();
            textGreen.updateCursorCounter();
            textBlue.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.color(1.f, 1.f, 1.f);
        mc.getTextureManager().bindTexture(bookPageTexture);
        int offsetFromScreenLeft = (width - bookImageWidth) / 2;
        int offsetFromScreenTop = (height - bookImageHeight) / 2;

        drawModalRectWithCustomSizedTexture(offsetFromScreenLeft, offsetFromScreenTop, 0, 0, bookImageWidth, bookImageHeight,
                bookTextureSize, bookTextureSize);

        if (buttonDelete.visible) {
            buttonDelete.drawButton(mc, mouseX, mouseY, partialTicks);
        }

        GlStateManager.color(1.f, 1.f, 1.f);
        mc.getTextureManager().bindTexture(bookPageTexture);

        if (currPage == 0) {
            drawModalRectWithCustomSizedTexture(offsetFromScreenLeft + bookImageWidth / 2, offsetFromScreenTop,
                    bookImageWidth / 2, bookImageHeight, bookImageWidth / 2, bookImageHeight, bookTextureSize, bookTextureSize);
        } else {
            drawModalRectWithCustomSizedTexture(offsetFromScreenLeft, offsetFromScreenTop, 0, bookImageHeight, bookImageWidth,
                    bookImageHeight, bookTextureSize, bookTextureSize);
        }

        int rightPageCornerX = width / 2;
        int rightPageCornerY = (height - bookImageHeight) / 2;

        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();

            textRed.drawTextBox();
            textGreen.drawTextBox();
            textBlue.drawTextBox();

            textName1.drawTextBox();
            textName2.drawTextBox();

            drawRect(rightPageCornerX + 123, rightPageCornerY + 14, rightPageCornerX + 143, rightPageCornerY + 33, 0xff000000);
            drawRect(rightPageCornerX + 125, rightPageCornerY + 16, rightPageCornerX + 141, rightPageCornerY + 31,
                    frontier.getColor() | 0xff000000);

            if (frontier.getVertexCount() > 0) {
                drawMap();
            }
        }

        for (GuiSimpleLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (buttonClose.visible && buttonClose.isMouseOver()) {
            drawHoveringText(I18n.format("mapfrontiers.close"), mouseX, mouseY);
        }

        if (buttonNextPage.visible && buttonNextPage.isMouseOver()) {
            drawHoveringText(I18n.format("mapfrontiers.next_page"), mouseX, mouseY);
        }

        if (buttonPreviousPage.visible && buttonPreviousPage.isMouseOver()) {
            drawHoveringText(I18n.format("mapfrontiers.previous_page"), mouseX, mouseY);
        }

        if (buttonBackToIndex.visible && buttonBackToIndex.isMouseOver()) {
            drawHoveringText(I18n.format("mapfrontiers.back_to_index"), mouseX, mouseY);
        }

        if (buttonNextVertex.visible && buttonNextVertex.isMouseOver()) {
            drawHoveringText(I18n.format("mapfrontiers.next_vertex"), mouseX, mouseY);
        }

        if (buttonPreviousVertex.visible && buttonPreviousVertex.isMouseOver()) {
            drawHoveringText(I18n.format("mapfrontiers.previous_vertex"), mouseX, mouseY);
        }

        if (buttonSliceUp.visible && buttonSliceUp.isMouseOver()) {
            int slice = sliderSlice.getSlice();
            drawHoveringText(I18n.format("mapfrontiers.vertical_chunk", slice), mouseX, mouseY);
        }

        if (buttonSliceDown.visible && buttonSliceDown.isMouseOver()) {
            int slice = sliderSlice.getSlice();
            if (slice == FrontierOverlay.SurfaceSlice) {
                drawHoveringText(I18n.format("mapfrontiers.vertical_chunk_surface"), mouseX, mouseY);
            } else {
                drawHoveringText(I18n.format("mapfrontiers.vertical_chunk", slice), mouseX, mouseY);
            }
        }

        if (sliderSlice.visible && sliderSlice.isMouseOver()) {
            int slice = sliderSlice.getSlice();
            if (slice == FrontierOverlay.SurfaceSlice) {
                drawHoveringText(I18n.format("mapfrontiers.vertical_chunk_surface"), mouseX, mouseY);
            } else {
                drawHoveringText(I18n.format("mapfrontiers.vertical_chunk", slice), mouseX, mouseY);
            }
        }

        if (buttonNameVisible.visible && buttonNameVisible.isMouseOver()) {
            String prefix = TextFormatting.YELLOW + "! " + TextFormatting.RESET;
            if (ConfigData.nameVisibility == ConfigData.NameVisibility.Show) {
                drawHoveringText(prefix + I18n.format("mapfrontiers.show_name_warn"), mouseX, mouseY);
            } else if (ConfigData.nameVisibility == ConfigData.NameVisibility.Hide) {
                drawHoveringText(prefix + I18n.format("mapfrontiers.hide_name_warn"), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int btn) throws IOException {
        if (deleteBookmarkPosition == DeleteBookmarkPosition.Open) {
            if (labelDeleteConfirm.visible && x >= buttonDelete.x && y >= labelDeleteConfirm.y
                    && x <= buttonDelete.x + buttonDelete.width && y <= labelDeleteConfirm.y + 12) {
                if (isInFrontierPage()) {
                    changeDeleteBookmarkPosition(DeleteBookmarkPosition.Normal);
                    FrontiersOverlayManager.instance.clientDeleteFrontier(dimension, getCurrentFrontierIndex());
                }
            }
        }

        if (buttonDelete.mousePressed(mc, x, y)) {
            actionPerformed(buttonDelete);
        }

        if (isInFrontierPage()) {
            textRed.mouseClicked(x, y, btn);
            textGreen.mouseClicked(x, y, btn);
            textBlue.mouseClicked(x, y, btn);
            textName1.mouseClicked(x, y, btn);
            textName2.mouseClicked(x, y, btn);
        }

        super.mouseClicked(x, y, btn);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (selectedButton != null && selectedButton instanceof GuiSliderSlice) {
            actionPerformed(selectedButton);
        }

        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int btn, long timeSinceLastClick) {
        if (selectedButton != null && selectedButton instanceof GuiSliderSlice) {
            ((GuiSliderSlice) selectedButton).mouseDragged(mouseX, mouseY);
        }

        super.mouseClickMove(mouseX, mouseY, btn, timeSinceLastClick);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if (isInFrontierPage()) {
            textRed.textboxKeyTyped(typedChar, keyCode);
            textGreen.textboxKeyTyped(typedChar, keyCode);
            textBlue.textboxKeyTyped(typedChar, keyCode);
            textName1.textboxKeyTyped(typedChar, keyCode);
            textName2.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updatedValue(int id, int value) {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();

            if (textRed.getId() == id) {
                int newColor = (frontier.getColor() & 0x00ffff) | (value << 16);
                if (newColor != frontier.getColor()) {
                    frontier.setColor(newColor);
                    updateIndexEntries();
                    updateButtonsVisibility();
                }
            } else if (textGreen.getId() == id) {
                int newColor = (frontier.getColor() & 0xff00ff) | (value << 8);
                if (newColor != frontier.getColor()) {
                    frontier.setColor(newColor);
                    updateIndexEntries();
                    updateButtonsVisibility();
                }
            } else if (textBlue.getId() == id) {
                int newColor = (frontier.getColor() & 0xffff00) | value;
                if (newColor != frontier.getColor()) {
                    frontier.setColor(newColor);
                    updateIndexEntries();
                    updateButtonsVisibility();
                }
            }
        }
    }

    @Override
    public void updatedValue(int id, String value) {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();

            if (textName1.getId() == id) {
                if (frontier.getName1() != value) {
                    frontier.setName1(value);
                    updateIndexEntries();
                    updateButtonsVisibility();
                }
            } else if (textName2.getId() == id) {
                if (frontier.getName2() != value) {
                    frontier.setName2(value);
                    updateIndexEntries();
                    updateButtonsVisibility();
                }
            }
        }
    }

    @Override
    public void lostFocus(int id, String value) {
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == buttonClose) {
            mc.displayGuiScreen((GuiScreen) null);
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
            FrontiersOverlayManager.instance.clientCreateNewfrontier(dimension);
        } else if (button == buttonDelete) {
            if (deleteBookmarkPosition == DeleteBookmarkPosition.Normal) {
                changeDeleteBookmarkPosition(DeleteBookmarkPosition.Open);
            } else if (deleteBookmarkPosition == DeleteBookmarkPosition.Open) {
                changeDeleteBookmarkPosition(DeleteBookmarkPosition.Normal);
            }
        } else if (button == buttonNameVisible) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setNameVisible(!frontier.getNameVisible());
            resetTextName();
        } else if (button == buttonAddVertex) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.addVertex(Minecraft.getMinecraft().player.getPosition());
            resetLabels();
            updateButtonsVisibility();
            updateGridRenderer();
        } else if (button == buttonRemoveVertex) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.removeSelectedVertex();
            resetLabels();
            resetFinishButton();
            updateButtonsVisibility();
            updateGridRenderer();
        } else if (button == buttonFinish) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setClosed(!frontier.getClosed());
            resetFinishButton();
            updateButtonsVisibility();
            updateGridRenderer();
        } else if (button == buttonSliceUp) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setMapSlice(frontier.getMapSlice() + 1);
            resetSliceSlider();
            updateButtonsVisibility();
            updateGridRenderer();
        } else if (button == buttonSliceDown) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setMapSlice(frontier.getMapSlice() - 1);
            resetSliceSlider();
            updateButtonsVisibility();
            updateGridRenderer();
        } else if (button == sliderSlice) {
            FrontierOverlay frontier = getCurrentFrontier();
            frontier.setMapSlice(sliderSlice.getSlice());
            updateButtonsVisibility();
            updateGridRenderer();
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
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        sendChangesToServer();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    public void newFrontierMessage(FrontierOverlay frontierOverlay, int playerID) {
        if (frontierOverlay.getDimension() != dimension) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player.getEntityId() == playerID) {
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

            String message;
            Entity otherPlayer = Minecraft.getMinecraft().world.getEntityByID(playerID);
            if (otherPlayer == null) {
                message = I18n.format("mapfrontiers.chat.frontier_created_unknown");
            } else {
                message = I18n.format("mapfrontiers.chat.frontier_created", TextFormatting.RESET, chatPlayerColor,
                        otherPlayer.getName());
            }

            player.sendMessage(new TextComponentString(message));
        }
    }

    public void updateFrontierMessage(FrontierOverlay frontierOverlay, int playerID) {
        if (frontierOverlay.getDimension() != dimension) {
            return;
        }

        boolean updatePage = false;
        if (isInFrontierPage()) {
            FrontierOverlay currentFrontier = getCurrentFrontier();
            if (currentFrontier.getId() == frontierOverlay.getId()) {
                updatePage = true;
            }
        } else {
            updatePage = true;
        }

        if (updatePage) {
            reloadPage(false);
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player.getEntityId() != playerID) {
            String message;
            Entity otherPlayer = Minecraft.getMinecraft().world.getEntityByID(playerID);
            int index = frontiersOverlayManager.getFrontierIndex(frontierOverlay);
            if (otherPlayer == null) {
                message = I18n.format("mapfrontiers.chat.frontier_updated_unknown", index + 1);
            } else {
                message = I18n.format("mapfrontiers.chat.frontier_updated", TextFormatting.RESET, chatPlayerColor,
                        otherPlayer.getName(), index + 1);
            }

            player.sendMessage(new TextComponentString(message));
        }
    }

    public void deleteFrontierMessage(int index, int dimension, int playerID) {
        if (dimension != this.dimension) {
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

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player.getEntityId() != playerID) {
            String message;
            Entity otherPlayer = Minecraft.getMinecraft().world.getEntityByID(playerID);
            if (otherPlayer == null) {
                message = I18n.format("mapfrontiers.chat.frontier_deleted_unknown", index + 1);
            } else {
                message = I18n.format("mapfrontiers.chat.frontier_deleted", TextFormatting.RESET, chatPlayerColor,
                        otherPlayer.getName(), index + 1);
            }

            player.sendMessage(new TextComponentString(message));
        }
    }

    public void updateSettingsMessage() {
        if (!isInFrontierPage()) {
            return;
        }

        reloadPage(true);
    }

    private void sendChangesToServer() {
        if (isInFrontierPage() && lastFrontierHash != getCurrentFrontier().getHash()) {
            frontiersOverlayManager.clientUpdatefrontier(dimension, getCurrentFrontierIndex());
        }
    }

    public void reloadPage(boolean syncFrontierWithServer) {
        changePage(currPage, syncFrontierWithServer);
    }

    private void changePage(int newPage) {
        changePage(newPage, true);
    }

    private void changePage(int newPage, boolean syncFrontierWithServer) {
        Sounds.playSoundTurnPage();

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

        resetLabels();
        updateGridRenderer();
        resetTextColor();
        resetTextName();
        resetFinishButton();
        resetSliceSlider();
        updateButtonsVisibility();
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
                Integer.valueOf(frontier.getMapSlice()), frontier.getDimension());
        state.setMapType(mapType);
        state.refresh(mc, mc.player, miniMapProperties);
        gridRenderer.clear();
        gridRenderer.setContext(state.getWorldDir(), mapType);
        gridRenderer.center(state.getWorldDir(), mapType, center.getX(), center.getZ(), state.getZoom());

        boolean highQuality = Journeymap.getClient().getCoreProperties().tileHighDisplayQuality.get().booleanValue();
        gridRenderer.updateTiles(mapType, state.getZoom(), highQuality, mc.displayWidth, mc.displayHeight, true, 0.0, 0.0);
    }

    private void updateIndexEntries() {
        buttonList.removeAll(indexEntryButtons);
        indexEntryButtons.clear();

        frontiersPageStart = ((frontiersOverlayManager.getFrontierCount(dimension) - 1) / 6 + 1) / 2 + 1;

        int offsetFromScreenLeft = (width - bookImageWidth) / 2;
        int offsetFromScreenTop = (height - bookImageHeight) / 2;
        int posX = offsetFromScreenLeft + bookImageWidth / 2 + 10;
        int posY = offsetFromScreenTop + 35;
        int id = 100;
        int page = frontiersPageStart;
        int count = 0;
        boolean rightPage = true;
        for (FrontierOverlay frontier : frontiersOverlayManager.getAllFrontiers(dimension)) {
            String name1 = frontier.getName1();
            String name2 = frontier.getName2();

            if (name1.isEmpty() && name2.isEmpty()) {
                name1 = I18n.format("mapfrontiers.index_unnamed_1", TextFormatting.ITALIC);
                name2 = I18n.format("mapfrontiers.index_unnamed_2", TextFormatting.ITALIC);
            }

            IndexEntryButton button = new IndexEntryButton(id, posX, posY, bookImageWidth / 2 - 20, page, name1, name2,
                    frontier.getColor(), rightPage);
            button.visible = false;
            indexEntryButtons.add(button);
            buttonList.add(button);

            ++id;
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

    private void beginStencil(double x, double y, double width, double height) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GlStateManager.colorMask(false, false, false, false);
        GL11.glStencilFunc(GL11.GL_NEVER, 1, 0xff);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilMask(0xff);
        GlStateManager.clear(GL11.GL_STENCIL_BUFFER_BIT);
        DrawUtil.drawRectangle(x, y, width, height, 0xffffff, 1.0f);
        GlStateManager.colorMask(true, true, true, true);
        GL11.glStencilMask(0x00);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xff);
    }

    private void endStencil() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private void drawMap() {
        double x = width / 2 - bookImageWidth / 4;
        double y = height / 2;
        beginStencil(x - 50.0, y - 50.0, 100.0, 100.0);

        GlStateManager.pushMatrix();
        GlStateManager.translate(-bookImageWidth / 4, 0.0, 0.0);
        GlStateManager.scale(1.0 / zoom, 1.0 / zoom, 1.0);
        double zoomFactor = zoom / 2.0 - 0.5;
        GlStateManager.translate((width - mc.displayWidth) / 2, (height - mc.displayHeight) / 2, 0.0);
        GlStateManager.translate(width * zoomFactor, height * zoomFactor, 0.0);
        gridRenderer.draw(1.f, 0.0, 0.0, miniMapProperties.showGrid.get().booleanValue());
        List<DrawStep> drawSteps = new ArrayList<DrawStep>();
        ClientAPI.INSTANCE.getDrawSteps(drawSteps, gridRenderer.getUIState());
        gridRenderer.draw(drawSteps, 0.0, 0.0, 1, 0);
        GlStateManager.popMatrix();

        endStencil();
    }

    private void resetLabels() {
        labels.clear();

        int offsetFromScreenLeft = (width - bookImageWidth) / 2;
        int offsetFromScreenTop = (height - bookImageHeight) / 2;
        int rightPageCornerX = width / 2;
        int rightPageCornerY = (height - bookImageHeight) / 2;

        if (isInFrontierPage()) {
            String frontierNumber = I18n.format("mapfrontiers.frontier_number", getCurrentFrontierIndex() + 1,
                    frontiersOverlayManager.getFrontierCount(dimension));
            labelFrontiernumber = new GuiSimpleLabel(mc.fontRenderer, offsetFromScreenLeft + bookImageWidth - 28,
                    offsetFromScreenTop + bookImageHeight - 15, GuiSimpleLabel.Align.Right, frontierNumber);
            labels.add(labelFrontiernumber);

            String color = I18n.format("mapfrontiers.color");
            labels.add(new GuiSimpleLabel(mc.fontRenderer, rightPageCornerX + 13, rightPageCornerY + 20,
                    GuiSimpleLabel.Align.Left, color));

            FrontierOverlay frontier = getCurrentFrontier();

            String area = I18n.format("mapfrontiers.area", frontier.area);
            labels.add(new GuiSimpleLabel(mc.fontRenderer, rightPageCornerX + 13, rightPageCornerY + 40,
                    GuiSimpleLabel.Align.Left, area));

            String perimeter = I18n.format("mapfrontiers.perimeter", frontier.perimeter);
            labels.add(new GuiSimpleLabel(mc.fontRenderer, rightPageCornerX + 13, rightPageCornerY + 52,
                    GuiSimpleLabel.Align.Left, perimeter));

            String ownerString;
            if (!StringUtils.isBlank(frontier.getOwner().username)) {
                ownerString = frontier.getOwner().username;
            } else if (frontier.getOwner().uuid != null) {
                ownerString = frontier.getOwner().uuid.toString();
                ownerString = ownerString.substring(0, 14) + '\n' + ownerString.substring(14);
            } else {
                ownerString = I18n.format("mapfrontiers.unknown", TextFormatting.ITALIC);
            }
            String owner = I18n.format("mapfrontiers.owner", ownerString);
            labels.add(new GuiSimpleLabel(mc.fontRenderer, rightPageCornerX + 13, rightPageCornerY + 64,
                    GuiSimpleLabel.Align.Left, owner));

            SettingsProfile profile = frontiersOverlayManager.getSettingsProfile();
            boolean isOwner = frontier.getOwner().equals(new SettingsUser(Minecraft.getMinecraft().player));

            boolean canUpdate = profile.updateFrontier == SettingsProfile.State.Enabled
                    || (isOwner && profile.updateFrontier == SettingsProfile.State.Owner);

            if (dimension == currentDimension && canUpdate) {
                if (frontier.getSelectedVertexIndex() >= 0) {
                    String vertex = I18n.format("mapfrontiers.vertex_number", frontier.getSelectedVertexIndex() + 1,
                            frontier.getVertexCount());
                    labels.add(new GuiSimpleLabel(mc.fontRenderer, offsetFromScreenLeft + bookImageWidth / 4,
                            rightPageCornerY + 150, GuiSimpleLabel.Align.Center, vertex));
                } else if (frontier.getVertexCount() > 0) {
                    String vertex = I18n.format("mapfrontiers.no_vertex_selected", TextFormatting.ITALIC);
                    labels.add(new GuiSimpleLabel(mc.fontRenderer, offsetFromScreenLeft + bookImageWidth / 4,
                            rightPageCornerY + 150, GuiSimpleLabel.Align.Center, vertex));
                }
            }
        } else {
            if (currPage > 0) {
                String index = I18n.format("mapfrontiers.index_number", currPage * 2, TextFormatting.BOLD);
                labels.add(new GuiSimpleLabel(mc.fontRenderer, offsetFromScreenLeft + bookImageWidth / 4, rightPageCornerY + 18,
                        GuiSimpleLabel.Align.Center, index));
            }

            if (currPage < frontiersPageStart - 1 || (frontiersOverlayManager.getFrontierCount(dimension) - 1) / 6 % 2 == 0) {
                String index;
                if (currPage > 0) {
                    index = I18n.format("mapfrontiers.index_number", currPage * 2 + 1, TextFormatting.BOLD);
                } else {
                    index = I18n.format("mapfrontiers.index", TextFormatting.BOLD);
                }
                labels.add(new GuiSimpleLabel(mc.fontRenderer, rightPageCornerX + bookImageWidth / 4, rightPageCornerY + 18,
                        GuiSimpleLabel.Align.Center, index));
            }
        }
    }

    private void resetTextColor() {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();
            textRed.setText((frontier.getColor() & 0xff0000) >> 16);
            textGreen.setText((frontier.getColor() & 0x00ff00) >> 8);
            textBlue.setText(frontier.getColor() & 0x0000ff);
        }
    }

    private void resetTextName() {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();
            textName1.setText(frontier.getName1());
            textName2.setText(frontier.getName2());

            String suffix = "";
            if (ConfigData.nameVisibility != ConfigData.NameVisibility.Manual) {
                suffix += " " + TextFormatting.YELLOW + "!";
            }

            if (frontier.getNameVisible()) {
                buttonNameVisible.setText(I18n.format("mapfrontiers.show_name") + suffix);
            } else {
                buttonNameVisible.setText(I18n.format("mapfrontiers.hide_name") + suffix);
            }
        }
    }

    private void resetFinishButton() {
        if (isInFrontierPage()) {
            FrontierOverlay frontier = getCurrentFrontier();
            if (frontier.getClosed()) {
                buttonFinish.setText(I18n.format("mapfrontiers.reopen"));
            } else {
                buttonFinish.setText(I18n.format("mapfrontiers.finish"));
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

        SettingsProfile profile = frontiersOverlayManager.getSettingsProfile();
        FrontierOverlay frontier = null;
        boolean isOwner = false;

        if (isInFrontierPage()) {
            frontier = getCurrentFrontier();
            isOwner = frontier.getOwner().equals(new SettingsUser(Minecraft.getMinecraft().player));
        }

        boolean canCreate = profile.createFrontier == SettingsProfile.State.Enabled;
        boolean canDelete = profile.deleteFrontier == SettingsProfile.State.Enabled
                || (isOwner && profile.deleteFrontier == SettingsProfile.State.Owner);
        boolean canUpdate = profile.updateFrontier == SettingsProfile.State.Enabled
                || (isOwner && profile.updateFrontier == SettingsProfile.State.Owner);

        if (isInFrontierPage()) {
            if (canDelete) {
                changeDeleteBookmarkPosition(DeleteBookmarkPosition.Normal);
            } else {
                changeDeleteBookmarkPosition(DeleteBookmarkPosition.Hidden);
            }

            buttonBackToIndex.visible = true;
            buttonNameVisible.visible = canUpdate;

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

            String frontierNumber = I18n.format("mapfrontiers.frontier_number", getCurrentFrontierIndex() + 1,
                    frontiersOverlayManager.getFrontierCount(dimension));
            labelFrontiernumber = new GuiSimpleLabel(mc.fontRenderer, offsetFromScreenLeft + bookImageWidth - 28,
                    offsetFromScreenTop + bookImageHeight - 15, GuiSimpleLabel.Align.Right, frontierNumber);
            labels.add(labelFrontiernumber);
        }
    }

    private void changeDeleteBookmarkPosition(DeleteBookmarkPosition newPosition) {
        deleteBookmarkPosition = newPosition;
        buttonDelete.changePosition(deleteBookmarkPosition.ordinal());
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

    private int getMaxWidth(String... strings) {
        int maxWidth = 0;

        for (String s : strings) {
            int width = fontRenderer.getStringWidth(s);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        return maxWidth;
    }
}
