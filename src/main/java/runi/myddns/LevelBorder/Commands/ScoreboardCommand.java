package runi.myddns.LevelBorder.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import runi.myddns.LevelBorder.Manager.ScoreboardManager;

import java.util.*;

public class ScoreboardCommand implements CommandExecutor, TabCompleter {

    private final ScoreboardManager scoreboardManager;

    public ScoreboardCommand(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl verwenden.");
            return true;
        }

        int playerLevelRank = getPlayerStufe(player);
        boolean isAdmin = playerLevelRank >= 4;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "hide" -> {
                scoreboardManager.hide();
                player.sendMessage(ChatColor.GRAY + "📉 Scoreboard ausgeblendet.");
            }

            case "reload" -> {
                scoreboardManager.show();
                scoreboardManager.startUpdater();
                player.sendMessage(ChatColor.GREEN + "📈 Scoreboard wieder eingeblendet.");
            }

            case "reset" -> {
                if (!isAdmin) {
                    player.sendMessage(ChatColor.RED + "❌ Nur der Admin darf das Scoreboard zurücksetzen!");
                    return true;
                }
                scoreboardManager.reset();
                player.sendMessage(ChatColor.GOLD + "♻ Scoreboard-Daten wurden zurückgesetzt.");
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "========== LevelBorder Scoreboard ==========");
        player.sendMessage(ChatColor.GRAY + "/lbscore hide" + ChatColor.DARK_GRAY + " → Scoreboard ausblenden");
        player.sendMessage(ChatColor.GRAY + "/lbscore reload" + ChatColor.DARK_GRAY + " → Scoreboard wieder anzeigen");
        player.sendMessage(ChatColor.GRAY + "/lbscore reset" + ChatColor.DARK_GRAY + " → Admin");
        player.sendMessage(ChatColor.GOLD + "===========================================");
    }

    private int getPlayerStufe(Player p) {
        return p.isOp() ? 4 : 2;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {

        if (args.length == 1) {
            return Arrays.asList("hide", "reload", "reset");
        }
        return Collections.emptyList();
    }
}