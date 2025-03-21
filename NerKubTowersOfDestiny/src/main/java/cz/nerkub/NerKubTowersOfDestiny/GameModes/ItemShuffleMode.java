package cz.nerkub.NerKubTowersOfDestiny.GameModes;

import cz.nerkub.NerKubTowersOfDestiny.Managers.GameManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.GameState;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import cz.nerkub.NerKubTowersOfDestiny.Utils.RandomItemGenerator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class ItemShuffleMode implements GameModeBase {
	private final NerKubTowersOfDestiny plugin;
	private final GameManager gameManager;
	private final RandomItemGenerator itemGenerator;

	public ItemShuffleMode(NerKubTowersOfDestiny plugin, GameManager gameManager) {
		this.plugin = plugin;
		this.gameManager = gameManager;
		this.itemGenerator = new RandomItemGenerator();
	}

	@Override
	public void startMode(String arenaName) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		int interval = plugin.getModes().getConfig().getInt("item-shuffle.shuffle-interval");
		List<Player> players = gameManager.getPlayersInArena(arenaName);

		if (players.isEmpty()) {
			return;
		}

		Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getModes().getConfig().getString("item-shuffle.start-message")
						.replace("%arenaname%", arenaName)
						.replace("%interval%", String.valueOf(interval))));

		new BukkitRunnable() {
			@Override
			public void run() {
				if (gameManager.getGameState(arenaName) != GameState.RUNNING) {
					cancel();
					return;
				}

				for (Player player : players) {
					ItemStack[] inventory = player.getInventory().getContents();

					// ✅ Vygenerujeme úplně nové itemy do hotbaru
					for (int i = 0; i < 9; i++) {
						inventory[i] = itemGenerator.generateRandomItem();
					}

					player.getInventory().setContents(inventory);
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
							plugin.getModes().getConfig().getString("item-shuffle.shuffle-notify")));
				}
			}
		}.runTaskTimer(plugin, 0L, 20L * 15); // Shuffle každých 15 sekund
	}
}
