package me.kajias.fireballfight.objects;

import fr.mrmicky.fastboard.FastBoard;
import me.clip.placeholderapi.PlaceholderAPI;
import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.configurations.DataConfiguration;
import me.kajias.fireballfight.objects.enums.ArenaState;
import me.kajias.fireballfight.objects.enums.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Scoreboard implements Listener {
    private final static FileConfiguration config = FireballFight.INSTANCE.getConfig();
    public static Map<UUID, FastBoard> boards = new HashMap<>();

    public Scoreboard() {
        FireballFight.INSTANCE.getServer().getScheduler().scheduleSyncRepeatingTask(FireballFight.INSTANCE, () -> {
            for (FastBoard board : boards.values()) {
                Player player = board.getPlayer();
                GamePlayer playerData = DataConfiguration.getPlayerData(player.getUniqueId());
                if (playerData == null) continue;

                List<String> lines = new ArrayList<>();
                if (Arena.getPlayerArenaMap().containsKey(player)) {
                    Arena arena = Arena.getPlayerArenaMap().get(player);
                    switch (arena.getState()) {
                        case WAITING:
                        case STARTING:
                            for (String line : PlaceholderAPI.setPlaceholders(player, config.getStringList("Scoreboard.Waiting"))) {
                                lines.add(line
                                        .replace("%arena_name%", String.valueOf(arena.getName()))
                                        .replace("%arena_players%", String.valueOf(arena.getPlayers().size()))
                                        .replace("%arena_players_total%", String.valueOf(arena.getAllowedPlayersAmount()))
                                        .replace("%game_type%", config.getString("Messages.GameType." + arena.getGameType()))
                                        .replace("%arena_type%", String.valueOf(arena.getArenaType()))
                                );
                            }
                            updateBoard(board, lines.toArray(new String[0]));
                            break;
                        case STARTED:
                            GamePlayer arenaPlayerData = arena.getGame().getPlayersData().stream().filter(x -> x.getUniqueId().equals(player.getUniqueId())).findAny().orElse(null);
                            for (String line : PlaceholderAPI.setPlaceholders(player, config.getStringList("Scoreboard.Started"))) {
                                lines.add(line
                                        .replace("%red_team_bed%", arena.getGame().isRedTeamBedDestroyed ? "&c" + arena.getGame().getAlivePlayersInTeam(TeamType.RED).size() : "&c✔")
                                        .replace("%red_team_indicator%", arena.getTeamType(player) == TeamType.RED ? config.getString("Messages.TeamIndicator") : "")
                                        .replace("%blue_team_bed%", arena.getGame().isBlueTeamBedDestroyed ? "&9" + arena.getGame().getAlivePlayersInTeam(TeamType.BLUE).size() : "&9✔")
                                        .replace("%blue_team_indicator%", arena.getTeamType(player) == TeamType.BLUE ? config.getString("Messages.TeamIndicator") : "")
                                        .replace("%kills%", arenaPlayerData != null ? String.valueOf(arenaPlayerData.getKills()) : "0")
                                        .replace("%arena_name%", String.valueOf(arena.getName()))
                                        .replace("%game_type%", config.getString("Messages.GameType." + arena.getGameType()))
                                        .replace("%arena_type%", String.valueOf(arena.getArenaType()))
                                );
                            }
                            updateBoard(board, lines.toArray(new String[0]));
                            break;
                        case ENDING:
                            lines.addAll(PlaceholderAPI.setPlaceholders(player, config.getStringList("Scoreboard.Ending")));
                            updateBoard(board, lines.toArray(new String[0]));
                            break;
                    }
                } else {
                    for (String line : PlaceholderAPI.setPlaceholders(player, config.getStringList("Scoreboard.Lobby"))) {
                        lines.add(line
                                .replace("%games_played%", String.valueOf(playerData.getGamesPlayed()))
                                .replace("%games_won%", String.valueOf(playerData.getGamesWon()))
                                .replace("%kills%", String.valueOf(playerData.getKills()))
                                .replace("%deaths%", String.valueOf(playerData.getDeaths()))
                                .replace("%beds_destroyed%", String.valueOf(playerData.getBedsDestroyed()))
                        );
                    }
                    updateBoard(board, lines.toArray(new String[0]));
                }
            }
        }, 0L, 10L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FastBoard board = new FastBoard(player);

        board.updateTitle(Utils.color( FireballFight.INSTANCE.getConfig().getString("Scoreboard.Title")));

        boards.put(player.getUniqueId(), board);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FastBoard board = boards.remove(player.getUniqueId());

        if (board != null) board.delete();
    }

    private void updateBoard(FastBoard board, String @NotNull ... lines) {
        for (int a = 0; a < lines.length; ++a) {
            lines[a] = Utils.color( lines[a]);
        }

        board.updateLines(lines);
    }
}
