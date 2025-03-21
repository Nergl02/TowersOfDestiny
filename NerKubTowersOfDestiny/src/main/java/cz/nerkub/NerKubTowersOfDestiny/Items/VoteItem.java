package cz.nerkub.NerKubTowersOfDestiny.Items;

import cz.nerkub.NerKubTowersOfDestiny.Managers.VoteManager;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class VoteItem implements Listener {
	private final NerKubTowersOfDestiny plugin;
	private final VoteManager voteManager;

	private static final String VOTE_ITEM_NAME = ChatColor.GOLD + "🗳 Hlasování o mód";

	public VoteItem(NerKubTowersOfDestiny plugin, VoteManager voteManager) {
		this.plugin = plugin;
		this.voteManager = voteManager;
	}

	/**
	 * 🎟 Vytvoří hlasovací item
	 */
	public static ItemStack createVoteItem() {
		ItemStack voteItem = new ItemStack(Material.PAPER);
		ItemMeta meta = voteItem.getItemMeta();

		if (meta != null) {
			meta.setDisplayName(VOTE_ITEM_NAME);
			meta.setLore(Arrays.asList(
					ChatColor.YELLOW + "Klikni pravým pro hlasování!",
					ChatColor.GRAY + "Vyber herní mód pro tuto hru."
			));
			voteItem.setItemMeta(meta);
		}
		return voteItem;
	}

	/**
	 * 🎟 Přidělení hlasovacího itemu hráči při vstupu do arény
	 */
	public void giveVoteItem(Player player) {
		if (player.hasPermission("towers.vote")) {
			player.getInventory().addItem(createVoteItem());
		}
	}

	/**
	 * 📜 Event – otevření hlasovacího GUI
	 */
	@EventHandler
	public void onVoteItemClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if (item != null && item.getType() == Material.PAPER && item.hasItemMeta() &&
				item.getItemMeta().getDisplayName().equals(VOTE_ITEM_NAME)) {
			event.setCancelled(true); // Zabráníme umístění itemu
			voteManager.openVoteMenu(player); // Otevřeme GUI pro hlasování
			player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
		}
	}

	public static void removeVoteItem(Player player) {
		for (ItemStack item : player.getInventory().getContents()) {
			if (item != null && item.hasItemMeta() &&
					VOTE_ITEM_NAME.equals(item.getItemMeta().getDisplayName())) {
				player.getInventory().remove(item);
				break; // ✅ Stačí odstranit první nalezený VoteItem
			}
		}
	}
}