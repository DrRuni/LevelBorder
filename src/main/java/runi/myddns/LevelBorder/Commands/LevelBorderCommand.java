package runi.myddns.levelborder.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import runi.myddns.levelborder.LevelBorderMain;
import runi.myddns.levelborder.Manager.BorderDataManager;
import runi.myddns.levelborder.Manager.LevelBorderManager;
import runi.myddns.levelborder.Manager.ScoreboardManager;
import runi.myddns.levelborder.Manager.TimerManager;
import runi.myddns.levelborder.GUI.OptionsGUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LevelBorderCommand implements CommandExecutor, TabCompleter {

    private final LevelBorderManager borderManager;
    private final ScoreboardManager scoreboardManager;
    private final TimerManager timerManager;
    private final OptionsGUI optionsGUI;

    public LevelBorderCommand(LevelBorderManager borderManager,
                              ScoreboardManager scoreboardManager,
                              TimerManager timerManager,
                              OptionsGUI optionsGUI) {
        this.borderManager = borderManager;
        this.scoreboardManager = scoreboardManager;
        this.timerManager = timerManager;
        this.optionsGUI = optionsGUI;
    }

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command cmd,
            final @NotNull String label,
            final @NotNull String @NonNull [] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können diesen Befehl nutzen.", NamedTextColor.RED));
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

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "info" -> sendDetailedInfo(player, data);

            case "optionen", "options" -> {
                optionsGUI.open(player);
                success(player, "⚙ Optionen geöffnet.");
            }

            case "center" -> {
                if (!isAdmin) {
                    error(player, "❌ Nur der Admin darf die Border-Mitte setzen!");
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
                success(player, "📍 Border-Mitte exakt auf Blockgrenze gesetzt!");
            }

            case "start" -> {
                if (!isAdmin) {
                    error(player, "❌ Nur der Admin darf den LevelBorder starten!");
                    return true;
                }

                Location startLocation = player.getLocation().clone();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.equals(player)) continue;

                    Bukkit.getScheduler().runTaskLater(
                            LevelBorderMain.getInstance(),
                            () -> {
                                if (!p.isOnline()) return;
                                p.teleport(startLocation);
                            },
                            10L
                    );
                }

                Bukkit.getScheduler().runTaskLater(
                        LevelBorderMain.getInstance(),
                        () -> {
                            borderManager.setCenter(startLocation.clone());
                            borderManager.setActive(true);

                            for (Player online : Bukkit.getOnlinePlayers()) {
                                borderManager.getData().savePlayerLevel(online);
                            }

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
                                                p.getLocation(),
                                                Sound.ENTITY_WARDEN_HEARTBEAT,
                                                1.2f,
                                                0.7f
                                        ),
                                        10L
                                );

                                Bukkit.getScheduler().runTaskLater(
                                        LevelBorderMain.getInstance(),
                                        () -> p.playSound(
                                                p.getLocation(),
                                                Sound.BLOCK_SCULK_SHRIEKER_SHRIEK,
                                                0.4f,
                                                0.6f
                                        ),
                                        18L
                                );
                            }

                            success(player, "✅ Alle Spieler gesammelt. Border aktiviert!");
                        },
                        30L
                );

                success(player, "⏳ Spieler werden gesammelt...");
            }

            case "stop" -> {
                if (!isAdmin) {
                    error(player, "❌ Nur der Admin darf den LevelBorder stoppen!");
                    return true;
                }

                borderManager.setActive(false);
                scoreboardManager.stopUpdater();

                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        0.6f,
                        1.2f
                );

                error(player, "🛑 Border deaktiviert!");
            }

            case "set" -> {
                if (!isAdmin) {
                    error(player, "❌ Nur der Admin darf die Bordergröße ändern!");
                    return true;
                }

                if (args.length < 2) {
                    error(player, "⚠ Nutzung: /levelborder set <größe>");
                    return true;
                }

                try {
                    double size = Double.parseDouble(args[1]);
                    borderManager.setSize(size);

                    player.sendMessage(
                            Component.text("📏 Bordergröße gesetzt auf ", NamedTextColor.YELLOW)
                                    .append(Component.text(size, NamedTextColor.GOLD))
                                    .append(Component.text(" Blöcke.", NamedTextColor.YELLOW))
                    );
                } catch (NumberFormatException e) {
                    error(player, "Bitte eine gültige Zahl eingeben!");
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
                    error(player, "❌ Nur der Admin darf den LevelBorder zurücksetzen!");
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

                success(player, "♻ LevelBorder + Portale wurden zurückgesetzt.");
            }

            default -> error(player, "Unbekannter Unterbefehl. Nutze /levelborder für Hilfe.");
        }

        return true;
    }

    private int getPlayerStufe(Player p) {
        return p.isOp() ? 4 : 2;
    }

    private void sendStatus(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("============= LevelBorder Befehle ============", NamedTextColor.GOLD));

        player.sendMessage(
                Component.text(" info", NamedTextColor.GRAY)
                        .append(Component.text(" → Status anzeigen", NamedTextColor.DARK_GRAY))
        );

        player.sendMessage(
                Component.text(" score", NamedTextColor.GRAY)
                        .append(Component.text(" → Scoreboard anzeigen", NamedTextColor.DARK_GRAY))
        );

        player.sendMessage(
                Component.text(" optionen / start / stop / set / reset / center", NamedTextColor.GRAY)
                        .append(Component.text(" → Admin", NamedTextColor.DARK_GRAY))
        );

        player.sendMessage(Component.text("============================================", NamedTextColor.GOLD));
    }

    private void sendDetailedInfo(Player player, BorderDataManager data) {
        int total = Bukkit.getOnlinePlayers().stream().mapToInt(Player::getLevel).sum();

        player.sendMessage(Component.text("============= 📊 LevelBorder Info =============", NamedTextColor.AQUA));

        player.sendMessage(
                Component.text("Aktuelle Gesamt-Level: ", NamedTextColor.GRAY)
                        .append(Component.text(total, NamedTextColor.YELLOW))
        );

        player.sendMessage(
                Component.text("Bisheriger Rekord: ", NamedTextColor.GRAY)
                        .append(Component.text(data.getMaxTotalLevel(), NamedTextColor.GOLD))
        );

        player.sendMessage(
                Component.text("Border-Größe: ", NamedTextColor.GRAY)
                        .append(Component.text(data.getSize(), NamedTextColor.GREEN))
        );

        player.sendMessage(
                Component.text("Aktiv: ", NamedTextColor.GRAY)
                        .append(data.isActive()
                                ? Component.text("Ja", NamedTextColor.GREEN)
                                : Component.text("Nein", NamedTextColor.RED))
        );

        player.sendMessage(Component.text("============================================", NamedTextColor.AQUA));
    }

    private void success(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private void error(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.RED));
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
            list.addAll(Arrays.asList("info", "score", "optionen", "center", "start", "stop", "set", "reset"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            list.addAll(Arrays.asList("10", "25", "50", "100"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("score")) {
            list.addAll(Arrays.asList("hide", "reload", "reset"));
        }
        return list;
    }
}