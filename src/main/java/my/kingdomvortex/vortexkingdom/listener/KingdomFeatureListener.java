
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.gui.KingdomFeatureGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomFeatureMenuHolder;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;

import org.bukkit.Bukkit;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KingdomFeatureListener implements Listener {

    private static final String PREFIX = "§8[§6Kingdom§8] §7";

    private final VortexKingdom plugin;
    private final KingdomFeatureGUIManager gui;

    private final Set<UUID> kingdomChat =
            ConcurrentHashMap.newKeySet();

    public KingdomFeatureListener(
            VortexKingdom plugin,
            KingdomFeatureGUIManager gui
    ) {
        this.plugin = plugin;
        this.gui = gui;
    }

    public boolean isChatEnabled(Player player) {
        return kingdomChat.contains(player.getUniqueId());
    }

    public void toggleChat(Player player) {
        Optional<Kingdom> kingdom =
                plugin.getKingdomManager().getKingdomByPlayer(
                        player.getUniqueId()
                );

        if (kingdom.isEmpty()) {
            player.sendMessage(
                    PREFIX + "§cKamu belum memiliki Kingdom."
            );
            return;
        }

        if (!kingdomChat.add(player.getUniqueId())) {
            kingdomChat.remove(player.getUniqueId());
            player.sendMessage(
                    PREFIX + "§cKingdom Chat dinonaktifkan."
            );
        } else {
            player.sendMessage(
                    PREFIX + "§aKingdom Chat diaktifkan."
            );
        }

        player.playSound(
                player.getLocation(),
                Sound.UI_BUTTON_CLICK,
                1f,
                1.2f
        );

        gui.openSettings(player);
    }

    // ==================================================
    // FEATURE GUI
    // ==================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFeatureInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof KingdomFeatureMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != top) {
            return;
        }

        int slot = event.getRawSlot();

        if (holder.getMenu()
                == KingdomFeatureMenuHolder.Menu.DESCRIPTION_INPUT
                || holder.getMenu()
                == KingdomFeatureMenuHolder.Menu.CREATE_KINGDOM_INPUT) {

            if (slot != 2 || !(top instanceof AnvilInventory anvil)) {
                return;
            }

            String text = anvil.getRenameText();

            if (text == null || text.isBlank()) {
                player.sendMessage(
                        PREFIX + "§cInput tidak boleh kosong."
                );
                return;
            }

            player.closeInventory();

            if (holder.getMenu()
                    == KingdomFeatureMenuHolder.Menu.CREATE_KINGDOM_INPUT) {

                boolean created =
                        plugin.getKingdomManager().createKingdom(
                                player.getUniqueId(),
                                player.getName(),
                                text.trim()
                        );

                player.sendMessage(
                        created
                                ? PREFIX + "§aKingdom §6" + text.trim()
                                    + " §aberhasil dibuat!"
                                : PREFIX + "§cNama Kingdom tidak valid "
                                    + "atau gagal dibuat."
                );

                if (created) {
                    Bukkit.getScheduler().runTask(
                            plugin,
                            () -> plugin.getGuiManager().openMain(player)
                    );
                }

                return;
            }

            int max = Math.max(
                    16,
                    plugin.getConfig().getInt(
                            "kingdom.maximum-description-length",
                            120
                    )
            );

            if (text.length() > max) {
                player.sendMessage(
                        PREFIX + "§cDeskripsi maksimal "
                                + max + " karakter."
                );

                Bukkit.getScheduler().runTask(
                        plugin,
                        () -> gui.openDescriptionInput(player)
                );

                return;
            }

            boolean updated =
                    plugin.getFeatureManager().setDescription(
                            player,
                            text
                    );

            player.sendMessage(
                    updated
                            ? PREFIX + "§aDeskripsi Kingdom berhasil diperbarui."
                            : PREFIX + "§cGagal memperbarui deskripsi."
            );

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> gui.openSettings(player)
            );

            return;
        }

        switch (holder.getMenu()) {
            case HOME -> handleHome(player, slot);
            case SETTINGS -> handleSettings(player, slot);
            case HOME_CONFIRMATION ->
                    handleHomeConfirmation(player, slot);
            case DESCRIPTION_INPUT, CREATE_KINGDOM_INPUT -> {
            }
        }
    }

    // ==================================================
    // HOME
    // ==================================================

    private void handleHome(Player player, int slot) {
        switch (slot) {
            case 20 -> {
                if (!plugin.getFeatureManager().canManage(
                        player.getUniqueId())) {
                    player.sendMessage(
                            PREFIX + "§cHanya Owner, Leader, atau Officer "
                                    + "yang dapat mengatur Home."
                    );
                    return;
                }

                boolean saved =
                        plugin.getFeatureManager().setHome(player);

                player.sendMessage(
                        saved
                                ? PREFIX + "§aKingdom Home berhasil disimpan."
                                : PREFIX + "§cGagal menyimpan Kingdom Home."
                );

                gui.openHome(player);
            }

            case 22 -> {
                Optional<org.bukkit.Location> home =
                        plugin.getFeatureManager().getHome(
                                player.getUniqueId()
                        );

                if (home.isEmpty()) {
                    player.sendMessage(
                            PREFIX + "§cKingdom belum memiliki Home "
                                    + "atau world tidak tersedia."
                    );
                    return;
                }

                player.closeInventory();

                player.teleportAsync(home.get()).thenAccept(success ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) {
                                player.sendMessage(
                                        success
                                                ? PREFIX + "§aBerhasil teleport "
                                                    + "ke Kingdom Home."
                                                : PREFIX + "§cTeleport gagal."
                                );
                            }
                        })
                );
            }

            case 24 -> {
                if (!plugin.getFeatureManager().canManage(
                        player.getUniqueId())) {
                    player.sendMessage(
                            PREFIX + "§cKamu tidak memiliki izin "
                                    + "menghapus Home."
                    );
                    return;
                }

                if (plugin.getFeatureManager().getHome(
                        player.getUniqueId()).isEmpty()) {
                    player.sendMessage(
                            PREFIX + "§cKingdom belum memiliki Home."
                    );
                    return;
                }

                gui.openHomeDeleteConfirmation(player);
            }

            case 49 -> plugin.getGuiManager().openMain(player);
        }
    }

    // ==================================================
    // SETTINGS
    // ==================================================

    private void handleSettings(Player player, int slot) {
        switch (slot) {
            case 20 -> {
                if (!plugin.getFeatureManager().canManage(
                        player.getUniqueId())) {
                    player.sendMessage(
                            PREFIX + "§cHanya Owner, Leader, atau Officer "
                                    + "yang dapat mengubah PvP."
                    );
                    return;
                }

                Optional<Kingdom> kingdom =
                        plugin.getKingdomManager().getKingdomByPlayer(
                                player.getUniqueId()
                        );

                if (kingdom.isEmpty()) {
                    return;
                }

                boolean newState = !kingdom.get().isPvpEnabled();

                boolean updated =
                        plugin.getFeatureManager().setPvp(
                                player,
                                newState
                        );

                player.sendMessage(
                        updated
                                ? PREFIX + "§aKingdom PvP sekarang "
                                    + (newState ? "§aON" : "§cOFF") + "§a."
                                : PREFIX + "§cGagal mengubah pengaturan PvP."
                );

                gui.openSettings(player);
            }

            case 22 -> gui.openDescriptionInput(player);

            case 24 -> toggleChat(player);

            case 49 -> plugin.getGuiManager().openProfile(player);
        }
    }

    // ==================================================
    // HOME DELETE CONFIRMATION
    // ==================================================

    private void handleHomeConfirmation(Player player, int slot) {
        if (slot == 11) {
            gui.openHome(player);
            return;
        }

        if (slot == 15) {
            boolean deleted =
                    plugin.getFeatureManager().deleteHome(player);

            player.sendMessage(
                    deleted
                            ? PREFIX + "§aKingdom Home berhasil dihapus."
                            : PREFIX + "§cGagal menghapus Kingdom Home."
            );

            gui.openHome(player);
        }
    }

    // ==================================================
    // FRIENDLY FIRE
    // ==================================================

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

        if (attacker == null) {
            return;
        }

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
                == attackerKingdom.get().getId()
                && !victimKingdom.get().isPvpEnabled()) {

            event.setCancelled(true);

            attacker.sendMessage(
                    PREFIX + "§cPvP Kingdom sedang dinonaktifkan."
            );
        }
    }

    // ==================================================
    // KINGDOM CHAT
    // ==================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKingdomChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();

        if (!kingdomChat.contains(sender.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String message = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Optional<Kingdom> kingdom =
                    plugin.getKingdomManager().getKingdomByPlayer(
                            sender.getUniqueId()
                    );

            if (kingdom.isEmpty()) {
                kingdomChat.remove(sender.getUniqueId());

                if (sender.isOnline()) {
                    sender.sendMessage(
                            PREFIX + "§cKamu tidak lagi memiliki Kingdom."
                    );
                }

                return;
            }

            String formatted =
                    "§8[§6K§8] §e" + sender.getName()
                            + " §8» §f" + message;

            for (KingdomMember member :
                    plugin.getKingdomManager().getMembers(
                            kingdom.get().getId())) {

                Player recipient =
                        Bukkit.getPlayer(member.getPlayerUniqueId());

                if (recipient != null && recipient.isOnline()) {
                    recipient.sendMessage(formatted);
                }
            }
        });
    }

    // ==================================================
    // INVENTORY DRAG
    // ==================================================

    @EventHandler
    public void onFeatureInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder()
                instanceof KingdomFeatureMenuHolder) {
            event.setCancelled(true);
        }
    }

    // ==================================================
    // PLAYER QUIT
    // ==================================================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        kingdomChat.remove(event.getPlayer().getUniqueId());
    }
}
