package cz.nerkub.NerKubTowersOfDestiny.GameModes;

import cz.nerkub.NerKubTowersOfDestiny.Managers.GameManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.GameState;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import cz.nerkub.NerKubTowersOfDestiny.Managers.ArenaManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class PlayerSwapperMode implements GameModeBase {
	private final NerKubTowersOfDestiny plugin;
	private final ArenaManager arenaManager;
	private final GameManager gameManager;
	private final Random random = new Random();

	public PlayerSwapperMode(NerKubTowersOfDestiny plugin, ArenaManager arenaManager, GameManager gameManager) {
		this.plugin = plugin;
		this.arenaManager = arenaManager;
		this.gameManager = gameManager;
	}

	@Override
	public void startMode(String arenaName) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		int interval = plugin.getModes().getConfig().getInt("player-swapper.swap-interval");
		List<Player> players = gameManager.getPlayersInArena(arenaName);

		if (players.size() < 2) {
			return;
		}

		plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getModes().getConfig().getString("player-swapper.start-message")
						.replace("%arenaname%", arenaName)
						.replace("%interval%", String.valueOf(interval))));

		new BukkitRunnable() {
			@Override
			public void run() {
				if (gameManager.getGameState(arenaName) != GameState.RUNNING) {
					cancel();
					return;
				}

				List<Player> players = gameManager.getPlayersInArena(arenaName);
				if (players.size() < 2) {
					return; // Nemá smysl swapovat, pokud je v aréně jen jeden hráč
				}

				// ✅ Získání dvou NÁHODNÝCH hráčů, kteří nejsou stejní
				Player player1 = players.get(random.nextInt(players.size()));
				Player player2;
				do {
					player2 = players.get(random.nextInt(players.size()));
				} while (player1 == player2); // Zajistí, že vybere dva různé hráče

				Location loc1 = player1.getLocation();
				Location loc2 = player2.getLocation();

				player1.teleport(loc2);
				player2.teleport(loc1);

			}
		}.runTaskTimer(plugin, 0L, 20L * interval); // ✅ Swap každých 10 sekund (můžeš změnit)

	}
}
