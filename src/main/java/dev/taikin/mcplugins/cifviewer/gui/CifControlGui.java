package dev.taikin.mcplugins.cifviewer.gui;

import dev.taikin.mcplugins.cifviewer.CifViewerPlugin;
import dev.taikin.mcplugins.cifviewer.render.ViewSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

public final class CifControlGui {

    public static final int SIZE = 36;

    private static final int MODELS = 0;
    private static final int INFO = 4;
    private static final int BONDS = 6;
    private static final int SHARE = 7;
    private static final int UNIT_CELL = 8;
    private static final int LABELS = 10;
    private static final int SCALE_UP = 9;
    private static final int REPEAT_UP = 11;
    private static final int ATOM_SCALE_UP = 13;
    private static final int ROTATE_X_UP = 15;
    private static final int ROTATE_Y_UP = 16;
    private static final int ROTATE_Z_UP = 17;
    private static final int SCALE_DOWN = 18;
    private static final int REPEAT_DOWN = 20;
    private static final int ATOM_SCALE_DOWN = 22;
    private static final int ROTATE_X_DOWN = 24;
    private static final int ROTATE_Y_DOWN = 25;
    private static final int ROTATE_Z_DOWN = 26;
    private static final int RECENTER = 27;
    private static final int SPACE_FILLING = 28;
    private static final int RESET = 29;
    private static final int ROTATION_AXIS = 30;
    private static final int PACK = 31;
    private static final int RADIUS_MODE = 32;
    private static final int CLEAR = 33;
    private static final int REPEAT_AXES = 34;
    private static final int CLOSE = 35;

    private final CifViewerPlugin plugin;

