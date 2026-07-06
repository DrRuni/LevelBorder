package runi.myddns.levelborder.Listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import runi.myddns.levelborder.Manager.BorderDataManager;
import runi.myddns.levelborder.Manager.LevelBorderManager;
import runi.myddns.levelborder.Manager.ScoreboardManager;
import runi.myddns.levelborder.Utils.ColorUtil;

@SuppressWarnings("unused")
public class PlayerListener implements Listener {

    private final JavaPlugin plugin;
    private final LevelBorderManager borderManager;
    private final ScoreboardManager scoreboardManager;

    public PlayerListener(JavaPlugin plugin, LevelBorderManager borderManager, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.borderManager = borderManager;
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        scoreboardManager.addPlayer(p);

        p.sendMessage(Component.empty());
        p.sendMessage(ColorUtil.borderColor("Run´s LevelBorder"));
        p.sendMessage(Component.empty());
        p.sendMessage(
                Component.text("Starte mit ", NamedTextColor.GRAY)
                        .append(Component.text("/levelborder ", NamedTextColor.AQUA))
                        .append(Component.text("start", NamedTextColor.GOLD))
        );
        p.sendMessage(Component.empty());
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
            if (world == null) return;

            int y = world.getHighestBlockYAt(center) + 1;
            event.setRespawnLocation(new Location(world, center.getX(), y, center.getZ()));
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!plugin.getConfig().getBoolean("mob-spawning.debug.vanilla-spawns", false)) return;

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        Location loc = event.getLocation();

        Bukkit.getConsoleSender().sendMessage(
                Component.text("[VanillaSpawn] "
                        + event.getEntityType()
                        + " bei X:" + loc.getBlockX()
                        + " Y:" + loc.getBlockY()
                        + " Z:" + loc.getBlockZ()
                        + " Welt:" + loc.getWorld().getName(), NamedTextColor.DARK_RED)
        );
    }
}