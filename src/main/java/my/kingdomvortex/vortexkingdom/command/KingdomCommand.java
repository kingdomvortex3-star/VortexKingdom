
package my.kingdomvortex.vortexkingdom.command;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardType;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KingdomCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX =
            "§8[§6VortexKingdom§8] §7";

    private final VortexKingdom plugin;

    public KingdomCommand(VortexKingdom plugin) {
        this.plugin = plugin;
    }

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

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> sendHelp(sender);

            case "reload" -> reloadPlugin(sender);

            case "invitations", "invites" ->
                    openInvitations(sender);

            case "leaderboard", "top" ->
                    openLeaderboard(sender, args);

            default -> {
                sender.sendMessage(
                        PREFIX + "§cSubcommand tidak ditemukan."
                );
                sender.sendMessage(
                        PREFIX + "§7Gunakan §e/kingdom help"
                );
            }
        }

        return true;
    }

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

        plugin.getGuiManager().openMainMenu(player);
    }

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

    private void openLeaderboard(
            CommandSender sender,
            String[] args
    ) {
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

        LeaderboardType type = LeaderboardType.LEVEL;

        if (args.length >= 2) {
            type = LeaderboardType.fromString(args[1]);
        }

        plugin.getLeaderboardGUIManager().open(
                player,
                type,
                0
        );
    }

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

            sender.sendMessage(
                    PREFIX + "§aKonfigurasi berhasil dimuat ulang."
            );

            plugin.getLogger().info(
                    "VortexKingdom configuration reloaded."
            );

        } catch (Exception exception) {
            sender.sendMessage(
                    PREFIX + "§cGagal reload konfigurasi."
            );

            plugin.getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Failed to reload configuration.",
                    exception
            );
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§6§lVORTEX KINGDOM §8- §7Commands");
        sender.sendMessage(" ");
        sender.sendMessage("§e/kingdom §8- §7Buka menu Kingdom");
        sender.sendMessage("§e/kingdom invitations §8- §7Lihat undangan");
        sender.sendMessage("§e/kingdom leaderboard §8- §7Leaderboard Level");
        sender.sendMessage("§e/kingdom leaderboard level §8- §7Ranking Level");
        sender.sendMessage("§e/kingdom leaderboard experience §8- §7Ranking EXP");
        sender.sendMessage("§e/kingdom leaderboard members §8- §7Ranking Member");
        sender.sendMessage("§e/kingdom leaderboard kills §8- §7Ranking Kill");
        sender.sendMessage("§e/kingdom help §8- §7Bantuan");

        if (sender.hasPermission("vortexkingdom.admin")) {
            sender.sendMessage(
                    "§c/kingdom reload §8- §7Reload konfigurasi"
            );
        }

        sender.sendMessage(" ");
    }

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
            suggestions.add("leaderboard");

            if (sender.hasPermission("vortexkingdom.admin")) {
                suggestions.add("reload");
            }

        } else if (args.length == 2
                && args[0].equalsIgnoreCase("leaderboard")) {
            suggestions.add("level");
            suggestions.add("experience");
            suggestions.add("members");
            suggestions.add("kills");
        }

        String input = args[args.length - 1]
                .toLowerCase(Locale.ROOT);

        suggestions.removeIf(
                suggestion -> !suggestion.startsWith(input)
        );

        return suggestions;
    }
}
