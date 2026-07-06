package runi.myddns.LevelBorder.Manager;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import runi.myddns.LevelBorder.Utils.ColorUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ScoreboardManager {

    private final Map<Player, Scoreboard> boards = new HashMap<>();
    private final Map<Player, Objective> objectives = new HashMap<>();

    private final JavaPlugin plugin;
    private final BorderDataManager data;
    private BukkitRunnable updaterTask;
    private boolean updaterRunning = false;
    private float titleTick = 0f;

    public ScoreboardManager(JavaPlugin plugin, BorderDataManager data) {
        this.plugin = plugin;
        this.data = data;
    }

    public void startUpdater() {
        if (updaterRunning) return;
        updaterRunning = true;

        updaterTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!data.isScoreboardVisible()) return;

                titleTick += 0.25f;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    updateBoard(p);
                }
            }
        };

        updaterTask.runTaskTimer(plugin, 0L, 4L);
    }

    private void updateBoard(Player p) {

        Scoreboard board = boards.get(p);
        Objective obj = objectives.get(p);

        if (board == null || obj == null) {
            addPlayer(p);
            return;
        }

        obj.setDisplayName(
                ColorUtil.borderColorScrolling("- LevelBorder -", titleTick)
        );

        if (p.getScoreboard() != board) {
            p.setScoreboard(board);
        }

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        AtomicInteger score = new AtomicInteger(0);

        addLine(p, score.getAndIncrement(), ChatColor.AQUA + "🧍 Spieler:");
        addLine(p, score.getAndIncrement(), " ");

        Map<String, Integer> allPlayers = new HashMap<>(data.getAllPlayerLevels());

        allPlayers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry ->
                        addLine(p, score.getAndIncrement(),
                                ChatColor.GRAY + "   - " + ChatColor.GREEN + entry.getKey()
                                        + ": " + ChatColor.GOLD + entry.getValue())
                );

        addLine(p, score.getAndIncrement(), " ");
        addLine(p, score.getAndIncrement(), ChatColor.AQUA + "🌐 Bordergröße:");
        addLine(p, score.getAndIncrement(),
                ChatColor.GRAY + "   - " + ChatColor.GOLD + (int) data.getSize() + " m");
    }

    private void addLine(Player p, int lineNumber, String text) {

        Scoreboard board = boards.get(p);
        Objective obj = objectives.get(p);

        String entry = ChatColor.values()[lineNumber].toString();

        Team team = board.getTeam("line" + lineNumber);
        if (team == null) {
            team = board.registerNewTeam("line" + lineNumber);
            team.addEntry(entry);
        }

        team.setPrefix(text);
        team.setSuffix("");

        obj.getScore(entry).setScore(0);
    }

    public void hide() {
        data.setScoreboardVisible(false);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public void show() {
        data.setScoreboardVisible(true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            addPlayer(p);
            updateBoard(p);
        }
    }

    public void reset() {
        hide();
        boards.clear();
        objectives.clear();
    }

    public void addPlayer(Player p) {
        if (p == null || boards.containsKey(p)) return;

        if (!data.isScoreboardVisible()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective(
                "lbinfo", "dummy", ChatColor.GOLD + "🌍 LevelBorder"
        );
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        boards.put(p, board);
        objectives.put(p, obj);

        p.setScoreboard(board);
        updateBoard(p);
    }
}