package runi.myddns.LevelBorder.Commands;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import runi.myddns.LevelBorder.LevelBorderMain;
import runi.myddns.LevelBorder.Manager.BorderDataManager;
import runi.myddns.LevelBorder.Manager.LevelBorderManager;
import runi.myddns.LevelBorder.Manager.ScoreboardManager;
import runi.myddns.LevelBorder.Manager.TimerManager;

import java.util.*;

public class LevelBorderCommand implements CommandExecutor, TabCompleter {

    private final LevelBorderManager borderManager;
    private final ScoreboardManager scoreboardManager;
    private final TimerManager timerManager;

    public LevelBorderCommand(LevelBorderManager borderManager,
                              ScoreboardManager scoreboardManager,
                              TimerManager timerManager) {
        this.borderManager = borderManager;
        this.scoreboardManager = scoreboardManager;
        this.timerManager = timerManager;
    }

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command cmd,
            final @NotNull String label,
            final @NotNull String @NonNull [] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }

        int playerLevelRank = getPlayerStufe(player);
        boolean isAdmin = playerLevelRank >= 4;

        if (args.length > 0 && args[0].equalsIgnoreCase("score")) {
            ScoreboardCommand scoreCmd = new ScoreboardCommand(scoreboardManager);
            String[] shifted = Arrays.copyOfRange(args, 1, args.length);
            return scoreCmd.onCommand(sender, cmd, label, shifted);
        }

        BorderDataManager data = borderManager.getData();

        if (args.length == 0) {
            sendStatus(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "info" -> sendDetailedInfo(player, data);

            case "center" -> {
                if (!isAdmin) {
                    player.sendMessage(ChatColor.RED + "❌ Nur der Admin darf die Border-Mitte setzen!");
                    return true;
                }
                Location loc = player.getLocation();
                double x = Math.floor(loc.getX()) + 0.5;
                double z = Math.floor(loc.getZ()) + 0.5;
                Location centered = new Location(loc.getWorld(), x, loc.getY(), z);

                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        0.6f,
                        1.2f
                );

                borderManager.setCenter(centered);
                player.sendMessage(ChatColor.GREEN + "📍 Border-Mitte exakt auf Blockgrenze gesetzt!");
            }

            case "start" -> {
                if (!isAdmin) {
                    player.sendMessage(ChatColor.RED + "❌ Nur der Admin darf den LevelBorder starten!");
                    return true;
                }

                if (borderManager.getData().getCenter() == null) {
                    borderManager.setCenter(player.getLocation());
                }

                borderManager.setActive(true);
                scoreboardManager.show();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location loc = p.getLocation();

                    p.playSound(
                            loc,
                            Sound.BLOCK_BEACON_ACTIVATE,
                            0.9f,
                            0.8f
                    );

                    Bukkit.getScheduler().runTaskLater(
                            LevelBorderMain.getInstance(),
                            () -> p.playSound(
                                    loc,
                                    Sound.ENTITY_WARDEN_HEARTBEAT,
                                    1.2f,
                                    0.7f
                            ),
                            10L
                    );

                    Bukkit.getScheduler().runTaskLater(
                            LevelBorderMain.getInstance(),
                            () -> p.playSound(
                                    loc,
                                    Sound.BLOCK_SCULK_SHRIEKER_SHRIEK,
                                    0.4f,
                                    0.6f
                            ),
                            18L
                    );
                }

                player.sendMessage(ChatColor.GREEN + "✅ Border aktiviert!");

            }

            case "stop" -> {
                if (!isAdmin) {
                    player.sendMessage(ChatColor.RED + "❌ Nur der Admin darf den LevelBorder stoppen!");
                    return true;
                }

                borderManager.setActive(false);

                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        0.6f,
                        1.2f
                );

                player.sendMessage(ChatColor.RED + "🛑 Border deaktiviert!");
            }

            case "set" -> {
                if (!isAdmin) {
                    player.sendMessage(ChatColor.RED + "❌ Nur der Admin darf die Bordergröße ändern!");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "⚠ Nutzung: /levelborder set <größe>");
                    return true;
                }

                try {
                    double size = Double.parseDouble(args[1]);
                    borderManager.setSize(size);
                    player.sendMessage(ChatColor.YELLOW + "📏 Bordergröße gesetzt auf " + size + " Blöcke.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Bitte eine gültige Zahl eingeben!");
                }

                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        0.6f,
                        1.2f
                );

            }

            case "reset" -> {
                if (!isAdmin) {
                    player.sendMessage(ChatColor.RED + "❌ Nur der Admin darf den LevelBorder zurücksetzen!");
                    return true;
                }

                scoreboardManager.reset();
                borderManager.resetBorder(player);
                timerManager.reset();
                LevelBorderMain.getInstance()
                        .getPortalManager()
                        .clearPortalWorldData();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(
                            p.getLocation(),
                            Sound.BLOCK_BEACON_DEACTIVATE,
                            0.6f,
                            0.7f
                    );
                }

                player.sendMessage(ChatColor.GREEN + "♻ LevelBorder + Portale wurden zurückgesetzt.");

            }

            default -> player.sendMessage(ChatColor.RED + "Unbekannter Unterbefehl. Nutze /levelborder für Hilfe.");
        }

        return true;
    }

    private int getPlayerStufe(Player p) {
        return p.isOp() ? 4 : 2;
    }

    private void sendStatus(Player player) {
        player.sendMessage("\n" + ChatColor.GOLD + "============= LevelBorder Befehle ============");
        player.sendMessage(ChatColor.GRAY + " info" + ChatColor.DARK_GRAY + " → Status anzeigen");
        player.sendMessage(ChatColor.GRAY + " score" + ChatColor.DARK_GRAY + " → Scoreboard anzeigen");
        player.sendMessage(ChatColor.GRAY + " start / stop / set / reset / center" +
                ChatColor.DARK_GRAY + " → Admin");
        player.sendMessage(ChatColor.GOLD + "============================================");
    }

    private void sendDetailedInfo(Player player, BorderDataManager data) {
        int total = Bukkit.getOnlinePlayers().stream().mapToInt(Player::getLevel).sum();

        player.sendMessage(ChatColor.AQUA + "============= 📊 LevelBorder Info =============");
        player.sendMessage(ChatColor.GRAY + "Aktuelle Gesamt-Level: " + ChatColor.YELLOW + total);
        player.sendMessage(ChatColor.GRAY + "Bisheriger Rekord: " + ChatColor.GOLD + data.getMaxTotalLevel());
        player.sendMessage(ChatColor.GRAY + "Border-Größe: " + ChatColor.GREEN + data.getSize());
        player.sendMessage(ChatColor.GRAY + "Aktiv: " +
                (data.isActive() ? ChatColor.GREEN + "Ja" : ChatColor.RED + "Nein"));
        player.sendMessage(ChatColor.AQUA + "============================================");
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command cmd,
            @NotNull String alias,
            @NotNull String @NonNull [] args
    ) {

        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.addAll(Arrays.asList("info", "score", "center", "start", "stop", "set", "reset"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            list.addAll(Arrays.asList("10", "25", "50", "100"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("score")) {
            list.addAll(Arrays.asList("hide", "reload", "reset"));
        }

        return list;
    }
}