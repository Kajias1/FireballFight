package me.kajias.fireballfight.gui.menus;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Sounds;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.configurations.DataConfiguration;
import me.kajias.fireballfight.gui.InventoryButton;
import me.kajias.fireballfight.gui.InventoryGUI;
import me.kajias.fireballfight.objects.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class BonusShopMenu extends InventoryGUI {
    private final static FileConfiguration config = FireballFight.INSTANCE.getConfig();

    public BonusShopMenu(boolean updateTick) {
        super(updateTick);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, config.getInt("Menus.BonusShop.Size"), Utils.color(FireballFight.INSTANCE.getConfig().getString("Menus.BonusShop.Title")));
    }

    @Override
    public void decorate(@NotNull Player player) {
        GamePlayer playerData = DataConfiguration.getPlayerData(player.getUniqueId());

        for (String fillerItemCategory : config.getConfigurationSection("Menus.BonusShop.Fillers").getKeys(false)) {
            ItemStack fillerItem = new ItemStack(Material.getMaterial(config.getString("Menus.BonusShop.Fillers." + fillerItemCategory + ".Material")), 1,
                    (short) config.getInt("Menus.BonusShop.Fillers." + fillerItemCategory + ".Data"));
            ItemMeta fillerMeta = fillerItem.getItemMeta();
            fillerMeta.setDisplayName(" ");
            fillerItem.setItemMeta(fillerMeta);
            for (int slot : config.getIntegerList("Menus.BonusShop.Fillers." + fillerItemCategory + ".Slots")) {
                this.getInventory().setItem(slot - 1, fillerItem);
            }
        }

        for (String bonusItemName : config.getConfigurationSection("Menus.BonusShop.Items").getKeys(false)) {
            ItemStack sellingItem = new ItemStack(Material.getMaterial(config.getString("Menus.BonusShop.Items." + bonusItemName + ".Material")));
            ItemMeta sellingItemMeta = sellingItem.getItemMeta();
            sellingItemMeta.setDisplayName(Utils.color(config.getString("Menus.BonusShop.Items." + bonusItemName + ".Name")));
            ArrayList<String> sellingItemLore = new ArrayList<>();
            for (String s : config.getStringList("Menus.BonusShop.Items." + bonusItemName + ".Lore")) {
                sellingItemLore.add(Utils.color(s));
            }
            sellingItemMeta.setLore(sellingItemLore);
            sellingItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            sellingItem.setItemMeta(sellingItemMeta);
            InventoryButton button = new InventoryButton()
                    .creator(player1 -> sellingItem)
                    .consumer(event -> {
                        if (!playerData.getBoughtBonusItems().containsKey(Utils.stripColor(config.getString("Menus.BonusShop.Items." + bonusItemName + ".Name")))) {
                            FireballFight.guiManager.openGUI(
                                    new ItemPurchaseMenu(
                                            sellingItem,
                                            config.getIntegerList("Menus.BonusShop.Items." + bonusItemName + ".Price.Anix"),
                                            config.getIntegerList("Menus.BonusShop.Items." + bonusItemName + ".Price.Bonus"),
                                            config.getStringList("Menus.BonusShop.Items." + bonusItemName + ".Price.Benefit"),
                                            false
                                    ), player);
                        } else {
                            itemWasAlreadyBought(player);
                        }
                    });
            this.setButton(config.getInt("Menus.BonusShop.Items." + bonusItemName + ".Slot") - 1, button);
        }

        super.decorate(player);
    }

    private void itemWasAlreadyBought(@NotNull Player player) {
        player.closeInventory();
        Utils.sendMessage(player, config.getString("Messages.ItemWasAlreadyBought"));
        Sounds.ORB_PICKUP.play(player);
    }
}
