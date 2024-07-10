package de.onyxp.onyxpannel.util;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.io.File;

@SuppressWarnings("unused")
public class OnyxChatComponent extends ChatComponentText {
    public OnyxChatComponent(String msg) {
        super(msg);
    }

    public OnyxChatComponent black() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.BLACK));
        return this;
    }

    public OnyxChatComponent darkBlue() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_BLUE));
        return this;
    }

    public OnyxChatComponent darkGreen() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_GREEN));
        return this;
    }

    public OnyxChatComponent darkAqua() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_AQUA));
        return this;
    }

    public OnyxChatComponent darkRed() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_RED));
        return this;
    }

    public OnyxChatComponent darkPurple() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_PURPLE));
        return this;
    }

    public OnyxChatComponent gold() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.GOLD));
        return this;
    }

    public OnyxChatComponent gray() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.GRAY));
        return this;
    }

    public OnyxChatComponent darkGray() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_GRAY));
        return this;
    }

    public OnyxChatComponent blue() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.BLUE));
        return this;
    }

    public OnyxChatComponent green() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.GREEN));
        return this;
    }

    public OnyxChatComponent aqua() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.AQUA));
        return this;
    }

    public OnyxChatComponent red() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.RED));
        return this;
    }

    public OnyxChatComponent lightPurple() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE));
        return this;
    }

    public OnyxChatComponent yellow() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.YELLOW));
        return this;
    }

    public OnyxChatComponent white() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.WHITE));
        return this;
    }

    public OnyxChatComponent obfuscated() {
        setChatStyle(getChatStyle().setObfuscated(true));
        return this;
    }

    public OnyxChatComponent bold() {
        setChatStyle(getChatStyle().setBold(true));
        return this;
    }

    public OnyxChatComponent strikethrough() {
        setChatStyle(getChatStyle().setStrikethrough(true));
        return this;
    }

    public OnyxChatComponent underline() {
        setChatStyle(getChatStyle().setUnderlined(true));
        return this;
    }

    public OnyxChatComponent italic() {
        setChatStyle(getChatStyle().setItalic(true));
        return this;
    }

    public OnyxChatComponent reset() {
        setChatStyle(getChatStyle().setParentStyle(null).setBold(false).setItalic(false).setObfuscated(false).setUnderlined(false).setStrikethrough(false));
        return this;
    }

    public OnyxChatComponent setHover(IChatComponent hover) {
        setChatStyle(getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        return this;
    }

    public OnyxChatComponent setUrl(String url) {
        return setUrl(url, new KeyValueTooltipComponent("Click to visit", url));
    }

    public OnyxChatComponent setUrl(String url, String hover) {
        return setUrl(url, new OnyxChatComponent(hover).yellow());
    }

    public OnyxChatComponent setUrl(String url, IChatComponent hover) {
        setChatStyle(getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        setHover(hover);
        return this;
    }

    public OnyxChatComponent setOpenFile(File filePath) {
        setChatStyle(getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, filePath.getAbsolutePath())));
        setHover(new OnyxChatComponent(filePath.isFile() ? "Open " + filePath.getName() : "Open folder: " + filePath).yellow());
        return this;
    }

    public OnyxChatComponent setSuggestCommand(String command) {
        setSuggestCommand(command, true);
        return this;
    }

    public OnyxChatComponent setSuggestCommand(String command, boolean addTooltip) {
        setChatStyle(getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
        if (addTooltip) {
            setHover(new KeyValueChatComponent("Run", command, " "));
        }
        return this;
    }

    @Override
    public OnyxChatComponent appendSibling(IChatComponent component) {
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
    public OnyxChatComponent appendFreshSibling(IChatComponent sibling) {
        this.siblings.add(new ChatComponentText("\n").appendSibling(sibling));
        return this;
    }

    public static class KeyValueChatComponent extends OnyxChatComponent {
        public KeyValueChatComponent(String key, String value) {
            this(key, value, ": ");
        }

        public KeyValueChatComponent(String key, String value, String separator) {
            super(key);
            appendText(separator);
            gold().appendSibling(new OnyxChatComponent(value).yellow());
        }
    }

    public static class KeyValueTooltipComponent extends OnyxChatComponent {
        public KeyValueTooltipComponent(String key, String value) {
            super(key);
            appendText(": ");
            gray().appendSibling(new OnyxChatComponent(value).yellow());
        }
    }
}
