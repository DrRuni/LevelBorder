package runi.myddns.levelborder.Manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.OfflinePlayer;
import org.bukkit.potion.PotionEffect;
import java.io.File;

import runi.myddns.levelborder.Utils.ConsoleColor;

@SuppressWarnings("ClassCanBeRecord")
public class LevelBorderManager {

    private final JavaPlugin plugin;
    private final BorderDataManager data;

    public LevelBorderManager(JavaPlugin plugin, BorderDataManager data) {
        this.plugin = plugin;
        this.data = data;

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
        if (loc == null || loc.getWorld() == null) return;

        loc.setX(Math.floor(loc.getX()) + 0.5);
        loc.setZ(Math.floor(loc.getZ()) + 0.5);

        data.setCenter(loc);

        loc.getWorld().setSpawnLocation(loc);

        if (data.isActive()) {
            applyBorderToAllWorlds(data.getSize(), 0);
        }
    }

    public void setSize(double level) {
        double diameter = (level * 2) + 1;
        data.setSize(diameter);

        if (data.isActive()) {
            applyBorderToAllWorlds(diameter, 0);
        }
    }

    public void setDiameter(double diameter) {
        if (diameter < 1.0) {
            diameter = 1.0;
        }

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

        applyBorderToAllWorlds(newSize, 60);

        Bukkit.broadcast(
                Component.text("🌱 Neuer Levelrekord durch ", NamedTextColor.GREEN)
                        .append(Component.text(trigger.getName(), NamedTextColor.GOLD))
                        .append(Component.text(": ", NamedTextColor.GREEN))
                        .append(Component.text(data.getMaxTotalLevel(), NamedTextColor.GOLD))
                        .append(Component.text(" → Border auf ", NamedTextColor.GREEN))
                        .append(Component.text((int) newSize + "m", NamedTextColor.GOLD))
        );
    }

    public void resetBorder(Player initiator) {
        plugin.getLogger().info(ConsoleColor.RED + "     🔄 LevelBorder-Reset wurde von " + initiator.getName() + " ausgeführt." + ConsoleColor.RESET);

        applyBorderToAllWorlds(1_000_000, 0);

        resetOnlinePlayers();
        resetOfflinePlayerData();

        data.resetToDefaults();
    }

    private void resetOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setLevel(0);
            p.setExp(0.0f);
            p.setTotalExperience(0);

            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setSaturation(20.0f);
            p.setExhaustion(0.0f);

            p.setFireTicks(0);
            p.setFallDistance(0.0f);

            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }

            p.getInventory().clear();
            p.getEnderChest().clear();

            p.setArrowsInBody(0);
            p.setRemainingAir(p.getMaximumAir());
        }
    }

    private void resetOfflinePlayerData() {
        World mainWorld = Bukkit.getWorlds().getFirst();
        File playerDataFolder = new File(mainWorld.getWorldFolder(), "playerdata");

        if (!playerDataFolder.exists() || !playerDataFolder.isDirectory()) {
            plugin.getLogger().warning("❌ PlayerData-Ordner nicht gefunden: " + playerDataFolder.getPath());
            return;
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.isOnline()) {
                continue;
            }

            File dataFile = new File(playerDataFolder, offlinePlayer.getUniqueId() + ".dat");
            File oldDataFile = new File(playerDataFolder, offlinePlayer.getUniqueId() + ".dat_old");

            if (dataFile.exists() && !dataFile.delete()) {
                plugin.getLogger().warning("❌ Konnte PlayerData nicht löschen: " + dataFile.getName());
            }

            if (oldDataFile.exists() && !oldDataFile.delete()) {
                plugin.getLogger().warning("❌ Konnte alte PlayerData nicht löschen: " + oldDataFile.getName());
            }
        }
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

    public BorderDataManager getData() { return data; }
    public JavaPlugin getPlugin() { return plugin; }
}