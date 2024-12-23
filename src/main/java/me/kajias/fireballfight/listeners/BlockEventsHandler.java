package me.kajias.fireballfight.listeners;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Sounds;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.commands.AdminCommand;
import me.kajias.fireballfight.configurations.DataConfiguration;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.Game;
import me.kajias.fireballfight.objects.GamePlayer;
import me.kajias.fireballfight.objects.enums.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockEventsHandler implements Listener
{
    private static final FileConfiguration config = FireballFight.INSTANCE.getConfig();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (Arena.getPlayerArenaMap().containsKey(e.getPlayer())) {
            Player player = e.getPlayer();
            Arena arena = Arena.getPlayerArenaMap().get(player);
            Game game = arena.getGame();
            if (game != null && game.hasStarted() && e.getBlock() != null ) {
                if (game.getRespawningPlayers().contains(player.getName()) || game.getSpectators().contains(player.getName())) {
                    e.setCancelled(true);
                    return;
                }
                if (e.getBlock().getType() == Material.ENDER_STONE || e.getBlock().getType() == Material.WOOD) return;
                if (e.getBlock().getType() == Material.BED_BLOCK) {
                    GamePlayer playerData = arena.getGame().getPlayersData().stream().filter(x -> x.getUniqueId().equals(player.getUniqueId())).findAny().orElse(null);
                    if (arena.getTeamType(player) == TeamType.RED &&
                            e.getBlock().getLocation().distance(arena.getSpawnLoc(TeamType.BLUE)) <= e.getBlock().getLocation().distance(arena.getSpawnLoc(TeamType.RED))) {
                        game.isBlueTeamBedDestroyed = true;
                        arena.getPlayers().forEach(p -> Utils.sendMessage(p, Utils.color(config.getString("Messages.PlayerDestroyedBed.Blue"))
                                .replace("%name%", Utils.getInGamePlayerName(player))));
                        if (playerData != null) {
                            playerData.setBedsDestroyed(playerData.getBedsDestroyed() + 1);
                            DataConfiguration.savePlayerData(playerData);
                        }
                        arena.getPlayers().forEach(x -> {
                            if (arena.getTeamType(x) == TeamType.BLUE) {
                                x.sendTitle(Utils.color(config.getString("Messages.BedDestroyedMessage")), "");
                                Bukkit.getScheduler().runTaskLater(FireballFight.INSTANCE, () -> {player.sendTitle("", "");}, 4 * 20L);
                                Sounds.WITHER_DEATH.play(x);
                            }
                        });
                    } else if (arena.getTeamType(player) == TeamType.BLUE &&
                            e.getBlock().getLocation().distance(arena.getSpawnLoc(TeamType.RED)) <= e.getBlock().getLocation().distance(arena.getSpawnLoc(TeamType.BLUE))) {
                        game.isRedTeamBedDestroyed = true;
                        arena.getPlayers().forEach(p -> Utils.sendMessage(p, Utils.color(config.getString("Messages.PlayerDestroyedBed.Red"))
                                .replace("%name%", Utils.getInGamePlayerName(player))));
                        if (playerData != null) {
                            playerData.setBedsDestroyed(playerData.getBedsDestroyed() + 1);
                            DataConfiguration.savePlayerData(playerData);
                        }
                        arena.getPlayers().forEach(x -> {
                            if (arena.getTeamType(x) == TeamType.RED) {
                                x.sendTitle(Utils.color(config.getString("Messages.BedDestroyedMessage")), "");
                                Bukkit.getScheduler().runTaskLater(FireballFight.INSTANCE, () -> {player.sendTitle("", "");}, 4 * 20L);
                                Sounds.WITHER_DEATH.play(x);
                            }
                        });
                    } else e.setCancelled(true);
                    return;
                }
                if (!game.getPlacedBlocks().contains(e.getBlock()) || e.getBlock().getType() == Material.SLIME_BLOCK) e.setCancelled(true);
                else return;
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (Arena.getPlayerArenaMap().containsKey(e.getPlayer())) {
            Player player = e.getPlayer();
            Game game = Arena.getPlayerArenaMap().get(player).getGame();

            if (game != null && game.hasStarted()) {
                game.getPlacedBlocks().add(e.getBlockPlaced());

                if (e.getBlock().getType() == Material.TNT) {
                    e.getBlock().setType(Material.AIR);
                    TNTPrimed tnt = (TNTPrimed) e.getBlock().getWorld().spawnEntity(e.getBlock().getLocation(), EntityType.PRIMED_TNT);
                    tnt.setFuseTicks(3 * 20);
                    Utils.applyTimerToTNT(tnt, player);
                    PlayerInteractItemHandler.TNT_PRIMED_PLAYER_HASH_MAP.put(tnt, player);
                    Sounds.FIRE_IGNITE.play(player);
                }
            }
        }
    }
}