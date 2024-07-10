package de.onyxp.onyxpannel.command.exception;

import de.onyxp.onyxpannel.Onyxpannel;
import de.onyxp.onyxpannel.util.OnyxChatComponent;
import net.minecraft.command.CommandException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {
    @Override
    default void accept(T t) {
        try {
            acceptThrows(t);
        } catch (CommandException e) {
            ChatComponentTranslation errorMsg = new ChatComponentTranslation(e.getMessage(), e.getErrorObjects());
            errorMsg.getChatStyle().setColor(EnumChatFormatting.RED);
            handleException(e, errorMsg);
        } catch (Exception e) {
            String stackTraceInfo = null;
            for (StackTraceElement traceElement : e.getStackTrace()) {
                if (traceElement.getClassName().startsWith("de.onyxp")) {
                    stackTraceInfo = traceElement.getClassName()
                            + EnumChatFormatting.WHITE + "#" + EnumChatFormatting.GRAY + traceElement.getMethodName()
                            + EnumChatFormatting.WHITE + ":" + EnumChatFormatting.GRAY + traceElement.getLineNumber();
                    break;
                }
            }
            handleException(e, new OnyxChatComponent(EnumChatFormatting.DARK_RED + "Something went wrong: " + EnumChatFormatting.RED + e
                    + (stackTraceInfo == null ? "" : EnumChatFormatting.GRAY + " (" + EnumChatFormatting.WHITE + "in " + EnumChatFormatting.GRAY + stackTraceInfo + EnumChatFormatting.GRAY + ")")));
        }
    }

    default void handleException(Exception exception, IChatComponent errorMsg) {
        Onyxpannel.getInstance().getChatHelper().sendMessage(errorMsg);
        exception.printStackTrace();
    }

    void acceptThrows(T t) throws CommandException;
}
