package me.kajias.fireballfight.listeners;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.configurations.DataConfiguration;
import me.kajias.fireballfight.objects.GamePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitJoinHandler implements Listener
{
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Utils.teleportToLobby(e.getPlayer());
        
        GamePlayer gamePlayer = DataConfiguration.getPlayerDataFromConfig(e.getPlayer().getUniqueId());
        gamePlayer.setPlayerName(e.getPlayer().getName());
        if (!FireballFight.INSTANCE.loadedPlayerData.isEmpty()) {
            if (FireballFight.INSTANCE.loadedPlayerData.stream().noneMatch(x -> x.getUniqueId().equals(e.getPlayer().getUniqueId()))) {
                FireballFight.INSTANCE.loadedPlayerData.add(gamePlayer);
            }
        } else {
            FireballFight.INSTANCE.loadedPlayerData.add(gamePlayer);
        }
        DataConfiguration.savePlayerData(gamePlayer);
        
        e.setJoinMessage("");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage("");
    }
}
