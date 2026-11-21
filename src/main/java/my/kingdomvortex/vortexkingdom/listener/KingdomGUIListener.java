
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.gui.KingdomGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomMenuHolder;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember.Role;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KingdomGUIListener implements Listener {

    private static final String PREFIX = "§8[§6Kingdom§8] §7";

    private static final int[] MEMBER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final VortexKingdom plugin;
    private final KingdomGUIManager gui;
    private final KingdomManager kingdoms;

    private final Set<UUID> creationRequests =
            ConcurrentHashMap.newKeySet();

    public KingdomGUIListener(
            VortexKingdom plugin,
            KingdomGUIManager gui
    ) {
        this.plugin = plugin;
        this.gui = gui;
        this.kingdoms = plugin.getKingdomManager();
    }

    // ==================================================
    // INVENTORY CLICK
    // ==================================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder()
                instanceof KingdomMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || event.getClickedInventory()
                != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 54) {
            return;
        }

        switch (holder.getMenu()) {
            case MAIN -> handleMain(player, slot);
            case PROFILE -> handleProfile(player, slot);
            case LEVEL -> handleLevel(player, slot);
            case MEMBERS -> handleMembers(player, holder, slot);
            case INVITE_PLAYERS -> handleInvitePlayers(player, slot);
            case MEMBER_ACTIONS -> handleMemberActions(player, holder, slot);
            case INVITATIONS -> handleInvitations(player, event, slot);
            case CONFIRMATION -> handleConfirmation(player, holder, slot);
        }
    }

    // ==================================================
    // MAIN MENU
    // ==================================================

    private void handleMain(Player player, int slot) {
        boolean hasKingdom = kingdoms.getKingdomByPlayer(
                player.getUniqueId()
        ).isPresent();

        switch (slot) {
            case 10 -> {
                if (hasKingdom) {
                    gui.openProfile(player);
                } else {
                    player.sendMessage(
                            PREFIX + "§cKamu belum memiliki Kingdom."
                    );
                }
            }

            case 12 -> {
                if (hasKingdom) {
                    gui.openMembers(player, 0);
                } else {
                    player.sendMessage(
                            PREFIX + "§cKamu belum memiliki Kingdom."
                    );
                }
            }

            case 14 -> {
                if (hasKingdom) {
                    gui.openLevel(player);
                } else {
                    player.sendMessage(
                            PREFIX + "§cKamu belum memiliki Kingdom."
                    );
                }
            }

            case 16 -> {
                player.closeInventory();
                player.sendMessage(
                        PREFIX + "§eFitur Kingdom Home belum tersedia."
                );
            }

            case 22 -> startCreation(player, hasKingdom);

            case 38 -> plugin.getLeaderboardGUIManager().open(
                    player,
                    my.kingdomvortex.vortexkingdom.manager
                            .KingdomLeaderboardManager.LeaderboardType.LEVEL,
                    0
            );

            case 40 -> gui.openInvitations(player);

            case 42 -> player.closeInventory();
        }
    }

    private void startCreation(Player player, boolean hasKingdom) {
        if (hasKingdom) {
            player.sendMessage(
                    PREFIX + "§cKamu sudah memiliki Kingdom."
            );
            return;
        }

        if (!creationRequests.add(player.getUniqueId())) {
            player.sendMessage(
                    PREFIX + "§cKamu sedang dalam proses membuat Kingdom."
            );
            return;
        }

        player.closeInventory();

        player.sendMessage(" ");
        player.sendMessage("§6§lKINGDOM CREATION");
        player.sendMessage(
                "§7Masukkan nama Kingdom yang ingin kamu buat."
        );
        player.sendMessage("§7Contoh: §eNovaEmpire");
        player.sendMessage("§7Ketik §cCANCEL §7untuk membatalkan.");
        player.sendMessage(" ");
    }

    // ==================================================
    // PROFILE
    // ==================================================

    private void handleProfile(Player player, int slot) {
        switch (slot) {
            case 29 -> gui.openMembers(player, 0);

            case 31 -> gui.openLevel(player);

            case 33 -> {
                var member = kingdoms.getMember(player.getUniqueId());

                if (member.isEmpty()) {
                    player.closeInventory();
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
                        player.sendMessage(
                                PREFIX + "§aKamu berhasil keluar dari Kingdom."
                        );
                        player.playSound(
                                player.getLocation(),
                                Sound.ENTITY_PLAYER_LEVELUP,
                                1f,
                                1.2f
                        );
                    } else {
                        player.sendMessage(
                                PREFIX + "§cGagal keluar dari Kingdom."
                        );
                    }
                }
            }

            case 49 -> gui.openMain(player);
        }
    }

    // ==================================================
    // LEVEL
    // ==================================================

    private void handleLevel(Player player, int slot) {
        if (slot == 49) {
            gui.openProfile(player);
        }
    }

    // ==================================================
    // MEMBERS
    // ==================================================

    private void handleMembers(
            Player player,
            KingdomMenuHolder holder,
            int slot
    ) {
        if (kingdoms.getKingdomByPlayer(
                player.getUniqueId()).isEmpty()) {
            player.closeInventory();
            return;
        }

        if (slot == 45) {
            gui.openProfile(player);
            return;
        }

        if (slot == 48) {
            gui.openMembers(player, holder.getPage() - 1);
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
                player.sendMessage(
                        PREFIX + "§cKamu tidak memiliki izin mengundang."
                );
            }
            return;
        }

        if (!isMemberSlot(slot)) {
            return;
        }

        ItemStack item = eventItem(player, slot);
        UUID target = readUuid(item, "UUID:");

        if (target == null) {
            return;
        }

        if (target.equals(player.getUniqueId())) {
            player.sendMessage(
                    PREFIX + "§cKamu tidak dapat mengelola dirimu sendiri."
            );
            return;
        }

        gui.openMemberActions(player, target);
    }

    // ==================================================
    // INVITE PLAYERS
    // ==================================================

    private void handleInvitePlayers(Player player, int slot) {
        if (slot == 49) {
            gui.openMembers(player, 0);
            return;
        }

        if (!isMemberSlot(slot)) {
            return;
        }

        if (!canInvite(player)) {
            player.sendMessage(
                    PREFIX + "§cKamu tidak memiliki izin mengundang."
            );
            return;
        }

        UUID target = readUuid(
                eventItem(player, slot),
                "UUID:"
        );

        if (target == null || target.equals(player.getUniqueId())) {
            return;
        }

        Player onlineTarget = Bukkit.getPlayer(target);

        if (onlineTarget == null) {
            player.sendMessage(
                    PREFIX + "§cPemain tersebut sudah offline."
            );
            gui.openInvitePlayers(player);
            return;
        }

        if (kingdoms.invitePlayer(
                player.getUniqueId(),
                target,
                onlineTarget.getName()
        )) {
            player.sendMessage(
                    PREFIX + "§aUndangan berhasil dikirim kepada §f"
                            + onlineTarget.getName() + "§a."
            );

            onlineTarget.sendMessage(" ");
            onlineTarget.sendMessage("§6§lKINGDOM INVITATION");
            onlineTarget.sendMessage(
                    "§7Kamu menerima undangan Kingdom."
            );
            onlineTarget.sendMessage(
                    "§7Gunakan §e/kingdom §7untuk melihat undangan."
            );
            onlineTarget.sendMessage(" ");

            onlineTarget.playSound(
                    onlineTarget.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_PLING,
                    1f,
                    1.5f
            );

            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    1f,
                    1.5f
            );
        } else {
            player.sendMessage(
                    PREFIX + "§cGagal mengirim undangan. Periksa izin, "
                            + "kapasitas, atau undangan yang sudah ada."
            );
        }

        gui.openInvitePlayers(player);
    }

    // ==================================================
    // MEMBER ACTIONS
    // ==================================================

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

        var actorMember = kingdoms.getMember(player.getUniqueId());
        var targetMember = kingdoms.getMember(target);

        if (actorMember.isEmpty() || targetMember.isEmpty()) {
            player.closeInventory();
            return;
        }

        if (actorMember.get().getKingdomId()
                != targetMember.get().getKingdomId()) {
            player.closeInventory();
            return;
        }

        if (target.equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }

        if (!canManage(
                actorMember.get().getRole(),
                targetMember.get().getRole()
        )) {
            player.sendMessage(
                    PREFIX + "§cKamu tidak memiliki izin mengelola anggota ini."
            );
            gui.openMembers(player, 0);
            return;
        }

        switch (slot) {
            case 20 -> {
                if (kingdoms.promoteMember(
                        player.getUniqueId(), target)) {
                    player.sendMessage(
                            PREFIX + "§aJabatan anggota berhasil dinaikkan."
                    );
                } else {
                    player.sendMessage(
                            PREFIX + "§cTidak dapat menaikkan jabatan anggota."
                    );
                }

                gui.openMembers(player, 0);
            }

            case 22 -> {
                if (kingdoms.demoteMember(
                        player.getUniqueId(), target)) {
                    player.sendMessage(
                            PREFIX + "§aJabatan anggota berhasil diturunkan."
                    );
                } else {
                    player.sendMessage(
                            PREFIX + "§cTidak dapat menurunkan jabatan anggota."
                    );
                }

                gui.openMembers(player, 0);
            }

            case 24 -> gui.openConfirmation(
                    player,
                    "KICK",
                    target.toString()
            );

            case 31 -> {
                if (actorMember.get().getRole() != Role.OWNER) {
                    player.sendMessage(
                            PREFIX + "§cHanya Owner yang dapat memindahkan kepemilikan."
                    );
                    return;
                }

                gui.openConfirmation(
                        player,
                        "TRANSFER",
                        target.toString()
                );
            }

            case 49 -> gui.openMembers(player, 0);
        }
    }

    // ==================================================
    // INVITATIONS
    // ==================================================

    private void handleInvitations(
            Player player,
            InventoryClickEvent event,
            int slot
    ) {
        if (slot == 49) {
            gui.openMain(player);
            return;
        }

        if (!isMemberSlot(slot)) {
            return;
        }

        ItemStack item = event.getCurrentItem();

        if (item == null
                || item.getType() != Material.WRITABLE_BOOK) {
            return;
        }

        Integer invitationId = readInteger(
                item,
                "INVITATION_ID:"
        );

        if (invitationId == null) {
            return;
        }

        if (event.isLeftClick()) {
            boolean accepted = kingdoms.acceptInvitation(
                    player.getUniqueId(),
                    invitationId
            );

            if (accepted) {
                player.sendMessage(
                        PREFIX + "§aKamu berhasil bergabung dengan Kingdom!"
                );

                player.playSound(
                        player.getLocation(),
                        Sound.UI_TOAST_CHALLENGE_COMPLETE,
                        1f,
                        1f
                );

                gui.openMain(player);
            } else {
                player.sendMessage(
                        PREFIX + "§cGagal menerima undangan. Undangan mungkin "
                                + "kedaluwarsa, kapasitas penuh, atau kamu "
                                + "sudah memiliki Kingdom."
                );

                gui.openInvitations(player);
            }

        } else if (event.isRightClick()) {
            boolean denied = kingdoms.denyInvitation(
                    player.getUniqueId(),
                    invitationId
            );

            if (denied) {
                player.sendMessage(
                        PREFIX + "§cUndangan berhasil ditolak."
                );

                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_BASS,
                        1f,
                        1f
                );
            } else {
                player.sendMessage(
                        PREFIX + "§cGagal menolak undangan."
                );
            }

            gui.openInvitations(player);
        }
    }

    // ==================================================
    // CONFIRMATION
    // ==================================================

    private void handleConfirmation(
            Player player,
            KingdomMenuHolder holder,
            int slot
    ) {
        if (slot == 11) {
            gui.openMain(player);
            return;
        }

        if (slot != 15 || holder.getTarget() == null) {
            return;
        }

        String[] parts = holder.getTarget().split(":", 2);

        if (parts.length != 2) {
            player.closeInventory();
            return;
        }

        String action = parts[0];

        UUID target;

        try {
            target = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            player.closeInventory();
            return;
        }

        switch (action) {
            case "DELETE" -> {
                if (!target.equals(player.getUniqueId())) {
                    player.sendMessage(
                            PREFIX + "§cTarget konfirmasi tidak valid."
                    );
                    player.closeInventory();
                    return;
                }

                if (kingdoms.deleteKingdom(player.getUniqueId())) {
                    player.sendMessage(
                            PREFIX + "§aKingdom berhasil dihapus."
                    );
                } else {
                    player.sendMessage(
                            PREFIX + "§cGagal menghapus Kingdom."
                    );
                }
            }

            case "KICK" -> {
                if (kingdoms.kickMember(
                        player.getUniqueId(), target)) {
                    player.sendMessage(
                            PREFIX + "§aAnggota berhasil dikeluarkan."
                    );
                } else {
                    player.sendMessage(
                            PREFIX + "§cGagal mengeluarkan anggota."
                    );
                }
            }

            case "TRANSFER" -> {
                if (kingdoms.transferOwnership(
                        player.getUniqueId(), target)) {
                    player.sendMessage(
                            PREFIX + "§aKepemilikan Kingdom berhasil dipindahkan."
                    );
                } else {
                    player.sendMessage(
                            PREFIX + "§cGagal memindahkan kepemilikan."
                    );
                }
            }

            default -> player.sendMessage(
                    PREFIX + "§cAksi tidak dikenal."
            );
        }

        player.closeInventory();
    }

    // ==================================================
    // KINGDOM CREATION CHAT
    // ==================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!creationRequests.remove(uuid)) {
            return;
        }

        event.setCancelled(true);

        String message = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (message.equalsIgnoreCase("CANCEL")) {
                player.sendMessage(
                        PREFIX + "§cPembuatan Kingdom dibatalkan."
                );
                return;
            }

            if (kingdoms.getKingdomByPlayer(uuid).isPresent()) {
                player.sendMessage(
                        PREFIX + "§cKamu sudah memiliki Kingdom."
                );
                return;
            }

            if (kingdoms.createKingdom(
                    uuid,
                    player.getName(),
                    message
            )) {
                player.sendMessage(
                        PREFIX + "§aKingdom §6" + message
                                + " §aberhasil dibuat!"
                );

                player.playSound(
                        player.getLocation(),
                        Sound.UI_TOAST_CHALLENGE_COMPLETE,
                        1f,
                        1f
                );

                gui.openMain(player);
            } else {
                creationRequests.add(uuid);

                player.sendMessage(
                        PREFIX + "§cNama Kingdom tidak valid atau gagal dibuat."
                );
                player.sendMessage(
                        PREFIX + "§7Gunakan 3-16 karakter: huruf, angka, "
                                + "atau underscore."
                );
                player.sendMessage(
                        PREFIX + "§7Masukkan nama lain atau ketik §cCANCEL§7."
                );
            }
        });
    }

    // ==================================================
    // INVENTORY DRAG
    // ==================================================

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder()
                instanceof KingdomMenuHolder) {
            event.setCancelled(true);
        }
    }

    // ==================================================
    // PLAYER QUIT
    // ==================================================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        creationRequests.remove(
                event.getPlayer().getUniqueId()
        );
    }

    // ==================================================
    // PERMISSION HELPERS
    // ==================================================

    private boolean canInvite(Player player) {
        var member = kingdoms.getMember(player.getUniqueId());

        if (member.isEmpty()) {
            return false;
        }

        Role role = member.get().getRole();

        return role == Role.OWNER
                || role == Role.LEADER
                || role == Role.OFFICER;
    }

    private boolean canManage(Role actor, Role target) {
        return switch (actor) {
            case OWNER -> target != Role.OWNER;
            case LEADER ->
                    target == Role.OFFICER || target == Role.MEMBER;
            case OFFICER -> target == Role.MEMBER;
            default -> false;
        };
    }

    private boolean isMemberSlot(int slot) {
        for (int memberSlot : MEMBER_SLOTS) {
            if (memberSlot == slot) {
                return true;
            }
        }

        return false;
    }

    // ==================================================
    // ITEM DATA HELPERS
    // ==================================================

    private ItemStack eventItem(Player player, int slot) {
        return player.getOpenInventory()
                .getTopInventory()
                .getItem(slot);
    }

    private UUID readUuid(ItemStack item, String prefix) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || meta.getLore() == null) {
            return null;
        }

        String marker = "§0" + prefix;

        for (String line : meta.getLore()) {
            if (line.startsWith(marker)) {
                try {
                    return UUID.fromString(
                            line.substring(marker.length())
                    );
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private Integer readInteger(ItemStack item, String prefix) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || meta.getLore() == null) {
            return null;
        }

        String marker = "§0" + prefix;

        for (String line : meta.getLore()) {
            if (line.startsWith(marker)) {
                try {
                    return Integer.parseInt(
                            line.substring(marker.length())
                    );
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }
}
