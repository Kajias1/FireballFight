package me.kajias.fireballfight.configurations;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.objects.GamePlayer;
import org.bukkit.Bukkit;

import java.time.LocalDate;
import java.util.*;

public class DataConfiguration
{
    private static final BaseConfiguration config = new BaseConfiguration("data");

    public static void savePlayerData(List<GamePlayer> loadedPlayers) {
        for(GamePlayer player : loadedPlayers) {
            savePlayerData(player);
        }
    }

    public static void savePlayerData(GamePlayer player) {
        config.getConfig().set("Data." + player.getUniqueId() + ".Name", player.getPlayerName());
        config.getConfig().set("Data." + player.getUniqueId() + ".BedsDestroyed", player.getBedsDestroyed());
        config.getConfig().set("Data." + player.getUniqueId() + ".GamesPlayed", player.getGamesPlayed());
        config.getConfig().set("Data." + player.getUniqueId() + ".GamesWon", player.getGamesWon());
        config.getConfig().set("Data." + player.getUniqueId() + ".Kills", player.getKills());
        if (config.getConfig().getConfigurationSection("Data." + player.getUniqueId() + ".BoughtBonusItems") != null)
            config.getConfig().set("Data." + player.getUniqueId() + ".BoughtBonusItems", null);
        for (Map.Entry<String, Integer> pair : player.getBoughtBonusItems().entrySet()) {
            config.getConfig().set("Data." + player.getUniqueId() + ".BoughtBonusItems." + pair.getKey(), pair.getValue());
        }
        config.getConfig().set("Data." + player.getUniqueId() + ".LastDate", player.getLastDate().toString());

        Optional<GamePlayer> optPlayer = FireballFight.INSTANCE.loadedPlayerData.stream()
                .filter(x -> x.getUniqueId().equals(player.getUniqueId())).findFirst();
        if(optPlayer.isPresent()) {
            FireballFight.INSTANCE.loadedPlayerData.set(FireballFight.INSTANCE.loadedPlayerData
                    .indexOf(optPlayer.get()), player);
        } else {
            FireballFight.INSTANCE.loadedPlayerData.add(player);
        }
        config.save();
    }

    public static GamePlayer getPlayerDataFromConfig(UUID uuid) {
        GamePlayer result = new GamePlayer(uuid);
        if(config.getConfig().getConfigurationSection("Data." + uuid.toString()) != null) {
            result.setPlayerName(config.getConfig().getString("Data." + uuid + ".Name"));
            result.setGamesPlayed(config.getConfig().getInt("Data." + uuid + ".GamesPlayed"));
            result.setBedsDestroyed(config.getConfig().getInt("Data." + uuid + ".BedsDestroyed"));
            result.setGamesWon(config.getConfig().getInt("Data." + uuid + ".GamesWon"));
            result.setKills(config.getConfig().getInt("Data." + uuid + ".Kills"));
            if (config.getConfig().getConfigurationSection("Data." + uuid + ".BoughtBonusItems") != null) {
                for (String bonusItemName : config.getConfig().getConfigurationSection("Data." + uuid + ".BoughtBonusItems").getKeys(true)) {
                    result.getBoughtBonusItems().put(bonusItemName, config.getConfig().getInt("Data." + uuid + ".BoughtBonusItems." + bonusItemName));
                }
            }
            if (config.getConfig().getString("Data." + uuid + ".LastDate") != null) {
                result.setLastDate(LocalDate.parse(config.getConfig().getString("Data." + uuid + ".LastDate")));
            }
        }
        result.updateDateAndItems(LocalDate.now());
        return result;
    }

    public static List<GamePlayer> getPlayersFromConfig() {
        List<GamePlayer> players = new ArrayList<>();
        for(String uuid : config.getConfig().getConfigurationSection("Data").getKeys(false)) {
            players.add(getPlayerDataFromConfig(UUID.fromString(uuid)));
        }

        return players;
    }

    public static GamePlayer getPlayerData(UUID uuid) {
        return FireballFight.INSTANCE.loadedPlayerData.stream().filter(x -> x.getUniqueId().equals(uuid)).findAny().orElse(null);
    }
}
