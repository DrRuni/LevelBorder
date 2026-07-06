package runi.myddns.levelborder.Manager;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PortalManager {

    private final JavaPlugin plugin;
    private final BorderDataManager data;
    private final File portalDir;
    private final Map<World, List<Location>> portalCache = new HashMap<>();

    public PortalManager(JavaPlugin plugin, BorderDataManager data) {
        this.plugin = plugin;
        this.data = data;
        this.portalDir = new File(plugin.getDataFolder(), "portals");

        if (!portalDir.exists() && !portalDir.mkdirs()) {
            plugin.getLogger().warning("❌ Portal-Ordner konnte nicht erstellt werden: " + portalDir.getPath());
        }
    }

    public void loadAllPortals() {
        for (World world : Bukkit.getWorlds()) {
            loadWorldPortals(world);
        }
    }

    private void loadWorldPortals(World world) {

        File file = new File(portalDir, world.getName() + ".yml");
        List<Location> list = new ArrayList<>();

        if (file.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String key : cfg.getKeys(false)) {
                list.add(new Location(
                        world,
                        cfg.getDouble(key + ".x"),
                        cfg.getDouble(key + ".y"),
                        cfg.getDouble(key + ".z")
                ));
            }
        }
        portalCache.put(world, list);
    }

    public Location getNearestPortal(Location target) {

        World w = target.getWorld();
        if (w == null) return null;

        List<Location> portals = portalCache.get(w);
        if (portals == null || portals.isEmpty()) return null;

        Location best = null;
        double bestDist = Double.MAX_VALUE;

        for (Location l : portals) {
            double d = l.distanceSquared(target);
            if (d < bestDist) {
                bestDist = d;
                best = l;
            }
        }
        return best != null ? best.clone() : null;
    }

    public void registerPortal(Location center) {

        if (center == null || center.getWorld() == null) return;

        center = new Location(
                center.getWorld(),
                center.getBlockX() + 0.5,
                center.getBlockY(),
                center.getBlockZ() + 0.5
        );

        World w = center.getWorld();
        List<Location> list = portalCache.computeIfAbsent(w, _ -> new ArrayList<>());

        for (Location l : list) {
            if (
                    l.getBlockX() == center.getBlockX() &&
                            l.getBlockY() == center.getBlockY() &&
                            l.getBlockZ() == center.getBlockZ()
            ) {
                return;
            }
        }

        list.add(center.clone());

        File file = new File(portalDir, w.getName() + ".yml");
        YamlConfiguration cfg = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();

        String key = center.getBlockX() + "_" + center.getBlockY() + "_" + center.getBlockZ();

        cfg.set(key + ".x", center.getX());
        cfg.set(key + ".y", center.getY());
        cfg.set(key + ".z", center.getZ());

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("❌ Portal konnte nicht gespeichert werden: " + file.getPath());
            plugin.getLogger().warning(e.getMessage());
        }
    }

    public Location findPortalCenter(Location start) {

        World w = start.getWorld();
        if (w == null) return null;

        int bx = start.getBlockX();
        int bz = start.getBlockZ();

        for (int y = start.getBlockY() - 2; y <= start.getBlockY() + 2; y++) {

            int minX = bx, maxX = bx, minZ = bz, maxZ = bz;
            boolean found = false;

            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {

                    int x = bx + dx;
                    int z = bz + dz;

                    if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;

                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() == Material.NETHER_PORTAL) {
                        found = true;
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }

            if (found) {
                return new Location(
                        w,
                        (minX + maxX) / 2.0 + 0.5,
                        y,
                        (minZ + maxZ) / 2.0 + 0.5
                );
            }
        }
        return null;
    }

    public Location createOverworldPortal(World overworld, Location fromNether) {

        int x = clampToBorder(fromNether.getBlockX() * 8);
        int z = clampToBorder(fromNether.getBlockZ() * 8);

        Chunk c = overworld.getChunkAt(x >> 4, z >> 4);
        if (!c.isLoaded()) c.load();

        int y = findSafeOverworldY(overworld, x, z);

        clearArea(overworld, x, y, z);
        buildPortal(overworld, x, y, z);

        Location center = new Location(overworld, x + 0.5, y + 1, z + 0.5);
        registerPortal(center);
        return center;
    }

    public Location createNetherPortal(World nether, Location fromOverworld) {

        int x = fromOverworld.getBlockX();
        int z = fromOverworld.getBlockZ();

        Chunk c = nether.getChunkAt(x >> 4, z >> 4);
        if (!c.isLoaded()) c.load();

        int y = findSafeNetherY(nether, x, z);

        clearArea(nether, x, y, z);
        buildPortal(nether, x, y, z);

        Location center = new Location(nether, x + 0.5, y + 1, z + 0.5);
        registerPortal(center);
        return center;
    }

    private int clampToBorder(int value) {
        Location c = data.getCenter();
        if (c == null) return value;

        double r = data.getSize() / 2.0 - 5;

        double min = c.getX() - r;
        double max = c.getX() + r;

        return (int) Math.clamp(value, min, max);
    }

    public void removeNearestPortal(Location loc, double radius) {

        World w = loc.getWorld();
        if (w == null) return;

        List<Location> list = portalCache.get(w);
        if (list == null || list.isEmpty()) return;

        Location remove = null;
        double max = radius * radius;

        for (Location l : list) {
            if (l.distanceSquared(loc) <= max) {
                remove = l;
                break;
            }
        }

        if (remove == null) return;

        list.remove(remove);
        saveWorldPortals(w);
    }

    private void buildPortal(World w, int x, int y, int z) {

        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                Block b = w.getBlockAt(x + dx, y + dy, z);
                if (dx == -1 || dx == 2 || dy == 0 || dy == 4) {
                    b.setType(Material.OBSIDIAN, false);
                } else {
                    b.setType(Material.AIR, false);
                }
            }
        }

        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                w.getBlockAt(x + dx, y + dy, z)
                        .setType(Material.NETHER_PORTAL, false);
            }
        }
    }

    private void clearArea(World w, int x, int y, int z) {
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    w.getBlockAt(x + dx, y + dy, z + dz).setType(Material.AIR);
                }
            }
        }
    }

    private int findSafeNetherY(World nether, int x, int z) {
        for (int y = 32; y <= 96; y++) {

            if (!nether.isChunkLoaded(x >> 4, z >> 4)) continue;

            Block feet = nether.getBlockAt(x, y, z);
            Block head = nether.getBlockAt(x, y + 1, z);
            Block ground = nether.getBlockAt(x, y - 1, z);

            if (!feet.getType().isAir()) continue;
            if (!head.getType().isAir()) continue;

            Material g = ground.getType();
            if (g == Material.LAVA || g == Material.BEDROCK) continue;

            return y;
        }
        return 64;
    }

    private int findSafeOverworldY(World world, int x, int z) {

        int top = world.getHighestBlockYAt(x, z);
        int minY = world.getMinHeight() + 5;

        for (int y = top; y > minY; y--) {

            if (portalFits(world, x, y, z, true))  return y;
            if (portalFits(world, x, y, z, false)) return y;
        }

        return world.getSeaLevel();
    }

    private boolean portalFits(World w, int x, int y, int z, boolean xAxis) {

        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 1; dy <= 3; dy++) {

                int px = x + (xAxis ? dx : 0);
                int pz = z + (xAxis ? 0 : dx);

                Block space = w.getBlockAt(px, y + dy, pz);
                if (!space.getType().isAir()) return false;
            }
        }

        for (int dx = 0; dx < 2; dx++) {

            int px = x + (xAxis ? dx : 0);
            int pz = z + (xAxis ? 0 : dx);

            Block ground = w.getBlockAt(px, y, pz);
            Material g = ground.getType();

            if (g == Material.BEDROCK) return false;
            if (ground.isLiquid()) return false;
            if (!g.isSolid()) return false;
        }

        return true;
    }

    private void saveWorldPortals(World world) {

        File file = new File(portalDir, world.getName() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();

        List<Location> list = portalCache.get(world);
        if (list != null) {
            for (Location l : list) {
                String key = l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ();
                cfg.set(key + ".x", l.getX());
                cfg.set(key + ".y", l.getY());
                cfg.set(key + ".z", l.getZ());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("❌ Portal-Datei konnte nicht gespeichert werden: " + file.getPath());
            plugin.getLogger().warning(e.getMessage());
        }
    }

    public void clearPortalWorldData() {

        portalCache.clear();

        if (portalDir.exists()) {
            File[] files = portalDir.listFiles((_, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        plugin.getLogger().warning("❌ Portal-Datei konnte nicht gelöscht werden: " + file.getPath());
                    }
                }
            }
        }
    }
}