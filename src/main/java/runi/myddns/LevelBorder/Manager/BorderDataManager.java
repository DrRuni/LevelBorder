package runi.myddns.LevelBorder.Manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import runi.myddns.LevelBorder.LevelBorderMain;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BorderDataManager {

    private final LevelBorderMain plugin;
    private final File dataFile;
    private final YamlConfiguration dataCfg;

    public BorderDataManager(LevelBorderMain plugin) {
        this.plugin = plugin;

        dataFile = new File(plugin.getDataFolder(), "BorderData.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("BorderData.yml", false);
        }

        dataCfg = YamlConfiguration.loadConfiguration(dataFile);
    }

    public Location getCenter() {
        String worldName = dataCfg.getString("border.world", null);
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        if (!dataCfg.isConfigurationSection("border.center")) return null;

        double x = dataCfg.getDouble("border.center.x", 0);
        double y = dataCfg.getDouble("border.center.y", 64);
        double z = dataCfg.getDouble("border.center.z", 0);

        return new Location(world, x, y, z);
    }

    public void setCenter(Location center) {
        if (center == null) {
            dataCfg.set("border.center", null);
            dataCfg.set("border.world", null);
            save();
            return;
        }

        if (center.getWorld() == null) return;

        dataCfg.set("border.world", center.getWorld().getName());
        dataCfg.set("border.center.x", center.getX());
        dataCfg.set("border.center.y", center.getY());
        dataCfg.set("border.center.z", center.getZ());
        save();
    }

    public double getSize() {
        return dataCfg.getDouble("border.size", 50.0);
    }

    public void setSize(double size) {
        dataCfg.set("border.size", size);
        save();
    }

    public boolean isActive() {
        return dataCfg.getBoolean("border.active", false);
    }

    public void setActive(boolean active) {
        dataCfg.set("border.active", active);
        save();
    }

    public int getMaxTotalLevel() {
        return dataCfg.getInt("border.max-total-level", 0);
    }

    public void setMaxTotalLevel(int level) {
        dataCfg.set("border.max-total-level", level);
        save();
    }

    public void savePlayerLevel(Player player) {
        String path = "players." + player.getName();
        dataCfg.set(path + ".uuid", player.getUniqueId().toString());
        dataCfg.set(path + ".level", player.getLevel());
        save();
    }

    public Map<String, Integer> getAllPlayerLevels() {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (dataCfg.isConfigurationSection("players")) {
            for (String name : dataCfg.getConfigurationSection("players").getKeys(false)) {
                map.put(name, dataCfg.getInt("players." + name + ".level", 0));
            }
        }
        return map;
    }

    public boolean isScoreboardVisible() {
        return dataCfg.getBoolean("scoreboard.visible", true);
    }

    public void setScoreboardVisible(boolean visible) {
        dataCfg.set("scoreboard.visible", visible);
        save();
    }

    public long getTimerSeconds() {
        return dataCfg.getLong("timer.seconds", 0);
    }

    public boolean isTimerPaused() {
        return dataCfg.getBoolean("timer.paused", true);
    }

    public void setTimer(long seconds) {
        dataCfg.set("timer.seconds", seconds);
        save();
    }

    private void save() {
        try {
            dataCfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("❌ Fehler beim Speichern von BorderData.yml");
        }
    }

    public int getTotalLevelSum() {
        int total = 0;

        if (dataCfg.isConfigurationSection("players")) {
            for (String name : dataCfg.getConfigurationSection("players").getKeys(false)) {
                total += dataCfg.getInt("players." + name + ".level", 0);
            }
        }

        return total;
    }

    public void resetAllPlayerLevels() {

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setLevel(0);
        }

        if (dataCfg.isConfigurationSection("players")) {
            dataCfg.set("players", null);
        }

        dataCfg.set("border.max-total-level", 0);
        dataCfg.set("border.size", 1.0);

        save();
    }
}