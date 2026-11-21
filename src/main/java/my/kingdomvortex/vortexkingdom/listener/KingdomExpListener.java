
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KingdomExpListener implements Listener {

    private final VortexKingdom plugin;

    // ==================================================
    // FARMING MATERIALS
    // ==================================================

    private static final Set<Material> FARM_CROPS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.COCOA,
            Material.SWEET_BERRY_BUSH,
            Material.MELON,
            Material.PUMPKIN,
            Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP
    );

    // ==================================================
    // BOSS REWARDS
    // ==================================================

    private static final Map<EntityType, String> BOSS_REWARDS = Map.of(
            EntityType.ENDER_DRAGON, "ender-dragon",
            EntityType.WITHER, "wither",
            EntityType.WARDEN, "warden",
            EntityType.ELDER_GUARDIAN, "elder-guardian"
    );

    public KingdomExpListener(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    // ==================================================
    // MINING EXP
    // ==================================================

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onMining(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        if (material.isAir()) {
            return;
        }

        // Tanaman tidak mendapatkan mining EXP.
        if (FARM_CROPS.contains(material)) {
            return;
        }

        plugin.getExpRewardManager().award(
                player.getUniqueId(),
                "mining",
                material.name()
        );
    }

    // ==================================================
    // FARMING EXP
    // ==================================================

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onFarming(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        if (!FARM_CROPS.contains(material)) {
            return;
        }

        // Melon dan Pumpkin tidak menggunakan Ageable.
        if (material == Material.MELON
                || material == Material.PUMPKIN) {

            awardFarming(player.getUniqueId(), material);
            return;
        }

        // Tanaman lainnya harus sudah matang.
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }

        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        awardFarming(player.getUniqueId(), material);
    }

    private void awardFarming(UUID player, Material material) {

        plugin.getExpRewardManager().award(
                player,
                "farming",
                material.name()
        );
    }

    // ==================================================
    // MONSTER / BOSS KILL
    // ==================================================

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onMonsterKill(EntityDeathEvent event) {

        Player killer = event.getEntity().getKiller();

        if (killer == null) {
            return;
        }

        EntityType type = event.getEntityType();

        // Periksa apakah target merupakan boss.
        String bossKey = BOSS_REWARDS.get(type);

        if (bossKey != null) {

            // Boss kill masuk statistik Kingdom.
            recordKingdomKill(killer.getUniqueId());

            // Boss menggunakan reward khusus.
            plugin.getExpRewardManager().award(
                    killer.getUniqueId(),
                    "boss-kill",
                    "boss:" + bossKey
            );

            return;
        }

        // Monster biasa.
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        // Catat kill monster.
        recordKingdomKill(killer.getUniqueId());

        // Berikan EXP monster.
        plugin.getExpRewardManager().award(
                killer.getUniqueId(),
                "monster-kill",
                type.name()
        );
    }

    // ==================================================
    // KINGDOM KILL STATISTICS
    // ==================================================

    private void recordKingdomKill(UUID player) {

        if (player == null) {
            return;
        }

        String sql = """
                UPDATE kingdoms
                SET total_kills = total_kills + 1,
                    updated_at = ?
                WHERE id = (
                    SELECT kingdom_id
                    FROM kingdom_members
                    WHERE player_uuid = ?
                    LIMIT 1
                )
                """;

        try {

            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setLong(
                            1,
                            System.currentTimeMillis()
                    );

                    statement.setString(
                            2,
                            player.toString()
                    );

                    statement.executeUpdate();
                }
            }

        } catch (Exception exception) {

            plugin.getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Failed to record Kingdom kill for " + player,
                    exception
            );
        }
    }
}
