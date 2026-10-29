package dev.taikin.mcplugins.cifviewer;

import dev.taikin.mcplugins.cifviewer.gui.CifControlGui;
import dev.taikin.mcplugins.cifviewer.gui.CifGuiHolder;
import dev.taikin.mcplugins.cifviewer.gui.CifModelListGuiHolder;
import dev.taikin.mcplugins.cifviewer.gui.CifRepeatGuiHolder;
import dev.taikin.mcplugins.cifviewer.render.CrystalRenderer;
import dev.taikin.mcplugins.cifviewer.render.ViewSession;
import io.papermc.paper.event.player.PlayerPickEntityEvent;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class CifListener implements Listener {

    private final CifViewerPlugin plugin;

    public CifListener(CifViewerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        CifWand.WandType wandType = plugin.wand().type(item);
        if (wandType == null) {
            return;
        }

        Player player = event.getPlayer();
        Action action = event.getAction();
        if (wandType == CifWand.WandType.ATOM) {
            event.setCancelled(true);
            handleAtomInteract(player, action);
            return;
        }
        if (wandType == CifWand.WandType.MENU) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.controlGui().open(player);
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
            }
            return;
        }
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.controlGui().open(player);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            try {
                Block clicked = event.getClickedBlock();
                BlockFace face = event.getBlockFace();
                if (player.isSneaking() && clicked != null && face != null) {
                    Location origin = clicked.getRelative(face).getLocation().add(0.5, 0.5, 0.5);
                    plugin.renderer().setOrigin(player, origin);
                    player.sendMessage(Component.text("原点（結晶の0,0,0）をブロック ("
                            + origin.getBlockX() + ", " + origin.getBlockY() + ", "
                            + origin.getBlockZ() + ") に設定しました。", NamedTextColor.GREEN));
                } else if (clicked != null && face != null) {
                    ViewSession.Axis axis = axisFor(face);
                    plugin.renderer().setRotationAxis(player, axis);
                    player.sendActionBar(Component.text("回転軸: " + axis.name(), NamedTextColor.AQUA));
                } else {
                    plugin.renderer().rotateAroundAxis(player, player.isSneaking() ? -15.0 : 15.0);
                }
            } catch (IllegalStateException e) {
                player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        CifWand.WandType heldType = plugin.wand().type(player.getInventory().getItemInMainHand());
        if (heldType == CifWand.WandType.ATOM) {
            event.setCancelled(true);
            handleAtomInteract(player, Action.RIGHT_CLICK_AIR);
            return;
        }
        CrystalRenderer.ModelRef ref = plugin.renderer().modelRef(event.getRightClicked().getUniqueId());
        if (ref == null) {
            return;
        }
        event.setCancelled(true);
        handleModelClick(player, ref, true, heldType);
    }

    @EventHandler
    public void onPickEntity(PlayerPickEntityEvent event) {
        CrystalRenderer.ModelRef ref = plugin.renderer().modelRef(event.getEntity().getUniqueId());
        if (ref == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!plugin.renderer().select(player, ref.modelId())) {
            player.sendMessage(Component.text("モデル #" + ref.modelId() + " はありません。",
                    NamedTextColor.GRAY));
            return;
        }
        if (plugin.wand().type(player.getInventory().getItemInMainHand()) == CifWand.WandType.ATOM) {
            String text = plugin.renderer().togglePolyhedronCenter(player);
            player.sendActionBar(Component.text(text, NamedTextColor.AQUA));
            return;
        }
        ViewSession selected = plugin.renderer().session(player);
        if (selected != null) {
            String shared = selected.sharedEdit() ? " [共有]" : "";
            player.sendActionBar(Component.text("選択中: モデル #" + selected.id() + " "
                    + selected.crystal().name() + shared, NamedTextColor.AQUA));
        }
        if (!plugin.wand().has(player)) {
            plugin.wand().give(player, CifWand.WandType.MENU);
            player.sendMessage(Component.text("CIFメニュー棒を入手しました。", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        CrystalRenderer.ModelRef ref = plugin.renderer().modelRef(event.getEntity().getUniqueId());
        if (ref == null) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        CifWand.WandType heldType = plugin.wand().type(player.getInventory().getItemInMainHand());
        if (heldType == CifWand.WandType.ATOM) {
            handleAtomInteract(player, Action.LEFT_CLICK_AIR);
            return;
        }
        handleModelClick(player, ref, false, heldType);
    }

    private void handleModelClick(Player player, CrystalRenderer.ModelRef ref, boolean rightClick,
            CifWand.WandType heldType) {
        if (!plugin.renderer().select(player, ref.modelId())) {
            player.sendMessage(Component.text("モデル #" + ref.modelId() + " はありません。",
                    NamedTextColor.GRAY));
            return;
        }
        ViewSession selected = plugin.renderer().session(player);
        if (selected != null) {
            String shared = selected.sharedEdit() ? " [共有]" : "";
            player.sendActionBar(Component.text("選択中: モデル #" + selected.id() + " "
                    + selected.crystal().name() + shared, NamedTextColor.AQUA));
        }
        if (heldType == null && !plugin.wand().has(player)) {
            plugin.wand().give(player, CifWand.WandType.MENU);
            player.sendMessage(Component.text("CIFメニュー棒を入手しました。モデル #" + ref.modelId()
                    + " を選択しました。", NamedTextColor.GREEN));
            return;
        }
        if (rightClick || heldType == null || heldType == CifWand.WandType.MENU) {
            plugin.controlGui().open(player);
            return;
        }
        try {
            plugin.renderer().rotateAroundAxis(player, player.isSneaking() ? -15.0 : 15.0);
        } catch (IllegalStateException e) {
            player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleAtomInteract(Player player, Action action) {
        try {
            String text;
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                text = player.isSneaking()
                        ? plugin.renderer().togglePolyhedronCenter(player)
                        : plugin.renderer().addAtomSelection(player);
            } else {
                text = player.isSneaking()
                        ? plugin.renderer().clearPolyhedronCenters(player)
                        : plugin.renderer().clearAtomSelection(player);
            }
            player.sendActionBar(Component.text(text, NamedTextColor.AQUA));
        } catch (IllegalStateException e) {
            player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
    }

    private ViewSession.Axis axisFor(BlockFace face) {
        return switch (face) {
            case UP, DOWN -> ViewSession.Axis.Y;
            case NORTH, SOUTH -> ViewSession.Axis.Z;
            case EAST, WEST -> ViewSession.Axis.X;
            default -> ViewSession.Axis.Y;
        };
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        ItemStack previous = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack next = player.getInventory().getItem(event.getNewSlot());
        if (plugin.wand().type(previous) != CifWand.WandType.VIEW
                && plugin.wand().type(next) != CifWand.WandType.VIEW) {
            return;
        }

        event.setCancelled(true);
        int delta = event.getNewSlot() - event.getPreviousSlot();
        if (delta == 0) {
            return;
        }
        try {
            plugin.renderer().adjustScale(player, delta > 0 ? 1.1 : 1.0 / 1.1);
        } catch (IllegalStateException e) {
            player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        UUID owner;
        if (holder instanceof CifGuiHolder guiHolder) {
            owner = guiHolder.playerId();
        } else if (holder instanceof CifRepeatGuiHolder repeatHolder) {
            owner = repeatHolder.playerId();
        } else if (holder instanceof CifModelListGuiHolder modelListHolder) {
            owner = modelListHolder.playerId();
        } else {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!owner.equals(player.getUniqueId())) {
            return;
        }
        if (event.getClickedInventory() != top) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        if (holder instanceof CifGuiHolder) {
            plugin.controlGui().handleClick(player, top, slot);
        } else if (holder instanceof CifRepeatGuiHolder) {
            plugin.repeatGui().handleClick(player, top, slot);
        } else {
            plugin.modelListGui().handleClick(player, top, slot, event.isShiftClick(),
                    event.isRightClick());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof CifGuiHolder || holder instanceof CifRepeatGuiHolder
                || holder instanceof CifModelListGuiHolder) {
            event.setCancelled(true);
        }
    }
}
