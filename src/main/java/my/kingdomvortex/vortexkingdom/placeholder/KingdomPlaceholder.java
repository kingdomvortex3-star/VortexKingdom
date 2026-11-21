
package my.kingdomvortex.vortexkingdom.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.Optional;

public final class KingdomPlaceholder extends PlaceholderExpansion {

    private final VortexKingdom plugin;

    public KingdomPlaceholder(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "vortexkingdom";
    }

    @Override
    public String getAuthor() {
        return "KingdomVortex";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(
            OfflinePlayer player,
            String params
    ) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        Optional<Kingdom> optional =
                plugin.getKingdomManager()
                        .getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            return getNoKingdomValue(params);
        }

        Kingdom kingdom = optional.get();

        int level = kingdom.getLevel();
        long currentExp = kingdom.getExperience();
        long totalExp = kingdom.getTotalExperience();

        return switch (params.toLowerCase(Locale.ROOT)) {

            case "name" ->
                    kingdom.getName();

            case "level" ->
                    String.valueOf(level);

            case "exp" ->
                    String.valueOf(currentExp);

            case "total_exp" ->
                    String.valueOf(totalExp);

            case "required_exp" ->
                    String.valueOf(
                            plugin.getLevelManager()
                                    .getRequiredExp(level)
                    );

            case "remaining_exp" ->
                    String.valueOf(
                            plugin.getLevelManager()
                                    .getRemainingExp(player.getUniqueId())
                    );

            case "progress" ->
                    String.format(
                            Locale.US,
                            "%.1f",
                            plugin.getLevelManager()
                                    .getProgress(player.getUniqueId())
                    );

            case "members" ->
                    String.valueOf(
                            plugin.getKingdomManager()
                                    .getMemberCount(kingdom.getId())
                    );

            case "max_members" ->
                    String.valueOf(
                            plugin.getKingdomManager()
                                    .getMaxMembers(kingdom.getId())
                    );

            case "kills" ->
                    String.valueOf(kingdom.getTotalKills());

            case "pvp" ->
                    kingdom.isPvpEnabled() ? "Enabled" : "Disabled";

            case "pvp_boolean" ->
                    String.valueOf(kingdom.isPvpEnabled());

            default -> null;
        };
    }

    private String getNoKingdomValue(String params) {
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "level", "exp", "total_exp",
                 "required_exp", "remaining_exp",
                 "progress", "members", "max_members",
                 "kills" -> "0";

            case "pvp_boolean" -> "false";

            case "pvp" -> "Disabled";

            case "name" -> "None";

            default -> null;
        };
    }
}