    public CifControlGui(CifViewerPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ViewSession session = plugin.renderer().session(player);
        if (session == null) {
            player.sendMessage(Component.text("構造が表示されていません。先に /cif view <file> を実行してください。",
                    NamedTextColor.RED));
            return;
        }

        CifGuiHolder holder = new CifGuiHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, title(session));
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
                case SCALE_DOWN -> plugin.renderer().adjustScale(player, 1.0 / 1.15);
                case SCALE_UP -> plugin.renderer().adjustScale(player, 1.15);
                case REPEAT_DOWN -> plugin.renderer().adjustRepeat(player, -1);
                case REPEAT_UP -> plugin.renderer().adjustRepeat(player, 1);
                case ATOM_SCALE_UP -> plugin.renderer().adjustAtomScale(player, 1.15);
                case ATOM_SCALE_DOWN -> plugin.renderer().adjustAtomScale(player, 1.0 / 1.15);
                case ROTATE_X_DOWN -> plugin.renderer().rotate(player, -15.0, 0.0, 0.0);
                case ROTATE_X_UP -> plugin.renderer().rotate(player, 15.0, 0.0, 0.0);
                case ROTATE_Y_DOWN -> plugin.renderer().rotate(player, 0.0, -15.0, 0.0);
                case ROTATE_Y_UP -> plugin.renderer().rotate(player, 0.0, 15.0, 0.0);
                case ROTATE_Z_DOWN -> plugin.renderer().rotate(player, 0.0, 0.0, -15.0);
                case ROTATE_Z_UP -> plugin.renderer().rotate(player, 0.0, 0.0, 15.0);
                case RECENTER -> plugin.renderer().replaceOrigin(player);
                case SPACE_FILLING -> plugin.renderer().toggleSpaceFilling(player);
                case BONDS -> plugin.renderer().toggleBonds(player);
                case SHARE -> plugin.renderer().toggleSharedEdit(player);
                case UNIT_CELL -> plugin.renderer().toggleUnitCell(player);
                case LABELS -> plugin.renderer().toggleLabels(player);
                case RADIUS_MODE -> plugin.renderer().cycleRadiusMode(player);
                case RESET -> plugin.renderer().reset(player);
                case ROTATION_AXIS -> plugin.renderer().cycleRotationAxis(player);
                case REPEAT_AXES -> {
                    plugin.repeatGui().open(player);
                    return;
                }
                case MODELS -> {
                    plugin.modelListGui().open(player);
                    return;
                }
                case PACK -> {
                    plugin.sendResourcePack(player);
                    return;
                }
                case CLEAR -> {
                    int removed = plugin.renderer().clear(player);
                    if (removed == 0) {
                        player.sendMessage(Component.text("このモデルを削除する権限がありません。",
                                NamedTextColor.RED));
                        return;
                    }
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
                    return;
                }
                case CLOSE -> {
                    player.closeInventory();
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

        inventory.setItem(MODELS, button(Material.CHEST, "モデル一覧",
                List.of(value("複数モデルの選択 / 削除"), value("選択中: #" + session.id()))));
        inventory.setItem(BONDS, bondsButton(session));
        inventory.setItem(SHARE, shareButton(session));
        inventory.setItem(UNIT_CELL, unitCellButton(session));
        inventory.setItem(LABELS, labelsButton(session));
        inventory.setItem(INFO, info(session));
        inventory.setItem(SCALE_UP, button(Material.LIME_STAINED_GLASS_PANE, "全体サイズ +",
                List.of(value("ブロック/Å: " + number(session.blocksPerAngstrom())))));
        inventory.setItem(SCALE_DOWN, button(Material.RED_STAINED_GLASS_PANE, "全体サイズ -",
                List.of(value("ブロック/Å: " + number(session.blocksPerAngstrom())))));
        inventory.setItem(REPEAT_UP, button(Material.LIME_STAINED_GLASS_PANE, "繰り返し + (全体)",
                List.of(value("セル数: " + repeatText(session)))));
        inventory.setItem(REPEAT_DOWN, button(Material.RED_STAINED_GLASS_PANE, "繰り返し - (全体)",
                List.of(value("セル数: " + repeatText(session)))));
        inventory.setItem(ATOM_SCALE_UP, button(Material.LIME_STAINED_GLASS_PANE, "原子サイズ +",
                List.of(value("原子倍率: " + number(session.atomScale())))));
        inventory.setItem(ATOM_SCALE_DOWN, button(Material.RED_STAINED_GLASS_PANE, "原子サイズ -",
                List.of(value("原子倍率: " + number(session.atomScale())))));

        inventory.setItem(ROTATE_X_UP, rotationButton(Material.LIME_STAINED_GLASS_PANE, "X軸回転 +",
                session.rotationX()));
        inventory.setItem(ROTATE_X_DOWN, rotationButton(Material.RED_STAINED_GLASS_PANE, "X軸回転 -",
                session.rotationX()));
        inventory.setItem(ROTATE_Y_UP, rotationButton(Material.LIME_STAINED_GLASS_PANE, "Y軸回転 +",
                session.rotationY()));
        inventory.setItem(ROTATE_Y_DOWN, rotationButton(Material.RED_STAINED_GLASS_PANE, "Y軸回転 -",
                session.rotationY()));
        inventory.setItem(ROTATE_Z_UP, rotationButton(Material.LIME_STAINED_GLASS_PANE, "Z軸回転 +",
                session.rotationZ()));
        inventory.setItem(ROTATE_Z_DOWN, rotationButton(Material.RED_STAINED_GLASS_PANE, "Z軸回転 -",
                session.rotationZ()));

        inventory.setItem(RECENTER, button(Material.ENDER_PEARL, "原点をここに",
                List.of(value("結晶の0,0,0を目の前に配置"))));
        inventory.setItem(SPACE_FILLING, spaceFillingButton(session));
        inventory.setItem(RADIUS_MODE, radiusModeButton(session));
        inventory.setItem(RESET, button(Material.COMPASS, "リセット",
                List.of(value("倍率・繰り返し・回転をリセット"))));
        inventory.setItem(ROTATION_AXIS, rotationAxisButton(session));
        inventory.setItem(REPEAT_AXES, button(Material.REPEATER, "軸別リピート",
                List.of(value("X / Y / Z を個別に設定"), value("現在: " + repeatText(session)))));
        inventory.setItem(PACK, button(Material.KNOWLEDGE_BOOK, "リソースパック",
                List.of(value("クリックでダウンロード / 再適用"), value("3D球体モデル"))));
        inventory.setItem(CLEAR, button(Material.BARRIER, "このモデルを消去",
                List.of(value("選択中のモデルを削除"))));
        inventory.setItem(CLOSE, button(Material.OAK_DOOR, "閉じる", List.of(value("メニューを閉じる"))));
    }

    private ItemStack info(ViewSession session) {
        List<Component> lore = new ArrayList<>();
        lore.add(value(session.crystal().contents() + "  (単位格子あたり "
                + session.crystal().atoms().size() + " 原子)"));
        lore.add(value("モデル: #" + session.id()));
        lore.add(value("所有者: " + ownerName(session)));
        lore.add(value("共有編集: " + (session.sharedEdit() ? "ON" : "OFF")));
        lore.add(value("表示原子数: " + session.renderedAtoms()));
        lore.add(value("表示形式: " + (session.spaceFilling() ? "空間充填" : "球体")));
        lore.add(value("単位胞: " + (session.showUnitCell() ? "ON" : "OFF")));
        lore.add(value("ボンド: " + (session.showBonds() ? "ON" : "OFF")));
        lore.add(value("ラベル: " + (session.showLabels() ? "ON" : "OFF")));
        lore.add(value("選択原子: " + session.selectedAtoms().size()));
        lore.add(value("充填半径: " + radiusModeName(session.radiusMode())));
        lore.add(value("回転軸: " + session.rotationAxis().name()));
        lore.add(value("原点: 結晶の0,0,0"));
        lore.add(value("倍率: " + number(session.blocksPerAngstrom()) + " ブロック/Å"));
        lore.add(value("繰り返し: " + repeatText(session)));
        lore.add(value("原子サイズ: " + number(session.atomScale())));
        lore.add(value(String.format(Locale.ROOT, "回転: %.0f / %.0f / %.0f°",
                session.rotationX(), session.rotationY(), session.rotationZ())));
        return button(Material.BOOK, "CIF Viewer", lore);
    }

