package me.kajias.fireballfight;

import me.kajias.fireballfight.commands.AdminCommand;
import me.kajias.fireballfight.commands.LeaveCommand;
import me.kajias.fireballfight.configurations.ArenaConfiguration;
import me.kajias.fireballfight.configurations.DataConfiguration;
import me.kajias.fireballfight.gui.GUIListener;
import me.kajias.fireballfight.gui.GUIManager;
import me.kajias.fireballfight.listeners.*;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.ArenaManager;
import me.kajias.fireballfight.objects.GamePlayer;
import me.kajias.fireballfight.objects.Scoreboard;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class FireballFight extends JavaPlugin
{
    public static FireballFight INSTANCE = null;
    public static GUIManager guiManager = null;
    public static GUIListener guiListener = null;
    private static Economy econ = null;

    private PlayerPoints playerPoints = null;
    public Location lobbyLocation;
    public List<GamePlayer> loadedPlayerData = new ArrayList<>();

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Bukkit.getLogger().severe("PlaceholderAPI is required to run this plugin");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            Bukkit.getLogger().severe("PlayerPoints is required to run this plugin");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        } else {
            playerPoints = (PlayerPoints) this.getServer().getPluginManager().getPlugin("PlayerPoints");
        }

        if (!setupEconomy() ) {
            getLogger().severe("Vault is required to run this plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        INSTANCE = this;
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (getConfig().getConfigurationSection("LobbySpawnPoint") != null) {
            lobbyLocation = new Location(
                    Bukkit.getWorld(getConfig().getString("LobbySpawnPoint.World")),
                    getConfig().getDouble("LobbySpawnPoint.X"),
                    getConfig().getDouble("LobbySpawnPoint.Y"),
                    getConfig().getDouble("LobbySpawnPoint.Z"));
            lobbyLocation.setYaw((float) getConfig().getDouble("LobbySpawnPoint.Yaw"));
        } else getLogger().warning( getConfig().getString("LogMessages.LobbyWasNotSet"));

        if(!DataConfiguration.getPlayersFromConfig().isEmpty()) {
            getLogger().fine( "Loading players data...");
            loadedPlayerData.addAll(DataConfiguration.getPlayersFromConfig());
            getLogger().fine( "Loaded " + loadedPlayerData.size() + " players data.");
        }

        try {
            for (Arena a : ArenaConfiguration.getArenas()) {
                if (ArenaManager.loadArena(a)) {
                    getLogger().fine("Arena \"" + a.getName() + "\" was loaded successfully.");
                }
            }
        } catch (Exception ignored) {}

        guiManager = new GUIManager();
        guiListener = new GUIListener(guiManager);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new BlockEventsHandler(), this);
        getServer().getPluginManager().registerEvents(new DisabledEvents(), this);
        getServer().getPluginManager().registerEvents(new DoubleJumpHandler(), this);
        getServer().getPluginManager().registerEvents(new InventoryClickHandler(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathHandler(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityHandler(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractItemHandler(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitJoinHandler(), this);
        getServer().getPluginManager().registerEvents(new Scoreboard(), this);

        getCommand("ff").setExecutor(new AdminCommand());
        getCommand("fireballfight").setExecutor(new AdminCommand());
        getCommand("leave").setExecutor(new LeaveCommand());
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public PlayerPoints getPlayerPoints() {
        return playerPoints;
    }

    public Economy getEconomy() {
        return econ;
    }

    @Override
    public void onDisable() {
        for(Arena arena : ArenaManager.getLoadedArenas()) {
            arena.stop();
        }

        ArenaManager.getLoadedArenas().forEach(ArenaConfiguration::saveArena);
        DataConfiguration.savePlayerData(loadedPlayerData);
    }
}
