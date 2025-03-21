package cz.nerkub.NerKubTowersOfDestiny.GUI;

import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import cz.nerkub.NerKubTowersOfDestiny.Managers.VoteManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

public class VoteGUI implements Listener {
	private final VoteManager voteManager;
	private final NerKubTowersOfDestiny plugin;
	private final Set<Player> clickCooldown = new HashSet<>();

	public VoteGUI(NerKubTowersOfDestiny plugin, VoteManager voteManager) {
		this.plugin = plugin;
		this.voteManager = voteManager;
	}

	public void open(Player player) {
		String arenaName = voteManager.getArena(player);
		String prefix = plugin.getMessages().getConfig().getString("prefix");

		if (arenaName == null) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("vote.player-not-in-arena")));
			return;
		}

		FileConfiguration config = plugin.getVoteGui().getConfig();
		String title = ChatColor.translateAlternateColorCodes('&', config.getString("gui.title", "Vote Menu"));
		int size = config.getInt("gui.size", 9);

		Inventory gui = Bukkit.createInventory(null, size, title);
		String playerVote = voteManager.getPlayerVote(player, arenaName);

		ConfigurationSection itemsSection = config.getConfigurationSection("gui.items");
		if (itemsSection != null) {
			for (String key : itemsSection.getKeys(false)) {
				ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
				if (itemSection == null) continue;

				int slot = itemSection.getInt("slot", 0);
				Material material = Material.matchMaterial(itemSection.getString("material", "BARRIER"));
				if (material == null) material = Material.BARRIER;

				String displayName = ChatColor.translateAlternateColorCodes('&', itemSection.getString("display_name", "Unnamed"));
				List<String> lore = itemSection.getStringList("lore");

				ItemStack item = createVoteItem(material, displayName, lore, playerVote);
				gui.setItem(slot, item);
			}
		}

		player.openInventory(gui);
	}

	private ItemStack createVoteItem(Material material, String name, List<String> lore, String playerVote) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();

		if (meta != null) {
			meta.setDisplayName(name);
			List<String> formattedLore = new ArrayList<>();
			for (String line : lore) {
				formattedLore.add(ChatColor.translateAlternateColorCodes('&', line));
			}
			meta.setLore(formattedLore);

			// ✅ Pokud je to aktuální hlas hráče, přidáme enchant efekt
			if (ChatColor.stripColor(name).equalsIgnoreCase(playerVote) &&
					plugin.getVoteGui().getConfig().getBoolean("selected.enchant", true)) {

				Enchantment enchantment = Enchantment.getByName(plugin.getVoteGui().getConfig().getString("selected.enchantment", "UNBREAKING"));
				if (enchantment != null) {
					meta.addEnchant(enchantment, 1, true);
				}

				if (plugin.getVoteGui().getConfig().getBoolean("selected.hide_enchants", true)) {
					meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				}
			}

			item.setItemMeta(meta);
		}
		return item;
	}


	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		Player player = (Player) event.getWhoClicked();

		String inventoryTitle = ChatColor.stripColor(player.getOpenInventory().getTitle());
		String configTitle = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
				plugin.getVoteGui().getConfig().getString("gui.title", "Vote Menu")));

		if (!inventoryTitle.equalsIgnoreCase(configTitle)) {
			return;
		}

		event.setCancelled(true);

		if (clickCooldown.contains(player)) {
			return;
		}

		clickCooldown.add(player);
		Bukkit.getScheduler().runTaskLater(plugin, () -> clickCooldown.remove(player), 5L);

		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem == null || clickedItem.getType() == Material.AIR) {
			return;
		}

		if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
			return;
		}

		String modeName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
		String arenaName = voteManager.getArena(player);

		if (arenaName == null) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("vote.player-not-in-arena")));
			return;
		}

		String previousVote = voteManager.getPlayerVote(player, arenaName);
		if (previousVote != null && previousVote.equals(modeName)) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("vote.already-voted")));
			return;
		}

		if (previousVote != null) {
			voteManager.removeVote(player, arenaName, previousVote);
		}

		voteManager.voteForMode(player, arenaName, modeName);
		open(player);

		player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
	}
}
