package runi.myddns.LevelBorder.Manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import runi.myddns.LevelBorder.Utils.ColorUtil;

public class TimerManager {

    private final JavaPlugin plugin;
    private final BorderDataManager data;

    private BukkitRunnable task;
    private long seconds;
    private float gradientTick = 0f;

    public TimerManager(JavaPlugin plugin, BorderDataManager data) {
        this.plugin = plugin;
        this.data = data;

        this.seconds = data.getTimerSeconds();
        start();
    }

    private void start() {
        if (task != null) return;

        task = new BukkitRunnable() {

            int saveCounter = 0;
            int tickCounter = 0;

            @Override
            public void run() {

                if (Bukkit.getOnlinePlayers().isEmpty()) return;

                if (!data.isActive()) {
                    clearActionBar();
                    return;
                }

                tickCounter++;

                if (tickCounter % 20 == 0) {
                    seconds++;
                    saveCounter++;

                    if (saveCounter >= 10) {
                        save();
                        saveCounter = 0;
                    }
                }

                String animated = ColorUtil.borderColorScrolling(
                        formatTime(seconds),
                        gradientTick
                );
                gradientTick += 0.35f;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar(animated);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 1L);
    }

    private void clearActionBar() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendActionBar(" ");
        }
    }

    private void save() {
        data.setTimer(seconds);
    }

    public void reset() {
        seconds = 0;
        save();
        clearActionBar();
    }

    public long getSeconds() {
        return seconds;
    }

    private String formatTime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;

        if (days > 0)
            return String.format("%dd %dh %dm", days, hours, minutes);

        if (hours > 0)
            return String.format("%dh %dm %ds", hours, minutes, secs);

        if (minutes > 0)
            return String.format("%dm %ds", minutes, secs);

        return secs + "s";
    }
}