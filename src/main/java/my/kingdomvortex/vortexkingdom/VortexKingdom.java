
package my.kingdomvortex.vortexkingdom;

import my.kingdomvortex.vortexkingdom.command.KingdomCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class VortexKingdom extends JavaPlugin {

    private static VortexKingdom instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        registerCommands();

        getLogger().info("==================================");
        getLogger().info("       VORTEX KINGDOM");
        getLogger().info("==================================");
        getLogger().info("Version: " + getPluginMeta().getVersion());
        getLogger().info("Kingdom System enabled!");
        getLogger().info("==================================");
    }

    private void registerCommands() {
        PluginCommand command = getCommand("kingdom");

        if (command == null) {
            getLogger().severe("Command 'kingdom' is missing from plugin.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        KingdomCommand executor = new KingdomCommand();

        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public void onDisable() {
        getLogger().info("VortexKingdom disabled.");
        instance = null;
    }

    public static VortexKingdom getInstance() {
        return instance;
    }
}
