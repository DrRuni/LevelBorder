package runi.myddns.levelborder.Manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import runi.myddns.levelborder.Utils.ColorUtil;

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
                if (data.isScoreboardVisible()) {
                    titleTick += 0.25f;

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        updateBoard(p);
                    }
                }
            }
        };

        updaterTask.runTaskTimer(plugin, 0L, 4L);
    }

    public void stopUpdater() {
        if (updaterTask != null) {
            updaterTask.cancel();
            updaterTask = null;
        }

        updaterRunning = false;
    }

    private void updateBoard(Player p) {

        Scoreboard board = boards.get(p);
        Objective obj = objectives.get(p);

        if (board == null || obj == null) {
            addPlayer(p);
            return;
        }

        obj.displayName(
                ColorUtil.borderColorScrolling("- LevelBorder -", titleTick)
        );

        if (p.getScoreboard() != board) {
            p.setScoreboard(board);
        }

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        AtomicInteger score = new AtomicInteger(0);

        addLine(p, score.getAndIncrement(), Component.text("🧍 Spieler:", NamedTextColor.AQUA));
        addLine(p, score.getAndIncrement(), Component.text(" "));

        Map<String, Integer> allPlayers = new HashMap<>(data.getAllPlayerLevels());

        allPlayers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry ->
                        addLine(
                                p,
                                score.getAndIncrement(),
                                Component.text("   - ", NamedTextColor.GRAY)
                                        .append(Component.text(entry.getKey(), NamedTextColor.GREEN))
                                        .append(Component.text(": ", NamedTextColor.GRAY))
                                        .append(Component.text(entry.getValue(), NamedTextColor.GOLD))
                        )
                );

        addLine(p, score.getAndIncrement(), Component.text(" "));
        addLine(p, score.getAndIncrement(), Component.text("🌐 Bordergröße:", NamedTextColor.AQUA));
        addLine(
                p,
                score.getAndIncrement(),
                Component.text("   - ", NamedTextColor.GRAY)
                        .append(Component.text((int) data.getSize() + " m", NamedTextColor.GOLD))
        );
    }

    private void addLine(Player p, int lineNumber, Component text) {

        Scoreboard board = boards.get(p);
        Objective obj = objectives.get(p);

        if (board == null || obj == null) return;

        String entry = "§" + Integer.toHexString(lineNumber);

        Team team = board.getTeam("line" + lineNumber);
        if (team == null) {
            team = board.registerNewTeam("line" + lineNumber);
            team.addEntry(entry);
        }

        team.prefix(text);
        team.suffix(Component.empty());

        obj.getScore(entry).setScore(0);
    }

    public void hide() {
        data.setScoreboardVisible(false);

        Scoreboard emptyBoard = Bukkit.getScoreboardManager().getNewScoreboard();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(emptyBoard);
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
                "lbinfo",
                Criteria.DUMMY,
                Component.text("🌍 LevelBorder", NamedTextColor.GOLD)
        );

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        boards.put(p, board);
        objectives.put(p, obj);

        p.setScoreboard(board);
        updateBoard(p);
    }
}