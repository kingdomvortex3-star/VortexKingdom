
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.gui.KingdomLeaderboardHolder;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardType;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class KingdomLeaderboardGUIListener implements Listener {

    private final VortexKingdom plugin;

    public KingdomLeaderboardGUIListener(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder()
                instanceof KingdomLeaderboardHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory()
                != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();

        switch (slot) {
            case 45 -> {
                if (holder.getPage() > 0) {
                    plugin.getLeaderboardGUIManager().open(
                            player,
                            holder.getType(),
                            holder.getPage() - 1
                    );
                }
            }

            case 53 -> plugin.getLeaderboardGUIManager().open(
                    player,
                    holder.getType(),
                    holder.getPage() + 1
            );

            case 48 -> plugin.getLeaderboardGUIManager().open(
                    player,
                    LeaderboardType.LEVEL,
                    0
            );

            case 49 -> player.closeInventory();

            case 50 -> plugin.getLeaderboardGUIManager().open(
                    player,
                    LeaderboardType.EXPERIENCE,
                    0
            );

            case 51 -> plugin.getLeaderboardGUIManager().open(
                    player,
                    LeaderboardType.MEMBERS,
                    0
            );

            case 52 -> plugin.getLeaderboardGUIManager().open(
                    player,
                    LeaderboardType.KILLS,
                    0
            );
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder()
                instanceof KingdomLeaderboardHolder) {
            event.setCancelled(true);
        }
    }
}
