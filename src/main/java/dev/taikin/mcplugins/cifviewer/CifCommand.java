package dev.taikin.mcplugins.cifviewer;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.taikin.mcplugins.cifviewer.cif.CifParser;
import dev.taikin.mcplugins.cifviewer.model.Crystal;
import dev.taikin.mcplugins.cifviewer.xrd.XrdMapRenderer;
import dev.taikin.mcplugins.cifviewer.xrd.XrdPattern;
import dev.taikin.mcplugins.cifviewer.xrd.XrdSimulator;
import dev.taikin.mcplugins.cifviewer.xrd.XraySource;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

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
                        .executes(context -> wand(sender(context), CifWand.WandType.OPERATION))
                        .then(Commands.literal("operation")
                                .executes(context -> wand(sender(context), CifWand.WandType.OPERATION)))
                        .then(Commands.literal("atom")
                                .executes(context -> wand(sender(context), CifWand.WandType.ATOM))))
                .then(Commands.literal("wands")
                        .executes(context -> wands(sender(context))))
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
                .then(Commands.literal("cod")
                        .then(Commands.argument("id", LongArgumentType.longArg(1))
                                .executes(context -> cod(sender(context),
                                        Long.toString(LongArgumentType.getLong(context, "id")), null, null))))
                .then(Commands.literal("codat")
                        .then(Commands.argument("scale", DoubleArgumentType.doubleArg(0.1, 10.0))
                                .then(Commands.argument("repeat", IntegerArgumentType.integer(1, 8))
                                        .then(Commands.argument("id", LongArgumentType.longArg(1))
                                                .executes(context -> cod(sender(context),
                                                        Long.toString(LongArgumentType.getLong(context, "id")),
                                                        DoubleArgumentType.getDouble(context, "scale"),
                                                        IntegerArgumentType.getInteger(context, "repeat")))))))
                .then(Commands.literal("xrd")
                        .executes(context -> xrd(sender(context), null, null, null, null))
                        .then(Commands.argument("file", StringArgumentType.greedyString())
                                .suggests(this::suggestFiles)
                                .executes(context -> xrd(sender(context),
                                        StringArgumentType.getString(context, "file"), null, null, null))))
                .then(Commands.literal("xrdat")
                        .then(Commands.argument("source", StringArgumentType.word())
                                .then(Commands.argument("min", DoubleArgumentType.doubleArg(0.1, 179.0))
                                        .then(Commands.argument("max", DoubleArgumentType.doubleArg(0.2, 179.9))
                                                .executes(context -> xrd(sender(context), null,
                                                        StringArgumentType.getString(context, "source"),
                                                        DoubleArgumentType.getDouble(context, "min"),
                                                        DoubleArgumentType.getDouble(context, "max")))
                                                .then(Commands.argument("file", StringArgumentType.greedyString())
                                                        .suggests(this::suggestFiles)
                                                        .executes(context -> xrd(sender(context),
                                                                StringArgumentType.getString(context, "file"),
                                                                StringArgumentType.getString(context, "source"),
                                                                DoubleArgumentType.getDouble(context, "min"),
                                                                DoubleArgumentType.getDouble(context, "max"))))))))
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
                .then(Commands.literal("tp")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(context -> teleport(sender(context),
                                        IntegerArgumentType.getInteger(context, "id")))))
                .then(Commands.literal("share")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .then(Commands.literal("on")
                                        .executes(context -> share(sender(context),
                                                IntegerArgumentType.getInteger(context, "id"), true)))
                                .then(Commands.literal("off")
                                        .executes(context -> share(sender(context),
                                                IntegerArgumentType.getInteger(context, "id"), false)))))
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
        sender.sendMessage(command("/cif cod ", "COD ID から取得して保存", false));
        sender.sendMessage(command("/cif codat ", "倍率・繰り返し指定で COD から取得", false));
        sender.sendMessage(command("/cif xrd", "表示中モデルから XRD 地図を作成", true));
        sender.sendMessage(command("/cif xrd ", "CIF から XRD シミュレーション地図を作成", false));
        sender.sendMessage(command("/cif xrdat ", "線源と 2θ 範囲を指定して XRD 地図を作成", false));
        sender.sendMessage(command("/cif models", "全モデル一覧", true));
        sender.sendMessage(command("/cif select ", "操作するモデルを選択", false));
        sender.sendMessage(command("/cif remove ", "指定モデルを削除（所有者/管理者）", false));
        sender.sendMessage(command("/cif tp ", "モデルの近くへ移動", false));
        sender.sendMessage(command("/cif share ", "共有編集の ON/OFF（所有者/管理者）", false));
        sender.sendMessage(command("/cif clear", "選択中モデルを削除", true));
        sender.sendMessage(command("/cif clearall", "自分のモデルを削除（管理者は全削除）", true));

        sender.sendMessage(Component.text("道具", NamedTextColor.AQUA));
        sender.sendMessage(command("/cif wand", "CIF操作棒を取得", true));
        sender.sendMessage(command("/cif wand atom", "CIF原子棒を取得", false));
        sender.sendMessage(command("/cif wands", "操作棒と原子棒をまとめて取得", true));
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
        sender.sendMessage(Component.text("GUI: 原子ラベル ON/OFF / 選択原子の表示", NamedTextColor.GRAY));

        sender.sendMessage(Component.text("CIF原子棒", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("右クリック: 原子を選択 / 追加", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("左クリック: 選択解除", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("スニーク+右クリック: 最後の選択を解除", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("2原子: 距離 / 3原子: 角度 / 4原子: 二面角", NamedTextColor.GRAY));
        return 1;
    }

    private Component command(String command, String description, boolean execute) {
        Component label = Component.text(command, NamedTextColor.WHITE)
                .clickEvent(execute ? ClickEvent.runCommand(command) : ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(execute ? "クリックで実行" : "クリックで入力")));
        return label.append(Component.text("  " + description, NamedTextColor.GRAY));
    }

    private int wand(CommandSender sender, CifWand.WandType type) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        plugin.wand().give(player, type);
        sender.sendMessage(Component.text(type == CifWand.WandType.ATOM
                ? "CIF原子棒を追加しました。右クリックで原子を選択できます。"
                : "CIF操作棒を追加しました。右クリックでメニューを開けます。",
                NamedTextColor.GREEN));
        return 1;
    }

    private int wands(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        plugin.wand().giveIfMissing(player, CifWand.WandType.OPERATION);
        plugin.wand().giveIfMissing(player, CifWand.WandType.ATOM);
        sender.sendMessage(Component.text("CIF操作棒とCIF原子棒を付与しました。",
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

    private int cod(CommandSender sender, String id, Double scale, Integer repeat) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        try {
            Path cached = plugin.cod().cachePath(id);
            if (Files.isRegularFile(cached)) {
                player.sendMessage(Component.text("COD " + id + " のローカルCIFを使用します。",
                        NamedTextColor.GRAY));
                return viewCod(player, cached, id, scale, repeat);
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            return 0;
        }

        player.sendMessage(Component.text("COD " + id + " を取得中...", NamedTextColor.GRAY));
        plugin.cod().fetch(id).whenComplete((text, error) -> {
            if (!plugin.isEnabled()) {
                return;
            }
            try {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (error != null) {
                        player.sendMessage(Component.text("COD fetch failed: "
                                + message(unwrap(error)), NamedTextColor.RED));
                        return;
                    }
                    try {
                        Path path = plugin.cod().save(id, text);
                        plugin.repository().reload();
                        player.sendMessage(Component.text("COD CIF を保存: " + path.getFileName(),
                                NamedTextColor.GRAY));
                        viewCod(player, path, id, scale, repeat);
                    } catch (IOException | RuntimeException e) {
                        player.sendMessage(Component.text("Could not display COD CIF: " + message(e),
                                NamedTextColor.RED));
                    }
                });
            } catch (RuntimeException ignored) {
            }
        });
        return 1;
    }

    private int viewCod(Player player, Path path, String id, Double scale, Integer repeat) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            Crystal crystal = new CifParser().parse(path.getFileName().toString(), text);
            int count = plugin.renderer().view(player, crystal,
                    scale == null ? plugin.config().blocksPerAngstrom() : scale,
                    repeat == null ? 1 : repeat);
            int modelId = plugin.renderer().activeId(player);
            player.sendMessage(Component.text("COD " + id + " を追加: モデル #" + modelId
                    + " (" + count + " atoms).", NamedTextColor.GREEN));
            giveWand(player);
            return count;
        } catch (IOException e) {
            player.sendMessage(Component.text("Could not read COD CIF: " + e.getMessage(),
                    NamedTextColor.RED));
            return 0;
        } catch (RuntimeException e) {
            player.sendMessage(Component.text("Could not display COD CIF: " + message(e),
                    NamedTextColor.RED));
            return 0;
        }
    }

    private int xrd(CommandSender sender, String file, String source, Double min, Double max) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        try {
            double wavelength = source == null
                    ? plugin.config().xrdWavelength()
                    : XraySource.wavelength(source);
            double twoThetaMin = min == null ? plugin.config().xrdTwoThetaMin() : min;
            double twoThetaMax = max == null ? plugin.config().xrdTwoThetaMax() : max;
            if (twoThetaMax <= twoThetaMin) {
                sender.sendMessage(Component.text("2theta max must be greater than min.",
                        NamedTextColor.RED));
                return 0;
            }

            Crystal crystal;
            if (file == null) {
                var session = plugin.renderer().session(player);
                if (session == null) {
                    sender.sendMessage(Component.text("表示中のモデルがありません。", NamedTextColor.GRAY));
                    return 0;
                }
                crystal = session.crystal();
            } else {
                Path path = plugin.repository().resolve(file);
                if (path == null) {
                    sender.sendMessage(Component.text("CIF not found or ambiguous: " + file,
                            NamedTextColor.RED));
                    return 0;
                }
                long size = Files.size(path);
                if (size > MAX_FILE_SIZE) {
                    sender.sendMessage(Component.text("CIF file is too large (" + size + " bytes).",
                            NamedTextColor.RED));
                    return 0;
                }
                String text = Files.readString(path, StandardCharsets.UTF_8);
                crystal = new CifParser().parse(path.getFileName().toString(), text);
            }
            XrdPattern pattern = XrdSimulator.simulate(crystal,
                    wavelength,
                    twoThetaMin,
                    twoThetaMax,
                    plugin.config().xrdStep(),
                    plugin.config().xrdFwhm(),
                    plugin.config().xrdMaxHkl());

            MapView view = Bukkit.createMap(player.getWorld());
            for (var renderer : List.copyOf(view.getRenderers())) {
                view.removeRenderer(renderer);
            }
            view.setScale(MapView.Scale.CLOSEST);
            view.setTrackingPosition(false);
            view.addRenderer(new XrdMapRenderer(pattern));

            ItemStack map = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) map.getItemMeta();
            meta.setMapView(view);
            meta.displayName(Component.text("XRD: " + crystal.name(), NamedTextColor.AQUA));
            meta.lore(List.of(
                    Component.text("Simulated (approximate) pattern", NamedTextColor.GRAY),
                    Component.text("lambda = " + pattern.wavelength() + " A", NamedTextColor.GRAY),
                    Component.text("2theta = " + pattern.twoThetaMin() + " - "
                            + pattern.twoThetaMax() + " deg", NamedTextColor.GRAY),
                    Component.text("reflections: " + pattern.reflections(), NamedTextColor.GRAY)));
            map.setItemMeta(meta);
            var leftover = player.getInventory().addItem(map);
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            player.sendMessage(Component.text("XRD map: " + crystal.name() + " ("
                    + pattern.reflections() + " reflections, approximate intensities).",
                    NamedTextColor.GREEN));
            return 1;
        } catch (IOException e) {
            sender.sendMessage(Component.text("Could not read CIF: " + e.getMessage(), NamedTextColor.RED));
            return 0;
        } catch (RuntimeException e) {
            sender.sendMessage(Component.text("Could not simulate XRD: " + message(e), NamedTextColor.RED));
            return 0;
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
            String shared = session.sharedEdit() ? " [共有]" : "";
            sender.sendMessage(Component.text(marker + "#" + session.id() + " " + session.crystal().name()
                    + " by " + ownerName(session) + shared + " (" + session.renderedAtoms() + " atoms)",
                    NamedTextColor.WHITE));
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
        player.sendActionBar(Component.text("選択中: モデル #" + id, NamedTextColor.AQUA));
        return 1;
    }

    private int remove(CommandSender sender, int id) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        var session = plugin.renderer().model(id);
        if (session == null) {
            sender.sendMessage(Component.text("モデル #" + id + " はありません。", NamedTextColor.RED));
            return 0;
        }
        if (!plugin.renderer().canDelete(player, session)) {
            sender.sendMessage(Component.text("このモデルを削除する権限がありません。",
                    NamedTextColor.RED));
            return 0;
        }
        int removed = plugin.renderer().clear(player, id);
        sender.sendMessage(Component.text("モデル #" + id + " を削除しました (" + removed + " entities).",
                NamedTextColor.GREEN));
        return removed;
    }

    private int clear(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        var session = plugin.renderer().session(player);
        if (session == null) {
            sender.sendMessage(Component.text("表示中のモデルはありません。", NamedTextColor.GRAY));
            return 0;
        }
        if (!plugin.renderer().canDelete(player, session)) {
            sender.sendMessage(Component.text("このモデルを削除する権限がありません。",
                    NamedTextColor.RED));
            return 0;
        }
        int id = session.id();
        int removed = plugin.renderer().clear(player);
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
        sender.sendMessage(Component.text("モデルを削除しました (" + removed + " entities).",
                NamedTextColor.GREEN));
        return removed;
    }

    private int teleport(CommandSender sender, int id) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        try {
            plugin.renderer().teleport(player, id);
            sender.sendMessage(Component.text("モデル #" + id + " の近くに移動しました。",
                    NamedTextColor.GREEN));
            return 1;
        } catch (IllegalStateException e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            return 0;
        }
    }

    private int share(CommandSender sender, int id, boolean shared) {
        Player player = player(sender);
        if (player == null) {
            return 0;
        }
        try {
            plugin.renderer().setSharedEdit(player, id, shared);
            sender.sendMessage(Component.text("モデル #" + id + " の共有編集を "
                    + (shared ? "ON" : "OFF") + " にしました。", NamedTextColor.GREEN));
            return 1;
        } catch (IllegalStateException e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            return 0;
        }
    }

    private String ownerName(dev.taikin.mcplugins.cifviewer.render.ViewSession session) {
        if (session.owner() == null) {
            return "不明";
        }
        String name = Bukkit.getOfflinePlayer(session.owner()).getName();
        return name == null ? session.owner().toString() : name;
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
