
package my.kingdomvortex.vortexkingdom.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KingdomMenuHolder implements InventoryHolder {

    public enum Menu {
        MAIN,
        PROFILE,
        LEVEL
    }

    private final Menu menu;
    private Inventory inventory;

    public KingdomMenuHolder(Menu menu) {
        this.menu = menu;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
