package runi.myddns.levelborder;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

import runi.myddns.levelborder.Commands.LevelBorderCommand;
import runi.myddns.levelborder.Commands.ScoreboardCommand;
import runi.myddns.levelborder.Listeners.PortalListener;
import runi.myddns.levelborder.Listeners.PlayerListener;
import runi.myddns.levelborder.Manager.*;
import runi.myddns.levelborder.Utils.ConsoleColor;

public class LevelBorderMain extends JavaPlugin {

    private static LevelBorderMain instance;

    private PortalManager portalManager;
    private MobSpawnManager mobSpawnManager;

    public static LevelBorderMain getInstance() {
        return instance;
    }

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

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("❌ Plugin-Ordner konnte nicht erstellt werden: " + getDataFolder().getPath());
        }

        saveBorderDataFile();

        BorderDataManager dataManager = new BorderDataManager(this);
        ScoreboardManager scoreboardManager = new ScoreboardManager(this, dataManager);
        TimerManager timerManager = new TimerManager(this, dataManager);
        LevelBorderManager borderManager = new LevelBorderManager(this, dataManager);

        portalManager = new PortalManager(this, dataManager);
        mobSpawnManager = new MobSpawnManager(this, dataManager, borderManager);
        portalManager.loadAllPortals();

        Bukkit.getPluginManager().registerEvents(
                new PortalListener(this, portalManager, dataManager),
                this
        );
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, borderManager, scoreboardManager),
                this
        );

        LevelBorderCommand command = new LevelBorderCommand(borderManager, scoreboardManager, timerManager);
        PluginCommand levelBorderCommand = getCommand("levelborder");

        if (levelBorderCommand != null) {
            levelBorderCommand.setExecutor(command);
            levelBorderCommand.setTabCompleter(command);
        } else {
            getLogger().severe("❌ Command 'levelborder' fehlt in der plugin.yml!");
        }

        ScoreboardCommand scoreCmd = new ScoreboardCommand(scoreboardManager);
        PluginCommand lbScoreCommand = getCommand("lbscore");

        if (lbScoreCommand != null) {
            lbScoreCommand.setExecutor(scoreCmd);
            lbScoreCommand.setTabCompleter(scoreCmd);
        } else {
            getLogger().severe("❌ Command 'lbscore' fehlt in der plugin.yml!");
        }

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

        getLogger().info(ConsoleColor.GOLD + "💾 LevelBorder beendet." + ConsoleColor.RESET);
    }



    private void saveBorderDataFile() {
        File file = new File(getDataFolder(), "BorderData.yml");
        if (!file.exists()) {
            saveResource("BorderData.yml", false);
            getLogger().info("📄 BorderData.yml wurde erstellt.");
        }
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }
}