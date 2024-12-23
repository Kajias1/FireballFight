package me.kajias.fireballfight.listeners;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.ArenaManager;
import me.kajias.fireballfight.objects.Game;
import me.kajias.fireballfight.objects.enums.ArenaState;
import me.kajias.fireballfight.objects.enums.GameType;
import me.kajias.fireballfight.objects.enums.TeamType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DisabledEvents implements Listener
{
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e) {
        if (e.getEntity().getItemStack().getType() == Material.BED) e.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        e.setFoodLevel(20);
        e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof ArmorStand) {
            e.setCancelled(true);
            return;
        }

        if (e.getEntity() instanceof Player) {
            Player player = ((Player) e.getEntity()).getPlayer();
            if (Arena.getPlayerArenaMap().containsKey(player)) {
                Arena arena = Arena.getPlayerArenaMap().get(player);
                Game game = arena.getGame();

                if (arena.getState() != ArenaState.STARTED) {
                    e.setCancelled(true);
                    return;
                }

                if (e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    e.setCancelled(true);
                    return;
                }

                if (game != null && game.hasStarted()) {
                    if (game.getRespawningPlayers().contains(player.getName()) || game.getSpectators().contains(player.getName())) e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlocksExplode(EntityExplodeEvent e) {
        if (e.getEntityType() == EntityType.FIREBALL || e.getEntityType() == EntityType.PRIMED_TNT) {
            e.setCancelled(true);
            Arena arena = ArenaManager.getLoadedArenas().stream().filter(x -> x.getWorldName().equals(e.getLocation().getWorld().getName())).findAny().orElse(null);
            if (arena != null) {
                Game game = arena.getGame();
                if (game != null && game.hasStarted()) {
                    for (Block block : e.blockList()) {
                        if (block.getType() == Material.ENDER_STONE || block.getType() == Material.WOOD) block.setType(Material.AIR);
                        else if (game.getPlacedBlocks().contains(block) && block.getType() != Material.SLIME_BLOCK) block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e){
        FileConfiguration config = FireballFight.INSTANCE.getConfig();
        Location l = e.getLocation();
        double radius = 3.5f;
        double powerV = config.getDouble("Game.ExplosionPushPower.Fireball.Vertical");
        double powerH = config.getDouble("Game.ExplosionPushPower.Fireball.Horizontal");

        List<Entity> nearbyEntities = (List<Entity>) l.getWorld().getNearbyEntities(l, radius, radius, radius);
        if(e.getEntityType() == EntityType.FIREBALL) {
            Player shooter = (Player) ((Fireball) e.getEntity()).getShooter();

            for (Entity entity : nearbyEntities) {
                if (entity instanceof Player) {
                    Player player = ((Player) entity).getPlayer();
                    if (Arena.getPlayerArenaMap().containsKey(player)) {
                        Arena arena = Arena.getPlayerArenaMap().get(player);
                        if (arena.getGameType() == GameType.X) powerV *= config.getDouble("Game.ExplosionPushPower.MultiplierWhenModeX");
                        if (player == shooter || arena.getTeamType(player) != arena.getTeamType(shooter)) {
                            pushAway((LivingEntity) entity, l, powerV, player.isSprinting() ? powerH + config.getDouble("Game.ExplosionPushPower.AdditionWhenSprinting") : powerH);
                        }
                    }
                }
            }
        }
        if(e.getEntityType() == EntityType.PRIMED_TNT) {
            Player shooter = PlayerInteractItemHandler.TNT_PRIMED_PLAYER_HASH_MAP.get((TNTPrimed) e.getEntity());
            powerH = config.getDouble("Game.ExplosionPushPower.Tnt.Vertical");
            powerV = config.getDouble("Game.ExplosionPushPower.Tnt.Horizontal");

            if (shooter != null) {
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof Player) {
                        Player player = ((Player) entity).getPlayer();
                        if (Arena.getPlayerArenaMap().containsKey(player)) {
                            Arena arena = Arena.getPlayerArenaMap().get(player);
                            if (arena.getGameType() == GameType.X) powerV *= config.getDouble("Game.ExplosionPushPower.MultiplierWhenModeX");
                            if (PlayerInteractItemHandler.TNT_PRIMED_PLAYER_HASH_MAP.containsKey((TNTPrimed) e.getEntity())) {
                                if (player == shooter || arena.getTeamType(player) != arena.getTeamType(shooter)) {
                                    pushAway((LivingEntity) entity, l, player.isSprinting() ? powerH + config.getDouble("Game.ExplosionPushPower.AdditionWhenSprinting") : powerH, powerV);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void pushAway(LivingEntity player, Location l, double hf, double rf) {
        final Location loc = player.getLocation();

        double hf1 = Math.max(-4, Math.min(4, hf));
        double rf1 = Math.max(-4, Math.min(4, -1*rf));

        player.setVelocity(l.toVector().subtract(loc.toVector()).normalize().multiply(rf1).setY(hf1));
    }

    @EventHandler
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            Player player = (Player) e.getEntity();
            Player attacker = (Player) e.getDamager();

            if (Arena.getPlayerArenaMap().containsKey(player) && Arena.getPlayerArenaMap().containsKey(attacker)) {
                Arena arena = Arena.getPlayerArenaMap().get(player);
                if (arena.getState() == ArenaState.STARTED) {
                    if (arena.getTeamType(player) == arena.getTeamType(attacker)) e.setCancelled(true);
                    arena.getGame().getPlayerDamagerMap().put(player, attacker);
                }
            }
        }
        if (e.getEntity() instanceof Fireball || e.getEntity() instanceof TNTPrimed) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommandExecution(PlayerCommandPreprocessEvent e) {
        FileConfiguration config = FireballFight.INSTANCE.getConfig();
        if (!e.getPlayer().isOp()) {
            if (Arena.getPlayerArenaMap().get(e.getPlayer()) != null && !e.getMessage().equals("/leave")) {
                Utils.sendMessage(e.getPlayer(), config.getString("Messages.NoCommandsInGame"));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        FileConfiguration config = FireballFight.INSTANCE.getConfig();
        Player player = e.getPlayer();
        String format = config.getString("Messages.GameChatFormat.Default");
        Arena arena = Arena.getPlayerArenaMap().get(player);
        if (arena != null && (arena.getState() == ArenaState.STARTED || arena.getState() == ArenaState.ENDING)) {
            format = config.getString("Messages.GameChatFormat.Team");
            if (!e.getMessage().isEmpty() && e.getMessage().toCharArray()[0] == '!') {
                e.setMessage(e.getMessage().replace("!", ""));
                format = config.getString("Messages.GameChatFormat.Global");
            } else {
                e.getRecipients().removeIf(recipient -> arena.getTeamType(recipient) != arena.getTeamType(e.getPlayer()));
            }
            if (arena.getTeamType(player) == TeamType.RED) format = format.replace("%team_color%", "&c");
            else if (arena.getTeamType(player) == TeamType.BLUE) format = format.replace("%team_color%", "&9");
        }
        e.getRecipients().removeIf(recipient -> recipient.getWorld() != e.getPlayer().getWorld());
        e.setFormat(ChatColor.translateAlternateColorCodes('&', format));
    }
}
