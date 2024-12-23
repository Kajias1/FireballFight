package me.kajias.fireballfight.listeners;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Sounds;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.enums.ArenaState;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Effect;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerInteractEntityHandler implements Listener
{
    private final static FileConfiguration config = FireballFight.INSTANCE.getConfig();

    @EventHandler
    public void onPlayerInteractWithEntity(PlayerInteractAtEntityEvent e) {
        Entity clickedEntity = e.getRightClicked();
        Player player = e.getPlayer();

        if (clickedEntity instanceof ArmorStand) {
            e.setCancelled(true);
            if (Arena.getPlayerArenaMap().containsKey(e.getPlayer()) && Arena.getPlayerArenaMap().get(e.getPlayer()).getState() == ArenaState.STARTED) {
                if (clickedEntity.getCustomName().equals(Utils.color(config.getString("Messages.HealthRuneHologram")))) {
                    player.setHealth(Math.min(20.0f, player.getHealth() + config.getDouble("Game.HealthRuneAmplifier")));
                    player.sendTitle(Utils.color(config.getString("Messages.HealthRunePickup")), "");
                }

                if (clickedEntity.getCustomName().equals(Utils.color(config.getString("Messages.SpeedRuneHologram")))) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, config.getInt("Game.SpeedRuneAmplifier") - 1, true));
                    player.sendTitle(Utils.color(config.getString("Messages.SpeedRunePickup")), "");
                }

                clickedEntity.getWorld().playEffect(clickedEntity.getLocation(), Effect.COLOURED_DUST, Integer.MAX_VALUE);
                Sounds.FIRE_IGNITE.play(player, 5, 1);
                clickedEntity.remove();
            }
        }
    }
}
