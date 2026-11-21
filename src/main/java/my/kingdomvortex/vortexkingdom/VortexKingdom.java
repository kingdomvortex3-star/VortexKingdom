
package my.kingdomvortex.vortexkingdom;

import my.kingdomvortex.vortexkingdom.command.KingdomCommand;
import my.kingdomvortex.vortexkingdom.database.DatabaseManager;
import my.kingdomvortex.vortexkingdom.feature.KingdomFeatureManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomFeatureGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomLeaderboardGUIManager;
import my.kingdomvortex.vortexkingdom.level.ExpRewardManager;
import my.kingdomvortex.vortexkingdom.level.KingdomLevelManager;
import my.kingdomvortex.vortexkingdom.listener.KingdomExpListener;
import my.kingdomvortex.vortexkingdom.listener.KingdomFeatureListener;
import my.kingdomvortex.vortexkingdom.listener.KingdomGUIListener;
import my.kingdomvortex.vortexkingdom.listener.KingdomLeaderboardGUIListener;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager;
import my.kingdomvortex.vortexkingdom.placeholder.KingdomPlaceholder;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class VortexKingdom extends JavaPlugin {

    private static VortexKingdom instance;

    private DatabaseManager databaseManager;
    private KingdomManager kingdomManager;
    private KingdomLevelManager levelManager;
    private ExpRewardManager expRewardManager;
    private KingdomLeaderboardManager leaderboardManager;
    private KingdomFeatureManager featureManager;

    private KingdomGUIManager guiManager;
    private KingdomLeaderboardGUIManager leaderboardGUIManager;
    private KingdomFeatureGUIManager featureGUIManager;

    private KingdomPlaceholder placeholder;

    private FileConfiguration expConfig;
    private File expFile;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadExpConfig();

        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.connect();

            kingdomManager = new KingdomManager(this, databaseManager);
            levelManager = new KingdomLevelManager(this);
            expRewardManager = new ExpRewardManager(this);
            leaderboardManager = new KingdomLeaderboardManager(this);

            featureManager = new KingdomFeatureManager(this);

            guiManager = new KingdomGUIManager(this);
            leaderboardGUIManager =
                    new KingdomLeaderboardGUIManager(this);
            featureGUIManager =
                    new KingdomFeatureGUIManager(this);

            getServer().getPluginManager().registerEvents(
                    new KingdomGUIListener(this, guiManager),
                    this
            );

            getServer().getPluginManager().registerEvents(
                    new KingdomExpListener(this),
                    this
            );

            getServer().getPluginManager().registerEvents(
                    new KingdomLeaderboardGUIListener(this),
                    this
            );

            getServer().getPluginManager().registerEvents(
                    new KingdomFeatureListener(this, featureGUIManager),
                    this
            );

            registerCommands();
            registerPlaceholder();

        } catch (Exception exception) {
            getLogger().severe("Failed to initialize VortexKingdom!");
            exception.printStackTrace();

            if (databaseManager != null) {
                databaseManager.disconnect();
            }

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("==================================");
        getLogger().info("          VORTEX KINGDOM");
        getLogger().info("==================================");
        getLogger().info("Database: Connected");
        getLogger().info("Kingdom Manager: Loaded");
        getLogger().info("Level Manager: Loaded");
        getLogger().info("EXP Reward Manager: Loaded");
        getLogger().info("Leaderboard Manager: Loaded");
        getLogger().info("Feature Manager: Loaded");
        getLogger().info("GUI Managers: Loaded");
        getLogger().info("Listeners: Registered");
        getLogger().info(
                "PlaceholderAPI: "
                        + (placeholder != null ? "Registered" : "Not Installed")
        );
        getLogger().info("==================================");
    }

    private void loadExpConfig() {
        expFile = new File(getDataFolder(), "exp.yml");

        if (!expFile.exists()) {
            saveResource("exp.yml", false);
        }

        expConfig = YamlConfiguration.loadConfiguration(expFile);
    }

    public void reloadExpConfig() {
        if (expFile == null) {
            loadExpConfig();
            return;
        }

        expConfig = YamlConfiguration.loadConfiguration(expFile);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("kingdom");

        if (command == null) {
            throw new IllegalStateException(
                    "Missing kingdom command in plugin.yml"
            );
        }

        KingdomCommand executor = new KingdomCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerPlaceholder() {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info(
                    "PlaceholderAPI not found. Placeholder support disabled."
            );
            return;
        }

        placeholder = new KingdomPlaceholder(this);

        if (!placeholder.register()) {
            placeholder = null;
            getLogger().warning(
                    "Failed to register Kingdom PlaceholderAPI expansion."
            );
        }
    }

    @Override
    public void onDisable() {
        if (placeholder != null) {
            placeholder.unregister();
            placeholder = null;
        }

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("VortexKingdom disabled.");
        instance = null;
    }

    public static VortexKingdom getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public KingdomManager getKingdomManager() {
        return kingdomManager;
    }

    public KingdomLevelManager getLevelManager() {
        return levelManager;
    }

    public ExpRewardManager getExpRewardManager() {
        return expRewardManager;
    }

    public KingdomLeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public KingdomFeatureManager getFeatureManager() {
        return featureManager;
    }

    public KingdomGUIManager getGuiManager() {
        return guiManager;
    }

    public KingdomLeaderboardGUIManager getLeaderboardGUIManager() {
        return leaderboardGUIManager;
    }

    public KingdomFeatureGUIManager getFeatureGUIManager() {
        return featureGUIManager;
    }

    public FileConfiguration getExpConfig() {
        return expConfig;
    }
}
