package de.onyxp.onyxpannel.command.exception;

import net.minecraft.command.CommandException;
import net.minecraft.util.EnumChatFormatting;

public class OnyxCommandException extends CommandException {
    private final String msg;

    public OnyxCommandException(String msg) {
        super("onyxpannel.commands.generic.exception", msg);
        this.msg = msg;
    }

    @Override
    public String getLocalizedMessage() {
        return EnumChatFormatting.getTextWithoutFormattingCodes(this.msg);
    }
}
