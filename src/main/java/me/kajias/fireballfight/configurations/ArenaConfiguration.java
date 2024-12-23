package me.kajias.fireballfight.configurations;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.Rune;
import me.kajias.fireballfight.objects.enums.GameType;
import me.kajias.fireballfight.objects.enums.RuneType;
import me.kajias.fireballfight.objects.enums.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArenaConfiguration
{
    private static final FileConfiguration config = FireballFight.INSTANCE.getConfig();
    private static final BaseConfiguration arenaConfig = new BaseConfiguration("arenas");

    public static void saveArena(Arena arena) {
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".WorldName", arena.getWorldName());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".TemplateName", arena.getTemplateName());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".ArenaType", arena.getArenaType().toString());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".GameType", arena.getGameType().toString());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.World", arena.getWorld().getName());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.RedX", arena.getSpawnLoc(TeamType.RED).getX());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.RedY", arena.getSpawnLoc(TeamType.RED).getY());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.RedZ", arena.getSpawnLoc(TeamType.RED).getZ());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.BlueX", arena.getSpawnLoc(TeamType.BLUE).getX());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.BlueY", arena.getSpawnLoc(TeamType.BLUE).getY());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.BlueZ", arena.getSpawnLoc(TeamType.BLUE).getZ());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.WaitingX", arena.getSpawnLocWaiting().getX());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.WaitingY", arena.getSpawnLocWaiting().getY());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.WaitingZ", arena.getSpawnLocWaiting().getZ());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.SpectatorX", arena.getSpawnLocSpectator().getX());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.SpectatorY", arena.getSpawnLocSpectator().getY());
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.SpectatorZ", arena.getSpawnLocSpectator().getZ());
        if (!arena.getRunes().isEmpty()) {
            for (int i = 0; i < arena.getRunes().size(); ++i) {
                Rune rune = arena.getRunes().get(i);
                arenaConfig.getConfig().set("Arenas." + arena.getName() + ".Runes." + "Rune" + i + ".X", rune.getLocation().getX());
                arenaConfig.getConfig().set("Arenas." + arena.getName() + ".Runes." + "Rune" + i + ".Y", rune.getLocation().getY());
                arenaConfig.getConfig().set("Arenas." + arena.getName() + ".Runes." + "Rune" + i + ".Z", rune.getLocation().getZ());
                arenaConfig.getConfig().set("Arenas." + arena.getName() + ".Runes." + "Rune" + i + ".Type", rune.getType().toString());
            }
        } else arenaConfig.getConfig().set("Arenas." + arena.getName() + ".Runes", null);
        arenaConfig.save();
    }

    public static void addArena(Arena arena) {
        arenaConfig.getConfig().set("Arenas." + arena.getName() + ".SpawnPoint.World", arena.getWorldName());
        arenaConfig.save();
    }

    public static void removeArena(String name) {
        arenaConfig.getConfig().set("Arenas." + name, null);
        arenaConfig.save();
    }
    
    public static List<Arena> getArenas() {
        List<Arena> result = new ArrayList<>();
        Arena arena;
        if(!arenaConfig.getConfig().getConfigurationSection("Arenas").getKeys(false).isEmpty()) {
            for(String arenaName : arenaConfig.getConfig().getConfigurationSection("Arenas").getKeys(false)) {
                arena = new Arena(arenaName, arenaConfig.getConfig().getString("Arenas." + arenaName + ".WorldName"),
                        arenaConfig.getConfig().getString("Arenas." + arenaName + ".TemplateName"));
                arena.setWorldName(arenaConfig.getConfig().getString("Arenas." + arena.getName() + ".SpawnPoint.World"));
                arena.setSpawnLoc(TeamType.RED, new Location(
                        Bukkit.getWorld(arenaConfig.getConfig().getString("Arenas." + arenaName + ".SpawnPoint.World")),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.RedX"),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.RedY"),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.RedZ")
                ));
                arena.setSpawnLoc(TeamType.BLUE, new Location(
                        Bukkit.getWorld(arenaConfig.getConfig().getString("Arenas." + arenaName + ".SpawnPoint.World")),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.BlueX"),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.BlueY"),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.BlueZ")
                ));
                arena.setSpawnLocWaiting(new Location(
                        Bukkit.getWorld(arenaConfig.getConfig().getString("Arenas." + arenaName + ".SpawnPoint.World")),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.WaitingX"),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.WaitingY"),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.WaitingZ")
                ));
                arena.setSpawnLocSpectator(new Location(
                        Bukkit.getWorld(arenaConfig.getConfig().getString("Arenas." + arenaName + ".SpawnPoint.World")),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.SpectatorX"),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.SpectatorY"),
                        arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".SpawnPoint.SpectatorZ")
                ));
                if (arenaConfig.getConfig().getConfigurationSection("Arenas." + arenaName + ".Runes") != null) {
                    for (String runeID : arenaConfig.getConfig().getConfigurationSection("Arenas." + arenaName + ".Runes").getKeys(false)) {
                        Location runeLoc = new Location(
                                Bukkit.getWorld(arenaConfig.getConfig().getString("Arenas." + arenaName + ".SpawnPoint.World")),
                                arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".Runes." + runeID + ".X"),
                                arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".Runes." + runeID + ".Y"),
                                arenaConfig.getConfig().getDouble("Arenas." + arenaName + ".Runes." + runeID + ".Z")
                        );
                        Rune rune = new Rune(runeLoc, RuneType.valueOf(arenaConfig.getConfig().getString("Arenas." + arenaName + ".Runes." + runeID + ".Type")));
                        arena.addRune(rune);
                    }
                }
                if (!arena.getRunes().isEmpty()) arena.getRunes().forEach(Rune::spawnArmorStand);
                arena.setArenaTypeFromString(arenaConfig.getConfig().getString("Arenas." + arena.getName() + ".ArenaType"));
                arena.setGameType(GameType.valueOf(arenaConfig.getConfig().getString("Arenas." + arena.getName() + ".GameType")));
                if (arena.haveSetupProperly()) result.add(arena);
            }
        }

        return result;
    }
}
