package runi.myddns.levelborder.GUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import runi.myddns.levelborder.LevelBorderMain;
import runi.myddns.levelborder.Manager.LevelBorderManager;
import runi.myddns.levelborder.Manager.ScoreboardManager;
import runi.myddns.levelborder.Manager.TimerManager;

import java.util.Arrays;

public class OptionsGUI implements Listener {

    private static final Component TITLE = Component.text("LevelBorder Optionen", NamedTextColor.GOLD);

    private final LevelBorderManager borderManager;
    private final ScoreboardManager scoreboardManager;
    private final TimerManager timerManager;

    public OptionsGUI(LevelBorderManager borderManager,
                      ScoreboardManager scoreboardManager,
                      TimerManager timerManager) {
        this.borderManager = borderManager;
        this.scoreboardManager = scoreboardManager;
        this.timerManager = timerManager;
    }

    private boolean isKeepInventoryEnabled() {
        return borderManager.getData().isKeepInventoryEnabled();
    }

    public void open(Player player) {

        if (!player.isOp()) {
            error(player, "❌ Nur OPs dürfen die Optionen öffnen!");
            return;
        }

        boolean keepInventory = isKeepInventoryEnabled();

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        inv.setItem(10, createItem(
                Material.BEACON,
                Component.text("LevelBorder starten", NamedTextColor.GREEN),
                Component.text("Aktiviert die LevelBorder.", NamedTextColor.GRAY),
                Component.text("Klick zum Starten", NamedTextColor.DARK_GRAY)
        ));

        inv.setItem(11, createItem(
                Material.REDSTONE_BLOCK,
                Component.text("LevelBorder stoppen", NamedTextColor.RED),
                Component.text("Deaktiviert die LevelBorder.", NamedTextColor.GRAY),
                Component.text("Klick zum Stoppen", NamedTextColor.DARK_GRAY)
        ));

        inv.setItem(12, createItem(
                Material.RECOVERY_COMPASS,
                Component.text("Border-Mitte setzen", NamedTextColor.AQUA),
                Component.text("Setzt die Border-Mitte", NamedTextColor.GRAY),
                Component.text("auf deine aktuelle Position.", NamedTextColor.GRAY),
                Component.text("Klick zum Setzen", NamedTextColor.DARK_GRAY)
        ));

        inv.setItem(13, createItem(
                Material.BARRIER,
                Component.text("LevelBorder resetten", NamedTextColor.DARK_RED),
                Component.text("Setzt LevelBorder, Timer", NamedTextColor.GRAY),
                Component.text("und Scoreboard zurück.", NamedTextColor.GRAY),
                Component.text("Klick zum Reset", NamedTextColor.DARK_GRAY)
        ));

        inv.setItem(14, createItem(
                keepInventory ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                keepInventory
                        ? Component.text("KeepInventory: AN", NamedTextColor.GREEN)
                        : Component.text("KeepInventory: AUS", NamedTextColor.RED),
                Component.text("Klick zum Umschalten", NamedTextColor.GRAY),
                keepInventory
                        ? Component.text("Aktuell aktiviert", NamedTextColor.DARK_GRAY)
                        : Component.text("Aktuell deaktiviert", NamedTextColor.DARK_GRAY)
        ));

        inv.setItem(15, createItem(
                Material.IRON_BARS,
                Component.text("Bordergröße ändern", NamedTextColor.GOLD),
                Component.text("Aktuelle Größe: ", NamedTextColor.GRAY)
                        .append(Component.text((int) borderManager.getData().getSize(), NamedTextColor.YELLOW)),
                Component.text("Linksklick: -1", NamedTextColor.DARK_GRAY),
                Component.text("Rechtsklick: +1", NamedTextColor.DARK_GRAY)
        ));

        inv.setItem(22, createItem(
                Material.ARROW,
                Component.text("Zurück", NamedTextColor.RED),
                Component.text("Geht zurück / schließt dieses Menü.", NamedTextColor.GRAY)
        ));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!event.getView().title().equals(TITLE)) {
            return;
        }

        event.setCancelled(true);

        HumanEntity clicker = event.getWhoClicked();

        if (!(clicker instanceof Player player)) {
            return;
        }

        if (!player.isOp()) {
            player.closeInventory();
            error(player, "❌ Nur OPs dürfen dieses Menü benutzen!");
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        switch (event.getRawSlot()) {

            case 10 -> {
                borderManager.setCenter(player.getLocation());
                borderManager.setActive(true);

                for (Player online : Bukkit.getOnlinePlayers()) {
                    borderManager.getData().savePlayerLevel(online);
                }

                scoreboardManager.show();

                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_BEACON_ACTIVATE,
                        0.9f,
                        0.8f
                );

                success(player, "✅ LevelBorder wurde gestartet.");
                open(player);
            }

            case 11 -> {
                borderManager.setActive(false);
                scoreboardManager.stopUpdater();

                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_BEACON_DEACTIVATE,
                        0.8f,
                        0.8f
                );

                error(player, "🛑 LevelBorder wurde gestoppt.");
                open(player);
            }

            case 12 -> {
                Location loc = player.getLocation();
                double x = Math.floor(loc.getX()) + 0.5;
                double z = Math.floor(loc.getZ()) + 0.5;
                Location centered = new Location(loc.getWorld(), x, loc.getY(), z);

                borderManager.setCenter(centered);

                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        0.6f,
                        1.2f
                );

                success(player, "📍 Border-Mitte wurde auf deine Position gesetzt.");
                open(player);
            }

            case 13 -> {
                scoreboardManager.reset();
                borderManager.resetBorder(player);
                timerManager.reset();

                LevelBorderMain.getInstance()
                        .getPortalManager()
                        .clearPortalWorldData();

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.playSound(
                            online.getLocation(),
                            Sound.BLOCK_BEACON_DEACTIVATE,
                            0.6f,
                            0.7f
                    );
                }

                success(player, "♻ LevelBorder wurde zurückgesetzt.");
                open(player);
            }

            case 14 -> {
                boolean current = borderManager.getData().isKeepInventoryEnabled();
                boolean newValue = !current;

                borderManager.getData().setKeepInventoryEnabled(newValue);
                applyKeepInventoryToAllWorlds(newValue);

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        0.6f,
                        1.2f
                );

                if (newValue) {
                    success(player, "✅ KeepInventory wurde aktiviert.");
                } else {
                    error(player, "❌ KeepInventory wurde deaktiviert.");
                }

                open(player);
            }

            case 15 -> {
                double currentSize = borderManager.getData().getSize();
                double newSize = currentSize;

                if (event.isRightClick()) {
                    newSize = currentSize + 1;
                } else if (event.isLeftClick()) {
                    newSize = Math.max(1.0, currentSize - 1);
                }

                borderManager.setDiameter(newSize);

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        0.6f,
                        event.isRightClick() ? 1.4f : 0.8f
                );

                player.sendMessage(
                        Component.text("📏 Bordergröße gesetzt auf ", NamedTextColor.YELLOW)
                                .append(Component.text((int) newSize, NamedTextColor.GOLD))
                                .append(Component.text(" Blöcke.", NamedTextColor.YELLOW))
                );

                open(player);
            }

            case 22 -> {
                player.closeInventory();

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        0.5f,
                        0.8f
                );
            }
        }
    }

    private ItemStack createItem(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(name);
            meta.lore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }

        return item;
    }

    private void success(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private void error(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private void applyKeepInventoryToAllWorlds(boolean enabled) {
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRules.KEEP_INVENTORY, enabled);
        }
    }
}