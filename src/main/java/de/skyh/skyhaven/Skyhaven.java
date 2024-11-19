package de.skyh.skyhaven;

import de.skyh.skyhaven.chesttracker.ChestTracker;
import de.skyh.skyhaven.command.*;
import de.skyh.skyhaven.config.CredentialStorage;
import de.skyh.skyhaven.config.SkyhConfig;
import de.skyh.skyhaven.handler.AnalyzeIslandTracker;
import de.skyh.skyhaven.handler.DungeonCache;
import de.skyh.skyhaven.handler.FriendsHandler;
import de.skyh.skyhaven.handler.PlayerCache;
import de.skyh.skyhaven.listener.ChatListener;
import de.skyh.skyhaven.listener.PlayerListener;
import de.skyh.skyhaven.partyfinder.Rules;
import de.skyh.skyhaven.util.ChatHelper;
import de.skyh.skyhaven.util.VersionChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;

@Mod(modid = Skyhaven.MODID, name = Skyhaven.MODNAME, version = Skyhaven.VERSION,
        clientSideOnly = true,
        updateJSON = "https://raw.githubusercontent.com/skyh-mc/Skyhaven/master/update.json")
public class Skyhaven {
    public static final String MODID = "@MODID@";
    public static final String VERSION = "@VERSION@";
    public static final String MODNAME = "@MODNAME@";
    public static final String GITURL = "@GITURL@";
    public static final String INVITE_URL = "https://discord.gg/fU2tFPf";
    public static KeyBinding[] keyBindings;
    private static Skyhaven instance;
    private File modsDir;
    private File configDir;
    private File modOutDir;
    private SkyhConfig config;
    private CredentialStorage skyh;
    private Rules partyFinderRules;
    private FriendsHandler friendsHandler;
    private VersionChecker versionChecker;
    private ChatHelper chatHelper;
    private PlayerCache playerCache;
    private DungeonCache dungeonCache;
    private ChestTracker chestTracker;
    private AnalyzeIslandTracker analyzeIslandTracker;
    private Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        instance = this;
        logger = e.getModLog();
        modsDir = e.getSourceFile().getParentFile();

        chatHelper = new ChatHelper();

        configDir = new File(e.getModConfigurationDirectory(), MODID + File.separatorChar);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        skyh = new CredentialStorage(new Configuration(new File(configDir, "do-not-share-me-with-other-players.cfg")));
        friendsHandler = new FriendsHandler(this, new File(configDir, "friends.json"));
        partyFinderRules = new Rules(this, new File(configDir, "partyfinder-rules.json"));
        config = new SkyhConfig(this, new Configuration(new File(configDir, MODID + ".cfg"), "2"));
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(new ChatListener(this));
        MinecraftForge.EVENT_BUS.register(new PlayerListener(this));
        ClientCommandHandler.instance.registerCommand(new SkyhCommand(this));
        ClientCommandHandler.instance.registerCommand(new NumerousCommandsCommand());
        ClientCommandHandler.instance.registerCommand(new ReplyCommand());
        ClientCommandHandler.instance.registerCommand(new ShrugCommand(this));
        for (String tabCompletableNamesCommand : SkyhConfig.tabCompletableNamesCommands) {
            ClientCommandHandler.instance.registerCommand(new TabCompletableCommand(this, tabCompletableNamesCommand));
        }
        // key bindings
        keyBindings = new KeyBinding[2];
        keyBindings[0] = new KeyBinding("key.skyhaven.skyh", Keyboard.KEY_NONE, "key.skyhaven.category");
        keyBindings[1] = new KeyBinding("key.skyhaven.waila", Keyboard.KEY_NONE, "key.skyhaven.category");

        for (KeyBinding keyBinding : keyBindings) {
            ClientRegistry.registerKeyBinding(keyBinding);
        }
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        versionChecker = new VersionChecker(this);
        playerCache = new PlayerCache();
        modOutDir = new File(Minecraft.getMinecraft().mcDataDir, Skyhaven.MODID.toLowerCase() + "_out");
    }

    public SkyhConfig getConfig() {
        return config;
    }

    public CredentialStorage getSkyh() {
        return skyh;
    }

    public Rules getPartyFinderRules() {
        return partyFinderRules;
    }

    public FriendsHandler getFriendsHandler() {
        return friendsHandler;
    }

    public VersionChecker getVersionChecker() {
        return versionChecker;
    }

    public ChatHelper getChatHelper() {
        return chatHelper;
    }

    public PlayerCache getPlayerCache() {
        return playerCache;
    }

    public DungeonCache getDungeonCache() {
        if (dungeonCache == null) {
            dungeonCache = new DungeonCache(this);
        }
        return dungeonCache;
    }

    public boolean enableChestTracker() {
        if (chestTracker == null) {
            chestTracker = new ChestTracker(this);
            return true;
        }
        return false;
    }

    public boolean disableChestTracker() {
        if (chestTracker != null) {
            chestTracker.clear();
            chestTracker = null;
            return true;
        }
        return false;
    }

    public ChestTracker getChestTracker() {
        return chestTracker;
    }

    public AnalyzeIslandTracker getAnalyzeIslandTracker() {
        if (analyzeIslandTracker == null) {
            analyzeIslandTracker = new AnalyzeIslandTracker(this);
        }
        return analyzeIslandTracker;
    }

    public File getModsDirectory() {
        return modsDir;
    }

    public File getConfigDirectory() {
        return configDir;
    }

    public File getModOutDirectory() {
        if (!modOutDir.exists() && !modOutDir.mkdirs()) {
            // dir didn't exist and couldn't be created
            return null;
        }
        return modOutDir;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Get mod's instance; instead of this method use dependency injection where possible
     */
    public static Skyhaven getInstance() {
        return instance;
    }
}
