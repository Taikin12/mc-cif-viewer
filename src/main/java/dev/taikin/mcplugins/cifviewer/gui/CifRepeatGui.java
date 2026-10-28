package dev.taikin.mcplugins.cifviewer.gui;

import dev.taikin.mcplugins.cifviewer.CifViewerPlugin;
import dev.taikin.mcplugins.cifviewer.render.ViewSession;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CifRepeatGui {

    private static final int SIZE = 36;
    private static final int INFO = 4;
    private static final int X_UP = 9;
    private static final int Y_UP = 11;
    private static final int Z_UP = 13;
    private static final int X_DOWN = 18;
    private static final int Y_DOWN = 20;
    private static final int Z_DOWN = 22;
    private static final int RESET = 27;
    private static final int BACK = 35;

    private final CifViewerPlugin plugin;

    public CifRepeatGui(CifViewerPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ViewSession session = plugin.renderer().session(player);
        if (session == null) {
            player.sendMessage(Component.text("構造が表示されていません。先に /cif view <file> を実行してください。",
                    NamedTextColor.RED));
            return;
        }

        CifRepeatGuiHolder holder = new CifRepeatGuiHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                Component.text("CIF: 繰り返し (X/Y/Z)", NamedTextColor.DARK_AQUA));
        holder.inventory(inventory);
        populate(inventory, session);
        player.openInventory(inventory);
    }

    public void handleClick(Player player, Inventory inventory, int slot) {
        ViewSession session = plugin.renderer().session(player);
        if (session == null) {
            player.closeInventory();
            return;
        }

        try {
            switch (slot) {
                case X_UP -> plugin.renderer().adjustRepeat(player, ViewSession.Axis.X, 1);
                case X_DOWN -> plugin.renderer().adjustRepeat(player, ViewSession.Axis.X, -1);
                case Y_UP -> plugin.renderer().adjustRepeat(player, ViewSession.Axis.Y, 1);
                case Y_DOWN -> plugin.renderer().adjustRepeat(player, ViewSession.Axis.Y, -1);
                case Z_UP -> plugin.renderer().adjustRepeat(player, ViewSession.Axis.Z, 1);
                case Z_DOWN -> plugin.renderer().adjustRepeat(player, ViewSession.Axis.Z, -1);
                case RESET -> plugin.renderer().resetRepeat(player);
                case BACK -> {
                    plugin.controlGui().open(player);
                    return;
                }
                default -> {
                    return;
                }
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            ViewSession updated = plugin.renderer().session(player);
            if (updated != null) {
                populate(inventory, updated);
            }
        } catch (RuntimeException e) {
            player.sendMessage(Component.text(e.getMessage() == null ? "表示を更新できませんでした。"
                    : e.getMessage(), NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
        }
    }

    private void populate(Inventory inventory, ViewSession session) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler());
        }

        inventory.setItem(INFO, info(session));
        inventory.setItem(X_UP, button(Material.LIME_STAINED_GLASS_PANE, "X 繰り返し +",
                List.of(value("現在: X" + session.repeatX()))));
        inventory.setItem(X_DOWN, button(Material.RED_STAINED_GLASS_PANE, "X 繰り返し -",
                List.of(value("現在: X" + session.repeatX()))));
        inventory.setItem(Y_UP, button(Material.LIME_STAINED_GLASS_PANE, "Y 繰り返し +",
                List.of(value("現在: Y" + session.repeatY()))));
        inventory.setItem(Y_DOWN, button(Material.RED_STAINED_GLASS_PANE, "Y 繰り返し -",
                List.of(value("現在: Y" + session.repeatY()))));
        inventory.setItem(Z_UP, button(Material.LIME_STAINED_GLASS_PANE, "Z 繰り返し +",
                List.of(value("現在: Z" + session.repeatZ()))));
        inventory.setItem(Z_DOWN, button(Material.RED_STAINED_GLASS_PANE, "Z 繰り返し -",
                List.of(value("現在: Z" + session.repeatZ()))));
        inventory.setItem(RESET, button(Material.COMPASS, "X/Y/Z を1に戻す",
                List.of(value("繰り返しをリセット"))));
        inventory.setItem(BACK, button(Material.OAK_DOOR, "戻る", List.of(value("メインメニューへ"))));
    }

    private ItemStack info(ViewSession session) {
        long total = (long) session.crystal().atoms().size()
                * session.repeatX() * session.repeatY() * session.repeatZ();
        return button(Material.BOOK, "繰り返し設定", List.of(
                value("モデル: #" + session.id()),
                value("X: " + session.repeatX()),
                value("Y: " + session.repeatY()),
                value("Z: " + session.repeatZ()),
                value("合計原子数: " + total),
                value("上限: " + plugin.config().maxAtoms())));
    }

    private ItemStack button(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private Component value(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }
}
