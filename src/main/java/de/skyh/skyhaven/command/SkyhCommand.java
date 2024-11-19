package de.skyh.skyhaven.command;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.realmsclient.util.Pair;
import de.skyh.skyhaven.Skyhaven;
import de.skyh.skyhaven.chesttracker.ChestOverviewGui;
import de.skyh.skyhaven.command.exception.ApiContactException;
import de.skyh.skyhaven.command.exception.InvalidPlayerNameException;
import de.skyh.skyhaven.command.exception.SkyhCommandException;
import de.skyh.skyhaven.config.CredentialStorage;
import de.skyh.skyhaven.config.SkyhConfig;
import de.skyh.skyhaven.config.gui.SkyhConfigBestFriendsMigration;
import de.skyh.skyhaven.config.gui.SkyhConfigGui;
import de.skyh.skyhaven.data.*;
import de.skyh.skyhaven.data.HySkyBlockStats.Profile.Pet;
import de.skyh.skyhaven.handler.DungeonCache;
import de.skyh.skyhaven.listener.skyblock.DungeonsPartyListener;
import de.skyh.skyhaven.partyfinder.RuleEditorGui;
import de.skyh.skyhaven.search.GuiSearch;
import de.skyh.skyhaven.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.*;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SkyhCommand extends CommandBase {
    private final Skyhaven main;
    private DungeonsPartyListener dungeonsPartyListener;

    public SkyhCommand(Skyhaven main) {
        this.main = main;
    }

    @Override
    public String getCommandName() {
        return "skyh";
    }

    @Override
    public List<String> getCommandAliases() {
        List<String> aliases = new ArrayList<>();
        if (StringUtils.isNotEmpty(SkyhConfig.skyhCmdAlias)) {
            aliases.add(SkyhConfig.skyhCmdAlias);
        }
        return aliases;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName() + " help";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "Tried to say " + EnumChatFormatting.YELLOW + getCommandName() + EnumChatFormatting.GOLD + "? Use " + EnumChatFormatting.YELLOW + getCommandName() + " say [optional text]" + EnumChatFormatting.GOLD + " instead.\n"
                    + "Tried to use the command " + EnumChatFormatting.YELLOW + "/" + getCommandName() + EnumChatFormatting.GOLD + "? Use " + EnumChatFormatting.YELLOW + "/" + getCommandName() + " help" + EnumChatFormatting.GOLD + " for a list of available commands");
        }
        //region sub commands: Best friends, friends & other players
        else if (args[0].equalsIgnoreCase("say")) {
            // work-around so you can still say 'skyh' in chat without triggering the client-side command
            String msg = CommandBase.buildString(args, 1);
            Minecraft.getMinecraft().thePlayer.sendChatMessage(getCommandName() + (!msg.isEmpty() ? " " + msg : ""));
        } else if (args[0].equalsIgnoreCase("stalk")
                || args[0].equalsIgnoreCase("s")
                || args[0].equalsIgnoreCase("askPolitelyWhereTheyAre")) {
            handleStalking(args);
        } else if (args[0].equalsIgnoreCase("add")
                || args[0].equalsIgnoreCase("list")
                || args[0].equalsIgnoreCase("online")
                || args[0].equalsIgnoreCase("nameChangeCheck")) {
            handleBestFriendSubcommands();
        } else if (args[0].equalsIgnoreCase("remove")) {
            handleBestFriendRemove(args);
        } else if (args[0].equalsIgnoreCase("bestfriends") || args[0].equalsIgnoreCase("bestfriend")) {
            displayGuiScreen(new SkyhConfigBestFriendsMigration());
        } else if (args[0].equalsIgnoreCase("I-read-the-login-logout-notification-changes")) {
            main.getConfig().acknowledgeLoginLogoutNotificationChanges();
            SkyhChatComponent confirmationMsg = new SkyhChatComponent("[§2Skyhaven§a] The 'login & logout notification filter' removal info message will no longer be shown.").green();
            if (main.getFriendsHandler().getBestFriendsListSize() > 0) {
                confirmationMsg.appendSibling(new SkyhChatComponent("\nHowever, don't forget to migrate your Skyhaven best friends by using §4/skyh bestfriends§c!").red())
                        .setSuggestCommand("/skyh bestfriends");
            }
            main.getChatHelper().sendMessage(confirmationMsg);
        }
        // + toggle (= alias for config)
        //endregion
        //region sub commands: SkyBlock
        else if (args[0].equalsIgnoreCase("stalkskyblock") || args[0].equalsIgnoreCase("skyblockstalk")
                || args[0].equalsIgnoreCase("ss")
                || args[0].equalsIgnoreCase("stalksb") || args[0].equalsIgnoreCase("sbstalk")
                || args[0].equalsIgnoreCase("askPolitelyAboutTheirSkyBlockProgress")) {
            handleStalkingSkyBlock(args);
        } else if (args[0].equalsIgnoreCase("chestAnalyzer") || args[0].equalsIgnoreCase("chestAnalyser") || args[0].equalsIgnoreCase("analyzeChests") || args[0].equalsIgnoreCase("analyseChests")) {
            handleAnalyzeChests(args);
        } else if (args[0].equalsIgnoreCase("analyzeIsland") || args[0].equalsIgnoreCase("analyseIsland")) {
            handleAnalyzeIsland(sender);
        } else if (args[0].equalsIgnoreCase("waila") || args[0].equalsIgnoreCase("whatAmILookingAt")) {
            boolean showAllInfo = SkyhConfig.keepFullWailaInfo();
            if (args.length == 2) {
                if (args[1].equalsIgnoreCase("all")) {
                    showAllInfo = true;
                } else if (args[1].equalsIgnoreCase("main")) {
                    showAllInfo = false;
                }
            }
            handleWhatAmILookingAt(sender, showAllInfo);
        } else if (args[0].equalsIgnoreCase("dungeon") || args[0].equalsIgnoreCase("dung")
                || /* dungeon party: */ args[0].equalsIgnoreCase("dp")
                || /* dungeon party finder rules: */ args[0].equalsIgnoreCase("dr")) {
            handleDungeon(args);
        }
        //endregion
        //region sub-commands: miscellaneous
        else if (args[0].equalsIgnoreCase("config")) {
            main.getConfig().theyOpenedTheConfigGui();
            displayGuiScreen(new SkyhConfigGui(buildString(args, 1)));
        } else if (args[0].equalsIgnoreCase("search")) {
            displayGuiScreen(new GuiSearch(CommandBase.buildString(args, 1)));
        } else if (args[0].equalsIgnoreCase("guiscale")) {
            handleGuiScale(args);
        } else if (args[0].equalsIgnoreCase("rr")) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/r " + CommandBase.buildString(args, 1));
        } else if (args[0].equalsIgnoreCase("shrug")) {
            main.getChatHelper().sendShrug(buildString(args, 1));
        } else if (args[0].equalsIgnoreCase("apikey")) {
            handleApiKey(args);
        } else if (args[0].equalsIgnoreCase("whatyearisit") || args[0].equalsIgnoreCase("year")) {
            long year = ((System.currentTimeMillis() - 1560275700000L) / (TimeUnit.HOURS.toMillis(124))) + 1;
            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, "It is SkyBlock year " + EnumChatFormatting.GOLD + year + EnumChatFormatting.YELLOW + ".");
        } else if (args[0].equalsIgnoreCase("worldage") || args[0].equalsIgnoreCase("serverage")) {
            handleWorldAge(args);
        } else if (args[0].equalsIgnoreCase("discord")) {
            main.getChatHelper().sendMessage(new SkyhChatComponent("➜ Need help with " + EnumChatFormatting.GOLD + Skyhaven.MODNAME + EnumChatFormatting.GREEN + "? Do you have any questions, suggestions or other feedback? " + EnumChatFormatting.GOLD + "Join the SkyhX discord!").green().setUrl(Skyhaven.INVITE_URL));
        }
        //endregion
        //region sub-commands: update mod
        else if (args[0].equalsIgnoreCase("update")) {
            handleUpdate(args);
        } else if (args[0].equalsIgnoreCase("updateHelp")) {
            handleUpdateHelp();
        } else if (args[0].equalsIgnoreCase("version")) {
            main.getVersionChecker().handleVersionStatus(true);
        } else if (args[0].equalsIgnoreCase("directory") || args[0].equalsIgnoreCase("folder")) {
            File directory = null;
            if (args.length > 1) {
                if (args[1].startsWith("c")) {
                    directory = main.getConfigDirectory();
                } else if (args[1].startsWith("m")) {
                    directory = main.getModsDirectory();
                }
            }
            if (directory == null) {
                main.getChatHelper().sendMessage(new SkyhChatComponent("[§2Skyhaven§a] open directory:").green()
                        .appendFreshSibling(new SkyhChatComponent(" §6➊ §a/config/skyhaven/").setSuggestCommand("/" + getCommandName() + " directory config"))
                        .appendFreshSibling(new SkyhChatComponent(" §6➋ §a/mods/").setSuggestCommand("/" + getCommandName() + " directory mods")));
                return;
            }
            try {
                Desktop.getDesktop().open(directory);
            } catch (IOException e) {
                e.printStackTrace();
                throw new SkyhCommandException("✖ An error occurred trying to open the mod's directory. I guess you have to open it manually ¯\\_(ツ)_/¯");
            }
        }
        //endregion
        // help
        else if (args[0].equalsIgnoreCase("help")) {
            sendCommandUsage(sender);
        }
        // fix: run server-side command /m with optional arguments
        else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
            String cmdArgs = CommandBase.buildString(args, 1);
            if (cmdArgs.length() > 0) {
                cmdArgs = " " + cmdArgs;
            }
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/" + SkyhConfig.skyhCmdAlias + cmdArgs);
        }
        // "catch-all" remaining sub-commands
        else {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Command " + EnumChatFormatting.DARK_RED + "/" + getCommandName() + " " + args[0] + EnumChatFormatting.RED + " doesn't exist. Use " + EnumChatFormatting.DARK_RED + "/" + getCommandName() + " help " + EnumChatFormatting.RED + "to show command usage."
                    + (StringUtils.isNotEmpty(SkyhConfig.skyhCmdAlias) ? "\n" + EnumChatFormatting.RED + "Are you trying to use a server-side command " + EnumChatFormatting.DARK_RED + "/" + SkyhConfig.skyhCmdAlias + EnumChatFormatting.RED + "? Use " + EnumChatFormatting.DARK_RED + "/" + SkyhConfig.skyhCmdAlias + " cmd [arguments] " + EnumChatFormatting.RED + "instead." : ""));
        }
    }

    //region sub commands: Best friends, friends & other players
    private void handleStalking(String[] args) throws CommandException {
        if (!CredentialStorage.isSkyhValid) {
            throw new SkyhCommandException("[Skyhaven] You haven't set your Hypixel API key yet or the API key is invalid. Use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey " + EnumChatFormatting.RED + "to manually set your existing API key.");
        }
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " stalk <playerName>");
        } else if (Utils.isInvalidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        } else {
            String playerName = args[1];
            main.getChatHelper().sendMessage(EnumChatFormatting.GRAY, "Stalking " + EnumChatFormatting.WHITE + playerName + EnumChatFormatting.GRAY + ". This may take a few seconds.");

            // fetch player uuid
            ApiUtils.fetchFriendData(playerName, stalkedPlayer -> {
                if (stalkedPlayer == null) {
                    throw new ApiContactException("Mojang", "couldn't stalk " + EnumChatFormatting.DARK_RED + playerName);
                } else if (stalkedPlayer.equals(Friend.FRIEND_NOT_FOUND)) {
                    throw new PlayerNotFoundException("There is no player with the name " + EnumChatFormatting.DARK_RED + playerName + EnumChatFormatting.RED + ".");
                } else {
                    // ... then stalk the player
                    stalkPlayer(stalkedPlayer);
                }
            });
        }
    }

    private void stalkPlayer(Friend stalkedPlayer) {
        ApiUtils.fetchPlayerStatus(stalkedPlayer, hyStalking -> {
            if (hyStalking != null && hyStalking.isSuccess()) {
                HyStalkingData.HySession session = hyStalking.getSession();
                if (session.isOnline()) {
                    main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD + stalkedPlayer.getName()
                            + EnumChatFormatting.YELLOW + " is currently playing " + EnumChatFormatting.GOLD + (session.getGameType() != null ? session.getGameType() : EnumChatFormatting.ITALIC + "something (but hiding their game mode)") + EnumChatFormatting.YELLOW
                            + (session.getMode() != null ? ": " + EnumChatFormatting.GOLD + session.getMode() : "")
                            + (session.getMap() != null ? EnumChatFormatting.YELLOW + " (Map: " + EnumChatFormatting.GOLD + session.getMap() + EnumChatFormatting.YELLOW + ")" : ""));
                } else {
                    ApiUtils.fetchHyPlayerDetails(stalkedPlayer, hyPlayerData -> {
                        if (hyPlayerData == null) {
                            throw new ApiContactException("Hypixel", "couldn't stalk " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + " but they appear to be offline currently.");
                        } else if (hyPlayerData.hasNeverJoinedHypixel()) {
                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD + stalkedPlayer.getName() + EnumChatFormatting.YELLOW + " has " + EnumChatFormatting.GOLD + "never " + EnumChatFormatting.YELLOW + "been on Hypixel (or might be nicked).");
                        } else if (hyPlayerData.isHidingOnlineStatus()) {
                            main.getChatHelper().sendMessage(new ChatComponentText(hyPlayerData.getPlayerNameFormatted()).appendSibling(new ChatComponentText(" is hiding their online status from the Hypixel API.").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW))));
                        } else if (hyPlayerData.hasNeverLoggedOut()) {
                            Pair<String, String> lastOnline = Utils.getDurationAsWords(hyPlayerData.getLastLogin());

                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, hyPlayerData.getPlayerNameFormatted() + EnumChatFormatting.YELLOW + " was last online " + EnumChatFormatting.GOLD + lastOnline.first() + EnumChatFormatting.YELLOW + " ago"
                                    + (lastOnline.second() != null ? " (" + EnumChatFormatting.GOLD + lastOnline.second() + EnumChatFormatting.YELLOW + ")" : "") + ".");
                        } else if (hyPlayerData.getLastLogin() > hyPlayerData.getLastLogout()) {
                            // player is logged in but is hiding their session details from API (My Profile > API settings > Online Status)
                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD + hyPlayerData.getPlayerNameFormatted() + EnumChatFormatting.YELLOW + " is currently playing " + EnumChatFormatting.GOLD + hyPlayerData.getLastGame() + "\n" + EnumChatFormatting.DARK_GRAY + "(" + hyPlayerData.getPlayerName() + " hides their session details from the API so that only their current game mode is visible)");
                        } else {
                            Pair<String, String> lastOnline = Utils.getDurationAsWords(hyPlayerData.getLastLogout());

                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, hyPlayerData.getPlayerNameFormatted() + EnumChatFormatting.YELLOW + " is " + EnumChatFormatting.GOLD + "offline" + EnumChatFormatting.YELLOW + " for " + EnumChatFormatting.GOLD + lastOnline.first() + EnumChatFormatting.YELLOW
                                    + ((lastOnline.second() != null || hyPlayerData.getLastGame() != null) ? (" ("
                                    + (lastOnline.second() != null ? EnumChatFormatting.GOLD + lastOnline.second() + EnumChatFormatting.YELLOW : "") // = last online date
                                    + (lastOnline.second() != null && hyPlayerData.getLastGame() != null ? "; " : "") // = delimiter
                                    + (hyPlayerData.getLastGame() != null ? "last played gamemode: " + EnumChatFormatting.GOLD + hyPlayerData.getLastGame() + EnumChatFormatting.YELLOW : "") // = last gamemode
                                    + ")") : "") + ".");
                        }
                    });
                }
            } else {
                String cause = (hyStalking != null) ? hyStalking.getCause() : null;
                throw new ApiContactException("Hypixel", "couldn't stalk " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + (cause != null ? " (Reason: " + EnumChatFormatting.DARK_RED + cause + EnumChatFormatting.RED + ")" : "") + ".");
            }
        });
    }

    private void handleBestFriendSubcommands() {
        main.getChatHelper().sendMessage(new SkyhChatComponent("[" + EnumChatFormatting.DARK_RED + Skyhaven.MODNAME + EnumChatFormatting.RED + "] The 'best friends list' feature has been removed from this mod.").red()
                .appendSibling(new SkyhChatComponent(" Run " + EnumChatFormatting.GOLD + "/skyh bestfriends " + EnumChatFormatting.YELLOW + "to migrate your best friends list").yellow())
                .setSuggestCommand("/skyh bestfriends"));
        Minecraft.getMinecraft().thePlayer.playSound("mob.villager.no", Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER), 1.4f);
    }

    private void handleBestFriendRemove(String[] args) throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " remove <playerName>");
        } else if (Utils.isInvalidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        }
        String username = args[1];
        boolean removed = main.getFriendsHandler().removeBestFriend(username);
        if (removed) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Removed " + EnumChatFormatting.DARK_GREEN + username + EnumChatFormatting.GREEN + " from best friends list.");
        } else {
            throw new SkyhCommandException(EnumChatFormatting.DARK_RED + username + EnumChatFormatting.RED + " isn't a best friend.");
        }
    }
    //endregion

    //region sub commands: SkyBlock
    private void handleStalkingSkyBlock(String[] args) throws CommandException {
        if (!CredentialStorage.isSkyhValid) {
            throw new SkyhCommandException("[Skyhaven] You haven't set your Hypixel API key yet or the API key is invalid. Use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey " + EnumChatFormatting.RED + "to manually set your existing API key.");
        }
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " skyblockstalk <playerName>");
        } else if (Utils.isInvalidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        } else {
            String playerName = args[1];
            main.getChatHelper().sendMessage(EnumChatFormatting.GRAY, "Stalking " + EnumChatFormatting.WHITE + playerName + EnumChatFormatting.GRAY + "'s SkyBlock stats. This may take a few seconds.");
            // fetch player uuid
            ApiUtils.fetchFriendData(playerName, stalkedPlayer -> {
                if (stalkedPlayer == null) {
                    throw new ApiContactException("Mojang", "couldn't stalk " + EnumChatFormatting.DARK_RED + playerName);
                } else if (stalkedPlayer.equals(Friend.FRIEND_NOT_FOUND)) {
                    throw new PlayerNotFoundException("There is no player with the name " + EnumChatFormatting.DARK_RED + playerName + EnumChatFormatting.RED + ".");
                } else {
                    // ... then stalk the player
                    stalkSkyBlockStats(stalkedPlayer);
                }
            });
        }
    }

    private void stalkSkyBlockStats(Friend stalkedPlayer) {
        ApiUtils.fetchSkyBlockStats(stalkedPlayer, hySBStalking -> {
            if (hySBStalking != null && hySBStalking.isSuccess()) {
                HySkyBlockStats.Profile activeProfile = hySBStalking.getActiveProfile(stalkedPlayer.getUuid());

                if (activeProfile == null) {
                    throw new SkyhCommandException("Looks like " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + " hasn't played SkyBlock yet.");
                }

                String highestSkill = null;
                int highestLevel = -1;

                SkyhChatComponent skillLevels = new SkyhChatComponent("Skill levels:").gold();
                HySkyBlockStats.Profile.Member member = activeProfile.getMember(stalkedPlayer.getUuid());
                int skillLevelsSum = 0;
                for (Map.Entry<XpTables.Skill, Integer> entry : member.getSkills().entrySet()) {
                    String skill = Utils.fancyCase(entry.getKey().name());
                    int level = entry.getValue();
                    String skillLevel = SkyhConfig.useRomanNumerals() ? Utils.convertArabicToRoman(level) : String.valueOf(level);
                    skillLevels.appendFreshSibling(new SkyhChatComponent.KeyValueTooltipComponent(skill, skillLevel));

                    if (level > highestLevel) {
                        highestSkill = skill;
                        highestLevel = level;
                    }
                    if (!skill.equals("Runecrafting") && !skill.equals("Social")) {
                        skillLevelsSum += level;
                    }
                }

                // output inspired by /profiles hover

                // coins:
                String coinsBankAndPurse = (activeProfile.getCoinBank() >= 0) ? Utils.formatNumberWithAbbreviations(activeProfile.getCoinBank() + member.getCoinPurse()) : Utils.formatNumberWithAbbreviations(member.getCoinPurse()) + " - purse only, bank API access disabled";
                Pair<String, String> fancyFirstJoined = member.getFancyFirstJoined();

                SkyhChatComponent wealthHover = new SkyhChatComponent("Accessible coins:").gold()
                        .appendFreshSibling(new SkyhChatComponent.KeyValueTooltipComponent("Purse", Utils.formatNumberWithAbbreviations(member.getCoinPurse())))
                        .appendFreshSibling(new SkyhChatComponent.KeyValueTooltipComponent("Bank", (activeProfile.getCoinBank() != -1) ? Utils.formatNumberWithAbbreviations(activeProfile.getCoinBank()) : "API access disabled"));
                if (activeProfile.coopCount() > 0) {
                    wealthHover.appendFreshSibling(new ChatComponentText(" "));
                    wealthHover.appendFreshSibling(new SkyhChatComponent.KeyValueTooltipComponent("Co-op members", String.valueOf(activeProfile.coopCount())));
                    wealthHover.appendFreshSibling(new SkyhChatComponent.KeyValueTooltipComponent("Co-ops' purses sum", Utils.formatNumberWithAbbreviations(activeProfile.getCoopCoinPurses(stalkedPlayer.getUuid()))));
                }

                String gameModeIcon = activeProfile.getGameModeIcon();
                SkyhChatComponent sbStats = new SkyhChatComponent("SkyBlock stats of " + stalkedPlayer.getName() + EnumChatFormatting.RESET + EnumChatFormatting.GRAY + " (" + (gameModeIcon.isEmpty() ? "" : gameModeIcon + EnumChatFormatting.GRAY + ", ") + activeProfile.getCuteName() + ")").gold().bold().setUrl("https://sky.shiiyu.moe/stats/" + stalkedPlayer.getName() + "/" + activeProfile.getCuteName(), "Click to view SkyBlock stats on sky.shiiyu.moe")
                        .appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent("Coins", coinsBankAndPurse).setHover(wealthHover));
                // highest skill + skill average:
                if (highestSkill != null) {
                    if (highestLevel == 0) {
                        sbStats.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent("Highest Skill", "All skills level 0"));
                    } else {
                        String highestSkillLevel = SkyhConfig.useRomanNumerals() ? Utils.convertArabicToRoman(highestLevel) : String.valueOf(highestLevel);
                        sbStats.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent("Highest Skill", highestSkill + " " + highestSkillLevel).setHover(skillLevels));
                    }
                    double skillAverage = XpTables.Skill.getSkillAverage(skillLevelsSum);
                    sbStats.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent("Skill average", String.format("%.1f", skillAverage))
                            .setHover(new SkyhChatComponent("Average skill level over all non-cosmetic skills\n(all except Runecrafting and Social)").gray()));
                } else {
                    sbStats.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent("Highest Skill", "API access disabled"));
                }

                // slayer levels:
                StringBuilder slayerLevels = new StringBuilder();
                StringBuilder slayerLevelsTooltip = new StringBuilder();
                SkyhChatComponent slayerLevelsTooltipComponent = new SkyhChatComponent("Slayer bosses:").gold();
                for (Map.Entry<XpTables.Slayer, Integer> entry : member.getSlayerLevels().entrySet()) {
                    String slayerBoss = Utils.fancyCase(entry.getKey().name());
                    if (slayerLevels.length() > 0) {
                        slayerLevels.append(EnumChatFormatting.GRAY).append(" | ").append(EnumChatFormatting.YELLOW);
                        slayerLevelsTooltip.append(EnumChatFormatting.DARK_GRAY).append(" | ").append(EnumChatFormatting.WHITE);
                    }
                    slayerLevelsTooltip.append(slayerBoss);
                    int level = entry.getValue();

                    String slayerLevel = (level > 0) ? (SkyhConfig.useRomanNumerals() ? Utils.convertArabicToRoman(level) : String.valueOf(level)) : "0";
                    slayerLevels.append(slayerLevel);
                }
                SkyhChatComponent slayerLevelsComponent = new SkyhChatComponent.KeyValueChatComponent("Slayer levels", slayerLevels.toString());
                slayerLevelsComponent.setHover(slayerLevelsTooltipComponent.appendFreshSibling(new SkyhChatComponent(slayerLevelsTooltip.toString()).white()));
                sbStats.appendFreshSibling(slayerLevelsComponent);

                // dungeons:
                SkyhChatComponent dungeonsComponent = null;
                HySkyBlockStats.Profile.Dungeons dungeons = member.getDungeons();
                boolean hasPlayedDungeons = dungeons != null && dungeons.hasPlayed();
                if (hasPlayedDungeons) {
                    SkyhChatComponent dungeonHover = new SkyhChatComponent("Dungeoneering").gold().bold();

                    DataHelper.DungeonClass selectedClass = dungeons.getSelectedClass();
                    String selectedDungClass = "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "no class selected";
                    if (selectedClass != null) {
                        int selectedClassLevel = dungeons.getSelectedClassLevel();
                        selectedDungClass = selectedClass.getName() + " " + (SkyhConfig.useRomanNumerals() ? Utils.convertArabicToRoman(selectedClassLevel) : selectedClassLevel);
                    }
                    dungeonsComponent = new SkyhChatComponent.KeyValueChatComponent("Dungeoneering", selectedDungClass)
                            .setHover(dungeonHover);


                    // for each class (Archer, Berserk, ...)
                    Map<DataHelper.DungeonClass, Integer> classLevels = dungeons.getClassLevels();
                    if (classLevels != null && !classLevels.isEmpty()) {
                        dungeonHover.appendFreshSibling(new SkyhChatComponent("Classes:").gold());
                        for (Map.Entry<DataHelper.DungeonClass, Integer> classEntry : classLevels.entrySet()) {
                            String classLevel = (SkyhConfig.useRomanNumerals() ? Utils.convertArabicToRoman(classEntry.getValue()) : String.valueOf(classEntry.getValue()));
                            dungeonHover.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent((classEntry.getKey() == selectedClass ? "➜ " : "   ") + classEntry.getKey().getName(), classLevel));
                        }
                    }

                    // for each dungeon type (Catacombs, ...)
                    Map<String, HySkyBlockStats.Profile.Dungeons.Type> dungeonTypes = dungeons.getDungeonTypes();
                    if (dungeonTypes != null && !dungeonTypes.isEmpty()) {
                        // for each dungeon type: catacombs, ...
                        for (Map.Entry<String, HySkyBlockStats.Profile.Dungeons.Type> dungeonTypeEntry : dungeonTypes.entrySet()) {
                            // dungeon type entry for chat
                            HySkyBlockStats.Profile.Dungeons.Type dungeonType = dungeonTypeEntry.getValue();
                            if (!dungeonType.hasPlayed()) {
                                // never played this dungeon type
                                continue;
                            }
                            String dungeonTypeName = Utils.fancyCase(dungeonTypeEntry.getKey());
                            boolean isMasterFloor = dungeonTypeName.startsWith("Master ");
                            dungeonsComponent.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent("   " + dungeonTypeName, dungeonType.getSummary(isMasterFloor)))
                                    .setHover(dungeonHover);
                            // dungeon type entry for tooltip
                            if (isMasterFloor) {
                                dungeonHover.appendFreshSibling(new SkyhChatComponent(dungeonTypeName).gold());
                            } else {
                                // non-master dungeon
                                int dungeonTypeLevel = dungeonTypeEntry.getValue().getLevel();
                                dungeonHover.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent(dungeonTypeName, "Level " + (SkyhConfig.useRomanNumerals() ? Utils.convertArabicToRoman(dungeonTypeLevel) : dungeonTypeLevel)));
                            }

                            // for each floor
                            SortedMap<String, StringBuilder> floorStats = new TreeMap<>();
                            // ... add completed floors:
                            if (dungeonType.getTierCompletions() != null) {
                                for (Map.Entry<String, Integer> floorCompletions : dungeonType.getTierCompletions().entrySet()) {
                                    StringBuilder floorSummary = new StringBuilder();
                                    floorStats.put(floorCompletions.getKey(), floorSummary);
                                    // completed floor count:
                                    floorSummary.append(floorCompletions.getValue());
                                }
                            }
                            // ... add played floors
                            Map<String, Integer> dungeonTypeTimesPlayed = dungeonType.getTimesPlayed();
                            if (dungeonTypeTimesPlayed != null) {
                                for (Map.Entry<String, Integer> floorPlayed : dungeonTypeTimesPlayed.entrySet()) {
                                    StringBuilder floorSummary = floorStats.get(floorPlayed.getKey());
                                    if (floorSummary == null) {
                                        // hasn't beaten this floor, but already attempted it
                                        floorSummary = new StringBuilder("0");
                                        floorStats.put(floorPlayed.getKey(), floorSummary);
                                    }
                                    // played floor count:
                                    floorSummary.append(EnumChatFormatting.DARK_GRAY).append(" / ").append(EnumChatFormatting.YELLOW).append(floorPlayed.getValue());
                                }
                            } else {
                                // missing value for attempted floors, only show completed floors
                                for (StringBuilder floorSummary : floorStats.values()) {
                                    floorSummary.append(EnumChatFormatting.DARK_GRAY).append(" / ").append(EnumChatFormatting.YELLOW).append(EnumChatFormatting.OBFUSCATED).append("#");
                                }
                            }
                            // ... add best scores
                            if (dungeonType.getBestScore() != null) {
                                for (Map.Entry<String, Integer> bestScores : dungeonType.getBestScore().entrySet()) {
                                    StringBuilder floorSummary = floorStats.getOrDefault(bestScores.getKey(), new StringBuilder());
                                    // best floor score:
                                    floorSummary.append(EnumChatFormatting.DARK_GRAY).append(" (").append(EnumChatFormatting.WHITE).append(bestScores.getValue()).append(EnumChatFormatting.DARK_GRAY).append(")");
                                }
                            }

                            // add floor stats to dungeon type:
                            for (Map.Entry<String, StringBuilder> floorInfo : floorStats.entrySet()) {
                                dungeonHover.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent("   Floor " + floorInfo.getKey(), floorInfo.getValue().toString()));
                            }
                        }
                        dungeonHover.appendFreshSibling(new SkyhChatComponent(" Floor nr: completed / total floors (best score)").gray().italic());
                    }
                }
                if (!hasPlayedDungeons) {
                    dungeonsComponent = new SkyhChatComponent.KeyValueChatComponent("Dungeons", EnumChatFormatting.ITALIC + "never played");
                }
                sbStats.appendFreshSibling(dungeonsComponent);

                // pets:
                Pet activePet = null;
                Pet bestPet = null;
                StringBuilder pets = new StringBuilder();
                List<Pet> memberPets = member.getPets();
                int showPetsLimit = Math.min(16, memberPets.size());
                for (int i = 0; i < showPetsLimit; i++) {
                    Pet pet = memberPets.get(i);
                    if (pet.isActive()) {
                        activePet = pet;
                    } else {
                        if (activePet == null && bestPet == null && pets.length() == 0) {
                            // no active pet, display highest pet instead
                            bestPet = pet;
                            continue;
                        } else if (pets.length() > 0) {
                            pets.append("\n");
                        }
                        pets.append(pet.toFancyString());
                    }
                }
                int remainingPets = memberPets.size() - showPetsLimit;
                if (remainingPets > 0 && pets.length() > 0) {
                    pets.append("\n").append(EnumChatFormatting.GRAY).append(" + ").append(remainingPets).append(" other pets");
                }
                SkyhChatComponent petsComponent = null;
                if (activePet != null) {
                    petsComponent = new SkyhChatComponent.KeyValueChatComponent("Active Pet", activePet.toFancyString());
                } else if (bestPet != null) {
                    petsComponent = new SkyhChatComponent.KeyValueChatComponent("Best Pet", bestPet.toFancyString());
                }
                if (pets.length() > 0 && petsComponent != null) {
                    petsComponent.setHover(new SkyhChatComponent("Other pets:").gold().bold().appendFreshSibling(new SkyhChatComponent(pets.toString())));
                }
                if (petsComponent == null) {
                    petsComponent = new SkyhChatComponent.KeyValueChatComponent("Pet", "none");
                }
                sbStats.appendFreshSibling(petsComponent);

                // minions:
                Pair<Integer, Integer> uniqueMinionsData = activeProfile.getUniqueMinions();
                String uniqueMinions = String.valueOf(uniqueMinionsData.first());
                String uniqueMinionsHoverText = null;
                if (uniqueMinionsData.second() > activeProfile.coopCount()) {
                    // all players have their unique minions api access disabled
                    uniqueMinions = "API access disabled";
                } else if (uniqueMinionsData.second() > 0) {
                    // at least one player has their unique minions api access disabled
                    uniqueMinions += EnumChatFormatting.GRAY + " or more";
                    uniqueMinionsHoverText = "" + EnumChatFormatting.WHITE + uniqueMinionsData.second() + " out of " + (activeProfile.coopCount() + 1) + EnumChatFormatting.GRAY + " Co-op members have disabled API access, so some unique minions may be missing";
                }

                SkyhChatComponent.KeyValueChatComponent uniqueMinionsComponent = new SkyhChatComponent.KeyValueChatComponent("Unique Minions", uniqueMinions);
                if (uniqueMinionsHoverText != null) {
                    uniqueMinionsComponent.setHover(new SkyhChatComponent(uniqueMinionsHoverText).gray());
                }
                sbStats.appendFreshSibling(uniqueMinionsComponent);
                // fairy souls:
                sbStats.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent("Fairy Souls", (member.getFairySoulsCollected() >= 0) ? String.valueOf(member.getFairySoulsCollected()) : "API access disabled"));
                // profile age:
                sbStats.appendFreshSibling(new SkyhChatComponent.KeyValueChatComponent("Profile age", fancyFirstJoined.first()).setHover(new SkyhChatComponent.KeyValueTooltipComponent("Join date", (fancyFirstJoined.second() == null ? "today" : fancyFirstJoined.second()))));

                main.getChatHelper().sendMessage(sbStats);
            } else {
                String cause = (hySBStalking != null) ? hySBStalking.getCause() : null;
                String reason = "";
                if (cause != null) {
                    reason = " (Reason: " + EnumChatFormatting.DARK_RED + cause + EnumChatFormatting.RED + ")";
                }
                throw new ApiContactException("Hypixel", "couldn't stalk " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + reason + ".");
            }
        });
    }

    private void handleAnalyzeChests(String[] args) {
        if (args.length == 1) {
            boolean enabledChestTracker = main.enableChestTracker();
            if (enabledChestTracker) {
                // chest tracker wasn't enabled before, now it is
                String analyzeCommand = "/" + getCommandName() + " analyzeChests";
                if (SkyhConfig.chestAnalyzerShowCommandUsage) {
                    main.getChatHelper().sendMessage(new SkyhChatComponent("Enabled chest tracker! You can now...").green()
                            .appendFreshSibling(new SkyhChatComponent(EnumChatFormatting.GREEN + "  ❶ " + EnumChatFormatting.YELLOW + "add chests on your island by opening them; deselect chests by Sneaking + Right Click.").yellow())
                            .appendFreshSibling(new SkyhChatComponent(EnumChatFormatting.GREEN + "  ❷ " + EnumChatFormatting.YELLOW + "use " + EnumChatFormatting.GOLD + analyzeCommand + EnumChatFormatting.YELLOW + " again to run the chest analysis.").yellow().setSuggestCommand(analyzeCommand))
                            .appendFreshSibling(new SkyhChatComponent("     (You can search for an item inside your chests by double clicking its analysis row)").gray().setSuggestCommand(analyzeCommand))
                            .appendFreshSibling(new SkyhChatComponent(EnumChatFormatting.GREEN + "  ❸ " + EnumChatFormatting.YELLOW + "use " + EnumChatFormatting.GOLD + analyzeCommand + " stop" + EnumChatFormatting.YELLOW + " to stop the chest tracker and clear current results.").yellow().setSuggestCommand(analyzeCommand + " stop")));
                } else {
                    main.getChatHelper().sendMessage(new SkyhChatComponent("Enabled chest tracker! " + EnumChatFormatting.GRAY + "Run " + analyzeCommand + " again to run the chest analysis.").green().setSuggestCommand(analyzeCommand));
                }
            } else {
                // chest tracker was already enabled, open analysis GUI
                main.getChestTracker().analyzeResults();
                new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(new ChestOverviewGui(main)), 1);
            }
        } else if (args.length == 2 && args[1].equalsIgnoreCase("stop")) {
            boolean disabledChestTracker = main.disableChestTracker();
            if (disabledChestTracker) {
                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Disabled chest tracker and cleared chest cache!");
            } else {
                main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, "Chest tracker wasn't even enabled...");
            }
        } else {
            String analyzeCommand = "/" + getCommandName() + " analyzeChests";
            main.getChatHelper().sendMessage(new SkyhChatComponent(Skyhaven.MODNAME + " chest tracker & analyzer").gold().bold()
                    .appendFreshSibling(new SkyhChatComponent("Use " + EnumChatFormatting.GOLD + analyzeCommand + EnumChatFormatting.YELLOW + " to start tracking chests on your island! " + EnumChatFormatting.GREEN + "Then you can...").yellow().setSuggestCommand(analyzeCommand))
                    .appendFreshSibling(new SkyhChatComponent(EnumChatFormatting.GREEN + "  ❶ " + EnumChatFormatting.YELLOW + "add chests by opening them; deselect chests by Sneaking + Right Click.").yellow())
                    .appendFreshSibling(new SkyhChatComponent(EnumChatFormatting.GREEN + "  ❷ " + EnumChatFormatting.YELLOW + "use " + EnumChatFormatting.GOLD + analyzeCommand + EnumChatFormatting.YELLOW + " again to run the chest analysis.").yellow().setSuggestCommand(analyzeCommand))
                    .appendFreshSibling(new SkyhChatComponent(EnumChatFormatting.GREEN + "  ❸ " + EnumChatFormatting.YELLOW + "use " + EnumChatFormatting.GOLD + analyzeCommand + " stop" + EnumChatFormatting.YELLOW + " to stop the chest tracker and clear current results.").yellow().setSuggestCommand(analyzeCommand + " stop")));
        }
    }

    private void handleAnalyzeIsland(ICommandSender sender) {
        this.main.getAnalyzeIslandTracker().analyzeIsland(sender.getEntityWorld());
    }

    private void handleWhatAmILookingAt(ICommandSender sender, boolean showAllInfo) {
        MovingObjectPosition lookingAt = Minecraft.getMinecraft().objectMouseOver;
        if (lookingAt != null) {
            switch (lookingAt.typeOfHit) {
                case BLOCK: {
                    TileEntity te = sender.getEntityWorld().getTileEntity(lookingAt.getBlockPos());
                    if (te instanceof TileEntitySkull) {
                        TileEntitySkull skull = (TileEntitySkull) te;
                        if (skull.getSkullType() != 3) {
                            // non-player skull, abort!
                            break;
                        }
                        NBTTagCompound nbt = new NBTTagCompound();
                        skull.writeToNBT(nbt);
                        // is a player head!
                        if (nbt.hasKey("Owner", Constants.NBT.TAG_COMPOUND)) {
                            NBTTagCompound relevantNbt = tldrInfo(nbt, showAllInfo);
                            BlockPos skullPos = skull.getPos();
                            relevantNbt.setTag("__position", new NBTTagIntArray(new int[]{skullPos.getX(), skullPos.getY(), skullPos.getZ()}));
                            Utils.copyToClipboardOrSaveAsFile("skull data", "skull", relevantNbt, true);
                            return;
                        }
                    } else if (te instanceof TileEntitySign) {
                        TileEntitySign sign = (TileEntitySign) te;
                        NBTTagCompound nbt = new NBTTagCompound();
                        for (int lineNr = 0; lineNr < sign.signText.length; lineNr++) {
                            nbt.setString("Text" + (lineNr + 1), sign.signText[lineNr].getFormattedText());
                            nbt.setString("TextUnformatted" + (lineNr + 1), sign.signText[lineNr].getUnformattedText());
                        }
                        Utils.copyToClipboardOrSaveAsFile("sign data", "sign", nbt, true);
                        return;
                    } else if (te instanceof TileEntityBanner) {
                        List<String> possiblePatterns = Arrays.asList("b", "bl", "bo", "br", "bri", "bs", "bt", "bts", "cbo", "cr", "cre", "cs", "dls", "drs", "flo", "gra", "hh", "ld", "ls", "mc", "moj", "mr", "ms", "rd", "rs", "sc", "sku", "ss", "tl", "tr", "ts", "tt", "tts", "vh", "lud", "rud", "gru", "hhb", "vhr");
                        String base64Alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/";

                        TileEntityBanner banner = (TileEntityBanner) te;
                        Iterator<TileEntityBanner.EnumBannerPattern> bannerPatterns = banner.getPatternList().iterator();
                        Iterator<EnumDyeColor> bannerColors = banner.getColorList().iterator();
                        try {
                            // hash used by needcoolshoes.com
                            StringBuilder bannerHash = new StringBuilder();
                            while (bannerPatterns.hasNext() && bannerColors.hasNext()) {
                                int patternId = possiblePatterns.indexOf(bannerPatterns.next().getPatternID());
                                int color = bannerColors.next().getDyeDamage();
                                int first = ((patternId >> 6) << 4) | (color & 0xF);
                                int second = patternId & 0x3F;
                                bannerHash.append(base64Alphabet.charAt(first)).append(base64Alphabet.charAt(second));
                            }
                            main.getChatHelper().sendMessage(new SkyhChatComponent("➡ View this banner on needcoolshoes.com").green().setUrl("https://www.needcoolshoes.com/banner?=" + bannerHash));
                        } catch (IndexOutOfBoundsException e) {
                            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Failed to parse banner data (unknown banner pattern).");
                        }
                        return;
                    }
                    break;
                }
                case ENTITY: {
                    Entity entity = lookingAt.entityHit;
                    if (entity instanceof EntityArmorStand) {
                        // looking at non-invisible armor stand (e.g. Minion)
                        EntityArmorStand armorStand = (EntityArmorStand) entity;
                        copyEntityInfoToClipboard("armor stand '" + armorStand.getName() + EnumChatFormatting.GREEN + "'", "armorstand", armorStand, showAllInfo);
                        return;
                    } else if (entity instanceof EntityOtherPlayerMP) {
                        // looking at NPC or another player
                        EntityOtherPlayerMP otherPlayer = (EntityOtherPlayerMP) entity;
                        copyEntityInfoToClipboard("player/npc '" + otherPlayer.getDisplayNameString() + EnumChatFormatting.GREEN + "'", "npc_" + otherPlayer.getDisplayNameString(), otherPlayer, showAllInfo);
                        return;
                    } else if (entity instanceof EntityItemFrame) {
                        EntityItemFrame itemFrame = (EntityItemFrame) entity;

                        ItemStack displayedItem = itemFrame.getDisplayedItem();
                        if (displayedItem != null) {
                            NBTTagCompound nbt = new NBTTagCompound();
                            if (displayedItem.getItem() == Items.filled_map) {
                                // filled map
                                MapData mapData = ItemMap.loadMapData(displayedItem.getItemDamage(), sender.getEntityWorld());
                                File mapFile = ImageUtils.saveMapToFile(mapData);
                                if (mapFile != null) {
                                    main.getChatHelper().sendMessage(new SkyhChatComponent("Saved map as " + mapFile.getName() + " ").green().setOpenFile(mapFile).appendSibling(new SkyhChatComponent("[open file]").gold())
                                            .appendSibling(new SkyhChatComponent(" [open folder]").darkAqua().setOpenFile(mapFile.getParentFile())));
                                } else {
                                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Couldn't save map for some reason");
                                }
                                return;
                            } else {
                                displayedItem.writeToNBT(nbt);
                            }
                            Utils.copyToClipboardOrSaveAsFile("item in item frame '" + displayedItem.getDisplayName() + EnumChatFormatting.GREEN + "'", "itemframe-item_" + displayedItem.getDisplayName(), nbt, true);
                            return;
                        }
                    } else if (entity instanceof EntityLiving) {
                        EntityLiving living = (EntityLiving) entity;
                        copyEntityInfoToClipboard("mob '" + living.getName() + EnumChatFormatting.GREEN + "'", "mob_" + living.getName(), living, showAllInfo);
                        return;
                    }
                    break;
                }
                default:
                    // didn't find anything...
            }
        }
        // didn't find anything special; search for all nearby entities
        double maxDistance = 5; // default 4
        Entity self = sender.getCommandSenderEntity();
        Vec3 selfLook = self.getLook(1);
        float searchRadius = 1.0F;
        List<Entity> nearbyEntities = sender.getEntityWorld().getEntitiesInAABBexcluding(self, self.getEntityBoundingBox().addCoord(selfLook.xCoord * maxDistance, selfLook.yCoord * maxDistance, selfLook.zCoord * maxDistance).expand(searchRadius, searchRadius, searchRadius), entity1 -> true);

        if (nearbyEntities.size() > 0) {
            NBTTagList entities = new NBTTagList();
            for (Entity entity : nearbyEntities) {
                NBTTagCompound relevantNbt = extractEntityInfo(entity, showAllInfo);
                // add additional info to make it easier to find the correct entity in the list of entities
                relevantNbt.setTag("_entityType", new NBTTagString(entity.getClass().getSimpleName()));
                NBTTagList position = new NBTTagList();
                position.appendTag(new NBTTagDouble(entity.posX));
                position.appendTag(new NBTTagDouble(entity.posY));
                position.appendTag(new NBTTagDouble(entity.posZ));
                relevantNbt.setTag("_position", position);
                entities.appendTag(relevantNbt);
            }

            Utils.copyToClipboardOrSaveAsFile(nearbyEntities.size() + " nearby entities", "entities", entities, true);
        } else {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "You stare into the void... and see nothing of interest. " + EnumChatFormatting.GRAY + "Try looking at: NPCs, mobs, armor stands, placed skulls, banners, signs, dropped items, item in item frames, or maps on a wall.");
        }
    }

    private NBTTagCompound extractEntityInfo(Entity entity, boolean showAllInfo) {
        NBTTagCompound nbt = new NBTTagCompound();
        entity.writeToNBT(nbt);
        NBTTagCompound relevantNbt = tldrInfo(nbt, showAllInfo);

        if (entity instanceof EntityOtherPlayerMP) {
            EntityOtherPlayerMP otherPlayer = (EntityOtherPlayerMP) entity;
            relevantNbt.setString("__name", otherPlayer.getName());
            if (otherPlayer.hasCustomName()) {
                relevantNbt.setString("__customName", otherPlayer.getCustomNameTag());
            }
            GameProfile gameProfile = otherPlayer.getGameProfile();
            for (Property property : gameProfile.getProperties().get("textures")) {
                relevantNbt.setString("_skin", property.getValue());
            }
        }
        if (entity instanceof EntityLiving || entity instanceof EntityOtherPlayerMP) {
            // either EntityLiving (any mob), or EntityOtherPlayerMP => find other nearby "name tag" EntityArmorStands
            List<Entity> nearbyArmorStands = entity.getEntityWorld().getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(0.2d, 3, 0.2d), nearbyEntity -> {
                if (nearbyEntity instanceof EntityArmorStand) {
                    EntityArmorStand armorStand = (EntityArmorStand) nearbyEntity;
                    if (armorStand.isInvisible() && armorStand.hasCustomName()) {
                        for (ItemStack equipment : armorStand.getInventory()) {
                            if (equipment != null) {
                                // armor stand has equipment, abort!
                                return false;
                            }
                        }
                        // armor stand has a custom name, is invisible and has no equipment -> probably a "name tag"-armor stand
                        return true;
                    }
                }
                return false;
            });
            if (nearbyArmorStands.size() > 0) {
                nearbyArmorStands.sort(Comparator.<Entity>comparingDouble(nearbyArmorStand -> nearbyArmorStand.posY).reversed());
                NBTTagList nearbyText = new NBTTagList();
                for (int i = 0, maxNearbyArmorStands = Math.min(10, nearbyArmorStands.size()); i < maxNearbyArmorStands; i++) {
                    Entity nearbyArmorStand = nearbyArmorStands.get(i);
                    nearbyText.appendTag(new NBTTagString(nearbyArmorStand.getCustomNameTag()));
                }
                relevantNbt.setTag("__nearbyText", nearbyText);
            }
        }
        return relevantNbt;
    }

    private void copyEntityInfoToClipboard(String what, String fileName, Entity entity, boolean showAllInfo) {
        NBTTagCompound relevantNbt = extractEntityInfo(entity, showAllInfo);
        Utils.copyToClipboardOrSaveAsFile(what, fileName, relevantNbt, true);
    }

    private NBTTagCompound tldrInfo(NBTTagCompound nbt, boolean showAllInfo) {
        if (showAllInfo) {
            // don't tl;dr!
            return nbt;
        }
        String[] importantTags = new String[]{"CustomName", "id", "Damage", "Count", "Equipment", "Item", "tag", "ExtraAttributes", "Owner"};
        NBTTagCompound relevantNbt = new NBTTagCompound();
        for (String tag : importantTags) {
            if (nbt.hasKey(tag)) {
                relevantNbt.setTag(tag, nbt.getTag(tag));
            }
        }
        return relevantNbt;
    }

    private void handleDungeon(String[] args) throws SkyhCommandException {
        DungeonCache dungeonCache = main.getDungeonCache();
        if (args.length == 2 && args[1].equalsIgnoreCase("enter")) {
            // enter dungeon in case for some reason it wasn't detected automatically
            dungeonCache.onDungeonEnterOrLeave(true);
        } else if (args.length == 2 && args[1].equalsIgnoreCase("leave")) {
            // leave dungeon in case for some reason it wasn't detected automatically
            dungeonCache.onDungeonEnterOrLeave(false);
        } else if ((args.length == 2 && (args[1].equalsIgnoreCase("party") || args[1].equalsIgnoreCase("p")))
                || args.length == 1 && args[0].equalsIgnoreCase("dp")) {
            if (!CredentialStorage.isSkyhValid) {
                throw new SkyhCommandException("[Skyhaven] You haven't set your Hypixel API key yet or the API key is invalid. Use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey " + EnumChatFormatting.RED + "to manually set your existing API key.");
            } else if (dungeonsPartyListener != null) {
                throw new SkyhCommandException("Please wait a few seconds before using this command again.");
            }
            main.getChatHelper().sendServerCommand("/party list");
            new TickDelay(() -> {
                // abort after 10 seconds
                if (dungeonsPartyListener.isStillRunning()) {
                    dungeonsPartyListener.shutdown();
                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Dungeon party analysis timed out. Probably the recognition of the party members failed.");
                }
                dungeonsPartyListener = null;
            }, 10 * 20);
            // register dungeon listener
            dungeonsPartyListener = new DungeonsPartyListener(main);
        } else if (args.length == 2 && args[1].equalsIgnoreCase("rules")
                || args.length == 1 && args[0].equalsIgnoreCase("dr")) {
            displayGuiScreen(new RuleEditorGui());
        } else if (dungeonCache.isInDungeon()) {
            dungeonCache.sendDungeonPerformance();
        } else {
            throw new SkyhCommandException(EnumChatFormatting.DARK_RED + "Looks like you're not in a dungeon... However, you can manually enable the Dungeon Performance overlay with " + EnumChatFormatting.RED + "/" + getCommandName() + " dungeon enter" + EnumChatFormatting.DARK_RED + ". You can also force-leave a dungeon with " + EnumChatFormatting.RED + "/" + getCommandName() + " dungeon leave.\n" + EnumChatFormatting.GRAY + "Want to inspect your current party members? Use " + EnumChatFormatting.WHITE + "/" + getCommandName() + " dungeon party");
        }
    }
    //endregion

    //region sub-commands: miscellaneous
    private void handleGuiScale(String[] args) throws CommandException {
        GameSettings gameSettings = Minecraft.getMinecraft().gameSettings;
        int currentGuiScale = gameSettings.guiScale;
        if (args.length == 1) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "➜ Current GUI scale: " + EnumChatFormatting.DARK_GREEN + currentGuiScale);
        } else {
            int scale = MathHelper.parseIntWithDefault(args[1], -1);
            if (scale == -1 || scale > 10) {
                throw new NumberInvalidException(EnumChatFormatting.DARK_RED + args[1] + EnumChatFormatting.RED + " is an invalid GUI scale value. Valid values are integers below 10");
            }
            gameSettings.guiScale = scale;
            gameSettings.saveOptions();
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "✔ New GUI scale: " + EnumChatFormatting.DARK_GREEN + scale + EnumChatFormatting.GREEN + " (previous: " + EnumChatFormatting.DARK_GREEN + currentGuiScale + EnumChatFormatting.GREEN + ")");
        }
    }

    private void handleApiKey(String[] args) throws CommandException {
        if (args.length == 1) {
            SkyhChatComponent msg;
            EnumChatFormatting color;
            EnumChatFormatting colorSecondary;
            if (CredentialStorage.isSkyhValid && StringUtils.isNotEmpty(CredentialStorage.skyh)) {
                msg = new SkyhChatComponent("[Skyhaven] You already set your Hypixel API key.").green();
                color = EnumChatFormatting.GRAY;
                colorSecondary = EnumChatFormatting.YELLOW;
            } else {
                msg = new SkyhChatComponent("[Skyhaven] You haven't set your Hypixel API key yet or the API key is invalid.").red();
                color = EnumChatFormatting.RED;
                colorSecondary = EnumChatFormatting.DARK_RED;
            }
            main.getChatHelper().sendMessage(msg.appendSibling(new SkyhChatComponent(color + " Use " + colorSecondary + "/" + this.getCommandName() + " apikey <key>" + color + " to manually set your API key."))
                    .appendFreshSibling(new SkyhChatComponent(" ❢ ").lightPurple().setUrl("https://github.com/skyh-mc/Skyhaven/blob/master/CHANGELOG.md#note-on-api-keys-")
                            .appendSibling(new SkyhChatComponent("[open 'Note on API keys']").darkAqua().underline())));
        } else {
            String key = args[1];
            if (Utils.isValidUuid(key)) {
                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "[Skyhaven] Saved new API key.");
                main.getSkyh().setSkyh(key);
            } else {
                throw new SyntaxErrorException("[Skyhaven] That doesn't look like a valid API key...");
            }
        }
    }

    private void handleWorldAge(String[] args) {
        if (args.length == 2) {
            boolean enable;
            switch (args[1]) {
                case "on":
                case "enable":
                    enable = true;
                    break;
                case "off":
                case "disable":
                    enable = false;
                    break;
                default:
                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Command usage: /" + getCommandName() + " worldage [on|off]");
                    return;
            }
            SkyhConfig.notifyServerAge = enable;
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "✔ " + (enable ? EnumChatFormatting.DARK_GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled") + EnumChatFormatting.GREEN + " world age notifications.");
            main.getConfig().syncFromFields();
        } else {
            long worldTime = Minecraft.getMinecraft().theWorld.getWorldTime();
            new TickDelay(() -> {
                WorldClient world = Minecraft.getMinecraft().theWorld;
                if (world == null) {
                    return;
                }
                String msgPrefix;
                long worldTime2 = world.getWorldTime();
                if (worldTime > worldTime2 || (worldTime2 - worldTime) < 15) {
                    // time is frozen
                    worldTime2 = world.getTotalWorldTime();
                    msgPrefix = "World time seems to be frozen at around " + worldTime + " ticks. ";
                    if (worldTime2 > 24 * 60 * 60 * 20) {
                        // total world time >24h
                        main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, msgPrefix + "However, how long this world is loaded cannot be determined.");
                        return;
                    }
                    msgPrefix += "However, this world is probably";
                } else {
                    msgPrefix = "This world is";
                }
                long days = worldTime2 / 24000L + 1;
                long minutes = days * 20;
                long hours = minutes / 60;
                minutes -= hours * 60;

                main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, msgPrefix + " loaded around " + EnumChatFormatting.GOLD + days + " ingame days "
                        + EnumChatFormatting.YELLOW + "(= less than" + EnumChatFormatting.GOLD + (hours > 0 ? " " + hours + " hours" : "") + (minutes > 0 ? " " + minutes + " mins" : "") + ")");
            }, 20);
        }
    }
    //endregion

    //region sub-commands: update mod
    private void handleUpdate(String[] args) throws SkyhCommandException {
        if (args.length == 2 && args[1].equalsIgnoreCase("help")) {
            handleUpdateHelp();
            return;
        }
        boolean updateCheckStarted = main.getVersionChecker().runUpdateCheck(true);

        if (updateCheckStarted) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "➜ Checking for a newer mod version...");
            // VersionChecker#handleVersionStatus will run with a 5 seconds delay
        } else {
            long nextUpdate = main.getVersionChecker().getNextCheck();
            String waitingTime = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(nextUpdate),
                    TimeUnit.MILLISECONDS.toSeconds(nextUpdate) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(nextUpdate)));
            throw new SkyhCommandException("⚠ Update checker is on cooldown. Please wait " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + waitingTime + EnumChatFormatting.RESET + EnumChatFormatting.RED + " more minutes before checking again.");
        }
    }

    private void handleUpdateHelp() {
        main.getChatHelper().sendMessage(new ChatComponentText("➜ Update instructions:").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true))
                .appendSibling(new ChatComponentText("\n➊" + EnumChatFormatting.YELLOW + " download latest mod version").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, main.getVersionChecker().getDownloadUrl()))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Download the latest version of " + Skyhaven.MODNAME + "\n➜ Click to download latest mod file")))))
                .appendSibling(new ChatComponentText("\n➋" + EnumChatFormatting.YELLOW + " exit Minecraft").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GOLD + "➋" + EnumChatFormatting.YELLOW + " Without closing Minecraft first,\n" + EnumChatFormatting.YELLOW + "you can't delete the old .jar file!")))))
                .appendSibling(new ChatComponentText("\n➌" + EnumChatFormatting.YELLOW + " copy " + EnumChatFormatting.GOLD + Skyhaven.MODNAME.replace(" ", "") + "-" + main.getVersionChecker().getNewVersion() + ".jar" + EnumChatFormatting.YELLOW + " into mods directory").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/skyh directory"))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Open mods directory with command " + EnumChatFormatting.GOLD + "/skyh directory\n➜ Click to open mods directory")))))
                .appendSibling(new ChatComponentText("\n➍" + EnumChatFormatting.YELLOW + " delete old mod file " + EnumChatFormatting.GOLD + Skyhaven.MODNAME.replace(" ", "") + "-" + Skyhaven.VERSION + ".jar ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)))
                .appendSibling(new ChatComponentText("\n➎" + EnumChatFormatting.YELLOW + " start Minecraft again").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false))));
    }
    //endregion

    // other helper methods:
    private void displayGuiScreen(GuiScreen gui) {
        // delay by 1 tick, because the chat closing would close the new gui instantly as well.
        new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(gui), 1);
    }

    private void sendCommandUsage(ICommandSender sender) {
        IChatComponent dungeonOverlayHint = SkyhConfig.dungOverlayEnabled
                ? new SkyhChatComponent("\n").reset().white().appendText(EnumChatFormatting.DARK_GREEN + "  ❢" + EnumChatFormatting.LIGHT_PURPLE + EnumChatFormatting.ITALIC + " To move the Dungeons overlay: " + EnumChatFormatting.WHITE + "/" + getCommandName() + " config " + EnumChatFormatting.GRAY + "➡ " + EnumChatFormatting.WHITE + "SB Dungeons " + EnumChatFormatting.GRAY + "➡ " + EnumChatFormatting.WHITE + "Dungeon Performance Overlay")
                : null;

        SkyhChatComponent usage = new SkyhChatComponent("➜ " + Skyhaven.MODNAME + " commands:").gold().bold()
                .appendSibling(createCmdHelpEntry("config", "Open mod's configuration"))
                .appendSibling(dungeonOverlayHint)
                .appendSibling(new SkyhChatComponent("\n").reset().gray().appendText(EnumChatFormatting.DARK_GREEN + "  ❢" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + " Commands marked with §d§l⚷" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + " require a valid API key"))
                .appendSibling(createCmdHelpSection(1, "SkyBlock"))
                .appendSibling(createCmdHelpEntry("stalkskyblock", "Get info of player's SkyBlock stats §d§l⚷"))
                .appendSibling(createCmdHelpEntry("analyzeChests", "Analyze chests' contents and evaluate potential Bazaar value"))
                .appendSibling(createCmdHelpEntry("analyzeIsland", "Analyze a SkyBlock private island (inspect minions)"))
                .appendSibling(createCmdHelpEntry("waila", "Copy the 'thing' you're looking at (optional keybinding: Minecraft controls > Skyhaven)"))
                .appendSibling(createCmdHelpEntry("dungeon", "SkyBlock Dungeons: display current dungeon performance"))
                .appendSibling(createCmdHelpEntry("dungeon party", "SkyBlock Dungeons: Shows armor and dungeon info about current party members " + EnumChatFormatting.GRAY + "(alias: " + EnumChatFormatting.WHITE + "/" + getCommandName() + " dp" + EnumChatFormatting.GRAY + ") §d§l⚷"))
                .appendSibling(createCmdHelpEntry("dungeon rules", "SkyBlock Dungeons: Edit rules for Party Finder " + EnumChatFormatting.GRAY + "(alias: " + EnumChatFormatting.WHITE + "/" + getCommandName() + " dr" + EnumChatFormatting.GRAY + ")"))
                .appendSibling(createCmdHelpSection(2, "Miscellaneous"))
                .appendSibling(createCmdHelpEntry("search", "Open Minecraft log search"))
                .appendSibling(createCmdHelpEntry("worldage", "Check how long the current world is loaded"))
                .appendSibling(createCmdHelpEntry("stalk", "Get info of player's online status §d§l⚷"))
                .appendSibling(createCmdHelpEntry("guiScale", "Change GUI scale"))
                .appendSibling(createCmdHelpEntry("rr", "Alias for /r without auto-replacement to /msg"))
                .appendSibling(createCmdHelpEntry("shrug", "¯\\_(ツ)_/¯"))
                .appendSibling(createCmdHelpEntry("discord", "Need help? Join the SkyhX discord"))
                .appendSibling(createCmdHelpSection(3, "Update mod"))
                .appendSibling(createCmdHelpEntry("update", "Check for new mod updates"))
                .appendSibling(createCmdHelpEntry("updateHelp", "Show mod update instructions"))
                .appendSibling(createCmdHelpEntry("version", "View results of last mod update check"))
                .appendSibling(createCmdHelpEntry("directory", "Open Minecraft's 'mods' or Skyhaven's 'config' directory"));
        if (main.getFriendsHandler().getBestFriendsListSize() > 0) {
            usage.appendSibling(createCmdHelpEntry("bestfriends", "§dMigrate best friends list"));
        }
        usage.appendFreshSibling(new SkyhChatComponent("➡ /commandslist " + EnumChatFormatting.YELLOW + "to list all commands added by your installed mods.").lightPurple().setSuggestCommand("/commandslist"))
                .appendFreshSibling(new SkyhChatComponent("➜ Need help with " + EnumChatFormatting.GOLD + Skyhaven.MODNAME + EnumChatFormatting.GREEN + "? Do you have any questions, suggestions or other feedback? " + EnumChatFormatting.GOLD + "Join the SkyhX discord!").green().setUrl(Skyhaven.INVITE_URL));
        sender.addChatMessage(usage);
    }

    private IChatComponent createCmdHelpSection(int nr, String title) {
        String prefix = Character.toString((char) (0x2789 + nr));
        return new ChatComponentText("\n").appendSibling(new ChatComponentText(prefix + " " + title).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true)));
    }

    private IChatComponent createCmdHelpEntry(String cmd, String usage) {
        String command = "/" + this.getCommandName() + " " + cmd;

        return new SkyhChatComponent("\n").reset().appendSibling(new SkyhChatComponent.KeyValueChatComponent(command, usage, " ➡ ").setSuggestCommand(command));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                    /* main */ "help", "config",
                    /* Best friends, friends & other players */ "stalk",
                    /* SkyBlock */ "stalkskyblock", "skyblockstalk", "chestAnalyzer", "analyzeChests", "analyzeIsland", "waila", "whatAmILookingAt", "dungeon",
                    /* miscellaneous */ "search", "worldage", "serverage", "guiscale", "rr", "shrug", "apikey", "discord",
                    /* update mod */ "update", "updateHelp", "version", "directory",
                    /* rarely used aliases */ "askPolitelyWhereTheyAre", "askPolitelyAboutTheirSkyBlockProgress", "year", "whatyearisit",
                    /* deprecated as of 0.16.0: */ "bestfriends", "remove");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("waila") || args[0].equalsIgnoreCase("whatAmILookingAt"))) {
            return getListOfStringsMatchingLastWord(args, "all", "main");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return getListOfStringsMatchingLastWord(args, main.getFriendsHandler().getBestFriends());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("dungeon")) {
            return getListOfStringsMatchingLastWord(args, "party", "rules", "enter", "leave");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("worldage") || args[0].equalsIgnoreCase("serverage"))) {
            return getListOfStringsMatchingLastWord(args, "off", "on", "disable", "enable");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("chestAnalyzer") || args[0].equalsIgnoreCase("chestAnalyser") || args[0].equalsIgnoreCase("analyzeChests") || args[0].equalsIgnoreCase("analyseChests"))) {
            return getListOfStringsMatchingLastWord(args, "stop");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("directory") || args[0].equalsIgnoreCase("folder"))) {
            return getListOfStringsMatchingLastWord(args, "config", "mods");
        }
        String commandArg = args[0].toLowerCase();
        if (args.length == 2 && (commandArg.equals("s") || commandArg.equals("ss") || commandArg.contains("stalk") || commandArg.contains("askpolitely"))) { // stalk & stalkskyblock
            return getListOfStringsMatchingLastWord(args, main.getPlayerCache().getAllNamesSorted());
        }
        return null;
    }
}
