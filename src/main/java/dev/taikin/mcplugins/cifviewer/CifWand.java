package dev.taikin.mcplugins.cifviewer;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class CifWand {

    private final NamespacedKey key;

    public CifWand(CifViewerPlugin plugin) {
        this.key = new NamespacedKey(plugin, "wand");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("CIF操作棒", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                lore("右クリック: メニュー"),
                lore("モデルをクリック: メニュー"),
                lore("左クリック(空): 選択軸で回転"),
                lore("左クリック(ブロック面): 回転軸を変更"),
                lore("スニーク+左クリック(ブロック): 原点(0,0,0)設定"),
                lore("スニーク+スクロール: 全体サイズ")));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) {
            return false;
        }
        Byte value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public boolean has(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isWand(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean giveIfMissing(Player player) {
        if (has(player)) {
            return false;
        }
        give(player);
        return true;
    }

    public void give(Player player) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(create());
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    private Component lore(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }
}
