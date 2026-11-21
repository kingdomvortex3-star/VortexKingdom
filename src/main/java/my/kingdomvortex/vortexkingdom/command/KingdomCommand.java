
package my.kingdomvortex.vortexkingdom.command;

import my.kingdomvortex.vortexkingdom.VortexKingdom;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class KingdomCommand implements CommandExecutor, TabCompleter {

    private final String prefix =
            ChatColor.DARK_GRAY + "[" +
            ChatColor.GOLD + "Kingdom" +
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
                    "You don't have permission.");
            return true;
        }

        if (args.length == 0) {
            VortexKingdom.getInstance()
                    .getGuiManager()
                    .openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "help" -> {
                player.sendMessage("");
                player.sendMessage("§6§lKINGDOM COMMANDS");
                player.sendMessage("§e/kingdom §7- Open Kingdom GUI");
                player.sendMessage("§e/kingdom help §7- Show commands");
                player.sendMessage("");
            }

            case "reload" -> {
                if (!player.hasPermission("vortexkingdom.admin")) {
                    player.sendMessage(prefix + ChatColor.RED +
                            "You don't have permission.");
                    return true;
                }

                VortexKingdom.getInstance().reloadConfig();
                VortexKingdom.getInstance().reloadExpConfig();

                player.sendMessage(prefix + ChatColor.GREEN +
                        "Configuration reloaded!");
            }

            default -> player.sendMessage(
                    prefix + "Use /kingdom to open the menu."
            );
        }

        return true;
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
