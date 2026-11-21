
package my.kingdomvortex.vortexkingdom.gui;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class KingdomGUIManager {

    private final VortexKingdom plugin;
    private final KingdomManager kingdomManager;

    public KingdomGUIManager(VortexKingdom plugin) {
        this.plugin = plugin;
        this.kingdomManager = plugin.getKingdomManager();
    }

    public void openMainMenu(Player player) {
        Kingdom kingdom = kingdomManager.getKingdomByPlayer(player.getUniqueId());

        KingdomMenuHolder holder = new KingdomMenuHolder(KingdomMenuHolder.Menu.MAIN);
        Inventory inventory = createInventory(holder, "§0✦ §6KINGDOM MENU");

        if (kingdom == null) {
            setItem(inventory, 22, Material.GOLDEN_HELMET,
                    "§6§lCreate Kingdom",
                    "§7Buat kerajaan milikmu sendiri.",
                    "",
                    "§eKlik untuk membuat Kingdom.");

            setItem(inventory, 31, Material.BOOK,
                    "§b§lKingdom Information",
                    "§7Pelajari sistem Kingdom.");
        } else {
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
                    "§7Level: §f" + kingdom.getLevel(),
                    "§7EXP: §f" + kingdom.getExperience(),
                    "",
                    "§eKlik untuk melihat progres.");

            setItem(inventory, 16, Material.COMPASS,
                    "§d§lKingdom Home",
                    "§7Teleport ke markas Kingdom.");

            setItem(inventory, 31, Material.BOOK,
                    "§b§lKingdom Information",
                    "§7Nama: §f" + kingdom.getName(),
                    "§7Level: §e" + kingdom.getLevel());
        }

        setItem(inventory, 38, Material.GOLD_INGOT,
                "§6§lLeaderboard",
                "§7Lihat Kingdom terbaik.");

        setItem(inventory, 40, Material.WRITABLE_BOOK,
                "§e§lInvitations",
                "§7Lihat undangan Kingdom.",
                "",
                "§eKlik untuk membuka.");

        setItem(inventory, 42, Material.BARRIER,
                "§c§lClose",
                "§7Tutup menu.");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openProfile(Player player) {
        Kingdom kingdom = kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (kingdom == null) {
            player.sendMessage("§cKamu belum memiliki Kingdom.");
            return;
        }

        KingdomMenuHolder holder = new KingdomMenuHolder(KingdomMenuHolder.Menu.PROFILE);
        Inventory inventory = createInventory(holder, "§0✦ §6KINGDOM PROFILE");

        setItem(inventory, 13, Material.GOLDEN_HELMET,
                "§6§l" + kingdom.getName(),
                "§7Level: §e" + kingdom.getLevel(),
                "§7EXP: §f" + kingdom.getExperience(),
                "§7Members: §f" + kingdomManager.getMemberCount(kingdom.getId()),
                "§7Capacity: §f" + kingdomManager.getMaxMembers(kingdom.getLevel()));

        setItem(inventory, 29, Material.PLAYER_HEAD,
                "§b§lManage Members",
                "§7Kelola anggota Kingdom.",
                "",
                "§eKlik untuk membuka.");

        setItem(inventory, 31, Material.EXPERIENCE_BOTTLE,
                "§a§lKingdom Level",
                "§7Lihat progres level Kingdom.");

        setItem(inventory, 33, Material.OAK_DOOR,
                "§c§lLeave Kingdom",
                "§7Keluar dari Kingdom.");

        setItem(inventory, 49, Material.ARROW,
                "§7Back to Main Menu");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openLevelMenu(Player player) {
        Kingdom kingdom = kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (kingdom == null) {
            player.sendMessage("§cKamu belum memiliki Kingdom.");
            return;
        }

        KingdomMenuHolder holder = new KingdomMenuHolder(KingdomMenuHolder.Menu.LEVEL);
        Inventory inventory = createInventory(holder, "§0✦ §aKINGDOM LEVEL");

        setItem(inventory, 13, Material.BEACON,
                "§a§lKingdom Level",
                "§7Level saat ini: §e" + kingdom.getLevel());

        setItem(inventory, 21, Material.EXPERIENCE_BOTTLE,
                "§b§lCurrent EXP",
                "§7EXP: §f" + kingdom.getExperience(),
                "§7Total EXP: §f" + kingdom.getTotalExperience());

        setItem(inventory, 23, Material.KNOWLEDGE_BOOK,
                "§e§lNext Level",
                "§7Level berikutnya: §f" + (kingdom.getLevel() + 1));

        setItem(inventory, 31, Material.CHEST,
                "§6§lMember Capacity",
                "§7Members: §f" + kingdomManager.getMemberCount(kingdom.getId()),
                "§7Capacity: §f" + kingdomManager.getMaxMembers(kingdom.getLevel()));

        setItem(inventory, 49, Material.ARROW, "§7Back");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openMembers(Player player, int page) {
        Kingdom kingdom = kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (kingdom == null) {
            player.sendMessage("§cKamu belum memiliki Kingdom.");
            return;
        }

        List<KingdomMember> members = kingdomManager.getMembers(kingdom.getId());

        KingdomMenuHolder holder = new KingdomMenuHolder(
                KingdomMenuHolder.Menu.MEMBERS, page
        );

        Inventory inventory = createInventory(
                holder, "§0✦ §6KINGDOM MEMBERS §8(" + (page + 1) + ")"
        );

        int start = page * 28;
        int end = Math.min(start + 28, members.size());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = start; i < end; i++) {
            KingdomMember member = members.get(i);
            UUID uuid = member.getPlayerUuid();

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(offlinePlayer);
                meta.setDisplayName("§6§l" + member.getPlayerName());

                List<String> lore = new ArrayList<>();
                lore.add("§7Role: §e" + member.getRole().name());
                lore.add("§7Status: " + (offlinePlayer.isOnline()
                        ? "§aOnline" : "§cOffline"));
                lore.add("");
                lore.add("§eKlik untuk mengelola anggota.");

                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            inventory.setItem(slots[i - start], head);
        }

        setItem(inventory, 45, Material.ARROW, "§7Back");

        if (page > 0) {
            setItem(inventory, 48, Material.PAPER, "§ePrevious Page");
        }

        setItem(inventory, 49, Material.EMERALD,
                "§aInvite Player",
                "§7Undang player online.");

        if (end < members.size()) {
            setItem(inventory, 50, Material.PAPER, "§eNext Page");
        }

        setItem(inventory, 53, Material.BARRIER, "§cClose");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openInvitePlayers(Player player) {
        Kingdom kingdom = kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (kingdom == null) {
            player.sendMessage("§cKamu belum memiliki Kingdom.");
            return;
        }

        KingdomMenuHolder holder = new KingdomMenuHolder(
                KingdomMenuHolder.Menu.INVITE_PLAYERS
        );

        Inventory inventory = createInventory(holder, "§0✦ §aINVITE PLAYERS");

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int index = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) {
                continue;
            }

            if (kingdomManager.getKingdomByPlayer(target.getUniqueId()) != null) {
                continue;
            }

            if (index >= slots.length) {
                break;
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName("§a§l" + target.getName());
                meta.setLore(List.of(
                        "§7Klik untuk mengundang player ini.",
                        "",
                        "§eInvite " + target.getName()
                ));
                head.setItemMeta(meta);
            }

            inventory.setItem(slots[index++], head);
        }

        if (index == 0) {
            setItem(inventory, 22, Material.BARRIER,
                    "§cNo Available Players",
                    "§7Tidak ada player online",
                    "§7yang bisa diundang.");
        }

        setItem(inventory, 49, Material.ARROW, "§7Back");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openMemberActions(Player player, UUID targetUuid) {
        Kingdom kingdom = kingdomManager.getKingdomByPlayer(player.getUniqueId());

        if (kingdom == null) {
            player.sendMessage("§cKamu belum memiliki Kingdom.");
            return;
        }

        KingdomMember target = kingdomManager.getMember(
                kingdom.getId(), targetUuid
        );

        if (target == null) {
            player.sendMessage("§cAnggota tidak ditemukan.");
            return;
        }

        KingdomMenuHolder holder = new KingdomMenuHolder(
                KingdomMenuHolder.Menu.MEMBER_ACTIONS,
                0,
                targetUuid.toString()
        );

        Inventory inventory = createInventory(
                holder, "§0✦ §6MEMBER MANAGEMENT"
        );

        setItem(inventory, 13, Material.PLAYER_HEAD,
                "§6§l" + target.getPlayerName(),
                "§7Role: §e" + target.getRole().name());

        setItem(inventory, 20, Material.LIME_DYE,
                "§a§lPromote",
                "§7Naikkan role anggota.");

        setItem(inventory, 22, Material.YELLOW_DYE,
                "§e§lDemote",
                "§7Turunkan role anggota.");

        setItem(inventory, 24, Material.RED_DYE,
                "§c§lKick Member",
                "§7Keluarkan anggota dari Kingdom.");

        setItem(inventory, 31, Material.GOLDEN_HELMET,
                "§6§lTransfer Ownership",
                "§7Pindahkan kepemilikan Kingdom.");

        setItem(inventory, 49, Material.ARROW, "§7Back");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openInvitations(Player player) {
        List<KingdomManager.Invitation> invitations =
                kingdomManager.getInvitations(player.getUniqueId());

        KingdomMenuHolder holder = new KingdomMenuHolder(
                KingdomMenuHolder.Menu.INVITATIONS
        );

        Inventory inventory = createInventory(
                holder, "§0✦ §eKINGDOM INVITATIONS"
        );

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int index = 0;

        for (KingdomManager.Invitation invitation : invitations) {
            if (index >= slots.length) {
                break;
            }

            setItem(inventory, slots[index++], Material.WRITABLE_BOOK,
                    "§e§l" + invitation.kingdomName(),
                    "§7Invited by: §f" + invitation.inviterUuid(),
                    "",
                    "§aLeft-click: Accept",
                    "§cRight-click: Deny");
        }

        if (index == 0) {
            setItem(inventory, 22, Material.BARRIER,
                    "§cNo Invitations",
                    "§7Kamu tidak memiliki undangan.");
        }

        setItem(inventory, 49, Material.ARROW, "§7Back");

        fillEmpty(inventory);
        player.openInventory(inventory);
    }

    private Inventory createInventory(KingdomMenuHolder holder, String title) {
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
}
