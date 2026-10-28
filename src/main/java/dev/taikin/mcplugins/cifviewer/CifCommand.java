package dev.taikin.mcplugins.cifviewer;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.taikin.mcplugins.cifviewer.cif.CifParser;
import dev.taikin.mcplugins.cifviewer.model.Crystal;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CifCommand {

    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    private static final int PAGE_SIZE = 10;

    private final CifViewerPlugin plugin;

    public CifCommand(CifViewerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("cif")
                .requires(source -> source.getSender().hasPermission("cifviewer.use"))
                .executes(context -> help(sender(context)))
                .then(Commands.literal("help")
                        .executes(context -> help(sender(context))))
                .then(Commands.literal("wand")
                        .executes(context -> wand(sender(context))))
                .then(Commands.literal("pack")
                        .executes(context -> pack(sender(context))))
                .then(Commands.literal("download")
                        .executes(context -> pack(sender(context))))
                .then(Commands.literal("list")
                        .executes(context -> list(sender(context), 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> list(sender(context),
                                        IntegerArgumentType.getInteger(context, "page")))))
                .then(Commands.literal("view")
                        .then(Commands.argument("file", StringArgumentType.greedyString())
                                .suggests(this::suggestFiles)
                                .executes(context -> view(sender(context),
                                        StringArgumentType.getString(context, "file"), null, null))))
                .then(Commands.literal("viewat")
                        .then(Commands.argument("scale", DoubleArgumentType.doubleArg(0.1, 10.0))
                                .then(Commands.argument("repeat", IntegerArgumentType.integer(1, 8))
                                        .then(Commands.argument("file", StringArgumentType.greedyString())
                                                .suggests(this::suggestFiles)
                                                .executes(context -> view(sender(context),
                                                        StringArgumentType.getString(context, "file"),
                                                        DoubleArgumentType.getDouble(context, "scale"),
                                                        IntegerArgumentType.getInteger(context, "repeat")))))))
                .then(Commands.literal("fetch")
                        .then(Commands.argument("material", StringArgumentType.word())
                                .executes(context -> fetch(sender(context),
                                        StringArgumentType.getString(context, "material"), null, null))))
                .then(Commands.literal("fetchat")
                        .then(Commands.argument("scale", DoubleArgumentType.doubleArg(0.1, 10.0))
                                .then(Commands.argument("repeat", IntegerArgumentType.integer(1, 8))
                                        .then(Commands.argument("material", StringArgumentType.word())
                                                .executes(context -> fetch(sender(context),
                                                        StringArgumentType.getString(context, "material"),
                                                        DoubleArgumentType.getDouble(context, "scale"),
                                                        IntegerArgumentType.getInteger(context, "repeat")))))))
                .then(Commands.literal("models")
                        .executes(context -> models(sender(context))))
                .then(Commands.literal("select")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(context -> select(sender(context),
                                        IntegerArgumentType.getInteger(context, "id")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(context -> remove(sender(context),
                                        IntegerArgumentType.getInteger(context, "id")))))
                .then(Commands.literal("clear")
                        .executes(context -> clear(sender(context))))
                .then(Commands.literal("clearall")
                        .executes(context -> clearAll(sender(context))))
                .then(Commands.literal("reload")
                        .executes(context -> reload(sender(context))))
                .build();
    }

    private int help(CommandSender sender) {
        sender.sendMessage(Component.text("CIF Viewer ヘルプ", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("表示", NamedTextColor.AQUA));
        sender.sendMessage(command("/cif list", "CIF一覧を表示", true));
        sender.sendMessage(command("/cif view ", "CIFモデルを追加", false));
        sender.sendMessage(command("/cif viewat ", "倍率と繰り返しを指定して追加", false));
        sender.sendMessage(command("/cif fetch ", "Materials Project から追加", false));
        sender.sendMessage(command("/cif fetchat ", "倍率と繰り返しを指定して追加", false));
        sender.sendMessage(command("/cif models", "表示中モデル一覧", true));
        sender.sendMessage(command("/cif select ", "操作するモデルを選択", false));
        sender.sendMessage(command("/cif remove ", "指定モデルを削除", false));
        sender.sendMessage(command("/cif clear", "選択中モデルを削除", true));
        sender.sendMessage(command("/cif clearall", "全モデルを削除", true));

        sender.sendMessage(Component.text("道具", NamedTextColor.AQUA));
        sender.sendMessage(command("/cif wand", "CIF操作棒を取得", true));
        sender.sendMessage(command("/cif pack", "リソースパックを再適用", true));
        sender.sendMessage(command("/cif reload", "設定を再読み込み", true));

        sender.sendMessage(Component.text("CIF操作棒", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("右クリック: メニュー", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("モデルをクリック: 操作棒を入手 / メニュー", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("左クリック(空): 選択軸で15°回転（スニークで逆回転）",
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("左クリック(ブロック面): 回転軸を変更", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("スニーク+左クリック(ブロック): 原点(0,0,0)を設定",
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("スニーク+スクロール: 全体サイズを変更", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("原点: 結晶の0,0,0 を基準に配置・回転", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("GUI: 空間充填の半径を vdW / イオン / 共有結合 から選択",
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("GUI: 繰り返しは「軸別リピート」で X / Y / Z を個別設定",
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("GUI: 複数モデルは「モデル一覧」から選択 / Shift+クリックで削除",
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("GUI: 単位胞の枠とボンド（棒球）表示を切替", NamedTextColor.GRAY));
        return 1;
    }

    private Component command(String command, String description, boolean execute) {
        Component label = Component.text(command, NamedTextColor.WHITE)
                .clickEvent(execute ? ClickEvent.runCommand(command) : ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(execute ? "クリックで実行" : "クリックで入力")));
        return label.append(Component.text("  " + description, NamedTextColor.GRAY));
    }

    private int wand(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        plugin.wand().give(player);
        sender.sendMessage(Component.text("CIF操作棒を追加しました。右クリックでメニューを開けます。",
                NamedTextColor.GREEN));
        return 1;
    }

    private int pack(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        return plugin.sendResourcePack(player) ? 1 : 0;
    }

    private int list(CommandSender sender, int page) {
        List<String> keys = plugin.repository().keys();
        if (keys.isEmpty()) {
            sender.sendMessage(Component.text("No CIF files found. Place .cif files in the configured sources.",
                    NamedTextColor.GRAY));
            return 0;
        }

        int pages = (keys.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int currentPage = Math.min(page, pages);
        int from = (currentPage - 1) * PAGE_SIZE;
        int to = Math.min(keys.size(), from + PAGE_SIZE);

        sender.sendMessage(Component.text("CIF files (" + keys.size() + "):", NamedTextColor.GOLD));
        for (int i = from; i < to; i++) {
            sender.sendMessage(Component.text("  " + keys.get(i), NamedTextColor.WHITE));
        }
        if (pages > 1) {
            sender.sendMessage(Component.text("Page " + currentPage + "/" + pages + "  /cif list <page>",
                    NamedTextColor.GRAY));
        }
        return keys.size();
    }

    private int view(CommandSender sender, String file, Double scale, Integer repeat) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }

        Path path = plugin.repository().resolve(file);
        if (path == null) {
            sender.sendMessage(Component.text("CIF not found or ambiguous: " + file, NamedTextColor.RED));
            return 0;
        }

        try {
            long size = Files.size(path);
            if (size > MAX_FILE_SIZE) {
                sender.sendMessage(Component.text("CIF file is too large (" + size + " bytes).", NamedTextColor.RED));
                return 0;
            }
            String text = Files.readString(path, StandardCharsets.UTF_8);
            Crystal crystal = new CifParser().parse(path.getFileName().toString(), text);
            int count = plugin.renderer().view(player, crystal,
                    scale == null ? plugin.config().blocksPerAngstrom() : scale,
                    repeat == null ? 1 : repeat);
            int id = plugin.renderer().activeId(player);
            sender.sendMessage(Component.text("モデル #" + id + " を追加: " + crystal.name()
                    + " (" + count + " atoms).", NamedTextColor.GREEN));
            giveWand(player);
            return count;
        } catch (IOException e) {
            sender.sendMessage(Component.text("Could not read CIF: " + e.getMessage(), NamedTextColor.RED));
            return 0;
        } catch (RuntimeException e) {
            sender.sendMessage(Component.text("Could not display CIF: " + message(e), NamedTextColor.RED));
            return 0;
        }
    }

    private int fetch(CommandSender sender, String materialId, Double scale, Integer repeat) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        if (!plugin.materialsProject().enabled()) {
            sender.sendMessage(Component.text("Materials Project API key is not set in config.yml.",
                    NamedTextColor.RED));
            return 0;
        }

        double usedScale = scale == null ? plugin.config().blocksPerAngstrom() : scale;
        int usedRepeat = repeat == null ? 1 : repeat;
        player.sendMessage(Component.text("Fetching " + materialId + " from Materials Project...",
                NamedTextColor.GRAY));

        plugin.materialsProject().fetch(materialId).whenComplete((crystal, error) -> {
            if (!plugin.isEnabled()) {
                return;
            }
            try {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (error != null) {
                        player.sendMessage(Component.text("Materials Project fetch failed: "
                                + message(unwrap(error)), NamedTextColor.RED));
                        return;
                    }
                    try {
                        int count = plugin.renderer().view(player, crystal, usedScale, usedRepeat);
                        int id = plugin.renderer().activeId(player);
                        player.sendMessage(Component.text("モデル #" + id + " を追加: " + crystal.name()
                                + " (" + count + " atoms).", NamedTextColor.GREEN));
                        giveWand(player);
                    } catch (RuntimeException e) {
                        player.sendMessage(Component.text("Could not display structure: " + message(e),
                                NamedTextColor.RED));
                    }
                });
            } catch (RuntimeException ignored) {
            }
        });
        return 1;
    }

    private void giveWand(Player player) {
        if (plugin.wand().giveIfMissing(player)) {
            player.sendMessage(Component.text("CIF操作棒を付与しました。右クリックでメニュー。",
                    NamedTextColor.GREEN));
        }
    }

    private int models(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        var models = plugin.renderer().models(player);
        if (models.isEmpty()) {
            sender.sendMessage(Component.text("表示中のモデルはありません。", NamedTextColor.GRAY));
            return 0;
        }
        int activeId = plugin.renderer().activeId(player);
        sender.sendMessage(Component.text("表示中モデル (" + models.size() + "):", NamedTextColor.GOLD));
        for (var session : models) {
            String marker = session.id() == activeId ? "* " : "  ";
            sender.sendMessage(Component.text(marker + "#" + session.id() + " " + session.crystal().name()
                    + " (" + session.renderedAtoms() + " atoms)", NamedTextColor.WHITE));
        }
        return models.size();
    }

    private int select(CommandSender sender, int id) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        if (!plugin.renderer().select(player, id)) {
            sender.sendMessage(Component.text("モデル #" + id + " はありません。", NamedTextColor.RED));
            return 0;
        }
        sender.sendMessage(Component.text("モデル #" + id + " を選択しました。", NamedTextColor.GREEN));
        return 1;
    }

    private int remove(CommandSender sender, int id) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        int removed = plugin.renderer().clear(player, id);
        if (removed == 0) {
            sender.sendMessage(Component.text("モデル #" + id + " はありません。", NamedTextColor.RED));
            return 0;
        }
        sender.sendMessage(Component.text("モデル #" + id + " を削除しました (" + removed + " entities).",
                NamedTextColor.GREEN));
        return removed;
    }

    private int clear(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        int id = plugin.renderer().activeId(player);
        int removed = plugin.renderer().clear(player);
        if (removed == 0) {
            sender.sendMessage(Component.text("表示中のモデルはありません。", NamedTextColor.GRAY));
            return 0;
        }
        sender.sendMessage(Component.text("モデル #" + id + " を削除しました (" + removed + " entities).",
                NamedTextColor.GREEN));
        return removed;
    }

    private int clearAll(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        int removed = plugin.renderer().clearAll(player);
        sender.sendMessage(Component.text("全モデルを削除しました (" + removed + " entities).",
                NamedTextColor.GREEN));
        return removed;
    }

    private int reload(CommandSender sender) {
        try {
            plugin.reloadPlugin();
            sender.sendMessage(Component.text("Reloaded CifViewer (" + plugin.repository().keys().size()
                    + " CIF file(s)).", NamedTextColor.GREEN));
            return 1;
        } catch (RuntimeException e) {
            sender.sendMessage(Component.text("Reload failed: " + message(e), NamedTextColor.RED));
            return 0;
        }
    }

    private CompletableFuture<Suggestions> suggestFiles(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        int added = 0;
        for (String key : plugin.repository().keys()) {
            if (key.toLowerCase(Locale.ROOT).contains(remaining)) {
                builder.suggest(key);
                added++;
                if (added >= 50) {
                    break;
                }
            }
        }
        return builder.buildFuture();
    }

    private CommandSender sender(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getExecutor() instanceof Player player) {
            return player;
        }
        return context.getSource().getSender();
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(Component.text("This command must be run by a player.", NamedTextColor.RED));
        return null;
    }

    private Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String message(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
