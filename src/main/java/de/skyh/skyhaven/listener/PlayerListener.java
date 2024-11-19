package de.skyh.skyhaven.listener;

import de.skyh.skyhaven.Skyhaven;
import de.skyh.skyhaven.config.SkyhConfig;
import de.skyh.skyhaven.error.ApiHttpErrorEvent;
import de.skyh.skyhaven.listener.skyblock.DungeonsListener;
import de.skyh.skyhaven.listener.skyblock.SkyBlockListener;
import de.skyh.skyhaven.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;

public class PlayerListener {
    private final Skyhaven main;
    private static DungeonsListener dungeonsListener;
    private static SkyBlockListener skyBlockListener;
    private boolean isPlayerJoiningServer;
    private boolean isOnSkyBlock;
    private boolean clickableMessageSent = false;
    private AbortableRunnable checkScoreboard;
    private long nextApiErrorMessage;
    private long nextMigrationNotification = 0;

    public PlayerListener(Skyhaven main) {
        this.main = main;
    }

    @SubscribeEvent
    public void onKeybindingPressed(InputEvent.KeyInputEvent e) {
        KeyBinding[] keyBindings = Skyhaven.keyBindings;

        if (keyBindings[0].isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null && mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
                mc.displayGuiScreen(new GuiChat("/skyh "));
            }
        } else if (keyBindings[1].isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null) {
                ClientCommandHandler.instance.executeCommand(mc.thePlayer, "/skyh waila");
            }
        }
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre e) {
        if (SkyhConfig.enableCopyInventory && Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_C && GuiScreen.isCtrlKeyDown()) {
            if (GuiScreen.isShiftKeyDown()) {
                // ctrl + shift + C: Copy inventory
                IInventory inventory;
                String inventoryName;
                if (e.gui instanceof GuiChest) {
                    // some kind of chest
                    ContainerChest chestContainer = (ContainerChest) ((GuiChest) e.gui).inventorySlots;
                    inventory = chestContainer.getLowerChestInventory();
                    inventoryName = (inventory.hasCustomName() ? EnumChatFormatting.getTextWithoutFormattingCodes(inventory.getDisplayName().getUnformattedTextForChat()) : inventory.getName());
                } else if (e.gui instanceof GuiInventory) {
                    // player inventory
                    inventory = Minecraft.getMinecraft().thePlayer.inventory;
                    inventoryName = "Player inventory";
                } else {
                    // another gui, abort!
                    return;
                }
                NBTTagList items = new NBTTagList();
                for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                    ItemStack item = inventory.getStackInSlot(slot);
                    if (item != null) {
                        // slot + item
                        NBTTagCompound tag = new NBTTagCompound();
                        tag.setByte("Slot", (byte) slot);
                        item.writeToNBT(tag);
                        items.appendTag(tag);
                    }
                }
                Utils.copyToClipboardOrSaveAsFile(items.tagCount() + " items from '" + inventoryName + "'", "inventory_" + inventoryName, items, false);
            } else {
                // ctrl + C: Copy one item
                if (e.gui instanceof GuiContainer) {
                    Slot slotUnderMouse = GuiHelper.getSlotUnderMouse((GuiContainer) e.gui);
                    if (slotUnderMouse != null && slotUnderMouse.getHasStack()) {
                        ItemStack itemUnderMouse = slotUnderMouse.getStack();
                        NBTTagCompound itemNbt = new NBTTagCompound();
                        itemUnderMouse.writeToNBT(itemNbt);
                        Utils.copyToClipboardOrSaveAsFile(itemUnderMouse.getDisplayName() + EnumChatFormatting.RESET + EnumChatFormatting.GREEN, "item_" + itemUnderMouse.getDisplayName(), itemNbt, false);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        if (!isPlayerJoiningServer) {
            isOnSkyBlock = false;
            isPlayerJoiningServer = true;
            main.getVersionChecker().runUpdateCheck(false);
        }
    }

    @SubscribeEvent
    public void onApiHttpError(ApiHttpErrorEvent e) {
        if (nextApiErrorMessage < System.currentTimeMillis() && Minecraft.getMinecraft().thePlayer != null) {
            this.nextApiErrorMessage = System.currentTimeMillis() + 3000;
            SkyhChatComponent hoverComponent = new SkyhChatComponent.KeyValueTooltipComponent("Click to visit", e.getUrl());
            if (e.wasUsingApiKey()) {
                hoverComponent.appendFreshSibling(new SkyhChatComponent(EnumChatFormatting.LIGHT_PURPLE + " ❢ " + EnumChatFormatting.RED + " Request was using your API-Key.").red());
            }
            SkyhChatComponent errorMsg = new SkyhChatComponent(e.getMessage()).red()
                    .setUrl(e.getUrl(), hoverComponent);
            if (e.wasUsingApiKey()) {
                errorMsg.appendFreshSibling(new SkyhChatComponent(" ❢ ").lightPurple().setUrl("https://github.com/SkyHavenMod/Skyhaven/blob/master/CHANGELOG.md#note-on-api-keys-")
                        .appendSibling(new SkyhChatComponent("[open 'Note on API keys']").darkAqua().underline()));
            }
            main.getChatHelper().sendMessage(errorMsg);
        }
    }

    @SubscribeEvent
    public void onWorldEnter(PlayerSetSpawnEvent e) {
        isPlayerJoiningServer = false;

        if (clickableMessageSent) return; // Prevent duplicate sends
        clickableMessageSent = true;

        main.getChatHelper().sendMessage(
                new SkyhChatComponent(
                        EnumChatFormatting.GRAY + "Thank you for using SkyHaven! " +
                                EnumChatFormatting.WHITE + "It appears you are using an " +
                                EnumChatFormatting.RED + EnumChatFormatting.BOLD + "OUTDATED" +
                                EnumChatFormatting.RESET + EnumChatFormatting.WHITE + " version. " +
                                EnumChatFormatting.GOLD + "Please download the latest version by clicking " +
                                EnumChatFormatting.BLUE + "[DOWNLOAD]" + EnumChatFormatting.RESET + EnumChatFormatting.WHITE + " below.\n\n" +
                                EnumChatFormatting.RED + EnumChatFormatting.BOLD + "NOTE: SOME FEATURES (e.g., ITEM DATA, UUID, TIMESTAMPS) MAY NOT FUNCTION CORRECTLY WITH THIS VERSION.\n\n"
                );
        );

        main.getChatHelper().sendMessage(
                new SkyhChatComponent(
                        EnumChatFormatting.DARK_GRAY + "                [ " + EnumChatFormatting.GREEN + "DOWNLOAD HERE " + EnumChatFormatting.DARK_GRAY + "]"
                )
                        .red()
                        .setUrl(
                                "https://github.com/SkyHavenMod/Skyhaven",
                                new SkyhChatComponent.KeyValueTooltipComponent(
                                        EnumChatFormatting.DARK_GRAY + "[ " + EnumChatFormatting.GREEN + "DOWNLOAD HERE " + EnumChatFormatting.DARK_GRAY + "]",
                                        "https://github.com/SkyHavenMod/Skyhaven"
                                )
                        )
        );
        if (this.nextMigrationNotification < System.currentTimeMillis()) {
            this.nextMigrationNotification = System.currentTimeMillis() + 600_000; // every 10 minutes
            new TickDelay(() -> {
                if (SkyhConfig.doBestFriendsOnlineCheck) {
                    if (main.getFriendsHandler().getBestFriendsListSize() == 0) {
                        SkyhConfig.doBestFriendsOnlineCheck = false;
                        main.getConfig().syncFromFields();
                    } else {
                        main.getChatHelper().sendMessage(new SkyhChatComponent("[" + EnumChatFormatting.DARK_RED + Skyhaven.MODNAME + EnumChatFormatting.RED + "] The 'best friends list' feature has been removed from this mod.").red()
                                .appendSibling(new SkyhChatComponent(" Run " + EnumChatFormatting.GOLD + "/skyh bestfriends " + EnumChatFormatting.YELLOW + "to migrate your best friends list").yellow())
                                .setSuggestCommand("/skyh bestfriends", false)
                                .setHover(new SkyhChatComponent.KeyValueChatComponent("Run", "/skyh bestfriends", " ")
                                        .appendFreshSibling(new SkyhChatComponent("(This message will re-appear as long as there are still names on your Skyhaven best friends list)").red())));
                    }
                }
                if (SkyhConfig.doMonitorNotifications()) {
                    main.getChatHelper().sendMessage(new SkyhChatComponent("[" + EnumChatFormatting.DARK_RED + Skyhaven.MODNAME + EnumChatFormatting.RED + "] The 'login & logout notifications filter' feature has been removed from this mod.").red()
                            .appendFreshSibling(new SkyhChatComponent("Use Hypixel's commands instead:").gold()
                                    .appendFreshSibling(new SkyhChatComponent(" §6➊ §eCycle through (best) friends notifications: §6/friend notifications §7(you may need to repeat this command to get the desired setting)").yellow().setSuggestCommand("/friend notifications"))
                                    .appendFreshSibling(new SkyhChatComponent(" §6➋ §eToggle Guild notifications: §6/guild notifications").yellow().setSuggestCommand("/guild notifications")))
                            .appendFreshSibling(new SkyhChatComponent("[Do not show this message again! I updated my settings accordingly]").darkAqua().underline().setSuggestCommand("/skyh I-read-the-login-logout-notification-changes")));
                }
            }, 3000);
        }

        if (SkyhConfig.getEnableSkyBlockOnlyFeatures() == SkyhConfig.Setting.ALWAYS) {
            main.getLogger().info("Registering SkyBlock listeners");
            isOnSkyBlock = true;
            registerSkyBlockListeners();
            checkWorldAge();
        } else if (SkyhConfig.getEnableSkyBlockOnlyFeatures() == SkyhConfig.Setting.SPECIAL) { // only on SkyBlock
            stopScoreboardChecker();

            // check if player is on SkyBlock or on another gamemode
            checkScoreboard = new AbortableRunnable() {
                private int retries = 20 * 20; // retry for up to 20 seconds

                @SubscribeEvent
                public void onTickCheckScoreboard(TickEvent.ClientTickEvent e) {
                    if (!stopped && e.phase == TickEvent.Phase.END) {
                        if (Minecraft.getMinecraft().theWorld == null || retries <= 0) {
                            // already stopped; or world gone, probably disconnected; or no retries left (took too long [20 seconds not enough?] or is not on SkyBlock): stop!
                            stop();
                            return;
                        }
                        retries--;
                        ScoreObjective scoreboardSidebar = Minecraft.getMinecraft().theWorld.getScoreboard().getObjectiveInDisplaySlot(1);
                        if (scoreboardSidebar == null && retries >= 0) {
                            // scoreboard hasn't loaded yet, retry next tick
                            return;
                        }

                        // ... either scoreboard has loaded now or no more retries left

                        boolean wasOnSkyBlock = isOnSkyBlock;
                        isOnSkyBlock = (scoreboardSidebar != null && EnumChatFormatting.getTextWithoutFormattingCodes(scoreboardSidebar.getDisplayName()).startsWith("SKYBLOCK"));

                        if (!wasOnSkyBlock && isOnSkyBlock) {
                            // player wasn't on SkyBlock before but now is on SkyBlock
                            main.getLogger().info("Entered SkyBlock! Registering SkyBlock listeners");
                            registerSkyBlockListeners();
                            checkWorldAge();
                        } else if (wasOnSkyBlock && !isOnSkyBlock) {
                            // player was on SkyBlock before and is now in another gamemode
                            unregisterSkyBlockListeners();
                            main.getLogger().info("Leaving SkyBlock! Un-registering SkyBlock listeners");
                        } else if (wasOnSkyBlock /* && isOnSkyBlock */) {
                            // player is still on SkyBlock
                            checkWorldAge();
                        }
                        stop();
                    }
                }

                @Override
                public void stop() {
                    if (!stopped) {
                        stopped = true;
                        retries = -1;
                        MinecraftForge.EVENT_BUS.unregister(this);
                        stopScoreboardChecker();
                    }
                }

                @Override
                public void run() {
                    MinecraftForge.EVENT_BUS.register(this);
                }
            };

            new TickDelay(checkScoreboard, 40); // 2-second delay + retrying for 20 seconds, making sure scoreboard got sent
        } else if (SkyhConfig.getEnableSkyBlockOnlyFeatures() == SkyhConfig.Setting.DISABLED) {
            isOnSkyBlock = false;
            unregisterSkyBlockListeners();
        }

    }

    private void checkWorldAge() {
        WorldClient theWorld = Minecraft.getMinecraft().theWorld;
        if (!SkyhConfig.notifyServerAge || SkyhConfig.notifyFreshServer == 0 && SkyhConfig.notifyOldServer == 0 || theWorld == null) {
            return;
        }
        long worldTime = theWorld.getWorldTime();
        new TickDelay(() -> {
            WorldClient world = Minecraft.getMinecraft().theWorld;

            if (world == null || theWorld != world || main.getDungeonCache().isInDungeon()) {
                // no longer in a world, or not in the same world as before, or inside dungeons
                return;
            }
            long worldTime2 = world.getWorldTime();

            String infix = "";
            if (worldTime > worldTime2 || (worldTime2 - worldTime) < 30) {
                // time is frozen
                worldTime2 = world.getTotalWorldTime();
                if (worldTime2 > 24 * 60 * 60 * 20) {
                    // total world time >24h
                    return;
                }
                infix = "probably ";
            }

            long days = worldTime2 / 24000L + 1;
            if (SkyhConfig.notifyFreshServer > 0 && days <= SkyhConfig.notifyFreshServer) {
                // fresh server
                long minutes = days * 20;
                long hours = minutes / 60;
                minutes -= hours * 60;
                main.getChatHelper().sendMessage(new SkyhChatComponent("⚠ ").darkGreen()
                        .appendSibling(new SkyhChatComponent("This world is " + infix + "loaded around " + EnumChatFormatting.DARK_GREEN + days + " ingame days.").green()
                                .setHover(new SkyhChatComponent("= less than" + EnumChatFormatting.DARK_GREEN + (hours > 0 ? " " + hours + " hours" : "") + (minutes > 0 ? " " + minutes + " mins" : "")).green())));
            } else if (SkyhConfig.notifyOldServer > 0 && days > SkyhConfig.notifyOldServer) {
                // old server
                main.getChatHelper().sendMessage(new SkyhChatComponent("⚠ ").red()
                        .appendSibling(new SkyhChatComponent("This server has not been restarted for " + EnumChatFormatting.RED + days + "+ ingame days!").gold()
                                .setHover(new SkyhChatComponent("Servers usually restart once they exceed 30-38 ingame days (10-13 hours)").yellow())));
            }
        }, 40);
    }

    private void stopScoreboardChecker() {
        if (checkScoreboard != null) {
            // there is still a scoreboard-checker running, stop it
            checkScoreboard.stop();
            checkScoreboard = null;
        }
    }

    public static boolean registerSkyBlockListeners() {
        if (dungeonsListener == null && skyBlockListener == null) {
            MinecraftForge.EVENT_BUS.register(dungeonsListener = new DungeonsListener(Skyhaven.getInstance()));
            MinecraftForge.EVENT_BUS.register(skyBlockListener = new SkyBlockListener(Skyhaven.getInstance()));
            return true;
        }
        return false;
    }

    public static void unregisterSkyBlockListeners() {
        Skyhaven.getInstance().getDungeonCache().onDungeonLeft();
        if (dungeonsListener != null && skyBlockListener != null) {
            MinecraftForge.EVENT_BUS.unregister(dungeonsListener);
            dungeonsListener = null;
            MinecraftForge.EVENT_BUS.unregister(skyBlockListener);
            skyBlockListener = null;
        }
    }

    @SubscribeEvent
    public void onServerLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        // check if player actually was on the server
        if (!isPlayerJoiningServer) {
            main.getFriendsHandler().saveBestFriends();
            main.getPlayerCache().clearAllCaches();
            unregisterSkyBlockListeners();
        }
    }
}
