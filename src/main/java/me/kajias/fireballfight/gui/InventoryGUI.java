package me.kajias.fireballfight.gui;

import me.kajias.fireballfight.FireballFight;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public abstract class InventoryGUI implements InventoryHandler
{
    private final Inventory inventory;
    private final Map<Integer, InventoryButton> buttonMap = new HashMap<>();
    private final boolean updateTick;

    public InventoryGUI(boolean updateTick) {
        this.inventory = this.createInventory();
        this.updateTick = updateTick;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void setButton(int slot, InventoryButton button) {
        this.buttonMap.put(slot, button);
    }

    public void decorate(Player player) {
        this.buttonMap.forEach((slot, button) -> {
            ItemStack icon = button.getIconCreator().apply(player);
            this.inventory.setItem(slot, icon);
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        InventoryButton button = this.buttonMap.get(slot);
        if (button != null) {
            button.getEventConsumer().accept(event);
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        if (updateTick) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    decorate((Player) event.getPlayer());
                    if (!event.getViewers().contains(event.getPlayer())) cancel();
                }
            }.runTaskTimer(FireballFight.INSTANCE, 0L, 10L);
        } else decorate((Player) event.getPlayer());
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    protected abstract Inventory createInventory();
}
