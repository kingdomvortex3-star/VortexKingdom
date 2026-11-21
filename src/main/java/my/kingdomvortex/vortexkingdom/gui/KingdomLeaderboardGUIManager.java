
package my.kingdomvortex.vortexkingdom.gui;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardEntry;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardType;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class KingdomLeaderboardGUIManager {

    private static final int PAGE_SIZE = 28;

    private final VortexKingdom plugin;
    private final KingdomLeaderboardManager leaderboard;

    public KingdomLeaderboardGUIManager(VortexKingdom plugin) {
        this.plugin = plugin;
        this.leaderboard = plugin.getLeaderboardManager();
    }

    public void open(
            Player player,
            LeaderboardType type,
            int page
    ) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (type == null) {
            type = LeaderboardType.LEVEL;
        }

        List<LeaderboardEntry> entries =
                leaderboard.getTop(type, 100);

        int maxPage = Math.max(
                0,
                (entries.size() - 1) / PAGE_SIZE
        );

        page = Math.max(0, Math.min(page, maxPage));

        KingdomLeaderboardHolder holder =
                new KingdomLeaderboardHolder(type, page);

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                getTitle(type, page)
        );

        holder.setInventory(inventory);

        fill(inventory);

        int start = page * PAGE_SIZE;
        int end = Math.min(
                start + PAGE_SIZE,
                entries.size()
        );

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int index = start; index < end; index++) {
            LeaderboardEntry entry = entries.get(index);
            int slot = slots[index - start];

            inventory.setItem(
                    slot,
                    createEntryItem(entry, type)
            );
        }

        inventory.setItem(
                4,
                createItem(
                        Material.NETHER_STAR,
                        "§6§lKINGDOM LEADERBOARD",
                        "§7Category: §e" + getTypeName(type),
                        "§7Page: §f" + (page + 1)
                                + "§7/§f" + (maxPage + 1),
                        "",
                        "§8VortexKingdom"
                )
        );

        if (page > 0) {
            inventory.setItem(
                    45,
                    createItem(
                            Material.ARROW,
                            "§ePrevious Page",
                            "§7Klik untuk halaman sebelumnya."
                    )
            );
        }

        if (page < maxPage) {
            inventory.setItem(
                    53,
                    createItem(
                            Material.ARROW,
                            "§eNext Page",
                            "§7Klik untuk halaman berikutnya."
                    )
            );
        }

        inventory.setItem(
                48,
                createItem(
                        Material.GOLD_INGOT,
                        "§6Level Ranking",
                        "§7Urutkan berdasarkan level."
                )
        );

        inventory.setItem(
                49,
                createItem(
                        Material.BARRIER,
                        "§cClose",
                        "§7Tutup leaderboard."
                )
        );

        inventory.setItem(
                50,
                createItem(
                        Material.EXPERIENCE_BOTTLE,
                        "§aEXP Ranking",
                        "§7Urutkan berdasarkan total EXP."
                )
        );

        inventory.setItem(
                51,
                createItem(
                        Material.PLAYER_HEAD,
                        "§bMember Ranking",
                        "§7Urutkan berdasarkan jumlah anggota."
                )
        );

        inventory.setItem(
                52,
                createItem(
                        Material.DIAMOND_SWORD,
                        "§cKill Ranking",
                        "§7Urutkan berdasarkan total kill."
                )
        );

        player.openInventory(inventory);
    }

    public void refresh(Player player) {
        if (player == null) {
            return;
        }

        if (!(player.getOpenInventory()
                .getTopInventory()
                .getHolder() instanceof KingdomLeaderboardHolder holder)) {
            return;
        }

        open(player, holder.getType(), holder.getPage());
    }

    private ItemStack createEntryItem(
            LeaderboardEntry entry,
            LeaderboardType type
    ) {
        Material material = switch (entry.position()) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            case 3 -> Material.COPPER_BLOCK;
            default -> Material.PLAYER_HEAD;
        };

        String scoreLabel = switch (type) {
            case LEVEL -> "§7Level: §e" + entry.level();
            case EXPERIENCE -> "§7Total EXP: §a"
                    + format(entry.totalExperience());
            case MEMBERS -> "§7Members: §b" + entry.memberCount();
            case KILLS -> "§7Total Kills: §c"
                    + format(entry.totalKills());
        };

        List<String> lore = new ArrayList<>();

        lore.add("§7Rank: " + getRankColor(entry.position())
                + "#" + entry.position());
        lore.add(scoreLabel);
        lore.add("§7Level: §e" + entry.level());
        lore.add("§7Members: §b" + entry.memberCount());
        lore.add("§7Kills: §c" + format(entry.totalKills()));
        lore.add("");
        lore.add("§8Kingdom ID: " + entry.kingdomId());

        return createItem(
                material,
                "§6§l" + entry.kingdomName(),
                lore.toArray(new String[0])
        );
    }

    private String getTitle(LeaderboardType type, int page) {
        return "§0✦ §6§lLEADERBOARD §8- §e"
                + getTypeName(type)
                + " §8(" + (page + 1) + ")";
    }

    private String getTypeName(LeaderboardType type) {
        return switch (type) {
            case LEVEL -> "LEVEL";
            case EXPERIENCE -> "EXPERIENCE";
            case MEMBERS -> "MEMBERS";
            case KILLS -> "KILLS";
        };
    }

    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "§6";
            case 2 -> "§f";
            case 3 -> "§c";
            default -> "§7";
        };
    }

    private String format(long number) {
        return String.format("%,d", number);
    }

    private ItemStack createItem(
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

        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                "§r"
        );

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }
}
