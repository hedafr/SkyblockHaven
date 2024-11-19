package de.skyh.skyhaven.config.gui;

import de.skyh.skyhaven.Skyhaven;
import de.skyh.skyhaven.config.SkyhConfig;
import de.skyh.skyhaven.config.SkyhConfigCategory;
import de.skyh.skyhaven.listener.PlayerListener;
import de.skyh.skyhaven.util.GuiHelper;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * Main config gui containing:
 * <ul>
 * <li>menu ({@link SkyhConfigMenuList}) with list of config categories ({@link SkyhConfigCategory})</li>
 * <li>the current opened config category with its sub-categories ({@link SkyhConfigCategoryScrolling})</li>
 * </ul>
 * Based on {@link net.minecraft.client.gui.GuiControls}
 */
public class SkyhConfigGui extends GuiScreen {
    public static long showDungeonPerformanceOverlayUntil;
    private static final String searchPlaceholder = "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Search config";
    public int menuWidth;
    private SkyhConfigMenuList menu;
    private int selectedMenuIndex = -1;
    private SkyhConfigCategory currentConfigCategory;
    private final boolean isOutsideOfSkyBlock;
    /**
     * equivalent of GuiModList.keyBindingList
     */
    private SkyhConfigCategoryScrolling currentConfigCategoryGui;
    private GuiButton btnClose;
    private GuiTextField fieldSearchQuery;
    private String searchQuery;

