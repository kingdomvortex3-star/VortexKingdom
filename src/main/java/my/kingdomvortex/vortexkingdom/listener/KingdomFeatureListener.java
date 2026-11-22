
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.feature.KingdomFeatureManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomFeatureGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomFeatureMenuHolder;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;

import java.util.Optional;
import java.util.UUID;

public final class KingdomFeatureListener implements Listener {

    private static final String PREFIX = "§8[§6Kingdom§8] §7";

    private final VortexKingdom plugin;
    private final KingdomFeatureManager features;
    private final KingdomFeatureGUIManager gui;

    public KingdomFeatureListener(
            VortexKingdom plugin,
            KingdomFeatureGUIManager gui
    ) {
        this.plugin = plugin;
        this.features = plugin.getFeatureManager();
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getView().getTopInventory();

        if (!(inventory.getHolder()
                instanceof KingdomFeatureMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.getMenu()) {
            case HOME -> handleHome(player, slot);
            case SETTINGS -> handleSettings(player, slot);
            case HOME_CONFIRMATION ->
                    handleHomeConfirmation(player, slot);
            case DESCRIPTION_INPUT ->
                    handleDescriptionInput(player, inventory, slot);
            case CREATE_KINGDOM_INPUT ->
                    handleCreateKingdom(player, inventory, slot);
        }
    }

    private void handleHome(Player player, int slot) {
        switch (slot) {
            case 20 -> {
                if (!features.canManage(player.getUniqueId())) {
                    deny(player);
                    return;
                }

                boolean saved = features.setHome(player);

                player.sendMessage(
                        saved
                                ? PREFIX + "§aKingdom Home berhasil disimpan."
                                : PREFIX + "§cGagal menyimpan Kingdom Home."
                );

                gui.openHome(player);
            }

            case 22 -> {
                Optional<Location> home =
                        features.getHome(player.getUniqueId());

                if (home.isEmpty()) {
                    player.sendMessage(
                            PREFIX + "§cKingdom belum memiliki Home."
                    );
                    return;
                }

                player.closeInventory();

                player.teleportAsync(home.get()).thenAccept(success ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!player.isOnline()) return;

                            player.sendMessage(
                                    success
                                            ? PREFIX + "§aBerhasil teleport ke Kingdom Home."
                                            : PREFIX + "§cTeleport gagal."
                            );
                        })
                );
            }

            case 24 -> {
                if (!features.canManage(player.getUniqueId())) {
                    deny(player);
                    return;
                }

                if (features.getHome(player.getUniqueId()).isEmpty()) {
                    player.sendMessage(
                            PREFIX + "§cKingdom belum memiliki Home."
                    );
                    return;
                }

                gui.openHomeDeleteConfirmation(player);
            }

