
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.gui.KingdomGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomMenuHolder;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember.Role;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager.Invitation;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KingdomGUIListener implements Listener {

    private final VortexKingdom plugin;
    private final KingdomGUIManager gui;
    private final KingdomManager kingdoms;

    private final Map<UUID, String> creationRequests = new HashMap<>();

    public KingdomGUIListener(
            VortexKingdom plugin,
            KingdomGUIManager gui
    ) {
        this.plugin = plugin;
        this.gui = gui;
        this.kingdoms = plugin.getKingdomManager();
    }

    // ==========================================
    // INVENTORY CLICK
    // ==========================================

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
                event.getClickedInventory() != event.getInventory()) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        switch (holder.getMenu()) {
            case MAIN -> handleMain(player, slot);
            case PROFILE -> handleProfile(player, slot);
            case LEVEL -> handleLevel(player, slot);
            case MEMBERS -> handleMembers(player, holder, slot);
            case INVITE_PLAYERS -> handleInvitePlayers(player, slot);
            case MEMBER_ACTIONS -> handleMemberActions(player, holder, slot);
            case INVITATIONS -> handleInvitations(player, holder, slot);
            case CONFIRMATION -> handleConfirmation(player, holder, slot);
        }
    }

    // ==========================================
    // MAIN MENU
    // ==========================================

    private void handleMain(Player player, int slot) {
        switch (slot) {
            case 10 -> {
                if (kingdoms.getKingdomByPlayer(player.getUniqueId()).isPresent()) {
                    gui.openProfile(player);
                } else {
                    player.sendMessage("§cKamu belum memiliki Kingdom.");
                }
            }

            case 12 -> {
                if (kingdoms.getKingdomByPlayer(player.getUniqueId()).isPresent()) {
                    gui.openMembers(player, 0);
                } else {
                    player.sendMessage("§cKamu belum memiliki Kingdom.");
                }
            }

            case 14 -> {
                if (kingdoms.getKingdomByPlayer(player.getUniqueId()).isPresent()) {
                    gui.openLevel(player);
                } else {
                    player.sendMessage("§cKamu belum memiliki Kingdom.");
                }
            }

            case 16 -> {
                if (kingdoms.getKingdomByPlayer(player.getUniqueId()).isPresent()) {
                    player.closeInventory();
                    player.sendMessage("§eFitur Kingdom Home akan segera tersedia.");
                } else {
                    player.sendMessage("§cKamu belum memiliki Kingdom.");
                }
            }

            case 20 -> {
                if (kingdoms.getKingdomByPlayer(player.getUniqueId()).isPresent()) {
                    player.sendMessage("§cKamu sudah memiliki Kingdom.");
                    return;
                }

                player.closeInventory();
                creationRequests.put(player.getUniqueId(), "CREATE");

                player.sendMessage(" ");
                player.sendMessage("§6§lKINGDOM CREATION");
                player.sendMessage("§7Ketik nama Kingdom yang ingin kamu buat.");
                player.sendMessage("§7Ketik §cCANCEL §7untuk membatalkan.");
                player.sendMessage(" ");
            }

            case 22 -> {
                if (kingdoms.getKingdomByPlayer(player.getUniqueId()).isPresent()) {
                    gui.openProfile(player);
                } else {
                    player.sendMessage("§cKamu belum memiliki Kingdom.");
                }
            }

            case 24 -> gui.openInvitations(player);

            case 40 -> player.closeInventory();
        }
    }

    // ==========================================
    // PROFILE
    // ==========================================

    private void handleProfile(Player player, int slot) {
        switch (slot) {
            case 20 -> gui.openMembers(player, 0);

            case 22 -> gui.openLevel(player);

            case 24 -> {
                player.closeInventory();
                player.sendMessage("§eFitur Kingdom Home akan segera tersedia.");
            }

            case 30 -> gui.openMain(player);

            case 33 -> {
                var member = kingdoms.getMember(player.getUniqueId());

                if (member.isEmpty()) {
                    return;
                }

                if (member.get().getRole() == Role.OWNER) {
                    gui.openConfirmation(
                            player,
                            "DELETE",
                            player.getUniqueId().toString()
                    );
                } else {
                    player.closeInventory();

                    if (kingdoms.leaveKingdom(player.getUniqueId())) {
                        player.sendMessage("§aKamu berhasil keluar dari Kingdom.");
                    } else {
                        player.sendMessage("§cGagal keluar dari Kingdom.");
                    }
                }
            }
        }
    }

    // ==========================================
    // LEVEL
    // ==========================================

    private void handleLevel(Player player, int slot) {
        if (slot == 45) {
            gui.openProfile(player);
        }
    }

    // ==========================================
    // MEMBERS
    // ==========================================

    private void handleMembers(
            Player player,
            KingdomMenuHolder holder,
            int slot
    ) {
        var kingdom = kingdoms.getKingdomByPlayer(player.getUniqueId());

        if (kingdom.isEmpty()) {
            player.closeInventory();
            return;
        }

        if (slot == 45) {
            gui.openProfile(player);
            return;
        }

        if (slot == 48) {
            gui.openMembers(player, Math.max(0, holder.getPage() - 1));
            return;
        }

        if (slot == 50) {
            gui.openMembers(player, holder.getPage() + 1);
            return;
        }

        if (slot == 49) {
            if (canInvite(player)) {
                gui.openInvitePlayers(player);
            } else {
                player.sendMessage("§cKamu tidak memiliki izin mengundang anggota.");
            }
            return;
        }

        ItemStack item = eventItem(player, slot);

        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasLore()) {
            return;
        }

        for (String line : meta.getLore()) {
            if (line.startsWith("§0UUID:")) {
                String rawUuid = line.substring("§0UUID:".length());

                try {
                    UUID target = UUID.fromString(rawUuid);

                    if (target.equals(player.getUniqueId())) {
                        player.sendMessage("§cKamu tidak dapat mengatur dirimu sendiri.");
                        return;
                    }

                    gui.openMemberActions(player, target);
                } catch (IllegalArgumentException ignored) {
                    player.sendMessage("§cData anggota tidak valid.");
                }

                return;
            }
        }
    }

    private ItemStack eventItem(Player player, int slot) {
        return player.getOpenInventory()
                .getTopInventory()
                .getItem(slot);
    }

    // ==========================================
    // INVITE PLAYERS
    // ==========================================

    private void handleInvitePlayers(Player player, int slot) {
        if (slot == 45) {
            gui.openMembers(player, 0);
            return;
        }

        ItemStack item = eventItem(player, slot);

        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasLore()) {
            return;
        }

        for (String line : meta.getLore()) {
            if (line.startsWith("§0UUID:")) {
                try {
                    UUID target = UUID.fromString(
                            line.substring("§0UUID:".length())
                    );

                    if (kingdoms.invitePlayer(
                            player.getUniqueId(),
                            target,
                            Bukkit.getOfflinePlayer(target).getName()
                    )) {
                        player.sendMessage("§aUndangan Kingdom berhasil dikirim.");
                        player.playSound(
                                player.getLocation(),
                                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                                1f,
                                1.5f
                        );
                    } else {
                        player.sendMessage("§cGagal mengirim undangan.");
                    }

                } catch (IllegalArgumentException exception) {
                    player.sendMessage("§cData pemain tidak valid.");
                }

                return;
            }
        }
    }

    // ==========================================
    // MEMBER ACTIONS
    // ==========================================

    private void handleMemberActions(
            Player player,
            KingdomMenuHolder holder,
            int slot
    ) {
        if (holder.getTarget() == null) {
            gui.openMembers(player, 0);
            return;
        }

        UUID target;

        try {
            target = UUID.fromString(holder.getTarget());
        } catch (IllegalArgumentException exception) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 20 -> {
                if (kingdoms.promoteMember(player.getUniqueId(), target)) {
                    player.sendMessage("§aJabatan anggota berhasil dinaikkan.");
                } else {
                    player.sendMessage("§cTidak dapat menaikkan jabatan anggota.");
                }

                gui.openMembers(player, 0);
            }

            case 22 -> {
                if (kingdoms.demoteMember(player.getUniqueId(), target)) {
                    player.sendMessage("§aJabatan anggota berhasil diturunkan.");
                } else {
                    player.sendMessage("§cTidak dapat menurunkan jabatan anggota.");
                }

                gui.openMembers(player, 0);
            }

            case 24 -> gui.openConfirmation(
                    player,
                    "KICK",
                    target.toString()
            );

            case 30 -> {
                var member = kingdoms.getMember(player.getUniqueId());

                if (member.isEmpty() ||
                        member.get().getRole() != Role.OWNER) {
                    player.sendMessage("§cHanya Owner yang dapat memindahkan kepemilikan.");
                    return;
                }

                gui.openConfirmation(
                        player,
                        "TRANSFER",
                        target.toString()
                );
            }

            case 45 -> gui.openMembers(player, 0);
        }
    }

    // ==========================================
    // INVITATIONS
    // ==========================================

    private void handleInvitations(
            Player player,
            KingdomMenuHolder holder,
            int slot
    ) {
        if (slot == 45) {
            gui.openMain(player);
            return;
        }

        List<Invitation> invitations =
                kingdoms.getInvitations(player.getUniqueId());

        // Slot undangan harus sama dengan slot yang dipakai GUI.
        // GUI manager saat ini menampilkan undangan dari slot 10.
        int invitationIndex = slot - 10;

        if (invitationIndex < 0 ||
                invitationIndex >= invitations.size() ||
                slot > 43) {
            return;
        }

        Invitation invitation = invitations.get(invitationIndex);

        // Klik kiri menerima undangan.
        if (player.getOpenInventory().getItem(slot) == null) {
            return;
        }

        // Klik kanan menolak undangan.
        if (kingdoms.denyInvitation(
                player.getUniqueId(),
                invitation.id()
        )) {
            player.sendMessage("§cUndangan Kingdom berhasil ditolak.");
            player.playSound(
                    player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS,
                    1f,
                    1f
            );
            gui.openInvitations(player);
            return;
        }

        player.sendMessage("§cGagal menolak undangan.");
    }

    // ==========================================
    // CONFIRMATION
    // ==========================================

    private void handleConfirmation(
            Player player,
            KingdomMenuHolder holder,
            int slot
    ) {
        if (slot == 11) {
            player.closeInventory();
            return;
        }

        if (slot != 15 || holder.getTarget() == null) {
            return;
        }

        String action = holder.getTarget();

        // Target konfirmasi disimpan dalam format ACTION:UUID
        if (action.startsWith("DELETE:")) {
            if (kingdoms.deleteKingdom(player.getUniqueId())) {
                player.sendMessage("§aKingdom berhasil dihapus.");
            } else {
                player.sendMessage("§cGagal menghapus Kingdom.");
            }

        } else if (action.startsWith("KICK:")) {
            UUID target = parseTarget(action);

            if (target != null &&
                    kingdoms.kickMember(player.getUniqueId(), target)) {
                player.sendMessage("§aAnggota berhasil dikeluarkan.");
            } else {
                player.sendMessage("§cGagal mengeluarkan anggota.");
            }

        } else if (action.startsWith("TRANSFER:")) {
            UUID target = parseTarget(action);

            if (target != null &&
                    kingdoms.transferOwnership(player.getUniqueId(), target)) {
                player.sendMessage("§aKepemilikan Kingdom berhasil dipindahkan.");
            } else {
                player.sendMessage("§cGagal memindahkan kepemilikan.");
            }
        }

        player.closeInventory();
    }

    private UUID parseTarget(String action) {
        try {
            return UUID.fromString(action.substring(action.indexOf(':') + 1));
        } catch (Exception exception) {
            return null;
        }
    }

    // ==========================================
    // CHAT CREATION
    // ==========================================

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!creationRequests.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String message = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase("CANCEL")) {
                creationRequests.remove(player.getUniqueId());
                player.sendMessage("§cPembuatan Kingdom dibatalkan.");
                return;
            }

            if (kingdoms.getKingdomByPlayer(player.getUniqueId()).isPresent()) {
                creationRequests.remove(player.getUniqueId());
                player.sendMessage("§cKamu sudah memiliki Kingdom.");
                return;
            }

            boolean success = kingdoms.createKingdom(
                    player.getUniqueId(),
                    player.getName(),
                    message
            );

            creationRequests.remove(player.getUniqueId());

            if (success) {
                player.sendMessage("§aKingdom §6" + message +
                        " §aberhasil dibuat!");
                player.playSound(
                        player.getLocation(),
                        Sound.UI_TOAST_CHALLENGE_COMPLETE,
                        1f,
                        1f
                );

                gui.openMain(player);
            } else {
                player.sendMessage("§cGagal membuat Kingdom.");
                player.sendMessage("§7Nama harus 3-16 karakter dan hanya boleh huruf, angka, serta underscore.");
            }
        });
    }

    // ==========================================
    // DRAG / QUIT
    // ==========================================

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder()
                instanceof KingdomMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        creationRequests.remove(event.getPlayer().getUniqueId());
    }

    // ==========================================
    // PERMISSIONS
    // ==========================================

    private boolean canInvite(Player player) {
        var member = kingdoms.getMember(player.getUniqueId());

        if (member.isEmpty()) {
            return false;
        }

        Role role = member.get().getRole();

        return role == Role.OWNER ||
                role == Role.LEADER ||
                role == Role.OFFICER;
    }
}
