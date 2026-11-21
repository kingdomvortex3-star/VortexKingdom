
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;

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

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonsterKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }

        UUID player = event.getEntity().getKiller().getUniqueId();
        EntityType type = event.getEntityType();

        // Boss mendapatkan reward khusus.
        String bossKey = BOSS_REWARDS.get(type);

        if (bossKey != null) {
            awardBoss(player, bossKey);
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

    private void awardBoss(UUID player, String bossKey) {
        String path = "boss-rewards." + bossKey + ".";

        if (!plugin.getExpConfig().getBoolean(
                "boss-rewards.enabled", true
        )) {
            return;
        }

        if (!plugin.getExpConfig().getBoolean(
                path + "enabled", true
        )) {
            return;
        }

        long exp = plugin.getExpConfig().getLong(
                path + "exp", 0
        );

        if (exp <= 0) {
            return;
        }

        /*
         * Boss reward menggunakan sumber boss-kill
         * agar cooldown dan daily limit tetap berlaku.
         */
        plugin.getExpRewardManager().award(
                player,
                "boss-kill",
                "boss:" + bossKey.toLowerCase(Locale.ROOT)
        );
    }
}
