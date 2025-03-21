package org.nojorundev.nexperience;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class NExperience extends JavaPlugin implements Listener {

    private String helpMessage;
    private String bottleName;
    private String bottleLore;
    private String noPermissionMessage;
    private String invalidArgMessage;
    private String notEnoughExpMessage;
    private String successMessage;
    private String receivedExpMessage;

    private NamespacedKey expKey;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        loadConfig();
        expKey = new NamespacedKey(this, "experience_amount"); // Создаем уникальный ключ для NBT
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void loadConfig() {
        this.reloadConfig();
        this.helpMessage = translateHexColors(getConfig().getString("messages.help", "NExperience помощь:\n/nexp <кол-во|max> - конвертировать опыт.\n/nexp reload - перезагрузить конфиг."));
        this.bottleName = translateHexColors(getConfig().getString("bottle.name", "Бутылка с {amount} опытом"));
        this.bottleLore = translateHexColors(getConfig().getString("bottle.lore", "Внутри {amount} опыта."));
        this.noPermissionMessage = translateHexColors(getConfig().getString("messages.no-permission", "§cУ вас нет прав на это."));
        this.invalidArgMessage = translateHexColors(getConfig().getString("messages.invalid-arg", "§cНеверный аргумент."));
        this.notEnoughExpMessage = translateHexColors(getConfig().getString("messages.not-enough-exp", "§cУ вас недостаточно опыта."));
        this.successMessage = translateHexColors(getConfig().getString("messages.success", "§aВы успешно конвертировали опыт."));
        this.receivedExpMessage = translateHexColors(getConfig().getString("messages.received-exp", "§aВы получили {amount} уровней опыта."));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только игрокам.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nexperience.use")) {
            player.sendMessage(noPermissionMessage);
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(helpMessage);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("nexperience.admin")) {
                player.sendMessage(noPermissionMessage);
                return true;
            }
            loadConfig();
            player.sendMessage(translateHexColors(getConfig().getString("messages.config-reloaded", "&aКонфиг перезагружен.")));
            return true;
        }

        int amount;
        if (args[0].equalsIgnoreCase("max")) {
            amount = player.getLevel();
        } else {
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(invalidArgMessage);
                return true;
            }
        }

        if (amount <= 0) {
            player.sendMessage(invalidArgMessage);
            return true;
        }

        if (player.getLevel() < amount) {
            player.sendMessage(notEnoughExpMessage);
            return true;
        }

        player.setLevel(player.getLevel() - amount); // Убираем уровни у игрока
        ItemStack bottle = createExperienceBottle(amount);
        player.getInventory().addItem(bottle);
        player.sendMessage(successMessage);

        return true;
    }

    private ItemStack createExperienceBottle(int amount) {
        ItemStack bottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = bottle.getItemMeta();
        meta.setDisplayName(bottleName.replace("{amount}", String.valueOf(amount)));
        List<String> lore = new ArrayList<>();
        lore.add(bottleLore.replace("{amount}", String.valueOf(amount)));
        meta.setLore(lore);

        // Сохраняем количество уровней в NBT
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(expKey, PersistentDataType.INTEGER, amount);

        bottle.setItemMeta(meta);
        return bottle;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.EXPERIENCE_BOTTLE && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();

            // Проверяем, есть ли в NBT количество уровней
            if (data.has(expKey, PersistentDataType.INTEGER)) {
                int amount = data.get(expKey, PersistentDataType.INTEGER);
                player.setLevel(player.getLevel() + amount); // Даем игроку уровни
                player.sendMessage(receivedExpMessage.replace("{amount}", String.valueOf(amount))); // Сообщение о получении опыта
                item.setAmount(item.getAmount() - 1); // Уменьшаем количество бутылок
                event.setCancelled(true); // Отменяем стандартное поведение бутылки
            }
        }
    }

    // Метод для конвертации HEX-цветов и стандартных цветов Minecraft
    private String translateHexColors(String text) {
        if (text == null) return "";
        // Сначала заменяем стандартные цвета Minecraft (&f, &c и т.д.)
        text = ChatColor.translateAlternateColorCodes('&', text);
        // Затем заменяем HEX-цвета
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("&#([0-9a-fA-F]{6})");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            // Заменяем &#ffffff на §x§f§f§f§f§f§f
            String hex = matcher.group(1);
            String replacement = "§x" + hex.chars()
                    .mapToObj(c -> "§" + (char) c)
                    .collect(java.util.stream.Collectors.joining());
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
