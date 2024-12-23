package me.kajias.fireballfight.listeners;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.objects.Arena;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathHandler implements Listener {
    private static final FileConfiguration config = FireballFight.INSTANCE.getConfig();

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity().getPlayer();
        if (Arena.getPlayerArenaMap().containsKey(player)) {
            e.setDeathMessage("");
            e.setDroppedExp(0);

            Arena arena = Arena.getPlayerArenaMap().get(player);

            if (arena != null && arena.getGame() != null && arena.getGame().hasStarted()) {
                arena.getGame().handleDeath(player, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            }
        }
    }
}
