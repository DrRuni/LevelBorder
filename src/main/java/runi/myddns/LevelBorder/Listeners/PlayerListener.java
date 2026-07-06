package runi.myddns.LevelBorder.Listeners;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import runi.myddns.LevelBorder.Manager.LevelBorderManager;
import runi.myddns.LevelBorder.Manager.BorderDataManager;
import runi.myddns.LevelBorder.Manager.ScoreboardManager;
import runi.myddns.LevelBorder.Manager.TimerManager;
import runi.myddns.LevelBorder.Utils.ColorUtil;

public class PlayerListener implements Listener {

    private final JavaPlugin plugin;
    private final LevelBorderManager borderManager;
    private final ScoreboardManager scoreboardManager;
    private final TimerManager timerManager;

    public PlayerListener(JavaPlugin plugin, LevelBorderManager borderManager, ScoreboardManager scoreboardManager, TimerManager timerManager) {
        this.plugin = plugin;
        this.borderManager = borderManager;
        this.scoreboardManager = scoreboardManager;
        this.timerManager = timerManager;
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        scoreboardManager.addPlayer(p);

        p.sendMessage("");
        p.sendMessage(ColorUtil.borderColor("Run´s LevelBorder"));
        p.sendMessage("");
        p.sendMessage(ChatColor.GRAY + "Starte mit " + ChatColor.AQUA + "/levelborder " + ChatColor.GOLD + "start");
        p.sendMessage("");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    public void onLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        BorderDataManager data = borderManager.getData();

        data.savePlayerLevel(player);

        if (!data.isActive() || data.getCenter() == null) return;

        int totalNow = data.getTotalLevelSum();
        int maxTotal = data.getMaxTotalLevel();

        if (totalNow > maxTotal) {
            int diff = totalNow - maxTotal;
            data.setMaxTotalLevel(totalNow);
            borderManager.growByLevel(player, diff);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        BorderDataManager data = borderManager.getData();

        if (!data.isActive() || data.getCenter() == null) return;

        if (!event.isBedSpawn()) {
            Location center = data.getCenter();
            World world = center.getWorld();
            int y = world.getHighestBlockYAt(center) + 1;
            event.setRespawnLocation(new Location(world, center.getX(), y, center.getZ()));
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        Location loc = event.getLocation();

        Bukkit.getConsoleSender().sendMessage(
                ChatColor.DARK_RED + "[VanillaSpawn] "
                        + event.getEntityType()
                        + " bei X:" + loc.getBlockX()
                        + " Y:" + loc.getBlockY()
                        + " Z:" + loc.getBlockZ()
                        + " Welt:" + loc.getWorld().getName()
        );
    }
}