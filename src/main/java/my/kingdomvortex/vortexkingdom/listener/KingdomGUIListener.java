
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.gui.KingdomGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomMenuHolder;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KingdomGUIListener implements Listener {

    private final VortexKingdom plugin;
    private final KingdomGUIManager guiManager;

    private final Map<UUID, Boolean> creatingKingdom =
            new ConcurrentHashMap<>();

    public KingdomGUIListener(
            VortexKingdom plugin,
            KingdomGUIManager guiManager
    ) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder()
                instanceof KingdomMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null ||
                event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack item = event.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.getMenu()) {

            case MAIN -> handleMainMenu(player, slot);

            case PROFILE -> handleProfileMenu(player, slot);

            case LEVEL -> handleLevelMenu(player, slot);
        }
    }

    private void handleMainMenu(Player player, int slot) {
        Optional<Kingdom> kingdom =
                plugin.getKingdomManager()
                        .getKingdomByPlayer(player.getUniqueId());

        if (kingdom.isEmpty()) {
            if (slot == 22) {
                startKingdomCreation(player);
            }

            if (slot == 42) {
                player.closeInventory();
            }

            return;
        }

        switch (slot) {
            case 10 -> guiManager.openProfile(player);

            case 14 -> guiManager.openLevelMenu(player);

            case 42 -> player.closeInventory();

            case 12, 16, 38, 40 -> player.sendMessage(
                    "§eThis Kingdom menu will be available in the next stage."
            );
        }
    }

    private void handleProfileMenu(Player player, int slot) {
        switch (slot) {
            case 31 -> guiManager.openLevelMenu(player);

            case 49 -> guiManager.openMainMenu(player);

            case 29, 33 -> player.sendMessage(
                    "§eThis feature is being prepared."
            );
        }
    }

    private void handleLevelMenu(Player player, int slot) {
        if (slot == 49) {
            guiManager.openMainMenu(player);
        }
    }

    private void startKingdomCreation(Player player) {
        if (plugin.getKingdomManager()
                .getKingdomByPlayer(player.getUniqueId())
                .isPresent()) {

            player.sendMessage("§cYou already belong to a Kingdom.");
            return;
        }

        creatingKingdom.put(player.getUniqueId(), true);

        player.closeInventory();

        player.sendMessage("");
        player.sendMessage("§6§lKINGDOM CREATION");
        player.sendMessage("§7Enter your Kingdom name in chat.");
        player.sendMessage("§7Allowed: letters, numbers and underscore.");
        player.sendMessage("§7Length: 3-16 characters.");
        player.sendMessage("§cType cancel to cancel.");
        player.sendMessage("");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!creatingKingdom.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String input = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            UUID uuid = player.getUniqueId();

            if (!creatingKingdom.remove(uuid, true)) {
                return;
            }

            if (!player.isOnline()) {
                return;
            }

            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage("§cKingdom creation cancelled.");
                guiManager.openMainMenu(player);
                return;
            }

            if (!input.matches("[a-zA-Z0-9_]{3,16}")) {
                player.sendMessage(
                        "§cInvalid name! Use 3-16 letters, numbers or underscore."
                );
                player.sendMessage(
                        "§7Use /kingdom to try again."
                );
                return;
            }

            boolean created = plugin.getKingdomManager()
                    .createKingdom(
                            uuid,
                            player.getName(),
                            input
                    );

            if (created) {
                player.sendMessage(
                        "§aKingdom §e" + input +
                                " §ahas been created successfully!"
                );

                guiManager.openMainMenu(player);

            } else {
                player.sendMessage(
                        "§cCould not create Kingdom. " +
                                "You may already belong to one, " +
                                "or the name is already taken."
                );
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder()
                instanceof KingdomMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        creatingKingdom.remove(event.getPlayer().getUniqueId());
    }
}
