
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.gui.KingdomFeatureGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomMenuHolder;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class KingdomFeatureEntryListener implements Listener {

    private final VortexKingdom plugin;
    private final KingdomFeatureGUIManager gui;

    public KingdomFeatureEntryListener(VortexKingdom plugin) {
        this.plugin = plugin;
        this.gui = plugin.getFeatureGUIManager();
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = false
    )
    public void onMainMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder()
                instanceof KingdomMenuHolder holder)) {
            return;
        }

        if (holder.getMenu() != KingdomMenuHolder.Menu.MAIN) {
            return;
        }

        if (event.getClickedInventory()
                != event.getView().getTopInventory()) {
            return;
        }

        if (plugin.getKingdomManager()
                .getKingdomByPlayer(player.getUniqueId()).isEmpty()) {
            return;
        }

        switch (event.getRawSlot()) {
            case 16 -> gui.openHome(player);
            case 22 -> gui.openSettings(player);
            default -> {
            }
        }
    }
}
