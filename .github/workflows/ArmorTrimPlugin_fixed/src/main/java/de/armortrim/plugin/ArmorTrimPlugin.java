package de.armortrim.plugin;

import de.armortrim.plugin.commands.TrimsCommand;
import de.armortrim.plugin.gui.GuiManager;
import de.armortrim.plugin.manager.RankManager;
import de.armortrim.plugin.manager.TrimManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ArmorTrimPlugin extends JavaPlugin {

    private static ArmorTrimPlugin instance;
    private RankManager rankManager;
    private TrimManager trimManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        rankManager = new RankManager(this);
        trimManager = new TrimManager(this);
        guiManager  = new GuiManager(this);

        getServer().getPluginManager().registerEvents(guiManager, this);

        TrimsCommand cmd = new TrimsCommand(this);
        getCommand("trims").setExecutor(cmd);
        getCommand("trims").setTabCompleter(cmd);

        getLogger().info("ArmorTrimPlugin v2.0 gestartet!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ArmorTrimPlugin deaktiviert.");
    }

    public static ArmorTrimPlugin getInstance() { return instance; }
    public RankManager getRankManager()          { return rankManager; }
    public TrimManager getTrimManager()          { return trimManager; }
    public GuiManager  getGuiManager()           { return guiManager;  }
}
