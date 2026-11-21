
package my.kingdomvortex.vortexkingdom.gui;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardEntry;
import my.kingdomvortex.vortexkingdom.manager.KingdomLeaderboardManager.LeaderboardType;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class KingdomLeaderboardGUIManager {

    private final VortexKingdom plugin;

    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public KingdomLeaderboardGUIManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    public void open(
            org.bukkit.entity.Player player,
            LeaderboardType type,
            int page
    ) {
        page = Math.max(0, page);

        KingdomLeaderboardHolder holder =
                new KingdomLeaderboardHolder(type, page);

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                "§8Kingdom Leaderboard"
        );

        holder.setInventory(inventory);

        fillBackground(inventory);

        addCategories(inventory, type);

        List<LeaderboardEntry> entries =
                plugin.getLeaderboardManager().getTop(type, 100);

        int start = page * ENTRY_SLOTS.length;
        int end = Math.min(start + ENTRY_SLOTS.length, entries.size());

        for (int index = start; index < end; index++) {
            LeaderboardEntry entry = entries.get(index);

            int slot = ENTRY_SLOTS[index - start];

            inventory.setItem(
                    slot,
                    createEntryItem(entry, type)
            );
        }

        inventory.setItem(
                45,
                createItem(
                        Material.ARROW,
                        "§cPrevious Page",
                        "§7Click to go back.",
                        "§8PAGE_PREVIOUS"
                )
        );

        inventory.setItem(
                49,
                createItem(
                        Material.BARRIER,
                        "§cClose",
                        "§7Close this menu."
                )
        );

        inventory.setItem(
                53,
                createItem(
                        Material.ARROW,
                        "§aNext Page",
                        "§7Click to continue.",
                        "§8PAGE_NEXT"
                )
        );

        inventory.setItem(
                4,
                createItem(
                        Material.NETHER_STAR,
                        "§6Kingdom Leaderboard",
                        "§7Category: §e" + getDisplayName(type),
                        "§7Page: §f" + (page + 1),
                        "§7Total Kingdoms: §f" + entries.size()
                )
        );

        player.openInventory(inventory);
    }

    public void openMain(PlayerPlaceholder player) {
        open(
                player.player(),
                LeaderboardType.LEVEL,
                0
        );
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                "§8 "
        );

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private void addCategories(
            Inventory inventory,
            LeaderboardType selected
    ) {
        inventory.setItem(
                2,
                categoryItem(
                        Material.GOLD_INGOT,
                        "§6§lLEVEL",
                        "§7Highest Kingdom Level",
                        selected == LeaderboardType.LEVEL
                )
        );

        inventory.setItem(
                3,
                categoryItem(
                        Material.EXPERIENCE_BOTTLE,
                        "§b§lEXPERIENCE",
                        "§7Highest Total EXP",
                        selected == LeaderboardType.EXPERIENCE
                )
        );

        inventory.setItem(
                5,
                categoryItem(
                        Material.PLAYER_HEAD,
                        "§a§lMEMBERS",
                        "§7Most Kingdom Members",
                        selected == LeaderboardType.MEMBERS
                )
        );

        inventory.setItem(
                6,
                categoryItem(
                        Material.IRON_SWORD,
                        "§c§lKILLS",
                        "§7Highest Total Kills",
                        selected == LeaderboardType.KILLS
                )
        );
    }

    private ItemStack categoryItem(
            Material material,
            String name,
            String description,
            boolean selected
    ) {
        return createItem(
                material,
                name,
                description,
                selected ? "§aCurrently selected" : "§eClick to select"
        );
    }

    private ItemStack createEntryItem(
            LeaderboardEntry entry,
            LeaderboardType type
    ) {
        int position = entry.position();

        Material material = switch (position) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            case 3 -> Material.COPPER_BLOCK;
            default -> Material.PAPER;
        };

        String medal = switch (position) {
            case 1 -> "§6§l#1";
            case 2 -> "§f§l#2";
            case 3 -> "§c§l#3";
            default -> "§e#" + position;
        };

        List<String> lore = new ArrayList<>();

        lore.add("§8Kingdom Ranking");
        lore.add("");
        lore.add("§7Rank: " + medal);
        lore.add("§7Level: §6" + entry.level());
        lore.add("§7Total EXP: §b" + format(entry.totalExperience()));
        lore.add("§7Members: §a" + entry.memberCount());
        lore.add("§7Total Kills: §c" + format(entry.totalKills()));
        lore.add("");
        lore.add("§7" + getScoreLabel(type) + ": §e"
                + format(entry.score()));

        return createItem(
                material,
                "§6§l" + entry.kingdomName(),
                lore.toArray(new String[0])
        );
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

            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private String getDisplayName(LeaderboardType type) {
        return switch (type) {
            case LEVEL -> "Level";
            case EXPERIENCE -> "Experience";
            case MEMBERS -> "Members";
            case KILLS -> "Kills";
        };
    }

    private String getScoreLabel(LeaderboardType type) {
        return switch (type) {
            case LEVEL -> "Kingdom Level";
            case EXPERIENCE -> "Total Experience";
            case MEMBERS -> "Members";
            case KILLS -> "Total Kills";
        };
    }

    private String format(long number) {
        return String.format("%,d", number);
    }

    public interface PlayerPlaceholder {
        org.bukkit.entity.Player player();
    }
}
