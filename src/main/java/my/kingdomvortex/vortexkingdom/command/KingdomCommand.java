
package my.kingdomvortex.vortexkingdom.command;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class KingdomCommand implements CommandExecutor, TabCompleter {

    private final String prefix =
            ChatColor.DARK_GRAY + "[" +
            ChatColor.AQUA + "Kingdom" +
            ChatColor.DARK_GRAY + "] " +
            ChatColor.GRAY;

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("vortexkingdom.use")) {
            player.sendMessage(prefix + ChatColor.RED +
                    "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            openKingdomMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "help" -> sendHelp(player);

            case "reload" -> {
                if (!player.hasPermission("vortexkingdom.admin")) {
                    player.sendMessage(prefix + ChatColor.RED +
                            "You don't have permission.");
                    return true;
                }

                VortexKingdom.getInstance().reloadConfig();

                player.sendMessage(prefix + ChatColor.GREEN +
                        "Configuration reloaded!");
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void openKingdomMenu(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD.toString() +
                ChatColor.BOLD + "       KINGDOM SYSTEM");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY +
                "  Kingdom GUI is being prepared.");
        player.sendMessage(ChatColor.GRAY +
                "  All kingdom features will be accessible");
        player.sendMessage(ChatColor.GRAY +
                "  through this menu.");
        player.sendMessage("");
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD.toString() +
                ChatColor.BOLD + "       KINGDOM COMMANDS");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW +
                "/kingdom " + ChatColor.GRAY + "- Open Kingdom GUI");
        player.sendMessage(ChatColor.YELLOW +
                "/kingdom help " + ChatColor.GRAY + "- Show commands");
        player.sendMessage(ChatColor.YELLOW +
                "/kingdom reload " + ChatColor.GRAY + "- Reload config");
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1 &&
                sender.hasPermission("vortexkingdom.admin")) {
            return List.of("help", "reload");
        }

        return List.of();
    }
}
