package runi.myddns.levelborder.Listeners;

import org.bukkit.GameRules;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import runi.myddns.levelborder.Manager.BorderDataManager;

public class WorldOptionsListener implements Listener {

    private final BorderDataManager dataManager;

    public WorldOptionsListener(BorderDataManager dataManager) {
        this.dataManager = dataManager;
    }
    @SuppressWarnings("unused")
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        event.getWorld().setGameRule(
                GameRules.KEEP_INVENTORY,
                dataManager.isKeepInventoryEnabled()
        );
    }
}