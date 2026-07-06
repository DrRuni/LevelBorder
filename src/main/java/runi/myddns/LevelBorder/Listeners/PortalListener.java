package runi.myddns.levelborder.Listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import runi.myddns.levelborder.LevelBorderMain;
import runi.myddns.levelborder.Manager.BorderDataManager;
import runi.myddns.levelborder.Manager.PortalManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class PortalListener implements Listener {

    private final LevelBorderMain plugin;
    private final PortalManager portalManager;
    private final BorderDataManager borderData;

    private final Map<UUID, Long> lastPortalUse = new HashMap<>();
    private static final long COOLDOWN_MS = 3000;

    public PortalListener(LevelBorderMain plugin,
                          PortalManager portalManager,
                          BorderDataManager borderData) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        this.borderData = borderData;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {

        if (e.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;

        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();

        Long last = lastPortalUse.get(id);
        if (last != null && now - last < COOLDOWN_MS) return;

        World fromWorld = e.getFrom().getWorld();
        if (fromWorld == null) return;

        Location portalCenter = portalManager.findPortalCenter(e.getFrom());
        if (portalCenter == null) portalCenter = e.getFrom().clone();

        portalManager.registerPortal(portalCenter);

        Location target = null;

        // 🌍 OVERWORLD → NETHER
        if (fromWorld.getEnvironment() == World.Environment.NORMAL) {

            World nether = Bukkit.getWorld(fromWorld.getName() + "_nether");
            if (nether == null) return;

            Location search = portalCenter.clone();
            search.setWorld(nether);
            search.setX(portalCenter.getX());
            search.setZ(portalCenter.getZ());

            target = portalManager.getNearestPortal(search);
            if (target == null) {
                target = portalManager.createNetherPortal(nether, portalCenter);
            }
        }

        // 🔥 NETHER → OVERWORLD
        else if (fromWorld.getEnvironment() == World.Environment.NETHER) {

            World overworld = Bukkit.getWorlds().getFirst();

            Location search = portalCenter.clone();
            search.setWorld(overworld);

            int x = clampToBorder(portalCenter.getBlockX());
            int z = clampToBorder(portalCenter.getBlockZ());

            search.setX(x);
            search.setZ(z);

            target = portalManager.getNearestPortal(search);
            if (target == null) {
                target = portalManager.createOverworldPortal(overworld, portalCenter);
            }
        }

        if (target == null) return;

        e.setCancelled(true);
        e.setCanCreatePortal(false);

        lastPortalUse.put(id, now);
        Location finalTarget = target.clone();

        Bukkit.getScheduler().runTask(plugin, () -> {
            p.teleport(finalTarget, PlayerTeleportEvent.TeleportCause.PLUGIN);
            p.setPortalCooldown(200);
        });
    }

    private int clampToBorder(int value) {

        Location center = borderData.getCenter();
        if (center == null) return value;

        double radius = borderData.getSize() / 2.0 - 5;

        double min = center.getX() - radius;
        double max = center.getX() + radius;

        return (int) Math.clamp(value, min, max);
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent e) {

        Block block = e.getBlock();
        World w = block.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL
                && w.getEnvironment() != World.Environment.NETHER) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location center = plugin.getPortalManager()
                    .findPortalCenter(block.getLocation());

            if (center != null) {
                plugin.getPortalManager().registerPortal(center);
            }
        }, 2L);
    }

    @EventHandler
    public void onPortalBreak(BlockBreakEvent e) {

        Block b = e.getBlock();
        if (b.getType() != Material.NETHER_PORTAL
                && b.getType() != Material.OBSIDIAN) return;

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    PortalManager pm = plugin.getPortalManager();

                    Location center = pm.findPortalCenter(b.getLocation());
                    if (center != null && center.getBlock().getType() == Material.NETHER_PORTAL) {
                        return;
                    }

                    pm.removeNearestPortal(b.getLocation(), 5.0);
                },
                2L
        );
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent e) {

        Location center = borderData.getCenter();
        if (center == null) return;

        double radius = borderData.getSize() / 2.0 - 1;

        for (BlockState state : e.getBlocks()) {

            Location l = state.getLocation();
            World w = l.getWorld();
            if (w == null) continue;

            if (w.getEnvironment() != World.Environment.NORMAL
                    && w.getEnvironment() != World.Environment.NETHER) {
                continue;
            }

            double dx = Math.abs(l.getX() - center.getX());
            double dz = Math.abs(l.getZ() - center.getZ());

            if (dx > radius || dz > radius) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastPortalUse.remove(e.getPlayer().getUniqueId());
    }
}
