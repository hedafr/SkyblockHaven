package de.onyxp.onyxpannel.util;

import de.onyxp.onyxpannel.Onyxpannel;
import de.onyxp.onyxpannel.config.OnyxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;

import java.util.concurrent.TimeUnit;

/**
 * @see ForgeVersion
 */
public class VersionChecker {
    /**
     * Cooldown between to update checks in minutes
     */
    private static final int CHECK_COOLDOWN = 15;
    private static final String CHANGELOG_URL = Onyxpannel.GITURL + "blob/master/CHANGELOG.md";
    private final Onyxpannel main;
    private long lastCheck;
    private String newVersion;
    private String downloadUrl;

    public VersionChecker(Onyxpannel main) {
        this.main = main;
        this.lastCheck = Minecraft.getSystemTime();
        newVersion = "[newVersion]";
        downloadUrl = Onyxpannel.GITURL + "releases";
    }

    public boolean runUpdateCheck(boolean isCommandTriggered) {
        if (isCommandTriggered || (!ForgeModContainer.disableVersionCheck && OnyxConfig.doUpdateCheck)) {
            Runnable handleResults = () -> main.getVersionChecker().handleVersionStatus(isCommandTriggered);

            long now = Minecraft.getSystemTime();

            // only re-run if last check was >CHECK_COOLDOWN minutes ago
            if (getNextCheck() < 0) { // next allowed check is "in the past", so we're good to go
                lastCheck = now;
                ForgeVersion.startVersionCheck();

                // check status after 5 seconds - hopefully that's enough to check
                new TickDelay(handleResults, 5 * 20);
                return true;
            } else {
                new TickDelay(handleResults, 1);
            }
        }
        return false;
    }

    public void handleVersionStatus(boolean isCommandTriggered) {
        ForgeVersion.CheckResult versionResult = ForgeVersion.getResult(Loader.instance().activeModContainer());
        if (versionResult.target != null) {
            newVersion = versionResult.target.toString();
            downloadUrl = Onyxpannel.GITURL + "releases/download/v" + newVersion + "/" + Onyxpannel.MODNAME.replace(" ", "") + "-" + newVersion + ".jar";
        }

        IChatComponent statusMsg = null;

        if (isCommandTriggered) {
            if (versionResult.status == ForgeVersion.Status.UP_TO_DATE) {
                // up to date
                statusMsg = new ChatComponentText("✔ You're running the latest version (" + Onyxpannel.VERSION + ").").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN));
            } else if (versionResult.status == ForgeVersion.Status.PENDING) {
                // pending
                statusMsg = new ChatComponentText("➜ Version check either failed or is still running.").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW))
                        .appendSibling(new ChatComponentText("\n ➊ Check for results again in a few seconds with " + EnumChatFormatting.GOLD + "/onyx version").setChatStyle(new ChatStyle()
                                .setColor(EnumChatFormatting.YELLOW)
                                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/onyx version"))
                                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + "/onyx version")))))
                        .appendSibling(new ChatComponentText("\n ➋ Re-run update check with " + EnumChatFormatting.GOLD + "/onyx update").setChatStyle(new ChatStyle()
                                .setColor(EnumChatFormatting.YELLOW)
                                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/onyx update"))
                                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + "/onyx update")))));
            } else if (versionResult.status == ForgeVersion.Status.FAILED) {
                // check failed
                statusMsg = new ChatComponentText("✖ Version check failed for an unknown reason. Check again in a few seconds with ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED))
                        .appendSibling(new ChatComponentText("/onyx update").setChatStyle(new ChatStyle()
                                .setColor(EnumChatFormatting.GOLD)
                                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/onyx update"))
                                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + "/onyx update")))));
            }
        }
        if (versionResult.status == ForgeVersion.Status.OUTDATED || versionResult.status == ForgeVersion.Status.BETA_OUTDATED) {
            // outdated
            IChatComponent spacer = new ChatComponentText(" ").setChatStyle(new ChatStyle().setParentStyle(null));

            IChatComponent text = new ChatComponentText("➜ New version of " + EnumChatFormatting.DARK_GREEN + Onyxpannel.MODNAME + " " + EnumChatFormatting.GREEN + "available (" + Onyxpannel.VERSION + " ➡ " + newVersion + ")\n").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN));

            IChatComponent download = new ChatComponentText("[Download]").setChatStyle(new ChatStyle()
                    .setColor(EnumChatFormatting.DARK_GREEN).setBold(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Download the latest version of " + Onyxpannel.MODNAME))));

            IChatComponent changelog = new ChatComponentText("[Changelog]").setChatStyle(new ChatStyle()
                    .setColor(EnumChatFormatting.DARK_AQUA).setBold(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, CHANGELOG_URL))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "View changelog"))));

            IChatComponent updateInstructions = new ChatComponentText("[Update instructions]").setChatStyle(new ChatStyle()
                    .setColor(EnumChatFormatting.GOLD).setBold(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/onyx updateHelp"))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + "/onyx updateHelp"))));

            IChatComponent openModsDirectory = new ChatComponentText("\n[Open Mods directory]").setChatStyle(new ChatStyle()
                    .setColor(EnumChatFormatting.GREEN).setBold(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/onyx directory"))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Open mods directory with command " + EnumChatFormatting.GOLD + "/onyx directory\n➜ Click to open mods directory"))));

            IChatComponent discord = new ChatComponentText("[OnyxX Discord]").setChatStyle(new ChatStyle()
                    .setColor(EnumChatFormatting.AQUA).setBold(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Onyxpannel.INVITE_URL))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Need help? Join the OnyxX discord!"))));

            statusMsg = text.appendSibling(download).appendSibling(spacer).appendSibling(changelog).appendSibling(spacer).appendSibling(updateInstructions).appendSibling(spacer).appendSibling(openModsDirectory).appendSibling(spacer).appendSibling(discord);
        }

        if (statusMsg != null) {
            if (isCommandTriggered) {
                main.getChatHelper().sendMessage(statusMsg);
            } else {
                IChatComponent finalStatusMsg = statusMsg;
                new TickDelay(() -> main.getChatHelper().sendMessage(finalStatusMsg), 6 * 20);
            }
        }
    }

    public long getNextCheck() {
        long cooldown = TimeUnit.MINUTES.toMillis(CHECK_COOLDOWN);
        long systemTime = Minecraft.getSystemTime();
        return cooldown - (systemTime - lastCheck);
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
