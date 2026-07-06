package runi.myddns.levelborder.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import runi.myddns.levelborder.GUI.OptionsGUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptionsCommand implements CommandExecutor, TabCompleter {

    private final OptionsGUI optionsGUI;

    public OptionsCommand(OptionsGUI optionsGUI) {
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

        if (args.length == 0) {
            optionsGUI.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "open", "öffnen", "oeffnen" -> {
                optionsGUI.open(player);
                player.sendMessage(Component.text("⚙ Optionen geöffnet.", NamedTextColor.GREEN));
            }

            case "help", "hilfe" -> sendHelp(player);

            default -> player.sendMessage(Component.text(
                    "Unbekannter Unterbefehl. Nutze /optionen oder /optionen help.",
                    NamedTextColor.RED
            ));
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("============= Optionen Befehle ============", NamedTextColor.GOLD));

        player.sendMessage(
                Component.text(" /optionen", NamedTextColor.GRAY)
                        .append(Component.text(" → Optionen öffnen", NamedTextColor.DARK_GRAY))
        );

        player.sendMessage(
                Component.text(" /optionen open", NamedTextColor.GRAY)
                        .append(Component.text(" → Optionen öffnen", NamedTextColor.DARK_GRAY))
        );

        player.sendMessage(
                Component.text(" /levelborder optionen", NamedTextColor.GRAY)
                        .append(Component.text(" → Optionen über LevelBorder öffnen", NamedTextColor.DARK_GRAY))
        );

        player.sendMessage(Component.text("==========================================", NamedTextColor.GOLD));
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
            list.addAll(Arrays.asList("open", "help"));
        }

        return list;
    }
}