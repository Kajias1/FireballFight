package me.kajias.fireballfight.listeners;

import me.kajias.fireballfight.commands.AdminCommand;
import me.kajias.fireballfight.objects.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickHandler implements Listener
{
    @EventHandler
    public void onInventoryItemClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (!Arena.getPlayerArenaMap().containsKey(player) || !AdminCommand.setupMap.containsKey(player)) return;

        e.setCancelled(true);
    }
}
