package runi.myddns.LevelBorder.Manager;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import runi.myddns.LevelBorder.Utils.ConsoleColor;

public class LevelBorderManager {

    private final JavaPlugin plugin;
    private final BorderDataManager data;
    private final ScoreboardManager scoreboard;

    public LevelBorderManager(JavaPlugin plugin, BorderDataManager data, ScoreboardManager scoreboard) {
        this.plugin = plugin;
        this.data = data;
        this.scoreboard = scoreboard;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (data.isActive()) applyBorder();
            }
        }.runTaskLater(plugin, 20L);
    }

    public void applyBorder() {
        if (data.getCenter() == null) return;
        applyBorderToAllWorlds(data.getSize(), 0);
    }

    public void setCenter(Location loc) {
        loc.setX(Math.floor(loc.getX()) + 0.5);
        loc.setZ(Math.floor(loc.getZ()) + 0.5);

        data.setCenter(loc);

        loc.getWorld().setSpawnLocation(loc);
    }

    public void setSize(double level) {
        double diameter = (level * 2) + 1;
        data.setSize(diameter);

        if (data.isActive()) {
            applyBorderToAllWorlds(diameter, 0);
        }
    }

    public void setActive(boolean active) {
        data.setActive(active);

        if (active) {
            if (data.getSize() <= 1.0) data.setSize(1.0);
            applyBorderToAllWorlds(data.getSize(), 0);
        } else {
            applyBorderToAllWorlds(1_000_000, 0);
        }
    }

    public void growByLevel(Player trigger, int diff) {

        double newSize = data.getSize() + (diff * 2);
        data.setSize(newSize);

        applyBorderToAllWorlds(newSize, 3);

        Bukkit.broadcastMessage(
                ChatColor.GREEN + "🌱 Neuer Levelrekord: " +
                        ChatColor.GOLD + data.getMaxTotalLevel() +
                        ChatColor.GREEN + " → Border auf " +
                        ChatColor.GOLD + ((int) newSize) + "m"
        );
    }

    public void resetBorder(Player initiator) {
        plugin.getLogger().info(ConsoleColor.RED + "     🔄 LevelBorder-Reset wurde von " + initiator.getName() + " ausgeführt." + ConsoleColor.RESET);

        applyBorderToAllWorlds(1_000_000, 0);
        data.setCenter(null);
        data.setActive(false);
        data.resetAllPlayerLevels();
    }

    private void applyBorderToWorld(World world, double size, long transitionSeconds) {
        if (world == null || data.getCenter() == null) return;

        Location c = data.getCenter();
        WorldBorder border = world.getWorldBorder();

        double cx = c.getX();
        double cz = c.getZ();

        border.setCenter(
                Math.floor(cx) + 0.5,
                Math.floor(cz) + 0.5
        );

        border.changeSize(size, transitionSeconds);
    }


    private void applyBorderToAllWorlds(double size, long transitionSeconds) {
        Location center = data.getCenter();
        if (center == null) return;

        World overworld = center.getWorld();
        if (overworld == null) return;

        applyBorderToWorld(overworld, size, transitionSeconds);

        applyBorderToWorld(
                Bukkit.getWorld(overworld.getName() + "_nether"),
                size,
                transitionSeconds
        );

        applyBorderToWorld(
                Bukkit.getWorld(overworld.getName() + "_the_end"),
                size,
                transitionSeconds
        );
    }

    public double getRadius() {
        return data.getSize() / 2.0;
    }

    public Location getCenterForWorld(World world) {
        Location center = data.getCenter();
        if (center == null || world == null) return null;

        double cx = center.getX();
        double cy = center.getY();
        double cz = center.getZ();

        return new Location(world, Math.floor(cx) + 0.5, cy, Math.floor(cz) + 0.5);
    }

    public boolean isInsideBorder(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!data.isActive()) return false;

        Location center = getCenterForWorld(loc.getWorld());
        if (center == null) return false;

        double radius = getRadius();

        double dx = Math.abs(loc.getX() - center.getX());
        double dz = Math.abs(loc.getZ() - center.getZ());

        return dx <= radius && dz <= radius;
    }

    public boolean isOutsideBorder(Location loc) {
        return !isInsideBorder(loc);
    }

    public BorderDataManager getData() { return data; }
    public JavaPlugin getPlugin() { return plugin; }
}