package me.kajias.fireballfight;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.kajias.fireballfight.configurations.DataConfiguration;
import me.kajias.fireballfight.objects.Arena;
import me.kajias.fireballfight.objects.Game;
import me.kajias.fireballfight.objects.GamePlayer;
import me.kajias.fireballfight.objects.enums.TeamType;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.regex.Pattern;

public class Utils
{
    private static final FileConfiguration config = FireballFight.INSTANCE.getConfig();
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + '&' + "[0-9A-FK-OR]");

    public static String stripColor(String input) {
        return input == null ? null : STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }
    
    public static void sendMessage(@NotNull Player player, String message) {
        player.sendMessage(color(FireballFight.INSTANCE.getConfig().getString("Messages.Prefix") + message));
    }

    public static void teleportToLobby(Player player) {
        if (Arena.getPlayerArenaMap().containsKey(player)) Arena.getPlayerArenaMap().get(player).removePlayer(player);

        Utils.removePotionEffects(player);
        player.setExp(0.0f);
        player.setLevel(0);
        player.setMaxHealth(20.0f);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        Bukkit.getOnlinePlayers().forEach(otherPlayer -> {
            if (otherPlayer != player) otherPlayer.showPlayer(player);
        });

        ItemStack fastJoinItem = new ItemStack(Material.getMaterial(config.getString("Items.FastJoinItem.Material")));
        ItemMeta fastJoinItemMeta = fastJoinItem.getItemMeta();
        fastJoinItemMeta.setDisplayName(Utils.color(config.getString("Items.FastJoinItem.Name")));
        fastJoinItem.setItemMeta(fastJoinItemMeta);
        player.getInventory().setItem(config.getInt("Items.FastJoinItem.Slot") - 1, fastJoinItem);

        ItemStack selectArenaItem = new ItemStack(Material.getMaterial(config.getString("Items.SelectArenaItem.Material")));
        ItemMeta selectArenaItemMeta = selectArenaItem.getItemMeta();
        selectArenaItemMeta.setDisplayName(Utils.color(config.getString("Items.SelectArenaItem.Name")));
        selectArenaItem.setItemMeta(selectArenaItemMeta);
        player.getInventory().setItem(config.getInt("Items.SelectArenaItem.Slot") - 1, selectArenaItem);

        ItemStack shopItem = new ItemStack(Material.getMaterial(config.getString("Items.BonusShopItem.Material")));
        ItemMeta shopItemMeta = shopItem.getItemMeta();
        shopItemMeta.setDisplayName(Utils.color(
                config.getString("Items.BonusShopItem.Name")));
        shopItem.setItemMeta(shopItemMeta);
        player.getInventory().setItem(config.getInt("Items.BonusShopItem.Slot") - 1, shopItem);

        player.setFallDistance(0.0f);
        if (FireballFight.INSTANCE.lobbyLocation != null) {
            player.teleport(FireballFight.INSTANCE.lobbyLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } else {
            sendMessage(player, config.getString("Messages.LobbyWasNotSet"));
            player.teleport(new Location(Bukkit.getWorld("world"), 0, Bukkit.getWorld("world").getHighestBlockYAt(0, 0), 0));
        }
    }

    public static void removePotionEffects(@NotNull Player p) {
        for(PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
    }

    public static void sendActionBar(Player p, String nachricht) {
        CraftPlayer cp = (CraftPlayer) p;
        IChatBaseComponent cbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + nachricht + "\"}");
        PacketPlayOutChat ppoc = new PacketPlayOutChat(cbc, (byte) 2);
        cp.getHandle().playerConnection.sendPacket(ppoc);
    }

    @Contract("_ -> new")
    public static @NotNull String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static void giveKit(Player player) {
        if (!Arena.getPlayerArenaMap().containsKey(player)) return;
        Arena arena = Arena.getPlayerArenaMap().get(player);
        if (arena.getGame() == null) return;
        Game game = arena.getGame();
        GamePlayer playerData = DataConfiguration.getPlayerData(player.getUniqueId());

        ItemStack stoneSword = new ItemStack(Material.STONE_SWORD);
        ItemMeta stoneSwordMeta = stoneSword.getItemMeta();
        stoneSwordMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        stoneSwordMeta.spigot().setUnbreakable(true);
        stoneSword.setItemMeta(stoneSwordMeta);
        player.getInventory().remove(Material.WOOL);
        player.getInventory().setItem(0, stoneSword);

        ItemStack wool = new ItemStack(Material.WOOL, 64, arena.getTeamType(player) == TeamType.RED ? (short) 14 : (short) 11);
        player.getInventory().remove(Material.WOOL);
        player.getInventory().setItem(1, wool);

        ItemStack stonePickaxe = new ItemStack(Material.STONE_PICKAXE);
        ItemMeta stonePickaxeMeta = stonePickaxe.getItemMeta();
        stonePickaxeMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        stonePickaxeMeta.spigot().setUnbreakable(true);
        stonePickaxeMeta.addEnchant(Enchantment.DIG_SPEED, 1, true);
        stonePickaxe.setItemMeta(stonePickaxeMeta);
        player.getInventory().remove(Material.STONE_PICKAXE);
        player.getInventory().setItem(2, stonePickaxe);

        ItemStack stoneAxe = new ItemStack(Material.STONE_AXE);
        ItemMeta stoneAxeMeta = stoneAxe.getItemMeta();
        stoneAxeMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        stoneAxeMeta.spigot().setUnbreakable(true);
        stoneAxeMeta.addEnchant(Enchantment.DIG_SPEED, 1, true);
        stoneAxe.setItemMeta(stoneAxeMeta);
        player.getInventory().remove(Material.STONE_AXE);
        player.getInventory().setItem(3, stoneAxe);

        ItemStack shears = new ItemStack(Material.SHEARS);
        ItemMeta shearsMeta = shears.getItemMeta();
        shearsMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        if (playerData != null && playerData.getBoughtBonusItems().containsKey(Utils.stripColor(config.getString("Menus.BonusShop.Items.ShearsEfficiency.Name")))) {
            shearsMeta.addEnchant(Enchantment.DIG_SPEED, 4, true);
        }
        shearsMeta.spigot().setUnbreakable(true);
        shears.setItemMeta(shearsMeta);
        player.getInventory().remove(Material.SHEARS);
        player.getInventory().setItem(4, shears);

        ItemStack wood = new ItemStack(Material.WOOD, 8);
        player.getInventory().remove(Material.WOOD);
        player.getInventory().setItem(5, wood);

        if (playerData != null && playerData.getBoughtBonusItems().containsKey(Utils.stripColor(config.getString("Menus.BonusShop.Items.Trampoline.Name")))) {
            ItemStack slimeball = new ItemStack(Material.getMaterial(config.getString("Items.TrampolineItem.Material")));
            ItemMeta slimeballMeta = slimeball.getItemMeta();
            slimeballMeta.setDisplayName(Utils.color(config.getString("Items.TrampolineItem.Name")));
            slimeball.setItemMeta(slimeballMeta);
            player.getInventory().remove(Material.getMaterial(config.getString("Items.TrampolineItem.Material")));
            player.getInventory().addItem(slimeball);
        }

        ItemStack leatherHelmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta leatherHelmetMeta = (LeatherArmorMeta) leatherHelmet.getItemMeta();
        leatherHelmetMeta.setColor(arena.getTeamType(player) == TeamType.RED ? Color.RED : Color.BLUE);
        leatherHelmetMeta.spigot().setUnbreakable(true);
        leatherHelmetMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        leatherHelmet.setItemMeta(leatherHelmetMeta);
        player.getInventory().remove(Material.LEATHER_HELMET);
        player.getInventory().setHelmet(leatherHelmet);

        ItemStack leatherChestPlate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta leatherChestPlateMeta = (LeatherArmorMeta) leatherChestPlate.getItemMeta();
        leatherChestPlateMeta.setColor(arena.getTeamType(player) == TeamType.RED ? Color.RED : Color.BLUE);
        leatherChestPlateMeta.spigot().setUnbreakable(true);
        leatherChestPlateMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        leatherChestPlate.setItemMeta(leatherChestPlateMeta);
        player.getInventory().remove(Material.LEATHER_CHESTPLATE);
        player.getInventory().setChestplate(leatherChestPlate);

        ItemStack leatherLeggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leatherLeggingsMeta = (LeatherArmorMeta) leatherLeggings.getItemMeta();
        leatherLeggingsMeta.setColor(arena.getTeamType(player) == TeamType.RED ? Color.RED : Color.BLUE);
        leatherLeggingsMeta.spigot().setUnbreakable(true);
        leatherLeggingsMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        leatherLeggings.setItemMeta(leatherLeggingsMeta);
        player.getInventory().remove(Material.LEATHER_LEGGINGS);
        player.getInventory().setLeggings(leatherLeggings);

        ItemStack leatherBoots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta leatherBootsMeta = (LeatherArmorMeta) leatherBoots.getItemMeta();
        leatherBootsMeta.setColor(arena.getTeamType(player) == TeamType.RED ? Color.RED : Color.BLUE);
        leatherBootsMeta.spigot().setUnbreakable(true);
        leatherBootsMeta.addEnchant(Enchantment.PROTECTION_FALL, 5, true);
        leatherBootsMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        leatherBootsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        leatherBoots.setItemMeta(leatherBootsMeta);
        player.getInventory().remove(Material.LEATHER_BOOTS);
        player.getInventory().setBoots(leatherBoots);

        ItemStack tnt = new ItemStack(Material.TNT, 2);
        player.getInventory().remove(Material.TNT);
        player.getInventory().addItem(tnt);

        ItemStack fireball = new ItemStack(Material.FIREBALL, 6);
        player.getInventory().remove(Material.FIREBALL);
        player.getInventory().addItem(fireball);

        if (playerData != null && playerData.getBoughtBonusItems().containsKey(Utils.stripColor(config.getString("Menus.BonusShop.Items.SlimePlatform.Name")))) {
            ItemStack slimePlatform = new ItemStack(Material.getMaterial(config.getString("Items.SlimePlatformItem.Material")), 2);
            ItemMeta slimePlatformMeta = slimePlatform.getItemMeta();
            slimePlatformMeta.setDisplayName(Utils.color(config.getString("Items.SlimePlatformItem.Name")));
            slimePlatform.setItemMeta(slimePlatformMeta);
            player.getInventory().remove(Material.getMaterial(config.getString("Items.SlimePlatformItem.Material")));
            player.getInventory().setItem(35, slimePlatform);
        }
    }

    public static String getInGamePlayerName(Player player) {
        Arena arena = Arena.getPlayerArenaMap().get(player);
        if (arena != null) {
            if (arena.getTeamType(player) == null) return player.getName();
            return config.getString("Messages.TeamType." + arena.getTeamType(player)) + player.getName();
        }
        return player.getName();
    }

    public static void applyTimerToTNT(@NotNull TNTPrimed tnt, Player player) {
        tnt.setFuseTicks(3 * 20);
        tnt.setCustomNameVisible(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (tnt.isDead()) cancel();
                tnt.setCustomName(new DecimalFormat("#0.0##").format(tnt.getFuseTicks() / 20.0));
            }
        }.runTaskTimer(FireballFight.INSTANCE, 0L, 1L);
    }

    public static ItemStack getCustomTextureHead(String value) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        profile.getProperties().put("textures", new Property("textures", value));
        Field profileField = null;
        try {
            profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
        head.setItemMeta(meta);
        return head;
    }
}
