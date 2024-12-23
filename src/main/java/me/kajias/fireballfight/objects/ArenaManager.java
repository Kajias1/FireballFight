package me.kajias.fireballfight.objects;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.configurations.ArenaConfiguration;
import me.kajias.fireballfight.objects.enums.ArenaState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class ArenaManager
{
    private static final List<Arena> loadedArenas = new ArrayList<>();

    public static boolean loadArena(Arena arena) {
        FireballFight.INSTANCE.getLogger().log(Level.INFO, "Загружаю арену \"" + arena.getName() + "\"...");
        if (!loadedArenas.contains(arena)) {
            if (arena.haveSetupProperly()) {
                loadedArenas.add(arena);
                arena.setState(ArenaState.WAITING);
                return true;
            } else {
                FireballFight.INSTANCE.getLogger().log(Level.WARNING, "Арена " + arena.getName() + " не была верно " +
                        "настроена. Пожалуйста, убедитесь, что точка появления игроков для этой арены была установлена.");
            }
        }
        return false;
    }

    public static void enableArena(Arena arena) {
        arena.setState(ArenaState.WAITING);
    }

    public static void disableArena(Arena arena) {
        arena.stop();
        arena.setState(ArenaState.DISABLED);
    }

    public static boolean removeArena(Arena arena) {
        Bukkit.unloadWorld(arena.getWorld(), false);
        for (Player player : arena.getPlayers()) {
            Arena.getPlayerArenaMap().remove(player);
        }
        loadedArenas.remove(arena);
        ArenaConfiguration.removeArena(arena.getName());
        try {
            Files.deleteIfExists(Paths.get(FireballFight.INSTANCE.getServer().getWorldContainer().getAbsolutePath() + arena.getWorldName()));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static Arena getArenaByName(String name) {
        Optional<Arena> result = loadedArenas.stream().filter(x -> x.getName().equalsIgnoreCase(name)).findFirst();
        return result.orElse(null);
    }

    public static List<Arena> getLoadedArenas() {
        return loadedArenas;
    }

    public static Arena findBestArena() {
        return loadedArenas.stream().filter(x -> (x.getState() == ArenaState.WAITING || x.getState() == ArenaState.STARTING) &&
                !x.getPlayers().isEmpty()).findAny().orElse(loadedArenas.stream()
                .filter(x -> (x.getState() == ArenaState.WAITING || x.getState() == ArenaState.STARTING)).findAny().orElse(null));
    }

    public static void copyWorldFolderToDestination(File source, File target){
        try {
            ArrayList<String> ignore = new ArrayList<>(Arrays.asList("uid.dat", "session.lock"));
            if(!ignore.contains(source.getName())) {
                if(source.isDirectory()) {
                    if(!target.exists())
                        if (!target.mkdirs())
                            throw new IOException("Couldn't create world directory!");
                    String files[] = source.list();
                    for (String file : files) {
                        File srcFile = new File(source, file);
                        File destFile = new File(target, file);
                        copyWorldFolderToDestination(srcFile, destFile);
                    }
                } else {
                    InputStream in = new FileInputStream(source);
                    OutputStream out = new FileOutputStream(target);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0)
                        out.write(buffer, 0, length);
                    in.close();
                    out.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteWorldFolder(String worldName, boolean loaded) {
        if (loaded) {
            if (Bukkit.getWorld(worldName) != null) {
                Bukkit.getServer().unloadWorld(Bukkit.getWorld(worldName), true);
            }
        }
        File path = new File(FireballFight.INSTANCE.getServer().getWorldContainer().getAbsolutePath() + "/" + worldName);
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if (file.isDirectory()) {
                    deleteWorldFolder(worldName + "\\" + file.getName(), false);
                } else {
                    file.delete();
                }
            }
        }
        path.delete();
    }
}