    public SkyhConfigGui(String searchQuery) {
        isOutsideOfSkyBlock = PlayerListener.registerSkyBlockListeners();
        this.searchQuery = searchQuery;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        SkyhConfigPreview.parent = this;

        // re-register SkyBlock listeners if necessary (mainly so that previews function correctly outside of SkyBlock)
        PlayerListener.registerSkyBlockListeners();

        for (SkyhConfigCategory configCategory : SkyhConfig.getConfigCategories()) {
            menuWidth = Math.max(menuWidth, fontRendererObj.getStringWidth(configCategory.getMenuDisplayName()) + 10 + 2);
        }
        menuWidth = Math.min(menuWidth, 150);
        this.menu = new SkyhConfigMenuList(this, menuWidth);

        this.buttonList.add(this.btnClose = new GuiButton(6, this.width - 25, 3, 22, 20, EnumChatFormatting.RED + "X"));
        this.fieldSearchQuery = new GuiTextField(42, this.fontRendererObj, 5, 11, 100, 15);
        this.fieldSearchQuery.setMaxStringLength(42);
        this.fieldSearchQuery.setText(searchPlaceholder);
        if (selectedMenuIndex == -1) {
            // no category selected yet: this isn't a resize
            if (!StringUtils.isNullOrEmpty(searchQuery)) {
                this.fieldSearchQuery.setText(searchQuery);
                // switch to search
                searchConfigEntries();
            } else {
                // switch to 1st category
                selectConfigCategory(0);
            }
        } else if (selectedMenuIndex == -42) {
            this.fieldSearchQuery.setText(searchQuery);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (currentConfigCategoryGui != null) {
            this.currentConfigCategoryGui.handleMouseInput();
        }
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id == 6) { // close gui
                this.mc.displayGuiScreen(null);
            }
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0 || (currentConfigCategoryGui != null && !this.currentConfigCategoryGui.mouseClicked(mouseX, mouseY, mouseButton))) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            fieldSearchQuery.mouseClicked(mouseX, mouseY, mouseButton);
            if (fieldSearchQuery.isFocused()) {
                if (mouseButton == /* right click */ 1
                        || (mouseButton == /* left click */ 0 && searchPlaceholder.equals(fieldSearchQuery.getText()))) {
                    // clear search query
                    fieldSearchQuery.setText("");
                    searchConfigEntries();
                }
            }
        }
    }

    /**
     * Called when a mouse button is released.  Args : mouseX, mouseY, releaseButton
     */
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state != 0 || currentConfigCategoryGui != null && !this.currentConfigCategoryGui.mouseReleased(mouseX, mouseY, state)) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    /**
     * Fired when a key is typed (except F11 which toggles full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE && (currentConfigCategoryGui == null || !currentConfigCategoryGui.isModifyingKeyBind())) {
            super.keyTyped(typedChar, keyCode);
        } else if (this.currentConfigCategoryGui != null) {
            if (this.fieldSearchQuery.isFocused() && this.fieldSearchQuery.textboxKeyTyped(typedChar, keyCode)) {
                String queryText = fieldSearchQuery.getText();
                if (queryText.length() >= 3) {
                    String soundName = "mob." + queryText + ".say";
                    SoundEventAccessorComposite sound = mc.getSoundHandler().getSound(new ResourceLocation(soundName));
                    if (sound != null) {
                        mc.thePlayer.playSound(soundName, 0.5f, 1);
                    }
                }
                searchConfigEntries();
            } else {
                this.currentConfigCategoryGui.keyTyped(typedChar, keyCode);
            }
        }
    }

    private void searchConfigEntries() {
        searchQuery = this.fieldSearchQuery.getText();
        // display search page if not already displayed
        if (!selectConfigCategory(-42)) {
            // was already searching before, update search results
            this.currentConfigCategoryGui.showFilteredConfigEntries(searchQuery);
        }
    }

    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY, renderPartialTicks
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!showDungeonPerformanceOverlay()) {
            this.menu.drawScreen(mouseX, mouseY, partialTicks);
        }

        String guiTitle = "" + EnumChatFormatting.BOLD + EnumChatFormatting.UNDERLINE + Skyhaven.MODNAME + " config:" + EnumChatFormatting.RESET + " " + (currentConfigCategory != null ? currentConfigCategory.getDisplayName() : "Search");
        int guiTitleX = ((menu.getRight() + this.width) / 2) - this.fontRendererObj.getStringWidth(guiTitle) / 2;
        this.drawCenteredString(this.fontRendererObj, guiTitle, guiTitleX, 16, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (currentConfigCategoryGui != null) {
            currentConfigCategoryGui.drawScreen(mouseX, mouseY, partialTicks);
        }
        fieldSearchQuery.drawTextBox();
        if (btnClose.isMouseOver()) {
            GuiHelper.drawHoveringText(Arrays.asList(EnumChatFormatting.RED + "Save & close settings", ""
                    + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Hint:" + EnumChatFormatting.RESET + " alternatively press ESC"), mouseX, mouseY, width, height, 300);
        } else if (mouseX >= fieldSearchQuery.xPosition && mouseX <= fieldSearchQuery.xPosition + fieldSearchQuery.width
                && mouseY >= fieldSearchQuery.yPosition && mouseY <= fieldSearchQuery.yPosition + fieldSearchQuery.height) {
            GuiHelper.drawHoveringText(Collections.singletonList("" + EnumChatFormatting.GREEN + EnumChatFormatting.ITALIC + "Hint:" + EnumChatFormatting.RESET
                    + " Search for \"" + EnumChatFormatting.GOLD + "new" + EnumChatFormatting.RESET + "\" to show new or changed config entries"), mouseX, mouseY, width, height, 300);
        }
    }

    @Override
    public void updateScreen() {
        fieldSearchQuery.updateCursorCounter();
    }

    @Override
    public void drawDefaultBackground() {
        if (!SkyhConfigGui.showDungeonPerformanceOverlay()) {
            super.drawDefaultBackground();
        }
    }

    // config category menu methods:

    /**
     * Select a config category via the menu
     */
    public boolean selectConfigCategory(int index) {
        if (index == this.selectedMenuIndex) {
            return false;
        }
        this.selectedMenuIndex = index;
        if (index >= 0 && index <= SkyhConfig.getConfigCategories().size()) {
            this.currentConfigCategory = SkyhConfig.getConfigCategories().get(selectedMenuIndex);
            // reset search to placeholder
            this.fieldSearchQuery.setText(searchPlaceholder);
            this.searchQuery = "";
        } else {
            // show search results
            this.currentConfigCategory = null;
        }

        switchDisplayedConfigCategory();
        Skyhaven.getInstance().getConfig().syncFromGui();
        return true;
    }

    /**
     * Helper method for menu: is config category selected?
     */
    public boolean isConfigCategorySelected(int index) {
        return index == selectedMenuIndex;
    }

    public void switchDisplayedConfigCategory() {
        if (currentConfigCategory == null) {
            // display search
            currentConfigCategoryGui = new SkyhConfigCategoryScrolling(this, mc, searchQuery, menu.getRight() + 3);
        } else {
            // display one category
            currentConfigCategoryGui = new SkyhConfigCategoryScrolling(this, mc, currentConfigCategory, menu.getRight() + 3);
        }
    }

    @Override
    public void renderToolTip(ItemStack stack, int x, int y) {
        super.renderToolTip(stack, x, y);
        GlStateManager.disableLighting();
    }

    @Override
    public void handleComponentHover(IChatComponent component, int x, int y) {
        super.handleComponentHover(component, x, y);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        Skyhaven.getInstance().getConfig().syncFromGui();

        if (isOutsideOfSkyBlock) {
            PlayerListener.unregisterSkyBlockListeners();
        }
        if (SkyhConfigPreview.parent != null) {
            SkyhConfigPreview.parent = null;
        }
    }

    public static boolean showDungeonPerformanceOverlay() {
        return showDungeonPerformanceOverlayUntil > System.currentTimeMillis();
    }
}
