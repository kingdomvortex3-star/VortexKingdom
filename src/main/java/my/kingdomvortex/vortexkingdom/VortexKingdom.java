
package my.kingdomvortex.vortexkingdom;

import my.kingdomvortex.vortexkingdom.command.KingdomCommand;
import my.kingdomvortex.vortexkingdom.database.DatabaseManager;
import my.kingdomvortex.vortexkingdom.level.KingdomLevelManager;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class VortexKingdom extends JavaPlugin {

    private static VortexKingdom instance;

    private DatabaseManager databaseManager;
    private KingdomManager kingdomManager;
    private KingdomLevelManager levelManager;

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

            kingdomManager = new KingdomManager(
                    this,
                    databaseManager
            );

            levelManager = new KingdomLevelManager(this);

            registerCommands();

        } catch (Exception exception) {
            getLogger().severe(
                    "Failed to initialize VortexKingdom!"
            );

            exception.printStackTrace();

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("==================================");
        getLogger().info("       VORTEX KINGDOM");
        getLogger().info("==================================");
        getLogger().info("Version: " + getPluginMeta().getVersion());
        getLogger().info("Database: Connected");
        getLogger().info("Kingdom Manager: Loaded");
        getLogger().info("Level Manager: Loaded");
        getLogger().info("EXP Configuration: Loaded");
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
        expConfig = YamlConfiguration.loadConfiguration(expFile);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("kingdom");

        if (command == null) {
            throw new IllegalStateException(
                    "Command 'kingdom' is missing from plugin.yml!"
            );
        }

        KingdomCommand executor = new KingdomCommand();

        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public void onDisable() {
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

    public FileConfiguration getExpConfig() {
        return expConfig;
    }
}
