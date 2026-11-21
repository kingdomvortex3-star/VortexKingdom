
package my.kingdomvortex.vortexkingdom.gui;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardEntry;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardType;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class KingdomLeaderboardGUIManager {

    private static final int INVENTORY_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 28;

    private final VortexKingdom plugin;

    public KingdomLeaderboardGUIManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    public void open(
            org.bukkit.entity.Player player,
            LeaderboardType type,
            int page
    ) {
        if (type == null) {
            type = LeaderboardType.LEVEL;
        }

        List<LeaderboardEntry> entries =
                plugin.getLeaderboardManager().getTop(type, 100);

        int maximumPage = Math.max(
                0,
                (entries.size() - 1) / ENTRIES_PER_PAGE
        );

        page = Math.max(0, Math.min(page, maximumPage));

        KingdomLeaderboardHolder holder =
                new KingdomLeaderboardHolder(type, page);

        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                "§8§lKINGDOM LEADERBOARD"
        );

        holder.setInventory(inventory);

        fillBackground(inventory);
        addHeader(inventory, type, page, entries.size());
        addEntries(inventory, entries, page, type);
        addNavigation(inventory, page, maximumPage);

        player.openInventory(inventory);
    }

    private void fillBackground(Inventory inventory) {
        ItemStack background = createItem(
                Material.BLACK_STAINED_GLASS_PANE,
                "§0 ",
                List.of()
        );

        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, background);
        }

        ItemStack border = createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                "§8 ",
                List.of()
        );

        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, border);
        }

        for (int slot = 45; slot < 54; slot++) {
            inventory.setItem(slot, border);
        }
    }

    private void addHeader(
            Inventory inventory,
            LeaderboardType type,
            int page,
            int total
    ) {
        inventory.setItem(
                4,
                createItem(
                        Material.NETHER_STAR,
                        "§6§lKINGDOM RANKING",
                        List.of(
                                "§7Peringkat Kingdom terbaik.",
                                "",
                                "§fKategori: §e" + getTypeName(type),
                                "§fHalaman: §e" + (page + 1),
                                "§fTotal Kingdom: §e" + total,
                                "",
                                "§8Ranking diperbarui saat menu dibuka."
                        )
                )
        );

        inventory.setItem(
                0,
                createItem(
                        Material.GOLD_INGOT,
                        "§6§lTOP KINGDOMS",
                        List.of(
                                "§7Lihat pencapaian seluruh Kingdom.",
                                "§7Bersaing untuk posisi tertinggi!"
                        )
                )
        );
    }

    private void addEntries(
            Inventory inventory,
            List<LeaderboardEntry> entries,
            int page,
            LeaderboardType type
    ) {
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        if (entries.isEmpty()) {
            inventory.setItem(
                    22,
                    createItem(
                            Material.BARRIER,
                            "§c§lBELUM ADA DATA",
                            List.of(
                                    "§7Belum ada Kingdom yang terdaftar.",
                                    "§7Buat Kingdom untuk masuk ranking."
                            )
                    )
            );
            return;
        }

        for (int index = start; index < end; index++) {
            LeaderboardEntry entry = entries.get(index);

            inventory.setItem(
                    slots[index - start],
                    createEntryItem(entry, type)
            );
        }
    }

    private ItemStack createEntryItem(
            LeaderboardEntry entry,
            LeaderboardType type
    ) {
        Material material = getRankMaterial(entry.position());
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                getRankColor(entry.position())
                        + "§l#" + entry.position()
                        + " §f" + entry.kingdomName()
        );

        List<String> lore = new ArrayList<>();

        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§7Kingdom: §f" + entry.kingdomName());
        lore.add("§7Level: §e" + entry.level());
        lore.add("§7Total EXP: §b" + formatNumber(entry.totalExperience()));
        lore.add("§7Members: §a" + entry.memberCount());
        lore.add("§7Total Kills: §c" + formatNumber(entry.totalKills()));
        lore.add("");
        lore.add("§fScore: §6" + formatNumber(entry.score()));
        lore.add("§7Kategori: §e" + getTypeName(type));
        lore.add("§8━━━━━━━━━━━━━━━━━━");

        if (entry.ownerUuid() != null) {
            try {
                UUID ownerId = UUID.fromString(entry.ownerUuid());
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);

                lore.add("§7Owner: §f" + (
                        owner.getName() == null
                                ? "Unknown"
                                : owner.getName()
                ));
            } catch (IllegalArgumentException exception) {
                lore.add("§7Owner: §fUnknown");
            }
        }

        meta.setLore(lore);

        if (meta instanceof SkullMeta skullMeta
                && entry.ownerUuid() != null) {
            try {
                UUID ownerId = UUID.fromString(entry.ownerUuid());
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerId));
            } catch (IllegalArgumentException ignored) {
                // Gunakan kepala default jika UUID tidak valid.
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    private void addNavigation(
            Inventory inventory,
            int page,
            int maximumPage
    ) {
        inventory.setItem(
                45,
                createItem(
                        page > 0 ? Material.ARROW : Material.GRAY_DYE,
                        page > 0
                                ? "§e§lHALAMAN SEBELUMNYA"
                                : "§7Halaman Pertama",
                        List.of(
                                page > 0
                                        ? "§7Klik untuk kembali."
                                        : "§8Tidak ada halaman sebelumnya."
                        )
                )
        );

        inventory.setItem(
                49,
                createItem(
                        Material.BARRIER,
                        "§c§lTUTUP LEADERBOARD",
                        List.of("§7Klik untuk menutup.")
                )
        );

        inventory.setItem(
                53,
                createItem(
                        page < maximumPage
                                ? Material.ARROW
                                : Material.GRAY_DYE,
                        page < maximumPage
                                ? "§e§lHALAMAN SELANJUTNYA"
                                : "§7Halaman Terakhir",
                        List.of(
                                page < maximumPage
                                        ? "§7Klik untuk melanjutkan."
                                        : "§8Tidak ada halaman berikutnya."
                        )
                )
        );
    }

    private ItemStack createItem(
            Material material,
            String name,
            List<String> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private Material getRankMaterial(int position) {
        return switch (position) {
            case 1 -> Material.NETHERITE_BLOCK;
            case 2 -> Material.GOLD_BLOCK;
            case 3 -> Material.IRON_BLOCK;
            default -> Material.PLAYER_HEAD;
        };
    }

    private String getRankColor(int position) {
        return switch (position) {
            case 1 -> "§6";
            case 2 -> "§f";
            case 3 -> "§c";
            default -> "§b";
        };
    }

    private String getTypeName(LeaderboardType type) {
        return switch (type) {
            case LEVEL -> "Level";
            case EXPERIENCE -> "Experience";
            case MEMBERS -> "Members";
            case KILLS -> "Kills";
        };
    }

    private String formatNumber(long number) {
        return String.format("%,d", number);
    }
}
