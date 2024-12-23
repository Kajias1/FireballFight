package me.kajias.fireballfight.configurations;

import me.kajias.fireballfight.FireballFight;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class BaseConfiguration
{
    private final FireballFight plugin = FireballFight.INSTANCE;
    private final File file;
    private final String name;
    private FileConfiguration config;

    public BaseConfiguration(String name)
    {
        this.name = name.toLowerCase();
        this.file = new File(plugin.getDataFolder(), name + ".yml");
    }

    public void load() {
        if(!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(name + ".yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch(IOException | IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        if(config == null) load();
        return config;
    }
}
