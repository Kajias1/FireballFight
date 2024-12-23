package me.kajias.fireballfight.objects;

import org.bukkit.Bukkit;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GamePlayer
{
    private final UUID uuid;
    private String playerName;
    private int bedsDestroyed;
    private int gamesPlayed;
    private int gamesWon;
    private int kills;
    private int deaths;
    private HashMap<String, Integer> boughtBonusItems;
    private LocalDate lastDate;

    public GamePlayer(UUID uuid) {
        this.uuid = uuid;
        playerName = null;
        bedsDestroyed = 0;
        gamesPlayed = 0;
        gamesWon = 0;
        kills = 0;
        deaths = 0;
        boughtBonusItems = new HashMap<>();
        lastDate = LocalDate.now();
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getBedsDestroyed() {
        return bedsDestroyed;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public HashMap<String, Integer> getBoughtBonusItems () {
        return boughtBonusItems;
    }

    public LocalDate getLastDate() {
        return lastDate;
    }

    public void setBedsDestroyed(int bedsDestroyed) {
        this.bedsDestroyed = bedsDestroyed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setLastDate(LocalDate lastDate) {
        this.lastDate = lastDate;
    }

    public void setBoughtBonusItems(HashMap<String, Integer> boughtBonusItems) {
        this.boughtBonusItems = boughtBonusItems;
    }

    public void updateDateAndItems(LocalDate lastDate) {
        if (this.lastDate.isBefore(lastDate)) {
            if (!this.boughtBonusItems.isEmpty()) {
                HashMap<String, Integer> updatedBoughtBonusItems = new HashMap<>();
                for (Map.Entry<String, Integer> entry : this.boughtBonusItems.entrySet()) {
                    if (entry.getValue() == -1) updatedBoughtBonusItems.put(entry.getKey(), entry.getValue());
                    else if (entry.getValue() > 1) updatedBoughtBonusItems.put(entry.getKey(), entry.getValue() - (int) ChronoUnit.DAYS.between(this.lastDate, lastDate));
                }
                boughtBonusItems = updatedBoughtBonusItems;
            }
        }
        this.lastDate = lastDate;
    }
}
