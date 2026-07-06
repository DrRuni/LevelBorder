package runi.myddns.levelborder.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import runi.myddns.levelborder.Manager.ScoreboardManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ScoreboardCommand implements CommandExecutor, TabCompleter {

    private final ScoreboardManager scoreboardManager;

    public ScoreboardCommand(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können diesen Befehl verwenden.", NamedTextColor.RED));
            return true;
        }

        int playerLevelRank = getPlayerStufe(player);
        boolean isAdmin = playerLevelRank >= 4;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "hide" -> {
                scoreboardManager.hide();
                player.sendMessage(Component.text("📉 Scoreboard ausgeblendet.", NamedTextColor.GRAY));
            }

            case "reload" -> {
                scoreboardManager.show();
                scoreboardManager.startUpdater();
                player.sendMessage(Component.text("📈 Scoreboard wieder eingeblendet.", NamedTextColor.GREEN));
            }

            case "reset" -> {
                if (!isAdmin) {
                    player.sendMessage(Component.text("❌ Nur der Admin darf das Scoreboard zurücksetzen!", NamedTextColor.RED));
                    return true;
                }

                scoreboardManager.reset();
                player.sendMessage(Component.text("♻ Scoreboard-Daten wurden zurückgesetzt.", NamedTextColor.GOLD));
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("========== LevelBorder Scoreboard ==========", NamedTextColor.GOLD));

        player.sendMessage(
                Component.text("/lbscore hide", NamedTextColor.GRAY)
                        .append(Component.text(" → Scoreboard ausblenden", NamedTextColor.DARK_GRAY))
        );

        player.sendMessage(
                Component.text("/lbscore reload", NamedTextColor.GRAY)
                        .append(Component.text(" → Scoreboard wieder anzeigen", NamedTextColor.DARK_GRAY))
        );

        player.sendMessage(
                Component.text("/lbscore reset", NamedTextColor.GRAY)
                        .append(Component.text(" → Admin", NamedTextColor.DARK_GRAY))
        );

        player.sendMessage(Component.text("===========================================", NamedTextColor.GOLD));
    }

    private int getPlayerStufe(Player p) {
        return p.isOp() ? 4 : 2;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String @NotNull [] args) {

        if (args.length == 1) {
            return Arrays.asList("hide", "reload", "reset");
        }

        return Collections.emptyList();
    }
}