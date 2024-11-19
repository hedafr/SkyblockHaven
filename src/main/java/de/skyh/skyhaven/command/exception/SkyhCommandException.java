package de.skyh.skyhaven.command.exception;

import net.minecraft.command.CommandException;
import net.minecraft.util.EnumChatFormatting;

public class SkyhCommandException extends CommandException {
    private final String msg;

    public SkyhCommandException(String msg) {
        super("skyhaven.commands.generic.exception", msg);
        this.msg = msg;
    }

    @Override
    public String getLocalizedMessage() {
        return EnumChatFormatting.getTextWithoutFormattingCodes(this.msg);
    }
}
