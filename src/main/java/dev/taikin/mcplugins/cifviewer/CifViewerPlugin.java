package dev.taikin.mcplugins.cifviewer;

import dev.taikin.mcplugins.cifviewer.cod.CodClient;
import dev.taikin.mcplugins.cifviewer.gui.CifControlGui;
import dev.taikin.mcplugins.cifviewer.gui.CifModelListGui;
import dev.taikin.mcplugins.cifviewer.gui.CifRepeatGui;
import dev.taikin.mcplugins.cifviewer.mp.MaterialsProjectClient;
import dev.taikin.mcplugins.cifviewer.render.CrystalRenderer;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.packs.ResourcePack;
import org.bukkit.plugin.java.JavaPlugin;

public final class CifViewerPlugin extends JavaPlugin {

    private volatile CifConfig config;
    private CifRepository repository;
    private CrystalRenderer renderer;
    private MaterialsProjectClient materialsProject;
    private CifWand wand;
    private CifControlGui controlGui;
    private CifRepeatGui repeatGui;
    private CifModelListGui modelListGui;
    private CodClient cod;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            Files.createDirectories(getDataFolder().toPath().resolve("cif"));
        } catch (IOException e) {
            getLogger().warning("Could not create CIF directory: " + e.getMessage());
        }

        this.config = new CifConfig(getConfig(), getDataFolder(), getLogger());
        this.repository = new CifRepository(this);
        this.repository.reload();
        this.renderer = new CrystalRenderer(this);
        this.renderer.removeOrphans();
        getServer().getScheduler().runTaskLater(this, renderer::load, 20L);
        this.materialsProject = new MaterialsProjectClient(config);
        this.wand = new CifWand(this);
        this.controlGui = new CifControlGui(this);
        this.repeatGui = new CifRepeatGui(this);
        this.modelListGui = new CifModelListGui(this);
        this.cod = new CodClient(config, getDataFolder().toPath().resolve("cif"));

        CifCommand command = new CifCommand(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.node(), "View CIF crystal structures", List.of("cifview")));

        getServer().getPluginManager().registerEvents(new CifListener(this), this);

        getLogger().info("CifViewer enabled: " + repository.keys().size() + " CIF file(s).");
    }

    @Override
    public void onDisable() {
        if (renderer != null) {
            renderer.save();
            renderer.clearAll();
        }
        getLogger().info("CifViewer disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        this.config = new CifConfig(getConfig(), getDataFolder(), getLogger());
        this.repository.reload();
        this.materialsProject = new MaterialsProjectClient(config);
        this.cod = new CodClient(config, getDataFolder().toPath().resolve("cif"));
    }

    public boolean sendResourcePack(Player player) {
        ResourcePack pack = getServer().getServerResourcePack();
        if (pack == null || pack.getUrl() == null || pack.getUrl().isBlank()) {
            player.sendMessage(Component.text("サーバーのリソースパックが設定されていません。",
                    NamedTextColor.RED));
            return false;
        }
        player.setResourcePack(pack.getUrl(), pack.getHash(), false,
                Component.text("CIF Viewer 3D球体モデル"));
        player.sendMessage(Component.text("リソースパック: ", NamedTextColor.GOLD)
                .append(Component.text(pack.getUrl(), NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl(pack.getUrl()))
                        .hoverEvent(HoverEvent.showText(Component.text("クリックでダウンロード")))));
        player.sendMessage(Component.text(".minecraft/resourcepacks にzipを入れても適用できます。",
                NamedTextColor.GRAY));
        return true;
    }

    public CifConfig config() {
        return config;
    }

    public CifRepository repository() {
        return repository;
    }

    public CrystalRenderer renderer() {
        return renderer;
    }

    public MaterialsProjectClient materialsProject() {
        return materialsProject;
    }

    public CifWand wand() {
        return wand;
    }

    public CifControlGui controlGui() {
        return controlGui;
    }

    public CifRepeatGui repeatGui() {
        return repeatGui;
    }

    public CifModelListGui modelListGui() {
        return modelListGui;
    }

    public CodClient cod() {
        return cod;
    }
}
