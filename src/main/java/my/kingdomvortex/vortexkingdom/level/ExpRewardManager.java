
package my.kingdomvortex.vortexkingdom.level;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ExpRewardManager {

    private final VortexKingdom plugin;

    public ExpRewardManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    public synchronized long award(
            UUID player,
            String source,
            String actionKey
    ) {
        String normalizedSource = source.toLowerCase(Locale.ROOT);

        String path = "sources." + normalizedSource + ".";

        if (!plugin.getExpConfig().getBoolean(
                path + "enabled", false
        )) {
            return 0;
        }

        long baseReward = plugin.getExpConfig().getLong(
                path + "exp", 0
        );

        if (baseReward <= 0) {
            return 0;
        }

        Optional<Kingdom> optional =
                plugin.getKingdomManager().getKingdomByPlayer(player);

        if (optional.isEmpty()) {
            return 0;
        }

        Kingdom kingdom = optional.get();

        if (kingdom.getLevel() >=
                plugin.getLevelManager().getMaxLevel()) {
            return 0;
        }

        long now = System.currentTimeMillis();

        int cooldown = plugin.getExpConfig().getInt(
                path + "cooldown", 0
        );

        int dailyLimit = plugin.getExpConfig().getInt(
                "settings.daily-exp-limit", 10000
        );

        if (dailyLimit <= 0) {
            return 0;
        }

        long reward = baseReward;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            if (isOnCooldown(connection, player, normalizedSource,
                    cooldown, now)) {
                return 0;
            }

            ZoneId zone = ZoneId.of(
                    plugin.getExpConfig().getString(
                            "settings.timezone", "Asia/Jakarta"
                    )
            );

            String today = LocalDate.now(zone).toString();

            long dailyEarned = getDailyExp(
                    connection, player, today
            );

            long remainingDaily = Math.max(
                    0, dailyLimit - dailyEarned
            );

            if (remainingDaily <= 0) {
                return 0;
            }

            boolean antiFarming = plugin.getExpConfig()
                    .getBoolean("anti-farming.enabled", true);

            if (antiFarming) {
                int count = registerActivity(
                        connection,
                        player,
                        normalizedSource,
                        actionKey,
                        now
                );

                int threshold = plugin.getExpConfig().getInt(
                        "anti-farming.repeat-threshold", 5
                );

                int maxReduced = plugin.getExpConfig().getInt(
                        "anti-farming.maximum-reduced-rewards", 10
                );

                double multiplier = plugin.getExpConfig().getDouble(
                        "anti-farming.repeated-action-multiplier", 0.25
                );

                if (count > threshold + maxReduced) {
                    return 0;
                }

                if (count > threshold) {
                    reward = Math.max(
                            1,
                            (long) Math.floor(reward * multiplier)
                    );
                }
            }

            reward = Math.min(reward, remainingDaily);

            if (reward <= 0) {
                return 0;
            }

            if (!plugin.getLevelManager().addExp(player, reward)) {
                return 0;
            }

            saveDailyExp(
                    connection,
                    player,
                    today,
                    reward
            );

            updateCooldown(
                    connection,
                    player,
                    normalizedSource,
                    now
            );

            return reward;

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "EXP reward error: " + exception.getMessage()
            );

            return 0;
        }
    }

    private boolean isOnCooldown(
            Connection connection,
            UUID player,
            String source,
            int cooldownSeconds,
            long now
    ) throws SQLException {

        if (cooldownSeconds <= 0) {
            return false;
        }

        String sql = """
                SELECT last_reward_at
                FROM kingdom_exp_source_cooldowns
                WHERE player_uuid = ? AND source = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, player.toString());
            statement.setString(2, source);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return false;
                }

                long last = result.getLong("last_reward_at");

                return now - last < cooldownSeconds * 1000L;
            }
        }
    }

    private long getDailyExp(
            Connection connection,
            UUID player,
            String date
    ) throws SQLException {

        String sql = """
                SELECT experience
                FROM kingdom_daily_exp
                WHERE player_uuid = ? AND date = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, player.toString());
            statement.setString(2, date);

            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? result.getLong("experience")
                        : 0;
            }
        }
    }

    private int registerActivity(
            Connection connection,
            UUID player,
            String source,
            String actionKey,
            long now
    ) throws SQLException {

        String key = actionKey == null || actionKey.isBlank()
                ? "default"
                : actionKey;

        int resetMinutes = plugin.getExpConfig().getInt(
                "anti-farming.reset-after-minutes", 30
        );

        long resetMillis = Math.max(1, resetMinutes) * 60_000L;

        String sql = """
                INSERT INTO kingdom_exp_activity
                (player_uuid, source, action_key, action_count,
                 window_started, last_reward_at)
                VALUES (?, ?, ?, 1, ?, ?)
                ON CONFLICT(player_uuid, source, action_key)
                DO UPDATE SET
                    action_count = CASE
                        WHEN excluded.window_started -
                             kingdom_exp_activity.window_started >= ?
                        THEN 1
                        ELSE kingdom_exp_activity.action_count + 1
                    END,
                    window_started = CASE
                        WHEN excluded.window_started -
                             kingdom_exp_activity.window_started >= ?
                        THEN excluded.window_started
                        ELSE kingdom_exp_activity.window_started
                    END,
                    last_reward_at = excluded.last_reward_at
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, player.toString());
            statement.setString(2, source);
            statement.setString(3, key);
            statement.setLong(4, now);
            statement.setLong(5, now);
            statement.setLong(6, resetMillis);
            statement.setLong(7, resetMillis);

            statement.executeUpdate();
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             """
                             SELECT action_count
                             FROM kingdom_exp_activity
                             WHERE player_uuid = ?
                             AND source = ?
                             AND action_key = ?
                             """
                     )) {

            statement.setString(1, player.toString());
            statement.setString(2, source);
            statement.setString(3, key);

            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? result.getInt("action_count")
                        : 1;
            }
        }
    }

    private void saveDailyExp(
            Connection connection,
            UUID player,
            String date,
            long amount
    ) throws SQLException {

        String sql = """
                INSERT INTO kingdom_daily_exp
                (player_uuid, date, experience)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid, date)
                DO UPDATE SET
                    experience = experience + excluded.experience
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, player.toString());
            statement.setString(2, date);
            statement.setLong(3, amount);
            statement.executeUpdate();
        }
    }

    private void updateCooldown(
            Connection connection,
            UUID player,
            String source,
            long now
    ) throws SQLException {

        String sql = """
                INSERT INTO kingdom_exp_source_cooldowns
                (player_uuid, source, last_reward_at)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid, source)
                DO UPDATE SET last_reward_at = excluded.last_reward_at
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, player.toString());
            statement.setString(2, source);
            statement.setLong(3, now);
            statement.executeUpdate();
        }
    }
}
