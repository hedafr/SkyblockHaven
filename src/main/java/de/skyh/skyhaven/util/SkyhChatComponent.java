package de.skyh.skyhaven.util;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.io.File;

@SuppressWarnings("unused")
public class SkyhChatComponent extends ChatComponentText {
    public SkyhChatComponent(String msg) {
        super(msg);
    }

    public SkyhChatComponent black() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.BLACK));
        return this;
    }

    public SkyhChatComponent darkBlue() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_BLUE));
        return this;
    }

    public SkyhChatComponent darkGreen() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_GREEN));
        return this;
    }

    public SkyhChatComponent darkAqua() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_AQUA));
        return this;
    }

    public SkyhChatComponent darkRed() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_RED));
        return this;
    }

    public SkyhChatComponent darkPurple() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_PURPLE));
        return this;
    }

    public SkyhChatComponent gold() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.GOLD));
        return this;
    }

    public SkyhChatComponent gray() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.GRAY));
        return this;
    }

    public SkyhChatComponent darkGray() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_GRAY));
        return this;
    }

    public SkyhChatComponent blue() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.BLUE));
        return this;
    }

    public SkyhChatComponent green() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.GREEN));
        return this;
    }

    public SkyhChatComponent aqua() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.AQUA));
        return this;
    }

    public SkyhChatComponent red() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.RED));
        return this;
    }

    public SkyhChatComponent lightPurple() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE));
        return this;
    }

    public SkyhChatComponent yellow() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.YELLOW));
        return this;
    }

    public SkyhChatComponent white() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.WHITE));
        return this;
    }

    public SkyhChatComponent obfuscated() {
        setChatStyle(getChatStyle().setObfuscated(true));
        return this;
    }

    public SkyhChatComponent bold() {
        setChatStyle(getChatStyle().setBold(true));
        return this;
    }

    public SkyhChatComponent strikethrough() {
        setChatStyle(getChatStyle().setStrikethrough(true));
        return this;
    }

    public SkyhChatComponent underline() {
        setChatStyle(getChatStyle().setUnderlined(true));
        return this;
    }

    public SkyhChatComponent italic() {
        setChatStyle(getChatStyle().setItalic(true));
        return this;
    }

    public SkyhChatComponent reset() {
        setChatStyle(getChatStyle().setParentStyle(null).setBold(false).setItalic(false).setObfuscated(false).setUnderlined(false).setStrikethrough(false));
        return this;
    }

    public SkyhChatComponent setHover(IChatComponent hover) {
        setChatStyle(getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        return this;
    }

    public SkyhChatComponent setUrl(String url) {
        return setUrl(url, new KeyValueTooltipComponent("Click to visit", url));
    }

    public SkyhChatComponent setUrl(String url, String hover) {
        return setUrl(url, new SkyhChatComponent(hover).yellow());
    }

    public SkyhChatComponent setUrl(String url, IChatComponent hover) {
        setChatStyle(getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        setHover(hover);
        return this;
    }

    public SkyhChatComponent setOpenFile(File filePath) {
        setChatStyle(getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, filePath.getAbsolutePath())));
        setHover(new SkyhChatComponent(filePath.isFile() ? "Open " + filePath.getName() : "Open folder: " + filePath).yellow());
        return this;
    }

    public SkyhChatComponent setSuggestCommand(String command) {
        setSuggestCommand(command, true);
        return this;
    }

    public SkyhChatComponent setSuggestCommand(String command, boolean addTooltip) {
        setChatStyle(getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
        if (addTooltip) {
            setHover(new KeyValueChatComponent("Run", command, " "));
        }
        return this;
    }

    @Override
    public SkyhChatComponent appendSibling(IChatComponent component) {
        if (component != null) {
            super.appendSibling(component);
        }
        return this;
    }

    /**
     * Appends the given component in a new line, without inheriting formatting of previous siblings.
     *
     * @see ChatComponentText#appendSibling
     */
    public SkyhChatComponent appendFreshSibling(IChatComponent sibling) {
        this.siblings.add(new ChatComponentText("\n").appendSibling(sibling));
        return this;
    }

    public static class KeyValueChatComponent extends SkyhChatComponent {
        public KeyValueChatComponent(String key, String value) {
            this(key, value, ": ");
        }

        public KeyValueChatComponent(String key, String value, String separator) {
            super(key);
            appendText(separator);
            gold().appendSibling(new SkyhChatComponent(value).yellow());
        }
    }

    public static class KeyValueTooltipComponent extends SkyhChatComponent {
        public KeyValueTooltipComponent(String key, String value) {
            super(key);
            appendText(": ");
            gray().appendSibling(new SkyhChatComponent(value).yellow());
        }
    }
}
