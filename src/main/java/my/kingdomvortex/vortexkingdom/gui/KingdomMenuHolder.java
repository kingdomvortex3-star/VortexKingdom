
package my.kingdomvortex.vortexkingdom.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KingdomMenuHolder implements InventoryHolder {

    public enum Menu {
        MAIN,
        PROFILE,
        LEVEL,
        MEMBERS,
        INVITE_PLAYERS,
        MEMBER_ACTIONS,
        INVITATIONS,
        CONFIRMATION,

        // New GUI menus
        HOME,
        SETTINGS,
        DESCRIPTION
    }

    private final Menu menu;
    private final int page;
    private final String target;

    private Inventory inventory;

    public KingdomMenuHolder(Menu menu) {
        this(menu, 0, null);
    }

    public KingdomMenuHolder(Menu menu, int page) {
        this(menu, page, null);
    }

    public KingdomMenuHolder(
            Menu menu,
            int page,
            String target
    ) {
        this.menu = menu;
        this.page = page;
        this.target = target;
    }

    public Menu getMenu() {
        return menu;
    }

    public int getPage() {
        return page;
    }

    public String getTarget() {
        return target;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
