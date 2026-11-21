package my.kingdomvortex.vortexkingdom.gui;

import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardType;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KingdomLeaderboardHolder implements InventoryHolder {

    private final LeaderboardType type;
    private final int page;

    private Inventory inventory;

    public KingdomLeaderboardHolder(
            LeaderboardType type,
            int page
    ) {
        this.type = type;
        this.page = page;
    }

    public LeaderboardType getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
