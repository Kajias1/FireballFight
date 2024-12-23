package me.kajias.fireballfight.listeners;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Sounds;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.commands.AdminCommand;
import me.kajias.fireballfight.configurations.DataConfiguration;
import me.kajias.fireballfight.gui.menus.ArenaSelectMenu;
import me.kajias.fireballfight.gui.menus.BonusShopMenu;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.ArenaManager;
import me.kajias.fireballfight.objects.Game;
import me.kajias.fireballfight.objects.GamePlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;

public class PlayerInteractItemHandler implements Listener
{
    private static final FileConfiguration config = FireballFight.INSTANCE.getConfig();

    public static final HashMap<TNTPrimed, Player> TNT_PRIMED_PLAYER_HASH_MAP = new HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null) return;

        Player player = e.getPlayer();

        if (AdminCommand.setupMap.containsKey(player)) return;

        if (e.getItem().hasItemMeta() && e.getItem().getItemMeta().hasDisplayName()) {
            if (e.getItem().getItemMeta().getDisplayName().equals(Utils.color(config.getString("Items.FastJoinItem.Name")))) {
                e.setCancelled(true);
                Arena arena = ArenaManager.findBestArena();
                if (arena != null) {
                    arena.addPlayer(player);
                } else Utils.sendMessage(player, config.getString("Messages.NoAvailableArenas"));
                return;
            }

            if (e.getItem().getItemMeta().getDisplayName().equals(Utils.color(config.getString("Items.LeaveGameItem.Name")))) {
                e.setCancelled(true);
                if (Arena.getPlayerArenaMap().containsKey(player)) {
                    Arena.getPlayerArenaMap().get(player).removePlayer(player);
                } else Utils.sendMessage(player, config.getString("Messages.NotInGame"));
                return;
            }

            if (e.getItem().getItemMeta().getDisplayName().equals(Utils.color(config.getString("Items.NewGameItem.Name")))) {
                e.setCancelled(true);
                Arena arena = ArenaManager.findBestArena();
                if (arena != null) {
                    arena.addPlayer(player);
                } else Utils.sendMessage(player, config.getString("Messages.NoAvailableArenas"));
                return;
            }

            if (e.getItem().getItemMeta().getDisplayName().equals(Utils.color(config.getString("Items.SlimePlatformItem.Name")))) {
                e.setCancelled(true);
                player.setVelocity(new Vector(player.getVelocity().getX(), 1.5f, player.getVelocity().getZ()));
                Arena arena = Arena.getPlayerArenaMap().get(player);
                if (arena != null && arena.getGame() != null && arena.getGame().hasStarted()) {
                    for (int dx = 0; dx < 3; ++dx) {
                        for (int dz = 0; dz < 3; ++dz) {
                            Block block = player.getLocation().add(dx - 1, -2, dz - 1).getBlock();
                            if (block.getType() == Material.AIR) {
                                block.setType(Material.SLIME_BLOCK);
                                arena.getGame().getPlacedBlocks().add(block);
                                Bukkit.getScheduler().runTaskLater(FireballFight.INSTANCE, () -> {
                                    block.setType(Material.AIR);
                                    arena.getGame().getPlacedBlocks().remove(block);
                                }, 5 * 20L);
                            }
                        }
                    }
                }
                if (player.getItemInHand().getAmount() > 1)
                    player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
                else player.getInventory().remove(player.getItemInHand());
                Sounds.ORB_PICKUP.play(player);
                return;
            }

            if (e.getItem().getItemMeta().getDisplayName().equals(Utils.color(config.getString("Items.TrampolineItem.Name")))) {
                e.setCancelled(true);
                int radius = config.getInt("Game.TrampolineRadius");
                List<Entity> nearbyEntities = (List<Entity>) player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius);
                if (nearbyEntities.size() > 1) {
                    for (Entity entity : nearbyEntities) {
                        if (entity instanceof Player) {
                            Player p = ((Player) entity).getPlayer();
                            p.setFallDistance(0.0f);
                            p.setVelocity(new Vector(0, 4, 0));
                            if (p != player) p.sendTitle(Utils.color(config.getString("Messages.TrampolineUsedByPlayer").replace("%name%", player.getName())), "");
                            Sounds.FIREWORK_LAUNCH.play(p);
                        }
                    }

                    if (player.getItemInHand().getAmount() > 1)
                        player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
                    else player.getInventory().remove(player.getItemInHand());
                } else Utils.sendMessage(player, config.getString("Messages.NoPlayersInRadius"));
                return;
            }

            if (e.getItem().getItemMeta().getDisplayName().equals(Utils.color(config.getString("Items.BonusShopItem.Name")))) {
                e.setCancelled(true);
                FireballFight.guiManager.openGUI(new BonusShopMenu(false), e.getPlayer());
                return;
            }

            if (e.getItem().getItemMeta().getDisplayName().equals(Utils.color(config.getString("Items.SelectArenaItem.Name")))) {
                e.setCancelled(true);
                FireballFight.guiManager.openGUI(new ArenaSelectMenu(), e.getPlayer());
                return;
            }
        }

        if (Arena.getPlayerArenaMap().containsKey(player)) {
            Game game = Arena.getPlayerArenaMap().get(player).getGame();

            if (game != null && game.hasStarted()) {
                if (e.getItem().getType() == Material.TNT) {
                    if (e.getAction() == Action.RIGHT_CLICK_AIR) {
                        GamePlayer playerData = DataConfiguration.getPlayerData(player.getUniqueId());
                        if (playerData != null && playerData.getBoughtBonusItems().containsKey(Utils.stripColor(config.getString("Menus.BonusShop.Items.ThrowableTNT.Name")))) {
                            TNTPrimed tnt = player.getWorld().spawn(player.getEyeLocation(), TNTPrimed.class);
                            tnt.setVelocity(player.getEyeLocation().getDirection().normalize());
                            tnt.setFuseTicks(3 * 20);
                            Utils.applyTimerToTNT(tnt, player);
                            PlayerInteractItemHandler.TNT_PRIMED_PLAYER_HASH_MAP.put(tnt, player);
                            Sounds.FIRE_IGNITE.play(player);
                            if (player.getItemInHand().getAmount() > 1)
                                player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
                            else player.getInventory().remove(player.getItemInHand());
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
                if (e.getItem().getType() == Material.FIREBALL) {
                    if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        Fireball fireball = player.launchProjectile(Fireball.class);
                        fireball.setVelocity(player.getLocation().getDirection().normalize());
                        fireball.setYield(2.0F);
                        fireball.setShooter(player);
                        fireball.setFireTicks(0);
                        if (player.getItemInHand().getAmount() > 1) player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
                        else player.getInventory().remove(player.getItemInHand());

                        Bukkit.getScheduler().runTaskLater(FireballFight.INSTANCE, () -> {
                            if (!fireball.isDead()) fireball.remove();
                        }, 200L);
                    }
                    e.setCancelled(true);
                }
            }
        }
    }
}
