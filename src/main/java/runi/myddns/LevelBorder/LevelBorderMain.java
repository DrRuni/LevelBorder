package runi.myddns.LevelBorder;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import runi.myddns.LevelBorder.Commands.LevelBorderCommand;
import runi.myddns.LevelBorder.Commands.ScoreboardCommand;
import runi.myddns.LevelBorder.Listeners.PortalListener;
import runi.myddns.LevelBorder.Listeners.PlayerListener;
import runi.myddns.LevelBorder.Manager.*;
import runi.myddns.LevelBorder.Utils.ConsoleColor;

import java.io.File;

public class LevelBorderMain extends JavaPlugin {

    private static LevelBorderMain instance;
    private BorderDataManager dataManager;
    private LevelBorderManager borderManager;
    private ScoreboardManager scoreboardManager;
    private TimerManager timerManager;
    private PortalManager portalManager;
    private MobSpawnManager mobSpawnManager;

    public static LevelBorderMain getInstance() { return instance; }

    @Override
    public void onLoad() {

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "  ════════════  Run´s LevelBorder  ════════════" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.COPPER + "                  L O A D I N G" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");
    }



    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveBorderDataFile();

        dataManager = new BorderDataManager(this);
        scoreboardManager = new ScoreboardManager(this, dataManager);
        timerManager = new TimerManager(this, dataManager);
        borderManager = new LevelBorderManager(this, dataManager, scoreboardManager);
        portalManager = new PortalManager(this, dataManager);
        mobSpawnManager = new MobSpawnManager(this, dataManager, borderManager);
        portalManager.loadAllPortals();

        Bukkit.getPluginManager().registerEvents(
                new PortalListener(this, portalManager, dataManager),
                this
        );
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, borderManager, scoreboardManager, timerManager),
                this
        );

        LevelBorderCommand command = new LevelBorderCommand(borderManager, scoreboardManager, timerManager);
        getCommand("levelborder").setExecutor(command);
        getCommand("levelborder").setTabCompleter(command);

        ScoreboardCommand scoreCmd = new ScoreboardCommand(scoreboardManager);
        getCommand("lbscore").setExecutor(scoreCmd);
        getCommand("lbscore").setTabCompleter(scoreCmd);

        scoreboardManager.startUpdater();
        mobSpawnManager.start();

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.DARK_GOLDEN_LIME + "  ════════════  Runi´s LevelBorder  ════════════" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage(
                ConsoleColor.DARK_GOLDEN_LIME + "                    R E A D Y" + ConsoleColor.RESET);
        Bukkit.getConsoleSender().sendMessage("");

    }

    @Override
    public void onDisable() {

        if (mobSpawnManager != null) {
            mobSpawnManager.stop();
        }

        getLogger().info(ChatColor.GOLD + "💾 LevelBorder beendet.");
    }



    private void saveBorderDataFile() {
        File file = new File(getDataFolder(), "BorderData.yml");
        if (!file.exists()) {
            saveResource("BorderData.yml", false);
            getLogger().info("📄 BorderData.yml wurde erstellt.");
        }
    }

    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public TimerManager getTimerManager() { return timerManager; }
    public LevelBorderManager getBorderManager() { return borderManager; }
    public BorderDataManager getDataManager() { return dataManager; }
    public PortalManager getPortalManager() {
        return portalManager;
    }
    public MobSpawnManager getMobSpawnManager() { return mobSpawnManager; }
}