package runi.myddns.levelborder.Manager;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MobSpawnManager {

    private final NamespacedKey mobKey;

    private final JavaPlugin plugin;
    private final BorderDataManager data;
    private final LevelBorderManager borderManager;
    private record MobOption(EntityType type, int weight, int maxPackSize) {}

    private BukkitTask task;

    public MobSpawnManager(JavaPlugin plugin, BorderDataManager data, LevelBorderManager borderManager) {
        this.plugin = plugin;
        this.data = data;
        this.borderManager = borderManager;
        this.mobKey = new NamespacedKey(plugin, "levelborder_mob");
    }

    public void start() {
        stop();

        if (!plugin.getConfig().getBoolean("mob-spawning.enabled", true)) {
            plugin.getLogger().info("LevelBorder MobSpawning ist deaktiviert.");
            return;
        }

        long interval = plugin.getConfig().getLong("mob-spawning.interval-ticks", 20L);
        if (interval < 20L) interval = 20L;

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::runSpawnTick, interval, interval);

        plugin.getLogger().info("LevelBorder MobSpawning gestartet. Intervall: " + interval + " Ticks");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void runSpawnTick() {
        if (!plugin.getConfig().getBoolean("mob-spawning.enabled", true)) {
            debug("Abbruch: mob-spawning.enabled ist false.");
            return;
        }

        if (!data.isActive()) {
            debug("Abbruch: Border ist nicht aktiv.");
            return;
        }

        if (data.getCenter() == null) {
            debug("Abbruch: Border-Center ist null.");
            return;
        }

        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(Player::isOnline)
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .collect(java.util.stream.Collectors.toList());

        if (players.isEmpty()) {
            debug("Abbruch: keine gültigen Spieler online. Spectator zählt nicht.");
            return;
        }

        int currentMobs = countLevelBorderMobs();
        int perWorldCap = plugin.getConfig().getInt("mob-spawning.caps.per-world", 80);

        if (currentMobs >= perWorldCap) {
            debug("Abbruch: Welt-Cap erreicht. PluginMobs=" + currentMobs + "/" + perWorldCap);
            return;
        }

        int rounds = plugin.getConfig().getInt("mob-spawning.attempts.rounds-per-check", 1);
        if (rounds < 1) rounds = 1;

        debug("SpawnTick: Spieler=" + players.size()
                + ", PluginMobs=" + currentMobs + "/" + perWorldCap
                + ", Runden=" + rounds
                + ", BorderSize=" + data.getSize());

        for (int i = 0; i < rounds; i++) {
            Player player = players.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(players.size()));
            debug("Runde " + (i + 1) + ": Spieler=" + player.getName()
                    + ", Welt=" + player.getWorld().getName()
                    + ", Pos=" + locShort(player.getLocation()));

            trySpawnNearPlayer(player);
        }
    }

    private void trySpawnNearPlayer(Player player) {
        World world = player.getWorld();

        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            debug("Abbruch Spieler " + player.getName() + ": Difficulty ist PEACEFUL.");
            return;
        }

        if (world.getEnvironment() != World.Environment.NORMAL) {
            debug("Abbruch Spieler " + player.getName() + ": Welt ist nicht NORMAL, sondern " + world.getEnvironment());
            return;
        }

        int perPlayerCap = plugin.getConfig().getInt("mob-spawning.caps.per-player", 8);
        int nearMobs = countLevelBorderMobsNearPlayer(player.getLocation());

        if (nearMobs >= perPlayerCap) {
            debug("Abbruch Spieler " + player.getName() + ": Spieler-Cap erreicht. Nähe=" + nearMobs + "/" + perPlayerCap);
            return;
        }

        int candidates = plugin.getConfig().getInt("mob-spawning.attempts.candidate-positions", 5);
        if (candidates < 1) candidates = 1;

        debug("Starte Kandidatenprüfung für " + player.getName()
                + ": candidates=" + candidates
                + ", nearMobs=" + nearMobs + "/" + perPlayerCap);

        for (int i = 0; i < candidates; i++) {
            Location candidate = randomCandidateAround(player);

            if (candidate == null) {
                debug("Kandidat " + (i + 1) + ": null. Wahrscheinlich Chunk nicht geladen oder kein Boden gefunden.");
                continue;
            }

            debug("Kandidat " + (i + 1) + ": " + locShort(candidate)
                    + ", innen=" + borderManager.isInsideBorder(candidate)
                    + ", blockLight=" + candidate.getBlock().getLightFromBlocks()
                    + ", skyLight=" + candidate.getBlock().getLightFromSky());

            if (isBlockedByBorderMode(candidate)) {
                debug("Kandidat " + (i + 1) + " abgelehnt: Border-Modus erlaubt diese Zone nicht.");
                continue;
            }

            if (isTooCloseToAnyPlayer(candidate)) {
                debug("Kandidat " + (i + 1) + " abgelehnt: zu nah an einem Spieler.");
                continue;
            }

            if (isTooFarFromAllPlayers(candidate)) {
                debug("Kandidat " + (i + 1) + " abgelehnt: zu weit von allen Spielern.");
                continue;
            }

            if (!isChunkLoaded(candidate)) {
                debug("Kandidat " + (i + 1) + " abgelehnt: Chunk nicht geladen.");
                continue;
            }

            if (isChunkCapReached(candidate)) {
                debug("Kandidat " + (i + 1) + " abgelehnt: Chunk-Cap erreicht.");
                continue;
            }

            MobOption option = randomMonsterType(candidate);

            if (option == null) {
                debug("Kandidat " + (i + 1) + " abgelehnt: kein Mobtyp verfügbar.");
                continue;
            }

            EntityType type = option.type();

            debug("Kandidat " + (i + 1) + ": ausgewürfelter Mob=" + type);

            int spawned = spawnPack(type, candidate, option.maxPackSize());

            debug("Kandidat " + (i + 1) + ": SpawnPack fertig. Typ=" + type + ", gespawnt=" + spawned);

            return;
        }

        debug("Keine gültige Position für " + player.getName() + " gefunden.");
    }

    private Location randomCandidateAround(Player player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        World world = player.getWorld();

        int maxDistance = plugin.getConfig().getInt("mob-spawning.distance.max-to-player", 96);

        double borderSize = data.getSize();
        double assistInsideUntil = plugin.getConfig().getDouble(
                "mob-spawning.assist-inside-until-border-size",
                31.0
        );

        boolean smallBorderMode = borderSize < assistInsideUntil;

        double insideChance = plugin.getConfig().getDouble(
                "mob-spawning.small-border.inside-candidate-chance",
                0.70
        );

        boolean tryInsideBorder = smallBorderMode && random.nextDouble() < insideChance;

        int x;
        int z;

        if (tryInsideBorder) {
            Location center = borderManager.getCenterForWorld(world);
            if (center == null) return null;

            double radius = borderManager.getRadius();

            int minX = (int) Math.floor(center.getX() - radius);
            int maxX = (int) Math.floor(center.getX() + radius);
            int minZ = (int) Math.floor(center.getZ() - radius);
            int maxZ = (int) Math.floor(center.getZ() + radius);

            x = random.nextInt(minX, maxX + 1);
            z = random.nextInt(minZ, maxZ + 1);
        } else {
            double angle = random.nextDouble(0, Math.PI * 2);
            double distance = random.nextDouble(0, maxDistance);

            x = player.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            z = player.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
        }

        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;

        boolean inside = borderManager.isInsideBorder(new Location(world, x + 0.5, player.getLocation().getY(), z + 0.5));

        // Außen soll nur Oberfläche nutzen.
        if (!inside) {
            return findNaturalSurface(new Location(world, x, 0, z));
        }

        // Innen darf Oberfläche + Untergrund nutzen.
        double surfaceChance = plugin.getConfig().getDouble(
                "mob-spawning.vertical-search.surface-chance",
                0.70
        );

        if (random.nextDouble() < surfaceChance) {
            return findNaturalSurface(new Location(world, x, 0, z));
        }

        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 3;

        int y = random.nextInt(minY, maxY);

        Location start = new Location(world, x + 0.5, y, z + 0.5);

        int searchDown = plugin.getConfig().getInt(
                "mob-spawning.vertical-search.underground-search-down",
                12
        );

        return findSpawnFloorNear(start, searchDown);
    }

    private Location findSpawnFloorNear(Location start, int searchDown) {
        World world = start.getWorld();
        if (world == null) return null;

        int x = start.getBlockX();
        int z = start.getBlockZ();
        int y = start.getBlockY();

        int minY = world.getMinHeight() + 1;

        for (int yy = y; yy >= Math.max(minY, y - searchDown); yy--) {
            Block floor = world.getBlockAt(x, yy - 1, z);
            Block feet = world.getBlockAt(x, yy, z);
            Block head = world.getBlockAt(x, yy + 1, z);

            if (floor.getType().isSolid() && feet.isPassable() && head.isPassable()) {
                return new Location(world, x + 0.5, yy, z + 0.5);
            }
        }

        return null;
    }

    private boolean isBlockedByBorderMode(Location loc) {
        double borderSize = data.getSize();
        double assistInsideUntil = plugin.getConfig().getDouble(
                "mob-spawning.assist-inside-until-border-size",
                31.0
        );

        boolean inside = borderManager.isInsideBorder(loc);

        return borderSize >= assistInsideUntil && inside;
    }

    private boolean isTooCloseToAnyPlayer(Location loc) {
        int minDistance = plugin.getConfig().getInt("mob-spawning.distance.min-to-player", 28);
        double minSquared = minDistance * minDistance;

        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;

            if (player.getLocation().distanceSquared(loc) < minSquared) {
                return true;
            }
        }

        return false;
    }

    private boolean isTooFarFromAllPlayers(Location loc) {
        int maxDistance = plugin.getConfig().getInt("mob-spawning.distance.max-to-player", 96);
        double maxSquared = maxDistance * maxDistance;

        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;

            if (player.getLocation().distanceSquared(loc) <= maxSquared) {
                return false;
            }
        }

        return true;
    }

    private boolean isChunkLoaded(Location loc) {
        return loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    private boolean isChunkCapReached(Location loc) {
        int cap = plugin.getConfig().getInt("mob-spawning.caps.per-chunk", 3);
        if (cap <= 0) return false;

        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;

        int count = 0;

        for (Entity entity : loc.getWorld().getEntities()) {
            if (!(entity instanceof Monster)) continue;
            if (!isLevelBorderMob(entity)) continue;

            Location eLoc = entity.getLocation();
            if ((eLoc.getBlockX() >> 4) == chunkX && (eLoc.getBlockZ() >> 4) == chunkZ) {
                count++;
                if (count >= cap) return true;
            }
        }

        return false;
    }

    private MobOption randomMonsterType(Location loc) {
        List<MobOption> options = new ArrayList<>();

        addMobOption(options, "zombie", EntityType.ZOMBIE);
        addMobOption(options, "zombie-villager", EntityType.ZOMBIE_VILLAGER);
        addMobOption(options, "skeleton", EntityType.SKELETON);
        addMobOption(options, "spider", EntityType.SPIDER);
        addMobOption(options, "creeper", EntityType.CREEPER);

        if (hasEndermanHeight(loc)) {
            addMobOption(options, "enderman", EntityType.ENDERMAN);
        }

        addMobOption(options, "witch", EntityType.WITCH);

        options.removeIf(option -> cannotSpawnTypeAt(option.type(), loc));

        if (options.isEmpty()) return null;

        int totalWeight = 0;
        for (MobOption option : options) {
            totalWeight += Math.max(0, option.weight());
        }

        if (totalWeight <= 0) return null;

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);

        for (MobOption option : options) {
            roll -= Math.max(0, option.weight());
            if (roll < 0) {
                return option;
            }
        }

        return options.getFirst();
    }

    private void addMobOption(List<MobOption> options, String pathName, EntityType type) {
        String path = "mob-spawning.mobs." + pathName;

        if (!plugin.getConfig().getBoolean(path + ".enabled", true)) return;

        int weight = plugin.getConfig().getInt(path + ".weight", 100);
        int maxPackSize = plugin.getConfig().getInt(path + ".max-pack-size", 3);

        if (weight <= 0 || maxPackSize <= 0) return;

        options.add(new MobOption(type, weight, maxPackSize));
    }

    private boolean cannotSpawnTypeAt(EntityType type, Location loc) {
        World world = loc.getWorld();
        if (world == null) return true;

        Block feet = loc.getBlock();
        Block head = world.getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());
        Block floor = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());

        if (feet.isLiquid() || head.isLiquid() || floor.isLiquid()) return true;
        if (feet.getType() == Material.WATER || head.getType() == Material.WATER || floor.getType() == Material.WATER) return true;

        if (feet.getLightFromBlocks() > 0) return true;

        boolean hasSkyAccess = feet.getLightFromSky() > 0;
        boolean isDay = world.getTime() < 13000 || world.getTime() > 23000;

        if (hasSkyAccess && isDay) return true;

        return switch (type) {
            case ZOMBIE, ZOMBIE_VILLAGER, SKELETON, CREEPER, WITCH -> cannotSpawnOneBlockMob(loc, 2);
            case SPIDER -> cannotSpawnSpider(loc);
            case ENDERMAN -> cannotSpawnOneBlockMob(loc, 3);
            default -> true;
        };
    }

    private boolean cannotSpawnOneBlockMob(Location loc, int height) {
        World world = loc.getWorld();
        if (world == null) return true;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        Block floor = world.getBlockAt(x, y - 1, z);
        if (!floor.getType().isSolid()) return true;

        for (int i = 0; i < height; i++) {
            if (!world.getBlockAt(x, y + i, z).isPassable()) {
                return true;
            }
        }

        return false;
    }

    private boolean cannotSpawnSpider(Location loc) {
        World world = loc.getWorld();
        if (world == null) return true;

        int baseX = loc.getBlockX();
        int y = loc.getBlockY();
        int baseZ = loc.getBlockZ();

        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                Block floor = world.getBlockAt(baseX + dx, y - 1, baseZ + dz);
                Block feet = world.getBlockAt(baseX + dx, y, baseZ + dz);
                Block head = world.getBlockAt(baseX + dx, y + 1, baseZ + dz);

                if (!floor.getType().isSolid()) return true;
                if (!feet.isPassable()) return true;
                if (!head.isPassable()) return true;
            }
        }

        return false;
    }

    private boolean hasEndermanHeight(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        for (int i = 0; i < 3; i++) {
            if (!world.getBlockAt(x, y + i, z).isPassable()) {
                return false;
            }
        }

        return true;
    }

    private int spawnPack(EntityType type, Location center, int mobMaxPackSize) {
        int maxPackSize = Math.max(1, mobMaxPackSize);

        int spawned = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int packSearchDown = plugin.getConfig().getInt(
                "mob-spawning.vertical-search.pack-search-down",
                4
        );

        for (int i = 0; i < maxPackSize; i++) {
            Location loc = center.clone().add(
                    random.nextInt(-4, 5),
                    0,
                    random.nextInt(-4, 5)
            );

            boolean inside = borderManager.isInsideBorder(loc);

            if (inside) {
                loc = findSpawnFloorNear(loc, packSearchDown);
            } else {
                loc = findNaturalSurface(loc);
            }

            if (loc == null) {
                debug("Pack " + type + ": Versuch " + (i + 1) + " abgelehnt: kein Boden nahe Packposition.");
                continue;
            }

            if (isBlockedByBorderMode(loc)) {
                debug("Pack " + type + ": Versuch " + (i + 1) + " abgelehnt: Border-Modus.");
                continue;
            }

            if (isTooCloseToAnyPlayer(loc)) {
                debug("Pack " + type + ": Versuch " + (i + 1) + " abgelehnt: zu nah an Spieler.");
                continue;
            }

            if (isTooFarFromAllPlayers(loc)) {
                debug("Pack " + type + ": Versuch " + (i + 1) + " abgelehnt: zu weit von Spieler.");
                continue;
            }

            if (cannotSpawnTypeAt(type, loc)) {
                debug("Pack " + type + ": Versuch " + (i + 1) + " abgelehnt: Spawnregeln nicht erfüllt bei "
                        + locShort(loc)
                        + ", blockLight=" + loc.getBlock().getLightFromBlocks()
                        + ", skyLight=" + loc.getBlock().getLightFromSky());
                continue;
            }

            Entity entity = loc.getWorld().spawnEntity(loc, type);
            entity.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);

            sendPluginSpawnDebug(type, loc);

            spawned++;
            debug("Pack " + type + ": gespawnt bei " + locShort(loc));

            if (spawned >= maxPackSize) {
                break;
            }
        }

        return spawned;
    }

    private int countLevelBorderMobs() {
        int count = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Monster && isLevelBorderMob(entity)) {
                    count++;
                }
            }
        }

        return count;
    }

    private int countLevelBorderMobsNearPlayer(Location center) {
        int maxDistance = plugin.getConfig().getInt("mob-spawning.distance.max-to-player", 96);
        double radiusSquared = maxDistance * maxDistance;

        int count = 0;

        for (Entity entity : center.getWorld().getEntities()) {
            if (!(entity instanceof Monster)) continue;
            if (!isLevelBorderMob(entity)) continue;

            if (entity.getLocation().distanceSquared(center) <= radiusSquared) {
                count++;
            }
        }

        return count;
    }

    private String locShort(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName()
                + " x=" + loc.getBlockX()
                + " y=" + loc.getBlockY()
                + " z=" + loc.getBlockZ();
    }

    private void sendPluginSpawnDebug(EntityType type, Location loc) {
        if (!debugPluginSpawns()) return;
        if (loc == null || loc.getWorld() == null) return;

        boolean inside = borderManager.isInsideBorder(loc);

        NamedTextColor color = inside ? NamedTextColor.GREEN : NamedTextColor.LIGHT_PURPLE;
        String zone = inside ? "INNEN" : "AUSSEN";

        Bukkit.getConsoleSender().sendMessage(
                Component.text("[PluginSpawn/" + zone + "] "
                        + type
                        + " bei X:" + loc.getBlockX()
                        + " Y:" + loc.getBlockY()
                        + " Z:" + loc.getBlockZ()
                        + " Welt:" + loc.getWorld().getName(), color)
        );
    }

    private Location findNaturalSurface(Location base) {
        World world = base.getWorld();
        if (world == null) return null;

        int x = base.getBlockX();
        int z = base.getBlockZ();

        int y = world.getHighestBlockYAt(x, z);

        for (int yy = y; yy >= world.getMinHeight() + 1; yy--) {
            Block floor = world.getBlockAt(x, yy, z);
            Block feet = world.getBlockAt(x, yy + 1, z);
            Block head = world.getBlockAt(x, yy + 2, z);

            Material floorType = floor.getType();

            if (isBadSurfaceBlock(floorType)) continue;

            // Nicht im Wasser / unter Wasser / in Flüssigkeiten spawnen.
            if (floor.isLiquid()) continue;
            if (feet.isLiquid()) continue;
            if (head.isLiquid()) continue;

            if (feet.getType() == Material.WATER || head.getType() == Material.WATER) continue;
            if (feet.getType() == Material.LAVA || head.getType() == Material.LAVA) continue;

            if (!floorType.isSolid()) continue;
            if (!feet.isPassable()) continue;
            if (!head.isPassable()) continue;

            if (world.getHighestBlockYAt(x, z) > yy) continue;

            return new Location(world, x + 0.5, yy + 1, z + 0.5);
        }

        return null;
    }

    private boolean isBadSurfaceBlock(Material type) {
        String name = type.name();

        if (name.endsWith("_LEAVES")) return true;
        if (name.endsWith("_LOG")) return true;
        if (name.endsWith("_WOOD")) return true;
        if (name.endsWith("_STEM")) return true;
        if (name.endsWith("_HYPHAE")) return true;

        if (name.contains("ICE")) return true;
        if (name.contains("KELP")) return true;
        if (name.contains("SEAGRASS")) return true;
        if (name.contains("CORAL")) return true;

        return switch (type) {
            case WATER,
                 LAVA,
                 CACTUS,
                 MAGMA_BLOCK,
                 CAMPFIRE,
                 SOUL_CAMPFIRE,
                 FIRE,
                 SOUL_FIRE,
                 POWDER_SNOW -> true;
            default -> false;
        };
    }

    private boolean debugPluginSpawns() {
        return plugin.getConfig().getBoolean("mob-spawning.debug.plugin-spawns", false);
    }

    private boolean debugFailedAttempts() {
        return plugin.getConfig().getBoolean("mob-spawning.debug.failed-attempts", false);
    }

    private void debug(String message) {
        if (!debugFailedAttempts()) return;
        plugin.getLogger().info("[MobSpawnDebug] " + message);
    }

    private boolean isLevelBorderMob(Entity entity) {
        return entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}
