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
    private final Map<World, List<PortalEntry>> portalCache = new HashMap<>();

    private static final int PORTAL_SEARCH_HORIZONTAL_RADIUS = 4;
    private static final int PORTAL_SEARCH_VERTICAL_RADIUS = 3;

    public PortalManager(JavaPlugin plugin, BorderDataManager data) {
        this.plugin = plugin;
        this.data = data;
        this.portalDir = new File(plugin.getDataFolder(), "portals");

        if (!portalDir.exists() && !portalDir.mkdirs()) {
            plugin.getLogger().warning("❌ Portal-Ordner konnte nicht erstellt werden: " + portalDir.getPath());
        }
    }

    private record PortalShape(
            World world,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {

        private String key() {
            return minX + "_" + minY + "_" + minZ + "__" + maxX + "_" + maxY + "_" + maxZ;
        }

        private Location center() {
            return new Location(
                    world,
                    (minX + maxX) / 2.0 + 0.5,
                    (minY + maxY) / 2.0,
                    (minZ + maxZ) / 2.0 + 0.5
            );
        }
    }

    private record PortalEntry(String key, Location center) {
    }

    public void loadAllPortals() {
        for (World world : Bukkit.getWorlds()) {
            loadWorldPortals(world);
        }
    }

    private void loadWorldPortals(World world) {

        File file = new File(portalDir, world.getName() + ".yml");
        List<PortalEntry> list = new ArrayList<>();

        if (file.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            for (String key : cfg.getKeys(false)) {
                Location center = new Location(
                        world,
                        cfg.getDouble(key + ".x"),
                        cfg.getDouble(key + ".y"),
                        cfg.getDouble(key + ".z")
                );

                list.add(new PortalEntry(key, center));
            }
        }

        portalCache.put(world, list);
    }

    public Location getNearestPortal(Location target) {

        World w = target.getWorld();
        if (w == null) return null;

        List<PortalEntry> portals = portalCache.get(w);
        if (portals == null || portals.isEmpty()) return null;

        Location best = null;
        double bestDist = Double.MAX_VALUE;

        for (PortalEntry entry : portals) {
            Location l = entry.center();

            double d = l.distanceSquared(target);
            if (d < bestDist) {
                bestDist = d;
                best = l;
            }
        }
        return best != null ? best.clone() : null;
    }

    public void registerPortal(Location location) {

        if (location == null || location.getWorld() == null) return;

        PortalShape shape = findPortalShape(location);
        if (shape == null) return;

        Location center = shape.center();
        World w = center.getWorld();
        if (w == null) return;

        List<PortalEntry> list = portalCache.computeIfAbsent(w, _ -> new ArrayList<>());

        String key = shape.key();

        for (PortalEntry existing : list) {
            if (existing.key().equals(key)) {
                return;
            }

            if (existing.center().distanceSquared(center) <= 1.0) {
                return;
            }
        }

        File file = new File(portalDir, w.getName() + ".yml");
        YamlConfiguration cfg = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();

        if (cfg.contains(key)) {
            return;
        }

        list.add(new PortalEntry(key, center.clone()));

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

        PortalShape shape = findPortalShape(start);
        if (shape == null) return null;

        return shape.center();
    }

    private PortalShape findPortalShape(Location start) {

        if (start == null || start.getWorld() == null) return null;

        World w = start.getWorld();

        Block seed = findNearestPortalBlock(start);
        if (seed == null) return null;

        int sx = seed.getX();
        int sy = seed.getY();
        int sz = seed.getZ();

        boolean xAxis = isPortalBlock(w, sx + 1, sy, sz)
                || isPortalBlock(w, sx - 1, sy, sz);

        boolean zAxis = isPortalBlock(w, sx, sy, sz + 1)
                || isPortalBlock(w, sx, sy, sz - 1);

        if (!xAxis && !zAxis) {
            xAxis = true;
        }

        Set<String> visited = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(seed);

        int minX = sx;
        int maxX = sx;
        int minY = sy;
        int maxY = sy;
        int minZ = sz;
        int maxZ = sz;

        while (!queue.isEmpty()) {

            Block current = queue.poll();

            String key = current.getX() + ":" + current.getY() + ":" + current.getZ();
            if (!visited.add(key)) continue;

            if (current.getType() != Material.NETHER_PORTAL) continue;

            int x = current.getX();
            int y = current.getY();
            int z = current.getZ();

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);

            queue.add(w.getBlockAt(x, y + 1, z));
            queue.add(w.getBlockAt(x, y - 1, z));

            if (xAxis) {
                queue.add(w.getBlockAt(x + 1, y, z));
                queue.add(w.getBlockAt(x - 1, y, z));
            } else {
                queue.add(w.getBlockAt(x, y, z + 1));
                queue.add(w.getBlockAt(x, y, z - 1));
            }
        }

        return new PortalShape(w, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private Block findNearestPortalBlock(Location start) {

        World w = start.getWorld();
        if (w == null) return null;

        int bx = start.getBlockX();
        int by = start.getBlockY();
        int bz = start.getBlockZ();

        Block best = null;
        double bestDist = Double.MAX_VALUE;

        for (int y = by - PORTAL_SEARCH_VERTICAL_RADIUS; y <= by + PORTAL_SEARCH_VERTICAL_RADIUS; y++) {
            for (int dx = -PORTAL_SEARCH_HORIZONTAL_RADIUS; dx <= PORTAL_SEARCH_HORIZONTAL_RADIUS; dx++) {
                for (int dz = -PORTAL_SEARCH_HORIZONTAL_RADIUS; dz <= PORTAL_SEARCH_HORIZONTAL_RADIUS; dz++) {

                    int x = bx + dx;
                    int z = bz + dz;

                    if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;

                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() != Material.NETHER_PORTAL) continue;

                    double dist = b.getLocation().distanceSquared(start);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = b;
                    }
                }
            }
        }

        return best;
    }

    private boolean isPortalBlock(World w, int x, int y, int z) {

        if (!w.isChunkLoaded(x >> 4, z >> 4)) return false;

        return w.getBlockAt(x, y, z).getType() == Material.NETHER_PORTAL;
    }

    public Location createOverworldPortal(World overworld, Location fromNether) {

        int x = clampToBorder(fromNether.getBlockX() * 8);
        int z = clampToBorder(fromNether.getBlockZ() * 8);

        Chunk c = overworld.getChunkAt(x >> 4, z >> 4);
        if (!c.isLoaded()) c.load();

        int y = findSafeOverworldY(overworld, x, z);

        clearArea(overworld, x, y, z);
        buildPortal(overworld, x, y, z);

        Location center = findPortalCenter(new Location(overworld, x + 0.5, y + 1, z + 0.5));
        if (center == null) {
            center = new Location(overworld, x + 0.5, y + 1, z + 0.5);
        }

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

        Location center = findPortalCenter(new Location(nether, x + 0.5, y + 1, z + 0.5));
        if (center == null) {
            center = new Location(nether, x + 0.5, y + 1, z + 0.5);
        }

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

        List<PortalEntry> list = portalCache.get(w);
        if (list == null || list.isEmpty()) return;

        PortalEntry remove = null;
        double max = radius * radius;

        for (PortalEntry entry : list) {
            if (entry.center().distanceSquared(loc) <= max) {
                remove = entry;
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

        List<PortalEntry> list = portalCache.get(world);
        if (list != null) {
            for (PortalEntry entry : list) {
                Location l = entry.center();
                String key = entry.key();

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