    private ItemStack rotationAxisButton(ViewSession session) {
        return button(Material.ARROW, "回転軸: " + session.rotationAxis().name(),
                List.of(value("クリックで X → Y → Z"), value("ワンドの左クリックに適用")));
    }

    private ItemStack spaceFillingButton(ViewSession session) {
        if (session.spaceFilling()) {
            return button(Material.SLIME_BALL, "表示: 空間充填",
                    List.of(value("クリックで球体表示に変更"), value("半径: " + radiusModeName(session.radiusMode()))));
        }
        return button(Material.SNOWBALL, "表示: 球体",
                List.of(value("クリックで空間充填に変更"), value("半径: 共有結合半径ベース")));
    }

    private ItemStack labelsButton(ViewSession session) {
        if (session.showLabels()) {
            return button(Material.NAME_TAG, "ラベル: ON",
                    List.of(value("全原子のラベルを表示"), value("選択原子は常に表示")));
        }
        return button(Material.PAPER, "ラベル: OFF",
                List.of(value("全原子のラベルを非表示"), value("選択原子は常に表示")));
    }

    private ItemStack shareButton(ViewSession session) {
        if (session.sharedEdit()) {
            return button(Material.ENDER_EYE, "共有編集: ON",
                    List.of(value("他の人も操作できます"), value("所有者 / 管理者が変更可能")));
        }
        return button(Material.ENDER_PEARL, "共有編集: OFF",
                List.of(value("所有者のみ操作できます"), value("所有者 / 管理者が変更可能")));
    }

    private ItemStack unitCellButton(ViewSession session) {
        if (session.showUnitCell()) {
            return button(Material.SCAFFOLDING, "単位胞: ON",
                    List.of(value("クリックで非表示"), value("格子の枠を表示")));
        }
        return button(Material.IRON_BARS, "単位胞: OFF",
                List.of(value("クリックで表示"), value("格子の枠を表示")));
    }

    private ItemStack bondsButton(ViewSession session) {
        if (session.showBonds()) {
            return button(Material.BLAZE_ROD, "ボンド: ON",
                    List.of(value("クリックで非表示"), value("球体表示時に結合線を表示")));
        }
        return button(Material.STICK, "ボンド: OFF",
                List.of(value("クリックで表示"), value("球体表示時に結合線を表示")));
    }

    private ItemStack radiusModeButton(ViewSession session) {
        return switch (session.radiusMode()) {
            case VDW -> button(Material.AMETHYST_SHARD, "充填半径: vdW",
                    List.of(value("分子結晶向け"), value("クリックでイオン半径")));
            case IONIC -> button(Material.AMETHYST_SHARD, "充填半径: イオン",
                    List.of(value("代表イオン半径（NaClなど）"), value("クリックで共有結合半径")));
            case COVALENT -> button(Material.AMETHYST_SHARD, "充填半径: 共有結合",
                    List.of(value("共有結合半径"), value("クリックでvdW半径")));
        };
    }

    private String ownerName(ViewSession session) {
        if (session.owner() == null) {
            return "不明";
        }
        String name = Bukkit.getOfflinePlayer(session.owner()).getName();
        return name == null ? session.owner().toString() : name;
    }

    private String radiusModeName(ViewSession.RadiusMode mode) {
        return switch (mode) {
            case VDW -> "vdW";
            case IONIC -> "イオン";
            case COVALENT -> "共有結合";
        };
    }

    private String repeatText(ViewSession session) {
        return "X" + session.repeatX() + " Y" + session.repeatY() + " Z" + session.repeatZ();
    }

    private ItemStack rotationButton(Material material, String name, double angle) {
        return button(material, name, List.of(value("現在: " + number(angle) + "°")));
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

    private Component title(ViewSession session) {
        String name = session.crystal().name();
        if (name.length() > 24) {
            name = name.substring(0, 23) + "...";
        }
        return Component.text("CIF: " + name, NamedTextColor.DARK_AQUA);
    }

    private String number(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
