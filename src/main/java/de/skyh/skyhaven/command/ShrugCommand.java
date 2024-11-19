package de.skyh.skyhaven.command;

import de.skyh.skyhaven.Skyhaven;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class ShrugCommand extends CommandBase {
    private final Skyhaven main;

    public ShrugCommand(Skyhaven main) {
        this.main = main;
    }

    @Override
    public String getCommandName() {
        return "shrug";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/shrug [message]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        main.getChatHelper().sendShrug(args);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
