package me.kajias.fireballfight.commands;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.configurations.ArenaConfiguration;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.ArenaManager;
import me.kajias.fireballfight.objects.Rune;
import me.kajias.fireballfight.objects.enums.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AdminCommand implements CommandExecutor
{
    public static final Map<Player, Arena> setupMap = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = FireballFight.INSTANCE.getConfig();
        if (sender instanceof Player) {
            Player player = ((Player) sender).getPlayer();

            if (player.hasPermission("ff.admin")) {
                if (args.length == 0) {
                    showHelp(player);
                } else {
                    switch (args[0].toLowerCase()) {
                        case "adminhelp":
                            showHelp(player);
                            break;
                        case "setlobby":
                            config.set("LobbySpawnPoint.World", player.getWorld().getName());
                            config.set("LobbySpawnPoint.X", player.getLocation().getX());
                            config.set("LobbySpawnPoint.Y", player.getLocation().getY());
                            config.set("LobbySpawnPoint.Z", player.getLocation().getZ());
                            config.set("LobbySpawnPoint.Yaw", player.getLocation().getYaw());
                            FireballFight.INSTANCE.saveConfig();
                            FireballFight.INSTANCE.lobbyLocation = player.getLocation();
                            Utils.sendMessage(player, config.getString("Messages.LobbyWasSet"));
                            break;
                        case "create":
                            Arena createdArena;
                            if (args.length == 4) {
                                if (ArenaManager.getArenaByName(args[1]) == null) {
                                    if (new File(args[3]).listFiles() != null) {
                                        createdArena = new Arena(args[1], args[2], args[3]);
                                        ArenaConfiguration.addArena(createdArena);
                                        Utils.sendMessage(player, config.getString("Messages.ArenaCreated").replace("%arena_name%", args[1]));
                                        player.teleport(new Location(createdArena.getWorld(), 0, 60, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
                                        player.setGameMode(GameMode.CREATIVE);
                                        setupMap.put(player, createdArena);
                                        showSetupStatus(createdArena, player);
                                    } else
                                        Utils.sendMessage(player, config.getString("Messages.TemplateWorldNotFound").replace("%world_name%", args[2]));
                                } else
                                    Utils.sendMessage(player, config.getString("Messages.ArenaExists").replace("%arena_name%", args[1]));
                            } else showHelp(player);
                            break;
                        case "setspawn":
                            if (args.length == 1) {
                                Utils.sendMessage(player, config.getString("Messages.TeamNotDefined"));
                            } else {
                                if (setupMap.containsKey(player)) {
                                    if (args[1].equalsIgnoreCase("red")) {
                                        Arena arena = setupMap.get(player);
                                        if (player.getWorld().equals(arena.getWorld())) {
                                            arena.setSpawnLoc(TeamType.RED, player.getLocation());
                                            Utils.sendMessage(player, config.getString("Messages.SpawnSet"));
                                            showSetupStatus(arena, player);
                                        } else
                                            Utils.sendMessage(player, config.getString("Messages.NotInArenaWorld"));
                                    } else if (args[1].equalsIgnoreCase("blue")) {
                                        Arena arena = setupMap.get(player);
                                        if (player.getWorld().equals(arena.getWorld())) {
                                            arena.setSpawnLoc(TeamType.BLUE, player.getLocation());
                                            Utils.sendMessage(player, config.getString("Messages.SpawnSet"));
                                            showSetupStatus(arena, player);
                                        } else
                                            Utils.sendMessage(player, config.getString("Messages.NotInArenaWorld"));
                                    } else if (args[1].equalsIgnoreCase("waiting")) {
                                        Arena arena = setupMap.get(player);
                                        if (player.getWorld().equals(arena.getWorld())) {
                                            arena.setSpawnLocWaiting(player.getLocation());
                                            Utils.sendMessage(player, config.getString("Messages.SpawnSet"));
                                            showSetupStatus(arena, player);
                                        } else
                                            Utils.sendMessage(player, config.getString("Messages.NotInArenaWorld"));
                                    } else if (args[1].equalsIgnoreCase("spectator")) {
                                        Arena arena = setupMap.get(player);
                                        if (player.getWorld().equals(arena.getWorld())) {
                                            arena.setSpawnLocSpectator(player.getLocation());
                                            Utils.sendMessage(player, config.getString("Messages.SpawnSet"));
                                            showSetupStatus(arena, player);
                                        } else
                                            Utils.sendMessage(player, config.getString("Messages.NotInArenaWorld"));
                                    }
                                } else Utils.sendMessage(player, config.getString("Messages.NotInSetup"));
                            }
                            break;
                        case "setarenatype":
                            if (args.length == 2) {
                                if (setupMap.containsKey(player)) {
                                    Arena arena = setupMap.get(player);
                                    if (arena.setArenaTypeFromString(args[1])) {
                                        showSetupStatus(arena, player);
                                    } else Utils.sendMessage(player, config.getString("Messages.ArenaTypeUnknown"));
                                } else Utils.sendMessage(player, config.getString("Messages.NotInSetup"));
                            } else showHelp(player);
                            break;
                        case "setgametype":
                            if (args.length == 2) {
                                if (setupMap.containsKey(player)) {
                                    Arena arena = setupMap.get(player);
                                    if (arena.setGameTypeFromString(args[1])) {
                                        showSetupStatus(arena, player);
                                    } else Utils.sendMessage(player, config.getString("Messages.GameTypeUnknown"));
                                } else Utils.sendMessage(player, config.getString("Messages.NotInSetup"));
                            } else showHelp(player);
                            break;
                        case "addrune":
                            if (args.length == 2) {
                                if (setupMap.containsKey(player)) {
                                    try {
                                        Rune rune = new Rune(player.getLocation().getBlock().getLocation().add(0.5, -0.5, 0.5), RuneType.valueOf(args[1]));
                                        rune.spawnArmorStand();
                                        setupMap.get(player).addRune(rune);
                                    } catch (IllegalArgumentException e) {
                                        Utils.sendMessage(player, config.getString("Messages.InvalidRuneName"));
                                    }
                                } else Utils.sendMessage(player, config.getString("Messages.NotInSetup"));
                            } else Utils.sendMessage(player, config.getString("Messages.RuneNameIsNotSpecified"));
                            break;
                        case "clearrunes":
                            if (setupMap.containsKey(player)) {
                                setupMap.get(player).removeRunes();
                            } else Utils.sendMessage(player, config.getString("Messages.NotInSetup"));
                            break;
                        case "disable":
                            if (args.length == 2) {
                                if (ArenaManager.getArenaByName(args[1]) != null) {
                                    Arena arena = ArenaManager.getArenaByName(args[1]);
                                    if (arena.getState() != ArenaState.DISABLED) {
                                        ArenaManager.disableArena(arena);
                                        Utils.sendMessage(player, config.getString("Messages.ArenaHasBeenDisabled").replace("%name%", args[1]));
                                    } else Utils.sendMessage(player, config.getString("Messages.FailedToDisableArena").replace("%name%", args[1]));
                                } else Utils.sendMessage(player, config.getString("Messages.ArenaDoesNotExist").replace("%name%%", args[1]));
                            } else Utils.sendMessage(player, config.getString("Messages.ArenaNameIsNotSpecified"));
                            break;
                        case "enable":
                            if (args.length == 2) {
                                if (ArenaManager.getArenaByName(args[1]) != null) {
                                    Arena arena = ArenaManager.getArenaByName(args[1]);
                                    if (arena.getState() == ArenaState.DISABLED) {
                                        ArenaManager.enableArena(arena);
                                        Utils.sendMessage(player, config.getString("Messages.ArenaHasBeenEnabled").replace("%name%", args[1]));
                                    } else Utils.sendMessage(player, config.getString("Messages.ArenaAlreadyEnabled").replace("%name%", args[1]));
                                } else Utils.sendMessage(player, config.getString("Messages.ArenaDoesNotExist").replace("%name%", args[1]));
                            } else Utils.sendMessage(player, config.getString("Messages.ArenaNameIsNotSpecified"));
                            break;
                        case "setup":
                            if (args.length == 2) {
                                if (ArenaManager.getArenaByName(args[1]) != null) {
                                    Arena modifiableArena = ArenaManager.getArenaByName(args[1]);
                                    ArenaManager.disableArena(modifiableArena);
                                    if (!setupMap.containsKey(player)) {
                                        Bukkit.getScheduler().runTaskLater(FireballFight.INSTANCE, () -> {
                                            modifiableArena.setState(ArenaState.SETUP);
                                            player.teleport(new Location(modifiableArena.getWorld(), 0, 60, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
                                            player.getInventory().clear();
                                            player.getInventory().setArmorContents(null);
                                            player.setGameMode(GameMode.CREATIVE);
                                            setupMap.put(player, modifiableArena);
                                            showSetupStatus(modifiableArena, player);
                                        }, 2 * 20L);
                                    } else Utils.sendMessage(player, config.getString("Strings.ArenaIsAlreadyInSetupMode"));
                                } else Utils.sendMessage(player, config.getString("Messages.ArenaDoesNotExist").replace("%name%", args[1]));
                            } else Utils.sendMessage(player, config.getString("Messages.ArenaNameIsNotSpecified"));
                            break;
                        case "finish":
                            if (setupMap.containsKey(player)) {
                                if (setupMap.get(player).haveSetupProperly()) {
                                    Utils.sendMessage(player, config.getString("Messages.ExitingSetup"));
                                    setupMap.get(player).getWorld().save();
                                    setupMap.get(player).setState(ArenaState.WAITING);
                                    ArenaConfiguration.saveArena(setupMap.get(player));
                                    ArenaManager.loadArena(setupMap.get(player));
                                    setupMap.remove(player);
                                    Utils.teleportToLobby(player);
                                } else Utils.sendMessage(player, config.getString("Messages.FailedToFinishSetup"));
                            } else Utils.sendMessage(player, config.getString("Messages.NotInSetup"));
                            break;
                        case "list":
                            listArenas(player);
                            break;
                        default:
                            Utils.sendMessage(player, config.getString("Messages.UnknownSubcommand"));
                            break;
                    }
                }
                return true;
            } else Utils.sendMessage(player, config.getString("Messages.NoPermission"));
            return false;
        }
        Bukkit.getLogger().warning(config.getString("LogMessages.MustBePlayerToExecuteCommand"));
        return false;
    }

    private void showHelp(Player player) {
        FileConfiguration config = FireballFight.INSTANCE.getConfig();
        for (String s : config.getStringList("Messages.AdminHelp")) {
            player.sendMessage(Utils.color(s));
        }
    }

    private void showSetupStatus(Arena arena, Player player) {
        FileConfiguration config = FireballFight.INSTANCE.getConfig();
        TextComponent setupStatusTextComponent = new TextComponent();
        TextComponent spawnPointWaitingTextComponent = new TextComponent();
        TextComponent spawnPointSpectatorTextComponent = new TextComponent();
        TextComponent spawnPointRedTextComponent = new TextComponent();
        TextComponent spawnPointBlueTextComponent = new TextComponent();
        TextComponent arenaTypeTextComponent = new TextComponent();
        TextComponent arenaTypeSOLO = new TextComponent();
        TextComponent arenaTypeDUO = new TextComponent();
        TextComponent gameTypeTextComponent = new TextComponent();
        TextComponent gameTypeNormal = new TextComponent();
        TextComponent gameTypeX = new TextComponent();
        TextComponent addHealthRuneTextComponent = new TextComponent();
        TextComponent addSpeedRuneTextComponent = new TextComponent();
        TextComponent removeRunesTextComponent = new TextComponent();
        TextComponent finishSetupTextComponent = new TextComponent();

        TextComponent lineTextComponent = new TextComponent(Utils.color("&6&m-------------------------------------------"));

        setupStatusTextComponent.setText(Utils.color(config.getString("Messages.SetupStatus")));

        StringBuilder builder = new StringBuilder();
        if(arena.getSpawnLocWaiting() != null) {
            builder.append("&7[&a✔&7] ").append(config.getString("Messages.SpawnPointWaiting")).append(" ")
                    .append(config.getString("Messages.XYZ")
                            .replace("%x%", String.valueOf(arena.getSpawnLocWaiting().getBlockX()))
                            .replace("%y%", String.valueOf(arena.getSpawnLocWaiting().getBlockY()))
                            .replace("%z%", String.valueOf(arena.getSpawnLocWaiting().getBlockZ())));
        } else {
            builder.append("&7[&c✕&7] ").append(config.getString("Messages.SpawnPointWaiting"));
        }
        spawnPointWaitingTextComponent.setText(Utils.color(builder.toString()));
        spawnPointWaitingTextComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Utils.color(config.getString("Messages.ModifySpawnPointHoverMessage"))).create()));
        spawnPointWaitingTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff setspawn waiting"));

        builder = new StringBuilder();
        if(arena.getSpawnLocSpectator() != null) {
            builder.append("&7[&a✔&7] ").append(config.getString("Messages.SpawnPointSpectator")).append(" ")
                    .append(config.getString("Messages.XYZ")
                            .replace("%x%", String.valueOf(arena.getSpawnLocSpectator().getBlockX()))
                            .replace("%y%", String.valueOf(arena.getSpawnLocSpectator().getBlockY()))
                            .replace("%z%", String.valueOf(arena.getSpawnLocSpectator().getBlockZ())));
        } else {
            builder.append("&7[&c✕&7] ").append(config.getString("Messages.SpawnPointSpectator"));
        }
        spawnPointSpectatorTextComponent.setText(Utils.color(builder.toString()));
        spawnPointSpectatorTextComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Utils.color(config.getString("Messages.ModifySpawnPointHoverMessage"))).create()));
        spawnPointSpectatorTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff setspawn spectator"));

        builder = new StringBuilder();
        if(arena.getSpawnLoc(TeamType.RED) != null) {
            builder.append("&7[&a✔&7] ").append(config.getString("Messages.SpawnPointRed")).append(" ")
                    .append(config.getString("Messages.XYZ")
                            .replace("%x%", String.valueOf(arena.getSpawnLoc(TeamType.RED).getBlockX()))
                            .replace("%y%", String.valueOf(arena.getSpawnLoc(TeamType.RED).getBlockY()))
                            .replace("%z%", String.valueOf(arena.getSpawnLoc(TeamType.RED).getBlockZ())));
        } else {
            builder.append("&7[&c✕&7] ").append(config.getString("Messages.SpawnPointRed"));
        }
        spawnPointRedTextComponent.setText(Utils.color(builder.toString()));
        spawnPointRedTextComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Utils.color(
                        config.getString("Messages.ModifySpawnPointHoverMessage"))).create()));
        spawnPointRedTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff setspawn red"));

        builder = new StringBuilder();
        if(arena.getSpawnLoc(TeamType.BLUE) != null) {
            builder.append("&7[&a✔&7] ").append(config.getString("Messages.SpawnPointBlue")).append(" ")
                    .append(config.getString("Messages.XYZ")
                            .replace("%x%", String.valueOf(arena.getSpawnLoc(TeamType.BLUE).getBlockX()))
                            .replace("%y%", String.valueOf(arena.getSpawnLoc(TeamType.BLUE).getBlockY()))
                            .replace("%z%", String.valueOf(arena.getSpawnLoc(TeamType.BLUE).getBlockZ())));
        } else {
            builder.append("&7[&c✕&7] ").append(config.getString("Messages.SpawnPointBlue"));
        }
        spawnPointBlueTextComponent.setText(Utils.color(builder.toString()));
        spawnPointBlueTextComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Utils.color(config.getString("Messages.ModifySpawnPointHoverMessage"))).create()));
        spawnPointBlueTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff setspawn blue"));

        arenaTypeTextComponent.setText(Utils.color(config.getString("Messages.ArenaTypeChoice")));

        arenaTypeSOLO.setText(Utils.color(arena.getArenaType() == ArenaType.SOLO ? "&a     1  VS  1":"&7     1  VS  1"));
        arenaTypeSOLO.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Utils.color(config.getString("Messages.ModifyHoverMessage"))).create()));
        arenaTypeSOLO.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff setarenatype 1v1"));

        arenaTypeDUO.setText(Utils.color(arena.getArenaType() == ArenaType.DUO ? "&a     2  VS  2":"&7     2  VS  2"));
        arenaTypeDUO.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Utils.color(config.getString("Messages.ModifyHoverMessage"))).create()));
        arenaTypeDUO.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff setarenatype 2v2"));

        gameTypeTextComponent.setText(Utils.color(config.getString("Messages.GameTypeChoice")));

        gameTypeNormal.setText(Utils.color(arena.getGameType() == GameType.NORMAL ? "&a     Normal":"&7     Normal"));
        gameTypeNormal.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Utils.color(config.getString("Messages.ModifyHoverMessage"))).create()));
        gameTypeNormal.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff setgametype normal"));

        gameTypeX.setText(Utils.color(arena.getGameType() == GameType.X ? "&a     Fireball Fight X":"&7     Fireball Fight X"));
        gameTypeX.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Utils.color(config.getString("Messages.ModifyHoverMessage"))).create()));
        gameTypeX.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff setgametype x"));

        builder = new StringBuilder();
        if(arena.getSpawnLocSpectator() != null) {
            builder.append("&7[&aЛКМ&7] ").append(config.getString("Messages.AddHealthRuneTextComponent"));
        }
        addHealthRuneTextComponent.setText(Utils.color(builder.toString()));
        addHealthRuneTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff addrune HEALTH"));

        builder = new StringBuilder();
        if(arena.getSpawnLocSpectator() != null) {
            builder.append("&7[&aЛКМ&7] ").append(config.getString("Messages.AddSpeedRuneTextComponent"));
        }
        addSpeedRuneTextComponent.setText(Utils.color(builder.toString()));
        addSpeedRuneTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff addrune SPEED"));

        builder = new StringBuilder();
        if(arena.getSpawnLocSpectator() != null) {
            builder.append("&7[&aЛКМ&7] ").append(config.getString("Messages.RemoveRunesTextComponent"));
        }
        removeRunesTextComponent.setText(Utils.color(builder.toString()));
        removeRunesTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff clearrunes"));

        finishSetupTextComponent.setText(Utils.color(config.getString("Messages.FinishSetupButtonText")));
        finishSetupTextComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Utils.color(config.getString("Messages.FinishSetupButtonHoverMessage"))).create()));
        finishSetupTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ff finish"));

        player.spigot().sendMessage(lineTextComponent);
        player.spigot().sendMessage(setupStatusTextComponent);
        player.spigot().sendMessage(spawnPointWaitingTextComponent);
        player.spigot().sendMessage(spawnPointSpectatorTextComponent);
        player.spigot().sendMessage(spawnPointRedTextComponent);
        player.spigot().sendMessage(spawnPointBlueTextComponent);
        player.spigot().sendMessage(arenaTypeTextComponent);
        player.spigot().sendMessage(arenaTypeSOLO);
        player.spigot().sendMessage(arenaTypeDUO);
        player.spigot().sendMessage(gameTypeTextComponent);
        player.spigot().sendMessage(gameTypeNormal);
        player.spigot().sendMessage(gameTypeX);
        player.spigot().sendMessage(addHealthRuneTextComponent);
        player.spigot().sendMessage(addSpeedRuneTextComponent);
        player.spigot().sendMessage(removeRunesTextComponent);
        player.spigot().sendMessage(finishSetupTextComponent);
        player.spigot().sendMessage(lineTextComponent);
    }

    private void listArenas(Player player) {
        FileConfiguration config = FireballFight.INSTANCE.getConfig();
        if (ArenaManager.getLoadedArenas().isEmpty()) {
            player.sendMessage(Utils.color(
                    config.getString("Messages.Prefix") + config.getString("Messages.ArenaListEmpty")));
            return;
        }
        Utils.sendMessage(player, config.getString("Messages.ArenaList"));
        for(Arena arena : ArenaManager.getLoadedArenas()) {
            String state;
            switch (arena.getState()) {
                case SETUP:
                    state = "&e●";
                    break;
                case DISABLED:
                    state = "&c●";
                    break;
                case WAITING:
                case STARTING:
                    state = "&a●";
                    break;
                case ENDING:
                case STARTED:
                    state = "&8●";
                    break;
                default:
                    state = "";
                    break;
            }
            player.sendMessage(Utils.color(
                    config.getString("Messages.ArenaListFormat")
                            .replace("%name%", arena.getName())
                            .replace("%world%", arena.getWorldName())
                            .replace("%state%", state)));
        }
    }
}
