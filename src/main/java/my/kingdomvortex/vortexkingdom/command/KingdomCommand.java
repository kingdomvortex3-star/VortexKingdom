
package my.kingdomvortex.vortexkingdom.command;

import my.kingdomvortex.vortexkingdom.VortexKingdom;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class KingdomCommand implements CommandExecutor, TabCompleter {

    private final VortexKingdom plugin;

    private static final String PREFIX =
            "§8[§6VortexKingdom§8] §7";

    public KingdomCommand(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // COMMAND EXECUTOR
    // ==========================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {
            openMainMenu(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "help" -> sendHelp(sender);

            case "reload" -> reloadPlugin(sender);

            case "invitations", "invite" -> openInvitations(sender);

            default -> {
                sender.sendMessage(PREFIX + "§cSubcommand tidak ditemukan.");
                sender.sendMessage(PREFIX + "§7Gunakan §e/kingdom help");
            }
        }

        return true;
    }

    // ==========================================
    // MAIN MENU
    // ==========================================

    private void openMainMenu(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    PREFIX + "§cCommand ini hanya bisa digunakan pemain."
            );
            return;
        }

        if (!player.hasPermission("vortexkingdom.use")) {
            player.sendMessage(
                    PREFIX + "§cKamu tidak memiliki izin menggunakan Kingdom."
            );
            return;
        }

        plugin.getGuiManager().openMain(player);
    }

    // ==========================================
    // INVITATIONS
    // ==========================================

    private void openInvitations(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    PREFIX + "§cCommand ini hanya bisa digunakan pemain."
            );
            return;
        }

        if (!player.hasPermission("vortexkingdom.use")) {
            player.sendMessage(
                    PREFIX + "§cKamu tidak memiliki izin menggunakan Kingdom."
            );
            return;
        }

        plugin.getGuiManager().openInvitations(player);
    }

    // ==========================================
    // RELOAD
    // ==========================================

    private void reloadPlugin(CommandSender sender) {

        if (!sender.hasPermission("vortexkingdom.admin")) {
            sender.sendMessage(
                    PREFIX + "§cKamu tidak memiliki izin untuk reload."
            );
            return;
        }

        try {
            plugin.reloadConfig();
            plugin.reloadExpConfig();

            sender.sendMessage(" ");
            sender.sendMessage("§6§lVORTEX KINGDOM");
            sender.sendMessage("§aConfiguration berhasil dimuat ulang.");
            sender.sendMessage("§7Config: §aReloaded");
            sender.sendMessage("§7EXP Config: §aReloaded");
            sender.sendMessage(" ");

            plugin.getLogger().info(
                    "Configuration successfully reloaded."
            );

        } catch (Exception exception) {
            sender.sendMessage(
                    PREFIX + "§cGagal melakukan reload konfigurasi."
            );

            plugin.getLogger().severe(
                    "Failed to reload VortexKingdom configuration."
            );

            exception.printStackTrace();
        }
    }

    // ==========================================
    // HELP
    // ==========================================

    private void sendHelp(CommandSender sender) {

        sender.sendMessage(" ");
        sender.sendMessage("§6§lVORTEX KINGDOM §8- §7Commands");
        sender.sendMessage(" ");
        sender.sendMessage("§e/kingdom §8- §7Buka menu Kingdom");
        sender.sendMessage("§e/kingdom invitations §8- §7Lihat undangan");
        sender.sendMessage("§e/kingdom help §8- §7Lihat bantuan");

        if (sender.hasPermission("vortexkingdom.admin")) {
            sender.sendMessage("§c/kingdom reload §8- §7Reload konfigurasi");
        }

        sender.sendMessage(" ");
    }

    // ==========================================
    // TAB COMPLETER
    // ==========================================

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("help");
            suggestions.add("invitations");

            if (sender.hasPermission("vortexkingdom.admin")) {
                suggestions.add("reload");
            }

            String input = args[0].toLowerCase();

            suggestions.removeIf(
                    suggestion -> !suggestion.startsWith(input)
            );
        }

        return suggestions;
    }
}
