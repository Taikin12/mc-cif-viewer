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

    public enum WandType {
        MENU,
        VIEW,
        ATOM
    }

    private final NamespacedKey legacyKey;
    private final NamespacedKey typeKey;

    public CifWand(CifViewerPlugin plugin) {
        this.legacyKey = new NamespacedKey(plugin, "wand");
        this.typeKey = new NamespacedKey(plugin, "wand-type");
    }

    public ItemStack create() {
        return create(WandType.MENU);
    }

    public ItemStack create(WandType type) {
        Material material = switch (type) {
            case MENU -> Material.STICK;
            case VIEW -> Material.BLAZE_ROD;
            case ATOM -> Material.END_ROD;
        };
        String name = switch (type) {
            case MENU -> "CIFメニュー棒";
            case VIEW -> "CIFビュー棒";
            case ATOM -> "CIF原子棒";
        };
        NamedTextColor color = switch (type) {
            case MENU -> NamedTextColor.GOLD;
            case VIEW -> NamedTextColor.YELLOW;
            case ATOM -> NamedTextColor.AQUA;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(switch (type) {
            case MENU -> List.of(
                    lore("右クリック: メニュー"),
                    lore("モデルをクリック: 選択 / メニュー"));
            case VIEW -> List.of(
                    lore("右クリック: メニュー"),
                    lore("左クリック(空): 選択軸で回転"),
                    lore("左クリック(ブロック面): 回転軸を変更"),
                    lore("スニーク+左クリック(ブロック): 原点(0,0,0)設定"),
                    lore("スニーク+スクロール: 全体サイズ"));
            case ATOM -> List.of(
                    lore("右クリック: 測定原子を選択/追加"),
                    lore("左クリック: 測定選択を解除"),
                    lore("中クリック / スニーク+右クリック: 多面体中心を追加/解除"),
                    lore("スニーク+左クリック: 多面体中心をすべて解除"),
                    lore("2原子: 距離 / 3原子: 角度 / 4原子: 二面角"));
        });
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
        if (type == WandType.VIEW) {
            meta.getPersistentDataContainer().set(legacyKey, PersistentDataType.BYTE, (byte) 1);
        }
        item.setItemMeta(meta);
        return item;
    }

    public WandType type(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String configured = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (configured != null) {
            if (configured.equalsIgnoreCase("OPERATION")) {
                return WandType.VIEW;
            }
            try {
                return WandType.valueOf(configured.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        Byte legacy = item.getItemMeta().getPersistentDataContainer().get(legacyKey, PersistentDataType.BYTE);
        return legacy != null && legacy == 1 ? WandType.VIEW : null;
    }

    public boolean isWand(ItemStack item) {
        return type(item) != null;
    }

    public boolean has(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isWand(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean has(Player player, WandType type) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (type(item) == type) {
                return true;
            }
        }
        return false;
    }

    public boolean giveIfMissing(Player player) {
        return giveIfMissing(player, WandType.MENU);
    }

    public boolean giveIfMissing(Player player, WandType type) {
        if (has(player, type)) {
            return false;
        }
        give(player, type);
        return true;
    }

    public void give(Player player) {
        give(player, WandType.MENU);
    }

    public void give(Player player, WandType type) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(create(type));
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    private Component lore(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }
}
