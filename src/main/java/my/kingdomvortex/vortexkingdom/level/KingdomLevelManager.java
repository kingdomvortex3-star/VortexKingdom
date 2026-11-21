
package my.kingdomvortex.vortexkingdom.level;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class KingdomLevelManager {

    private final VortexKingdom plugin;

    public KingdomLevelManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    public long getRequiredExp(int level) {
        if (level < 1 || level >= getMaxLevel()) {
            return 0;
        }

        double base = plugin.getExpConfig().getDouble(
                "settings.base-required-exp", 1000
        );

        double growth = plugin.getExpConfig().getDouble(
                "settings.growth-rate", 1.18
        );

        if (!Double.isFinite(base) || !Double.isFinite(growth)
                || base < 1 || growth < 1) {
            return 0;
        }

        double required = base * Math.pow(growth, level - 1);

        if (!Double.isFinite(required) || required >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Math.max(1, Math.round(required));
    }

    public int getMaxLevel() {
        return Math.max(1, plugin.getExpConfig().getInt(
                "settings.maximum-level", 100
        ));
    }

    public long getCurrentExp(UUID player) {
        return getKingdom(player)
                .map(Kingdom::getExperience)
                .orElse(0L);
    }

    public long getTotalExp(UUID player) {
        return getKingdom(player)
                .map(Kingdom::getTotalExperience)
                .orElse(0L);
    }

    public double getProgress(UUID player) {
        Optional<Kingdom> optional = getKingdom(player);

        if (optional.isEmpty()) {
            return 0;
        }

        Kingdom kingdom = optional.get();

        if (kingdom.getLevel() >= getMaxLevel()) {
            return 100;
        }

        long required = getRequiredExp(kingdom.getLevel());

        if (required <= 0) {
            return 0;
        }

        return Math.max(0, Math.min(
                100,
                kingdom.getExperience() * 100.0 / required
        ));
    }

    public long getRemainingExp(UUID player) {
        Optional<Kingdom> optional = getKingdom(player);

        if (optional.isEmpty()) {
            return 0;
        }

        Kingdom kingdom = optional.get();

        if (kingdom.getLevel() >= getMaxLevel()) {
            return 0;
        }

        return Math.max(
                0,
                getRequiredExp(kingdom.getLevel())
                        - kingdom.getExperience()
        );
    }

    /**
     * Menambahkan EXP ke Kingdom.
     * Mendukung kenaikan beberapa level sekaligus.
     * Sisa EXP akan dibawa ke level berikutnya.
     *
     * Method ini tidak melakukan commit sendiri.
     * Transaksi dapat dikelola oleh ExpRewardManager.
     */
    public boolean addExp(UUID player, long amount) {
        if (player == null || amount <= 0) {
            return false;
        }

        Optional<Kingdom> optional = getKingdom(player);

        if (optional.isEmpty()) {
            return false;
        }

        Kingdom kingdom = optional.get();

        String selectSql = """
                SELECT level, experience, total_experience
                FROM kingdoms
                WHERE id = ?
                """;

        String updateSql = """
                UPDATE kingdoms
                SET level = ?,
                    experience = ?,
                    total_experience = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND level = ?
                  AND experience = ?
                  AND total_experience = ?
                """;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {
                int oldLevel;
                long oldExp;
                long oldTotalExp;

                try (PreparedStatement statement =
                             connection.prepareStatement(selectSql)) {

                    statement.setInt(1, kingdom.getId());

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            return false;
                        }

                        oldLevel = result.getInt("level");
                        oldExp = Math.max(
                                0,
                                result.getLong("experience")
                        );
                        oldTotalExp = Math.max(
                                0,
                                result.getLong("total_experience")
                        );
                    }
                }

                int maxLevel = getMaxLevel();

                if (oldLevel >= maxLevel) {
                    return false;
                }

                int newLevel = oldLevel;
                long newExp = oldExp;
                long remainingAmount = amount;
                long acceptedExp = 0;

                List<Integer> levelUps = new ArrayList<>();

                while (newLevel < maxLevel && remainingAmount > 0) {
                    long required = getRequiredExp(newLevel);

                    if (required <= 0) {
                        break;
                    }

                    long needed = Math.max(0, required - newExp);

                    if (needed <= 0) {
                        newLevel++;
                        newExp = 0;
                        levelUps.add(newLevel);
                        continue;
                    }

                    if (remainingAmount >= needed) {
                        remainingAmount -= needed;

                        acceptedExp = saturatedAdd(
                                acceptedExp,
                                needed
                        );

                        newLevel++;
                        newExp = 0;
                        levelUps.add(newLevel);
                    } else {
                        newExp = saturatedAdd(
                                newExp,
                                remainingAmount
                        );

                        acceptedExp = saturatedAdd(
                                acceptedExp,
                                remainingAmount
                        );

                        remainingAmount = 0;
                    }
                }

                if (acceptedExp <= 0) {
                    return false;
                }

                long newTotalExp = saturatedAdd(
                        oldTotalExp,
                        acceptedExp
                );

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setInt(1, newLevel);
                    statement.setLong(2, newExp);
                    statement.setLong(3, newTotalExp);
                    statement.setInt(4, kingdom.getId());
                    statement.setInt(5, oldLevel);
                    statement.setLong(6, oldExp);
                    statement.setLong(7, oldTotalExp);

                    if (statement.executeUpdate() <= 0) {
                        return false;
                    }
                }

                if (!levelUps.isEmpty()) {
                    handleLevelUps(kingdom.getName(), levelUps);
                }

                return true;
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to add Kingdom EXP: "
                            + exception.getMessage()
            );
            return false;
        }
    }

    public boolean setExp(UUID player, long amount) {
        if (player == null || amount < 0) {
            return false;
        }

        Optional<Kingdom> optional = getKingdom(player);

        if (optional.isEmpty()) {
            return false;
        }

        Kingdom kingdom = optional.get();

        String sql = """
                UPDATE kingdoms
                SET experience = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND level < ?
                """;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {
                int level;

                try (PreparedStatement select =
                             connection.prepareStatement(
                                     "SELECT level FROM kingdoms WHERE id = ?"
                             )) {

                    select.setInt(1, kingdom.getId());

                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next()) {
                            return false;
                        }

                        level = result.getInt("level");
                    }
                }

                long required = getRequiredExp(level);

                if (required <= 0 || amount >= required) {
                    return false;
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setLong(1, amount);
                    statement.setInt(2, kingdom.getId());
                    statement.setInt(3, getMaxLevel());

                    return statement.executeUpdate() > 0;
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to set Kingdom EXP: "
                            + exception.getMessage()
            );
            return false;
        }
    }

    public boolean setLevel(UUID player, int level) {
        if (player == null || level < 1 || level > getMaxLevel()) {
            return false;
        }

        Optional<Kingdom> optional = getKingdom(player);

        if (optional.isEmpty()) {
            return false;
        }

        Kingdom kingdom = optional.get();

        String sql = """
                UPDATE kingdoms
                SET level = ?,
                    experience = 0,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {
                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setInt(1, level);
                    statement.setInt(2, kingdom.getId());

                    return statement.executeUpdate() > 0;
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to set Kingdom level: "
                            + exception.getMessage()
            );
            return false;
        }
    }

    private Optional<Kingdom> getKingdom(UUID player) {
        return plugin.getKingdomManager()
                .getKingdomByPlayer(player);
    }

    private long saturatedAdd(long first, long second) {
        if (second > 0 && first > Long.MAX_VALUE - second) {
            return Long.MAX_VALUE;
        }

        return first + second;
    }

    private void handleLevelUps(String name, List<Integer> levels) {
        List<Integer> completedLevels = List.copyOf(levels);

        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> {
                    for (int level : completedLevels) {
                        plugin.getServer().broadcastMessage(
                                "§8[§6Kingdom§8] §e" + name
                                        + " §7has reached §6Level "
                                        + level + "§7!"
                        );
                    }
                }
        );
    }
}
