package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import games.alejandrocoria.mapfrontiers.client.util.StringHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class TextUserBox extends TextBox {
    private final Minecraft mc;
    private String partialText;
    private List<String> suggestions;
    private List<String> suggestionsToDraw;
    private String error;
    private int maxSuggestionWidth = 0;
    private int suggestionIndex = 0;

    public TextUserBox(int componentId, Minecraft mc, FontRenderer fontRenderer, int x, int y, int width, String defaultText) {
        super(componentId, fontRenderer, x, y, width, defaultText);
        this.mc = mc;
        suggestions = new ArrayList<String>();
        suggestionsToDraw = new ArrayList<String>();

        setError("");
    }

    public void setError(String error) {
        this.error = error;

        if (this.error.isEmpty()) {
            setColor(GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        } else {
            setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
            suggestions.clear();
            suggestionsToDraw.clear();
        }
    }

    @Override
    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_TAB) {
            if (suggestions.isEmpty()) {
                suggestionIndex = 0;
                NetHandlerPlayClient handler = mc.getConnection();
                if (!StringUtils.isBlank(getText()) && handler != null) {
                    partialText = getText();
                    for (NetworkPlayerInfo playerInfo : handler.getPlayerInfoMap()) {
                        String name = playerInfo.getGameProfile().getName();
                        if (name != null && name.regionMatches(true, 0, partialText, 0, partialText.length())) {
                            suggestions.add(name);
                        }
                    }
                }
            } else {
                ++suggestionIndex;
                if (suggestionIndex >= suggestions.size()) {
                    suggestionIndex = 0;
                }
            }

            suggestionsToDraw.clear();

            if (!suggestions.isEmpty()) {
                setText(suggestions.get(suggestionIndex));

                if (suggestions.size() == 1) {
                    suggestions.clear();
                } else {
                    maxSuggestionWidth = 0;
                    int size = suggestions.size();

                    if (size > 7) {
                        size = 7;
                    }

                    int firstIndex = 0;
                    if (suggestionIndex > 6) {
                        firstIndex = suggestionIndex - 6;
                    }

                    for (int i = firstIndex; i < firstIndex + size; ++i) {
                        suggestionsToDraw.add(suggestions.get(i));
                        int textWidth = fontRenderer.getStringWidth(suggestions.get(i));
                        if (textWidth > maxSuggestionWidth) {
                            maxSuggestionWidth = textWidth;
                        }
                    }
                }
            }

            return true;
        } else {
            suggestions.clear();
            suggestionsToDraw.clear();
            return super.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void drawTextBox(int mouseX, int mouseY) {
        super.drawTextBox(mouseX, mouseY);

        if (!error.isEmpty()) {
            List<String> errorList = fontRenderer.listFormattedStringToWidth(error, width - 8);
            int maxErrorWidth = StringHelper.getMaxWidth(fontRenderer, errorList);

            Gui.drawRect(x - 1, y - errorList.size() * 12 - 5, x + maxErrorWidth + 9, y - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BORDER);
            Gui.drawRect(x, y - errorList.size() * 12 - 4, x + maxErrorWidth + 8, y - 1, GuiColors.SETTINGS_TEXT_BOX_EXTRA_BG);

            int posX = x + 4;
            int posY = y - errorList.size() * 12;
            for (String e : errorList) {
                fontRenderer.drawString(e, posX, posY, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                posY += 12;
            }
        } else if (!suggestionsToDraw.isEmpty()) {
            Gui.drawRect(x - 1, y - suggestionsToDraw.size() * 12 - 5, x + maxSuggestionWidth + 9, y - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BORDER);
            Gui.drawRect(x, y - suggestionsToDraw.size() * 12 - 4, x + maxSuggestionWidth + 8, y - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BG);

            int posX = x + 4;
            int posY = y - 12;
            for (int i = suggestionsToDraw.size() - 1; i >= 0; --i) {
                String t = suggestionsToDraw.get(i);
                if (suggestionsToDraw.get(i) == suggestions.get(suggestionIndex)) {
                    fontRenderer.drawString(t, posX, posY, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                } else {
                    String suffix = t.substring(0, partialText.length());
                    String rest = t.substring(partialText.length());
                    fontRenderer.drawString(suffix, posX, posY, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                    fontRenderer.drawString(rest, posX + fontRenderer.getStringWidth(suffix), posY,
                            GuiColors.SETTINGS_TEXT_MEDIUM);
                }

                posY -= 12;
            }
        }
    }

    @Override
    public void setFocused(boolean isFocusedIn) {
        if (!isFocusedIn) {
            suggestions.clear();
            suggestionsToDraw.clear();
        }

        setError("");

        super.setFocused(isFocusedIn);
    }
}