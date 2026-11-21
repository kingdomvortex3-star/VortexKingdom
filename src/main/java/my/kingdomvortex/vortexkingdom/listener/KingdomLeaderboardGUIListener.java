
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.gui.KingdomLeaderboardHolder;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardType;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class KingdomLeaderboardGUIListener implements Listener {

    private final VortexKingdom plugin;

    public KingdomLeaderboardGUIListener(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder()
                instanceof KingdomLeaderboardHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() != topInventory) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 45) {
            if (holder.getPage() > 0) {
                plugin.getLeaderboardGUIManager().open(
                        player,
                        holder.getType(),
                        holder.getPage() - 1
                );
            }
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 53) {
            plugin.getLeaderboardGUIManager().open(
                    player,
                    holder.getType(),
                    holder.getPage() + 1
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
