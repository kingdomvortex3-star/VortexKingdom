
package my.kingdomvortex.vortexkingdom.gui;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class KingdomGUIManager {

    private final VortexKingdom plugin;

    public KingdomGUIManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        KingdomMenuHolder holder = new KingdomMenuHolder(
                KingdomMenuHolder.Menu.MAIN
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                ChatColor.DARK_GRAY + "ᴋɪɴɢᴅᴏᴍ ᴍᴇɴᴜ"
        );

        holder.setInventory(inventory);

        fill(inventory);

        Optional<Kingdom> optional =
                plugin.getKingdomManager()
                        .getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            inventory.setItem(22, createItem(
                    Material.GOLDEN_CROWN,
                    "&6&lCREATE KINGDOM",
                    "&7Build your own kingdom!",
                    "",
                    "&eClick to create a kingdom."
            ));

            inventory.setItem(31, createItem(
                    Material.BOOK,
                    "&b&lKINGDOM INFORMATION",
                    "&7You are not a member",
                    "&7of any kingdom yet.",
                    "",
                    "&eCreate a kingdom to begin!"
            ));

        } else {
            Kingdom kingdom = optional.get();

            int memberCount = plugin.getKingdomManager()
                    .getMemberCount(kingdom.getId());

            int capacity = plugin.getKingdomManager()
                    .getMaxMembers(kingdom.getLevel());

            inventory.setItem(10, createItem(
                    Material.GOLDEN_CROWN,
                    "&6&lKINGDOM PROFILE",
                    "&7Name: &f" + kingdom.getName(),
                    "&7Owner: &f" + kingdom.getOwnerUuid(),
                    "&7Level: &e" + kingdom.getLevel(),
                    "",
                    "&eClick to view profile."
            ));

            inventory.setItem(12, createItem(
                    Material.PLAYER_HEAD,
                    "&b&lMEMBERS",
                    "&7Members: &f" + memberCount +
                            "&7/&f" + capacity,
                    "",
                    "&eClick to manage members."
            ));

            inventory.setItem(14, createItem(
                    Material.EXPERIENCE_BOTTLE,
                    "&a&lKINGDOM LEVEL",
                    "&7Level: &f" + kingdom.getLevel(),
                    "&7EXP: &f" + kingdom.getExperience(),
                    "",
                    "&eClick to view progression."
            ));

            inventory.setItem(16, createItem(
                    Material.BEACON,
                    "&d&lKINGDOM HOME",
                    "&7Teleport to your kingdom.",
                    "",
                    "&eClick to open home menu."
            ));

            inventory.setItem(31, createItem(
                    Material.WRITABLE_BOOK,
                    "&e&lKINGDOM INFORMATION",
                    "&7Kingdom: &f" + kingdom.getName(),
                    "&7Members: &f" + memberCount +
                            "&7/&f" + capacity,
                    "&7Level: &e" + kingdom.getLevel(),
                    "",
                    "&7Manage your kingdom here."
            ));
        }

        inventory.setItem(38, createItem(
                Material.EMERALD,
                "&a&lLEADERBOARD",
                "&7View the strongest kingdoms.",
                "",
                "&eClick to view leaderboard."
        ));

        inventory.setItem(40, createItem(
                Material.PAPER,
                "&e&lINVITATIONS",
                "&7View your pending invitations.",
                "",
                "&eClick to view invitations."
        ));

        inventory.setItem(42, createItem(
                Material.REDSTONE,
                "&c&lCLOSE",
                "&7Close this menu."
        ));

        player.openInventory(inventory);
    }

    public void openProfile(Player player) {
        Optional<Kingdom> optional =
                plugin.getKingdomManager()
                        .getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            player.sendMessage("§cYou don't have a Kingdom.");
            return;
        }

        Kingdom kingdom = optional.get();

        KingdomMenuHolder holder = new KingdomMenuHolder(
                KingdomMenuHolder.Menu.PROFILE
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                ChatColor.DARK_GRAY + "ᴋɪɴɢᴅᴏᴍ ᴘʀᴏꜰɪʟᴇ"
        );

        holder.setInventory(inventory);

        fill(inventory);

        int members = plugin.getKingdomManager()
                .getMemberCount(kingdom.getId());

        int capacity = plugin.getKingdomManager()
                .getMaxMembers(kingdom.getLevel());

        inventory.setItem(13, createItem(
                Material.GOLDEN_CROWN,
                "&6&l" + kingdom.getName(),
                "&7Kingdom Owner: &f" + kingdom.getOwnerUuid(),
                "&7Kingdom Level: &e" + kingdom.getLevel(),
                "&7Members: &f" + members + "/" + capacity,
                "&7Total EXP: &a" + kingdom.getTotalExperience(),
                "&7Total Kills: &c" + kingdom.getTotalKills()
        ));

        inventory.setItem(29, createItem(
                Material.PLAYER_HEAD,
                "&b&lMEMBER MANAGEMENT",
                "&7Manage kingdom members.",
                "",
                "&eComing in the next GUI stage."
        ));

        inventory.setItem(31, createItem(
                Material.EXPERIENCE_BOTTLE,
                "&a&lLEVEL PROGRESSION",
                "&7View kingdom EXP progression.",
                "",
                "&eClick to view."
        ));

        inventory.setItem(33, createItem(
                Material.BARRIER,
                "&c&lLEAVE KINGDOM",
                "&7Leave your current kingdom.",
                "",
                "&eConfirmation GUI coming next."
        ));

        inventory.setItem(49, createItem(
                Material.ARROW,
                "&e&lBACK",
                "&7Return to main menu."
        ));

        player.openInventory(inventory);
    }

    public void openLevelMenu(Player player) {
        Optional<Kingdom> optional =
                plugin.getKingdomManager()
                        .getKingdomByPlayer(player.getUniqueId());

        if (optional.isEmpty()) {
            player.sendMessage("§cYou don't have a Kingdom.");
            return;
        }

        Kingdom kingdom = optional.get();

        int level = kingdom.getLevel();

        long currentExp = kingdom.getExperience();

        long requiredExp = plugin.getLevelManager()
                .getRequiredExp(level);

        long totalExp = kingdom.getTotalExperience();

        double progress = plugin.getLevelManager()
                .getProgress(player.getUniqueId());

        int capacity = plugin.getKingdomManager()
                .getMaxMembers(level);

        KingdomMenuHolder holder = new KingdomMenuHolder(
                KingdomMenuHolder.Menu.LEVEL
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                ChatColor.DARK_GRAY + "ᴋɪɴɢᴅᴏᴍ ʟᴇᴠᴇʟ"
        );

        holder.setInventory(inventory);

        fill(inventory);

        inventory.setItem(13, createItem(
                Material.EXPERIENCE_BOTTLE,
                "&a&lKINGDOM LEVEL",
                "&7Current Level: &e" + level,
                "&7Maximum Level: &e100",
                "",
                level >= 100
                        ? "&6MAXIMUM LEVEL REACHED"
                        : "&7Progress: &a" +
                        String.format("%.2f", progress) + "%"
        ));

        inventory.setItem(21, createItem(
                Material.LIME_DYE,
                "&a&lCURRENT EXP",
                "&7EXP: &f" + currentExp,
                "&7Total Earned: &a" + totalExp
        ));

        inventory.setItem(23, createItem(
                Material.CLOCK,
                "&e&lNEXT LEVEL",
                level >= 100
                        ? "&6Maximum level reached."
                        : "&7Required EXP: &f" + requiredExp,
                level >= 100
                        ? "&7No further progression."
                        : "&7Remaining EXP: &e" +
                        plugin.getLevelManager()
                                .getRemainingExp(player.getUniqueId())
        ));

        inventory.setItem(31, createItem(
                Material.CHEST,
                "&6&lMEMBER CAPACITY",
                "&7Current Capacity: &f" + capacity,
                "&7Starting Capacity: &f5",
                "&7Every level: &a+1 Member"
        ));

        inventory.setItem(49, createItem(
                Material.ARROW,
                "&e&lBACK",
                "&7Return to main menu."
        ));

        player.openInventory(inventory);
    }

    private void fill(Inventory inventory) {
        ItemStack filler = createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                " ",
                ""
        );

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack createItem(
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));

            List<String> lines = new ArrayList<>();

            for (String line : lore) {
                lines.add(color(line));
            }

            meta.setLore(lines);

            item.setItemMeta(meta);
        }

        return item;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
