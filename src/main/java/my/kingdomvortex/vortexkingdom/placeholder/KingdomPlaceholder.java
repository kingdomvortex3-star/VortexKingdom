
package my.kingdomvortex.vortexkingdom.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardEntry;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardType;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null || params.isBlank()) {
            return "";
        }

        String key = params.toLowerCase(Locale.ROOT);

        // Global leaderboard placeholders.
        // These do not require the requesting player to have a Kingdom.
        if (key.startsWith("top_")) {
            return getLeaderboardPlaceholder(key);
        }

        if (player == null) {
            return "";
        }

        Optional<Kingdom> optional = plugin.getKingdomManager()
                .getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            return getNoKingdomValue(key);
        }

        Kingdom kingdom = optional.get();

        int level = kingdom.getLevel();

        int members = plugin.getKingdomManager()
                .getMemberCount(kingdom.getId());

        int maxMembers = plugin.getKingdomManager()
                .getMaxMembers(level);

        Optional<KingdomMember> member = plugin.getKingdomManager()
                .getMember(player.getUniqueId());

        return switch (key) {
            case "name" -> kingdom.getName();

            case "id" -> String.valueOf(kingdom.getId());

            case "owner" -> getPlayerName(kingdom.getOwnerUuid());

            case "owner_uuid" -> kingdom.getOwnerUuid();

            case "role" -> member
                    .map(value -> value.getRole().name())
                    .orElse("NONE");

            case "level" -> String.valueOf(level);

            case "exp" -> String.valueOf(kingdom.getExperience());

            case "total_exp" ->
                    String.valueOf(kingdom.getTotalExperience());

            case "required_exp" -> String.valueOf(
                    plugin.getLevelManager().getRequiredExp(level)
            );

            case "remaining_exp" -> String.valueOf(
                    plugin.getLevelManager()
                            .getRemainingExp(player.getUniqueId())
            );

            case "progress" -> String.format(
                    Locale.US,
                    "%.1f",
                    plugin.getLevelManager()
                            .getProgress(player.getUniqueId())
            );

            case "members" -> String.valueOf(members);

            case "max_members" -> String.valueOf(maxMembers);

            case "members_left" ->
                    String.valueOf(Math.max(0, maxMembers - members));

            case "kills" -> String.valueOf(kingdom.getTotalKills());

            case "pvp" -> kingdom.isPvpEnabled()
                    ? "Enabled"
                    : "Disabled";

            case "pvp_boolean" ->
                    String.valueOf(kingdom.isPvpEnabled());

            case "rank_level" -> String.valueOf(
                    plugin.getLeaderboardManager().getRank(
                            kingdom.getId(),
                            LeaderboardType.LEVEL
                    )
            );

            case "rank_exp" -> String.valueOf(
                    plugin.getLeaderboardManager().getRank(
                            kingdom.getId(),
                            LeaderboardType.EXPERIENCE
                    )
            );

            case "rank_members" -> String.valueOf(
                    plugin.getLeaderboardManager().getRank(
                            kingdom.getId(),
                            LeaderboardType.MEMBERS
                    )
            );

            case "rank_kills" -> String.valueOf(
                    plugin.getLeaderboardManager().getRank(
                            kingdom.getId(),
                            LeaderboardType.KILLS
                    )
            );

            default -> null;
        };
    }

    private String getLeaderboardPlaceholder(String key) {
        String[] parts = key.split("_");

        if (parts.length != 4 || !parts[0].equals("top")) {
            return null;
        }

        LeaderboardType type;

        switch (parts[1]) {
            case "level" -> type = LeaderboardType.LEVEL;
            case "exp", "experience" ->
                    type = LeaderboardType.EXPERIENCE;
            case "members" -> type = LeaderboardType.MEMBERS;
            case "kills" -> type = LeaderboardType.KILLS;
            default -> {
                return null;
            }
        }

        String field = parts[2];

        int position;

        try {
            position = Integer.parseInt(parts[3]);
        } catch (NumberFormatException exception) {
            return null;
        }

        if (position < 1 || position > 100) {
            return null;
        }

        List<LeaderboardEntry> entries =
                plugin.getLeaderboardManager().getTop(type, position);

        if (entries.size() < position) {
            return field.equals("rank") ? "0" : "None";
        }

        LeaderboardEntry entry = entries.get(position - 1);

        return switch (field) {
            case "name" -> entry.kingdomName();

            case "id" -> String.valueOf(entry.kingdomId());

            case "owner" -> getPlayerName(entry.ownerUuid());

            case "owner_uuid" -> entry.ownerUuid();

            case "level" -> String.valueOf(entry.level());

            case "exp", "total_exp" ->
                    String.valueOf(entry.totalExperience());

            case "members" -> String.valueOf(entry.memberCount());

            case "kills" -> String.valueOf(entry.totalKills());

            case "rank" -> String.valueOf(entry.position());

            case "score" -> String.valueOf(entry.score());

            default -> null;
        };
    }

    private String getPlayerName(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            String name = offlinePlayer.getName();

            return name != null ? name : uuidString;
        } catch (IllegalArgumentException exception) {
            return uuidString;
        }
    }

    private String getNoKingdomValue(String key) {
        return switch (key) {
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
                 "id",
                 "rank_level",
                 "rank_exp",
                 "rank_members",
                 "rank_kills" -> "0";

            case "pvp_boolean" -> "false";

            case "pvp" -> "Disabled";

            case "name",
                 "owner",
                 "owner_uuid" -> "None";

            case "role" -> "NONE";

            default -> null;
        };
    }
}
