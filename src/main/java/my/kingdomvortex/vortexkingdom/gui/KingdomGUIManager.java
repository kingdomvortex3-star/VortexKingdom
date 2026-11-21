
package my.kingdomvortex.vortexkingdom.gui;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

public final class KingdomFeatureGUIManager {

    private final VortexKingdom plugin;

    public KingdomFeatureGUIManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    public void openHome(Player player) {
        if (player == null || !player.isOnline()) return;

        Optional<Kingdom> optional = getKingdom(player);

        if (optional.isEmpty()) {
            player.sendMessage(prefix() + "§cKamu belum memiliki Kingdom.");
            return;
        }

        Kingdom kingdom = optional.get();

        boolean hasHome = plugin.getFeatureManager()
                .getHome(kingdom.getId()).isPresent();

        KingdomFeatureMenuHolder holder =
                new KingdomFeatureMenuHolder(
                        KingdomFeatureMenuHolder.Menu.HOME
                );

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                "§0✦ §6§lKINGDOM HOME"
        );

        holder.setInventory(inventory);

        setItem(inventory, 20, Material.RESPAWN_ANCHOR,
                "§a§lSet Kingdom Home",
                "§7Simpan lokasi kamu sebagai",
                "§7markas Kingdom.",
                "",
                "§eKlik untuk menyimpan.");

        setItem(inventory, 22, Material.ENDER_PEARL,
                "§b§lTeleport to Home",
                "§7Status: " + (hasHome ? "§aTersedia" : "§cBelum diatur"),
                "",
                hasHome
                        ? "§eKlik untuk teleport."
                        : "§cAtur Home terlebih dahulu.");

        setItem(inventory, 24, Material.BARRIER,
                "§c§lDelete Home",
                hasHome
                        ? "§7Hapus lokasi Home Kingdom."
                        : "§cBelum ada Home.",
                "",
                "§eKlik untuk menghapus.");

        setItem(inventory, 49, Material.ARROW, "§7Kembali");

        fill(inventory);
        player.openInventory(inventory);
    }

    public void openSettings(Player player) {
        if (player == null || !player.isOnline()) return;

        Optional<Kingdom> optional = getKingdom(player);

        if (optional.isEmpty()) {
            player.sendMessage(prefix() + "§cKamu belum memiliki Kingdom.");
            return;
        }

        Kingdom kingdom = optional.get();

        boolean pvp = plugin.getFeatureManager()
                .isPvpEnabled(kingdom.getId());

        boolean chat = plugin.getFeatureManager()
                .isChatEnabled(kingdom.getId());

        KingdomFeatureMenuHolder holder =
                new KingdomFeatureMenuHolder(
                        KingdomFeatureMenuHolder.Menu.SETTINGS
                );

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                "§0✦ §6§lKINGDOM SETTINGS"
        );

        holder.setInventory(inventory);

        setItem(inventory, 20,
                pvp ? Material.LIME_DYE : Material.GRAY_DYE,
                "§6§lKingdom PvP",
                "§7Status: " + (pvp ? "§aON" : "§cOFF"),
                "",
                "§eKlik untuk mengubah.");

        setItem(inventory, 22, Material.NAME_TAG,
                "§e§lKingdom Description",
                "§7Deskripsi:",
                "§f" + emptyIfNull(kingdom.getDescription()),
                "",
                "§eKlik untuk mengedit.");

        setItem(inventory, 24,
                chat ? Material.LIME_DYE : Material.GRAY_DYE,
                "§b§lKingdom Chat",
                "§7Status: " + (chat ? "§aON" : "§cOFF"),
                "",
                "§eKlik untuk mengubah.");

        setItem(inventory, 49, Material.ARROW, "§7Kembali");

        fill(inventory);
        player.openInventory(inventory);
    }

    public void openDescriptionInput(Player player) {
        if (player == null || !player.isOnline()) return;

        if (getKingdom(player).isEmpty()) {
            player.sendMessage(prefix() + "§cKamu belum memiliki Kingdom.");
            return;
        }

        KingdomFeatureMenuHolder holder =
                new KingdomFeatureMenuHolder(
                        KingdomFeatureMenuHolder.Menu.DESCRIPTION_INPUT
                );

        Inventory inventory = Bukkit.createInventory(
                holder,
                InventoryType.ANVIL,
                "§0Edit Description"
        );

        holder.setInventory(inventory);

        ItemStack input = new ItemStack(Material.PAPER);
        ItemMeta meta = input.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("Kingdom Description");
            meta.setLore(List.of(
                    "§7Masukkan deskripsi baru.",
                    "§7Maksimal 200 karakter."
            ));
            input.setItemMeta(meta);
        }

        inventory.setItem(0, input);
        player.openInventory(inventory);
    }

    public void openHomeDeleteConfirmation(Player player) {
        if (player == null || !player.isOnline()) return;

        KingdomFeatureMenuHolder holder =
                new KingdomFeatureMenuHolder(
                        KingdomFeatureMenuHolder.Menu.HOME_CONFIRMATION
                );

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                "§0✦ §c§lDELETE KINGDOM HOME"
        );

        holder.setInventory(inventory);

        setItem(inventory, 11, Material.LIME_WOOL,
                "§a§lCancel",
                "§7Kembali tanpa menghapus Home.");

        setItem(inventory, 13, Material.PAPER,
                "§e§lConfirmation",
                "§7Apakah kamu yakin ingin",
                "§7menghapus Kingdom Home?");

        setItem(inventory, 15, Material.RED_WOOL,
                "§c§lConfirm Delete",
                "§7Hapus Kingdom Home.",
                "",
                "§eKlik untuk mengonfirmasi.");

        fill(inventory);
        player.openInventory(inventory);
    }

    public void openCreateKingdomInput(Player player) {
        if (player == null || !player.isOnline()) return;

        KingdomFeatureMenuHolder holder =
                new KingdomFeatureMenuHolder(
                        KingdomFeatureMenuHolder.Menu.CREATE_KINGDOM_INPUT
                );

        Inventory inventory = Bukkit.createInventory(
                holder,
                InventoryType.ANVIL,
                "§0Create Kingdom"
        );

        holder.setInventory(inventory);

        ItemStack input = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = input.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("Kingdom Name");
            meta.setLore(List.of(
                    "§7Masukkan nama Kingdom."
            ));
            input.setItemMeta(meta);
        }

        inventory.setItem(0, input);
        player.openInventory(inventory);
    }

    private Optional<Kingdom> getKingdom(Player player) {
        return plugin.getKingdomManager()
                .getKingdomByPlayer(player.getUniqueId());
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

    private void fill(Inventory inventory) {
        ItemStack filler = new ItemStack(
                Material.BLACK_STAINED_GLASS_PANE
        );

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

    private String emptyIfNull(String value) {
        return value == null || value.isBlank()
                ? "Belum ada deskripsi."
                : value;
    }

    private String prefix() {
        return "§8[§6Kingdom§8] §7";
    }
}
