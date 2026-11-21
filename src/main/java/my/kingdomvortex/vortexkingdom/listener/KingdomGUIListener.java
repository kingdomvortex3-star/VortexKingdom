
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.gui.KingdomGUIManager;
import my.kingdomvortex.vortexkingdom.gui.KingdomMenuHolder;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KingdomGUIListener implements Listener {

    private final VortexKingdom plugin;
    private final KingdomGUIManager guiManager;
    private final KingdomManager kingdomManager;

    private final Map<UUID, Boolean> creatingKingdom =
            new ConcurrentHashMap<>();

    private static final int[] MEMBER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public KingdomGUIListener(
            VortexKingdom plugin,
            KingdomGUIManager guiManager
    ) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.kingdomManager = plugin.getKingdomManager();
    }

    // ==========================================
    // INVENTORY CLICK
    // ==========================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof KingdomMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != top) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.getMenu()) {
            case MAIN -> handleMain(player, slot);
            case PROFILE -> handleProfile(player, slot);
            case LEVEL -> handleLevel(player, slot);
            case MEMBERS -> handleMembers(player, holder, slot);
            case INVITE_PLAYERS -> handleInvitePlayers(player, slot);
            case MEMBER_ACTIONS ->
                    handleMemberActions(player, holder, slot);
            case INVITATIONS ->
                    handleInvitations(player, slot, event.isRightClick());
            case CONFIRMATION ->
                    handleConfirmation(player, holder, slot);
        }
    }

    // ==========================================
    // MAIN MENU
    // ==========================================

    private void handleMain(Player player, int slot) {

        switch (slot) {
            case 10 -> guiManager.openProfile(player);

            case 12 -> {
                if (hasKingdom(player)) {
                    guiManager.openMembers(player, 0);
                }
            }

            case 14 -> guiManager.openLevelMenu(player);

            case 16 -> player.sendMessage(
                    "§cKingdom Home belum tersedia."
            );

            case 22 -> {
                if (hasKingdom(player)) {
                    player.sendMessage(
                            "§cKamu sudah memiliki Kingdom."
                    );
                    return;
                }

                creatingKingdom.put(player.getUniqueId(), true);
                player.closeInventory();

                player.sendMessage("");
                player.sendMessage("§6§lKINGDOM CREATION");
                player.sendMessage(
                        "§7Masukkan nama Kingdom di chat."
                );
                player.sendMessage("§7Format: §f3-16 karakter");
                player.sendMessage(
                        "§7Gunakan huruf, angka, dan underscore."
                );
                player.sendMessage(
                        "§cKetik §fcancel §cuntuk membatalkan."
                );
                player.sendMessage("");
            }

            case 31 -> player.sendMessage(
                    "§7Kingdom System §8» §fSistem kerajaan dengan level, EXP, dan anggota."
            );

            case 38 -> player.sendMessage(
                    "§cLeaderboard belum disambungkan."
            );

            case 40 -> guiManager.openInvitations(player);

            case 42 -> player.closeInventory();
        }
    }

    // ==========================================
    // PROFILE MENU
    // ==========================================

    private void handleProfile(Player player, int slot) {

        switch (slot) {
            case 29 -> guiManager.openMembers(player, 0);

            case 31 -> guiManager.openLevelMenu(player);

            case 33 -> {
                var kingdomOptional =
                        kingdomManager.getKingdomByPlayer(
                                player.getUniqueId()
                        );

                if (kingdomOptional.isEmpty()) {
                    player.sendMessage(
                            "§cKamu tidak memiliki Kingdom."
                    );
                    return;
                }

                Kingdom kingdom = kingdomOptional.get();

                if (kingdom.getOwnerUuid().equals(
                        player.getUniqueId().toString())) {

                    openConfirmation(
                            player,
                            "DELETE",
                            "§c§lDELETE KINGDOM",
                            "§7Apakah kamu yakin ingin menghapus Kingdom?"
                    );

                } else {
                    boolean success =
                            kingdomManager.leaveKingdom(
                                    player.getUniqueId()
                            );

                    if (success) {
                        player.closeInventory();
                        player.sendMessage(
                                "§aKamu berhasil keluar dari Kingdom."
                        );
                    } else {
                        player.sendMessage(
                                "§cGagal keluar dari Kingdom."
                        );
                    }
                }
            }

            case 49 -> guiManager.openMainMenu(player);
        }
    }

    // ==========================================
    // LEVEL MENU
    // ==========================================

    private void handleLevel(Player player, int slot) {
        if (slot == 49) {
            guiManager.openMainMenu(player);
        }
    }

    // ==========================================
    // MEMBERS MENU
    // ==========================================

    private void handleMembers(
            Player player,
            KingdomMenuHolder holder,
            int slot
    ) {

        if (slot == 45) {
            guiManager.openMainMenu(player);
            return;
        }

        if (slot == 48) {
            guiManager.openMembers(
                    player, holder.getPage() - 1
            );
            return;
        }

        if (slot == 49) {
            guiManager.openInvitePlayers(player);
            return;
        }

        if (slot == 50) {
            guiManager.openMembers(
                    player, holder.getPage() + 1
            );
            return;
        }

        if (slot == 53) {
            player.closeInventory();
            return;
        }

        int memberIndex = getMemberIndex(slot);

        if (memberIndex < 0) {
            return;
        }

        var kingdomOptional =
                kingdomManager.getKingdomByPlayer(
                        player.getUniqueId()
                );

        if (kingdomOptional.isEmpty()) {
            player.sendMessage(
                    "§cKamu tidak memiliki Kingdom."
            );
            return;
        }

        List<KingdomMember> members =
                kingdomManager.getMembers(
                        kingdomOptional.get().getId()
                );

        int actualIndex =
                holder.getPage() * MEMBER_SLOTS.length + memberIndex;

        if (actualIndex < 0 || actualIndex >= members.size()) {
            return;
        }

        KingdomMember target = members.get(actualIndex);

        UUID targetUuid = target.getPlayerUniqueId();

        if (targetUuid == null) {
            player.sendMessage(
                    "§cUUID anggota tidak valid."
            );
            return;
        }

        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(
                    "§cKamu tidak bisa mengelola dirimu sendiri."
            );
            return;
        }

        guiManager.openMemberActions(player, targetUuid);
    }

    // ==========================================
    // INVITE PLAYERS MENU
    // ==========================================

    private void handleInvitePlayers(
            Player player,
            int slot
    ) {

        if (slot == 49) {
            guiManager.openMembers(player, 0);
            return;
        }

        if (getMemberIndex(slot) < 0) {
            return;
        }

        ItemStack item = player.getOpenInventory()
                .getTopInventory().getItem(slot);

        if (item == null ||
                item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null ||
                meta.getDisplayName() == null) {
            return;
        }

        String targetName =
                ChatColor.stripColor(meta.getDisplayName());

        if (targetName == null || targetName.isBlank()) {
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            player.sendMessage(
                    "§cPlayer tersebut sudah offline."
            );
            guiManager.openInvitePlayers(player);
            return;
        }

        boolean success = kingdomManager.invitePlayer(
                player.getUniqueId(),
                target.getUniqueId(),
                target.getName()
        );

        if (success) {
            player.sendMessage(
                    "§aUndangan berhasil dikirim kepada §f"
                            + target.getName() + "§a."
            );

            target.sendMessage("");
            target.sendMessage("§6§lKINGDOM INVITATION");
            target.sendMessage(
                    "§7Kamu menerima undangan Kingdom."
            );
            target.sendMessage(
                    "§7Gunakan §f/kingdom §7untuk membuka menu undangan."
            );
            target.sendMessage("");

        } else {
            player.sendMessage(
                    "§cGagal mengundang player. Periksa role, kapasitas, dan undangan yang masih aktif."
            );
        }

        guiManager.openInvitePlayers(player);
    }

    // ==========================================
    // MEMBER ACTIONS MENU
    // ==========================================

    private void handleMemberActions(
            Player player,
            KingdomMenuHolder holder,
            int slot
    ) {

        UUID target;

        try {
            target = UUID.fromString(holder.getTarget());
        } catch (Exception exception) {
            player.closeInventory();
            player.sendMessage(
                    "§cTarget anggota tidak valid."
            );
            return;
        }

        if (slot == 49) {
            guiManager.openMembers(player, 0);
            return;
        }

        if (slot == 20) {
            boolean success = kingdomManager.promoteMember(
                    player.getUniqueId(), target
            );

            if (success) {
                player.sendMessage(
                        "§aRole anggota berhasil dinaikkan."
                );
            } else {
                player.sendMessage(
                        "§cTidak dapat promote anggota. Periksa permission dan role."
                );
            }

            guiManager.openMemberActions(player, target);
            return;
        }

        if (slot == 22) {
            boolean success = kingdomManager.demoteMember(
                    player.getUniqueId(), target
            );

            if (success) {
                player.sendMessage(
                        "§aRole anggota berhasil diturunkan."
                );
            } else {
                player.sendMessage(
                        "§cTidak dapat demote anggota. Periksa permission dan role."
                );
            }

            guiManager.openMemberActions(player, target);
            return;
        }

        if (slot == 24) {
            openConfirmation(
                    player,
                    "KICK:" + target,
                    "§c§lKICK MEMBER",
                    "§7Keluarkan anggota ini dari Kingdom?"
            );
            return;
        }

        if (slot == 31) {
            openConfirmation(
                    player,
                    "TRANSFER:" + target,
                    "§6§lTRANSFER OWNERSHIP",
                    "§7Pindahkan kepemilikan Kingdom?"
            );
        }
    }

    // ==========================================
    // INVITATIONS MENU
    // ==========================================

    private void handleInvitations(
            Player player,
            int slot,
            boolean rightClick
    ) {

        if (slot == 49) {
            guiManager.openMainMenu(player);
            return;
        }

        if (getMemberIndex(slot) < 0) {
            return;
        }

        List<KingdomManager.Invitation> invitations =
                kingdomManager.getInvitations(
                        player.getUniqueId()
                );

        if (invitations.isEmpty()) {
            player.sendMessage(
                    "§cUndangan sudah tidak tersedia."
            );
            guiManager.openInvitations(player);
            return;
        }

        /*
         * Untuk sementara manager masih memproses
         * undangan terbaru, bukan ID undangan individual.
         */

        if (rightClick) {
            boolean success =
                    kingdomManager.denyInvitation(
                            player.getUniqueId()
                    );

            if (success) {
                player.sendMessage(
                        "§cUndangan Kingdom berhasil ditolak."
                );
            } else {
                player.sendMessage(
                        "§cGagal menolak undangan."
                );
            }

        } else {
            boolean success =
                    kingdomManager.acceptInvitation(
                            player.getUniqueId()
                    );

            if (success) {
                player.sendMessage(
                        "§aKamu berhasil bergabung dengan Kingdom!"
                );

                var kingdom =
                        kingdomManager.getKingdomByPlayer(
                                player.getUniqueId()
                        );

                kingdom.ifPresent(value ->
                        player.sendMessage(
                                "§7Kingdom: §6" + value.getName()
                        )
                );

            } else {
                player.sendMessage(
                        "§cGagal menerima undangan. Mungkin sudah kedaluwarsa atau kapasitas penuh."
                );
            }
        }

        guiManager.openInvitations(player);
    }

    // ==========================================
    // CONFIRMATION MENU
    // ==========================================

    private void openConfirmation(
            Player player,
            String action,
            String title,
            String description
    ) {

        KingdomMenuHolder holder = new KingdomMenuHolder(
                KingdomMenuHolder.Menu.CONFIRMATION,
                0,
                action
        );

        Inventory inventory = Bukkit.createInventory(
                holder, 54, title
        );

        holder.setInventory(inventory);

        setItem(
                inventory,
                20,
                Material.LIME_CONCRETE,
                "§a§lCONFIRM",
                "§7Klik untuk melanjutkan."
        );

        setItem(
                inventory,
                22,
                Material.PAPER,
                "§e§lConfirmation",
                description
        );

        setItem(
                inventory,
                24,
                Material.RED_CONCRETE,
                "§c§lCANCEL",
                "§7Batalkan tindakan."
        );

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    private void handleConfirmation(
            Player player,
            KingdomMenuHolder holder,
            int slot
    ) {

        if (slot == 24) {
            guiManager.openMainMenu(player);
            return;
        }

        if (slot != 20) {
            return;
        }

        String action = holder.getTarget();

        if (action == null) {
            player.closeInventory();
            return;
        }

        if (action.equals("DELETE")) {
            boolean success = kingdomManager.deleteKingdom(
                    player.getUniqueId()
            );

            player.closeInventory();

            if (success) {
                player.sendMessage(
                        "§aKingdom berhasil dihapus."
                );
            } else {
                player.sendMessage(
                        "§cGagal menghapus Kingdom."
                );
            }

            return;
        }

        String[] parts = action.split(":", 2);

        if (parts.length != 2) {
            player.closeInventory();
            return;
        }

        UUID target;

        try {
            target = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            player.closeInventory();
            player.sendMessage(
                    "§cTarget tidak valid."
            );
            return;
        }

        switch (parts[0]) {
            case "KICK" -> {
                boolean success = kingdomManager.kickMember(
                        player.getUniqueId(), target
                );

                if (success) {
                    player.sendMessage(
                            "§aAnggota berhasil dikeluarkan."
                    );

                    Player onlineTarget = Bukkit.getPlayer(target);

                    if (onlineTarget != null) {
                        onlineTarget.sendMessage(
                                "§cKamu telah dikeluarkan dari Kingdom."
                        );
                    }
                } else {
                    player.sendMessage(
                            "§cGagal mengeluarkan anggota."
                    );
                }
            }

            case "TRANSFER" -> {
                boolean success = kingdomManager.transferOwnership(
                        player.getUniqueId(), target
                );

                if (success) {
                    player.sendMessage(
                            "§aOwnership Kingdom berhasil dipindahkan."
                    );

                    Player onlineTarget = Bukkit.getPlayer(target);

                    if (onlineTarget != null) {
                        onlineTarget.sendMessage(
                                "§6Kamu sekarang adalah Owner Kingdom!"
                        );
                    }
                } else {
                    player.sendMessage(
                            "§cGagal transfer ownership. Hanya Owner yang dapat melakukan tindakan ini."
                    );
                }
            }

            default -> player.sendMessage(
                    "§cTindakan tidak dikenal."
            );
        }

        guiManager.openMembers(player, 0);
    }

    // ==========================================
    // KINGDOM CREATION CHAT
    // ==========================================

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!creatingKingdom.containsKey(uuid)) {
            return;
        }

        event.setCancelled(true);

        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            creatingKingdom.remove(uuid);

            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(
                            "§cPembuatan Kingdom dibatalkan."
                    )
            );

            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {

            if (!player.isOnline()) {
                creatingKingdom.remove(uuid);
                return;
            }

            if (hasKingdom(player)) {
                creatingKingdom.remove(uuid);
                player.sendMessage(
                        "§cKamu sudah memiliki Kingdom."
                );
                return;
            }

            boolean success = kingdomManager.createKingdom(
                    uuid,
                    player.getName(),
                    input
            );

            creatingKingdom.remove(uuid);

            if (success) {
                player.sendMessage("");
                player.sendMessage("§6§lKINGDOM CREATED");
                player.sendMessage(
                        "§7Kingdom: §e" + input
                );
                player.sendMessage("§7Level: §a1");
                player.sendMessage("§7Capacity: §a5");
                player.sendMessage("");
                player.sendMessage(
                        "§aKingdom berhasil dibuat!"
                );

                guiManager.openMainMenu(player);

            } else {
                player.sendMessage("");
                player.sendMessage(
                        "§cGagal membuat Kingdom."
                );
                player.sendMessage(
                        "§7Nama mungkin sudah digunakan atau format tidak valid."
                );
                player.sendMessage(
                        "§7Gunakan 3-16 karakter: huruf, angka, underscore."
                );
                player.sendMessage("");
            }
        });
    }

    // ==========================================
    // INVENTORY DRAG
    // ==========================================

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        if (event.getView().getTopInventory()
                .getHolder() instanceof KingdomMenuHolder) {
            event.setCancelled(true);
        }
    }

    // ==========================================
    // PLAYER QUIT
    // ==========================================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        creatingKingdom.remove(
                event.getPlayer().getUniqueId()
        );
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private boolean hasKingdom(Player player) {
        return kingdomManager.getKingdomByPlayer(
                player.getUniqueId()
        ).isPresent();
    }

    private int getMemberIndex(int slot) {
        for (int i = 0; i < MEMBER_SLOTS.length; i++) {
            if (MEMBER_SLOTS[i] == slot) {
                return i;
            }
        }

        return -1;
    }

    private void setItem(
            Inventory inventory,
            int slot,
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    private void fillEmpty(Inventory inventory) {
        ItemStack filler = new ItemStack(
                Material.GRAY_STAINED_GLASS_PANE
        );

        ItemMeta meta = filler.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§r");
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
}
