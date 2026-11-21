
package my.kingdomvortex.vortexkingdom;

import my.kingdomvortex.vortexkingdom.command.KingdomCommand;
import my.kingdomvortex.vortexkingdom.database.DatabaseManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomLeaderboardGUIManager;
import my.kingdomvortex.vortexkingdom.level.ExpRewardManager;
import my.kingdomvortex.vortexkingdom.level.KingdomLevelManager;
import my.kingdomvortex.vortexkingdom.listener.KingdomExpListener;
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
import java.util.logging.Level;

public final class VortexKingdom extends JavaPlugin {

    private static VortexKingdom instance;

    private DatabaseManager databaseManager;
    private KingdomManager kingdomManager;
    private KingdomLevelManager levelManager;
    private ExpRewardManager expRewardManager;
    private KingdomLeaderboardManager leaderboardManager;

    private KingdomGUIManager guiManager;
    private KingdomLeaderboardGUIManager leaderboardGUIManager;

    private KingdomPlaceholder placeholder;

    private FileConfiguration expConfig;
    private File expFile;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadExpConfig();

        try {
            initializeDatabase();
            initializeManagers();
            registerListeners();
            registerCommands();
            registerPlaceholder();

            getLogger().info("==================================");
            getLogger().info("          VORTEX KINGDOM");
            getLogger().info("==================================");
            getLogger().info("Database: Connected");
            getLogger().info("Kingdom Manager: Loaded");
            getLogger().info("Level Manager: Loaded");
            getLogger().info("EXP Reward Manager: Loaded");
            getLogger().info("Leaderboard Manager: Loaded");
            getLogger().info("GUI Manager: Loaded");
            getLogger().info("Listeners: Registered");
            getLogger().info(
                    "PlaceholderAPI: "
                            + (placeholder != null
                            ? "Registered" : "Not Installed")
            );
            getLogger().info("==================================");

        } catch (Exception exception) {
            getLogger().log(
                    Level.SEVERE,
                    "Failed to initialize VortexKingdom!",
                    exception
            );

            if (databaseManager != null) {
                databaseManager.disconnect();
            }

            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeDatabase() {
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        if (!databaseManager.isConnected()) {
            throw new IllegalStateException(
                    "SQLite database connection failed."
            );
        }
    }

    private void initializeManagers() {
        kingdomManager = new KingdomManager(
                this,
                databaseManager
        );

        levelManager = new KingdomLevelManager(this);
        expRewardManager = new ExpRewardManager(this);
        leaderboardManager = new KingdomLeaderboardManager(this);

        guiManager = new KingdomGUIManager(this);
        leaderboardGUIManager =
                new KingdomLeaderboardGUIManager(this);
    }

    private void registerListeners() {
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
        if (!getServer().getPluginManager()
                .isPluginEnabled("PlaceholderAPI")) {
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

    private void loadExpConfig() {
        expFile = new File(getDataFolder(), "exp.yml");

        if (!expFile.exists()) {
            if (!getDataFolder().exists()
                    && !getDataFolder().mkdirs()) {
                throw new IllegalStateException(
                        "Could not create plugin data folder."
                );
            }

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

    @Override
    public void onDisable() {
        if (placeholder != null) {
            placeholder.unregister();
            placeholder = null;
        }

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        instance = null;

        getLogger().info("VortexKingdom disabled.");
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

    public KingdomGUIManager getGuiManager() {
        return guiManager;
    }

    public KingdomLeaderboardGUIManager getLeaderboardGUIManager() {
        return leaderboardGUIManager;
    }

    public FileConfiguration getExpConfig() {
        return expConfig;
    }
}
