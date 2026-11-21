
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class KingdomExpListener implements Listener {

    private final VortexKingdom plugin;

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

    private static final Map<EntityType, String> BOSS_REWARDS = Map.of(
            EntityType.ENDER_DRAGON, "ender-dragon",
            EntityType.WITHER, "wither",
            EntityType.WARDEN, "warden",
            EntityType.ELDER_GUARDIAN, "elder-guardian"
    );

    public KingdomExpListener(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onMining(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();

        if (material.isAir() || FARM_CROPS.contains(material)) {
            return;
        }

        plugin.getExpRewardManager().award(
                event.getPlayer().getUniqueId(),
                "mining",
                material.name()
        );
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onFarming(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();

        if (!FARM_CROPS.contains(material)) {
            return;
        }

        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }

        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        plugin.getExpRewardManager().award(
                event.getPlayer().getUniqueId(),
                "farming",
                material.name()
        );
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onMonsterKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }

        UUID player = event.getEntity().getKiller().getUniqueId();
        EntityType type = event.getEntityType();

        // Catat kill Kingdom.
        recordKingdomKill(player);

        // Boss menggunakan reward khusus.
        String bossKey = BOSS_REWARDS.get(type);

        if (bossKey != null) {
            plugin.getExpRewardManager().award(
                    player,
                    "boss-kill",
                    "boss:" + bossKey
            );
            return;
        }

        // Monster biasa.
        if (event.getEntity() instanceof Monster) {
            plugin.getExpRewardManager().award(
                    player,
                    "monster-kill",
                    type.name()
            );
        }
    }

    private void recordKingdomKill(UUID player) {
        Optional<Kingdom> optional =
                plugin.getKingdomManager()
                        .getKingdomByPlayer(player);

        if (optional.isEmpty()) {
            return;
        }

        Kingdom kingdom = optional.get();

        String sql = """
                UPDATE kingdoms
                SET total_kills = total_kills + 1
                WHERE id = ?
                """;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {
                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setInt(1, kingdom.getId());
                    statement.executeUpdate();
                }
            }

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Failed to record Kingdom kill: "
                            + exception.getMessage()
            );
        }
    }
}
