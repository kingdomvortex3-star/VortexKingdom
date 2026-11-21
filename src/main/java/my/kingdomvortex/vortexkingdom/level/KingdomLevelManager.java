
package my.kingdomvortex.vortexkingdom.level;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class KingdomLevelManager {

    private final VortexKingdom plugin;

    public KingdomLevelManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    public long getRequiredExp(int level) {
        if (level >= getMaxLevel()) {
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

        if (!Double.isFinite(required) || required > Long.MAX_VALUE) {
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

    public synchronized boolean addExp(UUID player, long amount) {
        if (amount <= 0) {
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

        if (required <= 0) {
            return false;
        }

        long currentExp = kingdom.getExperience();
        long actualGain = Math.min(amount, required - currentExp);

        if (actualGain <= 0) {
            return false;
        }

        boolean levelUp = actualGain >= required - currentExp;

        String sql = """
                UPDATE kingdoms
                SET experience = ?,
                    total_experience = total_experience + ?,
                    level = level + ?
                WHERE id = ?
                  AND level = ?
                  AND experience = ?
                  AND level < ?
                """;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection()
                             .prepareStatement(sql)) {

            statement.setLong(1, levelUp ? 0 : currentExp + actualGain);
            statement.setLong(2, actualGain);
            statement.setInt(3, levelUp ? 1 : 0);
            statement.setInt(4, kingdom.getId());
            statement.setInt(5, kingdom.getLevel());
            statement.setLong(6, currentExp);
            statement.setInt(7, getMaxLevel());

            if (statement.executeUpdate() <= 0) {
                return false;
            }

            if (levelUp) {
                handleLevelUp(
                        kingdom.getName(),
                        kingdom.getLevel() + 1
                );
            }

            return true;

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to add Kingdom EXP: " +
                            exception.getMessage()
            );
            return false;
        }
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

        return Math.min(
                100,
                kingdom.getExperience() * 100.0 / required
        );
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
                getRequiredExp(kingdom.getLevel()) -
                        kingdom.getExperience()
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

        if (amount >= getRequiredExp(kingdom.getLevel())) {
            return false;
        }

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection()
                             .prepareStatement(
                                     """
                                     UPDATE kingdoms
                                     SET experience = ?
                                     WHERE id = ? AND level = ?
                                     """
                             )) {

            statement.setLong(1, amount);
            statement.setInt(2, kingdom.getId());
            statement.setInt(3, kingdom.getLevel());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to set Kingdom EXP: " +
                            exception.getMessage()
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

        try (PreparedStatement statement =
                     plugin.getDatabaseManager().getConnection()
                             .prepareStatement(
                                     """
                                     UPDATE kingdoms
                                     SET level = ?, experience = 0
                                     WHERE id = ?
                                     """
                             )) {

            statement.setInt(1, level);
            statement.setInt(2, optional.get().getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to set Kingdom level: " +
                            exception.getMessage()
            );
            return false;
        }
    }

    private Optional<Kingdom> getKingdom(UUID player) {
        return plugin.getKingdomManager()
                .getKingdomByPlayer(player);
    }

    private void handleLevelUp(String name, int level) {
        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> {
                    plugin.getServer().broadcastMessage(
                            "§8[§6Kingdom§8] §e" + name +
                                    " §7has reached §6Level " +
                                    level + "§7!"
                    );
                }
        );
    }
}
