
package my.kingdomvortex.vortexkingdom.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;

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

        if (params == null || params.isBlank()) {
            return null;
        }

        String key = params.toLowerCase(Locale.ROOT);

        Optional<Kingdom> optional =
                plugin.getKingdomManager()
                        .getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            return getNoKingdomValue(key);
        }

        Kingdom kingdom = optional.get();

        int level = kingdom.getLevel();
        long currentExp = kingdom.getExperience();
        long totalExp = kingdom.getTotalExperience();

        Optional<KingdomMember> member =
                plugin.getKingdomManager()
                        .getMember(player.getUniqueId());

        return switch (key) {

            // ==========================================
            // KINGDOM IDENTITY
            // ==========================================

            case "name" ->
                    kingdom.getName();

            case "id" ->
                    String.valueOf(kingdom.getId());

            case "owner" ->
                    kingdom.getOwnerUuid();

            case "role" ->
                    member.map(value -> value.getRole().name())
                            .orElse("NONE");

            // ==========================================
            // LEVEL AND EXPERIENCE
            // ==========================================

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

            // ==========================================
            // MEMBERS
            // ==========================================

            case "members" ->
                    String.valueOf(
                            plugin.getKingdomManager()
                                    .getMemberCount(kingdom.getId())
                    );

            case "max_members" ->
                    String.valueOf(
                            plugin.getKingdomManager()
                                    .getMaxMembers(level)
                    );

            case "members_left" ->
                    String.valueOf(
                            Math.max(
                                    0,
                                    plugin.getKingdomManager()
                                            .getMaxMembers(level)
                                            - plugin.getKingdomManager()
                                            .getMemberCount(kingdom.getId())
                            )
                    );

            // ==========================================
            // STATISTICS
            // ==========================================

            case "kills" ->
                    String.valueOf(kingdom.getTotalKills());

            // ==========================================
            // KINGDOM SETTINGS
            // ==========================================

            case "pvp" ->
                    kingdom.isPvpEnabled()
                            ? "Enabled"
                            : "Disabled";

            case "pvp_boolean" ->
                    String.valueOf(kingdom.isPvpEnabled());

            default -> null;
        };
    }

    private String getNoKingdomValue(String params) {
        return switch (params) {

            case "level",
                 "exp",
                 "total_exp",
                 "required_exp",
                 "remaining_exp",
                 "progress",
                 "members",
                 "max_members",
                 "members_left",
                 "kills",
                 "id" -> "0";

            case "pvp_boolean" -> "false";

            case "pvp" -> "Disabled";

            case "name",
                 "owner" -> "None";

            case "role" -> "NONE";

            default -> null;
        };
    }
}