            case 49 -> plugin.getGuiManager().openMainMenu(player);
        }
    }

    private void handleSettings(Player player, int slot) {
        switch (slot) {
            case 20 -> {
                if (!features.canManage(player.getUniqueId())) {
                    deny(player);
                    return;
                }

                Optional<Kingdom> optional =
                        plugin.getKingdomManager().getKingdomByPlayer(
                                player.getUniqueId()
                        );

                if (optional.isEmpty()) return;

                boolean enabled = !optional.get().isPvpEnabled();
                boolean updated = features.setPvp(player, enabled);

                player.sendMessage(
                        updated
                                ? PREFIX + "§aKingdom PvP: "
                                    + (enabled ? "§aON" : "§cOFF")
                                : PREFIX + "§cGagal mengubah PvP."
                );

                gui.openSettings(player);
            }

            case 22 -> {
                if (!features.canManage(player.getUniqueId())) {
                    deny(player);
                    return;
                }

                gui.openDescriptionInput(player);
            }

            case 24 -> {
                if (!features.canManage(player.getUniqueId())) {
                    deny(player);
                    return;
                }

                Optional<Kingdom> optional =
                        plugin.getKingdomManager().getKingdomByPlayer(
                                player.getUniqueId()
                        );

                if (optional.isEmpty()) return;

                boolean enabled = !features.isChatEnabled(
                        optional.get().getId()
                );

                boolean updated = features.setChatEnabled(player, enabled);

                player.sendMessage(
                        updated
                                ? PREFIX + "§aKingdom Chat: "
                                    + (enabled ? "§aON" : "§cOFF")
                                : PREFIX + "§cGagal mengubah Kingdom Chat."
                );

                gui.openSettings(player);
            }

            case 49 -> plugin.getGuiManager().openMainMenu(player);
        }
    }

    private void handleHomeConfirmation(Player player, int slot) {
        if (slot == 11) {
            gui.openHome(player);
            return;
        }

        if (slot == 15) {
            boolean deleted = features.deleteHome(player);

            player.sendMessage(
                    deleted
                            ? PREFIX + "§aKingdom Home berhasil dihapus."
                            : PREFIX + "§cGagal menghapus Kingdom Home."
            );

            gui.openHome(player);
        }
    }

    private void handleDescriptionInput(
            Player player,
            Inventory inventory,
            int slot
    ) {
        if (slot != 2 || !(inventory instanceof AnvilInventory anvil)) {
            return;
        }

        String description = anvil.getRenameText();

        if (description == null || description.isBlank()) {
            player.sendMessage(
                    PREFIX + "§cDeskripsi tidak boleh kosong."
            );
            return;
        }

        int maxLength = Math.max(
                16,
                plugin.getConfig().getInt(
                        "kingdom.maximum-description-length",
                        120
                )
        );

        if (description.length() > maxLength) {
            player.sendMessage(
                    PREFIX + "§cDeskripsi maksimal "
                            + maxLength + " karakter."
            );
            return;
        }

        boolean updated = features.setDescription(player, description);

        player.closeInventory();

        player.sendMessage(
                updated
                        ? PREFIX + "§aDeskripsi Kingdom berhasil diperbarui."
                        : PREFIX + "§cGagal memperbarui deskripsi."
        );

        gui.openSettings(player);
    }

    private void handleCreateKingdom(
            Player player,
            Inventory inventory,
            int slot
    ) {
        if (slot != 2 || !(inventory instanceof AnvilInventory anvil)) {
            return;
        }

        String name = anvil.getRenameText();

        if (name == null || name.isBlank()) {
            player.sendMessage(
                    PREFIX + "§cNama Kingdom tidak boleh kosong."
            );
            return;
        }

        boolean created = plugin.getKingdomManager().createKingdom(
                player.getUniqueId(),
                player.getName(),
                name.trim()
        );

        player.closeInventory();

        player.sendMessage(
                created
                        ? PREFIX + "§aKingdom §6" + name.trim()
                            + " §aberhasil dibuat!"
                        : PREFIX + "§cGagal membuat Kingdom. "
                            + "Periksa nama atau status keanggotaanmu."
        );

        if (created) {
            plugin.getGuiManager().openMainMenu(player);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = null;

        if (event.getDamager() instanceof Player direct) {
            attacker = direct;
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        if (attacker == null) return;

        Optional<Kingdom> victimKingdom =
                plugin.getKingdomManager().getKingdomByPlayer(
                        victim.getUniqueId()
                );

        Optional<Kingdom> attackerKingdom =
                plugin.getKingdomManager().getKingdomByPlayer(
                        attacker.getUniqueId()
                );

        if (victimKingdom.isEmpty() || attackerKingdom.isEmpty()) {
            return;
        }

        if (victimKingdom.get().getId()
                != attackerKingdom.get().getId()) {
            return;
        }

        if (!features.isPvpEnabled(victimKingdom.get().getId())) {
            event.setCancelled(true);

            attacker.sendMessage(
                    PREFIX + "§cPvP Kingdom sedang dinonaktifkan."
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKingdomChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();

        Optional<Kingdom> optional =
                plugin.getKingdomManager().getKingdomByPlayer(
                        sender.getUniqueId()
                );

        if (optional.isEmpty()) return;

        Kingdom kingdom = optional.get();

        if (!features.isChatEnabled(kingdom.getId())) return;

        event.setCancelled(true);

        String message = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (KingdomMember member :
                    plugin.getKingdomManager().getMembers(kingdom.getId())) {

                UUID uuid = member.getPlayerUniqueId();

                if (uuid == null) continue;

                Player recipient = Bukkit.getPlayer(uuid);

                if (recipient != null && recipient.isOnline()) {
                    recipient.sendMessage(
                            "§8[§6K§8] §e" + sender.getName()
                                    + " §8» §f" + message
                    );
                }
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder()
                instanceof KingdomFeatureMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void deny(Player player) {
        player.sendMessage(
                PREFIX + "§cKamu tidak memiliki izin untuk melakukan ini."
        );

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_VILLAGER_NO,
                1f,
                1f
        );
    }
}
