package me.kajias.fireballfight.commands;

import me.kajias.fireballfight.FireballFight;
import me.kajias.fireballfight.Utils;
import me.kajias.fireballfight.objects.Arena;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            FileConfiguration config = FireballFight.INSTANCE.getConfig();
            Player player = (Player) sender;

            if (args.length == 0) {
                if (Arena.getPlayerArenaMap().containsKey(player)) {
                    Arena.getPlayerArenaMap().get(player).removePlayer(player);
                } else Utils.sendMessage(player, config.getString("Messages.NotInGame"));
            }
        }

        return true;
    }
}
