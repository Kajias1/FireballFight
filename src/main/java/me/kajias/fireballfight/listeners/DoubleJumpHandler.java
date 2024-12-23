package me.kajias.fireballfight.listeners;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Sounds;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.configurations.DataConfiguration;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.Game;
import me.kajias.fireballfight.objects.GamePlayer;
import me.kajias.fireballfight.objects.enums.ArenaState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DoubleJumpHandler implements Listener
{
    private final List<UUID> playerDisableFlyList = new ArrayList<>();

    @EventHandler
    public void setVelocity(PlayerToggleFlightEvent e) {
        FileConfiguration config = FireballFight.INSTANCE.getConfig();
        Player player = e.getPlayer();

        if (Arena.getPlayerArenaMap().containsKey(player)) {

            Game game = Arena.getPlayerArenaMap().get(player).getGame();
            GamePlayer playerData = DataConfiguration.getPlayerData(player.getUniqueId());

            if (game != null && game.hasStarted() && Arena.getPlayerArenaMap().get(player).getState() == ArenaState.STARTED) {
                e.setCancelled(true);
                if (playerData != null && !playerDisableFlyList.contains(player.getUniqueId()) && playerData.getBoughtBonusItems().containsKey(
                        Utils.stripColor(config.getString("Menus.BonusShop.Items.DoubleJump.Name"))) && !game.getRespawningPlayers().contains(player.getName()) &&
                            !game.getSpectators().contains(player.getName())) {
                    playerDisableFlyList.add(player.getUniqueId());
                    player.setAllowFlight(false);
                    player.setVelocity(e.getPlayer().getLocation().getDirection().normalize().setY(1.1f));
                    Sounds.ENDERDRAGON_WINGS.play(player);

                    new BukkitRunnable() {
                        final int coolDownMax = config.getInt("Game.DoubleJumpCoolDownTicks");
                        int coolDown = coolDownMax;
                        String coolDownBar;

                        @Override
                        public void run() {
                            coolDownBar = Utils.color(
                                    new String(new char[coolDownMax - coolDown]).replace("\0", "&6|") + new String(new char[coolDown]).replace("\0", "&8|"));
                            Utils.sendActionBar(player, coolDownBar + "   ");
                            coolDown--;
                            if (coolDown <= 0) {
                                coolDownBar = Utils.color(
                                        new String(new char[coolDownMax]).replace("\0", "&a|"));
                                Utils.sendActionBar(player, coolDownBar + "   ");
                                playerDisableFlyList.remove(player.getUniqueId());
                                this.cancel();
                                player.setAllowFlight(true);
                            }
                        }
                    }.runTaskTimer(FireballFight.INSTANCE, 0L, 2L);
                }
            }
        }
    }
}
