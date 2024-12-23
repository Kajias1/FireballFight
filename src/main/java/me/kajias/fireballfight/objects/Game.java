package me.kajias.fireballfight.objects;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Sounds;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.configurations.DataConfiguration;
import me.kajias.fireballfight.objects.enums.ArenaState;
import me.kajias.fireballfight.objects.enums.RuneType;
import me.kajias.fireballfight.objects.enums.TeamType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Game
{
    private static final FileConfiguration config = FireballFight.INSTANCE.getConfig();
    private static final int startCountDown = config.getInt("Game.StartCountDownValue");
    private static final int gameDuration = config.getInt("Game.GameDuration");

    private final Arena arena;
    private boolean isStarted;
    private int countDown;
    private int gameTime;
    private BukkitTask countDownTask;
    private BukkitTask gameTimerTask;
    private BukkitTask gameTickTask;

    private final List<Block> placedBlocks;
    private final List<String> respawningPlayers;
    private final List<String> spectators;
    private final List<GamePlayer> playersData;
    private final HashMap<Player, Player> playerDamagerMap;

    public boolean isRedTeamBedDestroyed;
    public boolean isBlueTeamBedDestroyed;

    public Game(Arena arena) {
        this.arena = arena;
        isStarted = false;
        gameTime = 0;
        placedBlocks = new ArrayList<>();
        isRedTeamBedDestroyed = false;
        isBlueTeamBedDestroyed = false;
        respawningPlayers = new ArrayList<>();
        spectators = new ArrayList<>();
        playersData = new ArrayList<>();
        playerDamagerMap = new HashMap<>();
    }

    public void startCountDownTimer() {
        countDown = startCountDown;
        arena.setState(ArenaState.STARTING);

        countDownTask = Bukkit.getScheduler().runTaskTimer(FireballFight.INSTANCE, () -> {
            for(Player player : arena.getPlayers()) player.setLevel(countDown);

            if (countDown == startCountDown || countDown == 10 || countDown >= 1 && countDown <= 5) {
                for (Player player : arena.getPlayers()) {
                    Sounds.CLICK.play(player);
                    Utils.sendMessage(player, config.getString("Messages.StartCountDown").replace("%time%", String.valueOf(countDown)));
                }
            } else if(countDown == 0) {
                startGame();
            }

            countDown--;
        }, 0L,  20L);
    }

    public void stopCountDownTimer() {
        arena.setState(ArenaState.WAITING);
        countDown = startCountDown;
        countDownTask.cancel();
        for (Player p : arena.getPlayers()) p.setLevel(0);
    }

    public void startGame() {
        if (!isStarted) {
            isStarted = true;
            arena.setState(ArenaState.STARTED);
            countDownTask.cancel();
            arena.getRunes().forEach(Rune::spawnArmorStand);

            arena.getPlayers().forEach(player -> {
                Utils.removePotionEffects(player);
                player.setMaxHealth(20);
                player.setHealth(player.getMaxHealth());
                player.setGameMode(GameMode.SURVIVAL);
                if (!arena.isInTeam(player)) {
                    if (arena.getTeam(TeamType.BLUE).size() >= arena.getTeam(TeamType.RED).size()) arena.addToTeam(TeamType.RED, player);
                    else arena.addToTeam(TeamType.BLUE, player);
                }

                Location spawnLoc = arena.getSpawnLoc(arena.getTeamType(player));
                spawnLoc.setYaw(arena.getTeamType(player) == TeamType.RED ? 179 : 0);
                player.teleport(spawnLoc);

                ItemStack[] invContents = player.getInventory().getContents();
                player.getInventory().clear();
                Utils.giveKit(player);
                Sounds.ORB_PICKUP.play(player);
                player.sendTitle(Utils.color(config.getString("Messages.GameStartMessage")), "");
                playersData.add(new GamePlayer(player.getUniqueId()));
                for (GamePlayer gamePlayer : FireballFight.INSTANCE.loadedPlayerData) {
                    GamePlayer playerData = playersData.stream().filter(x -> x.getUniqueId().equals(gamePlayer.getUniqueId())).findAny().orElse(null);
                    if (playerData != null) playerData.setBoughtBonusItems(gamePlayer.getBoughtBonusItems());
                }

                player.setAllowFlight(false);
                GamePlayer playerData = DataConfiguration.getPlayerData(player.getUniqueId());
                if (playerData != null && playerData.getBoughtBonusItems().containsKey(Utils.stripColor(config.getString("Menus.BonusShop.Items.DoubleJump.Name")))) {
                    player.setAllowFlight(true);
                }
                player.setFlying(false);
            });

            gameTickTask = Bukkit.getScheduler().runTaskTimer(FireballFight.INSTANCE, () -> {
                arena.getRunes().forEach(Rune::rotateArmorStand);
            }, 0L, 1L);

            gameTimerTask = Bukkit.getScheduler().runTaskTimer(FireballFight.INSTANCE, () -> {
                if (gameTime >= gameDuration) endGame(null);

                for (Player player : arena.getPlayers()) {
                    if (player.getLocation().getY() < 0 && !respawningPlayers.contains(player.getName())) {
                        handleDeath(player, EntityDamageEvent.DamageCause.VOID);
                    }
                }

                if (gameTime % config.getInt("Game.RuneRespawnTime") == 0) {
                    arena.getRunes().forEach(rune -> {
                        if (rune.getType() == RuneType.HEALTH && new Random().nextInt(config.getInt("Game.HealthRuneRespawnChance")) == 0) rune.spawnArmorStand();
                        else if (new Random().nextInt(config.getInt("Game.SpeedRuneRespawnChance")) == 0) rune.spawnArmorStand();
                    });
                }

                gameTime++;
            }, 0L, 20L);
        }
    }

    public void endGame(TeamType winnerTeam) {
        arena.setState(ArenaState.ENDING);
        countDownTask.cancel();
        gameTimerTask.cancel();
        gameTickTask.cancel();

        ItemStack newGameItem = new ItemStack(Material.getMaterial(config.getString("Items.NewGameItem.Material")));
        ItemMeta newGameItemMeta = newGameItem.getItemMeta();
        newGameItemMeta.setDisplayName(Utils.color(config.getString("Items.NewGameItem.Name")));
        newGameItem.setItemMeta(newGameItemMeta);

        ItemStack leaveGameItem = new ItemStack(Material.getMaterial(config.getString("Items.LeaveGameItem.Material")));
        ItemMeta leaveGameItemMeta = leaveGameItem.getItemMeta();
        leaveGameItemMeta.setDisplayName(Utils.color(config.getString("Items.LeaveGameItem.Name")));
        leaveGameItem.setItemMeta(leaveGameItemMeta);

        for (Player player : arena.getPlayers()) {
            GamePlayer playerData = playersData.stream().filter(x -> x.getUniqueId().equals(player.getUniqueId())).findAny().orElse(null);

            int bonusReward = 0;
            if (playerData != null) {
                playerData.setGamesPlayed(playerData.getGamesPlayed() + 1);
                bonusReward += playerData.getKills() / config.getInt("Game.BonusReward." + arena.getArenaType() + "." + arena.getGameType() + ".Kills");
            }

            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItem(config.getInt("Items.NewGameItem.Slot") - 1, newGameItem);
            player.getInventory().setItem(config.getInt("Items.LeaveGameItem.Slot") - 1, leaveGameItem);
            player.setMaxHealth(20);
            player.setHealth(player.getMaxHealth());
            player.setLevel(0);
            player.setGameMode(GameMode.ADVENTURE);
            arena.getPlayers().forEach(otherPlayer -> {
                if (otherPlayer != player) otherPlayer.showPlayer(player);
            });
            player.setAllowFlight(player.isOp());
            player.setFlying(false);

            String victoryMessage = config.getString("Messages.VictoryMessage");
            if (winnerTeam != null) {
                if (arena.getTeamType(player) != winnerTeam) victoryMessage = config.getString("Messages.DefeatMessage");
            } else victoryMessage = config.getString("Messages.DrawMessage");
            if (arena.getTeamType(player) == winnerTeam) {
                if (playerData != null) playerData.setGamesWon(playerData.getGamesWon() + 1);
                bonusReward += config.getInt("Game.BonusReward." + arena.getArenaType() + "." + arena.getGameType() + ".Victory");

                final Firework f = player.getWorld().spawn(player.getLocation(), Firework.class);
                FireworkMeta fm = f.getFireworkMeta();
                fm.addEffect(FireworkEffect.builder()
                        .flicker(true)
                        .trail(true)
                        .with(FireworkEffect.Type.STAR)
                        .with(FireworkEffect.Type.BALL)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.AQUA)
                        .withColor(Color.YELLOW)
                        .withColor(Color.RED)
                        .withColor(Color.WHITE)
                        .build());
                fm.setPower(0);
                f.setFireworkMeta(fm);
                Sounds.FIREWORK_LAUNCH.play(player);
            }
            player.sendTitle(Utils.color(victoryMessage), "");

            if (bonusReward > 0) {
                Utils.sendMessage(player, config.getString("Messages.BonusRewardMessage").replace("%bonus%", String.valueOf(bonusReward)));
                FireballFight.INSTANCE.getEconomy().depositPlayer(player, bonusReward);
            }

            respawningPlayers.remove(player.getName());

            if (playerData != null) {
                GamePlayer gamePlayer = DataConfiguration.getPlayerData(playerData.getUniqueId());
                gamePlayer.setGamesPlayed(gamePlayer.getGamesPlayed() + playerData.getGamesPlayed());
                gamePlayer.setGamesWon(gamePlayer.getGamesWon() + playerData.getGamesWon());
                gamePlayer.setKills(gamePlayer.getKills() + playerData.getKills());
                gamePlayer.setDeaths(gamePlayer.getDeaths() + playerData.getDeaths());
                gamePlayer.setBedsDestroyed(gamePlayer.getBedsDestroyed() + playerData.getBedsDestroyed());
                DataConfiguration.savePlayerData(gamePlayer);
            }
        }
        if(isStarted) Bukkit.getScheduler().scheduleSyncDelayedTask(FireballFight.INSTANCE, () -> {
            isStarted = false;
            arena.restart();
        }, 10 * 20L);
    }

    public void destroy() {
        isStarted = false;
        if (countDownTask != null) countDownTask.cancel();
        if (gameTimerTask != null) gameTimerTask.cancel();
        if (gameTickTask != null) gameTickTask.cancel();
    }

    public void putToSpectators(Player player) {
        spectators.add(player.getName());
        player.setFallDistance(0.0F);
        player.setHealth(player.getMaxHealth());
        player.setAllowFlight(true);
        player.setFlying(true);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        arena.getPlayers().forEach(otherPlayer -> {if (otherPlayer != player) otherPlayer.hidePlayer(player);});
        player.sendTitle(Utils.color(config.getString("Messages.DefeatMessage")), "");
        player.teleport(arena.getSpawnLocSpectator());

        ItemStack newGameItem = new ItemStack(Material.getMaterial(config.getString("Items.NewGameItem.Material")));
        ItemMeta newGameItemMeta = newGameItem.getItemMeta();
        newGameItemMeta.setDisplayName(Utils.color(config.getString("Items.NewGameItem.Name")));
        newGameItem.setItemMeta(newGameItemMeta);
        player.getInventory().setItem(config.getInt("Items.NewGameItem.Slot") - 1, newGameItem);

        ItemStack leaveGameItem = new ItemStack(Material.getMaterial(config.getString("Items.LeaveGameItem.Material")));
        ItemMeta leaveGameItemMeta = leaveGameItem.getItemMeta();
        leaveGameItemMeta.setDisplayName(Utils.color(config.getString("Items.LeaveGameItem.Name")));
        leaveGameItem.setItemMeta(leaveGameItemMeta);
        player.getInventory().setItem(config.getInt("Items.LeaveGameItem.Slot") - 1, leaveGameItem);
    }

    public void respawnPlayer(@NotNull Player player, boolean immediately) {
        respawningPlayers.add(player.getName());
        player.setFallDistance(0.0F);
        player.setHealth(player.getMaxHealth());
        if (!immediately) {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.teleport(player.getEyeLocation().add(0, 3, 0));
            ItemStack[] invContents = player.getInventory().getContents();
            player.getInventory().clear();
            arena.getPlayers().forEach(otherPlayer -> {if (otherPlayer != player) otherPlayer.hidePlayer(player);});
            player.sendTitle(Utils.color(config.getString("Messages.DeathMessage")), "");

            int respawnCountDown = config.getInt("Game.RespawnCountDown");
            GamePlayer playerData = playersData.stream().filter(x -> x.getUniqueId().equals(player.getUniqueId())).findAny().orElse(null);
            if (playerData != null && playerData.getBoughtBonusItems().containsKey(Utils.stripColor(config.getString("Menus.BonusShop.Items.LessRespawnTime.Name")))) {
                respawnCountDown = config.getInt("Game.RespawnCountDownLess");
            }

            int finalRespawnCountDown = respawnCountDown;
            new BukkitRunnable() {
                int countDown = finalRespawnCountDown;

                @Override
                public void run() {
                    if (arena.getState() != ArenaState.STARTED && arena.getState() != ArenaState.ENDING) cancel();
                    if (countDown <= 0) {
                        arena.getPlayers().forEach(otherPlayer -> {
                            if (otherPlayer != player) otherPlayer.showPlayer(player);
                        });
                        Location spawnLoc = arena.getSpawnLoc(arena.getTeamType(player));
                        spawnLoc.setYaw(arena.getTeamType(player) == TeamType.RED ? 179 : 0);
                        player.teleport(spawnLoc);
                        player.setFallDistance(0.0f);
                        if (playerData != null && !playerData.getBoughtBonusItems().containsKey(Utils.stripColor(config.getString("Menus.BonusShop.Items.DoubleJump.Name")))) {
                            player.setAllowFlight(false);
                        }
                        player.setFlying(false);
                        Utils.giveKit(player);
                        GamePlayer playerData = playersData.stream().filter(x -> x.getUniqueId().equals(player.getUniqueId())).findAny().orElse(null);
                        if (playerData != null && playerData.getBoughtBonusItems().containsKey(Utils.stripColor(config.getString("Menus.BonusShop.Items.Trampoline.Name")))) {
                            ItemStack slimeball = new ItemStack(Material.valueOf(config.getString("Items.TrampolineItem.Material")));
                            ItemMeta slimeballMeta = slimeball.getItemMeta();
                            slimeballMeta.setDisplayName(Utils.color(config.getString("Items.TrampolineItem.Name")));
                            slimeball.setItemMeta(slimeballMeta);
                        }
                        respawningPlayers.remove(player.getName());
                        cancel();
                    }
                    Utils.sendMessage(player, config.getString("Messages.RespawnMessageCountDown").replace("%seconds%", "" + countDown));
                    countDown--;
                }
            }.runTaskTimer(FireballFight.INSTANCE, 0L, 20L);
        } else {
            Location spawnLoc = arena.getSpawnLoc(arena.getTeamType(player));
            spawnLoc.setYaw(arena.getTeamType(player) == TeamType.RED ? 179 : 0);
            player.teleport(spawnLoc);
        }
    }

    public void handleDeath(Player player, EntityDamageEvent.DamageCause damageCause) {
        playersData.stream().filter(x -> x.getUniqueId().equals(player.getUniqueId())).findAny()
                .ifPresent(playerData -> playerData.setDeaths(playerData.getDeaths() + 1));
        String deathMsg = null;
        if (damageCause == EntityDamageEvent.DamageCause.ENTITY_ATTACK && player.getKiller() != null) {
            deathMsg = config.getString("Messages.PlayerKilledByPlayer")
                    .replace("%name%", Utils.getInGamePlayerName(player)).replace("%killer_name%", Utils.getInGamePlayerName(playerDamagerMap.get(player)));
            playersData.stream().filter(x -> x.getUniqueId().equals(playerDamagerMap.get(player).getUniqueId())).findAny().ifPresent(x -> x.setKills(x.getKills() + 1));
        }

        if (damageCause == EntityDamageEvent.DamageCause.VOID) {
            deathMsg = config.getString("Messages.PlayerFellIntoVoid").replace("%name%", Utils.getInGamePlayerName(player));

            if (playerDamagerMap.containsKey(player)) {
                deathMsg = config.getString("Messages.PlayerKickedIntoVoidByPlayer")
                        .replace("%name%", Utils.getInGamePlayerName(player)).replace("%killer_name%", Utils.getInGamePlayerName(playerDamagerMap.get(player)));
                playersData.stream().filter(x -> x.getUniqueId().equals(playerDamagerMap.get(player).getUniqueId())).findAny().ifPresent(x -> x.setKills(x.getKills() + 1));
            }
            playerDamagerMap.remove(player);
        }

        for (Player p : arena.getPlayers()) {
            Utils.sendMessage(p, deathMsg);
        }


        if (arena.getTeamType(player) == TeamType.RED) {
            if (arena.getGame().isRedTeamBedDestroyed && arena.getGame().getAlivePlayersInTeam(TeamType.RED).isEmpty()) arena.getGame().endGame(TeamType.BLUE);
            else if (arena.getGame().isRedTeamBedDestroyed) arena.getGame().putToSpectators(player);
            else arena.getGame().respawnPlayer(player, false);
        } else if (arena.getTeamType(player) == TeamType.BLUE) {
            if (arena.getGame().isBlueTeamBedDestroyed && arena.getGame().getAlivePlayersInTeam(TeamType.BLUE).isEmpty()) arena.getGame().endGame(TeamType.RED);
            else if (arena.getGame().isBlueTeamBedDestroyed) arena.getGame().putToSpectators(player);
            else arena.getGame().respawnPlayer(player, false);
        }
        if (getAlivePlayersInTeam(TeamType.RED).isEmpty()) {
            endGame(TeamType.BLUE);
        } else if (getAlivePlayersInTeam(TeamType.BLUE).isEmpty()) {
            endGame(TeamType.RED);
        }
    }

    public boolean hasStarted() {
            return isStarted;
    }

    public List<Block> getPlacedBlocks() {
        return placedBlocks;
    }

    public List<String> getRespawningPlayers() {
        return respawningPlayers;
    }

    public List<String> getSpectators() {
        return spectators;
    }

    public List<GamePlayer> getPlayersData() {
        return playersData;
    }

    public List<String> getAlivePlayersInTeam(TeamType teamType) {
        List<String> alivePlayers = new ArrayList<>();
        arena.getTeam(teamType).forEach(playerName -> {
            if (!spectators.contains(playerName)) alivePlayers.add(playerName);
        });
        return alivePlayers;
    }

    public HashMap<Player, Player> getPlayerDamagerMap() {
        return playerDamagerMap;
    }
}
