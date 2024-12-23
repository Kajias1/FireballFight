package me.kajias.fireballfight.objects;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.objects.enums.ArenaState;
import me.kajias.fireballfight.objects.enums.ArenaType;
import me.kajias.fireballfight.objects.enums.GameType;
import me.kajias.fireballfight.objects.enums.TeamType;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Bed;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Arena
{
    private static final FileConfiguration config = FireballFight.INSTANCE.getConfig();
    private static final HashMap<Player, Arena> playerArenaMap = new HashMap<>();

    private String name;
    private String worldName;
    private String templateName;
    private Location spawnLocWaiting;
    private Location spawnLocSpectator;
    private Location spawnLocRedTeam;
    private Location redTeamBedLoc;
    private Location spawnLocBlueTeam;
    private Location blueTeamBedLoc;
    private ArenaState arenaState;
    private ArenaType arenaType;

    private GameType gameType;
    private int allowedPlayersAmount;
    private World world;
    private Game game;

    private final List<String> redTeam;
    private final List<String> blueTeam;
    private final List<Player> players;
    private final List<Rune> runes;

    public Arena(String name, String worldName, String templateName) {
        this.name = name;
        this.worldName = worldName;
        this.templateName = templateName;
        spawnLocWaiting = null;
        spawnLocSpectator = null;
        spawnLocRedTeam = null;
        redTeamBedLoc = null;
        spawnLocBlueTeam = null;
        blueTeamBedLoc = null;
        arenaState = ArenaState.SETUP;
        arenaType = ArenaType.SOLO;
        gameType = GameType.NORMAL;
        allowedPlayersAmount = 2;
        redTeam = new ArrayList<>();
        blueTeam = new ArrayList<>();
        players = new ArrayList<>();
        runes = new ArrayList<>();

        ArenaManager.copyWorldFolderToDestination(
                new File(FireballFight.INSTANCE.getServer().getWorldContainer().getAbsolutePath() + "/" + this.templateName),
                new File(FireballFight.INSTANCE.getServer().getWorldContainer().getAbsolutePath() + "/" + this.worldName)
        );
        createWorld(this.worldName);
    }

    public void createWorld(String worldName) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        world = Bukkit.getServer().createWorld(worldCreator);
        world.setAutoSave(false);
        world.setDifficulty(Difficulty.EASY);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doWeatherCycle", "false");
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doFireTick", "false");
        world.setGameRuleValue("doTileDrops", "true");
        world.setGameRuleValue("doMobLoot", "false");
        world.setGameRuleValue("keepInventory", "true");
        world.setGameRuleValue("announceAdvancements", "false");
        world.setGameRuleValue("commandBlockOutput", "false");
    }

    public void restart() {
        stop();
        resetWorld();

        Bukkit.getScheduler().runTaskLater(FireballFight.INSTANCE, () -> {
            setState(ArenaState.WAITING);
        }, 3 * 20L);
    }

    public void resetWorld() {
        runes.forEach(Rune::killArmorStand);
        ArenaManager.deleteWorldFolder(this.worldName, true);
        ArenaManager.copyWorldFolderToDestination(
                new File(FireballFight.INSTANCE.getServer().getWorldContainer().getAbsolutePath() + "/" + this.templateName),
                new File(FireballFight.INSTANCE.getServer().getWorldContainer().getAbsolutePath() + "/" + this.worldName)
        );
        createWorld(this.worldName);
        if (!runes.isEmpty()) {
            runes.forEach(rune -> {
                Location newLocation = rune.getLocation();
                newLocation.setWorld(world);
                rune.setLocation(newLocation);
                rune.spawnArmorStand();
            });
        }
    }

    public void addPlayer(Player player) {
        if (arenaState == ArenaState.WAITING || arenaState == ArenaState.STARTING) {
            if (players.size() < allowedPlayersAmount) {
                if (playerArenaMap.containsKey(player)) playerArenaMap.get(player).removePlayer(player);
                players.add(player);
                playerArenaMap.put(player, this);
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.setExp(0.0f);
                player.setLevel(0);
                player.setMaxHealth(20.0f);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                Utils.removePotionEffects(player);
                player.teleport(spawnLocWaiting, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(false);
                player.setFlying(false);

                ItemStack leaveGameItem = new ItemStack(Material.getMaterial(config.getString("Items.LeaveGameItem.Material")));
                ItemMeta leaveGameItemMeta = leaveGameItem.getItemMeta();
                leaveGameItemMeta.setDisplayName(Utils.color(
                        config.getString("Items.LeaveGameItem.Name")));
                leaveGameItem.setItemMeta(leaveGameItemMeta);
                player.getInventory().setItem(config.getInt("Items.LeaveGameItem.Slot") - 1, leaveGameItem);

                for(Player p : players) Utils.sendMessage(p, config.getString("Messages.PlayerJoined")
                        .replace("%player%", player.getName())
                        .replace("%current%", String.valueOf(players.size()))
                        .replace("%max%", String.valueOf(allowedPlayersAmount)));

                if (players.size() == allowedPlayersAmount) {
                    game = new Game(this);
                    game.startCountDownTimer();
                }
            } else Utils.sendMessage(player, config.getString("Messages.ArenaIsFull"));
        }
    }

    public void removePlayer(Player player) {
        players.remove(player);
        playerArenaMap.remove(player);
        Utils.teleportToLobby(player);
        removeFromTeam(player);

        for (Player p : players) {

            if (arenaState != ArenaState.STARTED)
                Utils.sendMessage(p, config.getString("Messages.PlayerLeft")
                        .replace("%player%", player.getName())
                        .replace("%current%", String.valueOf(players.size()))
                        .replace("%max%", String.valueOf(allowedPlayersAmount)));
            else Utils.sendMessage(p, config.getString("Messages.PlayerLeftInGame")
                    .replace("%player%", player.getName()));
        }

        if (arenaState == ArenaState.STARTED && players.size() <= 1) {
            try {
                Utils.sendMessage(players.get(0), config.getString("Messages.NoPlayersLeft"));
            } catch (IndexOutOfBoundsException ignored) {}
            restart();
        }

        if (game != null && arenaState == ArenaState.STARTING && players.size() < allowedPlayersAmount) {
            game.stopCountDownTimer();
            game = null;
        }
    }

    public void stop() {
        if (!world.getPlayers().isEmpty()) {
            for (Player p : world.getPlayers()) {
                removePlayer(p);
            }
        }

        if (game != null) {
            game.destroy();
            game = null;
        }
    }

    public boolean isInTeam(Player player) {
        try {
            return redTeam.contains(player.getName()) || blueTeam.contains(player.getName());
        } catch (NullPointerException e) {
            return false;
        }
    }

    public void addToTeam(TeamType type, Player player) {
        if (isInTeam(player)) removeFromTeam(player);
        if (type == TeamType.RED) {
            redTeam.add(player.getName());
            player.setPlayerListName(Utils.color( "&c" + player.getName()));
        } else {
            blueTeam.add(player.getName());
            player.setPlayerListName(Utils.color( "&9" + player.getName()));
        }
    }

    public void removeFromTeam(Player player) {
        if (!isInTeam(player)) return;
        redTeam.remove(player.getName());
        blueTeam.remove(player.getName());
        player.setPlayerListName(player.getName());
    }

    public void addRune(Rune rune) {
        runes.add(rune);
    }

    public void removeRunes() {
        if (!runes.isEmpty()) {
            runes.clear();
        }
    }

    public List<Rune> getRunes() {
        return runes;
    }

    public TeamType getTeamType(Player player) {
        if (!isInTeam(player)) return null;
        return (redTeam.contains(player.getName()) ? TeamType.RED : TeamType.BLUE);
    }

    public List<String> getTeam(TeamType teamType) {
        return teamType == TeamType.RED ? redTeam : blueTeam;
    }

    public static HashMap<Player, Arena> getPlayerArenaMap() {
        return playerArenaMap;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public boolean haveSetupProperly() {
        return spawnLocWaiting != null && spawnLocRedTeam != null && spawnLocBlueTeam != null;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public boolean setGameTypeFromString(String gameType) {
        try {
            this.gameType = GameType.valueOf(gameType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    public ArenaState getState() {
        return arenaState;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public World getWorld() {
        return world;
    }

    public Location getSpawnLoc(TeamType teamType) {
        return teamType == TeamType.RED ? spawnLocRedTeam : spawnLocBlueTeam;
    }

    public ArenaType getArenaType() {
        return arenaType;
    }

    public void setSpawnLoc(TeamType teamType, Location location) {
        if (teamType == TeamType.RED) spawnLocRedTeam = location;
        else if (teamType == TeamType.BLUE) spawnLocBlueTeam = location;
    }

    public void setSpawnLocWaiting(Location location) {
        spawnLocWaiting = location;
    }

    public void setSpawnLocSpectator(Location location) {
        spawnLocSpectator = location;
    }

    public boolean setArenaTypeFromString(String arenaType) {
        switch (arenaType) {
            case "1v1":
            case "SOLO":
                this.arenaType = ArenaType.SOLO;
                this.allowedPlayersAmount = 2;
                break;
            case "2v2":
            case "DUO":
                this.arenaType = ArenaType.DUO;
                this.allowedPlayersAmount = 4;
                break;
            default:
                return false;
        }
        return true;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public void setState(ArenaState arenaState) {
        this.arenaState = arenaState;
    }

    public Location getSpawnLocWaiting() {
        return spawnLocWaiting;
    }

    public Location getSpawnLocSpectator() {
        return spawnLocSpectator;
    }

    public Game getGame() {
        return game;
    }

    public String getTemplateName() {
        return templateName;
    }

    public int getAllowedPlayersAmount() {
        return allowedPlayersAmount;
    }
}
