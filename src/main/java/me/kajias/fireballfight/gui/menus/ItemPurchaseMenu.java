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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemPurchaseMenu extends InventoryGUI
{
    private final static FileConfiguration config = FireballFight.INSTANCE.getConfig();
    private final static List<Integer> yellowFillerSlots = Arrays.asList(0, 1, 2, 3, 4, 5, 9, 18, 19, 20, 21, 22, 23);
    private final static List<Integer> purpleFillerSlots = Arrays.asList(6, 7, 8, 15, 17, 24, 25, 26);
    private final static List<Integer> sellingItemSlots = Arrays.asList(10, 11, 12, 13, 14, 16);
    private final static List<Integer> sellingItemPeriods = Arrays.asList(15, 31, 60, 90, 170, 365);
    private final static int inventorySize = 27;

    private final ItemStack sellingItem;
    private final List<Integer> pricesAnix;
    private final List<Integer> pricesBonus;
    private final List<String> benefit;

    public ItemPurchaseMenu(ItemStack sellingItem, List<Integer> pricesAnix, List<Integer> pricesBonus, List<String> benefit, boolean updateTick) {
        super(updateTick);
        this.sellingItem = sellingItem;
        this.pricesAnix = pricesAnix;
        this.pricesBonus = pricesBonus;
        this.benefit = benefit;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, inventorySize, Utils.color(FireballFight.INSTANCE.getConfig().getString("Menus.ItemPurchaseMenu.Title")));
    }

    @Override
    public void decorate(Player player) {
        ItemStack fillerYellow = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
        ItemMeta fillerYellowMeta = fillerYellow.getItemMeta();
        fillerYellowMeta.setDisplayName(" ");
        fillerYellow.setItemMeta(fillerYellowMeta);
        ItemStack fillerPurple = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 2);
        ItemMeta fillerPurpleMeta = fillerPurple.getItemMeta();
        fillerPurpleMeta.setDisplayName(" ");
        fillerPurple.setItemMeta(fillerPurpleMeta);

        for (int i : yellowFillerSlots) {
            this.setButton(i, new InventoryButton()
                    .creator(player1 -> fillerYellow)
                    .consumer(event -> {}));
        }
        for (int i : purpleFillerSlots) {
            this.setButton(i, new InventoryButton()
                    .creator(player1 -> fillerPurple)
                    .consumer(event -> {}));
        }

        int index = 0;
        for (int i : sellingItemSlots) {
            ArrayList<String> sellingItemDescription = new ArrayList<>();
            ItemStack sellingItemIcon = sellingItem.clone();
            ItemMeta sellingItemMeta = sellingItemIcon.getItemMeta();

            List<String> descriptionFormat = config.getStringList("Menus.ItemPurchaseMenu.ItemDescriptionFormat.BothPrices");
            int period = sellingItemPeriods.get(index);
            if (period == sellingItemPeriods.get(sellingItemPeriods.size() - 1)) descriptionFormat = config.getStringList("Menus.ItemPurchaseMenu.ItemDescriptionFormat.AnixPrices");
            int priceAnix = pricesAnix.get(index);
            int priceBonus;
            try {
                priceBonus = pricesBonus.get(index);
            } catch (IndexOutOfBoundsException e) {
                priceBonus = 0;
            }
            for (String s : descriptionFormat) {
                s = s.replace("%price_anix%", String.valueOf(priceAnix));
                if (priceBonus >= 0)
                    s = s.replace("%price_bonus%", String.valueOf(priceBonus));
                try {
                    s = s.replace("%color%", config.getStringList("Menus.ItemPurchaseMenu.ItemDescriptionFormat.Colors").get(index));
                } catch (IndexOutOfBoundsException e) {
                    s = s.replace("%color%", "");
                };
                s = s.replace("%benefit%", benefit.get(index));
                s = s.replace("%period%", String.valueOf(period));
                sellingItemDescription.add(Utils.color( s));
            }
            index++;

            sellingItemMeta.setLore(sellingItemDescription);
            sellingItemIcon.setItemMeta(sellingItemMeta);

            GamePlayer playerData = DataConfiguration.getPlayerData(player.getUniqueId());

            int finalPriceBonus = priceBonus;
            this.setButton(i, new InventoryButton()
                    .creator(player1 -> sellingItemIcon)
                    .consumer(event -> {
                        if (playerData != null) {
                            if (event.getClick().isRightClick() && event.getSlot() == sellingItemSlots.get(sellingItemSlots.size() - 1)) {
                                Utils.sendMessage(player, config.getString("Messages.CantBuyForBonuses"));
                            } else {
                                boolean sufficientMoney = false;
                                if (event.getClick().isLeftClick()) {
                                    if (FireballFight.INSTANCE.getPlayerPoints().getAPI().look(player.getUniqueId()) >= priceAnix) {
                                        sufficientMoney = true;
                                        FireballFight.INSTANCE.getPlayerPoints().getAPI().take(player.getUniqueId(), priceAnix);
                                    }
                                }
                                if (event.getClick().isRightClick() && event.getSlot() != sellingItemSlots.get(sellingItemSlots.size() - 1)) {
                                    if (FireballFight.INSTANCE.getEconomy().getBalance(Bukkit.getOfflinePlayer(player.getUniqueId())) >= finalPriceBonus) {
                                        sufficientMoney = true;
                                        FireballFight.INSTANCE.getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()), priceAnix);
                                    }
                                }

                                if (!sufficientMoney) {
                                    Utils.sendMessage(player, config.getString("Messages.InsufficientMoney"));
                                } else {
                                    Utils.sendMessage(player, config.getString("Messages.ItemPurchaseSuccessful"));

                                    playerData.getBoughtBonusItems().put(ChatColor.stripColor(sellingItem.getItemMeta().getDisplayName()), period);
                                    DataConfiguration.savePlayerData(playerData);
                                }
                                DataConfiguration.savePlayerData(playerData);
                            }
                        }
                        player.closeInventory();
                        Sounds.ORB_PICKUP.play(player);
                    }));
        }

        super.decorate(player);
    }
}
