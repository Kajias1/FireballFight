package me.kajias.fireballfight.objects;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.objects.enums.RuneType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;

public class Rune
{
    private final static FileConfiguration config = FireballFight.INSTANCE.getConfig();

    private Location location;
    private RuneType type;
    private ArmorStand armorStand;

    public Rune(Location location, RuneType type) {
        this.location = location;
        this.type = type;
        armorStand = null;

        this.location.setYaw(0);
    }

    public Location getLocation() {
        return location;
    }

    public RuneType getType() {
        return type;
    }

    public ArmorStand getArmorStand() {
        return armorStand;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setType(RuneType type) {
        this.type = type;
    }

    public void spawnArmorStand() {
        if (armorStand == null || armorStand.isDead()) {
            armorStand = location.getWorld().spawn(location, ArmorStand.class);
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setRemoveWhenFarAway(false);
            armorStand.setCustomNameVisible(true);
            armorStand.setCustomName(type == RuneType.HEALTH ? Utils.color(config.getString("Messages.HealthRuneHologram")) : Utils.color(config.getString("Messages.SpeedRuneHologram")));
            armorStand.setHelmet(type == RuneType.HEALTH ? Utils.getCustomTextureHead(config.getString("PlayerSkulls.HealthRune")) :
                    Utils.getCustomTextureHead(config.getString("PlayerSkulls.SpeedRune")));
        }
    }

    public void rotateArmorStand() {
        if (armorStand != null && !armorStand.isDead()) {
            location.setYaw(armorStand.getLocation().getYaw() + 1.0f);
            armorStand.teleport(location);
        }
    }

    public void killArmorStand() {
        if (armorStand != null) {
            armorStand.remove();
        }
        armorStand = null;
    }
}
