
package my.kingdomvortex.vortexkingdom.level;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import java.sql.Connection;
import java.sql.PreparedStatement;
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

        if (base < 1 || growth <= 1) {
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

    /**
     * Menambahkan EXP ke Kingdom.
     * Mendukung kenaikan beberapa level sekaligus.
     * Sisa EXP akan dibawa ke level berikutnya.
     */
    public synchronized boolean addExp(UUID player, long amount) {
        if (amount <= 0) {
            return false;
        }

        Optional<Kingdom> optional = getKingdom(player);

        if (optional.isEmpty()) {
            return false;
        }

        Kingdom kingdom = optional.get();

        int oldLevel = kingdom.getLevel();
        int newLevel = oldLevel;
        int maxLevel = getMaxLevel();

        if (oldLevel >= maxLevel) {
            return false;
        }

        long oldExp = Math.max(0, kingdom.getExperience());
        long newExp = oldExp;
        long remainingAmount = amount;
        long acceptedExp = 0;

        List<Integer> levelUps = new ArrayList<>();

        while (newLevel < maxLevel && remainingAmount > 0) {

            long required = getRequiredExp(newLevel);

            if (required <= 0) {
                break;
            }

            long needed = required - newExp;

            if (needed <= 0) {
                newLevel++;
                newExp = 0;
                levelUps.add(newLevel);
                continue;
            }

            if (remainingAmount >= needed) {
                remainingAmount -= needed;
                acceptedExp += needed;

                newLevel++;
                newExp = 0;

                levelUps.add(newLevel);

            } else {
                newExp += remainingAmount;
                acceptedExp += remainingAmount;
                remainingAmount = 0;
            }
        }

        if (acceptedExp <= 0) {
            return false;
        }

        long oldTotalExp = Math.max(0, kingdom.getTotalExperience());
        long newTotalExp = saturatedAdd(oldTotalExp, acceptedExp);

        String sql = """
                UPDATE kingdoms
                SET level = ?,
                    experience = ?,
                    total_experience = ?
                WHERE id = ?
                  AND level = ?
                  AND experience = ?
                """;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, newLevel);
                statement.setLong(2, newExp);
                statement.setLong(3, newTotalExp);
                statement.setInt(4, kingdom.getId());
                statement.setInt(5, oldLevel);
                statement.setLong(6, kingdom.getExperience());

                if (statement.executeUpdate() <= 0) {
                    return false;
                }
            }

            if (!levelUps.isEmpty()) {
                handleLevelUps(kingdom.getName(), levelUps);
            }

            return true;

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to add Kingdom EXP: "
                            + exception.getMessage()
            );
            return false;
        }
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

    public synchronized boolean setExp(UUID player, long amount) {
        if (amount < 0) {
            return false;
        }

        Optional<Kingdom> optional = getKingdom(player);

        if (optional.isEmpty()) {
            return false;
        }

        Kingdom kingdom = optional.get();

        if (kingdom.getLevel() >= getMaxLevel()) {
            return false;
        }

        long required = getRequiredExp(kingdom.getLevel());

        if (required <= 0 || amount >= required) {
            return false;
        }

        String sql = """
                UPDATE kingdoms
                SET experience = ?
                WHERE id = ?
                  AND level = ?
                  AND experience = ?
                """;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setLong(1, amount);
                statement.setInt(2, kingdom.getId());
                statement.setInt(3, kingdom.getLevel());
                statement.setLong(4, kingdom.getExperience());

                return statement.executeUpdate() > 0;
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to set Kingdom EXP: "
                            + exception.getMessage()
            );
            return false;
        }
    }

    public synchronized boolean setLevel(UUID player, int level) {
        if (level < 1 || level > getMaxLevel()) {
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
                    experience = 0
                WHERE id = ?
                """;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, level);
                statement.setInt(2, kingdom.getId());

                return statement.executeUpdate() > 0;
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
