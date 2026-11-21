
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
    private final KingdomManager kingdoms;

    private static final int[] MEMBER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public KingdomGUIManager(VortexKingdom plugin) {
        this.plugin = plugin;
        this.kingdoms = plugin.getKingdomManager();
    }

    public void openMain(Player player) {
        openMainMenu(player);
    }

    public void openMainMenu(Player player) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        KingdomMenuHolder holder =
                new KingdomMenuHolder(KingdomMenuHolder.Menu.MAIN);

        Inventory inventory = createInventory(
                holder, "§0✦ §6§lKINGDOM"
        );

        if (optional.isEmpty()) {
            setItem(inventory, 22, Material.GOLDEN_HELMET,
                    "§6§lCreate Kingdom",
                    "§7Bangun kerajaanmu sendiri.",
                    "",
                    "§eKlik untuk membuat Kingdom.");

            setItem(inventory, 31, Material.BOOK,
                    "§b§lKingdom Information",
                    "§7Buat Kingdom dan kumpulkan anggota.",
                    "§7Tingkatkan level untuk membuka",
                    "§7kapasitas anggota lebih besar.");
        } else {
            Kingdom kingdom = optional.get();

            setItem(inventory, 10, Material.GOLDEN_HELMET,
                    "§6§lKingdom Profile",
                    "§7Nama: §f" + kingdom.getName(),
                    "§7Level: §e" + kingdom.getLevel(),
                    "",
                    "§eKlik untuk melihat profil.");

            setItem(inventory, 12, Material.PLAYER_HEAD,
                    "§b§lKingdom Members",
                    "§7Kelola anggota Kingdom.",
                    "",
                    "§eKlik untuk membuka.");

            setItem(inventory, 14, Material.EXPERIENCE_BOTTLE,
                    "§a§lKingdom Level",
                    "§7Level: §e" + kingdom.getLevel(),
                    "§7EXP: §f" + kingdom.getExperience(),
                    "",
                    "§eKlik untuk melihat progres.");

            setItem(inventory, 16, Material.COMPASS,
                    "§d§lKingdom Home",
                    "§7Teleport menuju markas Kingdom.",
                    "",
                    "§eKlik untuk teleport.");

            setItem(inventory, 31, Material.BOOK,
                    "§b§lKingdom Information",
                    "§7Nama: §f" + kingdom.getName(),
                    "§7Level: §e" + kingdom.getLevel(),
                    "§7Anggota: §f" + kingdoms.getMemberCount(kingdom.getId()),
                    "§7Kapasitas: §f" + kingdoms.getMaxMembers(kingdom.getLevel()));
        }

        setItem(inventory, 38, Material.GOLD_INGOT,
                "§6§lLeaderboard",
                "§7Lihat Kingdom terbaik.",
                "",
                "§eKlik untuk membuka.");

        setItem(inventory, 40, Material.WRITABLE_BOOK,
                "§e§lInvitations",
                "§7Periksa undangan Kingdom.",
                "",
                "§eKlik untuk membuka.");

        setItem(inventory, 42, Material.BARRIER,
                "§c§lClose",
                "§7Tutup menu.");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openProfile(Player player) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        Optional<KingdomMember> member =
                kingdoms.getMember(player.getUniqueId());

        if (optional.isEmpty() || member.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        Kingdom kingdom = optional.get();

        OfflinePlayer owner = Bukkit.getOfflinePlayer(
                UUID.fromString(kingdom.getOwnerUuid())
        );

        String ownerName = owner.getName() == null
                ? "Unknown" : owner.getName();

        KingdomMenuHolder holder =
                new KingdomMenuHolder(KingdomMenuHolder.Menu.PROFILE);

        Inventory inventory = createInventory(
                holder, "§0✦ §6§lKINGDOM PROFILE"
        );

        setItem(inventory, 13, Material.GOLDEN_HELMET,
                "§6§l" + kingdom.getName(),
                "§7Owner: §f" + ownerName,
                "§7Level: §e" + kingdom.getLevel(),
                "§7EXP: §f" + kingdom.getExperience(),
                "§7Total EXP: §f" + kingdom.getTotalExperience(),
                "§7Total Kills: §f" + kingdom.getTotalKills(),
                "§7Members: §f" + kingdoms.getMemberCount(kingdom.getId()),
                "§7Capacity: §f" + kingdoms.getMaxMembers(kingdom.getLevel()),
                "§7Your Role: §e" + member.get().getRole().name());

        setItem(inventory, 29, Material.PLAYER_HEAD,
                "§b§lManage Members",
                "§7Kelola anggota Kingdom.",
                "",
                "§eKlik untuk membuka.");

        setItem(inventory, 31, Material.EXPERIENCE_BOTTLE,
                "§a§lKingdom Level",
                "§7Lihat progres level.");

        boolean ownerRole = member.get().getRole() == Role.OWNER;

        setItem(inventory, 33,
                ownerRole ? Material.TNT : Material.OAK_DOOR,
                ownerRole ? "§c§lDelete Kingdom" : "§c§lLeave Kingdom",
                ownerRole
                        ? "§7Hapus Kingdom secara permanen."
                        : "§7Keluar dari Kingdom.",
                "",
                "§eKlik untuk melanjutkan.");

        setItem(inventory, 49, Material.ARROW,
                "§7Back to Main Menu");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openLevel(Player player) {
        openLevelMenu(player);
    }

    public void openLevelMenu(Player player) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        Kingdom kingdom = optional.get();
        int maxLevel = plugin.getLevelManager().getMaxLevel();

        double progress = plugin.getLevelManager().getProgress(
                player.getUniqueId()
        );

        long remaining = plugin.getLevelManager().getRemainingExp(
                player.getUniqueId()
        );

        KingdomMenuHolder holder =
                new KingdomMenuHolder(KingdomMenuHolder.Menu.LEVEL);

        Inventory inventory = createInventory(
                holder, "§0✦ §a§lKINGDOM LEVEL"
        );

        setItem(inventory, 13, Material.BEACON,
                "§a§lKingdom Level",
                "§7Current Level: §e" + kingdom.getLevel(),
                "§7Maximum Level: §f" + maxLevel,
                "",
                kingdom.getLevel() >= maxLevel
                        ? "§aMaximum level reached!"
                        : "§7Next Level: §e" + (kingdom.getLevel() + 1));

        setItem(inventory, 21, Material.EXPERIENCE_BOTTLE,
                "§b§lExperience",
                "§7Current EXP: §f" + kingdom.getExperience(),
                "§7Total EXP: §f" + kingdom.getTotalExperience(),
                "",
                "§7Progress: §a" + String.format("%.1f", progress) + "%",
                "§7Remaining EXP: §e" + remaining);

        setItem(inventory, 23, Material.KNOWLEDGE_BOOK,
                "§e§lNext Level",
                kingdom.getLevel() >= maxLevel
                        ? "§aMaximum level reached!"
                        : "§7Required EXP: §f"
                        + plugin.getLevelManager().getRequiredExp(
                                kingdom.getLevel()));

        setItem(inventory, 49, Material.ARROW,
                "§7Back to Main Menu");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openMembers(Player player, int page) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        Kingdom kingdom = optional.get();
        List<KingdomMember> members =
                kingdoms.getMembers(kingdom.getId());

        int maxPage = Math.max(
                0,
                (members.size() - 1) / MEMBER_SLOTS.length
        );

        page = Math.max(0, Math.min(page, maxPage));

        KingdomMenuHolder holder =
                new KingdomMenuHolder(
                        KingdomMenuHolder.Menu.MEMBERS,
                        page,
                        null
                );

        Inventory inventory = createInventory(
                holder, "§0✦ §b§lKINGDOM MEMBERS"
        );

        int start = page * MEMBER_SLOTS.length;
        int end = Math.min(start + MEMBER_SLOTS.length, members.size());

        for (int index = start; index < end; index++) {
            KingdomMember member = members.get(index);
            int slot = MEMBER_SLOTS[index - start];

            OfflinePlayer offline = Bukkit.getOfflinePlayer(
                    member.getPlayerUniqueId()
            );

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(offline);
                meta.setDisplayName("§b§l" + member.getPlayerName());

                List<String> lore = new ArrayList<>();
                lore.add("§7Role: §e" + member.getRole().name());
                lore.add("");
                lore.add("§eKlik untuk melihat aksi anggota.");
                lore.add("§0PLAYER_UUID:" + member.getPlayerUuid());

                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            inventory.setItem(slot, head);
        }

        if (page > 0) {
            setItem(inventory, 45, Material.ARROW,
                    "§ePrevious Page");
        }

        if (page < maxPage) {
            setItem(inventory, 53, Material.ARROW,
                    "§eNext Page");
        }

        setItem(inventory, 49, Material.ARROW,
                "§7Back to Profile");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openInvitePlayers(Player player) {
        if (player == null) return;

        Optional<Kingdom> optional =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            sendNoKingdom(player);
            return;
        }

        Kingdom kingdom = optional.get();

        Optional<KingdomMember> actor =
                kingdoms.getMember(player.getUniqueId());

        if (actor.isEmpty() || !canInvite(actor.get().getRole())) {
            player.sendMessage("§cKamu tidak memiliki izin untuk mengundang.");
            return;
        }

        KingdomMenuHolder holder =
                new KingdomMenuHolder(
                        KingdomMenuHolder.Menu.INVITE_PLAYERS
                );

        Inventory inventory = createInventory(
                holder, "§0✦ §d§lINVITE PLAYERS"
        );

        int slot = 10;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;

            if (kingdoms.getKingdomByPlayer(
                    target.getUniqueId()).isPresent()) {
                continue;
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName("§b" + target.getName());

                meta.setLore(List.of(
                        "§7Undang pemain ini ke Kingdom.",
                        "",
                        "§eKlik untuk mengirim undangan.",
                        "§0PLAYER_UUID:" + target.getUniqueId()
                ));

                head.setItemMeta(meta);
            }

            inventory.setItem(slot++, head);

            if (slot == 17 || slot == 26 || slot == 35 || slot == 44) {
                slot += 2;
            }

            if (slot >= 45) break;
        }

        setItem(inventory, 49, Material.ARROW,
                "§7Back to Members");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openMemberActions(Player player, UUID targetUuid) {
        if (player == null || targetUuid == null) return;

        Optional<KingdomMember> actor =
                kingdoms.getMember(player.getUniqueId());

        Optional<KingdomMember> target =
                kingdoms.getMember(targetUuid);

        if (actor.isEmpty() || target.isEmpty()) {
            player.sendMessage("§cAnggota tidak ditemukan.");
            return;
        }

        if (!actor.get().getKingdomIdEquals(target.get().getKingdomId())) {
            player.sendMessage("§cPemain tersebut bukan anggota Kingdom kamu.");
            return;
        }

        if (!canManage(actor.get().getRole(), target.get().getRole())) {
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
                holder, "§0✦ §e§lMEMBER ACTIONS"
        );

        setItem(inventory, 20, Material.IRON_INGOT,
                "§a§lPromote",
                "§7Naikkan jabatan anggota.");

        setItem(inventory, 22, Material.REDSTONE,
                "§e§lDemote",
                "§7Turunkan jabatan anggota.");

        setItem(inventory, 24, Material.BARRIER,
                "§c§lKick Member",
                "§7Keluarkan anggota dari Kingdom.");

        setItem(inventory, 49, Material.ARROW,
                "§7Back to Members");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openInvitations(Player player) {
        if (player == null) return;

        List<Invitation> invitations =
                kingdoms.getInvitations(player.getUniqueId());

        KingdomMenuHolder holder =
                new KingdomMenuHolder(
                        KingdomMenuHolder.Menu.INVITATIONS
                );

        Inventory inventory = createInventory(
                holder, "§0✦ §e§lKINGDOM INVITATIONS"
        );

        int slot = 10;

        for (Invitation invitation : invitations) {
            if (slot >= 44) break;

            ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(
                        "§6§l" + invitation.kingdomName()
                );

                OfflinePlayer inviter;

                try {
                    inviter = Bukkit.getOfflinePlayer(
                            UUID.fromString(invitation.inviterUuid())
                    );
                } catch (IllegalArgumentException exception) {
                    inviter = null;
                }

                String inviterName = inviter == null
                        || inviter.getName() == null
                        ? "Unknown" : inviter.getName();

                meta.setLore(List.of(
                        "§7Inviter: §f" + inviterName,
                        "",
                        "§aKlik kiri untuk menerima.",
                        "§cKlik kanan untuk menolak.",
                        "§0INVITATION_ID:" + invitation.id()
                ));

                item.setItemMeta(meta);
            }

            inventory.setItem(slot++, item);

            if (slot == 17 || slot == 26 || slot == 35) {
                slot += 2;
            }
        }

        if (invitations.isEmpty()) {
            setItem(inventory, 22, Material.BARRIER,
                    "§cNo Invitations",
                    "§7Kamu belum memiliki undangan.");
        }

        setItem(inventory, 49, Material.ARROW,
                "§7Back to Main Menu");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openConfirmation(
            Player player,
            String title,
            String action,
            UUID target
    ) {
        if (player == null || action == null || target == null) return;

        KingdomMenuHolder holder =
                new KingdomMenuHolder(
                        KingdomMenuHolder.Menu.CONFIRMATION,
                        0,
                        action + ":" + target
                );

        Inventory inventory = createInventory(
                holder, "§0✦ §c§lCONFIRM ACTION"
        );

        setItem(inventory, 11, Material.LIME_WOOL,
                "§a§lCancel",
                "§7Kembali tanpa melakukan aksi.");

        setItem(inventory, 15, Material.RED_WOOL,
                "§c§lConfirm",
                "§7" + title,
                "",
                "§eKlik untuk melanjutkan.");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    private boolean canInvite(Role role) {
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

    private Inventory createInventory(
            KingdomMenuHolder holder,
            String title
    ) {
        Inventory inventory = Bukkit.createInventory(
                holder, 54, title
        );

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

            if (lore != null && lore.length > 0) {
                meta.setLore(List.of(lore));
            }

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

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void sendNoKingdom(Player player) {
        player.sendMessage(
                "§cKamu belum bergabung dengan Kingdom."
        );
    }
}
