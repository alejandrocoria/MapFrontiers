package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class TextUserBox extends TextBox {
    private final Minecraft mc;
    private final Font font;
    private String partialText;
    private final List<String> suggestions;
    private final List<String> suggestionsToDraw;
    private Component error;
    private int maxSuggestionWidth = 0;
    private int suggestionIndex = 0;

    public TextUserBox(Minecraft mc, Font font, int x, int y, int width) {
        this(mc, font,x, y, width, "");
    }

    public TextUserBox(Minecraft mc, Font font, int x, int y, int width, String defaultText) {
        super(font, x, y, width, defaultText);
        this.mc = mc;
        this.font = font;
        suggestions = new ArrayList<>();
        suggestionsToDraw = new ArrayList<>();

        setError(null);
    }

    public void setError(@Nullable Component error) {
        this.error = error;

        if (this.error != null) {
            suggestions.clear();
            suggestionsToDraw.clear();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // @Note: Can't use Tab because it's used for accessibility.
        if (keyCode == GLFW.GLFW_KEY_LEFT_ALT) {
            if (suggestions.isEmpty()) {
                suggestionIndex = 0;
                ClientPacketListener handler = mc.getConnection();
                if (!StringUtils.isBlank(getValue()) && handler != null) {
                    partialText = getValue();
                    for (PlayerInfo playerInfo : handler.getOnlinePlayers()) {
                        String name = playerInfo.getProfile().getName();
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
                this.setValue(suggestions.get(suggestionIndex));

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
                        int textWidth = font.width(suggestions.get(i));
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
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (error == null) {
            setTextColor(GuiColors.SETTINGS_TEXT_BOX_TEXT);
        } else {
            setTextColor(GuiColors.SETTINGS_TEXT_ERROR);
        }

        super.renderWidget(matrixStack, mouseX, mouseY, partialTicks);

        if (error != null) {
            List<FormattedCharSequence> errorList = font.split(error, width - 8);
            int maxErrorWidth = width - 8;

            fill(matrixStack, getX() - 1, getY() - errorList.size() * 12 - 5, getX() + maxErrorWidth + 9, getY() - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BORDER);
            fill(matrixStack, getX(), getY() - errorList.size() * 12 - 4, getX() + maxErrorWidth + 8, getY() - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BG);

            int posX = getX() + 4;
            int posY = getY() - errorList.size() * 12;
            for (FormattedCharSequence e : errorList) {
                font.draw(matrixStack, e, posX, posY, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                posY += 12;
            }
        } else if (!suggestionsToDraw.isEmpty()) {
            fill(matrixStack, getX() - 1, getY() - suggestionsToDraw.size() * 12 - 5, getX() + maxSuggestionWidth + 9, getY() - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BORDER);
            fill(matrixStack, getX(), getY() - suggestionsToDraw.size() * 12 - 4, getX() + maxSuggestionWidth + 8, getY() - 1,
                    GuiColors.SETTINGS_TEXT_BOX_EXTRA_BG);

            int posX = getX() + 4;
            int posY = getY() - 12;
            for (int i = suggestionsToDraw.size() - 1; i >= 0; --i) {
                String t = suggestionsToDraw.get(i);
                // Strings are compared using == because they are the same objects
                // that are added to suggestions and suggestionsToDraw
                //noinspection StringEquality
                if (suggestionsToDraw.get(i) == suggestions.get(suggestionIndex)) {
                    font.draw(matrixStack, t, posX, posY, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                } else {
                    String suffix = t.substring(0, partialText.length());
                    String rest = t.substring(partialText.length());
                    font.draw(matrixStack, suffix, posX, posY, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                    font.draw(matrixStack, rest, posX + font.width(suffix), posY,
                            GuiColors.SETTINGS_TEXT_MEDIUM);
                }

                posY -= 12;
            }
        }
    }

    @Override
    public void setFocused(boolean isFocusedIn) {
        super.setFocused(isFocusedIn);

        if (!isFocusedIn) {
            suggestions.clear();
            suggestionsToDraw.clear();
        }

        setError(null);
    }
}
