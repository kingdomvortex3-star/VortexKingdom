
package my.kingdomvortex.vortexkingdom.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KingdomFeatureMenuHolder implements InventoryHolder {

    public enum Menu {
        HOME,
        SETTINGS,
        HOME_CONFIRMATION,
        DESCRIPTION_INPUT,
        CREATE_KINGDOM_INPUT
    }

    private final Menu menu;
    private Inventory inventory;

    public KingdomFeatureMenuHolder(Menu menu) {
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
