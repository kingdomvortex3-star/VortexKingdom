
package my.kingdomvortex.vortexkingdom.gui;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember.Role;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager.Invitation;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class KingdomGUIManager {

    private final VortexKingdom plugin;
    private final KingdomManager kingdomManager;

    private static final int[] MEMBER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public KingdomGUIManager(VortexKingdom plugin) {
        this.plugin = plugin;
        this.kingdomManager = plugin.getKingdomManager();
    }

    // ==================================================
    // MAIN MENU
    // ==================================================

    public void openMain(Player player) {
        openMainMenu(player);
    }

    public void openMainMenu(Player player) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdomManager.getKingdomByPlayer(player.getUniqueId());

        KingdomMenuHolder holder =
                new KingdomMenuHolder(KingdomMenuHolder.Menu.MAIN);

        Inventory inventory = createInventory(
                holder,
                "§0✦ §6§lKINGDOM"
        );

        if (optional.isEmpty()) {
            setItem(
                    inventory, 22, Material.GOLDEN_HELMET,
                    "§6§lCreate Kingdom",
                    "§7Bangun kerajaanmu sendiri.",
                    "",
                    "§eKlik untuk membuat Kingdom."
            );

            setItem(
                    inventory, 31, Material.BOOK,
                    "§b§lKingdom Information",
                    "§7Buat Kingdom dan kumpulkan anggota.",
                    "§7Tingkatkan level untuk membuka",
                    "§7kapasitas anggota lebih besar."
            );
        } else {
            Kingdom kingdom = optional.get();

            setItem(
                    inventory, 10, Material.GOLDEN_HELMET,
                    "§6§lKingdom Profile",
                    "§7Nama: §f" + kingdom.getName(),
                    "§7Level: §e" + kingdom.getLevel(),
                    "",
                    "§eKlik untuk melihat profil."
            );

            setItem(
                    inventory, 12, Material.PLAYER_HEAD,
                    "§b§lKingdom Members",
                    "§7Kelola anggota Kingdom.",
                    "",
                    "§eKlik untuk membuka."
            );

            setItem(
                    inventory, 14, Material.EXPERIENCE_BOTTLE,
                    "§a§lKingdom Level",
                    "§7Level: §e" + kingdom.getLevel(),
                    "§7EXP: §f" + kingdom.getExperience(),
                    "",
                    "§eKlik untuk melihat progres."
            );

            setItem(
                    inventory, 16, Material.COMPASS,
                    "§d§lKingdom Home",
                    "§7Teleport menuju markas Kingdom.",
                    "",
                    "§eKlik untuk teleport."
            );

            setItem(
                    inventory, 31, Material.BOOK,
                    "§b§lKingdom Information",
                    "§7Nama: §f" + kingdom.getName(),
                    "§7Level: §e" + kingdom.getLevel(),
                    "§7Anggota: §f" +
                            kingdomManager.getMemberCount(kingdom.getId()),
                    "§7Kapasitas: §f" +
                            kingdomManager.getMaxMembers(kingdom.getLevel())
            );
        }

        setItem(
                inventory, 38, Material.GOLD_INGOT,
                "§6§lLeaderboard",
                "§7Lihat Kingdom terbaik.",
                "",
                "§eKlik untuk membuka."
        );

        setItem(
                inventory, 40, Material.WRITABLE_BOOK,
                "§e§lInvitations",
                "§7Periksa undangan Kingdom.",
                "",
                "§eKlik untuk membuka."
        );

        setItem(
                inventory, 42, Material.BARRIER,
                "§c§lClose",
                "§7Tutup menu."
        );

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    // ==================================================
    // PROFILE MENU
    // ==================================================

    public void openProfile(Player player) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        Kingdom kingdom = optional.get();

        Optional<KingdomMember> member =
                kingdomManager.getMember(player.getUniqueId());

        if (member.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        KingdomMenuHolder holder =
                new KingdomMenuHolder(KingdomMenuHolder.Menu.PROFILE);

        Inventory inventory = createInventory(
                holder,
                "§0✦ §6§lKINGDOM PROFILE"
        );

        setItem(
                inventory, 13, Material.GOLDEN_HELMET,
                "§6§l" + kingdom.getName(),
                "§7Owner: §f" + kingdom.getOwnerUuid(),
                "§7Level: §e" + kingdom.getLevel(),
                "§7EXP: §f" + kingdom.getExperience(),
                "§7Total EXP: §f" + kingdom.getTotalExperience(),
                "§7Total Kills: §f" + kingdom.getTotalKills(),
                "§7Members: §f" +
                        kingdomManager.getMemberCount(kingdom.getId()),
                "§7Capacity: §f" +
                        kingdomManager.getMaxMembers(kingdom.getLevel()),
                "§7Your Role: §e" + member.get().getRole().name()
        );

        setItem(
                inventory, 29, Material.PLAYER_HEAD,
                "§b§lManage Members",
                "§7Kelola anggota Kingdom.",
                "",
                "§eKlik untuk membuka."
        );

        setItem(
                inventory, 31, Material.EXPERIENCE_BOTTLE,
                "§a§lKingdom Level",
                "§7Lihat progres level."
        );

        boolean owner = member.get().getRole() == Role.OWNER;

        setItem(
                inventory, 33,
                owner ? Material.TNT : Material.OAK_DOOR,
                owner ? "§c§lDelete Kingdom" : "§c§lLeave Kingdom",
                owner
                        ? "§7Hapus Kingdom secara permanen."
                        : "§7Keluar dari Kingdom.",
                "",
                "§eKlik untuk melanjutkan."
        );

        setItem(
                inventory, 49, Material.ARROW,
                "§7Back to Main Menu"
        );

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    // ==================================================
    // LEVEL MENU
    // ==================================================

    public void openLevel(Player player) {
        openLevelMenu(player);
    }

    public void openLevelMenu(Player player) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        Kingdom kingdom = optional.get();

        KingdomMenuHolder holder =
                new KingdomMenuHolder(KingdomMenuHolder.Menu.LEVEL);

        Inventory inventory = createInventory(
                holder,
                "§0✦ §a§lKINGDOM LEVEL"
        );

        setItem(
                inventory, 13, Material.BEACON,
                "§a§lKingdom Level",
                "§7Level: §e" + kingdom.getLevel(),
                "§7Maximum Level: §f100"
        );

        setItem(
                inventory, 21, Material.EXPERIENCE_BOTTLE,
                "§b§lExperience",
                "§7Current EXP: §f" + kingdom.getExperience(),
                "§7Total EXP: §f" + kingdom.getTotalExperience()
        );

        setItem(
                inventory, 23, Material.KNOWLEDGE_BOOK,
                "§e§lNext Level",
                kingdom.getLevel() >= 100
                        ? "§aMaximum level reached!"
                        : "§7Next Level: §f" + (kingdom.getLevel() + 1)
        );

        setItem(
                inventory, 31, Material.CHEST,
                "§6§lMember Capacity",
                "§7Members: §f" +
                        kingdomManager.getMemberCount(kingdom.getId()),
                "§7Capacity: §f" +
                        kingdomManager.getMaxMembers(kingdom.getLevel()),
                "",
                "§7Capacity bertambah setiap level."
        );

        setItem(inventory, 49, Material.ARROW, "§7Back");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    // ==================================================
    // MEMBERS MENU
    // ==================================================

    public void openMembers(Player player, int page) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        Kingdom kingdom = optional.get();

        List<KingdomMember> members =
                kingdomManager.getMembers(kingdom.getId());

        int maxPage = Math.max(
                0,
                (members.size() - 1) / MEMBER_SLOTS.length
        );

        page = Math.max(0, Math.min(page, maxPage));

        KingdomMenuHolder holder =
                new KingdomMenuHolder(
                        KingdomMenuHolder.Menu.MEMBERS,
                        page
                );

        Inventory inventory = createInventory(
                holder,
                "§0✦ §6§lMEMBERS §8[" + (page + 1) + "/" +
                        (maxPage + 1) + "]"
        );

        int start = page * MEMBER_SLOTS.length;
        int end = Math.min(
                start + MEMBER_SLOTS.length,
                members.size()
        );

        for (int i = start; i < end; i++) {
            KingdomMember member = members.get(i);
            UUID memberUuid = member.getPlayerUniqueId();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta == null) continue;

            if (memberUuid != null) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(memberUuid);
                meta.setOwningPlayer(offline);
            }

            meta.setDisplayName("§6§l" + member.getPlayerName());

            boolean online = memberUuid != null
                    && Bukkit.getPlayer(memberUuid) != null;

            List<String> lore = new ArrayList<>();
            lore.add("§7Role: §e" + member.getRole().name());
            lore.add("§7Status: " + (online ? "§aOnline" : "§cOffline"));
            lore.add("");
            lore.add("§eKlik untuk mengelola anggota.");
            lore.add("§0UUID:" + member.getPlayerUuid());

            meta.setLore(lore);
            head.setItemMeta(meta);

            inventory.setItem(MEMBER_SLOTS[i - start], head);
        }

        setItem(inventory, 45, Material.ARROW, "§7Back");

        if (page > 0) {
            setItem(inventory, 48, Material.PAPER, "§ePrevious Page");
        }

        setItem(
                inventory, 49, Material.EMERALD,
                "§a§lInvite Player",
                "§7Undang player online.",
                "",
                "§eKlik untuk membuka."
        );

        if (page < maxPage) {
            setItem(inventory, 50, Material.PAPER, "§eNext Page");
        }

        setItem(inventory, 53, Material.BARRIER, "§cClose");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    // ==================================================
    // INVITE PLAYERS MENU
    // ==================================================

    public void openInvitePlayers(Player player) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        Kingdom kingdom = optional.get();

        Optional<KingdomMember> inviter =
                kingdomManager.getMember(player.getUniqueId());

        if (inviter.isEmpty()
                || !canInvite(inviter.get().getRole())) {
            player.sendMessage("§cKamu tidak memiliki izin untuk mengundang anggota.");
            return;
        }

        if (kingdomManager.getMemberCount(kingdom.getId())
                >= kingdomManager.getMaxMembers(kingdom.getLevel())) {
            player.sendMessage("§cKingdom sudah mencapai kapasitas maksimum.");
            return;
        }

        KingdomMenuHolder holder =
                new KingdomMenuHolder(KingdomMenuHolder.Menu.INVITE_PLAYERS);

        Inventory inventory = createInventory(
                holder,
                "§0✦ §a§lINVITE PLAYERS"
        );

        int index = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(player.getUniqueId())) continue;

            if (kingdomManager.getKingdomByPlayer(
                    target.getUniqueId()).isPresent()) {
                continue;
            }

            if (index >= MEMBER_SLOTS.length) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta == null) continue;

            meta.setOwningPlayer(target);
            meta.setDisplayName("§a§l" + target.getName());

            meta.setLore(List.of(
                    "§7Player: §f" + target.getName(),
                    "",
                    "§eKlik untuk mengundang.",
                    "§0UUID:" + target.getUniqueId()
            ));

            head.setItemMeta(meta);
            inventory.setItem(MEMBER_SLOTS[index++], head);
        }

        if (index == 0) {
            setItem(
                    inventory, 22, Material.BARRIER,
                    "§cNo Available Players",
                    "§7Tidak ada player online",
                    "§7yang bisa diundang."
            );
        }

        setItem(inventory, 49, Material.ARROW, "§7Back");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    // ==================================================
    // MEMBER ACTIONS MENU
    // ==================================================

    public void openMemberActions(Player player, UUID targetUuid) {
        if (player == null || targetUuid == null) return;

        Optional<Kingdom> optional =
                kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        Kingdom kingdom = optional.get();

        Optional<KingdomMember> actorOptional =
                kingdomManager.getMember(player.getUniqueId());

        Optional<KingdomMember> targetOptional =
                kingdomManager.getMember(targetUuid);

        if (actorOptional.isEmpty() || targetOptional.isEmpty()
                || targetOptional.get().getKingdomId() != kingdom.getId()) {
            player.sendMessage("§cAnggota tidak ditemukan.");
            return;
        }

        KingdomMember actor = actorOptional.get();
        KingdomMember target = targetOptional.get();

        if (actor.getPlayerUniqueId() == null
                || actor.getPlayerUniqueId().equals(targetUuid)) {
            player.sendMessage("§cKamu tidak dapat mengelola dirimu sendiri.");
            return;
        }

        if (!canManage(actor.getRole(), target.getRole())) {
            player.sendMessage("§cKamu tidak memiliki izin mengelola anggota ini.");
            return;
        }

        KingdomMenuHolder holder =
                new KingdomMenuHolder(
                        KingdomMenuHolder.Menu.MEMBER_ACTIONS,
                        0,
                        targetUuid.toString()
                );

        Inventory inventory = createInventory(
                holder,
                "§0✦ §6§lMEMBER MANAGEMENT"
        );

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            meta.setDisplayName("§6§l" + target.getPlayerName());
            meta.setLore(List.of(
                    "§7Role: §e" + target.getRole().name(),
                    "§7UUID: §8" + target.getPlayerUuid()
            ));
            head.setItemMeta(meta);
        }

        inventory.setItem(13, head);

        boolean canPromote = actor.getRole() == Role.OWNER
                || (actor.getRole() == Role.LEADER
                && target.getRole() == Role.MEMBER);

        boolean canDemote = actor.getRole() == Role.OWNER
                && (target.getRole() == Role.LEADER
                || target.getRole() == Role.OFFICER);

        if (canPromote) {
            setItem(
                    inventory, 20, Material.LIME_DYE,
                    "§a§lPromote",
                    "§7Naikkan role anggota.",
                    "",
                    "§eKlik untuk promote."
            );
        }

        if (canDemote) {
            setItem(
                    inventory, 22, Material.YELLOW_DYE,
                    "§e§lDemote",
                    "§7Turunkan role anggota.",
                    "",
                    "§eKlik untuk demote."
            );
        }

        setItem(
                inventory, 24, Material.RED_DYE,
                "§c§lKick Member",
                "§7Keluarkan anggota dari Kingdom.",
                "",
                "§eKlik untuk melanjutkan."
        );

        if (actor.getRole() == Role.OWNER) {
            setItem(
                    inventory, 31, Material.GOLDEN_HELMET,
                    "§6§lTransfer Ownership",
                    "§7Pindahkan kepemilikan Kingdom.",
                    "",
                    "§eKlik untuk melanjutkan."
            );
        }

        setItem(inventory, 49, Material.ARROW, "§7Back");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    // ==================================================
    // INVITATIONS MENU
    // ==================================================

    public void openInvitations(Player player) {
        if (player == null) return;

        List<Invitation> invitations =
                kingdomManager.getInvitations(player.getUniqueId());

        KingdomMenuHolder holder =
                new KingdomMenuHolder(KingdomMenuHolder.Menu.INVITATIONS);

        Inventory inventory = createInventory(
                holder,
                "§0✦ §e§lINVITATIONS"
        );

        int index = 0;

        for (Invitation invitation : invitations) {
            if (index >= MEMBER_SLOTS.length) break;

            ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = item.getItemMeta();

            if (meta == null) continue;

            meta.setDisplayName("§e§l" + invitation.kingdomName());

            List<String> lore = new ArrayList<>();
            lore.add("§7Kingdom ID: §f" + invitation.kingdomId());
            lore.add("§7Inviter UUID: §f" + invitation.inviterUuid());
            lore.add("");
            lore.add("§aLeft-click: Accept");
            lore.add("§cRight-click: Deny");
            lore.add("§0INVITATION_ID:" + invitation.id());

            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(MEMBER_SLOTS[index++], item);
        }

        if (index == 0) {
            setItem(
                    inventory, 22, Material.BARRIER,
                    "§cNo Invitations",
                    "§7Kamu tidak memiliki undangan."
            );
        }

        setItem(inventory, 49, Material.ARROW, "§7Back");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    // ==================================================
    // CONFIRMATION MENU
    // ==================================================

    public void openConfirmation(
            Player player,
            String action,
            String target
    ) {
        if (player == null || action == null || target == null
                || action.isBlank() || target.isBlank()) {
            return;
        }

        String normalized = action.toUpperCase();

        if (!List.of("DELETE", "KICK", "TRANSFER").contains(normalized)) {
            return;
        }

        KingdomMenuHolder holder =
                new KingdomMenuHolder(
                        KingdomMenuHolder.Menu.CONFIRMATION,
                        0,
                        normalized + ":" + target
                );

        Inventory inventory = createInventory(
                holder,
                "§0✦ §c§lCONFIRM ACTION"
        );

        String description = switch (normalized) {
            case "DELETE" -> "§7Kingdom akan dihapus permanen.";
            case "KICK" -> "§7Anggota akan dikeluarkan.";
            case "TRANSFER" -> "§7Kepemilikan akan dipindahkan.";
            default -> "§7Konfirmasi tindakan ini.";
        };

        setItem(
                inventory, 13, Material.BARRIER,
                "§c§lAre You Sure?",
                description,
                "",
                "§7Tindakan ini tidak dapat dibatalkan."
        );

        setItem(
                inventory, 11, Material.RED_DYE,
                "§c§lCancel",
                "§7Batalkan tindakan."
        );

        setItem(
                inventory, 15, Material.LIME_DYE,
                "§a§lConfirm",
                "§7Ya, saya yakin."
        );

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    // ==================================================
    // PERMISSION HELPERS
    // ==================================================

    private boolean canInvite(Role role) {
        return role == Role.OWNER
                || role == Role.LEADER
                || role == Role.OFFICER;
    }

    private boolean canManage(Role actor, Role target) {
        return switch (actor) {
            case OWNER -> target != Role.OWNER;
            case LEADER -> target == Role.OFFICER
                    || target == Role.MEMBER;
            case OFFICER -> target == Role.MEMBER;
            default -> false;
        };
    }

    // ==================================================
    // INVENTORY HELPERS
    // ==================================================

    private Inventory createInventory(
            KingdomMenuHolder holder,
            String title
    ) {
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inventory);
        return inventory;
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
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
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

    private void sendNoKingdom(Player player) {
        player.sendMessage("§cKamu belum memiliki Kingdom.");
    }
}
