package me.kajias.fireballfight.gui.menus;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.gui.InventoryButton;
import me.kajias.fireballfight.gui.InventoryGUI;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.ArenaManager;
import me.kajias.fireballfight.objects.enums.ArenaState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaSelectMenu extends InventoryGUI
{
   private static final FileConfiguration config = FireballFight.INSTANCE.getConfig();

   public ArenaSelectMenu() {
      super(true);
   }

   @Override
   protected Inventory createInventory() {
      return Bukkit.createInventory(null, config.getInt("Menus.ArenaSelect.Size"), ChatColor.translateAlternateColorCodes('&', config.getString("Menus.ArenaSelect.Title")));
   }

   @Override
   public void decorate(Player player) {
      List<Integer> arenaSlots = config.getIntegerList("Menus.ArenaSelect.Slots");

      ItemStack fillerItem = new ItemStack(Material.STAINED_GLASS, 1, (short) 15);
      ItemMeta fillerItemMeta = fillerItem.getItemMeta();
      fillerItemMeta.setDisplayName(" ");
      fillerItem.setItemMeta(fillerItemMeta);
      for (int i : arenaSlots) {
         this.getInventory().setItem(i, fillerItem);
      }

      List<Arena> arenas = ArenaManager.getLoadedArenas().stream().filter(a -> a.getState() == ArenaState.WAITING)
              .sorted(Comparator.comparing(arena -> arena.getPlayers().size())).collect(Collectors.toList());
      Collections.reverse(arenas);

      for (int i = 0; i < arenaSlots.size() + 1; i++) {
         Arena arena;

         try {
            arena = arenas.get(i);

            ItemStack arenaIcon = new ItemStack(Material.getMaterial(config.getString("Menus.ArenaSelect.Material")), Integer.max(1, arena.getPlayers().size()), (short) (arena.getPlayers().isEmpty() ? 5 : 1));
            ItemMeta arenaIconMeta = arenaIcon.getItemMeta();
            arenaIconMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("Menus.ArenaSelect.DisplayName").replace("%arena_name%", arena.getName())));
            List<String> arenaIconLore = new ArrayList<>();
            for (String s : config.getStringList("Menus.ArenaSelect.Lore")) {
               arenaIconLore.add(ChatColor.translateAlternateColorCodes('&', s
                       .replace("%arena_players%", String.valueOf(arena.getPlayers().size()))
                       .replace("%arena_players_total%", String.valueOf(arena.getAllowedPlayersAmount()))
                       .replace("%arena_type%", arena.getArenaType().toString())
                       .replace("%arena_name%", arena.getName())
               ));
            }
            arenaIconMeta.setLore(arenaIconLore);
            arenaIcon.setItemMeta(arenaIconMeta);
            InventoryButton arenaJoinButton = new InventoryButton()
                    .creator(player1 -> arenaIcon)
                    .consumer(event -> {
                       arena.addPlayer(player);
                       player.closeInventory();
                    });
            int slot = arenaSlots.get(i + 1);
            this.setButton(slot - 1, arenaJoinButton);
         } catch (IndexOutOfBoundsException e) {
            break;
         }
      }

      super.decorate(player);
   }
}
