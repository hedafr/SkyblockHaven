package de.skyh.skyhaven.config.gui;

import de.skyh.skyhaven.config.SkyhConfig;
import de.skyh.skyhaven.config.SkyhConfigCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.client.GuiScrollingList;

/**
 * Config menu displaying a list of config categories
 * <p>
 * Based on {@link net.minecraftforge.fml.client.GuiSlotModList}
 */
public class SkyhConfigMenuList extends GuiScrollingList {
    private final SkyhConfigGui parent;

    public SkyhConfigMenuList(SkyhConfigGui parent, int listWidth) {
        super(Minecraft.getMinecraft(), listWidth, parent.height, 32, parent.height - 5, 5, 15, parent.width, parent.height);
        this.parent = parent;
    }

    @Override
    protected int getSize() {
        return SkyhConfig.getConfigCategories().size();
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick) {
        this.parent.selectConfigCategory(index);
    }

    @Override
    protected boolean isSelected(int index) {
        return this.parent.isConfigCategorySelected(index);
    }

    @Override
    protected void drawBackground() {
        this.parent.drawDefaultBackground();
    }

    @Override
    protected int getContentHeight() {
        return (this.getSize()) * 15 + 1;
    }

    @Override
    protected void drawScreen(int mouseX, int mouseY) {
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    protected void drawSlot(int idx, int right, int top, int height, Tessellator tess) {
        SkyhConfigCategory configCategory = SkyhConfig.getConfigCategories().get(idx);
        String name = EnumChatFormatting.getTextWithoutFormattingCodes(configCategory.getMenuDisplayName());
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;

        font.drawString(font.trimStringToWidth(name, listWidth - 10), this.left + 3, top + 2, 0xFFFFFF);
    }

    protected int getRight() {
        return this.right;
    }
}
