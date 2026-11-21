
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
        if (player == null || source == null || source.isBlank()) {
            return 0;
        }

        String normalizedSource = source.trim()
                .toLowerCase(Locale.ROOT);

        String path = "sources." + normalizedSource + ".";

        if (!plugin.getExpConfig().getBoolean(
                path + "enabled", false
        )) {
            return 0;
        }

        long baseReward = resolveReward(
                normalizedSource,
                actionKey
        );

        if (baseReward <= 0) {
            return 0;
        }

        Optional<Kingdom> optional =
                plugin.getKingdomManager()
                        .getKingdomByPlayer(player);

        if (optional.isEmpty()) {
            return 0;
        }

        Kingdom kingdom = optional.get();

        if (kingdom.getLevel() >=
                plugin.getLevelManager().getMaxLevel()) {
            return 0;
        }

        int cooldown = Math.max(
                0,
                plugin.getExpConfig().getInt(
                        path + "cooldown", 0
                )
        );

        long dailyLimit = plugin.getExpConfig().getLong(
                "settings.daily-exp-limit", 10000
        );

        if (dailyLimit <= 0) {
            return 0;
        }

        long now = System.currentTimeMillis();

        try {
            ZoneId zone = ZoneId.of(
                    plugin.getExpConfig().getString(
                            "settings.timezone",
                            "Asia/Jakarta"
                    )
            );

            String today = LocalDate.now(zone).toString();

            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {
                boolean previousAutoCommit =
                        connection.getAutoCommit();

                long awarded = 0;
                boolean committed = false;

                try {
                    connection.setAutoCommit(false);

                    if (isOnCooldown(
                            connection,
                            player,
                            normalizedSource,
                            cooldown,
                            now
                    )) {
                        connection.rollback();
                        return 0;
                    }

                    long dailyEarned = getDailyExp(
                            connection,
                            player,
                            today
                    );

                    long remainingDaily = Math.max(
                            0,
                            dailyLimit - dailyEarned
                    );

                    if (remainingDaily <= 0) {
                        connection.rollback();
                        return 0;
                    }

                    long reward = Math.min(
                            baseReward,
                            remainingDaily
                    );

                    boolean antiFarming = plugin.getExpConfig()
                            .getBoolean(
                                    "anti-farming.enabled",
                                    true
                            );

                    if (antiFarming) {
                        int count = registerActivity(
                                connection,
                                player,
                                normalizedSource,
                                normalizeActionKey(actionKey),
                                now
                        );

                        int threshold = Math.max(
                                0,
                                plugin.getExpConfig().getInt(
                                        "anti-farming.repeat-threshold",
                                        5
                                )
                        );

                        int maxReduced = Math.max(
                                0,
                                plugin.getExpConfig().getInt(
                                        "anti-farming.maximum-reduced-rewards",
                                        10
                                )
                        );

                        double multiplier = plugin.getExpConfig()
                                .getDouble(
                                        "anti-farming.repeated-action-multiplier",
                                        0.25
                                );

                        multiplier = Math.max(
                                0,
                                Math.min(1, multiplier)
                        );

                        if (count > threshold + maxReduced) {
                            connection.rollback();
                            return 0;
                        }

                        if (count > threshold) {
                            reward = Math.max(
                                    1,
                                    (long) Math.floor(
                                            reward * multiplier
                                    )
                            );
                        }
                    }

                    reward = Math.min(
                            reward,
                            remainingDaily
                    );

                    reward = calculateAcceptableExp(
                            kingdom,
                            reward
                    );

                    if (reward <= 0) {
                        connection.rollback();
                        return 0;
                    }

                    /*
                     * Tambahkan EXP tanpa mengirim notifikasi.
                     * Notifikasi akan dijalankan setelah commit.
                     */
                    if (!plugin.getLevelManager().addExp(
                            player,
                            reward,
                            false
                    )) {
                        connection.rollback();
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

                    connection.commit();

                    committed = true;
                    awarded = reward;

                } catch (Exception exception) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackException) {
                        exception.addSuppressed(rollbackException);
                    }

                    throw exception;

                } finally {
                    connection.setAutoCommit(previousAutoCommit);
                }

                if (!committed || awarded <= 0) {
                    return 0;
                }

                /*
                 * Ambil level terbaru setelah commit.
                 * Notifikasi tidak dikirim jika transaksi gagal.
                 */
                Optional<Kingdom> updatedKingdom =
                        plugin.getKingdomManager()
                                .getKingdomByPlayer(player);

                if (updatedKingdom.isPresent()) {
                    int oldLevel = kingdom.getLevel();
                    int newLevel = updatedKingdom.get().getLevel();

                    if (newLevel > oldLevel) {
                        plugin.getLevelManager().announceLevelUps(
                                updatedKingdom.get().getName(),
                                oldLevel,
                                newLevel
                        );
                    }
                }

                return awarded;
            }

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Failed to process EXP reward for "
                            + player + ": "
                            + exception.getMessage()
            );

            exception.printStackTrace();
            return 0;
        }
    }

    private long resolveReward(
            String source,
            String actionKey
    ) {
        if (!source.equals("boss-kill")) {
            return plugin.getExpConfig().getLong(
                    "sources." + source + ".exp", 0
            );
        }

        if (!plugin.getExpConfig().getBoolean(
                "boss-rewards.enabled", true
        )) {
            return 0;
        }

        if (actionKey == null
                || !actionKey.toLowerCase(Locale.ROOT)
                        .startsWith("boss:")) {
            return 0;
        }

        String bossKey = actionKey.substring(5)
                .trim()
                .toLowerCase(Locale.ROOT);

        if (bossKey.isBlank()) {
            return 0;
        }

        String path = "boss-rewards." + bossKey + ".";

        if (!plugin.getExpConfig().getBoolean(
                path + "enabled", false
        )) {
            return 0;
        }

        return Math.max(
                0,
                plugin.getExpConfig().getLong(
                        path + "exp", 0
                )
        );
    }

    private long calculateAcceptableExp(
            Kingdom kingdom,
            long requested
    ) {
        if (requested <= 0) {
            return 0;
        }

        int level = kingdom.getLevel();
        int maxLevel = plugin.getLevelManager().getMaxLevel();

        if (level >= maxLevel) {
            return 0;
        }

        long currentExp = Math.max(
                0,
                kingdom.getExperience()
        );

        long accepted = 0;
        long remaining = requested;

        while (level < maxLevel && remaining > 0) {
            long required = plugin.getLevelManager()
                    .getRequiredExp(level);

            if (required <= 0) {
                break;
            }

            long needed = Math.max(
                    0,
                    required - currentExp
            );

            if (needed <= 0) {
                level++;
                currentExp = 0;
                continue;
            }

            long gain = Math.min(remaining, needed);

            accepted = saturatedAdd(accepted, gain);
            remaining -= gain;
            currentExp += gain;

            if (currentExp >= required) {
                level++;
                currentExp = 0;
            }
        }

        return accepted;
    }

    private long saturatedAdd(long first, long second) {
        if (second > 0 && first > Long.MAX_VALUE - second) {
            return Long.MAX_VALUE;
        }

        return first + second;
    }

    private String normalizeActionKey(String actionKey) {
        if (actionKey == null || actionKey.isBlank()) {
            return "default";
        }

        String key = actionKey.trim()
                .toLowerCase(Locale.ROOT);

        return key.length() > 128
                ? key.substring(0, 128)
                : key;
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
                WHERE player_uuid = ?
                  AND source = ?
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
                WHERE player_uuid = ?
                  AND date = ?
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

        int resetMinutes = Math.max(
                1,
                plugin.getExpConfig().getInt(
                        "anti-farming.reset-after-minutes",
                        30
                )
        );

        long resetMillis = resetMinutes * 60_000L;

        String sql = """
                INSERT INTO kingdom_exp_activity
                (
                    player_uuid,
                    source,
                    action_key,
                    action_count,
                    window_started,
                    last_reward_at
                )
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
            statement.setString(3, actionKey);
            statement.setLong(4, now);
            statement.setLong(5, now);
            statement.setLong(6, resetMillis);
            statement.setLong(7, resetMillis);

            statement.executeUpdate();
        }

        String selectSql = """
                SELECT action_count
                FROM kingdom_exp_activity
                WHERE player_uuid = ?
                  AND source = ?
                  AND action_key = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(selectSql)) {

            statement.setString(1, player.toString());
            statement.setString(2, source);
            statement.setString(3, actionKey);

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
                DO UPDATE SET
                    last_reward_at = excluded.last_reward_at
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
